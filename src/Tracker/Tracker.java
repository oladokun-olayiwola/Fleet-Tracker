package Tracker;

import java.util.*;
import java.util.concurrent.*;

final class Location {
    private final double latitude;
    private final double longitude;
    private final long timestamp;

    public Location(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = System.currentTimeMillis();
    }

    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public long getTimestamp() { return timestamp; }
}

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

    public String getId() { return id; }
    public String getModel() { return model; }
    public String getDriverName() { return driverName; }
    public Location getLocation() { return location; }

    public VehicleRecord updateLocation(double lat, double lon) {
        return new VehicleRecord(this.id, this.model, this.driverName, new Location(lat, lon));
    }
}

public class Tracker {

    private static final Map<String, double[]> unilagStops = new HashMap<>();
    private static final Map<String, String> coordinateNames = new HashMap<>();
    private static final Map<String, Map<String, List<String>>> routes = new HashMap<>();
    private static final String[] startPoints = {"Gate", "DLI", "Education", "Campus"};

    static {
        unilagStops.put("New Hall", new double[]{6.518926, 3.391432});
        unilagStops.put("Campus", new double[]{6.517811, 3.397617});
        unilagStops.put("DLI", new double[]{6.512665, 3.391275});
        unilagStops.put("Gate", new double[]{6.517666, 3.384752});
        unilagStops.put("Education", new double[]{6.517383, 3.385804});
        unilagStops.put("FSS", new double[]{6.515858, 3.3917444});
        unilagStops.put("Sport", new double[]{6.516989, 3.390854});
        unilagStops.put("CITS", new double[]{6.518464, 3.395022});

        for (Map.Entry<String, double[]> entry : unilagStops.entrySet()) {
            double[] coords = entry.getValue();
            coordinateNames.put(coords[0] + "," + coords[1], entry.getKey());
        }

        routes.put("DLI", new HashMap<>());
        routes.get("DLI").put("Education_via_NewHall", Arrays.asList("DLI", "FSS", "New Hall", "Sport", "Education"));
        routes.get("DLI").put("Education", Arrays.asList("DLI", "Education"));
        routes.get("DLI").put("Campus_via_NewHall", Arrays.asList("DLI", "New Hall", "CITS", "Campus"));
        routes.get("DLI").put("Campus", Arrays.asList("DLI", "Campus"));

        routes.put("Campus", new HashMap<>());
        routes.get("Campus").put("Gate", Arrays.asList("Campus", "CITS", "New Hall", "Sport", "Gate"));
        routes.get("Campus").put("DLI", Arrays.asList("Campus", "DLI"));
        routes.get("Campus").put("DLI_via_NewHall", Arrays.asList("Campus", "CITS", "New Hall", "FSS", "DLI"));

        routes.put("Gate", new HashMap<>());
        routes.get("Gate").put("Campus", Arrays.asList("Gate", "Sport", "New Hall", "CITS", "Campus"));

        routes.put("Education", new HashMap<>());
        routes.get("Education").put("DLI", Arrays.asList("Education", "DLI"));
        routes.get("Education").put("DLI_via_NewHall", Arrays.asList("Education", "Sport", "New Hall", "FSS", "DLI"));

    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Random rand = new Random();
        SpatialVehicleTracker tracker = new SpatialVehicleTracker();

        System.out.println("Enter number of cars (Maximum 10 allowed): ");
        int fleetSize = scanner.nextInt();
        scanner.nextLine();
        if (fleetSize <= 0 || fleetSize > 10) {
            System.out.println("Invalid number. Only 1–10 cars allowed.");
            scanner.close();
            return;
        }

        for (int i = 1; i <= fleetSize; i++) {
            System.out.println("\nEnter ID of Car " + i + ": ");
            String id = scanner.nextLine();
            System.out.println("Enter driver name: ");
            String driver = scanner.nextLine();
            System.out.println("Enter model: ");
            String model = scanner.nextLine();

            String startStop = startPoints[rand.nextInt(startPoints.length)];
            double[] coords = unilagStops.get(startStop);

            tracker.registerVehicle(id, model, driver, coords[0], coords[1]);
            System.out.println(id + " initialized at " + startStop);
        }

        boolean running = true;
        while (running) {
            System.out.println("\n===== VEHICLE TRACKER MENU =====");
            System.out.println("1. Move cars (Simulate concurrent movement)");
            System.out.println("2. Move a car to a specific stop");
            System.out.println("3. Track ALL cars");
            System.out.println("4. Track specific car");
            System.out.println("5. QuadTree Spatial Radius Search (Find nearby cars)");
            System.out.println("6. Terminate System");
            System.out.print("Choose option: ");
            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1: // Concurrent movement via thread pool
                    Map<String, VehicleRecord> all = tracker.getAllVehicles();
                    ExecutorService pool = Executors.newFixedThreadPool(all.size());
                    List<Callable<Void>> tasks = new ArrayList<>();

                    for (VehicleRecord record : all.values()) {
                        tasks.add(() -> {
                            Location loc = record.getLocation();
                            String start = coordinateNames.get(loc.getLatitude() + "," + loc.getLongitude());
                            Map<String, List<String>> possibleDest = routes.get(start);

                            if (possibleDest != null && !possibleDest.isEmpty()) {
                                List<List<String>> allPaths = new ArrayList<>(possibleDest.values());
                                List<String> path = allPaths.get(rand.nextInt(allPaths.size()));
                                moveCarAlongPath(tracker, record.getId(), path, 1);
                            }
                            return null;
                        });
                    }

                    try {
                        pool.invokeAll(tasks);
                        pool.shutdown();
                    } catch (InterruptedException ignored) {}
                    System.out.println("All cars completed their concurrent routes.");
                    break;

                case 2: // Move single car
                    System.out.print("Enter Car ID: ");
                    String moveId = scanner.nextLine();
                    System.out.println("Available stops: New Hall, Campus, DLI, Gate, Education");
                    String chosenStop = scanner.nextLine();
                    if (!unilagStops.containsKey(chosenStop)) {
                        System.out.println("Invalid stop name.");
                        break;
                    }
                    double[] coords = unilagStops.get(chosenStop);
                    tracker.updateLocation(moveId, coords[0], coords[1]);
                    System.out.println(moveId + " moved to " + chosenStop + " (" + coords[0] + ", " + coords[1] + ")");
                    break;

                case 3: // Track all cars (Lock-free snapshot read)
                    System.out.println("\n--- All Vehicle Locations ---");
                    for (VehicleRecord car : tracker.getAllVehicles().values()) {
                        Location loc = car.getLocation();
                        String key = loc.getLatitude() + "," + loc.getLongitude();
                        String locationName = coordinateNames.getOrDefault(key, "In Transit");
                        System.out.println(car.getId() +
                                " | Driver: " + car.getDriverName() +
                                " | Model: " + car.getModel() +
                                " | Location: (" + loc.getLatitude() + ", " + loc.getLongitude() + ")" +
                                " | Stop: " + locationName);
                    }
                    break;

                case 4: // Track single car
                    System.out.print("Enter Car ID: ");
                    String trackId = scanner.nextLine();
                    VehicleRecord car = tracker.getVehicle(trackId);
                    if (car == null) {
                        System.out.println("Car not found.");
                    } else {
                        Location loc = car.getLocation();
                        String key = loc.getLatitude() + "," + loc.getLongitude();
                        String locationName = coordinateNames.getOrDefault(key, "In Transit");
                        System.out.println("\nCar Details:");
                        System.out.println("Driver: " + car.getDriverName());
                        System.out.println("Model: " + car.getModel());
                        System.out.println("Coordinates: (" + loc.getLatitude() + ", " + loc.getLongitude() + ")");
                        System.out.println("Stop: " + locationName);
                    }
                    break;

                case 5: // QuadTree Spatial Query
                    System.out.println("Available stops to query around: New Hall, Campus, DLI, Gate, Education");
                    System.out.print("Select reference stop: ");
                    String refStop = scanner.nextLine();
                    if (!unilagStops.containsKey(refStop)) {
                        System.out.println("Invalid stop name.");
                        break;
                    }
                    System.out.print("Enter radius in km (e.g. 0.5): ");
                    double radius = scanner.nextDouble();
                    scanner.nextLine();

                    double[] refCoords = unilagStops.get(refStop);
                    List<VehicleRecord> nearby = tracker.findVehiclesNear(refCoords[0], refCoords[1], radius);

                    System.out.println("\n--- Vehicles within " + radius + "km of " + refStop + " (via QuadTree) ---");
                    if (nearby.isEmpty()) {
                        System.out.println("No vehicles found in this radius.");
                    } else {
                        for (VehicleRecord v : nearby) {
                            System.out.println(v.getId() + " | Driver: " + v.getDriverName() + " | Model: " + v.getModel());
                        }
                    }
                    break;

                case 6:
                    running = false;
                    System.out.println("Tracking system terminated.");
                    break;

                default:
                    System.out.println("Invalid option.");
            }
        }
        scanner.close();
    }

    public static void moveCarAlongPath(SpatialVehicleTracker tracker, String carId, List<String> path, int delaySeconds) {
        for (String stop : path) {
            double[] coords = unilagStops.get(stop);
            tracker.updateLocation(carId, coords[0], coords[1]);
            System.out.println(carId + " arrived at " + stop);
            try {
                Thread.sleep(delaySeconds * 1000L);
            } catch (InterruptedException ignored) {}
        }
    }
}