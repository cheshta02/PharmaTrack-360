package pharmatrack.temperature.service;

import pharmatrack.storage.InMemoryDataStore;
import pharmatrack.temperature.model.*;

import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Optional;

/**
 * Generates temperature reporting, summary metrics, and excursion analytics.
 */
public class TemperatureStatistics {
    private final InMemoryDataStore dataStore;
    private final TemperatureMonitor temperatureMonitor;

    public TemperatureStatistics(InMemoryDataStore dataStore, TemperatureMonitor temperatureMonitor) {
        this.dataStore = dataStore;
        this.temperatureMonitor = temperatureMonitor;
    }

    public List<TemperatureReading> getReadingsForCrate(String crateId) {
        return dataStore.getReadings().stream()
                .filter(r -> r.getCrateId().equals(crateId))
                .toList();
    }

    public Optional<DoubleSummaryStatistics> getStatsForCrate(String crateId) {
        List<TemperatureReading> readings = getReadingsForCrate(crateId);
        if (readings.isEmpty()) {
            return Optional.empty();
        }
        DoubleSummaryStatistics stats = readings.stream()
                .filter(TemperatureReading::isValid)
                .mapToDouble(TemperatureReading::getTemperature)
                .summaryStatistics();
        return Optional.of(stats);
    }

    public List<Crate> getCratesOutsidePermittedRange() {
        return dataStore.getCrates().values().stream()
                .filter(c -> {
                    TemperatureStatus s = temperatureMonitor.getCurrentCrateStatus(c.getCrateId());
                    return s == TemperatureStatus.ABOVE_RANGE || s == TemperatureStatus.BELOW_RANGE;
                })
                .toList();
    }

    public String generateCratesCurrentTemperatureReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Current Crates Cold-Chain Status ===\n");
        sb.append(String.format("%-10s %-12s %-20s %-15s %-15s %-12s\n",
                "CrateID", "Status", "Capability Range", "Current Temp", "Temp Status", "Shipment"));
        sb.append("-----------------------------------------------------------------------------------------\n");

        for (Crate c : dataStore.getCrates().values()) {
            List<TemperatureReading> readings = getReadingsForCrate(c.getCrateId());
            String latestTempStr = "No Readings";
            if (!readings.isEmpty()) {
                TemperatureReading latest = readings.get(readings.size() - 1);
                latestTempStr = String.format("%.2f°C", latest.getTemperature());
            }

            TemperatureStatus tStatus = temperatureMonitor.getCurrentCrateStatus(c.getCrateId());
            String rangeStr = String.format("%.1f°C to %.1f°C", c.getMinTempCapability(), c.getMaxTempCapability());

            sb.append(String.format("%-10s %-12s %-20s %-15s %-15s %-12s\n",
                    c.getCrateId(),
                    c.getStatus(),
                    rangeStr,
                    latestTempStr,
                    tStatus,
                    c.getCurrentShipmentId() != null ? c.getCurrentShipmentId() : "None"));
        }
        return sb.toString();
    }

    public String generateExcursionSummaryReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Temperature Excursions History ===\n");
        sb.append(String.format("Total Excursions: %d\n", dataStore.getExcursions().size()));
        sb.append("---------------------------------------------------------------------------------------------------------\n");
        sb.append(String.format("%-10s %-10s %-10s %-12s %-12s %-12s %-15s %-10s\n",
                "ExcID", "CrateID", "BatchID", "ShipmentID", "Observed", "Permitted", "Timestamp", "Duration(s)"));
        sb.append("---------------------------------------------------------------------------------------------------------\n");

        for (TemperatureExcursion e : dataStore.getExcursions()) {
            String permitted = String.format("%.1f to %.1f", e.getMinPermitted(), e.getMaxPermitted());
            sb.append(String.format("%-10s %-10s %-10s %-12s %-12s %-12s %-15s %-10d\n",
                    e.getExcursionId(),
                    e.getCrateId(),
                    e.getBatchId() != null ? e.getBatchId() : "N/A",
                    e.getShipmentId() != null ? e.getShipmentId() : "N/A",
                    String.format("%.2f°C", e.getObservedTemperature()),
                    permitted,
                    e.getTimestamp().toLocalTime().toString(),
                    e.getDurationSeconds()));
        }
        return sb.toString();
    }
}
