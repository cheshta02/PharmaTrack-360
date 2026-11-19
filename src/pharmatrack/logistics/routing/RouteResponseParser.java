package pharmatrack.logistics.routing;

import pharmatrack.json.JsonNode;
import pharmatrack.json.ObjectMapper;
import pharmatrack.common.exception.ApiException;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses JSON responses from external OSRM road routing API.
 */
public class RouteResponseParser {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public static class ParsedRouteData {
        public final double distanceKm;
        public final double durationMinutes;
        public final List<double[]> geometryCoordinates; // Each is [lat, lon]

        public ParsedRouteData(double distanceKm, double durationMinutes, List<double[]> geometryCoordinates) {
            this.distanceKm = distanceKm;
            this.durationMinutes = durationMinutes;
            this.geometryCoordinates = geometryCoordinates;
        }
    }

    public ParsedRouteData parseOsrmResponse(String jsonString) {
        try {
            JsonNode root = objectMapper.readTree(jsonString);
            String code = root.path("code").asText();
            if (!"Ok".equalsIgnoreCase(code)) {
                throw new ApiException("OSRM response status code not 'Ok': " + code);
            }

            JsonNode routes = root.path("routes");
            if (!routes.isArray() || routes.isEmpty()) {
                throw new ApiException("No routes found in OSRM response.");
            }

            JsonNode firstRoute = routes.get(0);
            double distanceMeters = firstRoute.path("distance").asDouble();
            double durationSeconds = firstRoute.path("duration").asDouble();

            double distanceKm = distanceMeters / 1000.0;
            double durationMinutes = durationSeconds / 60.0;

            List<double[]> coords = new ArrayList<>();
            JsonNode coordinatesNode = firstRoute.path("geometry").path("coordinates");
            if (coordinatesNode.isArray()) {
                for (JsonNode coordPair : coordinatesNode) {
                    if (coordPair.isArray() && coordPair.size() >= 2) {
                        double lon = coordPair.get(0).asDouble();
                        double lat = coordPair.get(1).asDouble();
                        coords.add(new double[]{lat, lon});
                    }
                }
            }

            return new ParsedRouteData(distanceKm, durationMinutes, coords);
        } catch (Exception e) {
            throw new ApiException("Error parsing OSRM routing response: " + e.getMessage(), e);
        }
    }
}
