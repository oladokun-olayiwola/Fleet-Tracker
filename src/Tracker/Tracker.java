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

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        System.out.println("Enter number of cars (Maximum 10 allowed): ");
        int fleetSize = scanner.nextInt();
        scanner.nextLine();

        if (fleetSize <= 0 || fleetSize > 10) {
            System.out.println("Invalid number. Only 1–10 cars allowed.");
        }

        Map<String, Car> cars = new HashMap<>();
        Map<String, MutableLocation> locations = new HashMap<>();

        for (int i = 1; i <= fleetSize; i++) {

            System.out.println("\nEnter ID ( NAME ) of Car " + i + ": ");
            String id = scanner.nextLine();

            System.out.println("Enter driver name: ");
            String driver = scanner.nextLine();

            System.out.println("Enter model: ");
            String model = scanner.nextLine();

            cars.put(id, new Car(id, model, driver));
            locations.put(id, new MutableLocation(40.0 + i, -74.0 - i));
        }

        MonitorVehicleTracker tracker =
                new MonitorVehicleTracker(cars, locations);

        boolean running = true;

        while (running) {

            System.out.println("\n===== VEHICLE TRACKER MENU =====");
            System.out.println("1. Move cars");
            System.out.println("2. Move a car to specific location");
            System.out.println("3. Track ALL cars");
            System.out.println("4. Track specific car");
            System.out.println("5. Terminate System");
            System.out.print("Choose option: ");

            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1:

                    List<Thread> threads = new ArrayList<>();

                    for (String id : cars.keySet()) {

                        Thread t = new Thread(() -> {
                        	double newLat = -90 + Math.random() * 180;
                        	double newLon = -180 + Math.random() * 360;

                            tracker.setLocation(id, newLat, newLon);

                            System.out.println("Moved " + id +
                                    " -> (" + newLat + ", " + newLon + ")");
                        });

                        threads.add(t);
                        t.start();
                    }

                    for (Thread t : threads) {
                        try { t.join(); } catch (InterruptedException ignored) {}
                    }

                    System.out.println("All cars moved successfully.");
                    break;

                case 2:

                    System.out.print("Enter Car ID ( NAME ): ");
                    String moveId = scanner.nextLine();

                    double lat = 0;
                    double lon = 0;
                    boolean validInput = false;

                    while (!validInput) {
                        try {
                            System.out.print("Enter new latitude (-90 to 90): ");
                            lat = Double.parseDouble(scanner.nextLine());
                            if (lat < -90 || lat > 90) {
                                System.out.println("Latitude must be between -90 and 90.");
                                continue;
                            }

                            System.out.print("Enter new longitude (-180 to 180): ");
                            lon = Double.parseDouble(scanner.nextLine());
                            if (lon < -180 || lon > 180) {
                                System.out.println("Longitude must be between -180 and 180.");
                                continue;
                            }

                            validInput = true;

                        } catch (NumberFormatException e) {
                            System.out.println("Invalid input. Please enter numeric values.");
                        }
                    }

                    final double finalLat = lat;
                    final double finalLon = lon;

                    Thread moveOne = new Thread(() -> {
                        tracker.setLocation(moveId, finalLat, finalLon);
                        System.out.println("Car moved successfully.");
                    });

                    moveOne.start();
                    try { moveOne.join(); } catch (InterruptedException ignored) {}
                    break;

                case 3:

                    Thread viewAll = new Thread(() -> {

                        Map<String, MutableLocation> snapshot =
                                tracker.getLocations();

                        System.out.println("\n--- All Vehicle Locations ---");

                        for (String id : snapshot.keySet()) {

                            MutableLocation loc = snapshot.get(id);
                            Car car = tracker.getCar(id);

                            System.out.println(id +
                                    " | Driver: " + car.getDriverName() +
                                    " | Model: " + car.getModel() +
                                    " | Location: (" +
                                    loc.latitude + ", " +
                                    loc.longitude + ")");
                        }
                    });

                    viewAll.start();
                    try { viewAll.join(); } catch (InterruptedException ignored) {}
                    break;

                case 4:

                    System.out.print("Enter Car ID: ");
                    String trackId = scanner.nextLine();

                    Thread viewOne = new Thread(() -> {

                        MutableLocation loc =
                                tracker.getLocation(trackId);

                        Car car = tracker.getCar(trackId);

                        if (loc == null || car == null) {
                            System.out.println("Car not found.");
                        } else {
                            System.out.println("\nCar Details:");
                            System.out.println("Driver: " + car.getDriverName());
                            System.out.println("Model: " + car.getModel());
                            System.out.println("Location: (" +
                                    loc.latitude + ", " +
                                    loc.longitude + ")");
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
}

