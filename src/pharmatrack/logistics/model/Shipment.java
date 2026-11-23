package pharmatrack.logistics.model;

import pharmatrack.json.JsonCreator;
import pharmatrack.json.JsonProperty;
import pharmatrack.common.exception.InvalidStateTransitionException;
import pharmatrack.common.util.InputValidator;

import java.time.LocalDateTime;
import java.util.*;

public class Shipment {
    private final String shipmentId;
    private final String orderRef;
    private final String destinationCity;
    private double destLat;
    private double destLon;
    private final String productId;
    private String batchId;
    private final int requiredQuantity;
    private final int requiredCrates;
    private final ShipmentPriority priority;
    private final LocalDateTime expectedDeliveryDeadline;
    private String sourceWarehouseId;
    private ShipmentStatus status;
    private final LocalDateTime registrationTime;
    private LocalDateTime deliveryTime;
    private final List<String> allocatedCrateIds = new ArrayList<>();
    private String assignedTripId;

    @JsonCreator
    public Shipment(
            @JsonProperty("shipmentId") String shipmentId,
            @JsonProperty("orderRef") String orderRef,
            @JsonProperty("destinationCity") String destinationCity,
            @JsonProperty("destLat") double destLat,
            @JsonProperty("destLon") double destLon,
            @JsonProperty("productId") String productId,
            @JsonProperty("batchId") String batchId,
            @JsonProperty("requiredQuantity") int requiredQuantity,
            @JsonProperty("requiredCrates") int requiredCrates,
            @JsonProperty("priority") ShipmentPriority priority,
            @JsonProperty("expectedDeliveryDeadline") LocalDateTime expectedDeliveryDeadline,
            @JsonProperty("sourceWarehouseId") String sourceWarehouseId,
            @JsonProperty("status") ShipmentStatus status,
            @JsonProperty("registrationTime") LocalDateTime registrationTime,
            @JsonProperty("deliveryTime") LocalDateTime deliveryTime,
            @JsonProperty("allocatedCrateIds") List<String> allocatedCrateIds,
            @JsonProperty("assignedTripId") String assignedTripId) {
        this.shipmentId = InputValidator.requireNonEmpty(shipmentId, "Shipment ID");
        this.orderRef = InputValidator.requireNonEmpty(orderRef, "Order Reference");
        this.destinationCity = InputValidator.requireNonEmpty(destinationCity, "Destination City");
        this.destLat = destLat;
        this.destLon = destLon;
        this.productId = InputValidator.requireNonEmpty(productId, "Product ID");
        this.batchId = batchId;
        this.requiredQuantity = InputValidator.requirePositive(requiredQuantity, "Required Quantity");
        this.requiredCrates = InputValidator.requirePositive(requiredCrates, "Required Crates");
        this.priority = priority != null ? priority : ShipmentPriority.NORMAL;
        this.expectedDeliveryDeadline = expectedDeliveryDeadline != null ? expectedDeliveryDeadline : LocalDateTime.now().plusDays(2);
        this.sourceWarehouseId = sourceWarehouseId;
        this.status = status != null ? status : ShipmentStatus.PENDING;
        this.registrationTime = registrationTime != null ? registrationTime : LocalDateTime.now();
        this.deliveryTime = deliveryTime;
        if (allocatedCrateIds != null) {
            this.allocatedCrateIds.addAll(allocatedCrateIds);
        }
        this.assignedTripId = assignedTripId;
    }

    public Shipment(String shipmentId, String orderRef, String destinationCity, double destLat, double destLon, String productId,     String batchId, int requiredQuantity, int requiredCrates, ShipmentPriority priority, LocalDateTime expectedDeliveryDeadline) {
        this(shipmentId, orderRef, destinationCity, destLat, destLon, productId, batchId, requiredQuantity,
                requiredCrates, priority, expectedDeliveryDeadline, null, ShipmentStatus.PENDING,
                LocalDateTime.now(), null, null, null);
    }

    public String getShipmentId() {
        return shipmentId;
    }

    public String getOrderRef() {
        return orderRef;
    }

    public String getDestinationCity() {
        return destinationCity;
    }

    public double getDestLat() {
        return destLat;
    }

    public void setDestLat(double destLat) {
        this.destLat = destLat;
    }

    public double getDestLon() {
        return destLon;
    }

    public void setDestLon(double destLon) {
        this.destLon = destLon;
    }

    public String getProductId() {
        return productId;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public int getRequiredQuantity() {
        return requiredQuantity;
    }

    public int getRequiredCrates() {
        return requiredCrates;
    }

    public ShipmentPriority getPriority() {
        return priority;
    }

    public LocalDateTime getExpectedDeliveryDeadline() {
        return expectedDeliveryDeadline;
    }

    public String getSourceWarehouseId() {
        return sourceWarehouseId;
    }

    public void setSourceWarehouseId(String sourceWarehouseId) {
        this.sourceWarehouseId = sourceWarehouseId;
    }

    public ShipmentStatus getStatus() {
        return status;
    }

    public LocalDateTime getRegistrationTime() {
        return registrationTime;
    }

    public LocalDateTime getDeliveryTime() {
        return deliveryTime;
    }

    public void setDeliveryTime(LocalDateTime deliveryTime) {
        this.deliveryTime = deliveryTime;
    }

    public List<String> getAllocatedCrateIds() {
        return Collections.unmodifiableList(allocatedCrateIds);
    }

    public synchronized void setAllocatedCrates(List<String> crateIds) {
        this.allocatedCrateIds.clear();
        if (crateIds != null) {
            this.allocatedCrateIds.addAll(crateIds);
        }
    }

    public String getAssignedTripId() {
        return assignedTripId;
    }

    public void setAssignedTripId(String assignedTripId) {
        this.assignedTripId = assignedTripId;
    }

    public synchronized void transitionStatus(ShipmentStatus newStatus) {
        if (this.status == newStatus) {
            return;
        }

        boolean valid = switch (this.status) {
            case PENDING -> newStatus == ShipmentStatus.ALLOCATED || newStatus == ShipmentStatus.CANCELLED;
            case ALLOCATED -> newStatus == ShipmentStatus.IN_TRANSIT || newStatus == ShipmentStatus.PENDING || newStatus == ShipmentStatus.CANCELLED;
            case IN_TRANSIT -> newStatus == ShipmentStatus.DELIVERED;
            case DELIVERED -> false;
            case CANCELLED -> false;
        };

        if (!valid) {
            throw new InvalidStateTransitionException(String.format(
                "Invalid shipment lifecycle transition for shipment %s: %s -> %s.",
                shipmentId, this.status, newStatus));
        }

        this.status = newStatus;
        if (newStatus == ShipmentStatus.DELIVERED) {
            this.deliveryTime = LocalDateTime.now();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Shipment shipment)) return false;
        return Objects.equals(shipmentId, shipment.shipmentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shipmentId);
    }

    @Override
    public String toString() {
        return String.format("%s (%s) -> %s [Priority: %s, Product: %s, Batch: %s, Qty: %d, Crates: %d, Status: %s, Source: %s, Trip: %s]",
                shipmentId, orderRef, destinationCity, priority, productId,
                batchId != null ? batchId : "Unassigned", requiredQuantity, requiredCrates,
                status, sourceWarehouseId != null ? sourceWarehouseId : "Unselected",
                assignedTripId != null ? assignedTripId : "None");
    }
}
