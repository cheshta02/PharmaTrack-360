package pharmatrack.ui;

import pharmatrack.common.util.InputValidator;
import pharmatrack.storage.InMemoryDataStore;
import pharmatrack.temperature.model.*;
import pharmatrack.temperature.service.*;
import pharmatrack.temperature.simulator.SensorSimulator;

import java.time.LocalDate;
import java.util.*;

public class TemperatureMenu {
    private final InMemoryDataStore dataStore;
    private final ProductService productService;
    private final TemperaturePolicyService policyService;
    private final ProductBatchService batchService;
    private final CrateService crateService;
    private final SensorService sensorService;
    private final SensorSimulator sensorSimulator;
    private final TemperatureStatistics stats;

    public TemperatureMenu(InMemoryDataStore dataStore,
                           ProductService productService,
                           TemperaturePolicyService policyService,
                           ProductBatchService batchService,
                           CrateService crateService,
                           SensorService sensorService,
                           SensorSimulator sensorSimulator,
                           TemperatureStatistics stats) {
        this.dataStore = dataStore;
        this.productService = productService;
        this.policyService = policyService;
        this.batchService = batchService;
        this.crateService = crateService;
        this.sensorService = sensorService;
        this.sensorSimulator = sensorSimulator;
        this.stats = stats;
    }

    public void display(Scanner scanner) {
        while (true) {
            System.out.println("\n========================================================");
            System.out.println("       MODULE 1: TEMPERATURE INTELLIGENCE MENU          ");
            System.out.println("========================================================");
            System.out.println(" 1. Register Pharmaceutical Product");
            System.out.println(" 2. View All Products");
            System.out.println(" 3. Register Temperature Policy");
            System.out.println(" 4. View Temperature Policies");
            System.out.println(" 5. Register Product Batch");
            System.out.println(" 6. View Product Batches");
            System.out.println(" 7. View All Crates and Validated Capabilities");
            System.out.println(" 8. Toggle Sensor Simulation (" + (sensorSimulator.isRunning() ? "RUNNING" : "STOPPED") + ")");
            System.out.println(" 9. Inject Temperature Spike / Drop (Excursion Test)");
            System.out.println(" 10. Inject Sensor Fault / Communication Timeout");
            System.out.println(" 0. Back to Main Menu");
            System.out.println("========================================================");

            int choice = InputValidator.readInt(scanner, "Select option", 0, 10);
            switch (choice) {
                case 1 -> handleRegisterProduct(scanner);
                case 2 -> handleViewProducts();
                case 3 -> handleRegisterPolicy(scanner);
                case 4 -> handleViewPolicies();
                case 5 -> handleRegisterBatch(scanner);
                case 6 -> handleViewBatches();
                case 7 -> handleViewCrates();
                case 8 -> handleToggleSimulation();
                case 9 -> handleInjectExcursion(scanner);
                case 10 -> handleInjectSensorFault(scanner);
                case 0 -> { return; }
            }
        }
    }

    private void handleRegisterProduct(Scanner scanner) {
        try {
            System.out.println("\n--- Register Pharmaceutical Product ---");
            String pId = InputValidator.readString(scanner, "Product ID (leave empty for auto-generated)", true);
            String name = InputValidator.readString(scanner, "Product Name", false);
            String category = InputValidator.readString(scanner, "Category (e.g. Vaccine, Biologics, Pharmaceutical)", false);
            String desc = InputValidator.readString(scanner, "Description", true);
            String polId = InputValidator.readString(scanner, "Applicable Policy ID (e.g. POL001)", false);

            var product = productService.registerProduct(pId.isEmpty() ? null : pId, name, category, desc, polId);
            System.out.println("  [+] Success: Product registered -> " + product);
        } catch (Exception e) {
            System.out.println("  [!] Error: " + e.getMessage());
        }
    }

    private void handleViewProducts() {
        System.out.println("\n--- Registered Pharmaceutical Products ---");
        List<PharmaceuticalProduct> list = productService.getAllProducts();
        if (list.isEmpty()) {
            System.out.println("No products registered.");
            return;
        }
        System.out.printf("%-10s %-32s %-15s %-10s %-8s\n", "ID", "Name", "Category", "PolicyID", "Active");
        System.out.println("-----------------------------------------------------------------------------");
        for (PharmaceuticalProduct p : list) {
            System.out.printf("%-10s %-32s %-15s %-10s %-8s\n", p.getProductId(), p.getName(), p.getCategory(), p.getPolicyId(), p.isActive());
        }
    }

    private void handleRegisterPolicy(Scanner scanner) {
        try {
            System.out.println("\n--- Register Temperature Policy ---");
            String polId = InputValidator.readString(scanner, "Policy ID (leave empty for auto-generated)", true);
            String name = InputValidator.readString(scanner, "Policy Name", false);
            double minTemp = InputValidator.readDouble(scanner, "Minimum Permitted Temperature (°C)");
            double maxTemp = InputValidator.readDouble(scanner, "Maximum Permitted Temperature (°C)");
            String desc = InputValidator.readString(scanner, "Reference / Description", true);

            var policy = policyService.registerPolicy(polId.isEmpty() ? null : polId, name, minTemp, maxTemp, "°C", "1.0", desc);
            System.out.println("  [+] Success: Policy registered -> " + policy);
        } catch (Exception e) {
            System.out.println("  [!] Error: " + e.getMessage());
        }
    }

    private void handleViewPolicies() {
        System.out.println("\n--- Temperature Policies ---");
        List<TemperaturePolicy> list = policyService.getAllPolicies();
        if (list.isEmpty()) {
            System.out.println("No policies registered.");
            return;
        }
        System.out.printf("%-10s %-30s %-20s %-8s %-8s\n", "ID", "Name", "Permitted Range", "Version", "Active");
        System.out.println("--------------------------------------------------------------------------------");
        for (TemperaturePolicy p : list) {
            String range = String.format("%.1f%s to %.1f%s", p.getMinTemp(), p.getUnit(), p.getMaxTemp(), p.getUnit());
            System.out.printf("%-10s %-30s %-20s %-8s %-8s\n", p.getPolicyId(), p.getName(), range, p.getVersion(), p.isActive());
        }
    }

    private void handleRegisterBatch(Scanner scanner) {
        try {
            System.out.println("\n--- Register Product Batch ---");
            String bId = InputValidator.readString(scanner, "Batch ID (leave empty for auto-generated)", true);
            String prodId = InputValidator.readString(scanner, "Product ID", false);
            int qty = InputValidator.readInt(scanner, "Total Quantity", 1, 1_000_000);
            String whId = InputValidator.readString(scanner, "Warehouse ID (e.g. WH001)", false);

            LocalDate now = LocalDate.now();
            var batch = batchService.registerBatch(bId.isEmpty() ? null : bId, prodId, now.minusMonths(1), now.plusMonths(12), qty, whId);
            System.out.println("  [+] Success: Batch registered -> " + batch);
        } catch (Exception e) {
            System.out.println("  [!] Error: " + e.getMessage());
        }
    }

    private void handleViewBatches() {
        System.out.println("\n--- Product Batches In Inventory ---");
        List<ProductBatch> list = batchService.getAllBatches();
        if (list.isEmpty()) {
            System.out.println("No batches registered.");
            return;
        }
        System.out.printf("%-10s %-10s %-8s %-12s %-10s %-10s %-12s %-8s\n",
                "BatchID", "ProductID", "WH", "Available", "Reserved", "Total", "Expiry", "Blocked");
        System.out.println("----------------------------------------------------------------------------------------");
        for (ProductBatch b : list) {
            System.out.printf("%-10s %-10s %-8s %-12d %-10d %-12d %-12s %-8s\n",
                    b.getBatchId(), b.getProductId(), b.getWarehouseId(), b.getAvailableQuantity(), b.getReservedQuantity(), 
                    b.getTotalQuantity(), b.getExpiryDate(), b.isBlocked());
        }
    }

    private void handleViewCrates() {
        System.out.println("\n" + stats.generateCratesCurrentTemperatureReport());
    }

    private void handleToggleSimulation() {
        if (sensorSimulator.isRunning()) {
            sensorSimulator.stop();
            System.out.println("Sensor simulation stopped.");
        } else {
            sensorSimulator.start(2000); // 2 second cycle
            System.out.println("Sensor simulation started (2-second interval).");
        }
    }

    private void handleInjectExcursion(Scanner scanner) {
        System.out.println("\n--- Inject Temperature Spike / Drop ---");
        String crateId = InputValidator.readString(scanner, "Target Crate ID (e.g. CR001)", false);
        Crate crate = crateService.findCrateById(crateId).orElse(null);
        if (crate == null) {
            System.out.println("  [!] Crate not found: " + crateId);
            return;
        }

        System.out.printf("Crate %s capability: %.1f°C to %.1f°C\n", crateId, crate.getMinTempCapability(), crate.getMaxTempCapability());
        System.out.println("1. Inject High Temperature Spike (Above range)");
        System.out.println("2. Inject Low Temperature Drop (Below range)");
        System.out.println("3. Reset to Normal");
        int opt = InputValidator.readInt(scanner, "Choose action", 1, 3);

        if (opt == 1) {
            double temp = InputValidator.readDouble(scanner, "Enter spike temperature (°C, e.g. 25.0)");
            sensorSimulator.injectSpike(crateId, temp);
            sensorSimulator.tick(); // Immediate tick to process reading
            System.out.printf("  [!] Temperature spike (%.2f°C) injected into Crate %s.\n", temp, crateId);
        } else if (opt == 2) {
            double temp = InputValidator.readDouble(scanner, "Enter drop temperature (°C, e.g. -95.0)");
            sensorSimulator.injectDrop(crateId, temp);
            sensorSimulator.tick();
            System.out.printf("  [!] Temperature drop (%.2f°C) injected into Crate %s.\n", temp, crateId);
        } else {
            sensorSimulator.injectNormal(crateId);
            sensorSimulator.tick();
            System.out.printf("  [*] Crate %s restored to normal nominal temperature.\n", crateId);
        }
    }

    private void handleInjectSensorFault(Scanner scanner) {
        System.out.println("\n--- Inject Sensor Fault / Timeout ---");
        String sensorId = InputValidator.readString(scanner, "Sensor ID (e.g. SEN001)", false);
        Sensor s = sensorService.findSensorById(sensorId).orElse(null);
        if (s == null) {
            System.out.println("  [!] Sensor not found: " + sensorId);
            return;
        }

        System.out.println("1. Mark FAULTY (Hardware Failure)");
        System.out.println("2. Inject Invalid Output (corrupted data)");
        System.out.println("3. Reset Sensor to ACTIVE");
        int opt = InputValidator.readInt(scanner, "Select", 1, 3);

        if (opt == 1) {
            sensorSimulator.injectSensorFailure(sensorId);
            System.out.printf("  [!] Sensor %s marked FAULTY.\n", sensorId);
        } else if (opt == 2) {
            sensorSimulator.injectInvalidOutput(sensorId);
            sensorSimulator.tick();
            System.out.printf("  [!] Invalid corrupted output injected into Sensor %s.\n", sensorId);
        } else {
            s.setStatus(SensorStatus.ACTIVE);
            System.out.printf("Sensor %s reset to ACTIVE.\n", sensorId);
        }
    }
}
