package pharmatrack.temperature.model;

/**
 * Temperature reading evaluation status against an applicable product temperature policy.
 */
public enum TemperatureStatus {
    NORMAL,
    ABOVE_RANGE,
    BELOW_RANGE,
    SENSOR_UNAVAILABLE,
    INVALID_READING
}
