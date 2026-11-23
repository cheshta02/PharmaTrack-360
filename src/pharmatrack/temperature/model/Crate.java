package pharmatrack.temperature.model;

import pharmatrack.json.JsonCreator;
import pharmatrack.json.JsonProperty;
import pharmatrack.common.exception.InvalidStateTransitionException;
import pharmatrack.common.util.InputValidator;

import java.util.Objects;

public class Crate {
    private final String crateId;
    private double minTempCapability;
    private double maxTempCapability;
    private double capacityLiters;
    private String warehouseId;
    private String assignedSensorId;
    private CrateStatus status;
    private String currentTripId;
    private String currentShipmentId;

    @JsonCreator
    public Crate(
            @JsonProperty("crateId") String crateId,
            @JsonProperty("minTempCapability") double minTempCapability,
            @JsonProperty("maxTempCapability") double maxTempCapability,
            @JsonProperty("capacityLiters") double capacityLiters,
            @JsonProperty("warehouseId") String warehouseId,
            @JsonProperty("assignedSensorId") String assignedSensorId,
            @JsonProperty("status") CrateStatus status,
            @JsonProperty("currentTripId") String currentTripId,
            @JsonProperty("currentShipmentId") String currentShipmentId) {
        this.crateId = InputValidator.requireNonEmpty(crateId, "Crate ID");
        InputValidator.requireValidTemperatureRange(minTempCapability, maxTempCapability);
        this.minTempCapability = minTempCapability;
        this.maxTempCapability = maxTempCapability;
        this.capacityLiters = capacityLiters > 0 ? capacityLiters : 50.0;
        this.warehouseId = InputValidator.requireNonEmpty(warehouseId, "Warehouse ID");
        this.assignedSensorId = assignedSensorId != null ? assignedSensorId.trim() : null;
        this.status = status != null ? status : CrateStatus.AVAILABLE;
        this.currentTripId = currentTripId;
        this.currentShipmentId = currentShipmentId;
    }

    public Crate(String crateId, double minTempCapability, double maxTempCapability, double capacityLiters, String warehouseId) {
        this(crateId, minTempCapability, maxTempCapability, capacityLiters, warehouseId, null, CrateStatus.AVAILABLE, null, null);
    }

    public String getCrateId() {
        return crateId;
    }

    public double getMinTempCapability() {
        return minTempCapability;
    }

    public double getMaxTempCapability() {
        return maxTempCapability;
    }

    public void setTemperatureCapabilities(double minTempCapability, double maxTempCapability) {
        InputValidator.requireValidTemperatureRange(minTempCapability, maxTempCapability);
        this.minTempCapability = minTempCapability;
        this.maxTempCapability = maxTempCapability;
    }

    public double getCapacityLiters() {
        return capacityLiters;
    }

    public void setCapacityLiters(double capacityLiters) {
        InputValidator.requirePositive(capacityLiters, "Capacity Liters");
        this.capacityLiters = capacityLiters;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(String warehouseId) {
        this.warehouseId = InputValidator.requireNonEmpty(warehouseId, "Warehouse ID");
    }

    public String getAssignedSensorId() {
        return assignedSensorId;
    }

    public void setAssignedSensorId(String assignedSensorId) {
        this.assignedSensorId = assignedSensorId;
    }

    public CrateStatus getStatus() {
        return status;
    }

    public String getCurrentTripId() {
        return currentTripId;
    }

    public void setCurrentTripId(String currentTripId) {
        this.currentTripId = currentTripId;
    }

    public String getCurrentShipmentId() {
        return currentShipmentId;
    }

    public void setCurrentShipmentId(String currentShipmentId) {
        this.currentShipmentId = currentShipmentId;
    }

    public boolean isCompatibleWith(TemperaturePolicy policy) {
        if (policy == null || !policy.isActive()) {
            return false;
        }
        return this.minTempCapability <= policy.getMinTemp() && this.maxTempCapability >= policy.getMaxTemp();
    }

    public synchronized void transitionStatus(CrateStatus newStatus) {
        if (this.status == newStatus) {
            return;
        }
        boolean valid = switch (this.status) {
            case AVAILABLE -> newStatus == CrateStatus.ALLOCATED;
            case ALLOCATED -> newStatus == CrateStatus.LOADED || newStatus == CrateStatus.AVAILABLE; // Release if shipment cancelled
            case LOADED -> newStatus == CrateStatus.IN_TRANSIT || newStatus == CrateStatus.ALLOCATED;
            case IN_TRANSIT -> newStatus == CrateStatus.RETURNING;
            case RETURNING -> newStatus == CrateStatus.INSPECTION;
            case INSPECTION -> newStatus == CrateStatus.AVAILABLE;
        };

        if (!valid) {
            throw new InvalidStateTransitionException(String.format(
                    "Invalid crate lifecycle transition for crate %s: %s -> %s.",
                    crateId, this.status, newStatus));
        }

        this.status = newStatus;
        if (newStatus == CrateStatus.AVAILABLE) {
            this.currentTripId = null;
            this.currentShipmentId = null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Crate crate)) return false;
        return Objects.equals(crateId, crate.crateId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(crateId);
    }

    @Override
    public String toString() {
        return String.format("%s [Capability: %.1f°C to %.1f°C, Status: %s, WH: %s, Sensor: %s, Trip: %s, Shipment: %s]",
                crateId, minTempCapability, maxTempCapability, status, warehouseId,
                assignedSensorId != null ? assignedSensorId : "None",
                currentTripId != null ? currentTripId : "None",
                currentShipmentId != null ? currentShipmentId : "None");
    }
}
