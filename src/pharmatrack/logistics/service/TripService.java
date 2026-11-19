package pharmatrack.logistics.service;

import pharmatrack.common.exception.ValidationException;
import pharmatrack.common.util.IdGenerator;
import pharmatrack.logistics.model.*;
import pharmatrack.logistics.routing.RouteService;
import pharmatrack.storage.InMemoryDataStore;
import pharmatrack.temperature.model.Crate;
import pharmatrack.temperature.model.CrateStatus;
import pharmatrack.temperature.service.CrateService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service managing vehicle trip creation, loading validation, dispatching, and lifecycle progression.
 */
public class TripService {
    private final InMemoryDataStore dataStore;
    private final CrateService crateService;
    private final VehicleService vehicleService;
    private final RouteService routeService;
    private final ShipmentValidator shipmentValidator;

    public TripService(InMemoryDataStore dataStore,
                       CrateService crateService,
                       VehicleService vehicleService,
                       RouteService routeService,
                       ShipmentValidator shipmentValidator) {
        this.dataStore = dataStore;
        this.crateService = crateService;
        this.vehicleService = vehicleService;
        this.routeService = routeService;
        this.shipmentValidator = shipmentValidator;
    }

    public VehicleTrip createTrip(String sourceWarehouseId, List<Shipment> shipments, Vehicle vehicle) {
        if (sourceWarehouseId == null) {
            throw new ValidationException("Source warehouse ID cannot be null.");
        }
        if (shipments == null || shipments.isEmpty()) {
            throw new ValidationException("Trip must contain at least one shipment.");
        }
        if (vehicle == null) {
            throw new ValidationException("Vehicle cannot be null.");
        }

        // Validate all shipments belong to this warehouse and have allocated crates
        List<String> shipmentIds = new ArrayList<>();
        List<String> allCrateIds = new ArrayList<>();
        int totalCrates = 0;

        for (Shipment s : shipments) {
            shipmentValidator.validateForDispatch(s);
            if (!s.getSourceWarehouseId().equals(sourceWarehouseId)) {
                throw new ValidationException(String.format("Shipment %s source warehouse (%s) does not match trip source (%s).",
                        s.getShipmentId(), s.getSourceWarehouseId(), sourceWarehouseId));
            }
            shipmentIds.add(s.getShipmentId());
            allCrateIds.addAll(s.getAllocatedCrateIds());
            totalCrates += s.getRequiredCrates();
        }

        if (totalCrates > Vehicle.MAX_CRATE_CAPACITY) {
            throw new ValidationException(String.format("Combined crate count (%d) exceeds vehicle max capacity (%d).",
                    totalCrates, Vehicle.MAX_CRATE_CAPACITY));
        }

        String tripId = IdGenerator.nextId("TRP");
        VehicleTrip trip = new VehicleTrip(tripId, vehicle.getVehicleId(), sourceWarehouseId, shipmentIds, allCrateIds, totalCrates);

        // Associate trip with vehicle and crates
        vehicle.setCurrentTripId(tripId);
        vehicle.transitionStatus(VehicleStatus.LOADING);

        for (String cId : allCrateIds) {
            Crate c = crateService.findCrateById(cId).orElse(null);
            if (c != null) {
                c.setCurrentTripId(tripId);
                c.transitionStatus(CrateStatus.LOADED);
            }
        }

        for (Shipment s : shipments) {
            s.setAssignedTripId(tripId);
        }

        // Generate combined route
        Warehouse wh = dataStore.getWarehouses().get(sourceWarehouseId);
        Route route = routeService.generateRoute(wh, shipments, LocalDateTime.now());
        trip.setRoute(route);

        dataStore.getVehicleTrips().put(tripId, trip);
        dataStore.getWarehouseInventories().get(sourceWarehouseId).addActiveTrip(tripId);

        return trip;
    }

    public void dispatchTrip(String tripId) {
        VehicleTrip trip = findTripById(tripId)
                .orElseThrow(() -> new ValidationException("Trip " + tripId + " not found."));

        if (trip.getStatus() != TripStatus.PLANNED) {
            throw new ValidationException("Trip " + tripId + " cannot be dispatched. Status: " + trip.getStatus());
        }

        Vehicle vehicle = vehicleService.findVehicleById(trip.getVehicleId())
                .orElseThrow(() -> new ValidationException("Vehicle not found."));

        trip.transitionStatus(TripStatus.DISPATCHED);
        vehicle.transitionStatus(VehicleStatus.IN_TRANSIT);

        for (String cId : trip.getCrateIds()) {
            Crate c = crateService.findCrateById(cId).orElse(null);
            if (c != null) {
                c.transitionStatus(CrateStatus.IN_TRANSIT);
            }
        }

        for (String sId : trip.getShipmentIds()) {
            Shipment s = dataStore.getShipments().get(sId);
            if (s != null) {
                s.transitionStatus(ShipmentStatus.IN_TRANSIT);
            }
        }
    }

    public Optional<VehicleTrip> findTripById(String tripId) {
        if (tripId == null) return Optional.empty();
        return Optional.ofNullable(dataStore.getVehicleTrips().get(tripId));
    }

    public List<VehicleTrip> getAllTrips() {
        return new ArrayList<>(dataStore.getVehicleTrips().values());
    }

    public List<VehicleTrip> getActiveTrips() {
        return dataStore.getVehicleTrips().values().stream()
                .filter(t -> t.getStatus() == TripStatus.PLANNED ||
                             t.getStatus() == TripStatus.DISPATCHED ||
                             t.getStatus() == TripStatus.IN_TRANSIT)
                .toList();
    }
}
