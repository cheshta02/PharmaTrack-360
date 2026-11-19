package pharmatrack.logistics.model;

import pharmatrack.json.JsonCreator;
import pharmatrack.json.JsonProperty;
import pharmatrack.common.util.InputValidator;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Waypoint or delivery stop along a vehicle trip route.
 */
public class RouteCheckpoint {
    private final String checkpointId;
    private final String name;
    private final double latitude;
    private final double longitude;
    private final double distanceFromStartKm;
    private final boolean deliveryStop;
    private final String associatedShipmentId;
    private LocalDateTime expectedArrivalTime;
    private LocalDateTime simulatedArrivalTime;
    private CheckpointStatus status;

    @JsonCreator
    public RouteCheckpoint(
            @JsonProperty("checkpointId") String checkpointId,
            @JsonProperty("name") String name,
            @JsonProperty("latitude") double latitude,
            @JsonProperty("longitude") double longitude,
            @JsonProperty("distanceFromStartKm") double distanceFromStartKm,
            @JsonProperty("deliveryStop") boolean deliveryStop,
            @JsonProperty("associatedShipmentId") String associatedShipmentId,
            @JsonProperty("expectedArrivalTime") LocalDateTime expectedArrivalTime,
            @JsonProperty("simulatedArrivalTime") LocalDateTime simulatedArrivalTime,
            @JsonProperty("status") CheckpointStatus status) {
        this.checkpointId = InputValidator.requireNonEmpty(checkpointId, "Checkpoint ID");
        this.name = InputValidator.requireNonEmpty(name, "Checkpoint Name");
        this.latitude = latitude;
        this.longitude = longitude;
        this.distanceFromStartKm = distanceFromStartKm >= 0 ? distanceFromStartKm : 0.0;
        this.deliveryStop = deliveryStop;
        this.associatedShipmentId = associatedShipmentId;
        this.expectedArrivalTime = expectedArrivalTime;
        this.simulatedArrivalTime = simulatedArrivalTime;
        this.status = status != null ? status : CheckpointStatus.PENDING;
    }

    public RouteCheckpoint(String checkpointId, String name, double latitude, double longitude,
                           double distanceFromStartKm, boolean deliveryStop, String associatedShipmentId) {
        this(checkpointId, name, latitude, longitude, distanceFromStartKm, deliveryStop, associatedShipmentId,
                null, null, CheckpointStatus.PENDING);
    }

    public String getCheckpointId() {
        return checkpointId;
    }

    public String getName() {
        return name;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getDistanceFromStartKm() {
        return distanceFromStartKm;
    }

    public boolean isDeliveryStop() {
        return deliveryStop;
    }

    public String getAssociatedShipmentId() {
        return associatedShipmentId;
    }

    public LocalDateTime getExpectedArrivalTime() {
        return expectedArrivalTime;
    }

    public void setExpectedArrivalTime(LocalDateTime expectedArrivalTime) {
        this.expectedArrivalTime = expectedArrivalTime;
    }

    public LocalDateTime getSimulatedArrivalTime() {
        return simulatedArrivalTime;
    }

    public void setSimulatedArrivalTime(LocalDateTime simulatedArrivalTime) {
        this.simulatedArrivalTime = simulatedArrivalTime;
    }

    public CheckpointStatus getStatus() {
        return status;
    }

    public void setStatus(CheckpointStatus status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RouteCheckpoint that)) return false;
        return Objects.equals(checkpointId, that.checkpointId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(checkpointId);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s (%.1f km) [%.4f, %.4f] %s (Status: %s)",
                checkpointId, name, distanceFromStartKm, latitude, longitude,
                deliveryStop ? "(DELIVERY -> " + associatedShipmentId + ")" : "(WAYPOINT)",
                status);
    }
}
