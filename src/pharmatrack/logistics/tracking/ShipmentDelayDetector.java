package pharmatrack.logistics.tracking;

import pharmatrack.common.util.IdGenerator;
import pharmatrack.logistics.model.CheckpointStatus;
import pharmatrack.logistics.model.RouteCheckpoint;
import pharmatrack.logistics.model.ShipmentEvent;
import pharmatrack.logistics.model.VehicleTrip;
import pharmatrack.storage.InMemoryDataStore;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Detects schedule variances during simulated transit and emits delay alerts for affected shipments.
 */
public class ShipmentDelayDetector {
    private final InMemoryDataStore dataStore;
    private final long delayThresholdMinutes;

    public ShipmentDelayDetector(InMemoryDataStore dataStore, long delayThresholdMinutes) {
        this.dataStore = dataStore;
        this.delayThresholdMinutes = delayThresholdMinutes > 0 ? delayThresholdMinutes : 15;
    }

    public ShipmentDelayDetector(InMemoryDataStore dataStore) {
        this(dataStore, 15);
    }

    public Optional<ShipmentEvent> checkDelay(VehicleTrip trip, RouteCheckpoint checkpoint, LocalDateTime simulatedTime) {
        if (trip == null || checkpoint == null || simulatedTime == null) {
            return Optional.empty();
        }

        LocalDateTime expected = checkpoint.getExpectedArrivalTime();
        if (expected == null) {
            return Optional.empty();
        }

        long delayMinutes = Duration.between(expected, simulatedTime).toMinutes();

        if (delayMinutes >= delayThresholdMinutes) {
            checkpoint.setStatus(CheckpointStatus.DELAYED);

            // Identify affected pending shipments on this trip
            List<String> affectedShipments = new ArrayList<>();
            for (String sId : trip.getShipmentIds()) {
                var shipment = dataStore.getShipments().get(sId);
                if (shipment != null && shipment.getStatus() != pharmatrack.logistics.model.ShipmentStatus.DELIVERED) {
                    affectedShipments.add(sId);
                }
            }

            String desc = String.format("DELAY DETECTED on Trip %s at checkpoint '%s': Delayed by %d mins (Expected: %s, Simulated: %s). Affected shipments: %s",
                    trip.getTripId(), checkpoint.getName(), delayMinutes, expected.toLocalTime(), simulatedTime.toLocalTime(),
                    String.join(", ", affectedShipments));

            ShipmentEvent event = new ShipmentEvent(
                    IdGenerator.nextId("EVT"), "ShipmentDelayEvent",
                    checkpoint.getAssociatedShipmentId(), trip.getTripId(), null, desc);

            dataStore.addEvent(event);
            return Optional.of(event);
        }

        return Optional.empty();
    }
}
