package pharmatrack.logistics.model;

import pharmatrack.json.JsonCreator;
import pharmatrack.json.JsonProperty;
import pharmatrack.common.exception.InvalidStateTransitionException;
import pharmatrack.common.util.InputValidator;

import java.util.*;

public class Vehicle {
    public static final int MAX_CRATE_CAPACITY = 5;

    private final String vehicleId;
    private final String registrationNumber;
    private final String homeWarehouseId;
    private final int maxCrateCapacity;
    private VehicleStatus status;
    private Set<String> supportedCapabilities;
    private String currentTripId;
    private String lastKnownLocation;

    @JsonCreator
    public Vehicle(
            @JsonProperty("vehicleId") String vehicleId,
            @JsonProperty("registrationNumber") String registrationNumber,
            @JsonProperty("homeWarehouseId") String homeWarehouseId,
            @JsonProperty("maxCrateCapacity") int maxCrateCapacity,
            @JsonProperty("status") VehicleStatus status,
            @JsonProperty("supportedCapabilities") Set<String> supportedCapabilities,
            @JsonProperty("currentTripId") String currentTripId,
            @JsonProperty("lastKnownLocation") String lastKnownLocation) {
        this.vehicleId = InputValidator.requireNonEmpty(vehicleId, "Vehicle ID");
        this.registrationNumber = InputValidator.requireNonEmpty(registrationNumber, "Registration Number");
        this.homeWarehouseId = InputValidator.requireNonEmpty(homeWarehouseId, "Home Warehouse ID");
        this.maxCrateCapacity = maxCrateCapacity > 0 ? maxCrateCapacity : MAX_CRATE_CAPACITY;
        this.status = status != null ? status : VehicleStatus.AVAILABLE;
        this.supportedCapabilities = supportedCapabilities != null ? new HashSet<>(supportedCapabilities) : new HashSet<>();
        if (this.supportedCapabilities.isEmpty()) {
            this.supportedCapabilities.add("MULTI_ZONE_COLD_CHAIN");
            this.supportedCapabilities.add("ISOLATED_COMPARTMENTS");
        }
        this.currentTripId = currentTripId;
        this.lastKnownLocation = lastKnownLocation != null ? lastKnownLocation : homeWarehouseId;
    }

    public Vehicle(String vehicleId, String registrationNumber, String homeWarehouseId) {
        this(vehicleId, registrationNumber, homeWarehouseId, MAX_CRATE_CAPACITY, VehicleStatus.AVAILABLE, null, null, homeWarehouseId);
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public String getHomeWarehouseId() {
        return homeWarehouseId;
    }

    public int getMaxCrateCapacity() {
        return maxCrateCapacity;
    }

    public VehicleStatus getStatus() {
        return status;
    }

    public Set<String> getSupportedCapabilities() {
        return Collections.unmodifiableSet(supportedCapabilities);
    }

    public void addSupportedCapability(String capability) {
        supportedCapabilities.add(capability);
    }

    public String getCurrentTripId() {
        return currentTripId;
    }

    public void setCurrentTripId(String currentTripId) {
        this.currentTripId = currentTripId;
    }

    public String getLastKnownLocation() {
        return lastKnownLocation;
    }

    public void setLastKnownLocation(String lastKnownLocation) {
        this.lastKnownLocation = lastKnownLocation;
    }

    public synchronized void transitionStatus(VehicleStatus newStatus) {
        if (this.status == newStatus) {
            return;
        }

        boolean valid = switch (this.status) {
            case AVAILABLE -> newStatus == VehicleStatus.RESERVED || newStatus == VehicleStatus.LOADING || newStatus == VehicleStatus.MAINTENANCE;
            case RESERVED -> newStatus == VehicleStatus.LOADING || newStatus == VehicleStatus.AVAILABLE; // Cancellation release
            case LOADING -> newStatus == VehicleStatus.IN_TRANSIT || newStatus == VehicleStatus.AVAILABLE;
            case IN_TRANSIT -> newStatus == VehicleStatus.RETURNING;
            case RETURNING -> newStatus == VehicleStatus.AVAILABLE || newStatus == VehicleStatus.MAINTENANCE;
            case MAINTENANCE -> newStatus == VehicleStatus.AVAILABLE || newStatus == VehicleStatus.UNAVAILABLE;
            case UNAVAILABLE -> newStatus == VehicleStatus.AVAILABLE;
        };

        if (!valid) {
            throw new InvalidStateTransitionException(String.format(
                    "Invalid vehicle lifecycle transition for vehicle %s: %s -> %s.",
                    vehicleId, this.status, newStatus));
        }

        this.status = newStatus;
        if (newStatus == VehicleStatus.AVAILABLE) {
            this.currentTripId = null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Vehicle vehicle)) return false;
        return Objects.equals(vehicleId, vehicle.vehicleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vehicleId);
    }

    @Override
    public String toString() {
        return String.format("%s (%s) [Home: %s, Cap: %d crates, Status: %s, Trip: %s, Loc: %s]",
                vehicleId, registrationNumber, homeWarehouseId, maxCrateCapacity, status,
                currentTripId != null ? currentTripId : "None", lastKnownLocation);
    }
}
