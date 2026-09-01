package Tracker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class SpatialVehicleTracker {
    // Lock-free key-value store for O(1) concurrent updates
    private final ConcurrentHashMap<String, VehicleRecord> registry = new ConcurrentHashMap<>();
    
    // Boundary enclosing the operational region (e.g., Lagos / UNILAG coordinates)
    private final BoundingBox regionBoundary = new BoundingBox(6.40, 3.30, 6.65, 3.50);

    public void registerVehicle(String id, String model, String driver, double lat, double lon) {
        validateCoordinates(lat, lon);
        registry.put(id, new VehicleRecord(id, model, driver, new Location(lat, lon)));
    }

    // Atomic, lock-free update via CAS (Compare-And-Swap) semantics
    public void updateLocation(String id, double latitude, double longitude) {
        validateCoordinates(latitude, longitude);
        registry.computeIfPresent(id, (k, current) -> current.updateLocation(latitude, longitude));
    }

    public VehicleRecord getVehicle(String id) {
        return registry.get(id); // Lock-free O(1) read
    }

    public Map<String, VehicleRecord> getAllVehicles() {
        return Collections.unmodifiableMap(registry);
    }

    // Ephemeral QuadTree build + candidate filtering spatial radius query
    public List<VehicleRecord> findVehiclesNear(double lat, double lon, double radiusKm) {
        if (radiusKm < 0) {
            throw new IllegalArgumentException("Radius cannot be negative.");
        }
        QuadTreeNode quadTree = new QuadTreeNode(regionBoundary);
        for (VehicleRecord record : registry.values()) {
            quadTree.insert(record);
        }

        // Approx: 1 deg latitude ≈ 111km; 1 deg longitude ≈ 111km * cos(lat)
        validateCoordinates(lat, lon);
        double latDelta = radiusKm / 111.0;
        double lonDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(lat)));
        BoundingBox searchRange = new BoundingBox(lat - latDelta, lon - lonDelta, lat + latDelta, lon + lonDelta);
        List<VehicleRecord> candidates = new ArrayList<>();
        quadTree.queryRange(searchRange, candidates);

        // Precise Euclidean/Haversine filter on candidates
        List<VehicleRecord> results = new ArrayList<>();
        for (VehicleRecord candidate : candidates) {
            if (haversineDistance(lat, lon, candidate.getLocation().getLatitude(), candidate.getLocation().getLongitude()) <= radiusKm) {
                results.add(candidate);
            }
        }
        return results;
    }

    private void validateCoordinates(double lat, double lon) {
        if (!regionBoundary.contains(lat, lon)) {
            throw new IllegalArgumentException("Coordinates are outside the supported region boundary.");
        }
    }

    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
