package Tracker;

import java.util.*;
import java.util.concurrent.*;
public class Tracker {

    private static final Map<String, double[]> UNILAG_STOPS = new LinkedHashMap<>();
    private static final Map<String, List<List<String>>> ROUTES = new HashMap<>();
    private static final String[] START_POINTS = { "Gate", "DLI", "Education", "Campus" };

    static {
        UNILAG_STOPS.put("New Hall", new double[] { 6.518926, 3.391432 });
        UNILAG_STOPS.put("Campus", new double[] { 6.517811, 3.397617 });
        UNILAG_STOPS.put("DLI", new double[] { 6.512665, 3.391275 });
        UNILAG_STOPS.put("Gate", new double[] { 6.517666, 3.384752 });
        UNILAG_STOPS.put("Education", new double[] { 6.517383, 3.385804 });
        UNILAG_STOPS.put("FSS", new double[] { 6.515858, 3.391744 });
        UNILAG_STOPS.put("Sport", new double[] { 6.516989, 3.390854 });
        UNILAG_STOPS.put("CITS", new double[] { 6.518464, 3.395022 });

        // Routes mapped strictly to valid start points
        addRoute("Gate", Arrays.asList("Gate", "Sport", "New Hall", "CITS", "Campus"));
        addRoute("Gate", Arrays.asList("Gate", "Sport", "FSS", "DLI"));

        addRoute("Education", Arrays.asList("Education", "Sport", "New Hall", "FSS", "DLI"));
        addRoute("Education", Arrays.asList("Education", "Sport", "New Hall", "CITS", "Campus"));

        addRoute("DLI", Arrays.asList("DLI", "FSS", "New Hall", "Sport", "Gate"));
        addRoute("DLI", Arrays.asList("DLI", "FSS", "New Hall", "CITS", "Campus"));
        addRoute("DLI", Arrays.asList("DLI", "Campus"));

        addRoute("Campus", Arrays.asList("Campus", "CITS", "New Hall", "Sport", "Gate"));
        addRoute("Campus", Arrays.asList("Campus", "CITS", "New Hall", "FSS", "DLI"));
        addRoute("Campus", Arrays.asList("Campus", "CITS", "DLI"));

        // Fallbacks for intermediate stops if vehicles are moved there directly
        addRoute("New Hall", Arrays.asList("New Hall", "CITS", "Campus"));
        addRoute("New Hall", Arrays.asList("New Hall", "Sport", "Gate"));
        addRoute("Sport", Arrays.asList("Sport", "Education", "Gate"));
        addRoute("Sport", Arrays.asList("Sport", "New Hall", "Campus"));
        addRoute("FSS", Arrays.asList("FSS", "DLI"));
        addRoute("FSS", Arrays.asList("FSS", "New Hall", "Gate"));
        addRoute("CITS", Arrays.asList("CITS", "Campus"));
        addRoute("CITS", Arrays.asList("CITS", "New Hall", "Gate"));
    }

    private static void addRoute(String fromStop, List<String> path) {
        ROUTES.computeIfAbsent(fromStop, k -> new ArrayList<>()).add(path);
    }

    public static String findStopName(double lat, double lon) {
        final double EPSILON = 0.0001;
        for (Map.Entry<String, double[]> entry : UNILAG_STOPS.entrySet()) {
            double[] coords = entry.getValue();
            if (Math.abs(coords[0] - lat) < EPSILON && Math.abs(coords[1] - lon) < EPSILON) {
                return entry.getKey();
            }
        }
        return "In Transit";
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        SpatialVehicleTracker tracker = new SpatialVehicleTracker();

        System.out.print("Enter number of cars (Maximum 10 allowed): ");
        if (!scanner.hasNextInt()) {
            System.out.println("Invalid input. Terminating.");
            scanner.close();
            return;
        }

        int fleetSize = scanner.nextInt();
        scanner.nextLine();
        if (fleetSize <= 0 || fleetSize > 10) {
            System.out.println("Invalid number. Only 1–10 cars allowed.");
            scanner.close();
            return;
        }

        for (int i = 1; i <= fleetSize; i++) {
            System.out.println("\n--- Registering Car " + i + " of " + fleetSize + " ---");
            String id;
            while (true) {
                System.out.print("Enter ID: ");
                id = scanner.nextLine().trim();
                if (id.isEmpty()) {
                    System.out.println("Vehicle ID cannot be empty.");
                    continue;
                }
                if (tracker.getVehicle(id) != null) {
                    System.out.println("Vehicle ID already exists. Please enter a unique ID.");
                    continue;
                }
                break;
            }
            System.out.print("Enter driver name: ");
            String driver = scanner.nextLine().trim();
            System.out.print("Enter car model: ");
            String model = scanner.nextLine().trim();

            String startStop = START_POINTS[ThreadLocalRandom.current().nextInt(START_POINTS.length)];
            double[] coords = UNILAG_STOPS.get(startStop);

            tracker.registerVehicle(id, model, driver, coords[0], coords[1]);
            System.out.println("Car [" + id + "] assigned driver (" + driver + ") and initialized at [" + startStop + "]");
        }

        boolean running = true;
        while (running) {
            System.out.println("\n=================================");
            System.out.println("      VEHICLE TRACKER MENU       ");
            System.out.println("=================================");
            System.out.println("1. Move cars (Simulate concurrent movement)");
            System.out.println("2. Move a car to a specific stop");
            System.out.println("3. Track ALL cars");
            System.out.println("4. Track specific car");
            System.out.println("5. QuadTree Spatial Search (Find cars in radius)");
            System.out.println("6. Terminate System");
            System.out.print("Choose option (1-6): ");

            if (!scanner.hasNextInt()) {
                System.out.println("Invalid choice. Terminating.");
                break;
            }

            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1:
                    Map<String, VehicleRecord> all = tracker.getAllVehicles();
                    if (all.isEmpty()) {
                        System.out.println("No vehicles registered to move.");
                        break;
                    }

                    ExecutorService pool = Executors.newFixedThreadPool(all.size());
                    List<Callable<Void>> tasks = new ArrayList<>();

                    for (VehicleRecord record : all.values()) {
                        tasks.add(() -> {
                            VehicleRecord currentRecord = tracker.getVehicle(record.getId());
                            if (currentRecord == null) {
                                return null;
                            }

                            Location loc = currentRecord.getLocation();
                            String start = findStopName(loc.getLatitude(), loc.getLongitude());
                            List<List<String>> possiblePaths = ROUTES.get(start);

                            if (possiblePaths != null && !possiblePaths.isEmpty()) {
                                List<String> selectedPath = possiblePaths.get(
                                        ThreadLocalRandom.current().nextInt(possiblePaths.size()));
                                moveCarAlongPath(tracker, currentRecord.getId(), selectedPath, 1);
                            } else {
                                System.out.println("No route found from stop: " + start + " for Car " + currentRecord.getId());
                            }
                            return null;
                        });
                    }

                    try {
                        pool.invokeAll(tasks);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        pool.shutdown();
                        try {
                            if (!pool.awaitTermination(30, TimeUnit.SECONDS)) {
                                pool.shutdownNow();
                            }
                        } catch (InterruptedException e) {
                            pool.shutdownNow();
                            Thread.currentThread().interrupt();
                        }
                    }
                    System.out.println("\nAll cars completed their concurrent routes.");
                    break;

                case 2:
                    System.out.print("Enter Car ID: ");
                    String moveId = scanner.nextLine().trim();
                    VehicleRecord existingCar = tracker.getVehicle(moveId);
                    if (existingCar == null) {
                        System.out.println("Car ID not found.");
                        break;
                    }

                    System.out.println("Available stops: " + String.join(", ", UNILAG_STOPS.keySet()));
                    System.out.print("Choose destination stop: ");
                    String chosenStop = scanner.nextLine().trim();

                    if (!UNILAG_STOPS.containsKey(chosenStop)) {
                        System.out.println("Invalid stop name. Movement cancelled.");
                        break;
                    }

                    double[] coords = UNILAG_STOPS.get(chosenStop);
                    tracker.updateLocation(moveId, coords[0], coords[1]);
                    System.out.println("Success: " + moveId + " moved to " + chosenStop + " (" + coords[0] + ", " + coords[1] + ")");
                    break;

                case 3:
                    System.out.println("\n--- Current Vehicle Fleet Snapshot ---");
                    Map<String, VehicleRecord> vehicles = tracker.getAllVehicles();
                    if (vehicles.isEmpty()) {
                        System.out.println("No vehicles in the tracker.");
                    } else {
                        for (VehicleRecord car : vehicles.values()) {
                            Location loc = car.getLocation();
                            String stopName = findStopName(loc.getLatitude(), loc.getLongitude());
                            System.out.printf("ID: %-8s | Driver: %-12s | Model: %-14s | Location: (%.6f, %.6f) | Stop: %s%n",
                                    car.getId(), car.getDriverName(), car.getModel(),
                                    loc.getLatitude(), loc.getLongitude(), stopName);
                        }
                    }
                    break;

                case 4:
                    System.out.print("Enter Car ID: ");
                    String trackId = scanner.nextLine().trim();
                    VehicleRecord car = tracker.getVehicle(trackId);
                    if (car == null) {
                        System.out.println("Car ID not found.");
                    } else {
                        Location loc = car.getLocation();
                        String stopName = findStopName(loc.getLatitude(), loc.getLongitude());
                        System.out.println("\n--- Vehicle Details ---");
                        System.out.println("ID:          " + car.getId());
                        System.out.println("Driver:      " + car.getDriverName());
                        System.out.println("Model:       " + car.getModel());
                        System.out.printf("Coordinates: (%.6f, %.6f)%n", loc.getLatitude(), loc.getLongitude());
                        System.out.println("Status/Stop: " + stopName);
                    }
                    break;

                case 5:
                    System.out.println("Available stops: " + String.join(", ", UNILAG_STOPS.keySet()));
                    System.out.print("Select reference stop: ");
                    String refStop = scanner.nextLine().trim();
                    if (!UNILAG_STOPS.containsKey(refStop)) {
                        System.out.println("Invalid stop name.");
                        break;
                    }
                    System.out.print("Enter search radius in km (e.g., 0.5): ");
                    if (!scanner.hasNextDouble()) {
                        System.out.println("Invalid radius input.");
                        scanner.nextLine();
                        break;
                    }
                    double radius = scanner.nextDouble();
                    scanner.nextLine();

                    double[] refCoords = UNILAG_STOPS.get(refStop);
                    List<VehicleRecord> nearby = tracker.findVehiclesNear(refCoords[0], refCoords[1], radius);

                    System.out.println("\n--- Vehicles within " + radius + "km of " + refStop + " ---");
                    if (nearby.isEmpty()) {
                        System.out.println("No vehicles found within this radius.");
                    } else {
                        for (VehicleRecord v : nearby) {
                            Location vLoc = v.getLocation();
                            System.out.printf("ID: %-8s | Driver: %-12s | Model: %-14s | Location: (%.6f, %.6f)%n",
                                    v.getId(), v.getDriverName(), v.getModel(), vLoc.getLatitude(), vLoc.getLongitude());
                        }
                    }
                    break;

                case 6:
                    running = false;
                    System.out.println("Terminating tracking system.");
                    break;

                default:
                    System.out.println("Invalid option selected. Please choose 1–6.");
            }
        }
        scanner.close();
    }

    public static void moveCarAlongPath(SpatialVehicleTracker tracker, String carId, List<String> path, int delaySeconds) {
        for (String stop : path) {
            double[] coords = UNILAG_STOPS.get(stop);
            if (coords != null) {
                tracker.updateLocation(carId, coords[0], coords[1]);
                System.out.println("[" + carId + "] arrived at " + stop);
                try {
                    Thread.sleep(delaySeconds * 1000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
}