# Routing algos

This project contains implementations of several shortest-path routing algorithms and
exposes them via a simple Spring Boot REST web service. 
A small JavaFX visualizer is also included for visualizing the results.

## Algos
- Dijkstra (forward and backward)
- Bidirectional Dijkstra
- A*
- ALT (A*, Landmarks, Triangle inequality)
- CH (Contraction Hierarchies)

## How to run the web service
Prerequisites:
- Java 21+
- Maven 3.9+

Data files:
- The service loads a small demo road network from CSV/binary files defined in `src/main/resources/application.properties`:
  - `path.nodes=data/sk_nodes.csv`
  - `path.edges=data/sk_edges.csv`
  - `path.alt=data/alt.dat`
  - `path.shortcuts=data/shortcuts.dat`
  - `path.hierarchy=data/hierarchy.dat`
  - `path.definitions=data/definitions.dat`
The default files are already present under the `data/` folder. Adjust paths in `application.properties` if you want to use your own data.

Start the service (default port 8080):
- Using Maven (recommended during development):
  - `mvn spring-boot:run`
- Or build a jar and run:
  - `mvn -DskipTests package`
  - `java -jar target/routing-0.0.1-SNAPSHOT.jar`

Once running, the API is available at `http://localhost:8080`.

## REST API
Base path: `/route/find`

All endpoints accept two required query parameters:
- `from` — source node ID (long)
- `to` — target node ID (long)

Response shape (JSON):
- `length` (number) — total path length (sum of edge weights)
- `edges` (array of string) — ordered list of edge IDs
- `nodes` (array of number) — ordered list of node IDs on the path

Endpoints:
- GET `/route/find/dijkstra/fwd?from={id}&to={id}` — Dijkstra forward search
- GET `/route/find/dijkstra/bckg?from={id}&to={id}` — Dijkstra backward search
- GET `/route/find/dijkstra/bidir?from={id}&to={id}` — Bidirectional Dijkstra
- GET `/route/find/dijkstra/ch?from={id}&to={id}` — Dijkstra over Contraction Hierarchies
- GET `/route/find/astar?from={id}&to={id}` — A* search
- GET `/route/find/alt?from={id}&to={id}` — ALT search

Example requests:
- `curl "http://localhost:8080/route/find/astar?from=1001&to=2050"`
- `curl "http://localhost:8080/route/find/dijkstra/bidir?from=1001&to=2050"`

Notes:
- Node and edge IDs refer to entries in your data files (e.g., `data/sk_nodes.csv` and `data/sk_edges.csv`).
- If you change data paths, restart the application for the changes to take effect.

## Applications
- web-service (Spring Boot REST API)
- vizualizer (JavaFX) — can be launched via your IDE using `com.itera.routing.vizualization.RoutingVisualizer` or with the JavaFX Maven plugin.


# Routing algos

This project contains implementations of several shortest-path routing algorithms and exposes them via a simple Spring Boot REST web service. A small JavaFX visualizer is also included for experimentation.

## Algos
- Dijkstra (forward and backward)
- Bidirectional Dijkstra
- A*
- ALT (A*, Landmarks, Triangle inequality)
- CH (Contraction Hierarchies)

## How to run the web service
Prerequisites:
- Java 21+
- Maven 3.9+

Data files:
- The service loads a small demo road network from CSV/binary files defined in `src/main/resources/application.properties`:
  - `path.nodes=data/sk_nodes.csv`
  - `path.edges=data/sk_edges.csv`
  - `path.alt=data/alt.dat`
  - `path.shortcuts=data/shortcuts.dat`
  - `path.hierarchy=data/hierarchy.dat`
  - `path.definitions=data/definitions.dat`
The default files are already present under the `data/` folder. Adjust paths in `application.properties` if you want to use your own data.

Start the service (default port 8080):
- Using Maven (recommended during development):
  - `mvn spring-boot:run`
- Or build a jar and run:
  - `mvn -DskipTests package`
  - `java -jar target/routing-0.0.1-SNAPSHOT.jar`

Once running, the API is available at `http://localhost:8080`.

## REST API
Base path: `/route/find`

All endpoints accept two required query parameters:
- `from` — source node ID (long)
- `to` — target node ID (long)

Response shape (JSON):
- `length` (number) — total path length (sum of edge weights)
- `edges` (array of string) — ordered list of edge IDs
- `nodes` (array of number) — ordered list of node IDs on the path

Endpoints:
- GET `/route/find/dijkstra/fwd?from={id}&to={id}` — Dijkstra forward search
- GET `/route/find/dijkstra/bckg?from={id}&to={id}` — Dijkstra backward search
- GET `/route/find/dijkstra/bidir?from={id}&to={id}` — Bidirectional Dijkstra
- GET `/route/find/dijkstra/ch?from={id}&to={id}` — Dijkstra over Contraction Hierarchies
- GET `/route/find/astar?from={id}&to={id}` — A* search
- GET `/route/find/alt?from={id}&to={id}` — ALT search

Example requests:
- `curl "http://localhost:8080/route/find/astar?from=1001&to=2050"`
- `curl "http://localhost:8080/route/find/dijkstra/bidir?from=1001&to=2050"`

Notes:
- Node and edge IDs refer to entries in your data files (e.g., `data/sk_nodes.csv` and `data/sk_edges.csv`).
- If you change data paths, restart the application for the changes to take effect.

## How to run the JavaFX visualizer (RoutingVisualizer)
Prerequisites:
- Java 21+
- Maven 3.9+
- JavaFX runtime (already declared as Maven deps: `org.openjfx:javafx-controls` and `org.openjfx:javafx-fxml` version 22.0.1)
- OS-specific notes:
  - macOS: works out of the box with recent JDKs; allow the app to open a window.
  - Linux: ensure GTK 3 and OpenGL drivers are installed (`libgtk-3`, `libcanberra-gtk3`); some distros need `xorg-x11-fonts`.
  - Windows: no extra steps typically required.

Data files used by the visualizer:
- The visualizer loads exactly the same demo data as the web service, but the file paths are currently hard-coded inside `RoutingVisualizer.initGraph()`.
- Open `src/main/java/com/itera/routing/vizualization/RoutingVisualizer.java` and adjust these lines to match your local project path if needed:
  - nodes: `.../data/sk_nodes.csv`
  - edges: `.../data/sk_edges.csv`
  - ALT: `.../data/alt.dat`
  - CH shortcuts: `.../data/shortcuts.dat`
  - CH hierarchy: `.../data/hierarchy.dat`
  - CH definitions: `.../data/definitions.dat`
Tip: you can replace these with absolute paths to your cloned project’s `data/` directory, or refactor them to relative paths like `"data/sk_nodes.csv"` for convenience.

Start options:
- Using Maven (recommended):
  - `mvn -DskipTests javafx:run`
  The project includes the JavaFX Maven plugin configured with `mainClass=com.itera.routing.vizualization.RoutingVisualizer`.
- From IntelliJ IDEA or another IDE:
  - Import the project as a Maven project so JavaFX dependencies are on the classpath.
  - Create a run configuration with Main class: `com.itera.routing.vizualization.RoutingVisualizer`.
  - If you get `java.lang.NoClassDefFoundError: javafx/application/Application` or similar, run via Maven goal `javafx:run` or add VM options: `--add-modules=javafx.controls,javafx.fxml`.

What you should see and how to use it:
- A window with the graph drawn and simple controls below.
- Controls:
  - Algorithm selector: Dijkstra, Bidir, A*, ALT, CH.
  - Buttons: `run` (step the chosen algorithm with animation), `init` (reset and redraw), `shortcuts` (draw CH shortcuts overlay).
  - Readouts: `Settled` (number of visited nodes), `length` (length of the found path).
- Default source/target nodes are hardcoded as:
  - `fromId = 347582937L`
  - `toId = 59153855L`
  You can change these in `RoutingVisualizer.java` to visualize different routes.

Troubleshooting:
- If the canvas is empty or you get file-not-found errors, double-check the data file paths in `RoutingVisualizer.initGraph()`.
- On Linux, if the app fails to start due to GTK/GL errors, install the GTK 3 and OpenGL packages noted above and try again.

## Applications
- web-service (Spring Boot REST API)
- vizualizer (JavaFX) — can be launched via your IDE using `com.itera.routing.vizualization.RoutingVisualizer` or with the JavaFX Maven plugin.
