package pharmatrack.logistics.routing;

import pharmatrack.common.exception.ApiException;
import pharmatrack.common.util.IdGenerator;
import pharmatrack.logistics.model.Route;
import pharmatrack.logistics.model.RouteCheckpoint;
import pharmatrack.logistics.model.Shipment;
import pharmatrack.logistics.model.Warehouse;
import pharmatrack.storage.InMemoryDataStore;

import java.time.LocalDateTime;
import java.util.*;

public class RouteService {
    private final GeocodingService geocodingService;
    private final RoutingApiClient routingApiClient;
    private final RouteResponseParser routeResponseParser;
    private final CheckpointGenerator checkpointGenerator;

    public RouteService(GeocodingService geocodingService,
                        RoutingApiClient routingApiClient,
                        RouteResponseParser routeResponseParser,
                        CheckpointGenerator checkpointGenerator) {
        this.geocodingService = geocodingService;
        this.routingApiClient = routingApiClient;
        this.routeResponseParser = routeResponseParser;
        this.checkpointGenerator = checkpointGenerator;
    }

    public Route generateRoute(Warehouse sourceWarehouse, List<Shipment> shipments, LocalDateTime dispatchTime) {
        if (sourceWarehouse == null) {
            throw new ApiException("Source warehouse cannot be null.");
        }
        if (shipments == null || shipments.isEmpty()) {
            throw new ApiException("At least one shipment is required to generate a route.");
        }

        List<CheckpointGenerator.DeliveryDestination> deliveryDests = new ArrayList<>();
        List<String> destCityNames = new ArrayList<>();
        List<double[]> waypoints = new ArrayList<>();

        double[] sourceCoords = new double[]{sourceWarehouse.getLatitude(), sourceWarehouse.getLongitude()};
        waypoints.add(sourceCoords);

        for (Shipment s : shipments) {
            double[] coords = geocodingService.geocode(s.getDestinationCity());
            s.setDestLat(coords[0]);
            s.setDestLon(coords[1]);
            deliveryDests.add(new CheckpointGenerator.DeliveryDestination(s.getShipmentId(), s.getDestinationCity(), coords[0], coords[1]));
            destCityNames.add(s.getDestinationCity());
            waypoints.add(coords);
        }

        double distanceKm;
        double durationMinutes;
        List<double[]> geometryCoordinates = new ArrayList<>();

        try {
            String jsonResponse = routingApiClient.fetchRouteJson(waypoints);
            RouteResponseParser.ParsedRouteData parsed = routeResponseParser.parseOsrmResponse(jsonResponse);
            distanceKm = parsed.distanceKm;
            durationMinutes = parsed.durationMinutes;
            geometryCoordinates.addAll(parsed.geometryCoordinates);
        } catch (Exception ex) {
            distanceKm = 0.0;
            double prevLat = sourceCoords[0];
            double prevLon = sourceCoords[1];
            for (double[] pt : waypoints.subList(1, waypoints.size())) {
                double crowFlies = CheckpointGenerator.haversineDistanceKm(prevLat, prevLon, pt[0], pt[1]);
                distanceKm += (crowFlies * 1.25); // Road winding factor
                prevLat = pt[0];
                prevLon = pt[1];
            }
            durationMinutes = (distanceKm / 60.0) * 60.0; // Assuming 60 km/h average road speed

            geometryCoordinates.add(sourceCoords);
            for (double[] pt : waypoints.subList(1, waypoints.size())) {
                geometryCoordinates.add(pt);
            }
        }

        List<RouteCheckpoint> checkpoints = checkpointGenerator.generateCheckpoints(
                sourceWarehouse.getName(), sourceCoords[0], sourceCoords[1],
                deliveryDests, geometryCoordinates, dispatchTime);

        String routeId = IdGenerator.nextId("RTE");
        Route route = new Route(routeId, sourceWarehouse.getCity(), destCityNames, distanceKm, durationMinutes,
                geometryCoordinates, checkpoints);

        return route;
    }
}
