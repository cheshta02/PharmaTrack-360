package pharmatrack.logistics.model;

import pharmatrack.json.JsonCreator;
import pharmatrack.json.JsonProperty;
import pharmatrack.common.util.InputValidator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Road route connecting a source warehouse to one or more shipment delivery destinations.
 */
public class Route {
    private final String routeId;
    private final String sourceCity;
    private final List<String> destinationCities = new ArrayList<>();
    private double distanceKm;
    private double estimatedDurationMinutes;
    private final List<double[]> geometryCoordinates = new ArrayList<>();
    private final List<RouteCheckpoint> checkpoints = new ArrayList<>();

    @JsonCreator
    public Route(
            @JsonProperty("routeId") String routeId,
            @JsonProperty("sourceCity") String sourceCity,
            @JsonProperty("destinationCities") List<String> destinationCities,
            @JsonProperty("distanceKm") double distanceKm,
            @JsonProperty("estimatedDurationMinutes") double estimatedDurationMinutes,
            @JsonProperty("geometryCoordinates") List<double[]> geometryCoordinates,
            @JsonProperty("checkpoints") List<RouteCheckpoint> checkpoints) {
        this.routeId = InputValidator.requireNonEmpty(routeId, "Route ID");
        this.sourceCity = InputValidator.requireNonEmpty(sourceCity, "Source City");
        if (destinationCities != null) {
            this.destinationCities.addAll(destinationCities);
        }
        this.distanceKm = distanceKm >= 0 ? distanceKm : 0.0;
        this.estimatedDurationMinutes = estimatedDurationMinutes >= 0 ? estimatedDurationMinutes : 0.0;
        if (geometryCoordinates != null) {
            this.geometryCoordinates.addAll(geometryCoordinates);
        }
        if (checkpoints != null) {
            this.checkpoints.addAll(checkpoints);
        }
    }

    public Route(String routeId, String sourceCity, List<String> destinationCities,
                 double distanceKm, double estimatedDurationMinutes) {
        this(routeId, sourceCity, destinationCities, distanceKm, estimatedDurationMinutes, null, null);
    }

    public String getRouteId() {
        return routeId;
    }

    public String getSourceCity() {
        return sourceCity;
    }

    public List<String> getDestinationCities() {
        return Collections.unmodifiableList(destinationCities);
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public double getEstimatedDurationMinutes() {
        return estimatedDurationMinutes;
    }

    public void setEstimatedDurationMinutes(double estimatedDurationMinutes) {
        this.estimatedDurationMinutes = estimatedDurationMinutes;
    }

    public List<double[]> getGeometryCoordinates() {
        return Collections.unmodifiableList(geometryCoordinates);
    }

    public void setGeometryCoordinates(List<double[]> coords) {
        this.geometryCoordinates.clear();
        if (coords != null) {
            this.geometryCoordinates.addAll(coords);
        }
    }

    public List<RouteCheckpoint> getCheckpoints() {
        return Collections.unmodifiableList(checkpoints);
    }

    public void setCheckpoints(List<RouteCheckpoint> checkpoints) {
        this.checkpoints.clear();
        if (checkpoints != null) {
            this.checkpoints.addAll(checkpoints);
        }
    }

    public void addCheckpoint(RouteCheckpoint checkpoint) {
        this.checkpoints.add(checkpoint);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Route route)) return false;
        return Objects.equals(routeId, route.routeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(routeId);
    }

    @Override
    public String toString() {
        return String.format("%s: %s -> %s [%.1f km, %.1f mins, %d checkpoints]",
                routeId, sourceCity, String.join(" -> ", destinationCities),
                distanceKm, estimatedDurationMinutes, checkpoints.size());
    }
}
