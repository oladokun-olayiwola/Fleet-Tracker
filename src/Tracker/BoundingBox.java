package Tracker;

class BoundingBox {
    final double minLat, minLon, maxLat, maxLon;

    public BoundingBox(double minLat, double minLon, double maxLat, double maxLon) {
        this.minLat = minLat;
        this.minLon = minLon;
        this.maxLat = maxLat;
        this.maxLon = maxLon;
    }

    public boolean contains(double lat, double lon) {
        return lat >= minLat && lat <= maxLat && lon >= minLon && lon <= maxLon;
    }

    public boolean intersects(BoundingBox other) {
        return !(other.maxLat < this.minLat || other.minLat > this.maxLat ||
                 other.maxLon < this.minLon || other.minLon > this.maxLon);
    }
}