package pharmatrack.logistics.service;

import pharmatrack.common.exception.ValidationException;
import pharmatrack.common.util.IdGenerator;
import pharmatrack.common.util.InputValidator;
import pharmatrack.logistics.model.Vehicle;
import pharmatrack.logistics.model.VehicleStatus;
import pharmatrack.storage.InMemoryDataStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service managing the fleet of pharmaceutical transport vehicles across warehouses.
 */
public class VehicleService {
    private final InMemoryDataStore dataStore;

    public VehicleService(InMemoryDataStore dataStore) {
        this.dataStore = dataStore;
    }

    public Vehicle registerVehicle(String vehicleId, String regNumber, String homeWarehouseId) {
        if (vehicleId == null || vehicleId.trim().isEmpty()) {
            vehicleId = IdGenerator.nextId("VEH");
        }
        if (dataStore.getVehicles().containsKey(vehicleId)) {
            throw new ValidationException("Vehicle with ID " + vehicleId + " already exists.");
        }
        InputValidator.requireNonEmpty(regNumber, "Registration Number");
        InputValidator.requireNonEmpty(homeWarehouseId, "Home Warehouse ID");

        Vehicle vehicle = new Vehicle(vehicleId, regNumber, homeWarehouseId);
        dataStore.getVehicles().put(vehicleId, vehicle);

        if (dataStore.getWarehouseInventories().containsKey(homeWarehouseId)) {
            dataStore.getWarehouseInventories().get(homeWarehouseId).addVehicle(vehicleId);
        }

        return vehicle;
    }

    public Optional<Vehicle> findVehicleById(String vehicleId) {
        if (vehicleId == null) return Optional.empty();
        return Optional.ofNullable(dataStore.getVehicles().get(vehicleId));
    }

    public List<Vehicle> getAllVehicles() {
        return new ArrayList<>(dataStore.getVehicles().values());
    }

    public List<Vehicle> getVehiclesByWarehouse(String warehouseId) {
        return dataStore.getVehicles().values().stream()
                .filter(v -> v.getHomeWarehouseId().equals(warehouseId))
                .toList();
    }

    public List<Vehicle> getAvailableVehiclesByWarehouse(String warehouseId) {
        return dataStore.getVehicles().values().stream()
                .filter(v -> v.getHomeWarehouseId().equals(warehouseId) && v.getStatus() == VehicleStatus.AVAILABLE)
                .toList();
    }

    public void updateVehicleStatus(String vehicleId, VehicleStatus status) {
        Vehicle vehicle = findVehicleById(vehicleId)
                .orElseThrow(() -> new ValidationException("Vehicle " + vehicleId + " not found."));
        vehicle.transitionStatus(status);
    }

    /**
     * Seeds the initial fleet: Exactly 5 vehicles per warehouse across the 5 demonstration warehouses (25 total).
     */
    public void seedDefaultFleet() {
        if (dataStore.getVehicles().isEmpty()) {
            String[] whIds = {"WH001", "WH002", "WH003", "WH004", "WH005"};
            String[] prefixes = {"DL-01-PT-", "MH-02-PT-", "WB-03-PT-", "TN-04-PT-", "KA-05-PT-"};
            int vehIndex = 1;

            for (int i = 0; i < whIds.length; i++) {
                String whId = whIds[i];
                String regPrefix = prefixes[i];
                for (int j = 1; j <= 5; j++) {
                    String vId = String.format("VEH%03d", vehIndex);
                    String reg = String.format("%s%04d", regPrefix, 1000 + j);
                    registerVehicle(vId, reg, whId);
                    vehIndex++;
                }
            }
        }
    }
}
