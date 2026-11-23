package pharmatrack.logistics.tracking;

import pharmatrack.logistics.model.Route;
import pharmatrack.logistics.model.RouteCheckpoint;
import pharmatrack.logistics.model.Shipment;
import pharmatrack.logistics.model.VehicleTrip;
import pharmatrack.storage.InMemoryDataStore;

import java.util.List;

public class ShipmentTracker {
    private final InMemoryDataStore dataStore;

    public ShipmentTracker(InMemoryDataStore dataStore) {
        this.dataStore = dataStore;
    }

    public String generateTripTrackingReport(String tripId) {
        VehicleTrip trip = dataStore.getVehicleTrips().get(tripId);
        if (trip == null) {
            return "Trip " + tripId + " not found.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("=== Tracking Report for Trip %s ===\n", tripId));
        sb.append(String.format("Vehicle: %s | Source: %s | Status: %s | Total Crates: %d\n",
                trip.getVehicleId(), trip.getSourceWarehouseId(), trip.getStatus(), trip.getTotalCrates()));
        sb.append(String.format("Shipments Carried: %s\n", String.join(", ", trip.getShipmentIds())));
        sb.append(String.format("Crates Carried: %s\n", String.join(", ", trip.getCrateIds())));

        Route route = trip.getRoute();
        if (route != null) {
            sb.append(String.format("Route: %s (Total Distance: %.1f km, Est. Duration: %.1f mins)\n",
                    route.getRouteId(), route.getDistanceKm(), route.getEstimatedDurationMinutes()));
            sb.append("--- Checkpoint Progression ---\n");
            sb.append(String.format("%-8s %-30s %-10s %-10s %-15s %-12s\n",
                    "Idx", "Name", "Dist(km)", "Type", "Status", "Simulated Time"));
            sb.append("----------------------------------------------------------------------------------------\n");

            List<RouteCheckpoint> cps = route.getCheckpoints();
            for (int i = 0; i < cps.size(); i++) {
                RouteCheckpoint cp = cps.get(i);
                String activeMarker = (i == trip.getCurrentCheckpointIndex()) ? " -> [CURRENT]" : "";
                String simTime = cp.getSimulatedArrivalTime() != null ? cp.getSimulatedArrivalTime().toLocalTime().toString() : "Pending";
                String type = cp.isDeliveryStop() ? "DELIVERY" : "WAYPOINT";

                sb.append(String.format("%-8s %-30s %-10.1f %-10s %-15s %-12s%s\n",
                        "#" + i, cp.getName(), cp.getDistanceFromStartKm(), type, cp.getStatus(), simTime, activeMarker));
            }
        }
        return sb.toString();
    }
}
