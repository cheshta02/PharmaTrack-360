package pharmatrack.logistics.model;

import pharmatrack.json.JsonCreator;
import pharmatrack.json.JsonProperty;
import pharmatrack.common.util.InputValidator;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WarehouseInventory {
    private final String warehouseId;
    private final Set<String> batchIds = ConcurrentHashMap.newKeySet();
    private final Set<String> crateIds = ConcurrentHashMap.newKeySet();
    private final Set<String> vehicleIds = ConcurrentHashMap.newKeySet();
    private final Set<String> activeTripIds = ConcurrentHashMap.newKeySet();
    private final Set<String> pendingShipmentIds = ConcurrentHashMap.newKeySet();

    @JsonCreator
    public WarehouseInventory(@JsonProperty("warehouseId") String warehouseId) {
        this.warehouseId = InputValidator.requireNonEmpty(warehouseId, "Warehouse ID");
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public Set<String> getBatchIds() {
        return Collections.unmodifiableSet(batchIds);
    }

    public void addBatch(String batchId) {
        batchIds.add(batchId);
    }

    public void removeBatch(String batchId) {
        batchIds.remove(batchId);
    }

    public Set<String> getCrateIds() {
        return Collections.unmodifiableSet(crateIds);
    }

    public void addCrate(String crateId) {
        crateIds.add(crateId);
    }

    public void removeCrate(String crateId) {
        crateIds.remove(crateId);
    }

    public Set<String> getVehicleIds() {
        return Collections.unmodifiableSet(vehicleIds);
    }

    public void addVehicle(String vehicleId) {
        vehicleIds.add(vehicleId);
    }

    public void removeVehicle(String vehicleId) {
        vehicleIds.remove(vehicleId);
    }

    public Set<String> getActiveTripIds() {
        return Collections.unmodifiableSet(activeTripIds);
    }

    public void addActiveTrip(String tripId) {
        activeTripIds.add(tripId);
    }

    public void removeActiveTrip(String tripId) {
        activeTripIds.remove(tripId);
    }

    public Set<String> getPendingShipmentIds() {
        return Collections.unmodifiableSet(pendingShipmentIds);
    }

    public void addPendingShipment(String shipmentId) {
        pendingShipmentIds.add(shipmentId);
    }

    public void removePendingShipment(String shipmentId) {
        pendingShipmentIds.remove(shipmentId);
    }

    @Override
    public String toString() {
        return String.format("Warehouse %s Inventory [Batches: %d, Crates: %d, Vehicles: %d, Trips: %d, Pending: %d]",
                warehouseId, batchIds.size(), crateIds.size(), vehicleIds.size(), activeTripIds.size(), pendingShipmentIds.size());
    }
}
