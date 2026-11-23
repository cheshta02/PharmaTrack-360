package pharmatrack;

import pharmatrack.integration.TemperatureShipmentBridge;
import pharmatrack.logistics.routing.*;
import pharmatrack.logistics.service.*;
import pharmatrack.logistics.tracking.RouteSimulator;
import pharmatrack.logistics.tracking.ShipmentDelayDetector;
import pharmatrack.logistics.tracking.ShipmentTracker;
import pharmatrack.storage.DataBootstrapper;
import pharmatrack.storage.FileStorageService;
import pharmatrack.storage.InMemoryDataStore;
import pharmatrack.temperature.service.*;
import pharmatrack.temperature.simulator.SensorSimulator;
import pharmatrack.ui.LogisticsMenu;
import pharmatrack.ui.MainMenu;
import pharmatrack.ui.ReportMenu;
import pharmatrack.ui.TemperatureMenu;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        
        InMemoryDataStore dataStore = new InMemoryDataStore();
        FileStorageService fileStorageService = new FileStorageService("data");

        
        TemperaturePolicyService policyService = new TemperaturePolicyService(dataStore);
        ProductService productService = new ProductService(dataStore, policyService);
        ProductBatchService batchService = new ProductBatchService(dataStore, productService);
        CrateService crateService = new CrateService(dataStore);
        CrateInventoryService crateInventoryService = new CrateInventoryService(dataStore, crateService);
        SensorService sensorService = new SensorService(dataStore, crateService);
        SensorHealthMonitor sensorHealthMonitor = new SensorHealthMonitor(dataStore);
        ExcursionDetector excursionDetector = new ExcursionDetector(dataStore);
        TemperatureMonitor temperatureMonitor = new TemperatureMonitor(dataStore, crateService, sensorService, batchService, productService, policyService, excursionDetector);
        TemperatureStatistics temperatureStats = new TemperatureStatistics(dataStore, temperatureMonitor);
        SensorSimulator sensorSimulator = new SensorSimulator(dataStore, temperatureMonitor, sensorHealthMonitor, productService, policyService);

        WarehouseService warehouseService = new WarehouseService(dataStore);
        WarehouseInventoryService warehouseInventoryService = new WarehouseInventoryService(dataStore);
        VehicleService vehicleService = new VehicleService(dataStore);
        VehicleCompatibilityValidator vehicleValidator = new VehicleCompatibilityValidator();
        ShipmentValidator shipmentValidator = new ShipmentValidator(dataStore);
        ShipmentService shipmentService = new ShipmentService(dataStore, shipmentValidator, batchService);

        GeocodingService geocodingService = new GeocodingService(dataStore);
        RoutingApiClient routingApiClient = new RoutingApiClient();
        RouteResponseParser routeResponseParser = new RouteResponseParser();
        CheckpointGenerator checkpointGenerator = new CheckpointGenerator(30.0, 60.0);
        RouteService routeService = new RouteService(geocodingService, routingApiClient, routeResponseParser, checkpointGenerator);

        WarehouseSelectionService warehouseSelectionService = new WarehouseSelectionService( dataStore, warehouseService, warehouseInventoryService, productService, batchService, policyService, crateInventoryService, vehicleService, vehicleValidator, geocodingService);

        CrateAllocationService crateAllocationService = new CrateAllocationService(dataStore, crateService, crateInventoryService);
        VehicleAllocationService vehicleAllocationService = new VehicleAllocationService(vehicleService, vehicleValidator);
        EmergencyDispatchService emergencyDispatchService = new EmergencyDispatchService(
                dataStore, warehouseSelectionService, productService, policyService, batchService, crateAllocationService, vehicleAllocationService);
        ShipmentConsolidationService consolidationService = new ShipmentConsolidationService(vehicleValidator, crateService);
        TripService tripService = new TripService(dataStore, crateService, vehicleService, routeService, shipmentValidator);
        DeliveryService deliveryService = new DeliveryService(dataStore, crateService, batchService, vehicleService);
        CrateReturnService crateReturnService = new CrateReturnService(dataStore, crateService, vehicleService);
        ShipmentDelayDetector delayDetector = new ShipmentDelayDetector(dataStore, 15);
        RouteSimulator routeSimulator = new RouteSimulator(dataStore, deliveryService, crateReturnService, delayDetector, vehicleService);
        ShipmentTracker shipmentTracker = new ShipmentTracker(dataStore);

        
        TemperatureShipmentBridge bridge = new TemperatureShipmentBridge(dataStore, temperatureMonitor);

        
        DataBootstrapper bootstrapper = new DataBootstrapper( dataStore, warehouseService, vehicleService, policyService,
            productService, batchService, crateService, sensorService);

        java.io.File warehouseFile = new java.io.File("data/warehouses.json");
        if (warehouseFile.exists() && warehouseFile.length() > 0) {
            try {
                fileStorageService.loadAll(dataStore);
            } catch (Exception e) {
                bootstrapper.bootstrapInitialData();
            }
        } else {
            bootstrapper.bootstrapInitialData();
            try {
                fileStorageService.saveAll(dataStore);
            } catch (Exception ignored) {
            }
        }

        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            sensorSimulator.shutdown();
            routeSimulator.shutdown();
        }, "PharmaTrack-ShutdownHook"));

       
        TemperatureMenu temperatureMenu = new TemperatureMenu(dataStore, productService, policyService, batchService, crateService, sensorService, sensorSimulator, temperatureStats);
        LogisticsMenu logisticsMenu = new LogisticsMenu( dataStore, warehouseService, warehouseInventoryService, vehicleService, shipmentService,warehouseSelectionService, crateAllocationService, vehicleAllocationService,emergencyDispatchService, consolidationService, tripService, routeSimulator,shipmentTracker, crateReturnService, productService, policyService, batchService);
        ReportMenu reportMenu = new ReportMenu(  dataStore, temperatureStats, sensorHealthMonitor, warehouseService, warehouseInventoryService, shipmentTracker, bridge);

        MainMenu mainMenu = new MainMenu( dataStore, temperatureMenu, logisticsMenu, reportMenu, fileStorageService,
            warehouseService, warehouseInventoryService, vehicleService);

        try (Scanner scanner = new Scanner(System.in)) {
            mainMenu.run(scanner);
        }
    }
}