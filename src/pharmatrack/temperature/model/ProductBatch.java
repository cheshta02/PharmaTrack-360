package pharmatrack.temperature.model;

import pharmatrack.json.JsonCreator;
import pharmatrack.json.JsonProperty;
import pharmatrack.common.exception.ValidationException;
import pharmatrack.common.util.InputValidator;

import java.time.LocalDate;
import java.util.Objects;

public class ProductBatch {
    private final String batchId;
    private final String productId;
    private LocalDate mfgDate;
    private LocalDate expiryDate;
    private int totalQuantity;
    private int reservedQuantity;
    private String warehouseId;
    private boolean blocked;

    @JsonCreator
    public ProductBatch(
            @JsonProperty("batchId") String batchId,
            @JsonProperty("productId") String productId,
            @JsonProperty("mfgDate") LocalDate mfgDate,
            @JsonProperty("expiryDate") LocalDate expiryDate,
            @JsonProperty("totalQuantity") int totalQuantity,
            @JsonProperty("reservedQuantity") int reservedQuantity,
            @JsonProperty("warehouseId") String warehouseId,
            @JsonProperty("blocked") boolean blocked) {
        this.batchId = InputValidator.requireNonEmpty(batchId, "Batch ID");
        this.productId = InputValidator.requireNonEmpty(productId, "Product ID");
        this.mfgDate = mfgDate != null ? mfgDate : LocalDate.now().minusMonths(1);
        this.expiryDate = expiryDate != null ? expiryDate : LocalDate.now().plusMonths(12);
        if (this.expiryDate.isBefore(this.mfgDate)) {
            throw new ValidationException("Expiry date cannot be before manufacturing date.");
        }
        if (totalQuantity < 0) {
            throw new ValidationException("Total quantity cannot be negative.");
        }
        if (reservedQuantity < 0 || reservedQuantity > totalQuantity) {
            throw new ValidationException("Reserved quantity must be between 0 and total quantity.");
        }
        this.totalQuantity = totalQuantity;
        this.reservedQuantity = reservedQuantity;
        this.warehouseId = InputValidator.requireNonEmpty(warehouseId, "Warehouse ID");
        this.blocked = blocked;
    }

    public ProductBatch(String batchId, String productId, LocalDate mfgDate, LocalDate expiryDate,
                        int totalQuantity, String warehouseId) {
        this(batchId, productId, mfgDate, expiryDate, totalQuantity, 0, warehouseId, false);
    }

    public String getBatchId() {
        return batchId;
    }

    public String getProductId() {
        return productId;
    }

    public LocalDate getMfgDate() {
        return mfgDate;
    }

    public void setMfgDate(LocalDate mfgDate) {
        this.mfgDate = mfgDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        if (expiryDate.isBefore(this.mfgDate)) {
            throw new ValidationException("Expiry date cannot be before manufacturing date.");
        }
        this.expiryDate = expiryDate;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public int getReservedQuantity() {
        return reservedQuantity;
    }

    public synchronized int getAvailableQuantity() {
        return Math.max(0, totalQuantity - reservedQuantity);
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(String warehouseId) {
        this.warehouseId = InputValidator.requireNonEmpty(warehouseId, "Warehouse ID");
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public boolean isExpired() {
        return LocalDate.now().isAfter(expiryDate);
    }

    public boolean isEligibleForAllocation(int requestedQty) {
        return !blocked && !isExpired() && getAvailableQuantity() >= requestedQty;
    }

    public synchronized void reserve(int quantity) {
        InputValidator.requirePositive(quantity, "Reservation quantity");
        if (quantity > getAvailableQuantity()) {
            throw new ValidationException(String.format(
                    "Cannot reserve %d units from batch %s. Only %d units available (Total: %d, Reserved: %d).",
                    quantity, batchId, getAvailableQuantity(), totalQuantity, reservedQuantity));
        }
        this.reservedQuantity += quantity;
    }

    public synchronized void releaseReservation(int quantity) {
        InputValidator.requirePositive(quantity, "Release quantity");
        if (quantity > this.reservedQuantity) {
            throw new ValidationException(String.format(
                    "Cannot release %d units from batch %s. Currently reserved: %d.",
                    quantity, batchId, this.reservedQuantity));
        }
        this.reservedQuantity -= quantity;
    }

    public synchronized void deduct(int quantity) {
        InputValidator.requirePositive(quantity, "Deduct quantity");
        if (quantity > this.totalQuantity) {
            throw new ValidationException(String.format(
                    "Cannot deduct %d units from batch %s. Total stock: %d.",
                    quantity, batchId, this.totalQuantity));
        }
        this.totalQuantity -= quantity;
        if (this.reservedQuantity >= quantity) {
            this.reservedQuantity -= quantity;
        } else {
            this.reservedQuantity = 0;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductBatch that)) return false;
        return Objects.equals(batchId, that.batchId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(batchId);
    }

    @Override
    public String toString() {
        return String.format("%s: Product %s at %s [Avail: %d, Res: %d, Total: %d, Exp: %s, Blocked: %s]", batchId, productId, warehouseId, getAvailableQuantity(), reservedQuantity, totalQuantity, expiryDate, blocked);
    }
}
