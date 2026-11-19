package pharmatrack.logistics.service;

import pharmatrack.common.exception.ResourceUnavailableException;
import pharmatrack.common.exception.ValidationException;
import pharmatrack.logistics.model.Vehicle;
import pharmatrack.logistics.model.VehicleStatus;
import pharmatrack.temperature.model.Crate;

import java.util.List;

/**
 * Service managing assignment of transport vehicles to prepared trips.
 */
public class VehicleAllocationService {
    private final VehicleService vehicleService;
    private final VehicleCompatibilityValidator compatibilityValidator;

    public VehicleAllocationService(VehicleService vehicleService, VehicleCompatibilityValidator compatibilityValidator) {
        this.vehicleService = vehicleService;
        this.compatibilityValidator = compatibilityValidator;
    }

    public Vehicle allocateVehicleForTrip(String sourceWarehouseId, List<Crate> assignedCrates) {
        if (sourceWarehouseId == null) {
            throw new ValidationException("Source warehouse ID cannot be null.");
        }
        if (assignedCrates == null || assignedCrates.isEmpty()) {
            throw new ValidationException("Trip must have assigned crates.");
        }
        if (assignedCrates.size() > Vehicle.MAX_CRATE_CAPACITY) {
            throw new ResourceUnavailableException(String.format(
                    "Exceeds max vehicle capacity: requested %d crates, max is %d.",
                    assignedCrates.size(), Vehicle.MAX_CRATE_CAPACITY));
        }

        List<Vehicle> available = vehicleService.getAvailableVehiclesByWarehouse(sourceWarehouseId);
        if (available.isEmpty()) {
            throw new ResourceUnavailableException("No available vehicles stationed at warehouse " + sourceWarehouseId);
        }

        for (Vehicle v : available) {
            try {
                compatibilityValidator.validateVehicleForDispatch(v, sourceWarehouseId, assignedCrates);
                v.transitionStatus(VehicleStatus.RESERVED);
                return v;
            } catch (Exception ignored) {
            }
        }

        throw new ResourceUnavailableException("No vehicle at " + sourceWarehouseId + " satisfied compatibility constraints for assigned crates.");
    }

    public void releaseVehicle(Vehicle vehicle) {
        if (vehicle != null && vehicle.getStatus() == VehicleStatus.RESERVED) {
            vehicle.transitionStatus(VehicleStatus.AVAILABLE);
        }
    }
}
