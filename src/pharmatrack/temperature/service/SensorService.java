package pharmatrack.temperature.service;

import pharmatrack.common.exception.ValidationException;
import pharmatrack.common.util.IdGenerator;
import pharmatrack.common.util.InputValidator;
import pharmatrack.storage.InMemoryDataStore;
import pharmatrack.temperature.model.Crate;
import pharmatrack.temperature.model.Sensor;
import pharmatrack.temperature.model.SensorStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service managing temperature sensors attached to crates.
 */
public class SensorService {
    private final InMemoryDataStore dataStore;
    private final CrateService crateService;

    public SensorService(InMemoryDataStore dataStore, CrateService crateService) {
        this.dataStore = dataStore;
        this.crateService = crateService;
    }

    public Sensor registerSensor(String sensorId, String crateId, String sensorType, long timeoutSeconds) {
        if (sensorId == null || sensorId.trim().isEmpty()) {
            sensorId = IdGenerator.nextId("SEN");
        }
        if (dataStore.getSensors().containsKey(sensorId)) {
            throw new ValidationException("Sensor with ID " + sensorId + " already exists.");
        }

        if (crateId != null && !crateId.trim().isEmpty()) {
            Crate crate = crateService.findCrateById(crateId)
                    .orElseThrow(() -> new ValidationException("Cannot assign sensor to non-existent crate: " + crateId));
            crate.setAssignedSensorId(sensorId);
        }

        Sensor sensor = new Sensor(sensorId, crateId, sensorType, SensorStatus.ACTIVE, null, LocalDateTime.now(), timeoutSeconds);
        dataStore.getSensors().put(sensorId, sensor);
        return sensor;
    }

    public Optional<Sensor> findSensorById(String sensorId) {
        if (sensorId == null) return Optional.empty();
        return Optional.ofNullable(dataStore.getSensors().get(sensorId));
    }

    public Optional<Sensor> findSensorByCrateId(String crateId) {
        if (crateId == null) return Optional.empty();
        return dataStore.getSensors().values().stream()
                .filter(s -> crateId.equals(s.getCrateId()))
                .findFirst();
    }

    public List<Sensor> getAllSensors() {
        return new ArrayList<>(dataStore.getSensors().values());
    }

    public void recordHeartbeat(String sensorId) {
        Sensor sensor = findSensorById(sensorId)
                .orElseThrow(() -> new ValidationException("Sensor " + sensorId + " not found."));
        sensor.recordHeartbeat(LocalDateTime.now());
    }

    public void setSensorStatus(String sensorId, SensorStatus status) {
        Sensor sensor = findSensorById(sensorId)
                .orElseThrow(() -> new ValidationException("Sensor " + sensorId + " not found."));
        sensor.setStatus(status);
    }
}
