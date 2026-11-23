package pharmatrack.ui;

import pharmatrack.common.util.InputValidator;
import pharmatrack.integration.TemperatureShipmentBridge;
import pharmatrack.logistics.model.ShipmentEvent;
import pharmatrack.logistics.model.VehicleTrip;
import pharmatrack.logistics.model.Warehouse;
import pharmatrack.logistics.service.WarehouseInventoryService;
import pharmatrack.logistics.service.WarehouseService;
import pharmatrack.logistics.tracking.ShipmentTracker;
import pharmatrack.storage.InMemoryDataStore;
import pharmatrack.temperature.service.SensorHealthMonitor;
import pharmatrack.temperature.service.TemperatureStatistics;

import java.util.*;

public class ReportMenu {
    private final InMemoryDataStore dataStore;
    private final TemperatureStatistics temperatureStats;
    private final SensorHealthMonitor sensorHealthMonitor;
    private final WarehouseService warehouseService;
    private final WarehouseInventoryService inventoryService;
    private final ShipmentTracker shipmentTracker;
    private final TemperatureShipmentBridge bridge;

    public ReportMenu(InMemoryDataStore dataStore,
                      TemperatureStatistics temperatureStats,
                      SensorHealthMonitor sensorHealthMonitor,
                      WarehouseService warehouseService,
                      WarehouseInventoryService inventoryService,
                      ShipmentTracker shipmentTracker,
                      TemperatureShipmentBridge bridge) {
        this.dataStore = dataStore;
        this.temperatureStats = temperatureStats;
        this.sensorHealthMonitor = sensorHealthMonitor;
        this.warehouseService = warehouseService;
        this.inventoryService = inventoryService;
        this.shipmentTracker = shipmentTracker;
        this.bridge = bridge;
    }

    public void display(Scanner scanner) {
        while (true) {
            System.out.println("\n========================================================");
            System.out.println("               ALERTS & REPORTS MENU                    ");
            System.out.println("========================================================");
            System.out.println(" 1. Crates Cold-Chain Status Report");
            System.out.println(" 2. Temperature Excursions History");
            System.out.println(" 3. Sensor Health Report");
            System.out.println(" 4. Warehouse Inventory Status");
            System.out.println(" 5. Active Vehicle Trips & Route Tracking");
            System.out.println(" 6. Integrated Shipment Cold-Chain Alerts");
            System.out.println(" 7. Complete System Audit Events Stream");
            System.out.println(" 0. Back to Main Menu");
            System.out.println("========================================================");

            int choice = InputValidator.readInt(scanner, "Select option", 0, 7);
            switch (choice) {
                case 1 -> System.out.println("\n" + temperatureStats.generateCratesCurrentTemperatureReport());
                case 2 -> System.out.println("\n" + temperatureStats.generateExcursionSummaryReport());
                case 3 -> System.out.println("\n" + sensorHealthMonitor.generateHealthReport());
                case 4 -> handleWarehouseReports();
                case 5 -> handleTripReports(scanner);
                case 6 -> System.out.println("\n" + bridge.generateAlertsSummary());
                case 7 -> handleAuditStream();
                case 0 -> { return; }
            }
        }
    }

    private void handleWarehouseReports() {
        for (Warehouse wh : warehouseService.getAllWarehouses()) {
            System.out.println("\n" + inventoryService.generateInventoryReport(wh.getWarehouseId()));
        }
    }

    private void handleTripReports(Scanner scanner) {
        List<VehicleTrip> trips = new java.util.ArrayList<>(dataStore.getVehicleTrips().values());
        if (trips.isEmpty()) {
            System.out.println("No trips currently registered.");
            return;
        }

        System.out.println("\nAvailable Trips: " + trips.stream().map(VehicleTrip::getTripId).toList());
        String tripId = InputValidator.readString(scanner, "Enter Trip ID to view tracking", false);
        System.out.println("\n" + shipmentTracker.generateTripTrackingReport(tripId));
    }

    private void handleAuditStream() {
        System.out.println("\n=== System Audit Events Log ===");
        List<ShipmentEvent> events = dataStore.getEvents();
        if (events.isEmpty()) {
            System.out.println("No events logged yet.");
            return;
        }
        for (ShipmentEvent evt : events) {
            System.out.println("  " + evt);
        }
    }
}
