package Tracker;
import java.util.*;

class MutableLocation {
    public double latitude;
    public double longitude;
    public MutableLocation(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }
    public MutableLocation(MutableLocation other) {
        this.latitude = other.latitude;
        this.longitude = other.longitude;
    }
}

class Car {
    private final String id;
    private final String model;
    private final String driverName;
    public Car(String id, String model, String driverName) {
        this.id = id;
        this.model = model;
        this.driverName = driverName;
    }
    public String getId() { return id; }
    public String getModel() { return model; }
    public String getDriverName() { return driverName; }
}

class MonitorVehicleTracker {
    private final Map<String, Car> cars;
    private final Map<String, MutableLocation> locations;
    public MonitorVehicleTracker(Map<String, Car> cars,
                                 Map<String, MutableLocation> initialLocations) {
        this.cars = Collections.unmodifiableMap(new HashMap<>(cars));
        this.locations = deepCopy(initialLocations);
    }
    public synchronized void setLocation(String id,
                                         double latitude,
                                         double longitude) {
        MutableLocation loc = locations.get(id);
        if (loc == null) {
            throw new IllegalArgumentException("No such vehicle: " + id);
        }
        loc.latitude = latitude;
        loc.longitude = longitude;
    }
    public synchronized MutableLocation getLocation(String id) {
        MutableLocation loc = locations.get(id);
        return (loc == null) ? null : new MutableLocation(loc);
    }
    public synchronized Map<String, MutableLocation> getLocations() {
        return Collections.unmodifiableMap(deepCopy(locations));
    }
    public Car getCar(String id) {
        return cars.get(id);
    }
    private static Map<String, MutableLocation> deepCopy(
            Map<String, MutableLocation> original) {
        Map<String, MutableLocation> result = new HashMap<>();
        for (String id : original.keySet()) {
            result.put(id, new MutableLocation(original.get(id)));
        }
        return result;
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

        for (Map.Entry<String, double[]> entry : unilagStops.entrySet()) {
            double[] coords = entry.getValue();
            coordinateNames.put(coords[0] + "," + coords[1], entry.getKey());
        }

        routes.put("DLI", new HashMap<>());
        routes.get("DLI").put("Education", Arrays.asList("DLI", "Education"));
        routes.get("DLI").put("Gate", Arrays.asList("DLI", "New Hall", "Gate"));

        routes.put("Campus", new HashMap<>());
        routes.get("Campus").put("Gate", Arrays.asList("Campus", "Gate"));
        routes.get("Campus").put("Gate_via_NewHall", Arrays.asList("Campus", "New Hall", "Gate"));
        routes.get("Campus").put("DLI", Arrays.asList("Campus", "DLI"));
        routes.get("Campus").put("DLI_via_NewHall", Arrays.asList("Campus", "New Hall", "DLI"));
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Random rand = new Random();

        System.out.println("Enter number of cars (Maximum 10 allowed): ");
        int fleetSize = scanner.nextInt();
        scanner.nextLine();
        if (fleetSize <= 0 || fleetSize > 10) {
            System.out.println("Invalid number. Only 1–10 cars allowed.");
            return;
        }

        Map<String, Car> cars = new HashMap<>();
        Map<String, MutableLocation> locations = new HashMap<>();

        for (int i = 1; i <= fleetSize; i++) {
            System.out.println("\nEnter ID (NAME) of Car " + i + ": ");
            String id = scanner.nextLine();
            System.out.println("Enter driver name: ");
            String driver = scanner.nextLine();
            System.out.println("Enter model: ");
            String model = scanner.nextLine();

            String startStop = startPoints[rand.nextInt(startPoints.length)];
            double[] coords = unilagStops.get(startStop);

            cars.put(id, new Car(id, model, driver));
            locations.put(id, new MutableLocation(coords[0], coords[1]));
            System.out.println(id + " starts at " + startStop);
        }

        MonitorVehicleTracker tracker = new MonitorVehicleTracker(cars, locations);

        boolean running = true;
        while (running) {
            System.out.println("\n===== VEHICLE TRACKER MENU =====");
            System.out.println("1. Move cars (simulate movement)");
            System.out.println("2. Move a car to specific stop");
            System.out.println("3. Track ALL cars");
            System.out.println("4. Track specific car");
            System.out.println("5. Terminate System");
            System.out.print("Choose option: ");
            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1: // Move all cars along routes
                    List<Thread> threads = new ArrayList<>();
                    for (String carId : cars.keySet()) {
                        Thread t = new Thread(() -> {
                            MutableLocation loc = tracker.getLocation(carId);
                            String start = coordinateNames.get(loc.latitude + "," + loc.longitude);

                            Map<String, List<String>> possibleDest = routes.get(start);
                            if (possibleDest != null && !possibleDest.isEmpty()) {
                                List<List<String>> allPaths = new ArrayList<>(possibleDest.values());
                                List<String> path = allPaths.get(rand.nextInt(allPaths.size()));
                                moveCarAlongPath(tracker, carId, path, 2);
                            }
                        });
                        threads.add(t);
                        t.start();
                    }
                    for (Thread t : threads) {
                        try { t.join(); } catch (InterruptedException ignored) {}
                    }
                    System.out.println("All cars completed their routes.");
                    break;

                case 2: // Move a single car
                    System.out.print("Enter Car ID (NAME): ");
                    String moveId = scanner.nextLine();
                    System.out.println("Available stops: New Hall, Campus, DLI, Gate, Education");
                    String chosenStop = scanner.nextLine();
                    if (!unilagStops.containsKey(chosenStop)) {
                        System.out.println("Invalid stop name.");
                        break;
                    }
                    double[] coords = unilagStops.get(chosenStop);
                    final double finalLat = coords[0];
                    final double finalLon = coords[1];
                    Thread moveOne = new Thread(() -> {
                        tracker.setLocation(moveId, finalLat, finalLon);
                        System.out.println(moveId + " moved to " + chosenStop
                                + " (" + finalLat + ", " + finalLon + ")");
                    });
                    moveOne.start();
                    try { moveOne.join(); } catch (InterruptedException ignored) {}
                    break;

                case 3: // Track all cars
                    Thread viewAll = new Thread(() -> {
                        Map<String, MutableLocation> snapshot = tracker.getLocations();
                        System.out.println("\n--- All Vehicle Locations ---");
                        for (String id : snapshot.keySet()) {
                            MutableLocation loc = snapshot.get(id);
                            Car car = tracker.getCar(id);
                            String key = loc.latitude + "," + loc.longitude;
                            String locationName = coordinateNames.getOrDefault(key, "Unknown");
                            System.out.println(id +
                                    " | Driver: " + car.getDriverName() +
                                    " | Model: " + car.getModel() +
                                    " | Location: (" +
                                    loc.latitude + ", " +
                                    loc.longitude + ")" +
                                    " | Stop: " + locationName);
                        }
                    });
                    viewAll.start();
                    try { viewAll.join(); } catch (InterruptedException ignored) {}
                    break;

                case 4: // Track specific car
                    System.out.print("Enter Car ID: ");
                    String trackId = scanner.nextLine();
                    Thread viewOne = new Thread(() -> {
                        MutableLocation loc = tracker.getLocation(trackId);
                        Car car = tracker.getCar(trackId);
                        if (loc == null || car == null) {
                            System.out.println("Car not found.");
                        } else {
                            String key = loc.latitude + "," + loc.longitude;
                            String locationName = coordinateNames.getOrDefault(key, "Unknown");
                            System.out.println("\nCar Details:");
                            System.out.println("Driver: " + car.getDriverName());
                            System.out.println("Model: " + car.getModel());
                            System.out.println("Coordinates: (" +
                                    loc.latitude + ", " + loc.longitude + ")");
                            System.out.println("Stop: " + locationName);
                        }
                    });
                    viewOne.start();
                    try { viewOne.join(); } catch (InterruptedException ignored) {}
                    break;

                case 5:
                    running = false;
                    System.out.println("Tracking system terminated.");
                    break;

                default:
                    System.out.println("Invalid option.");
            }
        }

        scanner.close();
    }

    public static void moveCarAlongPath(MonitorVehicleTracker tracker,
                                        String carId,
                                        List<String> path,
                                        int delaySeconds) {
        for (String stop : path) {
            double[] coords = unilagStops.get(stop);
            tracker.setLocation(carId, coords[0], coords[1]);
            System.out.println(carId + " arrived at " + stop
                    + " (" + coords[0] + ", " + coords[1] + ")");
            try {
                Thread.sleep(delaySeconds * 5000);
            } catch (InterruptedException ignored) {}
        }
        System.out.println(carId + " completed its route.");
    }
}