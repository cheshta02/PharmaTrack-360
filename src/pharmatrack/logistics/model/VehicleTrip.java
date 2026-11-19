package pharmatrack.logistics.model;

import pharmatrack.json.JsonCreator;
import pharmatrack.json.JsonProperty;
import pharmatrack.common.exception.InvalidStateTransitionException;
import pharmatrack.common.exception.ValidationException;
import pharmatrack.common.util.InputValidator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Operates a consolidated or single-order transportation mission using one vehicle.
 * A trip contains up to 5 crates across one or more shipments originating from the same warehouse.
 */
public class VehicleTrip {
    private final String tripId;
    private final String vehicleId;
    private final String sourceWarehouseId;
    private final List<String> shipmentIds = new ArrayList<>();
    private final List<String> crateIds = new ArrayList<>();
    private Route route;
    private int totalCrates;
    private TripStatus status;
    private LocalDateTime dispatchTime;
    private LocalDateTime expectedCompletionTime;
    private LocalDateTime actualCompletionTime;
    private int currentCheckpointIndex;

    @JsonCreator
    public VehicleTrip(
            @JsonProperty("tripId") String tripId,
            @JsonProperty("vehicleId") String vehicleId,
            @JsonProperty("sourceWarehouseId") String sourceWarehouseId,
            @JsonProperty("shipmentIds") List<String> shipmentIds,
            @JsonProperty("crateIds") List<String> crateIds,
            @JsonProperty("route") Route route,
            @JsonProperty("totalCrates") int totalCrates,
            @JsonProperty("status") TripStatus status,
            @JsonProperty("dispatchTime") LocalDateTime dispatchTime,
            @JsonProperty("expectedCompletionTime") LocalDateTime expectedCompletionTime,
            @JsonProperty("actualCompletionTime") LocalDateTime actualCompletionTime,
            @JsonProperty("currentCheckpointIndex") int currentCheckpointIndex) {
        this.tripId = InputValidator.requireNonEmpty(tripId, "Trip ID");
        this.vehicleId = InputValidator.requireNonEmpty(vehicleId, "Vehicle ID");
        this.sourceWarehouseId = InputValidator.requireNonEmpty(sourceWarehouseId, "Source Warehouse ID");
        if (shipmentIds != null) {
            this.shipmentIds.addAll(shipmentIds);
        }
        if (crateIds != null) {
            this.crateIds.addAll(crateIds);
        }
        this.route = route;
        if (totalCrates > Vehicle.MAX_CRATE_CAPACITY) {
            throw new ValidationException("Trip total crates cannot exceed vehicle capacity (" + Vehicle.MAX_CRATE_CAPACITY + "). Given: " + totalCrates);
        }
        this.totalCrates = totalCrates;
        this.status = status != null ? status : TripStatus.PLANNED;
        this.dispatchTime = dispatchTime;
        this.expectedCompletionTime = expectedCompletionTime;
        this.actualCompletionTime = actualCompletionTime;
        this.currentCheckpointIndex = currentCheckpointIndex;
    }

    public VehicleTrip(String tripId, String vehicleId, String sourceWarehouseId,
                       List<String> shipmentIds, List<String> crateIds, int totalCrates) {
        this(tripId, vehicleId, sourceWarehouseId, shipmentIds, crateIds, null, totalCrates,
                TripStatus.PLANNED, null, null, null, 0);
    }

    public String getTripId() {
        return tripId;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public String getSourceWarehouseId() {
        return sourceWarehouseId;
    }

    public List<String> getShipmentIds() {
        return Collections.unmodifiableList(shipmentIds);
    }

    public List<String> getCrateIds() {
        return Collections.unmodifiableList(crateIds);
    }

    public Route getRoute() {
        return route;
    }

    public void setRoute(Route route) {
        this.route = route;
    }

    public int getTotalCrates() {
        return totalCrates;
    }

    public void setTotalCrates(int totalCrates) {
        if (totalCrates > Vehicle.MAX_CRATE_CAPACITY) {
            throw new ValidationException("Exceeds max vehicle capacity (" + Vehicle.MAX_CRATE_CAPACITY + ")");
        }
        this.totalCrates = totalCrates;
    }

    public int getRemainingCapacity() {
        return Math.max(0, Vehicle.MAX_CRATE_CAPACITY - totalCrates);
    }

    public TripStatus getStatus() {
        return status;
    }

    public LocalDateTime getDispatchTime() {
        return dispatchTime;
    }

    public void setDispatchTime(LocalDateTime dispatchTime) {
        this.dispatchTime = dispatchTime;
    }

    public LocalDateTime getExpectedCompletionTime() {
        return expectedCompletionTime;
    }

    public void setExpectedCompletionTime(LocalDateTime expectedCompletionTime) {
        this.expectedCompletionTime = expectedCompletionTime;
    }

    public LocalDateTime getActualCompletionTime() {
        return actualCompletionTime;
    }

    public void setActualCompletionTime(LocalDateTime actualCompletionTime) {
        this.actualCompletionTime = actualCompletionTime;
    }

    public int getCurrentCheckpointIndex() {
        return currentCheckpointIndex;
    }

    public void setCurrentCheckpointIndex(int currentCheckpointIndex) {
        this.currentCheckpointIndex = currentCheckpointIndex;
    }

    public synchronized void transitionStatus(TripStatus newStatus) {
        if (this.status == newStatus) {
            return;
        }

        boolean valid = switch (this.status) {
            case PLANNED -> newStatus == TripStatus.DISPATCHED || newStatus == TripStatus.CANCELLED;
            case DISPATCHED -> newStatus == TripStatus.IN_TRANSIT || newStatus == TripStatus.CANCELLED;
            case IN_TRANSIT -> newStatus == TripStatus.DELIVERED_ALL;
            case DELIVERED_ALL -> newStatus == TripStatus.COMPLETED;
            case COMPLETED -> false;
            case CANCELLED -> false;
        };

        if (!valid) {
            throw new InvalidStateTransitionException(String.format(
                    "Invalid trip lifecycle transition for trip %s: %s -> %s.",
                    tripId, this.status, newStatus));
        }

        this.status = newStatus;
        if (newStatus == TripStatus.DISPATCHED && this.dispatchTime == null) {
            this.dispatchTime = LocalDateTime.now();
        } else if (newStatus == TripStatus.COMPLETED) {
            this.actualCompletionTime = LocalDateTime.now();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VehicleTrip that)) return false;
        return Objects.equals(tripId, that.tripId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tripId);
    }

    @Override
    public String toString() {
        return String.format("%s: Vehicle %s from %s [%d crates, %d shipments, Status: %s, Route: %s]",
                tripId, vehicleId, sourceWarehouseId, totalCrates, shipmentIds.size(), status,
                route != null ? route.getRouteId() : "Pending");
    }
}
