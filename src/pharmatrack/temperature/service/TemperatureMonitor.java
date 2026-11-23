package pharmatrack.temperature.service;

import pharmatrack.common.util.IdGenerator;
import pharmatrack.storage.InMemoryDataStore;
import pharmatrack.temperature.model.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class TemperatureMonitor {
    private final InMemoryDataStore dataStore;
    private final CrateService crateService;
    private final SensorService sensorService;
    private final ProductBatchService batchService;
    private final ProductService productService;
    private final TemperaturePolicyService policyService;
    private final ExcursionDetector excursionDetector;

    private final ConcurrentHashMap<String, TemperatureStatus> currentCrateStatus = new ConcurrentHashMap<>();
    private final List<Consumer<TemperatureExcursion>> excursionListeners = new CopyOnWriteArrayList<>();

    public TemperatureMonitor(InMemoryDataStore dataStore, CrateService crateService, SensorService sensorService, ProductBatchService batchService, ProductService productService, TemperaturePolicyService policyService,
     ExcursionDetector excursionDetector) {
        this.dataStore = dataStore;
        this.crateService = crateService;
        this.sensorService = sensorService;
        this.batchService = batchService;
        this.productService = productService;
        this.policyService = policyService;
        this.excursionDetector = excursionDetector;
    }

    public void addExcursionListener(Consumer<TemperatureExcursion> listener) {
        if (listener != null) {
            excursionListeners.add(listener);
        }
    }

    public TemperatureReading processReading(String sensorId, String crateId, double temperature, LocalDateTime timestamp) {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }

        Sensor sensor = sensorService.findSensorById(sensorId).orElse(null);
        if (sensor != null) {
            sensor.recordReading(timestamp);
        }

        Crate crate = crateService.findCrateById(crateId).orElse(null);
        if (crate == null) {
            TemperatureReading invalidReading = new TemperatureReading(IdGenerator.nextId("RDG"), sensorId, crateId, timestamp, temperature, TemperatureStatus.INVALID_READING);
            dataStore.getReadings().add(invalidReading);
            return invalidReading;
        }

        String batchId = null;
        String shipmentId = crate.getCurrentShipmentId();
        String tripId = crate.getCurrentTripId();
        TemperaturePolicy policy = null;

        if (shipmentId != null && dataStore.getShipments().containsKey(shipmentId)) {
            var shipment = dataStore.getShipments().get(shipmentId);
            batchId = shipment.getBatchId();
            if (shipment.getProductId() != null) {
                var product = productService.findProductById(shipment.getProductId()).orElse(null);
                if (product != null) {
                    policy = policyService.findPolicyById(product.getPolicyId()).orElse(null);
                }
            }
        }

        if (policy == null) {
            policy = new TemperaturePolicy("DEFAULT-" + crate.getCrateId(), "Crate Native Range",
                    crate.getMinTempCapability(), crate.getMaxTempCapability(), "Operating capability");
        }

        TemperatureStatus tempStatus = excursionDetector.evaluateReading(temperature, policy);
        currentCrateStatus.put(crateId, tempStatus);

        TemperatureReading reading = new TemperatureReading(IdGenerator.nextId("RDG"), sensorId, crateId, timestamp, temperature, tempStatus);
        dataStore.getReadings().add(reading);

        Optional<TemperatureExcursion> excursionOpt = excursionDetector.checkAndRecordExcursion(
                crateId, sensorId, batchId, shipmentId, tripId, temperature, policy, timestamp);

        excursionOpt.ifPresent(excursion -> {
            for (Consumer<TemperatureExcursion> listener : excursionListeners) {
                try {
                    listener.accept(excursion);
                } catch (Exception ignored) {
                }
            }
        });

        return reading;
    }

    public TemperatureStatus getCurrentCrateStatus(String crateId) {
        return currentCrateStatus.getOrDefault(crateId, TemperatureStatus.NORMAL);
    }
}
