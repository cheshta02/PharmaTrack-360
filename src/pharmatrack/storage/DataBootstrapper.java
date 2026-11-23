package pharmatrack.storage;

import pharmatrack.logistics.service.VehicleService;
import pharmatrack.logistics.service.WarehouseService;
import pharmatrack.temperature.model.*;
import pharmatrack.temperature.service.*;

import java.time.LocalDate;
public class DataBootstrapper {
    private final InMemoryDataStore dataStore;
    private final WarehouseService warehouseService;
    private final VehicleService vehicleService;
    private final TemperaturePolicyService policyService;
    private final ProductService productService;
    private final ProductBatchService batchService;
    private final CrateService crateService;
    private final SensorService sensorService;

    public DataBootstrapper(InMemoryDataStore dataStore, WarehouseService warehouseService, VehicleService vehicleService, TemperaturePolicyService policyService, ProductService productService, ProductBatchService batchService, CrateService crateService, SensorService sensorService) {
        this.dataStore = dataStore;
        this.warehouseService = warehouseService;
        this.vehicleService = vehicleService;
        this.policyService = policyService;
        this.productService = productService;
        this.batchService = batchService;
        this.crateService = crateService;
        this.sensorService = sensorService;
    }

    public void bootstrapInitialData() {
        warehouseService.seedDefaultWarehouses();
        vehicleService.seedDefaultFleet();
        seedPolicies();
        seedProducts();
        seedCratesAndSensors();
        seedBatches();
    }

    private void seedPolicies() {
        if (dataStore.getPolicies().isEmpty()) {
            policyService.registerPolicy("POL001", "Ultra-Low Deep Freeze", -80.0, -60.0, "°C", "1.0", "Required for specialized mRNA vaccines");
            policyService.registerPolicy("POL002", "Refrigerated Cold Chain", 2.0, 8.0, "°C", "1.0", "Standard 2°C to 8°C pharmaceutical cold chain");
            policyService.registerPolicy("POL003", "Frozen Biologics Storage", -20.0, -15.0, "°C", "1.0", "Sub-zero frozen pharmaceutical storage");
            policyService.registerPolicy("POL004", "Controlled Room Temperature", 15.0, 25.0, "°C", "1.0", "Regulated ambient storage");
        }
    }

    private void seedProducts() {
        if (dataStore.getProducts().isEmpty()) {
            productService.registerProduct("PRD001", "mRNA COVID-19 Vaccine", "Vaccine", "Ultra-low temperature mRNA formulation", "POL001");
            productService.registerProduct("PRD002", "Insulin Glargine Solostar", "Biologics", "Long-acting human insulin analog", "POL002");
            productService.registerProduct("PRD003", "Varicella Zoster Vaccine", "Vaccine", "Live attenuated frozen virus vaccine", "POL003");
            productService.registerProduct("PRD004", "Monoclonal Antibodies mAb-21", "Biologics", "Therapeutic monoclonal antibody vials", "POL002");
            productService.registerProduct("PRD005", "Amoxicillin Oral Suspension", "Pharmaceutical", "Liquid antibiotic formulation", "POL004");
        }
    }

    private void seedCratesAndSensors() {
        if (dataStore.getCrates().isEmpty()) {
            String[] whIds = {"WH001", "WH002", "WH003", "WH004", "WH005"};
            int crateCounter = 1;

            for (String whId : whIds) {
                for (int i = 0; i < 5; i++) {
                    createCrateWithSensor(crateCounter++, -90.0, -50.0, 50.0, whId, "Ultra-Low Cryo-Crate");
                }
                for (int i = 0; i < 10; i++) {
                    createCrateWithSensor(crateCounter++, 0.0, 12.0, 60.0, whId, "Standard Cold-Box");
                }
                for (int i = 0; i < 5; i++) {
                    createCrateWithSensor(crateCounter++, -25.0, -10.0, 55.0, whId, "Sub-Zero Freeze-Crate");
                }
                for (int i = 0; i < 5; i++) {
                    createCrateWithSensor(crateCounter++, 10.0, 30.0, 70.0, whId, "Controlled-Ambient Crate");
                }
            }
        }
    }

    private void createCrateWithSensor(int num, double minCap, double maxCap, double capacityLiters, String whId, String type) {
        String crateId = String.format("CR%03d", num);
        String sensorId = String.format("SEN%03d", num);

        crateService.registerCrate(crateId, minCap, maxCap, capacityLiters, whId);
        sensorService.registerSensor(sensorId, crateId, "IoT-" + type, 60);
    }

    private void seedBatches() {
        if (dataStore.getBatches().isEmpty()) {
            LocalDate now = LocalDate.now();
            LocalDate mfg = now.minusMonths(2);
            LocalDate exp1 = now.plusMonths(10);
            LocalDate exp2 = now.plusMonths(18);
            batchService.registerBatch("BAT001", "PRD001", mfg, exp1, 5000, "WH001"); 
            batchService.registerBatch("BAT002", "PRD002", mfg, exp2, 12000, "WH001"); 
            batchService.registerBatch("BAT003", "PRD003", mfg, exp1, 4000, "WH001"); 
            batchService.registerBatch("BAT004", "PRD004", mfg, exp2, 6000, "WH001"); 
            batchService.registerBatch("BAT005", "PRD005", mfg, exp2, 15000, "WH001"); 
            batchService.registerBatch("BAT006", "PRD001", mfg, exp1, 4500, "WH002");
            batchService.registerBatch("BAT007", "PRD002", mfg, exp2, 10000, "WH002");
            batchService.registerBatch("BAT008", "PRD004", mfg, exp2, 8000, "WH002");
            batchService.registerBatch("BAT009", "PRD002", mfg, exp2, 7000, "WH003");
            batchService.registerBatch("BAT010", "PRD005", mfg, exp2, 9000, "WH003");
            batchService.registerBatch("BAT011", "PRD001", mfg, exp1, 3000, "WH004");
            batchService.registerBatch("BAT012", "PRD002", mfg, exp2, 11000, "WH004");
            batchService.registerBatch("BAT013", "PRD003", mfg, exp1, 3500, "WH004");
            batchService.registerBatch("BAT014", "PRD001", mfg, exp1, 6000, "WH005");
            batchService.registerBatch("BAT015", "PRD002", mfg, exp2, 15000, "WH005");
            batchService.registerBatch("BAT016", "PRD004", mfg, exp2, 12000, "WH005");
            batchService.registerBatch("BAT017", "PRD005", mfg, exp2, 20000, "WH005");
        }
    }
}
