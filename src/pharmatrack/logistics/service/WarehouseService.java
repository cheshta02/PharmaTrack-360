package pharmatrack.logistics.service;

import pharmatrack.common.exception.ValidationException;
import pharmatrack.common.util.IdGenerator;
import pharmatrack.common.util.InputValidator;
import pharmatrack.logistics.model.Warehouse;
import pharmatrack.logistics.model.WarehouseInventory;
import pharmatrack.storage.InMemoryDataStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service managing regional warehouse facilities.
 */
public class WarehouseService {
    private final InMemoryDataStore dataStore;

    public WarehouseService(InMemoryDataStore dataStore) {
        this.dataStore = dataStore;
    }

    public Warehouse registerWarehouse(String warehouseId, String name, String city, double latitude, double longitude) {
        if (warehouseId == null || warehouseId.trim().isEmpty()) {
            warehouseId = IdGenerator.nextId("WH");
        }
        if (dataStore.getWarehouses().containsKey(warehouseId)) {
            throw new ValidationException("Warehouse with ID " + warehouseId + " already exists.");
        }
        InputValidator.requireNonEmpty(name, "Warehouse Name");
        InputValidator.requireNonEmpty(city, "City");

        Warehouse warehouse = new Warehouse(warehouseId, name, city, latitude, longitude);
        dataStore.getWarehouses().put(warehouseId, warehouse);
        dataStore.getWarehouseInventories().putIfAbsent(warehouseId, new WarehouseInventory(warehouseId));
        dataStore.getGeocodingCache().put(city.toLowerCase(), new double[]{latitude, longitude});
        return warehouse;
    }

    public Optional<Warehouse> findWarehouseById(String warehouseId) {
        if (warehouseId == null) return Optional.empty();
        return Optional.ofNullable(dataStore.getWarehouses().get(warehouseId));
    }

    public Optional<Warehouse> findWarehouseByCity(String city) {
        if (city == null) return Optional.empty();
        return dataStore.getWarehouses().values().stream()
                .filter(w -> w.getCity().equalsIgnoreCase(city.trim()))
                .findFirst();
    }

    public List<Warehouse> getAllWarehouses() {
        return new ArrayList<>(dataStore.getWarehouses().values());
    }

    /**
     * Seeds the mandatory five demonstration warehouses: Delhi, Mumbai, Kolkata, Chennai, Bengaluru.
     */
    public void seedDefaultWarehouses() {
        if (dataStore.getWarehouses().isEmpty()) {
            registerWarehouse("WH001", "Delhi Central Distribution Hub", "Delhi", 28.6139, 77.2090);
            registerWarehouse("WH002", "Mumbai Western Distribution Hub", "Mumbai", 19.0760, 72.8777);
            registerWarehouse("WH003", "Kolkata Eastern Distribution Hub", "Kolkata", 22.5726, 88.3639);
            registerWarehouse("WH004", "Chennai Southern Gateway Hub", "Chennai", 13.0827, 80.2707);
            registerWarehouse("WH005", "Bengaluru Tech & Pharma Hub", "Bengaluru", 12.9716, 77.5946);
        }
    }
}
