package Tracker;

import java.util.ArrayList;
import java.util.List;

public class QuadTreeNode {
    private static final int CAPACITY = 4;
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

    public boolean insert(VehicleRecord vehicle) {
        Location loc = vehicle.getLocation();
        if (!boundary.contains(loc.getLatitude(), loc.getLongitude())) {
            return false;
        }

        if (points.size() < CAPACITY && !divided) {
            points.add(vehicle);
            return true;
        }

        if (!divided) {
            subdivide();
            for (VehicleRecord p : points) {
                insertToChildren(p);
            }
            points.clear();
        }

        return insertToChildren(vehicle);
    }

    private boolean insertToChildren(VehicleRecord vehicle) {
        return northWest.insert(vehicle) || northEast.insert(vehicle) ||
               southWest.insert(vehicle) || southEast.insert(vehicle);
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