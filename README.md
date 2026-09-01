# Concurrent Geospatial Fleet Telemetry & QuadTree Tracker

A high-performance, concurrent vehicle telemetry tracking and transit simulation engine built in Java. The system pairs lock-free memory primitives (`ConcurrentHashMap`, immutable value snapshots) with a hierarchical **2D Point QuadTree** for spatial candidate pruning across the UNILAG campus transit network.

---

## Architectural Highlights

* **Lock-Free Concurrency Model:** Utilizes `ConcurrentHashMap` and immutable domain records (`VehicleRecord`, `Location`) to guarantee race-condition-free reads and atomic writes without coarse global synchronization.
* **Spatial Indexing with Candidate Pruning:** Implements a recursive 2D Point QuadTree that partitions geographic coordinate space and prunes candidates before precise Haversine filtering.
* **Two-Tier Search Pruning:** Employs fast bounding-box intersection testing for quadrant candidate pruning (broad phase) followed by precise Haversine circular distance validation (narrow phase).
* **Deterministic Route Graph & Deadlock-Free Fallbacks:** Features an explicit transit route network (`ROUTES`) paired with an insertion-ordered `LinkedHashMap` (`UNILAG_STOPS`) to preserve stop order and provide robust fallback paths for intermediate transit nodes.
* **Epsilon-Tolerant Geofencing:** Resolves raw floating-point coordinates to designated transit stop names using an epsilon threshold (ε = 0.0001) to prevent floating-point precision mismatches.
* **Thread-Safe Simulation Engine:** Uses dynamic thread pools (`ExecutorService`), `ThreadLocalRandom`, and `Callable<Void>` tasks with graceful timeout await logic to simulate concurrent vehicle movement.

---

## System Architecture

```text
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
         Atomic O(1) State Updates                Candidate-Pruned Proximity Searches
```

---

## File Structure

```text
Fleet-Tracker/
├── src/
│   └── Tracker/
│       ├── BoundingBox.java            # 2D coordinate range and bounding box intersection logic
│       ├── Location.java               # Immutable coordinate & timestamp entity
│       ├── QuadTreeNode.java           # Recursive QuadTree node subdivision and spatial queries
│       ├── SpatialVehicleTracker.java  # Lock-free registry & spatial query engine
│       ├── Tracker.java                # CLI menu, route graphs, and simulation loop
│       └── VehicleRecord.java          # Immutable vehicle entity and state updates
├── .gitignore
└── README.md
```

---

## Getting Started

### Prerequisites

* **Java Development Kit (JDK):** Version 11 or higher

### Build & Execution

1. Clone the repository and checkout the working branch:
```bash
    git clone https://github.com/oladokun-olayiwola/Fleet-Tracker.git
cd Fleet-Tracker
git checkout refactor/lock-free-quadtree
```

2. Compile all source files into a `bin` directory:
```bash
javac -d bin src/Tracker/*.java
```

3. Run the tracking engine:
```bash
java -cp bin Tracker.Tracker
```

---

## CLI Features & Capabilities

1. **Move cars (Simulate concurrent movement):** Dispatches asynchronous route-traversal tasks across an `ExecutorService` thread pool. Each vehicle independently navigates dynamic transit paths using non-blocking random selection.
2. **Move a car to a specific stop:** Updates a vehicle's coordinate state atomically to an existing transit stop (`New Hall`, `Campus`, `DLI`, `Gate`, `Education`, `FSS`, `Sport`, `CITS`).
3. **Track ALL cars:** Formatted snapshot output displaying current coordinates and resolved transit stops in O(N) read time with zero write blocking.
4. **Track specific car:** Direct O(1) lookup for an individual vehicle by ID.
5. **QuadTree Spatial Search:** Executes a bounding-box range query on the QuadTree and filters results using the Haversine formula to return all vehicles within a specified radius (in kilometers) of a chosen landmark.
6. **Terminate System:** Shuts down thread pools and cleanly terminates execution.

---

## Algorithmic Complexity

| Operation | Baseline Approach | This Engine (Lock-Free + QuadTree) |
| :--- | :--- | :--- |
| **Coordinate Update** | O(1) (Thread-blocking lock) | O(1) (Lock-free atomic compute) |
| **Vehicle Lookup (by ID)** | O(1) | O(1) |
| **Spatial Proximity Search** | O(N) (Full fleet scan) | O(N log N + k) avg per query (ephemeral QuadTree build + candidate filter) |
| **Memory Publication** | Defensive deep-copying (O(N)) | Immutable memory reference (O(1)) |
