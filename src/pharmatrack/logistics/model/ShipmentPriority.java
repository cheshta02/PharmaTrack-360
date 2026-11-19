package pharmatrack.logistics.model;

public enum ShipmentPriority {
    EMERGENCY(1),
    URGENT(2),
    NORMAL(3);

    private final int rank;

    ShipmentPriority(int rank) {
        this.rank = rank;
    }

    public int getRank() {
        return rank;
    }
}
