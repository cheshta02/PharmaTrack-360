package pharmatrack.logistics.model;

import pharmatrack.json.JsonCreator;
import pharmatrack.json.JsonProperty;
import pharmatrack.common.util.InputValidator;

import java.util.Objects;

public class Warehouse {
    private final String warehouseId;
    private String name;
    private String city;
    private double latitude;
    private double longitude;

    @JsonCreator
    public Warehouse(
            @JsonProperty("warehouseId") String warehouseId,
            @JsonProperty("name") String name,
            @JsonProperty("city") String city,
            @JsonProperty("latitude") double latitude,
            @JsonProperty("longitude") double longitude) {
        this.warehouseId = InputValidator.requireNonEmpty(warehouseId, "Warehouse ID");
        this.name = InputValidator.requireNonEmpty(name, "Warehouse Name");
        this.city = InputValidator.requireNonEmpty(city, "City");
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = InputValidator.requireNonEmpty(name, "Warehouse Name");
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = InputValidator.requireNonEmpty(city, "City");
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Warehouse warehouse)) return false;
        return Objects.equals(warehouseId, warehouse.warehouseId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(warehouseId);
    }

    @Override
    public String toString() {
        return String.format("%s: %s (%s) [%.4f, %.4f]", warehouseId, name, city, latitude, longitude);
    }
}
