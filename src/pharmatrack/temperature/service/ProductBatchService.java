package pharmatrack.temperature.service;

import pharmatrack.common.exception.ResourceUnavailableException;
import pharmatrack.common.exception.ValidationException;
import pharmatrack.common.util.IdGenerator;
import pharmatrack.common.util.InputValidator;
import pharmatrack.storage.InMemoryDataStore;
import pharmatrack.temperature.model.PharmaceuticalProduct;
import pharmatrack.temperature.model.ProductBatch;

import java.time.LocalDate;
import java.util.*;

public class ProductBatchService {
    private final InMemoryDataStore dataStore;
    private final ProductService productService;

    public ProductBatchService(InMemoryDataStore dataStore, ProductService productService) {
        this.dataStore = dataStore;
        this.productService = productService;
    }

    public ProductBatch registerBatch(String batchId, String productId, LocalDate mfgDate, LocalDate expiryDate,int totalQuantity, String warehouseId) {
        if (batchId == null || batchId.trim().isEmpty()) {
            batchId = IdGenerator.nextId("BAT");
        }
        if (dataStore.getBatches().containsKey(batchId)) {
            throw new ValidationException("Batch with ID " + batchId + " already exists.");
        }

        PharmaceuticalProduct product = productService.findProductById(productId)
                .orElseThrow(() -> new ValidationException("Cannot register batch for non-existent product: " + productId));
        if (!product.isActive()) {
            throw new ValidationException("Cannot register batch for inactive product: " + productId);
        }

        InputValidator.requirePositive(totalQuantity, "Total Quantity");
        ProductBatch batch = new ProductBatch(batchId, productId, mfgDate, expiryDate, totalQuantity, warehouseId);
        dataStore.getBatches().put(batchId, batch);

        if (dataStore.getWarehouseInventories().containsKey(warehouseId)) {
            dataStore.getWarehouseInventories().get(warehouseId).addBatch(batchId);
        }

        return batch;
    }

    public Optional<ProductBatch> findBatchById(String batchId) {
        if (batchId == null) return Optional.empty();
        return Optional.ofNullable(dataStore.getBatches().get(batchId));
    }

    public List<ProductBatch> getBatchesByProduct(String productId) {
        return dataStore.getBatches().values().stream().filter(b -> b.getProductId().equals(productId)).toList();
    }

    public List<ProductBatch> getBatchesByWarehouse(String warehouseId) {
        return dataStore.getBatches().values().stream().filter(b -> b.getWarehouseId().equals(warehouseId)).toList();
    }

    public List<ProductBatch> getAllBatches() {
        return new ArrayList<>(dataStore.getBatches().values());
    }

    public void reserveBatchStock(String batchId, int quantity) {
        ProductBatch batch = findBatchById(batchId).orElseThrow(() -> new ValidationException("Batch with ID " + batchId + " not found."));

        if (batch.isBlocked()) {
            throw new ResourceUnavailableException("Batch " + batchId + " is blocked and cannot be allocated.");
        }
        if (batch.isExpired()) {
            throw new ResourceUnavailableException("Batch " + batchId + " is expired (Expiry: " + batch.getExpiryDate() + ").");
        }
        batch.reserve(quantity);
    }

    public void releaseBatchStock(String batchId, int quantity) {
        ProductBatch batch = findBatchById(batchId)
                .orElseThrow(() -> new ValidationException("Batch with ID " + batchId + " not found."));
        batch.releaseReservation(quantity);
    }

    public void deductBatchStock(String batchId, int quantity) {
        ProductBatch batch = findBatchById(batchId)
                .orElseThrow(() -> new ValidationException("Batch with ID " + batchId + " not found."));
        batch.deduct(quantity);
    }

    public void setBatchBlocked(String batchId, boolean blocked) {
        ProductBatch batch = findBatchById(batchId)
                .orElseThrow(() -> new ValidationException("Batch with ID " + batchId + " not found."));
        batch.setBlocked(blocked);
    }
}
