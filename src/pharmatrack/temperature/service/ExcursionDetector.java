package pharmatrack.temperature.service;

import pharmatrack.common.util.IdGenerator;
import pharmatrack.storage.InMemoryDataStore;
import pharmatrack.temperature.model.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Evaluates temperature readings against applicable policies and detects excursions per individual crate.
 */
public class ExcursionDetector {
    private final InMemoryDataStore dataStore;
    // Track ongoing excursions to compute continuous excursion durations: crateId -> ongoing excursion
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

    public Optional<TemperatureExcursion> checkAndRecordExcursion(
            String crateId, String sensorId, String batchId, String shipmentId,
            String tripId, double temperature, TemperaturePolicy policy, LocalDateTime timestamp) {

        if (policy == null) {
            return Optional.empty();
        }

        TemperatureStatus status = evaluateReading(temperature, policy);

        if (status == TemperatureStatus.ABOVE_RANGE || status == TemperatureStatus.BELOW_RANGE) {
            String excursionId = IdGenerator.nextId("EXC");
            TemperatureExcursion excursion = new TemperatureExcursion(
                    excursionId, crateId, sensorId, batchId, shipmentId, tripId,
                    temperature, policy.getMinTemp(), policy.getMaxTemp(), timestamp, status, 0);

            // Compute duration if this is an extension of an ongoing excursion
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
            // Temperature returned to normal -> clear ongoing excursion tracker for this crate
            ongoingExcursions.remove(crateId);
            return Optional.empty();
        }
    }

    public List<TemperatureExcursion> getAllExcursions() {
        return new ArrayList<>(dataStore.getExcursions());
    }

    public List<TemperatureExcursion> getExcursionsByCrate(String crateId) {
        return dataStore.getExcursions().stream()
                .filter(e -> e.getCrateId().equals(crateId))
                .toList();
    }

    public List<TemperatureExcursion> getExcursionsByShipment(String shipmentId) {
        return dataStore.getExcursions().stream()
                .filter(e -> shipmentId != null && shipmentId.equals(e.getShipmentId()))
                .toList();
    }
}
