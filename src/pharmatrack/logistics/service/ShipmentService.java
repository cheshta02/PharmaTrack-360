package pharmatrack.logistics.service;

import pharmatrack.common.exception.ValidationException;
import pharmatrack.common.util.IdGenerator;
import pharmatrack.logistics.model.Shipment;
import pharmatrack.logistics.model.ShipmentPriority;
import pharmatrack.logistics.model.ShipmentStatus;
import pharmatrack.storage.InMemoryDataStore;
import pharmatrack.temperature.service.ProductBatchService;

import java.time.LocalDateTime;
import java.util.*;

public class ShipmentService {
    private final InMemoryDataStore dataStore;
    private final ShipmentValidator validator;
    private final ProductBatchService batchService;

    public static final Comparator<Shipment> PRIORITY_COMPARATOR = Comparator.comparing((Shipment s) -> s.getPriority().getRank())
        .thenComparing(Shipment::getRegistrationTime);

    public ShipmentService(InMemoryDataStore dataStore, ShipmentValidator validator, ProductBatchService batchService) {
        this.dataStore = dataStore;
        this.validator = validator;
        this.batchService = batchService;
    }

    public Shipment registerShipment(String shipmentId, String orderRef, String destinationCity, double destLat, double destLon, String productId, String batchId, int requiredQuantity, int requiredCrates, ShipmentPriority priority, LocalDateTime deadline) {
        if (shipmentId == null || shipmentId.trim().isEmpty()) {
            shipmentId = IdGenerator.nextId("SH");
        }
        if (dataStore.getShipments().containsKey(shipmentId)) {
            throw new ValidationException("Shipment with ID " + shipmentId + " already exists.");
        }

        validator.validateRegistration(orderRef, destinationCity, productId, batchId, requiredQuantity, requiredCrates, deadline);

        Shipment shipment = new Shipment(shipmentId, orderRef, destinationCity, destLat, destLon, productId, batchId, requiredQuantity, requiredCrates, priority, deadline);

        dataStore.getShipments().put(shipmentId, shipment);
        return shipment;
    }

    public Optional<Shipment> findShipmentById(String shipmentId) {
        if (shipmentId == null) return Optional.empty();
        return Optional.ofNullable(dataStore.getShipments().get(shipmentId));
    }

    public List<Shipment> getAllShipments() {
        return new ArrayList<>(dataStore.getShipments().values());
    }

    public List<Shipment> getPendingShipmentsSortedByPriority() {
        PriorityQueue<Shipment> queue = new PriorityQueue<>(PRIORITY_COMPARATOR);
        dataStore.getShipments().values().stream().filter(s -> s.getStatus() == ShipmentStatus.PENDING).forEach(queue::offer);

        List<Shipment> sortedList = new ArrayList<>();
        while (!queue.isEmpty()) {
            sortedList.add(queue.poll());
        }
        return sortedList;
    }

    public void cancelShipment(String shipmentId) {
        Shipment shipment = findShipmentById(shipmentId)
                .orElseThrow(() -> new ValidationException("Shipment " + shipmentId + " not found."));

        if (shipment.getStatus() == ShipmentStatus.IN_TRANSIT || shipment.getStatus() == ShipmentStatus.DELIVERED) {
            throw new ValidationException("Cannot cancel shipment " + shipmentId + " because it is " + shipment.getStatus());
        }

        if (shipment.getBatchId() != null && shipment.getStatus() == ShipmentStatus.ALLOCATED) {
            batchService.releaseBatchStock(shipment.getBatchId(), shipment.getRequiredQuantity());
        }

        for (String crateId : shipment.getAllocatedCrateIds()) {
            var crate = dataStore.getCrates().get(crateId);
            if (crate != null) {
                crate.setCurrentShipmentId(null);
                crate.transitionStatus(pharmatrack.temperature.model.CrateStatus.AVAILABLE);
            }
        }
        shipment.setAllocatedCrates(new ArrayList<>());

        shipment.transitionStatus(ShipmentStatus.CANCELLED);
    }
}
