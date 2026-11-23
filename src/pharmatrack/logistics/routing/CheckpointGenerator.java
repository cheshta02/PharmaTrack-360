package pharmatrack.logistics.routing;

import pharmatrack.common.util.IdGenerator;
import pharmatrack.logistics.model.CheckpointStatus;
import pharmatrack.logistics.model.RouteCheckpoint;

import java.time.LocalDateTime;
import java.util.*;

public class CheckpointGenerator {
    private final double intervalKm;
    private final double averageSpeedKmh;

    public static class DeliveryDestination {
        public final String shipmentId;
        public final String cityName;
        public final double latitude;
        public final double longitude;

        public DeliveryDestination(String shipmentId, String cityName, double latitude, double longitude) {
            this.shipmentId = shipmentId;
            this.cityName = cityName;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    public CheckpointGenerator(double intervalKm, double averageSpeedKmh) {
        this.intervalKm = intervalKm > 0 ? intervalKm : 30.0;
        this.averageSpeedKmh = averageSpeedKmh > 0 ? averageSpeedKmh : 60.0;
    }

    public CheckpointGenerator() {
        this(30.0, 60.0);
    }

    public List<RouteCheckpoint> generateCheckpoints(
            String sourceName, double sourceLat, double sourceLon,
            List<DeliveryDestination> deliveryDestinations,
            List<double[]> geometryCoordinates,
            LocalDateTime dispatchTime) {

        if (dispatchTime == null) {
            dispatchTime = LocalDateTime.now();
        }

        List<RouteCheckpoint> checkpoints = new ArrayList<>();

        RouteCheckpoint start = new RouteCheckpoint(
                IdGenerator.nextId("CHK"),
                sourceName + " (Departure Hub)",
                sourceLat, sourceLon, 0.0, false, null,
                dispatchTime, null, CheckpointStatus.PENDING);
        checkpoints.add(start);

        if (geometryCoordinates == null || geometryCoordinates.size() < 2) {
            double cumDist = 0.0;
            double prevLat = sourceLat;
            double prevLon = sourceLon;

            for (DeliveryDestination dest : deliveryDestinations) {
                double segDist = haversineDistanceKm(prevLat, prevLon, dest.latitude, dest.longitude);
                cumDist += segDist;
                long travelMinutes = (long) ((cumDist / averageSpeedKmh) * 60);
                LocalDateTime expectedTime = dispatchTime.plusMinutes(travelMinutes);

                RouteCheckpoint stop = new RouteCheckpoint(
                        IdGenerator.nextId("CHK"),
                        dest.cityName + " (Delivery Stop)",
                        dest.latitude, dest.longitude, Math.round(cumDist * 10.0) / 10.0,
                        true, dest.shipmentId,
                        expectedTime, null, CheckpointStatus.PENDING);
                checkpoints.add(stop);
                prevLat = dest.latitude;
                prevLon = dest.longitude;
            }
            return checkpoints;
        }

        double totalAccumulated = 0.0;
        double nextIntervalThreshold = intervalKm;
        Set<String> placedDestinations = new HashSet<>();

        for (int i = 1; i < geometryCoordinates.size(); i++) {
            double[] prev = geometryCoordinates.get(i - 1);
            double[] curr = geometryCoordinates.get(i);
            double seg = haversineDistanceKm(prev[0], prev[1], curr[0], curr[1]);
            totalAccumulated += seg;

            for (DeliveryDestination dest : deliveryDestinations) {
                if (!placedDestinations.contains(dest.shipmentId)) {
                    double distToDest = haversineDistanceKm(curr[0], curr[1], dest.latitude, dest.longitude);
                    if (distToDest < 15.0) {
                        placedDestinations.add(dest.shipmentId);
                        long travelMinutes = (long) ((totalAccumulated / averageSpeedKmh) * 60);
                        LocalDateTime expectedTime = dispatchTime.plusMinutes(travelMinutes);

                        RouteCheckpoint delivCheckpoint = new RouteCheckpoint(
                                IdGenerator.nextId("CHK"),
                                dest.cityName + " (Delivery Stop)",
                                dest.latitude, dest.longitude,
                                Math.round(totalAccumulated * 10.0) / 10.0,
                                true, dest.shipmentId,
                                expectedTime, null, CheckpointStatus.PENDING);
                        checkpoints.add(delivCheckpoint);
                    }
                }
            }

            if (totalAccumulated >= nextIntervalThreshold && i < geometryCoordinates.size() - 1) {
                long travelMinutes = (long) ((totalAccumulated / averageSpeedKmh) * 60);
                LocalDateTime expectedTime = dispatchTime.plusMinutes(travelMinutes);

                RouteCheckpoint virtualCp = new RouteCheckpoint(
                        IdGenerator.nextId("CHK"),
                        String.format("Waypoint @ ~%.0f km", totalAccumulated),
                        curr[0], curr[1],
                        Math.round(totalAccumulated * 10.0) / 10.0,
                        false, null,
                        expectedTime, null, CheckpointStatus.PENDING);
                checkpoints.add(virtualCp);
                nextIntervalThreshold += intervalKm;
            }
        }

        for (DeliveryDestination dest : deliveryDestinations) {
            if (!placedDestinations.contains(dest.shipmentId)) {
                double segDist = haversineDistanceKm(
                        checkpoints.get(checkpoints.size() - 1).getLatitude(),
                        checkpoints.get(checkpoints.size() - 1).getLongitude(),
                        dest.latitude, dest.longitude);
                totalAccumulated += segDist;
                long travelMinutes = (long) ((totalAccumulated / averageSpeedKmh) * 60);
                LocalDateTime expectedTime = dispatchTime.plusMinutes(travelMinutes);

                RouteCheckpoint delivCheckpoint = new RouteCheckpoint(
                        IdGenerator.nextId("CHK"),
                        dest.cityName + " (Delivery Stop)",
                        dest.latitude, dest.longitude,
                        Math.round(totalAccumulated * 10.0) / 10.0,
                        true, dest.shipmentId,
                        expectedTime, null, CheckpointStatus.PENDING);
                checkpoints.add(delivCheckpoint);
            }
        }

        return checkpoints;
    }

    public static double haversineDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0; // Earth radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
