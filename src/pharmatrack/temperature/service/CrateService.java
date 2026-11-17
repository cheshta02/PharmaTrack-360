package pharmatrack.temperature.service;

import pharmatrack.common.exception.CompatibilityException;
import pharmatrack.common.exception.ValidationException;
import pharmatrack.common.util.IdGenerator;
import pharmatrack.common.util.InputValidator;
import pharmatrack.storage.InMemoryDataStore;
import pharmatrack.temperature.model.Crate;
import pharmatrack.temperature.model.CrateStatus;
import pharmatrack.temperature.model.TemperaturePolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service managing physical crate containers, lifecycle transitions, and temperature capability verification.
 */
public class CrateService {
    private final InMemoryDataStore dataStore;

    public CrateService(InMemoryDataStore dataStore) {
        this.dataStore = dataStore;
    }

    public Crate registerCrate(String crateId, double minTempCapability, double maxTempCapability,
                               double capacityLiters, String warehouseId) {
        if (crateId == null || crateId.trim().isEmpty()) {
            crateId = IdGenerator.nextId("CR");
        }
        if (dataStore.getCrates().containsKey(crateId)) {
            throw new ValidationException("Crate with ID " + crateId + " already exists.");
        }
        InputValidator.requireValidTemperatureRange(minTempCapability, maxTempCapability);
        InputValidator.requirePositive(capacityLiters, "Capacity Liters");
        InputValidator.requireNonEmpty(warehouseId, "Warehouse ID");

        Crate crate = new Crate(crateId, minTempCapability, maxTempCapability, capacityLiters, warehouseId);
        dataStore.getCrates().put(crateId, crate);

        // Register in warehouse inventory
        if (dataStore.getWarehouseInventories().containsKey(warehouseId)) {
            dataStore.getWarehouseInventories().get(warehouseId).addCrate(crateId);
        }

        return crate;
    }

    public Optional<Crate> findCrateById(String crateId) {
        if (crateId == null) return Optional.empty();
        return Optional.ofNullable(dataStore.getCrates().get(crateId));
    }

    public List<Crate> getAllCrates() {
        return new ArrayList<>(dataStore.getCrates().values());
    }

    public List<Crate> getCratesByWarehouse(String warehouseId) {
        return dataStore.getCrates().values().stream()
                .filter(c -> c.getWarehouseId().equals(warehouseId))
                .toList();
    }

    public List<Crate> getAvailableCratesByWarehouse(String warehouseId) {
        return dataStore.getCrates().values().stream()
                .filter(c -> c.getWarehouseId().equals(warehouseId) && c.getStatus() == CrateStatus.AVAILABLE)
                .toList();
    }

    /**
     * Validates that crate operating capability safely covers the entire policy required range.
     * Throws CompatibilityException if invalid.
     */
    public void validateCompatibility(Crate crate, TemperaturePolicy policy) {
        if (crate == null) {
            throw new ValidationException("Crate cannot be null.");
        }
        if (policy == null) {
            throw new ValidationException("Temperature Policy cannot be null.");
        }
        if (!crate.isCompatibleWith(policy)) {
            throw new CompatibilityException(String.format(
                    "Crate %s capability [%.1f°C to %.1f°C] is incompatible with product policy %s [%.1f°C to %.1f°C].",
                    crate.getCrateId(), crate.getMinTempCapability(), crate.getMaxTempCapability(),
                    policy.getPolicyId(), policy.getMinTemp(), policy.getMaxTemp()));
        }
    }

    public void updateCrateStatus(String crateId, CrateStatus newStatus) {
        Crate crate = findCrateById(crateId)
                .orElseThrow(() -> new ValidationException("Crate with ID " + crateId + " not found."));
        crate.transitionStatus(newStatus);
    }
}
