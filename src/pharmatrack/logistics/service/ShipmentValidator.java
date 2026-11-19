package pharmatrack.logistics.service;

import pharmatrack.common.exception.ValidationException;
import pharmatrack.common.util.InputValidator;
import pharmatrack.logistics.model.Shipment;
import pharmatrack.logistics.model.Vehicle;
import pharmatrack.storage.InMemoryDataStore;
import pharmatrack.temperature.model.PharmaceuticalProduct;
import pharmatrack.temperature.model.ProductBatch;

import java.time.LocalDateTime;

/**
 * Validates customer shipment registration requirements, batches, stock availability, and crate constraints.
 */
public class ShipmentValidator {
    private final InMemoryDataStore dataStore;

    public ShipmentValidator(InMemoryDataStore dataStore) {
        this.dataStore = dataStore;
    }

    public void validateRegistration(String orderRef, String destinationCity, String productId,
                                     String batchId, int requiredQuantity, int requiredCrates,
                                     LocalDateTime deadline) {
        InputValidator.requireNonEmpty(orderRef, "Order Reference");
        InputValidator.requireNonEmpty(destinationCity, "Destination City");
        InputValidator.requireNonEmpty(productId, "Product ID");
        InputValidator.requirePositive(requiredQuantity, "Required Quantity");
        InputValidator.requirePositive(requiredCrates, "Required Crates");

        PharmaceuticalProduct product = dataStore.getProducts().get(productId);
        if (product == null) {
            throw new ValidationException("Product " + productId + " does not exist.");
        }
        if (!product.isActive()) {
            throw new ValidationException("Product " + productId + " is inactive.");
        }

        if (batchId != null && !batchId.trim().isEmpty()) {
            ProductBatch batch = dataStore.getBatches().get(batchId);
            if (batch == null) {
                throw new ValidationException("Batch " + batchId + " does not exist.");
            }
            if (!batch.getProductId().equals(productId)) {
                throw new ValidationException("Batch " + batchId + " does not belong to product " + productId);
            }
            if (batch.isBlocked()) {
                throw new ValidationException("Batch " + batchId + " is blocked.");
            }
            if (batch.isExpired()) {
                throw new ValidationException("Batch " + batchId + " is expired.");
            }
            if (batch.getAvailableQuantity() < requiredQuantity) {
                throw new ValidationException(String.format(
                        "Insufficient stock in batch %s: requested %d, available %d.",
                        batchId, requiredQuantity, batch.getAvailableQuantity()));
            }
        }

        if (deadline != null && deadline.isBefore(LocalDateTime.now())) {
            throw new ValidationException("Delivery deadline cannot be in the past.");
        }
    }

    public void validateForDispatch(Shipment shipment) {
        if (shipment == null) {
            throw new ValidationException("Shipment cannot be null.");
        }
        if (shipment.getSourceWarehouseId() == null) {
            throw new ValidationException("Shipment " + shipment.getShipmentId() + " has no source warehouse assigned.");
        }
        if (shipment.getBatchId() == null) {
            throw new ValidationException("Shipment " + shipment.getShipmentId() + " has no batch allocated.");
        }
        if (shipment.getAllocatedCrateIds().isEmpty()) {
            throw new ValidationException("Shipment " + shipment.getShipmentId() + " has no crates allocated.");
        }
        if (shipment.getAllocatedCrateIds().size() != shipment.getRequiredCrates()) {
            throw new ValidationException(String.format(
                    "Shipment %s requires %d crates but has %d allocated.",
                    shipment.getShipmentId(), shipment.getRequiredCrates(), shipment.getAllocatedCrateIds().size()));
        }
    }
}
