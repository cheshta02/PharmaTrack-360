package pharmatrack.temperature.model;

import pharmatrack.json.JsonCreator;
import pharmatrack.json.JsonProperty;
import pharmatrack.common.util.InputValidator;

import java.time.LocalDateTime;
import java.util.Objects;

public class TemperatureExcursion {
    private final String excursionId;
    private final String crateId;
    private final String sensorId;
    private final String batchId;
    private final String shipmentId;
    private final String tripId;
    private final double observedTemperature;
    private final double minPermitted;
    private final double maxPermitted;
    private final LocalDateTime timestamp;
    private final TemperatureStatus excursionType;
    private long durationSeconds;

    @JsonCreator
    public TemperatureExcursion(
            @JsonProperty("excursionId") String excursionId,
            @JsonProperty("crateId") String crateId,
            @JsonProperty("sensorId") String sensorId,
            @JsonProperty("batchId") String batchId,
            @JsonProperty("shipmentId") String shipmentId,
            @JsonProperty("tripId") String tripId,
            @JsonProperty("observedTemperature") double observedTemperature,
            @JsonProperty("minPermitted") double minPermitted,
            @JsonProperty("maxPermitted") double maxPermitted,
            @JsonProperty("timestamp") LocalDateTime timestamp,
            @JsonProperty("excursionType") TemperatureStatus excursionType,
            @JsonProperty("durationSeconds") long durationSeconds) {
        this.excursionId = InputValidator.requireNonEmpty(excursionId, "Excursion ID");
        this.crateId = InputValidator.requireNonEmpty(crateId, "Crate ID");
        this.sensorId = InputValidator.requireNonEmpty(sensorId, "Sensor ID");
        this.batchId = batchId;
        this.shipmentId = shipmentId;
        this.tripId = tripId;
        this.observedTemperature = observedTemperature;
        this.minPermitted = minPermitted;
        this.maxPermitted = maxPermitted;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
        this.excursionType = excursionType;
        this.durationSeconds = durationSeconds;
    }

    public TemperatureExcursion(String excursionId, String crateId, String sensorId, String batchId,
                                String shipmentId, String tripId, double observedTemperature,
                                double minPermitted, double maxPermitted, TemperatureStatus excursionType) {
        this(excursionId, crateId, sensorId, batchId, shipmentId, tripId, observedTemperature,
                minPermitted, maxPermitted, LocalDateTime.now(), excursionType, 0);
    }

    public String getExcursionId() {
        return excursionId;
    }

    public String getCrateId() {
        return crateId;
    }

    public String getSensorId() {
        return sensorId;
    }

    public String getBatchId() {
        return batchId;
    }

    public String getShipmentId() {
        return shipmentId;
    }

    public String getTripId() {
        return tripId;
    }

    public double getObservedTemperature() {
        return observedTemperature;
    }

    public double getMinPermitted() {
        return minPermitted;
    }

    public double getMaxPermitted() {
        return maxPermitted;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public TemperatureStatus getExcursionType() {
        return excursionType;
    }

    public long getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TemperatureExcursion that)) return false;
        return Objects.equals(excursionId, that.excursionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(excursionId);
    }

    @Override
    public String toString() {
        return String.format("[%s] EXCURSION %s: Crate %s (Sensor %s, Batch %s, Shipment %s, Trip %s) -> %.2f°C " +
                        "[Permitted: %.1f°C to %.1f°C, Type: %s, Duration: %ds]",
                timestamp, excursionId, crateId, sensorId,
                batchId != null ? batchId : "N/A",
                shipmentId != null ? shipmentId : "N/A",
                tripId != null ? tripId : "N/A",
                observedTemperature, minPermitted, maxPermitted, excursionType, durationSeconds);
    }
}
