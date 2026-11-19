package pharmatrack.logistics.service;

import pharmatrack.common.exception.ResourceUnavailableException;
import pharmatrack.logistics.model.*;
import pharmatrack.storage.InMemoryDataStore;
import pharmatrack.temperature.model.Crate;
import pharmatrack.temperature.model.ProductBatch;
import pharmatrack.temperature.model.TemperaturePolicy;
import pharmatrack.temperature.service.ProductBatchService;
import pharmatrack.temperature.service.ProductService;
import pharmatrack.temperature.service.TemperaturePolicyService;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles expedited emergency dispatch allocations, ensuring emergency shipments receive top resource priority
 * without violating physical safety constraints or pre-empting in-transit vehicles.
 */
public class EmergencyDispatchService {
    private final InMemoryDataStore dataStore;
    private final WarehouseSelectionService warehouseSelectionService;
    private final ProductService productService;
    private final TemperaturePolicyService policyService;
    private final ProductBatchService batchService;
    private final CrateAllocationService crateAllocationService;
    private final VehicleAllocationService vehicleAllocationService;

    public EmergencyDispatchService(InMemoryDataStore dataStore,
                                    WarehouseSelectionService warehouseSelectionService,
                                    ProductService productService,
                                    TemperaturePolicyService policyService,
                                    ProductBatchService batchService,
                                    CrateAllocationService crateAllocationService,
                                    VehicleAllocationService vehicleAllocationService) {
        this.dataStore = dataStore;
        this.warehouseSelectionService = warehouseSelectionService;
        this.productService = productService;
        this.policyService = policyService;
        this.batchService = batchService;
        this.crateAllocationService = crateAllocationService;
        this.vehicleAllocationService = vehicleAllocationService;
    }

    public static class EmergencyDispatchResult {
        public final boolean success;
        public final Shipment shipment;
        public final Warehouse sourceWarehouse;
        public final ProductBatch batch;
        public final List<Crate> crates;
        public final Vehicle vehicle;
        public final String message;

        public EmergencyDispatchResult(boolean success, Shipment shipment, Warehouse sourceWarehouse,
                                       ProductBatch batch, List<Crate> crates, Vehicle vehicle, String message) {
            this.success = success;
            this.shipment = shipment;
            this.sourceWarehouse = sourceWarehouse;
            this.batch = batch;
            this.crates = crates != null ? crates : new ArrayList<>();
            this.vehicle = vehicle;
            this.message = message;
        }
    }

    public EmergencyDispatchResult planEmergencyDispatch(Shipment shipment) {
        if (shipment == null) {
            return new EmergencyDispatchResult(false, null, null, null, null, null, "Shipment cannot be null.");
        }
        if (shipment.getPriority() != ShipmentPriority.EMERGENCY) {
            return new EmergencyDispatchResult(false, shipment, null, null, null, null, "Shipment is not marked EMERGENCY priority.");
        }

        // 1. Evaluate warehouse feasibility
        WarehouseSelectionService.WarehouseFeasibilityResult eval = warehouseSelectionService.selectFeasibleWarehouse(
                shipment.getDestinationCity(), shipment.getProductId(), shipment.getRequiredQuantity(),
                shipment.getRequiredCrates(), shipment.getExpectedDeliveryDeadline());

        if (!eval.isFeasible()) {
            // Retain shipment in pending queue with shortage alert
            String alert = "[EMERGENCY ALERT] Resource shortage for Emergency Shipment " + shipment.getShipmentId() +
                           ": " + eval.getExplanation();
            return new EmergencyDispatchResult(false, shipment, null, null, null, null, alert);
        }

        Warehouse sourceWh = eval.getSelectedWarehouse();
        ProductBatch batch = eval.getSelectedBatch();
        shipment.setSourceWarehouseId(sourceWh.getWarehouseId());
        shipment.setBatchId(batch.getBatchId());

        // 2. Reserve stock
        try {
            batchService.reserveBatchStock(batch.getBatchId(), shipment.getRequiredQuantity());
        } catch (Exception ex) {
            return new EmergencyDispatchResult(false, shipment, sourceWh, batch, null, null,
                    "[EMERGENCY ALERT] Failed to reserve batch stock: " + ex.getMessage());
        }

        // 3. Allocate crates
        TemperaturePolicy policy = policyService.findPolicyById(
                productService.findProductById(shipment.getProductId()).get().getPolicyId()).get();

        List<Crate> allocatedCrates;
        try {
            allocatedCrates = crateAllocationService.allocateCratesForShipment(shipment, policy);
        } catch (Exception ex) {
            batchService.releaseBatchStock(batch.getBatchId(), shipment.getRequiredQuantity());
            return new EmergencyDispatchResult(false, shipment, sourceWh, batch, null, null,
                    "[EMERGENCY ALERT] Failed to allocate compatible crates: " + ex.getMessage());
        }

        // 4. Allocate vehicle
        Vehicle vehicle;
        try {
            vehicle = vehicleAllocationService.allocateVehicleForTrip(sourceWh.getWarehouseId(), allocatedCrates);
        } catch (Exception ex) {
            crateAllocationService.releaseAllocatedCrates(shipment);
            batchService.releaseBatchStock(batch.getBatchId(), shipment.getRequiredQuantity());
            return new EmergencyDispatchResult(false, shipment, sourceWh, batch, allocatedCrates, null,
                    "[EMERGENCY ALERT] No available vehicle at " + sourceWh.getWarehouseId() + ": " + ex.getMessage());
        }

        shipment.transitionStatus(ShipmentStatus.ALLOCATED);
        String successMsg = String.format("Emergency shipment %s successfully allocated resources at %s with Vehicle %s and %d crates.",
                shipment.getShipmentId(), sourceWh.getCity(), vehicle.getVehicleId(), allocatedCrates.size());

        return new EmergencyDispatchResult(true, shipment, sourceWh, batch, allocatedCrates, vehicle, successMsg);
    }
}
