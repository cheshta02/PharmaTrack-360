package pharmatrack.logistics.service;

import pharmatrack.common.exception.ValidationException;
import pharmatrack.logistics.model.VehicleStatus;
import pharmatrack.logistics.model.WarehouseInventory;
import pharmatrack.storage.InMemoryDataStore;
import pharmatrack.temperature.model.CrateStatus;
import pharmatrack.temperature.model.ProductBatch;

import java.util.List;
import java.util.Optional;

/**
 * Service managing warehouse-specific stocks, fleet availability, and crate inventory.
 */
public class WarehouseInventoryService {
    private final InMemoryDataStore dataStore;

    public WarehouseInventoryService(InMemoryDataStore dataStore) {
        this.dataStore = dataStore;
    }

    public WarehouseInventory getInventory(String warehouseId) {
        if (!dataStore.getWarehouses().containsKey(warehouseId)) {
            throw new ValidationException("Warehouse " + warehouseId + " does not exist.");
        }
        return dataStore.getWarehouseInventories().computeIfAbsent(warehouseId, WarehouseInventory::new);
    }

    public List<ProductBatch> getAvailableBatches(String warehouseId, String productId, int requiredQty) {
        WarehouseInventory inv = getInventory(warehouseId);
        return inv.getBatchIds().stream()
                .map(id -> dataStore.getBatches().get(id))
                .filter(b -> b != null &&
                             b.getProductId().equals(productId) &&
                             b.isEligibleForAllocation(requiredQty))
                .toList();
    }

    public int getTotalAvailableQuantity(String warehouseId, String productId) {
        WarehouseInventory inv = getInventory(warehouseId);
        return inv.getBatchIds().stream()
                .map(id -> dataStore.getBatches().get(id))
                .filter(b -> b != null && b.getProductId().equals(productId) && !b.isBlocked() && !b.isExpired())
                .mapToInt(ProductBatch::getAvailableQuantity)
                .sum();
    }

    public int getAvailableCrateCount(String warehouseId) {
        WarehouseInventory inv = getInventory(warehouseId);
        return (int) inv.getCrateIds().stream()
                .map(id -> dataStore.getCrates().get(id))
                .filter(c -> c != null && c.getStatus() == CrateStatus.AVAILABLE)
                .count();
    }

    public int getAvailableVehicleCount(String warehouseId) {
        WarehouseInventory inv = getInventory(warehouseId);
        return (int) inv.getVehicleIds().stream()
                .map(id -> dataStore.getVehicles().get(id))
                .filter(v -> v != null && v.getStatus() == VehicleStatus.AVAILABLE)
                .count();
    }

    public String generateInventoryReport(String warehouseId) {
        var wh = dataStore.getWarehouses().get(warehouseId);
        if (wh == null) return "Warehouse " + warehouseId + " not found.";

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("=== Inventory Report for %s (%s, %s) ===\n", wh.getWarehouseId(), wh.getName(), wh.getCity()));
        sb.append(String.format("Available Vehicles: %d / %d\n",
                getAvailableVehicleCount(warehouseId), getInventory(warehouseId).getVehicleIds().size()));
        sb.append(String.format("Available Crates: %d / %d\n",
                getAvailableCrateCount(warehouseId), getInventory(warehouseId).getCrateIds().size()));
        sb.append(String.format("Active Trips: %d | Pending Shipments: %d\n",
                getInventory(warehouseId).getActiveTripIds().size(), getInventory(warehouseId).getPendingShipmentIds().size()));
        sb.append("--- Batches Stored ---\n");
        sb.append(String.format("%-10s %-10s %-12s %-10s %-12s %-8s\n",
                "BatchID", "ProductID", "Available", "Reserved", "Total", "Status"));
        for (String bId : getInventory(warehouseId).getBatchIds()) {
            ProductBatch b = dataStore.getBatches().get(bId);
            if (b != null) {
                String status = b.isBlocked() ? "BLOCKED" : (b.isExpired() ? "EXPIRED" : "OK");
                sb.append(String.format("%-10s %-10s %-12d %-10d %-12d %-8s\n",
                        b.getBatchId(), b.getProductId(), b.getAvailableQuantity(),
                        b.getReservedQuantity(), b.getTotalQuantity(), status));
            }
        }
        return sb.toString();
    }
}
