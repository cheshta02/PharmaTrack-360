package pharmatrack.logistics.routing;

import pharmatrack.json.JsonNode;
import pharmatrack.json.ObjectMapper;
import pharmatrack.common.exception.ApiException;
import pharmatrack.storage.InMemoryDataStore;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GeocodingService {
    private final InMemoryDataStore dataStore;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    private static final Map<String, double[]> KNOWN_LOCATIONS = new ConcurrentHashMap<>();

    static {
        KNOWN_LOCATIONS.put("delhi", new double[]{28.6139, 77.2090});
        KNOWN_LOCATIONS.put("mumbai", new double[]{19.0760, 72.8777});
        KNOWN_LOCATIONS.put("kolkata", new double[]{22.5726, 88.3639});
        KNOWN_LOCATIONS.put("chennai", new double[]{13.0827, 80.2707});
        KNOWN_LOCATIONS.put("bengaluru", new double[]{12.9716, 77.5946});
        KNOWN_LOCATIONS.put("bangalore", new double[]{12.9716, 77.5946});
        KNOWN_LOCATIONS.put("amritsar", new double[]{31.6340, 74.8723});
        KNOWN_LOCATIONS.put("jalandhar", new double[]{31.3260, 75.5762});
        KNOWN_LOCATIONS.put("chandigarh", new double[]{30.7333, 76.7794});
        KNOWN_LOCATIONS.put("jaipur", new double[]{26.9124, 75.7873});
        KNOWN_LOCATIONS.put("pune", new double[]{18.5204, 73.8567});
        KNOWN_LOCATIONS.put("hyderabad", new double[]{17.3850, 78.4867});
        KNOWN_LOCATIONS.put("ahmedabad", new double[]{23.0225, 72.5714});
        KNOWN_LOCATIONS.put("lucknow", new double[]{26.8467, 80.9462});
        KNOWN_LOCATIONS.put("agra", new double[]{27.1767, 78.0081});
        KNOWN_LOCATIONS.put("ludhiana", new double[]{30.9010, 75.8573});
    }

    public GeocodingService(InMemoryDataStore dataStore) {
        this.dataStore = dataStore;
        HttpClient client = null;
        try {
            client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(4))
                    .build();
        } catch (Throwable ignored) {
        }
        this.httpClient = client;
        this.objectMapper = new ObjectMapper();
    }

    public double[] geocode(String locationName) {
        if (locationName == null || locationName.trim().isEmpty()) {
            throw new ApiException("Location name cannot be null or empty.");
        }
        String cleanName = locationName.trim().toLowerCase();

        if (dataStore.getGeocodingCache().containsKey(cleanName)) {
            return dataStore.getGeocodingCache().get(cleanName);
        }

        if (KNOWN_LOCATIONS.containsKey(cleanName)) {
            double[] coords = KNOWN_LOCATIONS.get(cleanName);
            dataStore.getGeocodingCache().put(cleanName, coords);
            return coords;
        }

        if (httpClient != null) {
            try {
                String url = "https://nominatim.openstreetmap.org/search?format=json&limit=1&q=" +
                             URLEncoder.encode(locationName + ", India", StandardCharsets.UTF_8);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("User-Agent", "PharmaTrack360-AcademicColdChain/1.0 (academic-project)")
                        .timeout(Duration.ofSeconds(4))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    JsonNode root = objectMapper.readTree(response.body());
                    if (root.isArray() && !root.isEmpty()) {
                        JsonNode first = root.get(0);
                        double lat = first.get("lat").asDouble();
                        double lon = first.get("lon").asDouble();
                        double[] coords = new double[]{lat, lon};
                        dataStore.getGeocodingCache().put(cleanName, coords);
                        return coords;
                    }
                }
            } catch (Exception ignored) {
                // Network failure or sandboxed mode
            }
        }

        for (Map.Entry<String, double[]> entry : KNOWN_LOCATIONS.entrySet()) {
            if (cleanName.contains(entry.getKey()) || entry.getKey().contains(cleanName)) {
                dataStore.getGeocodingCache().put(cleanName, entry.getValue());
                return entry.getValue();
            }
        }

        throw new ApiException("Unable to geocode location '" + locationName + "'. Coordinates not found in cache or external service.");
    }
}
