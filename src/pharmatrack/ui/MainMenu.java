package pharmatrack.ui;

import pharmatrack.common.util.InputValidator;
import pharmatrack.logistics.service.VehicleService;
import pharmatrack.logistics.service.WarehouseInventoryService;
import pharmatrack.logistics.service.WarehouseService;
import pharmatrack.storage.FileStorageService;
import pharmatrack.storage.InMemoryDataStore;

import java.io.IOException;
import java.util.Scanner;

public class MainMenu {
    private final InMemoryDataStore dataStore;
    private final TemperatureMenu temperatureMenu;
    private final LogisticsMenu logisticsMenu;
    private final ReportMenu reportMenu;
    private final FileStorageService fileStorageService;
    private final WarehouseService warehouseService;
    private final WarehouseInventoryService inventoryService;
    private final VehicleService vehicleService;

    public MainMenu(InMemoryDataStore dataStore,
                    TemperatureMenu temperatureMenu,
                    LogisticsMenu logisticsMenu,
                    ReportMenu reportMenu,
                    FileStorageService fileStorageService,
                    WarehouseService warehouseService,
                    WarehouseInventoryService inventoryService,
                    VehicleService vehicleService) {
        this.dataStore = dataStore;
        this.temperatureMenu = temperatureMenu;
        this.logisticsMenu = logisticsMenu;
        this.reportMenu = reportMenu;
        this.fileStorageService = fileStorageService;
        this.warehouseService = warehouseService;
        this.inventoryService = inventoryService;
        this.vehicleService = vehicleService;
    }

    public void run(Scanner scanner) {
        while (true) {
            System.out.println("\n========================================================");
            System.out.println("                   PHARMATRACK 360                      ");
            System.out.println("  Cold-Chain Temperature Intelligence & Logistics System");
            System.out.println("========================================================");
            System.out.println(" 1. Temperature Intelligence");
            System.out.println(" 2. Shipment & Logistics Operations");
            System.out.println(" 3. Warehouse Management & Inventories");
            System.out.println(" 4. Vehicle Fleet Management");
            System.out.println(" 5. Alerts and Reports");
            System.out.println(" 6. Save Data to JSON Files");
            System.out.println(" 7. Load Data from JSON Files");
            System.out.println(" 0. Exit System");
            System.out.println("========================================================");

            int choice = InputValidator.readInt(scanner, "Select option", 0, 9);
            switch (choice) {
                case 1 -> temperatureMenu.display(scanner);
                case 2 -> logisticsMenu.display(scanner);
                case 3 -> handleWarehouseManagement();
                case 4 -> handleVehicleManagement();
                case 5 -> reportMenu.display(scanner);
                case 6 -> handleSaveData();
                case 7 -> handleLoadData();
                case 0 -> {
                    System.out.println("\nExiting PharmaTrack 360. Goodbye.");
                    return;
                }
            }
        }
    }

    private void handleWarehouseManagement() {
        System.out.println("\n--- Regional Distribution Warehouses (Total 5) ---");
        for (var wh : warehouseService.getAllWarehouses()) {
            System.out.println(inventoryService.generateInventoryReport(wh.getWarehouseId()));
        }
    }

    private void handleVehicleManagement() {
        System.out.println("\n--- Fleet Transport Vehicles (Total 25, 5 per Warehouse) ---");
        System.out.printf("%-10s %-16s %-10s %-12s %-10s %-18s\n",
                "VehicleID", "Registration", "Home WH", "Max Crates", "Status", "Current Trip");
        System.out.println("-----------------------------------------------------------------------------------");
        for (var v : vehicleService.getAllVehicles()) {
            System.out.printf("%-10s %-16s %-10s %-12d %-10s %-18s\n",
                v.getVehicleId(), v.getRegistrationNumber(), v.getHomeWarehouseId(),
                v.getMaxCrateCapacity(), v.getStatus(),
                v.getCurrentTripId() != null ? v.getCurrentTripId() : "None");
        }
    }

    private void handleSaveData() {
        try {
            fileStorageService.saveAll(dataStore);
            System.out.println("  [+] All domain data saved successfully to ./data/ directory.");
        } catch (IOException e) {
            System.out.println("  [!] Failed to save data: " + e.getMessage());
        }
    }

    private void handleLoadData() {
        try {
            fileStorageService.loadAll(dataStore);
            System.out.println("  [+] Data successfully reloaded from ./data/ directory.");
        } catch (IOException e) {
            System.out.println("  [!] Failed to load data: " + e.getMessage());
        }
    }
}
