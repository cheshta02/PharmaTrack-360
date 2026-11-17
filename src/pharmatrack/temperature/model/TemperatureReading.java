package pharmatrack.temperature.model;

import pharmatrack.json.JsonCreator;
import pharmatrack.json.JsonProperty;
import pharmatrack.common.util.InputValidator;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * An individual temperature observation captured by a crate sensor.
 */
public class TemperatureReading {
    private final String readingId;
    private final String sensorId;
    private final String crateId;
    private final LocalDateTime timestamp;
    private final double temperature;
    private final TemperatureStatus status;

    @JsonCreator
    public TemperatureReading(
            @JsonProperty("readingId") String readingId,
            @JsonProperty("sensorId") String sensorId,
            @JsonProperty("crateId") String crateId,
            @JsonProperty("timestamp") LocalDateTime timestamp,
            @JsonProperty("temperature") double temperature,
            @JsonProperty("status") TemperatureStatus status) {
        this.readingId = InputValidator.requireNonEmpty(readingId, "Reading ID");
        this.sensorId = InputValidator.requireNonEmpty(sensorId, "Sensor ID");
        this.crateId = InputValidator.requireNonEmpty(crateId, "Crate ID");
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
        this.temperature = temperature;
        this.status = status != null ? status : TemperatureStatus.NORMAL;
    }

    public String getReadingId() {
        return readingId;
    }

    public String getSensorId() {
        return sensorId;
    }

    public String getCrateId() {
        return crateId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public double getTemperature() {
        return temperature;
    }

    public TemperatureStatus getStatus() {
        return status;
    }

    public boolean isValid() {
        return status != TemperatureStatus.INVALID_READING && status != TemperatureStatus.SENSOR_UNAVAILABLE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TemperatureReading that)) return false;
        return Objects.equals(readingId, that.readingId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(readingId);
    }

    @Override
    public String toString() {
        return String.format("[%s] Reading %s: Crate %s, Sensor %s -> %.2f°C (%s)",
                timestamp, readingId, crateId, sensorId, temperature, status);
    }
}
