package pharmatrack.temperature.model;

/**
 * Operational lifecycle states for reusable pharmaceutical transportation crates.
 * Full lifecycle: AVAILABLE -> ALLOCATED -> LOADED -> IN_TRANSIT -> RETURNING -> INSPECTION -> AVAILABLE
 */
public enum CrateStatus {
    AVAILABLE,
    ALLOCATED,
    LOADED,
    IN_TRANSIT,
    RETURNING,
    INSPECTION
}
