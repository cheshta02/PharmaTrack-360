package pharmatrack.storage;

import pharmatrack.logistics.model.*;
import pharmatrack.temperature.model.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryDataStore {
    private final Map<String, PharmaceuticalProduct> products = new ConcurrentHashMap<>();
    private final Map<String, TemperaturePolicy> policies = new ConcurrentHashMap<>();
    private final Map<String, ProductBatch> batches = new ConcurrentHashMap<>();
    private final Map<String, Crate> crates = new ConcurrentHashMap<>();
    private final Map<String, Sensor> sensors = new ConcurrentHashMap<>();
    private final List<TemperatureReading> readings = new CopyOnWriteArrayList<>();
    private final List<TemperatureExcursion> excursions = new CopyOnWriteArrayList<>();

    private final Map<String, Warehouse> warehouses = new ConcurrentHashMap<>();
    private final Map<String, WarehouseInventory> warehouseInventories = new ConcurrentHashMap<>();
    private final Map<String, Vehicle> vehicles = new ConcurrentHashMap<>();
    private final Map<String, Shipment> shipments = new ConcurrentHashMap<>();
    private final Map<String, VehicleTrip> vehicleTrips = new ConcurrentHashMap<>();
    private final List<ShipmentEvent> events = new CopyOnWriteArrayList<>();
    private final Map<String, double[]> geocodingCache = new ConcurrentHashMap<>();

    public InMemoryDataStore() {
    }

    public Map<String, PharmaceuticalProduct> getProducts() {
        return products;
    }

    public Map<String, TemperaturePolicy> getPolicies() {
        return policies;
    }

    public Map<String, ProductBatch> getBatches() {
        return batches;
    }

    public Map<String, Crate> getCrates() {
        return crates;
    }

    public Map<String, Sensor> getSensors() {
        return sensors;
    }

    public List<TemperatureReading> getReadings() {
        return readings;
    }

    public List<TemperatureExcursion> getExcursions() {
        return excursions;
    }

    public Map<String, Warehouse> getWarehouses() {
        return warehouses;
    }

    public Map<String, WarehouseInventory> getWarehouseInventories() {
        return warehouseInventories;
    }

    public Map<String, Vehicle> getVehicles() {
        return vehicles;
    }

    public Map<String, Shipment> getShipments() {
        return shipments;
    }

    public Map<String, VehicleTrip> getVehicleTrips() {
        return vehicleTrips;
    }

    public List<ShipmentEvent> getEvents() {
        return events;
    }

    public Map<String, double[]> getGeocodingCache() {
        return geocodingCache;
    }

    public void addEvent(ShipmentEvent event) {
        if (event != null) {
            events.add(event);
        }
    }

    public void clear() {
        products.clear();
        policies.clear();
        batches.clear();
        crates.clear();
        sensors.clear();
        readings.clear();
        excursions.clear();
        warehouses.clear();
        warehouseInventories.clear();
        vehicles.clear();
        shipments.clear();
        vehicleTrips.clear();
        events.clear();
    }
}
