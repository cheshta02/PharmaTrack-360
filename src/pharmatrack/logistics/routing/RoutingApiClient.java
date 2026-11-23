package pharmatrack.logistics.routing;

import pharmatrack.common.exception.ApiException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;


public class RoutingApiClient {
    private static final String OSRM_BASE_URL = "http://router.project-osrm.org/route/v1/driving/";
    private final HttpClient httpClient;

    public RoutingApiClient() {
        HttpClient client = null;
        try {
            client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
        } catch (Throwable ignored) {
            // Loopback socket restriction in sandbox
        }
        this.httpClient = client;
    }

    public String fetchRouteJson(List<double[]> waypoints) {
        if (httpClient == null) {
            throw new ApiException("HTTP client unavailable (offline / sandbox environment).");
        }
        if (waypoints == null || waypoints.size() < 2) {
            throw new ApiException("At least two waypoints (source and destination) are required for routing.");
        }

        StringBuilder coordsBuilder = new StringBuilder();
        for (int i = 0; i < waypoints.size(); i++) {
            double[] pt = waypoints.get(i);
            coordsBuilder.append(String.format(java.util.Locale.US, "%.6f,%.6f", pt[1], pt[0])); // lon,lat
            if (i < waypoints.size() - 1) {
                coordsBuilder.append(";");
            }
        }

        String requestUrl = OSRM_BASE_URL + coordsBuilder + "?overview=full&geometries=geojson&steps=true";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(requestUrl))
                    .header("User-Agent", "PharmaTrack360-AcademicLogistics/1.0")
                    .timeout(Duration.ofSeconds(6))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return response.body();
            } else {
                throw new ApiException(String.format("OSRM routing returned HTTP %d: %s", response.statusCode(), response.body()));
            }
        } catch (Exception e) {
            throw new ApiException("Failed to communicate with OSRM routing service: " + e.getMessage(), e);
        }
    }
}
