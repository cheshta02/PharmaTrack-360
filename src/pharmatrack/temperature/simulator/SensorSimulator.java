package pharmatrack.temperature.simulator;

import pharmatrack.logistics.model.Shipment;
import pharmatrack.storage.InMemoryDataStore;
import pharmatrack.temperature.model.*;
import pharmatrack.temperature.service.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SensorSimulator {
    private final InMemoryDataStore dataStore;
    private final TemperatureMonitor temperatureMonitor;
    private final SensorHealthMonitor sensorHealthMonitor;
    private final ProductService productService;
    private final TemperaturePolicyService policyService;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "SensorSimulator-Thread");
        t.setDaemon(true);
        return t;
    });

    private ScheduledFuture<?> scheduledTask;
    private final Random random = new Random(42); 
    private boolean running = false;

    private final ConcurrentHashMap<String, Double> temperatureOverrides = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Double> gradualTrends = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> trendRemainingCycles = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> invalidOutputSensors = new ConcurrentHashMap<>();

    public SensorSimulator(InMemoryDataStore dataStore, TemperatureMonitor temperatureMonitor, SensorHealthMonitor sensorHealthMonitor, ProductService productService, TemperaturePolicyService policyService) {
        this.dataStore = dataStore;
        this.temperatureMonitor = temperatureMonitor;
        this.sensorHealthMonitor = sensorHealthMonitor;
        this.productService = productService;
        this.policyService = policyService;
    }

    public synchronized void start(long periodMillis) {
        if (running) return;
        running = true;
        scheduledTask = scheduler.scheduleAtFixedRate(this::tick, 500, periodMillis, TimeUnit.MILLISECONDS);
    }

    public synchronized void stop() {
        if (!running) return;
        running = false;
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
        }
    }

    public synchronized void shutdown() {
        stop();
        scheduler.shutdownNow();
    }

    public boolean isRunning() {
        return running;
    }

    public void tick() {
        try {
            LocalDateTime now = LocalDateTime.now();

            for (Crate crate : dataStore.getCrates().values()) {
                String sensorId = crate.getAssignedSensorId();
                if (sensorId == null) continue;

                Sensor sensor = dataStore.getSensors().get(sensorId);
                if (sensor == null) continue;

                if (sensor.getStatus() == SensorStatus.FAULTY) {
                    continue; 
                }

                if (Boolean.TRUE.equals(invalidOutputSensors.get(sensorId))) {
                    temperatureMonitor.processReading(sensorId, crate.getCrateId(), 9999.0, now);
                    continue;
                }

                double targetTemp = determineNominalTemperature(crate);

                if (gradualTrends.containsKey(crate.getCrateId())) {
                    double delta = gradualTrends.get(crate.getCrateId());
                    int remaining = trendRemainingCycles.getOrDefault(crate.getCrateId(), 0);
                    if (remaining > 0) {
                        double current = temperatureOverrides.getOrDefault(crate.getCrateId(), targetTemp);
                        double newTemp = current + delta;
                        temperatureOverrides.put(crate.getCrateId(), newTemp);
                        trendRemainingCycles.put(crate.getCrateId(), remaining - 1);
                    } else {
                        gradualTrends.remove(crate.getCrateId());
                        trendRemainingCycles.remove(crate.getCrateId());
                    }
                }

                double readingVal;
                if (temperatureOverrides.containsKey(crate.getCrateId())) {
                    readingVal = temperatureOverrides.get(crate.getCrateId());
                } else {
                    double jitter = (random.nextDouble() * 0.6) - 0.3;
                    readingVal = targetTemp + jitter;
                }

                temperatureMonitor.processReading(sensorId, crate.getCrateId(), readingVal, now);
            }

            sensorHealthMonitor.evaluateAllSensors();

        } catch (Exception e) {
            System.err.println("Error in SensorSimulator cycle: " + e.getMessage());
        }
    }

    private double determineNominalTemperature(Crate crate) {
        String shipmentId = crate.getCurrentShipmentId();
        if (shipmentId != null && dataStore.getShipments().containsKey(shipmentId)) {
            Shipment shipment = dataStore.getShipments().get(shipmentId);
            if (shipment.getProductId() != null) {
                PharmaceuticalProduct product = productService.findProductById(shipment.getProductId()).orElse(null);
                if (product != null) {
                    TemperaturePolicy policy = policyService.findPolicyById(product.getPolicyId()).orElse(null);
                    if (policy != null) {
                        return (policy.getMinTemp() + policy.getMaxTemp()) / 2.0;
                    }
                }
            }
        }
        return (crate.getMinTempCapability() + crate.getMaxTempCapability()) / 2.0;
    }

    public void injectSpike(String crateId, double spikeTemperature) {
        temperatureOverrides.put(crateId, spikeTemperature);
    }

    public void injectDrop(String crateId, double dropTemperature) {
        temperatureOverrides.put(crateId, dropTemperature);
    }

    public void injectGradualTrend(String crateId, double deltaPerCycle, int cycles) {
        gradualTrends.put(crateId, deltaPerCycle);
        trendRemainingCycles.put(crateId, cycles);
    }

    public void injectSensorFailure(String sensorId) {
        Sensor s = dataStore.getSensors().get(sensorId);
        if (s != null) {
            s.setStatus(SensorStatus.FAULTY);
        }
    }

    public void injectInvalidOutput(String sensorId) {
        invalidOutputSensors.put(sensorId, true);
    }

    public void injectNormal(String crateId) {
        temperatureOverrides.remove(crateId);
        gradualTrends.remove(crateId);
        trendRemainingCycles.remove(crateId);
        Crate c = dataStore.getCrates().get(crateId);
        if (c != null && c.getAssignedSensorId() != null) {
            invalidOutputSensors.remove(c.getAssignedSensorId());
            Sensor s = dataStore.getSensors().get(c.getAssignedSensorId());
            if (s != null) {
                s.setStatus(SensorStatus.ACTIVE);
            }
        }
    }
}
