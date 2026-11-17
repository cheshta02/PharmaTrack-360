package pharmatrack.temperature.service;

import pharmatrack.storage.InMemoryDataStore;
import pharmatrack.temperature.model.Sensor;
import pharmatrack.temperature.model.SensorStatus;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Monitors sensor communication health based on timestamp timeouts and explicit fault signals.
 */
public class SensorHealthMonitor {
    private final InMemoryDataStore dataStore;

    public SensorHealthMonitor(InMemoryDataStore dataStore) {
        this.dataStore = dataStore;
    }

    public synchronized void evaluateAllSensors() {
        LocalDateTime now = LocalDateTime.now();
        for (Sensor sensor : dataStore.getSensors().values()) {
            evaluateSensorHealth(sensor, now);
        }
    }

    public void evaluateSensorHealth(Sensor sensor, LocalDateTime evaluationTime) {
        if (sensor == null) return;

        // If explicitly marked faulty, preserve faulty status
        if (sensor.getStatus() == SensorStatus.FAULTY) {
            return;
        }

        LocalDateTime lastComm = sensor.getLastHeartbeatTime();
        if (lastComm == null) {
            sensor.setStatus(SensorStatus.INACTIVE);
            return;
        }

        long secondsElapsed = Duration.between(lastComm, evaluationTime).getSeconds();
        if (secondsElapsed > sensor.getTimeoutSeconds()) {
            sensor.setStatus(SensorStatus.INACTIVE);
        } else {
            sensor.setStatus(SensorStatus.ACTIVE);
        }
    }

    public List<Sensor> getActiveSensors() {
        return dataStore.getSensors().values().stream()
                .filter(s -> s.getStatus() == SensorStatus.ACTIVE)
                .toList();
    }

    public List<Sensor> getInactiveSensors() {
        return dataStore.getSensors().values().stream()
                .filter(s -> s.getStatus() == SensorStatus.INACTIVE)
                .toList();
    }

    public List<Sensor> getFaultySensors() {
        return dataStore.getSensors().values().stream()
                .filter(s -> s.getStatus() == SensorStatus.FAULTY)
                .toList();
    }

    public String generateHealthReport() {
        evaluateAllSensors();
        StringBuilder sb = new StringBuilder();
        sb.append("=== Sensor Health Report ===\n");
        sb.append(String.format("Total Sensors: %d | Active: %d | Inactive: %d | Faulty: %d\n",
                dataStore.getSensors().size(),
                getActiveSensors().size(),
                getInactiveSensors().size(),
                getFaultySensors().size()));
        sb.append("--------------------------------------------------------------------------------\n");
        sb.append(String.format("%-10s %-10s %-12s %-10s %-20s %-10s\n",
                "SensorID", "CrateID", "Type", "Status", "Last Seen", "Timeout(s)"));
        sb.append("--------------------------------------------------------------------------------\n");
        for (Sensor s : dataStore.getSensors().values()) {
            sb.append(String.format("%-10s %-10s %-12s %-10s %-20s %-10d\n",
                    s.getSensorId(),
                    s.getCrateId() != null ? s.getCrateId() : "Unassigned",
                    s.getSensorType(),
                    s.getStatus(),
                    s.getLastHeartbeatTime() != null ? s.getLastHeartbeatTime().toString() : "Never",
                    s.getTimeoutSeconds()));
        }
        return sb.toString();
    }
}
