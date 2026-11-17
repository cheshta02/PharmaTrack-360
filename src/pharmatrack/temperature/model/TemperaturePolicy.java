package pharmatrack.temperature.model;

import pharmatrack.json.JsonCreator;
import pharmatrack.json.JsonProperty;
import pharmatrack.common.util.InputValidator;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Represents a temperature policy governing storage and transit requirements for pharmaceutical products.
 */
public class TemperaturePolicy {
    private final String policyId;
    private String name;
    private double minTemp;
    private double maxTemp;
    private String unit;
    private String version;
    private LocalDate effectiveDate;
    private String description;
    private boolean active;

    @JsonCreator
    public TemperaturePolicy(
            @JsonProperty("policyId") String policyId,
            @JsonProperty("name") String name,
            @JsonProperty("minTemp") double minTemp,
            @JsonProperty("maxTemp") double maxTemp,
            @JsonProperty("unit") String unit,
            @JsonProperty("version") String version,
            @JsonProperty("effectiveDate") LocalDate effectiveDate,
            @JsonProperty("description") String description,
            @JsonProperty("active") boolean active) {
        this.policyId = InputValidator.requireNonEmpty(policyId, "Policy ID");
        this.name = InputValidator.requireNonEmpty(name, "Policy Name");
        InputValidator.requireValidTemperatureRange(minTemp, maxTemp);
        this.minTemp = minTemp;
        this.maxTemp = maxTemp;
        this.unit = (unit == null || unit.trim().isEmpty()) ? "°C" : unit.trim();
        this.version = (version == null || version.trim().isEmpty()) ? "1.0" : version.trim();
        this.effectiveDate = effectiveDate != null ? effectiveDate : LocalDate.now();
        this.description = description != null ? description.trim() : "";
        this.active = active;
    }

    public TemperaturePolicy(String policyId, String name, double minTemp, double maxTemp, String description) {
        this(policyId, name, minTemp, maxTemp, "°C", "1.0", LocalDate.now(), description, true);
    }

    public String getPolicyId() {
        return policyId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = InputValidator.requireNonEmpty(name, "Policy Name");
    }

    public double getMinTemp() {
        return minTemp;
    }

    public double getMaxTemp() {
        return maxTemp;
    }

    public void updateTemperatureLimits(double minTemp, double maxTemp) {
        InputValidator.requireValidTemperatureRange(minTemp, maxTemp);
        this.minTemp = minTemp;
        this.maxTemp = maxTemp;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isWithinRange(double temperature) {
        return temperature >= minTemp && temperature <= maxTemp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TemperaturePolicy that)) return false;
        return Objects.equals(policyId, that.policyId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(policyId);
    }

    @Override
    public String toString() {
        return String.format("%s: %s [%.1f%s to %.1f%s] (v%s, active=%s)",
                policyId, name, minTemp, unit, maxTemp, unit, version, active);
    }
}
