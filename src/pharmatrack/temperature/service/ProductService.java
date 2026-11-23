package pharmatrack.temperature.service;

import pharmatrack.common.exception.ValidationException;
import pharmatrack.common.util.IdGenerator;
import pharmatrack.common.util.InputValidator;
import pharmatrack.storage.InMemoryDataStore;
import pharmatrack.temperature.model.PharmaceuticalProduct;
import pharmatrack.temperature.model.TemperaturePolicy;

import java.util.ArrayList;
import java.util.*;

public class ProductService {
    private final InMemoryDataStore dataStore;
    private final TemperaturePolicyService policyService;

    public ProductService(InMemoryDataStore dataStore, TemperaturePolicyService policyService) {
        this.dataStore = dataStore;
        this.policyService = policyService;
    }

    public PharmaceuticalProduct registerProduct(String productId, String name, String category,String description, String policyId) {
        if (productId == null || productId.trim().isEmpty()) {
            productId = IdGenerator.nextId("PRD");
        }
        if (dataStore.getProducts().containsKey(productId)) {
            throw new ValidationException("Product with ID " + productId + " already exists.");
        }

        TemperaturePolicy policy = policyService.findPolicyById(policyId)
                .orElseThrow(() -> new ValidationException("Cannot assign invalid Policy ID: " + policyId));
        if (!policy.isActive()) {
            throw new ValidationException("Cannot assign inactive Policy: " + policyId);
        }

        PharmaceuticalProduct product = new PharmaceuticalProduct(productId, name, category, description, policyId);
        dataStore.getProducts().put(productId, product);
        return product;
    }

    public Optional<PharmaceuticalProduct> findProductById(String productId) {
        if (productId == null) return Optional.empty();
        return Optional.ofNullable(dataStore.getProducts().get(productId));
    }

    public List<PharmaceuticalProduct> searchProducts(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllProducts();
        }
        String lowerQuery = query.trim().toLowerCase();
        return dataStore.getProducts().values().stream().filter(p -> p.getProductId().toLowerCase().contains(lowerQuery) ||
        p.getName().toLowerCase().contains(lowerQuery) || p.getCategory().toLowerCase().contains(lowerQuery)).toList();
    }

    public List<PharmaceuticalProduct> getAllProducts() {
        return new ArrayList<>(dataStore.getProducts().values());
    }

    public PharmaceuticalProduct updateProduct(String productId, String name, String category, String description, String policyId) {
        PharmaceuticalProduct product = findProductById(productId)
                .orElseThrow(() -> new ValidationException("Product with ID " + productId + " not found."));

        if (policyId != null && !policyId.equals(product.getPolicyId())) {
            TemperaturePolicy policy = policyService.findPolicyById(policyId)
                    .orElseThrow(() -> new ValidationException("Cannot assign invalid Policy ID: " + policyId));
            if (!policy.isActive()) {
                throw new ValidationException("Cannot assign inactive Policy: " + policyId);
            }
            product.setPolicyId(policyId);
        }

        product.setName(name);
        product.setCategory(category);
        product.setDescription(description);
        return product;
    }

    public void deactivateProduct(String productId) {
        PharmaceuticalProduct product = findProductById(productId)
                .orElseThrow(() -> new ValidationException("Product with ID " + productId + " not found."));
        product.setActive(false);
    }
}
