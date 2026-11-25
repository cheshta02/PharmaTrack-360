package pharmatrack.temperature.service;

import pharmatrack.common.exception.ResourceUnavailableException;
import pharmatrack.common.exception.ValidationException;
import pharmatrack.storage.InMemoryDataStore;
import pharmatrack.temperature.model.Crate;
import pharmatrack.temperature.model.CrateStatus;
import pharmatrack.temperature.model.TemperaturePolicy;

import java.util.ArrayList;
import java.util.List;

public class CrateInventoryService {
    private final InMemoryDataStore dataStore;
    private final CrateService crateService;

    public CrateInventoryService(InMemoryDataStore dataStore, CrateService crateService) {
        this.dataStore = dataStore;
        this.crateService = crateService;
    }

    public List<Crate> findCompatibleAvailableCrates(String warehouseId, TemperaturePolicy policy, int countNeeded) {
        if (warehouseId == null) {
            throw new ValidationException("Warehouse ID cannot be null.");
        }
        if (policy == null) {
            throw new ValidationException("Temperature Policy cannot be null.");
        }

        List<Crate> available = crateService.getAvailableCratesByWarehouse(warehouseId);
        List<Crate> compatible = new ArrayList<>();

        for (Crate crate : available) {
            if (crate.isCompatibleWith(policy)) {
                compatible.add(crate);
                if (compatible.size() == countNeeded) {
                    break;
                }
            }
        }

        return compatible;
    }

    public int getAvailableCrateCount(String warehouseId) {
        return (int) dataStore.getCrates().values().stream()
                .filter(c -> c.getWarehouseId().equals(warehouseId) && c.getStatus() == CrateStatus.AVAILABLE)
                .count();
    }

    public int getCompatibleAvailableCrateCount(String warehouseId, TemperaturePolicy policy) {
        if (policy == null) return 0;
        return (int) dataStore.getCrates().values().stream()
                .filter(c -> c.getWarehouseId().equals(warehouseId) &&
                             c.getStatus() == CrateStatus.AVAILABLE &&
                             c.isCompatibleWith(policy))
                .count();
    }

    public void allocateCrate(String crateId, String tripId, String shipmentId) {
        Crate crate = crateService.findCrateById(crateId)
                .orElseThrow(() -> new ValidationException("Crate " + crateId + " not found."));

        if (crate.getStatus() != CrateStatus.AVAILABLE) {
            throw new ResourceUnavailableException(String.format(
                    "Crate %s cannot be allocated. Current status: %s", crateId, crate.getStatus()));
        }

        crate.setCurrentTripId(tripId);
        crate.setCurrentShipmentId(shipmentId);
        crate.transitionStatus(CrateStatus.ALLOCATED);
    }

    public void completeReturnAndInspect(String crateId) {
        Crate crate = crateService.findCrateById(crateId)
                .orElseThrow(() -> new ValidationException("Crate " + crateId + " not found."));

        if (crate.getStatus() == CrateStatus.RETURNING) {
            crate.transitionStatus(CrateStatus.INSPECTION);
        }
        if (crate.getStatus() == CrateStatus.INSPECTION) {
            crate.transitionStatus(CrateStatus.AVAILABLE);
        }
    }
}
