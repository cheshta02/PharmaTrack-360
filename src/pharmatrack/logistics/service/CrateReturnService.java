package pharmatrack.logistics.service;

import pharmatrack.common.exception.ValidationException;
import pharmatrack.common.util.IdGenerator;
import pharmatrack.logistics.model.ShipmentEvent;
import pharmatrack.logistics.model.TripStatus;
import pharmatrack.logistics.model.Vehicle;
import pharmatrack.logistics.model.VehicleStatus;
import pharmatrack.logistics.model.VehicleTrip;
import pharmatrack.storage.InMemoryDataStore;
import pharmatrack.temperature.model.Crate;
import pharmatrack.temperature.model.CrateStatus;
import pharmatrack.temperature.service.CrateService;

import java.time.LocalDateTime;

public class CrateReturnService {
    private final InMemoryDataStore dataStore;
    private final CrateService crateService;
    private final VehicleService vehicleService;

    public CrateReturnService(InMemoryDataStore dataStore, CrateService crateService, VehicleService vehicleService) {
        this.dataStore = dataStore;
        this.crateService = crateService;
        this.vehicleService = vehicleService;
    }

    public void processCrateInspection(String crateId) {
        Crate crate = crateService.findCrateById(crateId)
                .orElseThrow(() -> new ValidationException("Crate " + crateId + " not found."));

        if (crate.getStatus() == CrateStatus.RETURNING) {
            crate.transitionStatus(CrateStatus.INSPECTION);
        }
        if (crate.getStatus() == CrateStatus.INSPECTION) {
            crate.transitionStatus(CrateStatus.AVAILABLE);
            crate.setCurrentShipmentId(null);
            crate.setCurrentTripId(null);

            ShipmentEvent evt = new ShipmentEvent(
                    IdGenerator.nextId("EVT"), "CrateReturnedEvent",
                    null, null, crateId,
                    "Crate " + crateId + " successfully inspected, sanitized, and returned to AVAILABLE status.");
            dataStore.addEvent(evt);
        }
    }

    public void completeVehicleTripReturn(String tripId) {
        VehicleTrip trip = dataStore.getVehicleTrips().get(tripId);
        if (trip == null) {
            throw new ValidationException("Trip " + tripId + " not found.");
        }

        if (trip.getStatus() == TripStatus.DELIVERED_ALL) {
            trip.transitionStatus(TripStatus.COMPLETED);
        }

        Vehicle v = vehicleService.findVehicleById(trip.getVehicleId()).orElse(null);
        if (v != null && (v.getStatus() == VehicleStatus.RETURNING || v.getStatus() == VehicleStatus.IN_TRANSIT)) {
            v.transitionStatus(VehicleStatus.AVAILABLE);
            v.setLastKnownLocation(trip.getSourceWarehouseId());
        }

        for (String cId : trip.getCrateIds()) {
            processCrateInspection(cId);
        }

        dataStore.getWarehouseInventories().get(trip.getSourceWarehouseId()).removeActiveTrip(tripId);
    }
}
