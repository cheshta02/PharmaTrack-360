package pharmatrack.temperature.model;

import pharmatrack.json.JsonCreator;
import pharmatrack.json.JsonProperty;
import pharmatrack.common.util.InputValidator;

import java.util.Objects;

public class PharmaceuticalProduct {
    private final String productId;
    private String name;
    private String category;
    private String description;
    private String policyId;
    private boolean active;

    @JsonCreator
    public PharmaceuticalProduct(
            @JsonProperty("productId") String productId,
            @JsonProperty("name") String name,
            @JsonProperty("category") String category,
            @JsonProperty("description") String description,
            @JsonProperty("policyId") String policyId,
            @JsonProperty("active") boolean active) {
        this.productId = InputValidator.requireNonEmpty(productId, "Product ID");
        this.name = InputValidator.requireNonEmpty(name, "Product Name");
        this.category = InputValidator.requireNonEmpty(category, "Category");
        this.description = description != null ? description.trim() : "";
        this.policyId = InputValidator.requireNonEmpty(policyId, "Policy ID");
        this.active = active;
    }

    public PharmaceuticalProduct(String productId, String name, String category, String description, String policyId) {
        this(productId, name, category, description, policyId, true);
    }

    public String getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = InputValidator.requireNonEmpty(name, "Product Name");
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = InputValidator.requireNonEmpty(category, "Category");
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPolicyId() {
        return policyId;
    }

    public void setPolicyId(String policyId) {
        this.policyId = InputValidator.requireNonEmpty(policyId, "Policy ID");
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PharmaceuticalProduct that)) return false;
        return Objects.equals(productId, that.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId);
    }

    @Override
    public String toString() {
        return String.format("%s: %s (%s) [Policy: %s, active=%s]", productId, name, category, policyId, active);
    }
}
