package pharmatrack.temperature.service;

import pharmatrack.common.util.IdGenerator;
import pharmatrack.storage.InMemoryDataStore;
import pharmatrack.temperature.model.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ExcursionDetector {
    private final InMemoryDataStore dataStore;

    private final ConcurrentHashMap<String, TemperatureExcursion> ongoingExcursions = new ConcurrentHashMap<>();

    public ExcursionDetector(InMemoryDataStore dataStore) {
        this.dataStore = dataStore;
    }

    public TemperatureStatus evaluateReading(double temperature, TemperaturePolicy policy) {
        if (policy == null) {
            return TemperatureStatus.INVALID_READING;
        }
        if (temperature < policy.getMinTemp()) {
            return TemperatureStatus.BELOW_RANGE;
        } else if (temperature > policy.getMaxTemp()) {
            return TemperatureStatus.ABOVE_RANGE;
        } else {
            return TemperatureStatus.NORMAL;
        }
    }

    public Optional<TemperatureExcursion> checkAndRecordExcursion(String crateId, String sensorId, String batchId, String shipmentId,
        String tripId, double temperature, TemperaturePolicy policy, LocalDateTime timestamp) {

        if (policy == null) {
            return Optional.empty();
        }

        TemperatureStatus status = evaluateReading(temperature, policy);

        if (status == TemperatureStatus.ABOVE_RANGE || status == TemperatureStatus.BELOW_RANGE) {
            String excursionId = IdGenerator.nextId("EXC");
            TemperatureExcursion excursion = new TemperatureExcursion(excursionId, crateId, sensorId, batchId, shipmentId, tripId,
                temperature, policy.getMinTemp(), policy.getMaxTemp(), timestamp, status, 0);

            TemperatureExcursion previous = ongoingExcursions.get(crateId);
            if (previous != null && previous.getExcursionType() == status) {
                long durationSec = Duration.between(previous.getTimestamp(), timestamp).getSeconds();
                excursion.setDurationSeconds(Math.max(0, durationSec));
            } else {
                ongoingExcursions.put(crateId, excursion);
            }

            dataStore.getExcursions().add(excursion);
            return Optional.of(excursion);
        } else {
            ongoingExcursions.remove(crateId);
            return Optional.empty();
        }
    }

    public List<TemperatureExcursion> getAllExcursions() {
        return new ArrayList<>(dataStore.getExcursions());
    }

    public List<TemperatureExcursion> getExcursionsByCrate(String crateId) {
        return dataStore.getExcursions().stream().filter(e -> e.getCrateId().equals(crateId)).toList();
    }

    public List<TemperatureExcursion> getExcursionsByShipment(String shipmentId) {
        return dataStore.getExcursions().stream().filter(e -> shipmentId != null && shipmentId.equals(e.getShipmentId())).toList();
    }
}
