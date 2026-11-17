package pharmatrack.temperature.model;

import pharmatrack.json.JsonCreator;
import pharmatrack.json.JsonProperty;
import pharmatrack.common.util.InputValidator;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Temperature sensor attached to a crate for continuous cold-chain monitoring.
 */
public class Sensor {
    private final String sensorId;
    private String crateId;
    private String sensorType;
    private SensorStatus status;
    private LocalDateTime lastReadingTime;
    private LocalDateTime lastHeartbeatTime;
    private long timeoutSeconds;

    @JsonCreator
    public Sensor(
            @JsonProperty("sensorId") String sensorId,
            @JsonProperty("crateId") String crateId,
            @JsonProperty("sensorType") String sensorType,
            @JsonProperty("status") SensorStatus status,
            @JsonProperty("lastReadingTime") LocalDateTime lastReadingTime,
            @JsonProperty("lastHeartbeatTime") LocalDateTime lastHeartbeatTime,
            @JsonProperty("timeoutSeconds") long timeoutSeconds) {
        this.sensorId = InputValidator.requireNonEmpty(sensorId, "Sensor ID");
        this.crateId = crateId;
        this.sensorType = (sensorType == null || sensorType.trim().isEmpty()) ? "IoT-BLE-Thermometer" : sensorType.trim();
        this.status = status != null ? status : SensorStatus.ACTIVE;
        this.lastReadingTime = lastReadingTime;
        this.lastHeartbeatTime = lastHeartbeatTime != null ? lastHeartbeatTime : LocalDateTime.now();
        this.timeoutSeconds = timeoutSeconds > 0 ? timeoutSeconds : 60;
    }

    public Sensor(String sensorId, String crateId, String sensorType) {
        this(sensorId, crateId, sensorType, SensorStatus.ACTIVE, null, LocalDateTime.now(), 60);
    }

    public String getSensorId() {
        return sensorId;
    }

    public String getCrateId() {
        return crateId;
    }

    public void setCrateId(String crateId) {
        this.crateId = crateId;
    }

    public String getSensorType() {
        return sensorType;
    }

    public void setSensorType(String sensorType) {
        this.sensorType = sensorType;
    }

    public SensorStatus getStatus() {
        return status;
    }

    public void setStatus(SensorStatus status) {
        this.status = status;
    }

    public LocalDateTime getLastReadingTime() {
        return lastReadingTime;
    }

    public void setLastReadingTime(LocalDateTime lastReadingTime) {
        this.lastReadingTime = lastReadingTime;
    }

    public LocalDateTime getLastHeartbeatTime() {
        return lastHeartbeatTime;
    }

    public void setLastHeartbeatTime(LocalDateTime lastHeartbeatTime) {
        this.lastHeartbeatTime = lastHeartbeatTime;
    }

    public long getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(long timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public void recordReading(LocalDateTime timestamp) {
        this.lastReadingTime = timestamp;
        this.lastHeartbeatTime = timestamp;
        if (this.status != SensorStatus.FAULTY) {
            this.status = SensorStatus.ACTIVE;
        }
    }

    public void recordHeartbeat(LocalDateTime timestamp) {
        this.lastHeartbeatTime = timestamp;
        if (this.status != SensorStatus.FAULTY) {
            this.status = SensorStatus.ACTIVE;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Sensor sensor)) return false;
        return Objects.equals(sensorId, sensor.sensorId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sensorId);
    }

    @Override
    public String toString() {
        return String.format("%s: Crate %s (%s, Status: %s, LastSeen: %s)",
                sensorId, crateId != null ? crateId : "Unassigned", sensorType, status,
                lastHeartbeatTime != null ? lastHeartbeatTime : "Never");
    }
}
