package pharmatrack.logistics.service;

import pharmatrack.common.exception.CompatibilityException;
import pharmatrack.common.exception.ValidationException;
import pharmatrack.logistics.model.Vehicle;
import pharmatrack.logistics.model.VehicleStatus;
import pharmatrack.temperature.model.Crate;

import java.util.List;

/**
 * Validates vehicle operational readiness, crate capacity (max 5), home warehouse matching,
 * and multi-temperature isolated transport capabilities.
 */
public class VehicleCompatibilityValidator {

    public void validateVehicleForDispatch(Vehicle vehicle, String sourceWarehouseId, List<Crate> assignedCrates) {
        if (vehicle == null) {
            throw new ValidationException("Vehicle cannot be null.");
        }
        if (!vehicle.getHomeWarehouseId().equals(sourceWarehouseId)) {
            throw new CompatibilityException(String.format(
                    "Vehicle %s belongs to warehouse %s, cannot operate from source warehouse %s.",
                    vehicle.getVehicleId(), vehicle.getHomeWarehouseId(), sourceWarehouseId));
        }
        if (vehicle.getStatus() != VehicleStatus.AVAILABLE && vehicle.getStatus() != VehicleStatus.RESERVED) {
            throw new CompatibilityException(String.format(
                    "Vehicle %s is not available for dispatch. Current status: %s",
                    vehicle.getVehicleId(), vehicle.getStatus()));
        }
        if (assignedCrates == null || assignedCrates.isEmpty()) {
            throw new ValidationException("Trip must have at least one assigned crate.");
        }
        if (assignedCrates.size() > Vehicle.MAX_CRATE_CAPACITY) {
            throw new CompatibilityException(String.format(
                    "Vehicle capacity exceeded: %d crates requested, but maximum capacity is %d.",
                    assignedCrates.size(), Vehicle.MAX_CRATE_CAPACITY));
        }

        // Validate multi-zone / isolated transport capabilities
        // A vehicle can transport mixed temperature ranges because it has multi-zone isolated compartments
        boolean hasMultiZone = vehicle.getSupportedCapabilities().contains("MULTI_ZONE_COLD_CHAIN") ||
                               vehicle.getSupportedCapabilities().contains("ISOLATED_COMPARTMENTS");

        if (!hasMultiZone && assignedCrates.size() > 1) {
            // If vehicle does not support multi-zone isolation, all crates would need compatible ranges
            double globalMin = assignedCrates.stream().mapToDouble(Crate::getMinTempCapability).max().orElse(0);
            double globalMax = assignedCrates.stream().mapToDouble(Crate::getMaxTempCapability).min().orElse(0);
            if (globalMin > globalMax) {
                throw new CompatibilityException(String.format(
                        "Vehicle %s lacks MULTI_ZONE_COLD_CHAIN capability to carry conflicting crate temperature ranges safely.",
                        vehicle.getVehicleId()));
            }
        }
    }
}
