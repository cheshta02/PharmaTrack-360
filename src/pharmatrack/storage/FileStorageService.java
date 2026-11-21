package pharmatrack.storage;

import pharmatrack.json.TypeReference;
import pharmatrack.json.ObjectMapper;
import pharmatrack.json.SerializationFeature;
import pharmatrack.json.JavaTimeModule;
import pharmatrack.logistics.model.*;
import pharmatrack.temperature.model.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class FileStorageService {
    private final String dataDirectory;
    private final ObjectMapper objectMapper;

    public FileStorageService(String dataDirectory) {
        this.dataDirectory = dataDirectory != null ? dataDirectory : "data";
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        try {
            Files.createDirectories(Paths.get(this.dataDirectory));
        } catch (IOException ignored) {
        }
    }

    public FileStorageService() {
        this("data");
    }

    public void saveAll(InMemoryDataStore store) throws IOException {
        saveList("policies.json", new ArrayList<>(store.getPolicies().values()));
        saveList("products.json", new ArrayList<>(store.getProducts().values()));
        saveList("batches.json", new ArrayList<>(store.getBatches().values()));
        saveList("crates.json", new ArrayList<>(store.getCrates().values()));
        saveList("sensors.json", new ArrayList<>(store.getSensors().values()));
        saveList("warehouses.json", new ArrayList<>(store.getWarehouses().values()));
        saveList("vehicles.json", new ArrayList<>(store.getVehicles().values()));
        saveList("shipments.json", new ArrayList<>(store.getShipments().values()));
        saveList("trips.json", new ArrayList<>(store.getVehicleTrips().values()));
        saveList("readings.json", new ArrayList<>(store.getReadings()));
        saveList("excursions.json", new ArrayList<>(store.getExcursions()));
        saveList("events.json", new ArrayList<>(store.getEvents()));
    }

    public void loadAll(InMemoryDataStore store) throws IOException {
        store.clear();

        List<TemperaturePolicy> policies = loadList("policies.json", new TypeReference<>() {});
        if (policies != null) {
            policies.forEach(p -> store.getPolicies().put(p.getPolicyId(), p));
        }

        List<PharmaceuticalProduct> products = loadList("products.json", new TypeReference<>() {});
        if (products != null) {
            products.forEach(p -> store.getProducts().put(p.getProductId(), p));
        }

        List<ProductBatch> batches = loadList("batches.json", new TypeReference<>() {});
        if (batches != null) {
            batches.forEach(b -> store.getBatches().put(b.getBatchId(), b));
        }

        List<Crate> crates = loadList("crates.json", new TypeReference<>() {});
        if (crates != null) {
            crates.forEach(c -> store.getCrates().put(c.getCrateId(), c));
        }

        List<Sensor> sensors = loadList("sensors.json", new TypeReference<>() {});
        if (sensors != null) {
            sensors.forEach(s -> store.getSensors().put(s.getSensorId(), s));
        }

        List<Warehouse> warehouses = loadList("warehouses.json", new TypeReference<>() {});
        if (warehouses != null) {
            warehouses.forEach(w -> {
                store.getWarehouses().put(w.getWarehouseId(), w);
                store.getWarehouseInventories().computeIfAbsent(w.getWarehouseId(), WarehouseInventory::new);
                store.getGeocodingCache().put(w.getCity().toLowerCase(), new double[]{w.getLatitude(), w.getLongitude()});
            });
        }

        List<Vehicle> vehicles = loadList("vehicles.json", new TypeReference<>() {});
        if (vehicles != null) {
            vehicles.forEach(v -> {
                store.getVehicles().put(v.getVehicleId(), v);
                if (v.getHomeWarehouseId() != null) {
                    store.getWarehouseInventories()
                            .computeIfAbsent(v.getHomeWarehouseId(), WarehouseInventory::new)
                            .addVehicle(v.getVehicleId());
                }
            });
        }
        for (ProductBatch b : store.getBatches().values()) {
            if (b.getWarehouseId() != null) {
                store.getWarehouseInventories()
                        .computeIfAbsent(b.getWarehouseId(), WarehouseInventory::new)
                        .addBatch(b.getBatchId());
            }
        }
        for (Crate c : store.getCrates().values()) {
            if (c.getWarehouseId() != null) {
                store.getWarehouseInventories()
                        .computeIfAbsent(c.getWarehouseId(), WarehouseInventory::new)
                        .addCrate(c.getCrateId());
            }
        }

        List<Shipment> shipments = loadList("shipments.json", new TypeReference<>() {});
        if (shipments != null) {
            shipments.forEach(s -> {
                store.getShipments().put(s.getShipmentId(), s);
                if (s.getStatus() == ShipmentStatus.PENDING && s.getSourceWarehouseId() != null) {
                    store.getWarehouseInventories()
                            .computeIfAbsent(s.getSourceWarehouseId(), WarehouseInventory::new)
                            .addPendingShipment(s.getShipmentId());
                }
            });
        }

        List<VehicleTrip> trips = loadList("trips.json", new TypeReference<>() {});
        if (trips != null) {
            trips.forEach(t -> {
                store.getVehicleTrips().put(t.getTripId(), t);
                if (t.getStatus() == TripStatus.PLANNED || t.getStatus() == TripStatus.DISPATCHED || t.getStatus() == TripStatus.IN_TRANSIT) {
                    if (t.getSourceWarehouseId() != null) {
                        store.getWarehouseInventories()
                                .computeIfAbsent(t.getSourceWarehouseId(), WarehouseInventory::new)
                                .addActiveTrip(t.getTripId());
                    }
                }
            });
        }

        List<TemperatureReading> readings = loadList("readings.json", new TypeReference<>() {});
        if (readings != null) {
            store.getReadings().addAll(readings);
        }

        List<TemperatureExcursion> excursions = loadList("excursions.json", new TypeReference<>() {});
        if (excursions != null) {
            store.getExcursions().addAll(excursions);
        }

        List<ShipmentEvent> events = loadList("events.json", new TypeReference<>() {});
        if (events != null) {
            events.forEach(store::addEvent);
        }
    }

    private <T> void saveList(String fileName, List<T> list) throws IOException {
        Path filePath = Paths.get(dataDirectory, fileName);
        objectMapper.writeValue(filePath.toFile(), list);
    }

    private <T> List<T> loadList(String fileName, TypeReference<List<T>> typeReference) {
        File file = Paths.get(dataDirectory, fileName).toFile();
        if (!file.exists() || file.length() == 0) {
            return null;
        }
        try {
            return objectMapper.readValue(file, typeReference);
        } catch (IOException e) {
            System.err.println("Warning: Could not load " + fileName + ": " + e.getMessage());
            return null;
        }
    }
}
