package pharmatrack.logistics.service;

import pharmatrack.common.exception.ValidationException;
import pharmatrack.common.util.IdGenerator;
import pharmatrack.logistics.model.*;
import pharmatrack.storage.InMemoryDataStore;
import pharmatrack.temperature.model.Crate;
import pharmatrack.temperature.model.CrateStatus;
import pharmatrack.temperature.service.CrateService;
import pharmatrack.temperature.service.ProductBatchService;

import java.time.LocalDateTime;

public class DeliveryService {
    private final InMemoryDataStore dataStore;
    private final CrateService crateService;
    private final ProductBatchService batchService;
    private final VehicleService vehicleService;

    public DeliveryService(InMemoryDataStore dataStore, CrateService crateService, ProductBatchService batchService, VehicleService vehicleService) {
        this.dataStore = dataStore;
        this.crateService = crateService;
        this.batchService = batchService;
        this.vehicleService = vehicleService;
    }

    public void deliverShipment(String shipmentId, LocalDateTime deliveryTime) {
        if (deliveryTime == null) {
            deliveryTime = LocalDateTime.now();
        }

        Shipment shipment = dataStore.getShipments().get(shipmentId);
        if (shipment == null) {
            throw new ValidationException("Shipment " + shipmentId + " not found.");
        }

        if (shipment.getStatus() == ShipmentStatus.DELIVERED) {
            return; 
        }

        shipment.transitionStatus(ShipmentStatus.DELIVERED);
        shipment.setDeliveryTime(deliveryTime);

        if (shipment.getBatchId() != null) {
            batchService.deductBatchStock(shipment.getBatchId(), shipment.getRequiredQuantity());
        }

        for (String cId : shipment.getAllocatedCrateIds()) {
            Crate c = crateService.findCrateById(cId).orElse(null);
            if (c != null) {
                c.transitionStatus(CrateStatus.RETURNING);
            }
        }

        ShipmentEvent evt = new ShipmentEvent(
                IdGenerator.nextId("EVT"), "ShipmentDeliveredEvent",
                shipmentId, shipment.getAssignedTripId(), null,
                String.format("Shipment %s delivered at %s. Crates entered RETURNING lifecycle.",
                        shipmentId, shipment.getDestinationCity()));
        dataStore.addEvent(evt);

        String tripId = shipment.getAssignedTripId();
        if (tripId != null && dataStore.getVehicleTrips().containsKey(tripId)) {
            VehicleTrip trip = dataStore.getVehicleTrips().get(tripId);
            boolean allDelivered = trip.getShipmentIds().stream().map(id -> dataStore.getShipments().get(id)).allMatch(s -> s != null && s.getStatus() == ShipmentStatus.DELIVERED);

            if (allDelivered) {
                if (trip.getStatus() == TripStatus.IN_TRANSIT || trip.getStatus() == TripStatus.DISPATCHED) {
                    trip.transitionStatus(TripStatus.DELIVERED_ALL);
                }
                Vehicle v = vehicleService.findVehicleById(trip.getVehicleId()).orElse(null);
                if (v != null && v.getStatus() == VehicleStatus.IN_TRANSIT) {
                    v.transitionStatus(VehicleStatus.RETURNING);
                }
            }
        }
    }
}
