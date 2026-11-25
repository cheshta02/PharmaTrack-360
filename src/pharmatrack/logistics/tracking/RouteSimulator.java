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

public class RouteSimulator {
    private final InMemoryDataStore dataStore;
    private final DeliveryService deliveryService;
    private final CrateReturnService crateReturnService;
    private final ShipmentDelayDetector delayDetector;
    private final VehicleService vehicleService;
    private final ConcurrentHashMap<String, Long> injectedDelaysMinutes = new ConcurrentHashMap<>();

    public RouteSimulator(InMemoryDataStore dataStore, DeliveryService deliveryService, CrateReturnService crateReturnService,  ShipmentDelayDetector delayDetector,  VehicleService vehicleService) {
        this.dataStore = dataStore;
        this.deliveryService = deliveryService;
        this.crateReturnService = crateReturnService;
        this.delayDetector = delayDetector;
        this.vehicleService = vehicleService;
    }

    public void injectDelay(String tripId, long delayMinutes) {
        injectedDelaysMinutes.put(tripId, delayMinutes);
    }
    public synchronized boolean advanceTripStep(String tripId) {
        VehicleTrip trip = dataStore.getVehicleTrips().get(tripId);
        if (trip == null) {
            throw new ValidationException("Trip " + tripId + " not found.");
        }

        if (trip.getStatus() == TripStatus.PLANNED) {
            trip.transitionStatus(TripStatus.DISPATCHED);
        }

        Route route = trip.getRoute();
        if (route == null || route.getCheckpoints().isEmpty()) {
            return false;
        }

        List<RouteCheckpoint> checkpoints = route.getCheckpoints();
        int currentIndex = trip.getCurrentCheckpointIndex();

        if (currentIndex >= checkpoints.size()) {
            if (trip.getStatus() != TripStatus.COMPLETED) {
                crateReturnService.completeVehicleTripReturn(tripId);
            }
            return false;
        }

        RouteCheckpoint cp = checkpoints.get(currentIndex);
        LocalDateTime simulatedTime = LocalDateTime.now();

        if (injectedDelaysMinutes.containsKey(tripId)) {
            long delayMins = injectedDelaysMinutes.remove(tripId);
            LocalDateTime baseExpected = cp.getExpectedArrivalTime() != null ? cp.getExpectedArrivalTime() : simulatedTime;
            simulatedTime = baseExpected.plusMinutes(delayMins);
        }

        cp.setSimulatedArrivalTime(simulatedTime);
        if (cp.getStatus() == CheckpointStatus.PENDING) {
            cp.setStatus(CheckpointStatus.REACHED);
        }

        delayDetector.checkDelay(trip, cp, simulatedTime);

        Vehicle vehicle = vehicleService.findVehicleById(trip.getVehicleId()).orElse(null);
        if (vehicle != null) {
            vehicle.setLastKnownLocation(cp.getName());
        }

        ShipmentEvent evt = new ShipmentEvent(
                IdGenerator.nextId("EVT"), "CheckpointReachedEvent",
                cp.getAssociatedShipmentId(), tripId, null,
                String.format("Trip %s reached checkpoint [%d/%d]: %s", tripId, currentIndex + 1, checkpoints.size(), cp.getName()));
        dataStore.addEvent(evt);

        if (cp.isDeliveryStop() && cp.getAssociatedShipmentId() != null) {
            deliveryService.deliverShipment(cp.getAssociatedShipmentId(), simulatedTime);
        }

        int nextIndex = currentIndex + 1;
        trip.setCurrentCheckpointIndex(nextIndex);

        if (trip.getStatus() == TripStatus.DISPATCHED && nextIndex > 0) {
            trip.transitionStatus(TripStatus.IN_TRANSIT);
        }

        if (nextIndex >= checkpoints.size()) {
            crateReturnService.completeVehicleTripReturn(tripId);
            return false;
        }

        return true;
    }
}
