package pharmatrack.logistics.service;

import pharmatrack.common.exception.ResourceUnavailableException;
import pharmatrack.common.exception.ValidationException;
import pharmatrack.logistics.model.Shipment;
import pharmatrack.storage.InMemoryDataStore;
import pharmatrack.temperature.model.Crate;
import pharmatrack.temperature.model.CrateStatus;
import pharmatrack.temperature.model.TemperaturePolicy;
import pharmatrack.temperature.service.CrateInventoryService;
import pharmatrack.temperature.service.CrateService;

import java.util.ArrayList;
import java.util.List;

/**
 * Service handling crate allocation, reservation, and rollback for shipments.
 */
public class CrateAllocationService {
    private final InMemoryDataStore dataStore;
    private final CrateService crateService;
    private final CrateInventoryService crateInventoryService;

    public CrateAllocationService(InMemoryDataStore dataStore, CrateService crateService, CrateInventoryService crateInventoryService) {
        this.dataStore = dataStore;
        this.crateService = crateService;
        this.crateInventoryService = crateInventoryService;
    }

    public List<Crate> allocateCratesForShipment(Shipment shipment, TemperaturePolicy policy) {
        if (shipment == null) {
            throw new ValidationException("Shipment cannot be null.");
        }
        if (shipment.getSourceWarehouseId() == null) {
            throw new ValidationException("Source warehouse must be assigned before allocating crates.");
        }

        List<Crate> availableCompatible = crateInventoryService.findCompatibleAvailableCrates(
                shipment.getSourceWarehouseId(), policy, shipment.getRequiredCrates());

        if (availableCompatible.size() < shipment.getRequiredCrates()) {
            throw new ResourceUnavailableException(String.format(
                    "Insufficient compatible available crates at %s for policy %s. Needed: %d, Available: %d",
                    shipment.getSourceWarehouseId(), policy.getPolicyId(), shipment.getRequiredCrates(), availableCompatible.size()));
        }

        List<String> allocatedIds = new ArrayList<>();
        for (Crate crate : availableCompatible) {
            crate.setCurrentShipmentId(shipment.getShipmentId());
            crate.transitionStatus(CrateStatus.ALLOCATED);
            allocatedIds.add(crate.getCrateId());
        }

        shipment.setAllocatedCrates(allocatedIds);
        return availableCompatible;
    }

    public void releaseAllocatedCrates(Shipment shipment) {
        if (shipment == null) return;
        for (String crateId : shipment.getAllocatedCrateIds()) {
            Crate crate = crateService.findCrateById(crateId).orElse(null);
            if (crate != null && crate.getStatus() == CrateStatus.ALLOCATED) {
                crate.setCurrentShipmentId(null);
                crate.setCurrentTripId(null);
                crate.transitionStatus(CrateStatus.AVAILABLE);
            }
        }
        shipment.setAllocatedCrates(new ArrayList<>());
    }
}
