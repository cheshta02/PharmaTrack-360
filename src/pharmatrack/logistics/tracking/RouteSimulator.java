package pharmatrack.logistics.tracking;

import pharmatrack.common.exception.ValidationException;
import pharmatrack.common.util.IdGenerator;
import pharmatrack.logistics.model.*;
import pharmatrack.logistics.service.CrateReturnService;
import pharmatrack.logistics.service.DeliveryService;
import pharmatrack.logistics.service.TripService;
import pharmatrack.logistics.service.VehicleService;
import pharmatrack.storage.InMemoryDataStore;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;

/**
 * Simulates vehicle transit through checkpoints, delivery arrivals, delays, and vehicle returns.
 * Note: Simulates planned checkpoints; this is not real GPS or live traffic telemetry.
 */
public class RouteSimulator {
    private final InMemoryDataStore dataStore;
    private final DeliveryService deliveryService;
    private final CrateReturnService crateReturnService;
    private final ShipmentDelayDetector delayDetector;
    private final VehicleService vehicleService;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "RouteSimulator-Thread");
        t.setDaemon(true);
        return t;
    });

    private ScheduledFuture<?> scheduledTask;
    private boolean running = false;
    private final ConcurrentHashMap<String, Long> injectedDelaysMinutes = new ConcurrentHashMap<>();

    public RouteSimulator(InMemoryDataStore dataStore,
                          DeliveryService deliveryService,
                          CrateReturnService crateReturnService,
                          ShipmentDelayDetector delayDetector,
                          VehicleService vehicleService) {
        this.dataStore = dataStore;
        this.deliveryService = deliveryService;
        this.crateReturnService = crateReturnService;
        this.delayDetector = delayDetector;
        this.vehicleService = vehicleService;
    }

    public synchronized void start(long periodMillis) {
        if (running) return;
        running = true;
        scheduledTask = scheduler.scheduleAtFixedRate(this::tickAllActiveTrips, 500, periodMillis, TimeUnit.MILLISECONDS);
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

    public void injectDelay(String tripId, long delayMinutes) {
        injectedDelaysMinutes.put(tripId, delayMinutes);
    }

    public void tickAllActiveTrips() {
        for (VehicleTrip trip : dataStore.getVehicleTrips().values()) {
            if (trip.getStatus() == TripStatus.DISPATCHED || trip.getStatus() == TripStatus.IN_TRANSIT) {
                advanceTripStep(trip.getTripId());
            }
        }
    }

    /**
     * Deterministically advances a specific trip by one checkpoint.
     * Ideal for unit tests, scripted evaluations, and console interactive stepping.
     */
    public synchronized boolean advanceTripStep(String tripId) {
        VehicleTrip trip = dataStore.getVehicleTrips().get(tripId);
        if (trip == null) {
            throw new ValidationException("Trip " + tripId + " not found.");
        }

        if (trip.getStatus() == TripStatus.PLANNED) {
            // First transition from PLANNED to DISPATCHED
            trip.transitionStatus(TripStatus.DISPATCHED);
        }

        Route route = trip.getRoute();
        if (route == null || route.getCheckpoints().isEmpty()) {
            return false;
        }

        List<RouteCheckpoint> checkpoints = route.getCheckpoints();
        int currentIndex = trip.getCurrentCheckpointIndex();

        if (currentIndex >= checkpoints.size()) {
            // Trip already at or past the end
            if (trip.getStatus() != TripStatus.COMPLETED) {
                crateReturnService.completeVehicleTripReturn(tripId);
            }
            return false;
        }

        RouteCheckpoint cp = checkpoints.get(currentIndex);
        LocalDateTime simulatedTime = LocalDateTime.now();

        // Apply injected delay if present
        if (injectedDelaysMinutes.containsKey(tripId)) {
            long delayMins = injectedDelaysMinutes.remove(tripId);
            LocalDateTime baseExpected = cp.getExpectedArrivalTime() != null ? cp.getExpectedArrivalTime() : simulatedTime;
            simulatedTime = baseExpected.plusMinutes(delayMins);
        }

        cp.setSimulatedArrivalTime(simulatedTime);
        if (cp.getStatus() == CheckpointStatus.PENDING) {
            cp.setStatus(CheckpointStatus.REACHED);
        }

        // Evaluate delay against expected arrival time
        delayDetector.checkDelay(trip, cp, simulatedTime);

        // Update vehicle simulated location
        Vehicle vehicle = vehicleService.findVehicleById(trip.getVehicleId()).orElse(null);
        if (vehicle != null) {
            vehicle.setLastKnownLocation(cp.getName());
        }

        // Log Checkpoint Reached Event
        ShipmentEvent evt = new ShipmentEvent(
                IdGenerator.nextId("EVT"), "CheckpointReachedEvent",
                cp.getAssociatedShipmentId(), tripId, null,
                String.format("Trip %s reached checkpoint [%d/%d]: %s",
                        tripId, currentIndex + 1, checkpoints.size(), cp.getName()));
        dataStore.addEvent(evt);

        // If this is a delivery stop, trigger shipment unloading and delivery
        if (cp.isDeliveryStop() && cp.getAssociatedShipmentId() != null) {
            deliveryService.deliverShipment(cp.getAssociatedShipmentId(), simulatedTime);
        }

        // Advance to next index
        int nextIndex = currentIndex + 1;
        trip.setCurrentCheckpointIndex(nextIndex);

        // If trip was DISPATCHED, ensure marked IN_TRANSIT
        if (trip.getStatus() == TripStatus.DISPATCHED && nextIndex > 0) {
            trip.transitionStatus(TripStatus.IN_TRANSIT);
        }

        // If reached the end of the route
        if (nextIndex >= checkpoints.size()) {
            crateReturnService.completeVehicleTripReturn(tripId);
            return false;
        }

        return true;
    }
}
