package Tracker;

import java.util.ArrayList;
import java.util.List;

class QuadTreeNode {
    private static final int CAPACITY = 4;
    private static final int MAX_DEPTH = 10; // Prevents StackOverflow on identical/clustered coordinates
    private final BoundingBox boundary;
    private final List<VehicleRecord> points = new ArrayList<>();
    private QuadTreeNode northWest, northEast, southWest, southEast;
    private boolean divided = false;

    public QuadTreeNode(BoundingBox boundary) {
        this.boundary = boundary;
    }

    private void subdivide() {
        double midLat = (boundary.minLat + boundary.maxLat) / 2.0;
        double midLon = (boundary.minLon + boundary.maxLon) / 2.0;

        northWest = new QuadTreeNode(new BoundingBox(midLat, boundary.minLon, boundary.maxLat, midLon));
        northEast = new QuadTreeNode(new BoundingBox(midLat, midLon, boundary.maxLat, boundary.maxLon));
        southWest = new QuadTreeNode(new BoundingBox(boundary.minLat, boundary.minLon, midLat, midLon));
        southEast = new QuadTreeNode(new BoundingBox(boundary.minLat, midLon, midLat, boundary.maxLon));
        divided = true;
    }

    // Public entry point maintaining backward compatibility
    public boolean insert(VehicleRecord vehicle) {
        return insert(vehicle, 0);
    }

    // Depth-tracked internal insert
    private boolean insert(VehicleRecord vehicle, int depth) {
        Location loc = vehicle.getLocation();
        if (!boundary.contains(loc.getLatitude(), loc.getLongitude())) {
            return false;
        }

        // If the node has capacity OR we hit MAX_DEPTH, keep the point in this node
        if (depth >= MAX_DEPTH) {
            points.add(vehicle);
            return true;
        }

        if (!divided) {
            if (points.size() < CAPACITY) {
                points.add(vehicle);
                return true;
            }

            subdivide();
            List<VehicleRecord> existing = new ArrayList<>(points);
            points.clear();
            for (VehicleRecord p : existing) {
                insertToChildren(p, depth + 1);
            }
        }

        return insertToChildren(vehicle, depth + 1);
    }

    private boolean insertToChildren(VehicleRecord vehicle, int depth) {
        return northWest.insert(vehicle, depth) || northEast.insert(vehicle, depth) ||
               southWest.insert(vehicle, depth) || southEast.insert(vehicle, depth);
    }

    public void queryRange(BoundingBox range, List<VehicleRecord> found) {
        if (!boundary.intersects(range)) return;

        for (VehicleRecord p : points) {
            if (range.contains(p.getLocation().getLatitude(), p.getLocation().getLongitude())) {
                found.add(p);
            }
        }

        if (divided) {
            northWest.queryRange(range, found);
            northEast.queryRange(range, found);
            southWest.queryRange(range, found);
            southEast.queryRange(range, found);
        }
    }
}