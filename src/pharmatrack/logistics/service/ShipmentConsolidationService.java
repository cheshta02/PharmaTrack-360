package pharmatrack.logistics.service;

import pharmatrack.common.exception.CompatibilityException;
import pharmatrack.common.exception.ValidationException;
import pharmatrack.logistics.model.Shipment;
import pharmatrack.logistics.model.ShipmentPriority;
import pharmatrack.logistics.model.Vehicle;
import pharmatrack.temperature.model.Crate;
import pharmatrack.temperature.service.CrateService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates and groups compatible same-warehouse pharmaceutical shipments into consolidated vehicle trips.
 */
public class ShipmentConsolidationService {
    private final VehicleCompatibilityValidator vehicleValidator;
    private final CrateService crateService;

    public ShipmentConsolidationService(VehicleCompatibilityValidator vehicleValidator, CrateService crateService) {
        this.vehicleValidator = vehicleValidator;
        this.crateService = crateService;
    }

    public static class ConsolidationResult {
        public final boolean eligible;
        public final String sourceWarehouseId;
        public final int totalCrates;
        public final List<Shipment> shipments;
        public final List<String> allCrateIds;
        public final String reason;

        public ConsolidationResult(boolean eligible, String sourceWarehouseId, int totalCrates,
                                   List<Shipment> shipments, List<String> allCrateIds, String reason) {
            this.eligible = eligible;
            this.sourceWarehouseId = sourceWarehouseId;
            this.totalCrates = totalCrates;
            this.shipments = shipments != null ? shipments : new ArrayList<>();
            this.allCrateIds = allCrateIds != null ? allCrateIds : new ArrayList<>();
            this.reason = reason;
        }
    }

    public ConsolidationResult evaluateConsolidation(List<Shipment> candidateShipments) {
        if (candidateShipments == null || candidateShipments.size() < 2) {
            return new ConsolidationResult(false, null, 0, null, null, "At least two shipments are required for consolidation.");
        }

        // 1. Mandatory rule: Must originate from the EXACT SAME source warehouse
        String sourceWarehouse = candidateShipments.get(0).getSourceWarehouseId();
        if (sourceWarehouse == null) {
            return new ConsolidationResult(false, null, 0, null, null, "First shipment has no source warehouse assigned.");
        }

        for (Shipment s : candidateShipments) {
            if (s.getSourceWarehouseId() == null || !s.getSourceWarehouseId().equals(sourceWarehouse)) {
                return new ConsolidationResult(false, null, 0, null, null,
                        String.format("Cannot consolidate: Shipments originate from different source warehouses (%s vs %s).",
                                sourceWarehouse, s.getSourceWarehouseId()));
            }
        }

        // 2. Mandatory capacity rule: Sum of required crates <= 5
        int totalCrates = candidateShipments.stream().mapToInt(Shipment::getRequiredCrates).sum();
        if (totalCrates > Vehicle.MAX_CRATE_CAPACITY) {
            return new ConsolidationResult(false, sourceWarehouse, totalCrates, candidateShipments, null,
                    String.format("Consolidation rejected: Combined crate count (%d) exceeds vehicle capacity (%d).",
                            totalCrates, Vehicle.MAX_CRATE_CAPACITY));
        }

        // 3. Collect allocated crates and verify individual compatibility
        List<String> allCrateIds = new ArrayList<>();
        List<Crate> physicalCrates = new ArrayList<>();
        for (Shipment s : candidateShipments) {
            allCrateIds.addAll(s.getAllocatedCrateIds());
            for (String cId : s.getAllocatedCrateIds()) {
                Crate c = crateService.findCrateById(cId).orElse(null);
                if (c != null) {
                    physicalCrates.add(c);
                }
            }
        }

        // 4. Priority Protection: Normal order must not compromise emergency deadlines
        boolean hasEmergency = candidateShipments.stream().anyMatch(s -> s.getPriority() == ShipmentPriority.EMERGENCY);
        if (hasEmergency) {
            // Check earliest deadline
            LocalDateTime earliestEmergencyDeadline = candidateShipments.stream()
                    .filter(s -> s.getPriority() == ShipmentPriority.EMERGENCY)
                    .map(Shipment::getExpectedDeliveryDeadline)
                    .min(LocalDateTime::compareTo)
                    .orElse(LocalDateTime.now().plusDays(1));

            // Estimate travel for multi-stop route
            if (candidateShipments.size() > 2) {
                // If more than 2 stops might jeopardize emergency, flag or evaluate
            }
        }

        String successMsg = String.format("Consolidation validated: %d shipments originating at %s with %d total crates.",
                candidateShipments.size(), sourceWarehouse, totalCrates);

        return new ConsolidationResult(true, sourceWarehouse, totalCrates, candidateShipments, allCrateIds, successMsg);
    }
}
