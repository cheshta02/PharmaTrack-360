package pharmatrack.logistics.service;

import pharmatrack.common.util.InputValidator;
import pharmatrack.logistics.model.Vehicle;
import pharmatrack.logistics.model.Warehouse;
import pharmatrack.logistics.routing.CheckpointGenerator;
import pharmatrack.logistics.routing.GeocodingService;
import pharmatrack.storage.InMemoryDataStore;
import pharmatrack.temperature.model.Crate;
import pharmatrack.temperature.model.PharmaceuticalProduct;
import pharmatrack.temperature.model.ProductBatch;
import pharmatrack.temperature.model.TemperaturePolicy;
import pharmatrack.temperature.service.CrateInventoryService;
import pharmatrack.temperature.service.ProductBatchService;
import pharmatrack.temperature.service.ProductService;
import pharmatrack.temperature.service.TemperaturePolicyService;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class WarehouseSelectionService {
    private final InMemoryDataStore dataStore;
    private final WarehouseService warehouseService;
    private final WarehouseInventoryService inventoryService;
    private final ProductService productService;
    private final ProductBatchService batchService;
    private final TemperaturePolicyService policyService;
    private final CrateInventoryService crateInventoryService;
    private final VehicleService vehicleService;
    private final VehicleCompatibilityValidator vehicleValidator;
    private final GeocodingService geocodingService;

    public static class WarehouseFeasibilityResult {
        private final Warehouse selectedWarehouse;
        private final ProductBatch selectedBatch;
        private final List<Crate> compatibleCrates;
        private final Vehicle availableVehicle;
        private final boolean feasible;
        private final String explanation;
        private final List<String> evaluationLogs;

        public WarehouseFeasibilityResult(Warehouse selectedWarehouse, ProductBatch selectedBatch, List<Crate> compatibleCrates, Vehicle availableVehicle, boolean feasible, String explanation, List<String> evaluationLogs) {
            this.selectedWarehouse = selectedWarehouse;
            this.selectedBatch = selectedBatch;
            this.compatibleCrates = compatibleCrates != null ? compatibleCrates : Collections.emptyList();
            this.availableVehicle = availableVehicle;
            this.feasible = feasible;
            this.explanation = explanation;
            this.evaluationLogs = evaluationLogs != null ? evaluationLogs : Collections.emptyList();
        }

        public Warehouse getSelectedWarehouse() {
            return selectedWarehouse;
        }

        public ProductBatch getSelectedBatch() {
            return selectedBatch;
        }

        public List<Crate> getCompatibleCrates() {
            return compatibleCrates;
        }

        public Vehicle getAvailableVehicle() {
            return availableVehicle;
        }

        public boolean isFeasible() {
            return feasible;
        }

        public String getExplanation() {
            return explanation;
        }

        public List<String> getEvaluationLogs() {
            return evaluationLogs;
        }
    }

    public WarehouseSelectionService(InMemoryDataStore dataStore, WarehouseService warehouseService, WarehouseInventoryService inventoryService, ProductService productService, ProductBatchService batchService, TemperaturePolicyService policyService, CrateInventoryService crateInventoryService, VehicleService vehicleService, VehicleCompatibilityValidator vehicleValidator, GeocodingService geocodingService) {
        this.dataStore = dataStore;
        this.warehouseService = warehouseService;
        this.inventoryService = inventoryService;
        this.productService = productService;
        this.batchService = batchService;
        this.policyService = policyService;
        this.crateInventoryService = crateInventoryService;
        this.vehicleService = vehicleService;
        this.vehicleValidator = vehicleValidator;
        this.geocodingService = geocodingService;
    }

    public WarehouseFeasibilityResult selectFeasibleWarehouse(String destinationCity, String productId, int requiredQuantity, int requiredCrates, LocalDateTime deliveryDeadline) {

        List<String> logs = new ArrayList<>();
        double[] destCoords = geocodingService.geocode(destinationCity);

        PharmaceuticalProduct product = productService.findProductById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product " + productId + " not found."));
        TemperaturePolicy policy = policyService.findPolicyById(product.getPolicyId())
                .orElseThrow(() -> new IllegalArgumentException("Policy " + product.getPolicyId() + " not found."));

        List<Warehouse> allWarehouses = warehouseService.getAllWarehouses();
        if (allWarehouses.isEmpty()) {
            logs.add("No warehouses registered in system.");
            return new WarehouseFeasibilityResult(null, null, null, null, false, "No warehouses available.", logs);
        }

        allWarehouses.sort(Comparator.comparingDouble(wh -> CheckpointGenerator.haversineDistanceKm(wh.getLatitude(), wh.getLongitude(), destCoords[0], destCoords[1])));

        logs.add(String.format("Evaluating %d warehouses for order: Product %s, Qty: %d, Crates: %d -> %s",
            allWarehouses.size(), productId, requiredQuantity, requiredCrates, destinationCity));

        for (Warehouse wh : allWarehouses) {
            String whId = wh.getWarehouseId();
            double distKm = CheckpointGenerator.haversineDistanceKm(wh.getLatitude(), wh.getLongitude(), destCoords[0], destCoords[1]);
            logs.add(String.format("Evaluating %s (%s) - Air distance: %.1f km", whId, wh.getCity(), distKm));

            List<ProductBatch> eligibleBatches = inventoryService.getAvailableBatches(whId, productId, requiredQuantity);
            if (eligibleBatches.isEmpty()) {
                logs.add(String.format("  [Rejected] %s: Insufficient non-expired/unblocked stock of product %s (Required: %d, Available: %d)",
                        whId, productId, requiredQuantity, inventoryService.getTotalAvailableQuantity(whId, productId)));
                continue;
            }
            ProductBatch chosenBatch = eligibleBatches.get(0);

            List<Crate> compatibleCrates = crateInventoryService.findCompatibleAvailableCrates(whId, policy, requiredCrates);
            if (compatibleCrates.size() < requiredCrates) {
                logs.add(String.format("  [Rejected] %s: Insufficient compatible available crates for policy %s (Needed: %d, Found: %d)", whId, policy.getPolicyId(), requiredCrates, compatibleCrates.size()));
                continue;
            }

            List<Vehicle> availableVehicles = vehicleService.getAvailableVehiclesByWarehouse(whId);
            if (availableVehicles.isEmpty()) {
                logs.add(String.format("  [Rejected] %s: No available vehicles currently stationed at warehouse.", whId));
                continue;
            }
            Vehicle chosenVehicle = availableVehicles.get(0);

            try {
                vehicleValidator.validateVehicleForDispatch(chosenVehicle, whId, compatibleCrates);
            } catch (Exception ex) {
                logs.add(String.format("  [Rejected] %s: Vehicle %s compatibility failed: %s", whId, chosenVehicle.getVehicleId(), ex.getMessage()));
                continue;
            }

            if (deliveryDeadline != null) {
                double estTravelHours = (distKm * 1.25) / 60.0; // 60 km/h average road speed with winding factor
                long hoursToDeadline = ChronoUnit.HOURS.between(LocalDateTime.now(), deliveryDeadline);
                if (hoursToDeadline < 0 || estTravelHours > hoursToDeadline) {
                    logs.add(String.format("  [Rejected] %s: Travel duration (est. %.1f hrs) exceeds deadline window (%.1f hrs)",
                        whId, estTravelHours, (double) hoursToDeadline));
                    continue;
                }
            }

            String explanation = String.format("Selected nearest feasible warehouse %s (%s, %.1f km) with %d units stock, %d compatible crates, and available vehicle %s.",
                    whId, wh.getCity(), distKm, chosenBatch.getAvailableQuantity(), compatibleCrates.size(), chosenVehicle.getVehicleId());
            logs.add("  [SUCCESS] " + explanation);

            return new WarehouseFeasibilityResult(wh, chosenBatch, compatibleCrates, chosenVehicle, true, explanation, logs);
        }

        String failureMsg = "No feasible warehouse found satisfying stock, crate compatibility, vehicle availability, and deadline requirements.";
        logs.add("  [FAILED] " + failureMsg);
        return new WarehouseFeasibilityResult(null, null, null, null, false, failureMsg, logs);
    }
}
