# Concurrent Geospatial Fleet Telemetry & QuadTree Tracker

A high-performance, concurrent vehicle telemetry tracking and transit simulation engine built in Java. The system pairs lock-free memory primitives (`ConcurrentHashMap`, immutable value snapshots) with a hierarchical **2D Point QuadTree** for $\mathcal{O}(\log N)$ geospatial proximity filtering across the UNILAG campus transit network.

---

## Architectural Highlights

* **Lock-Free Concurrency Model:** Utilizes `ConcurrentHashMap` and immutable domain records (`VehicleRecord`, `Location`) to guarantee race-condition-free reads and atomic writes without coarse global synchronization.
* **$\mathcal{O}(\log N)$ Spatial Indexing:** Implements a recursive 2D Point QuadTree that partitions geographic coordinate space, reducing radial proximity queries from an $\mathcal{O}(N)$ brute-force distance scan to $\mathcal{O}(\log N)$.
* **Two-Tier Search Pruning:** Employs fast bounding-box intersection testing for quadrant candidate pruning (broad phase) followed by precise Haversine circular distance validation (narrow phase).
* **Deterministic Route Graph & Deadlock-Free Fallbacks:** Features an explicit transit route network (`ROUTES`) paired with a `LinkedHashMap` (`UNILAG_STOPS`) to preserve stop order and fallback paths for intermediate transit nodes.
* **Epsilon-Tolerant Geofencing:** Resolves raw floating-point coordinates to designated transit stop names using an epsilon threshold ($\epsilon = 0.0001$) to prevent floating-point mismatch errors.
* **Thread-Safe Simulation Engine:** Uses dynamic thread pools (`ExecutorService`), `ThreadLocalRandom`, and `Callable<Void>` tasks with graceful timeout await logic to simulate concurrent vehicle movement.

---

## System Architecture
+----------------------------+
                           |          Tracker           |
                           |    (CLI & Simulation)      |
                           +--------------+-------------+
                                          |
                                          v
                           +----------------------------+
                           |   SpatialVehicleTracker    |
                           +--------------+-------------+
                                          |
                  +-----------------------+-----------------------+
                  |                                               |
                  v                                               v
    +---------------------------+                   +---------------------------+
    |    Concurrent Registry    |                   |       Spatial Index       |
    |  (ConcurrentHashMap<K,V>) |                   |     (Point QuadTree)      |
    +-------------+-------------+                   +-------------+-------------+
                  |                                               |
                  v                                               v
     Atomic O(1) State Updates                       O(log N) Proximity Searches

---

## File Structure

Fleet-Tracker/├── src/│   └── Tracker/│       └── Tracker.java         # Monolithic source containing all models, QuadTree, Tracker, and CLI driver└── README.md
---

## Getting Started

### Prerequisites

* **Java Development Kit (JDK):** Version 11 or higher

### Build & Execution

1. Clone the repository and checkout the working branch:
```bash
git clone [https://github.com/oladokun-olayiwola/Fleet-Tracker.git](https://github.com/oladokun-olayiwola/Fleet-Tracker.git)
cd Fleet-Tracker
git checkout refactor/lock-free-quadtree
Compile the source code:Bashjavac -d bin src/Tracker/Tracker.java
Run the tracking engine:Bashjava -cp bin Tracker.Tracker

CLI Features & CapabilitiesMove cars (Simulate concurrent movement): Dispatches asynchronous route-traversal tasks across an ExecutorService thread pool. Each vehicle independently navigates dynamic transit paths using non-blocking random selection.Move a car to a specific stop: Updates a vehicle's coordinate state atomically to an existing transit stop (New Hall, Campus, DLI, Gate, Education, FSS, Sport, CITS).Track ALL cars: Formatted snapshot output displaying current coordinates and resolved transit stops in $\mathcal{O}(N)$ read time with zero write blocking.Track specific car: Direct $\mathcal{O}(1)$ lookup for an individual vehicle by ID.QuadTree Spatial Search: Executes a bounding-box range query on the QuadTree and filters results using the Haversine formula to return all vehicles within a specified radius (in kilometers) of a chosen landmark.Terminate System: Shuts down thread pools and cleanly terminates execution.Algorithmic ComplexityOperationBaseline ApproachThis Engine (Lock-Free + QuadTree)Coordinate Update$\mathcal{O}(1)$ (Thread-blocking lock)$\mathcal{O}(1)$ (Lock-free atomic compute)Vehicle Lookup (by ID)$\mathcal{O}(1)$$\mathcal{O}(1)$Spatial Proximity Search$\mathcal{O}(N)$ (Full fleet scan)$\mathcal{O}(\log N)$ (Hierarchical quadrant partition)Memory PublicationDefensive deep-copying ($\mathcal{O}(N)$)Immutable memory reference ($\mathcal{O}(1)$)