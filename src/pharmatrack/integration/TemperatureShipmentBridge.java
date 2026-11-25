package pharmatrack.integration;

import pharmatrack.common.util.IdGenerator;
import pharmatrack.logistics.model.Shipment;
import pharmatrack.logistics.model.ShipmentEvent;
import pharmatrack.logistics.model.VehicleTrip;
import pharmatrack.storage.InMemoryDataStore;
import pharmatrack.temperature.model.Crate;
import pharmatrack.temperature.model.TemperatureExcursion;
import pharmatrack.temperature.service.TemperatureMonitor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;


public class TemperatureShipmentBridge {
    private final InMemoryDataStore dataStore;
    private final TemperatureMonitor temperatureMonitor;

    private final ConcurrentHashMap<String, List<TemperatureExcursion>> shipmentExcursions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<TemperatureExcursion>> tripExcursions = new ConcurrentHashMap<>();

    public TemperatureShipmentBridge(InMemoryDataStore dataStore, TemperatureMonitor temperatureMonitor) {
        this.dataStore = dataStore;
        this.temperatureMonitor = temperatureMonitor;
        initBridge();
    }

    private void initBridge() {
        temperatureMonitor.addExcursionListener(this::handleExcursionEvent);
    }

    private void handleExcursionEvent(TemperatureExcursion excursion) {
        String crateId = excursion.getCrateId();
        String shipmentId = excursion.getShipmentId();
        String tripId = excursion.getTripId();

        if (shipmentId == null || tripId == null) {
            Crate crate = dataStore.getCrates().get(crateId);
            if (crate != null) {
                if (shipmentId == null) shipmentId = crate.getCurrentShipmentId();
                if (tripId == null) tripId = crate.getCurrentTripId();
            }
        }

        if (shipmentId != null) {
            shipmentExcursions.computeIfAbsent(shipmentId, k -> new CopyOnWriteArrayList<>()).add(excursion);
        }
        if (tripId != null) {
            tripExcursions.computeIfAbsent(tripId, k -> new CopyOnWriteArrayList<>()).add(excursion);
        }

        String desc = String.format("TEMPERATURE EXCURSION ALERT on Crate %s: %.2f°C is %s permitted range [%.1f°C to %.1f°C] (Batch: %s, Shipment: %s, Trip: %s)",
                crateId, excursion.getObservedTemperature(), excursion.getExcursionType(),
                excursion.getMinPermitted(), excursion.getMaxPermitted(),
                excursion.getBatchId() != null ? excursion.getBatchId() : "N/A",
                shipmentId != null ? shipmentId : "N/A",
                tripId != null ? tripId : "N/A");

        ShipmentEvent event = new ShipmentEvent(
                IdGenerator.nextId("EVT"), "TemperatureExcursionEvent",
                shipmentId, tripId, crateId, desc);

        dataStore.addEvent(event);
    }

    public void bindCrateToShipment(String crateId, String shipmentId, String tripId) {
        Crate crate = dataStore.getCrates().get(crateId);
        if (crate != null) {
            crate.setCurrentShipmentId(shipmentId);
            crate.setCurrentTripId(tripId);
        }
    }

    public List<TemperatureExcursion> getExcursionsForShipment(String shipmentId) {
        return shipmentExcursions.getOrDefault(shipmentId, new ArrayList<>());
    }

    public List<TemperatureExcursion> getExcursionsForTrip(String tripId) {
        return tripExcursions.getOrDefault(tripId, new ArrayList<>());
    }

    public String generateAlertsSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Cold-Chain Integrated Alerts Summary ===\n");
        sb.append(String.format("Shipments with Excursions: %d | Trips with Excursions: %d\n",
                shipmentExcursions.size(), tripExcursions.size()));
        sb.append("--------------------------------------------------------------------------------\n");

        for (var entry : shipmentExcursions.entrySet()) {
            String sId = entry.getKey();
            List<TemperatureExcursion> list = entry.getValue();
            Shipment s = dataStore.getShipments().get(sId);
            String dest = s != null ? s.getDestinationCity() : "Unknown";
            sb.append(String.format("Shipment %s (to %s) -> %d excursions recorded.\n", sId, dest, list.size()));
            for (TemperatureExcursion exc : list) {
                sb.append(String.format("  - Crate %s: %.2f°C at %s (Permitted: %.1f to %.1f°C)\n",
                        exc.getCrateId(), exc.getObservedTemperature(), exc.getTimestamp().toLocalTime(),
                        exc.getMinPermitted(), exc.getMaxPermitted()));
            }
        }
        return sb.toString();
    }
}
