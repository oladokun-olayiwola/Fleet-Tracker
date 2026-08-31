package Tracker;

final class VehicleRecord {
    private final String id;
    private final String model;
    private final String driverName;
    private final Location location;

    public VehicleRecord(String id, String model, String driverName, Location location) {
        this.id = id;
        this.model = model;
        this.driverName = driverName;
        this.location = location;
    }

    public String getId() {
        return id;
    }

    public String getModel() {
        return model;
    }

    public String getDriverName() {
        return driverName;
    }

    public Location getLocation() {
        return location;
    }

    public VehicleRecord updateLocation(double lat, double lon) {
        return new VehicleRecord(this.id, this.model, this.driverName, new Location(lat, lon));
    }
}

