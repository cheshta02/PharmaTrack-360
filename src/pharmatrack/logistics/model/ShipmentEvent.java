package pharmatrack.logistics.model;

import pharmatrack.json.JsonCreator;
import pharmatrack.json.JsonProperty;
import pharmatrack.common.util.InputValidator;

import java.time.LocalDateTime;

/**
 * Audit event capturing operational milestones across temperature monitoring and shipment logistics.
 */
public class ShipmentEvent {
    private final String eventId;
    private final String eventType;
    private final String shipmentId;
    private final String tripId;
    private final String crateId;
    private final LocalDateTime timestamp;
    private final String description;

    @JsonCreator
    public ShipmentEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("eventType") String eventType,
            @JsonProperty("shipmentId") String shipmentId,
            @JsonProperty("tripId") String tripId,
            @JsonProperty("crateId") String crateId,
            @JsonProperty("timestamp") LocalDateTime timestamp,
            @JsonProperty("description") String description) {
        this.eventId = InputValidator.requireNonEmpty(eventId, "Event ID");
        this.eventType = InputValidator.requireNonEmpty(eventType, "Event Type");
        this.shipmentId = shipmentId;
        this.tripId = tripId;
        this.crateId = crateId;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
        this.description = description != null ? description : "";
    }

    public ShipmentEvent(String eventId, String eventType, String shipmentId, String tripId, String crateId, String description) {
        this(eventId, eventType, shipmentId, tripId, crateId, LocalDateTime.now(), description);
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getShipmentId() {
        return shipmentId;
    }

    public String getTripId() {
        return tripId;
    }

    public String getCrateId() {
        return crateId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s | Shipment: %s | Trip: %s | Crate: %s | %s",
                timestamp, eventType,
                shipmentId != null ? shipmentId : "N/A",
                tripId != null ? tripId : "N/A",
                crateId != null ? crateId : "N/A",
                description);
    }
}
