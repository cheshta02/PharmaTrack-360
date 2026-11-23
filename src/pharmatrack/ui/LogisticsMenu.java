package pharmatrack.ui;

import pharmatrack.common.util.InputValidator;
import pharmatrack.logistics.model.*;
import pharmatrack.logistics.service.*;
import pharmatrack.logistics.tracking.RouteSimulator;
import pharmatrack.logistics.tracking.ShipmentTracker;
import pharmatrack.storage.InMemoryDataStore;
import pharmatrack.temperature.model.Crate;
import pharmatrack.temperature.model.TemperaturePolicy;
import pharmatrack.temperature.service.ProductBatchService;
import pharmatrack.temperature.service.ProductService;
import pharmatrack.temperature.service.TemperaturePolicyService;

import java.time.LocalDateTime;
import java.util.*;



public class LogisticsMenu {
    private final InMemoryDataStore dataStore;
    private final WarehouseService warehouseService;
    private final WarehouseInventoryService inventoryService;
    private final VehicleService vehicleService;
    private final ShipmentService shipmentService;
    private final WarehouseSelectionService warehouseSelectionService;
    private final CrateAllocationService crateAllocationService;
    private final VehicleAllocationService vehicleAllocationService;
    private final EmergencyDispatchService emergencyDispatchService;
    private final ShipmentConsolidationService consolidationService;
    private final TripService tripService;
    private final RouteSimulator routeSimulator;
    private final ShipmentTracker shipmentTracker;
    private final CrateReturnService crateReturnService;
    private final ProductService productService;
    private final TemperaturePolicyService policyService;
    private final ProductBatchService batchService;

    public LogisticsMenu(InMemoryDataStore dataStore,
                         WarehouseService warehouseService,
                         WarehouseInventoryService inventoryService,
                         VehicleService vehicleService,
                         ShipmentService shipmentService,
                         WarehouseSelectionService warehouseSelectionService,
                         CrateAllocationService crateAllocationService,
                         VehicleAllocationService vehicleAllocationService,
                         EmergencyDispatchService emergencyDispatchService,
                         ShipmentConsolidationService consolidationService,
                         TripService tripService,
                         RouteSimulator routeSimulator,
                         ShipmentTracker shipmentTracker,
                         CrateReturnService crateReturnService,
                         ProductService productService,
                         TemperaturePolicyService policyService,
                         ProductBatchService batchService) {
        this.dataStore = dataStore;
        this.warehouseService = warehouseService;
        this.inventoryService = inventoryService;
        this.vehicleService = vehicleService;
        this.shipmentService = shipmentService;
        this.warehouseSelectionService = warehouseSelectionService;
        this.crateAllocationService = crateAllocationService;
        this.vehicleAllocationService = vehicleAllocationService;
        this.emergencyDispatchService = emergencyDispatchService;
        this.consolidationService = consolidationService;
        this.tripService = tripService;
        this.routeSimulator = routeSimulator;
        this.shipmentTracker = shipmentTracker;
        this.crateReturnService = crateReturnService;
        this.productService = productService;
        this.policyService = policyService;
        this.batchService = batchService;
    }

    public void display(Scanner scanner) {
        while (true) {
            System.out.println("\n========================================================");
            System.out.println("   MODULE 2: SHIPMENT & LOGISTICS OPERATIONS MENU       ");
            System.out.println("========================================================");
            System.out.println(" 1. View All Warehouses and Inventory Summaries");
            System.out.println(" 2. View Vehicle Fleet Availability");
            System.out.println(" 3. Register Customer Shipment Order");
            System.out.println(" 4. View Pending Shipments (Priority Queue)");
            System.out.println(" 5. Evaluate Feasible Warehouse for Shipment");
            System.out.println(" 6. Plan Emergency Dispatch");
            System.out.println(" 7. Consolidate Shipments into Vehicle Trip");
            System.out.println(" 8. Dispatch Vehicle Trip");
            System.out.println(" 9. Advance Route Simulation (Step-by-Step)");
            System.out.println(" 10. Inject Route Delay Alert");
            System.out.println(" 11. Process Crate Return & Inspection");
            System.out.println(" 0. Back to Main Menu");
            System.out.println("========================================================");

            int choice = InputValidator.readInt(scanner, "Select option", 0, 11);
            switch (choice) {
                case 1 -> handleViewWarehouses();
                case 2 -> handleViewVehicles();
                case 3 -> handleRegisterShipment(scanner);
                case 4 -> handleViewPendingShipments();
                case 5 -> handleEvaluateFeasibleWarehouse(scanner);
                case 6 -> handleEmergencyDispatch(scanner);
                case 7 -> handleConsolidation(scanner);
                case 8 -> handleDispatchTrip(scanner);
                case 9 -> handleAdvanceSimulation(scanner);
                case 10 -> handleInjectDelay(scanner);
                case 11 -> handleCrateReturn(scanner);
                case 0 -> { return; }
            }
        }
    }

    private void handleViewWarehouses() {
        System.out.println("\n--- Regional Distribution Warehouses ---");
        for (Warehouse wh : warehouseService.getAllWarehouses()) {
            System.out.println(inventoryService.generateInventoryReport(wh.getWarehouseId()));
        }
    }

    private void handleViewVehicles() {
        System.out.println("\n--- Fleet Transport Vehicles ---");
        List<Vehicle> list = vehicleService.getAllVehicles();
        System.out.printf("%-10s %-16s %-10s %-12s %-10s %-18s\n",
                "VehicleID", "Registration", "Home WH", "Max Crates", "Status", "Current Trip");
        System.out.println("-----------------------------------------------------------------------------------");
        for (Vehicle v : list) {
            System.out.printf("%-10s %-16s %-10s %-12d %-10s %-18s\n",
                    v.getVehicleId(), v.getRegistrationNumber(), v.getHomeWarehouseId(),
                    v.getMaxCrateCapacity(), v.getStatus(),
                    v.getCurrentTripId() != null ? v.getCurrentTripId() : "None");
        }
    }

    private void handleRegisterShipment(Scanner scanner) {
        try {
            System.out.println("\n--- Register Pharmaceutical Shipment Order ---");
            String shId = InputValidator.readString(scanner, "Shipment ID (leave empty for auto-generated)", true);
            String orderRef = InputValidator.readString(scanner, "Customer / Order Reference", false);
            String destCity = InputValidator.readString(scanner, "Destination City (e.g. Amritsar, Jalandhar, Pune, Jaipur)", false);
            String prodId = InputValidator.readString(scanner, "Product ID (e.g. PRD001)", false);
            int qty = InputValidator.readInt(scanner, "Required Product Quantity", 1, 100_000);
            int crates = InputValidator.readInt(scanner, "Required Crates (1-5)", 1, 5);

            System.out.println("Priority: 1. EMERGENCY  2. URGENT  3. NORMAL");
            int pChoice = InputValidator.readInt(scanner, "Select Priority", 1, 3);
            ShipmentPriority priority = switch (pChoice) {
                case 1 -> ShipmentPriority.EMERGENCY;
                case 2 -> ShipmentPriority.URGENT;
                default -> ShipmentPriority.NORMAL;
            };

            int deadlineDays = InputValidator.readInt(scanner, "Delivery Deadline Window (days from now)", 1, 30);
            LocalDateTime deadline = LocalDateTime.now().plusDays(deadlineDays);

            Shipment shipment = shipmentService.registerShipment(
                    shId.isEmpty() ? null : shId, orderRef, destCity, 0, 0,
                    prodId, null, qty, crates, priority, deadline);

            System.out.println("  [+] Success: Shipment registered -> " + shipment);
        } catch (Exception e) {
            System.out.println("  [!] Error: " + e.getMessage());
        }
    }

    private void handleViewPendingShipments() {
        System.out.println("\n--- Pending Shipments Queue (Priority Order: EMERGENCY > URGENT > NORMAL, FIFO) ---");
        List<Shipment> pending = shipmentService.getPendingShipmentsSortedByPriority();
        if (pending.isEmpty()) {
            System.out.println("No pending shipments in queue.");
            return;
        }

        System.out.printf("%-10s %-12s %-15s %-10s %-10s %-8s %-8s %-15s\n",
                "ShipmentID", "OrderRef", "Destination", "Priority", "Product", "Qty", "Crates", "Deadline");
        System.out.println("-----------------------------------------------------------------------------------------------");
        for (Shipment s : pending) {
            System.out.printf("%-10s %-12s %-15s %-10s %-10s %-8d %-8d %-15s\n",
                    s.getShipmentId(), s.getOrderRef(), s.getDestinationCity(),
                    s.getPriority(), s.getProductId(), s.getRequiredQuantity(), s.getRequiredCrates(),
                    s.getExpectedDeliveryDeadline().toLocalDate());
        }
    }

    private void handleEvaluateFeasibleWarehouse(Scanner scanner) {
        System.out.println("\n--- Evaluate Feasible Source Warehouse ---");
        String sId = InputValidator.readString(scanner, "Shipment ID", false);
        Shipment s = shipmentService.findShipmentById(sId).orElse(null);
        if (s == null) {
            System.out.println("  [!] Shipment not found: " + sId);
            return;
        }

        var result = warehouseSelectionService.selectFeasibleWarehouse(
                s.getDestinationCity(), s.getProductId(), s.getRequiredQuantity(),
                s.getRequiredCrates(), s.getExpectedDeliveryDeadline());

        System.out.println("\n--- Feasibility Evaluation Logs ---");
        for (String log : result.getEvaluationLogs()) {
            System.out.println("  " + log);
        }

        if (result.isFeasible()) {
            System.out.println("\nDo you want to allocate this warehouse and reserve stock? (1: Yes, 2: No)");
            int opt = InputValidator.readInt(scanner, "Choice", 1, 2);
            if (opt == 1) {
                s.setSourceWarehouseId(result.getSelectedWarehouse().getWarehouseId());
                s.setBatchId(result.getSelectedBatch().getBatchId());
                batchService.reserveBatchStock(result.getSelectedBatch().getBatchId(), s.getRequiredQuantity());

                var prod = productService.findProductById(s.getProductId()).get();
                var pol = policyService.findPolicyById(prod.getPolicyId()).get();
                crateAllocationService.allocateCratesForShipment(s, pol);
                s.transitionStatus(ShipmentStatus.ALLOCATED);
                System.out.println("  [+] Shipment " + sId + " successfully allocated to " + result.getSelectedWarehouse().getCity());
            }
        }
    }

    private void handleEmergencyDispatch(Scanner scanner) {
        System.out.println("\n--- Execute Emergency Dispatch ---");
        String sId = InputValidator.readString(scanner, "Emergency Shipment ID", false);
        Shipment s = shipmentService.findShipmentById(sId).orElse(null);
        if (s == null) {
            System.out.println("  [!] Shipment not found: " + sId);
            return;
        }

        var result = emergencyDispatchService.planEmergencyDispatch(s);
        System.out.println("\nResult: " + result.message);
        if (result.success) {
            System.out.println("Do you want to create and dispatch the emergency vehicle trip immediately? (1: Yes, 2: No)");
            int opt = InputValidator.readInt(scanner, "Choice", 1, 2);
            if (opt == 1) {
                VehicleTrip trip = tripService.createTrip(result.sourceWarehouse.getWarehouseId(), List.of(s), result.vehicle);
                tripService.dispatchTrip(trip.getTripId());
                System.out.println("  [+] Emergency Trip " + trip.getTripId() + " dispatched successfully!");
            }
        }
    }

    private void handleConsolidation(Scanner scanner) {
        System.out.println("\n--- Consolidate Shipments into Shared Vehicle Trip ---");
        System.out.println("Enter shipment IDs separated by comma (e.g. SH001, SH002):");
        String input = scanner.nextLine().trim();
        String[] ids = input.split(",");
        List<Shipment> candidateShipments = new ArrayList<>();
        for (String id : ids) {
            shipmentService.findShipmentById(id.trim()).ifPresent(candidateShipments::add);
        }

        if (candidateShipments.size() < 2) {
            System.out.println("  [!] Please specify at least two valid shipment IDs.");
            return;
        }

        var res = consolidationService.evaluateConsolidation(candidateShipments);
        System.out.println("\nConsolidation Evaluation: " + res.reason);

        if (res.eligible) {
            System.out.println("Do you want to allocate a vehicle and create the consolidated trip? (1: Yes, 2: No)");
            int opt = InputValidator.readInt(scanner, "Choice", 1, 2);
            if (opt == 1) {
                try {
                    List<Crate> allCrates = new ArrayList<>();
                    for (String cId : res.allCrateIds) {
                        dataStore.getCrates().computeIfPresent(cId, (k, c) -> { allCrates.add(c); return c; });
                    }
                    Vehicle vehicle = vehicleAllocationService.allocateVehicleForTrip(res.sourceWarehouseId, allCrates);
                    VehicleTrip trip = tripService.createTrip(res.sourceWarehouseId, candidateShipments, vehicle);
                    System.out.println("  [+] Consolidated Trip created successfully: " + trip.getTripId());
                } catch (Exception e) {
                    System.out.println("  [!] Error: " + e.getMessage());
                }
            }
        }
    }

    private void handleDispatchTrip(Scanner scanner) {
        System.out.println("\n--- Dispatch Prepared Vehicle Trip ---");
        String tripId = InputValidator.readString(scanner, "Trip ID (e.g. TRP001)", false);
        try {
            tripService.dispatchTrip(tripId);
            System.out.println("  [+] Trip " + tripId + " is now DISPATCHED and in transit!");
        } catch (Exception e) {
            System.out.println("  [!] Dispatch failed: " + e.getMessage());
        }
    }

    private void handleAdvanceSimulation(Scanner scanner) {
        System.out.println("\n--- Advance Route Simulation ---");
        String tripId = InputValidator.readString(scanner, "Trip ID", false);
        try {
            boolean hasNext = routeSimulator.advanceTripStep(tripId);
            System.out.println("\n" + shipmentTracker.generateTripTrackingReport(tripId));
            if (!hasNext) {
                System.out.println("  [*] Trip has reached all checkpoints and completed full delivery and vehicle return!");
            }
        } catch (Exception e) {
            System.out.println("  [!] Simulation step failed: " + e.getMessage());
        }
    }

    private void handleInjectDelay(Scanner scanner) {
        System.out.println("\n--- Inject Road Delay Alert ---");
        String tripId = InputValidator.readString(scanner, "Trip ID", false);
        long delayMins = InputValidator.readInt(scanner, "Delay duration (minutes, e.g. 45)", 1, 1000);
        routeSimulator.injectDelay(tripId, delayMins);
        System.out.printf("  [!] Injected %d minute delay on Trip %s. Will trigger on next simulation step.\n", delayMins, tripId);
    }

    private void handleCrateReturn(Scanner scanner) {
        System.out.println("\n--- Process Crate Return & Inspection ---");
        String crateId = InputValidator.readString(scanner, "Crate ID", false);
        try {
            crateReturnService.processCrateInspection(crateId);
            System.out.println("  [+] Crate " + crateId + " processed and returned to AVAILABLE status.");
        } catch (Exception e) {
            System.out.println("  [!] Return processing failed: " + e.getMessage());
        }
    }
}
