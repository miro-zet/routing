package com.itera.routing.vizualization;

import com.itera.routing.algos.Dijkstra;
import com.itera.routing.algos.alt.ALTData;
import com.itera.routing.algos.alt.ALTHeuristics;
import com.itera.routing.algos.ch.CHData;
import com.itera.routing.algos.ch.CHLoader;
import com.itera.routing.loaders.CsvGraphLoader;
import com.itera.routing.model.ShortestPath;
import com.itera.routing.topology.Edge;
import com.itera.routing.topology.Graph;
import com.itera.routing.topology.Node;
import com.itera.routing.utils.DistanceUtils;
import io.vavr.control.Try;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.Pair;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class RoutingVisualizer extends Application {

    private static final Logger log = LoggerFactory.getLogger(RoutingVisualizer.class);
    final int width = 1500;
    final int height = 900;
    int nodeSize = 5;
    int offset = 20;

    double dlon;
    double dlat;

    double lonMax;
    double latMax;
    double lonMin;
    double latMin;

    String algo = "Dijkstra";

    Graph graph;
    ALTData altData;
    CHData chData;

    static long fromId = 347582937L;
    static long toId = 59153855L;

    Timeline timeline;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        Canvas canvas = new Canvas(width, height);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        initGraph();

        Label settledLabel = new Label("Settled: ");
        TextField settled = new TextField("0");

        Label lengthLabel = new Label("length: ");
        TextField length = new TextField("0");

        Button run = new Button("run");
        run.setOnAction(event -> runAlgo(gc, settled, length));

        Button init = new Button("init");
        init.setOnAction(event -> Try.run(() -> initAlgo(gc, settled, length)));

        Button show = new Button("shortcuts");
        show.setOnAction(event -> Try.run(() -> showShortcuts(gc)));

        ComboBox<String> combo = new ComboBox();
        combo.getItems().addAll(List.of("Dijkstra", "Bidir", "A*", "ALT", "CH"));
        combo.setOnAction(event -> Try.run(() -> initAlgo(combo.getValue(), gc, settled, length)));
        combo.setValue("Dijkstra");

        HBox controls = new HBox();
        controls.getChildren().addAll(combo, run, init, show, settledLabel, settled, lengthLabel, length);

        VBox root = new VBox();
        root.getChildren().add(canvas);
        root.getChildren().add(controls);
        Scene scene = new Scene(root, width + 50, height + 200);

        initCanvas(gc);

        primaryStage.setTitle("Map Matching Visualizer");
        primaryStage.setScene(scene);
        primaryStage.show();

    }

    void runAlgo(GraphicsContext gc, TextField settled, TextField length) {

        Node from = graph.getNode(fromId);
        Node to = graph.getNode(toId);
        switch (algo) {
            case "Dijkstra":
                dijkstra(gc, from, to, settled, length);
                break;
            case "A*":
                aStar(gc, from, to, settled, length);
                break;
            case "ALT":
                aALT(gc, from, to, settled, length);
                break;
            case "Bidir":
                bidijkstra(gc, from, to, 1, settled, length);
                break;
            case "CH":
                chDijkstra(gc, 1, settled, length);
                break;
        }
    }

    void dijkstra(GraphicsContext gc, Node from, Node to, TextField settled, TextField length) {

        Dijkstra dijkstra = new Dijkstra(Dijkstra.Direction.FORWARD, n -> 0.0);
        dijkstra.init();
        dijkstra.initRouting(from);

        dijkstra(dijkstra, gc, from, to, 1, settled, length);

    }

    void aStar(GraphicsContext gc, Node from, Node to, TextField settled, TextField length) {

        Dijkstra dijkstra = new Dijkstra(Dijkstra.Direction.FORWARD, n -> DistanceUtils.getSphericalDistance(n, to));
        dijkstra.init();
        dijkstra.initRouting(from);

        dijkstra(dijkstra, gc, from, to, 1, settled, length);

    }

    void aALT(GraphicsContext gc, Node from, Node to, TextField settled, TextField length) {

        ALTHeuristics heuristics = new ALTHeuristics(to, altData);
        Dijkstra dijkstra = new Dijkstra(Dijkstra.Direction.FORWARD, n -> heuristics.heuristics(n));
        dijkstra.init();
        dijkstra.initRouting(from);

        dijkstra(dijkstra, gc, from, to, 1, settled, length);

    }

    void dijkstra(
        Dijkstra dijkstra,
        GraphicsContext gc,
        Node from,
        Node to,
        int pause,
        TextField settled,
        TextField length
    ) {

        timeline = new Timeline(new KeyFrame(
            Duration.millis(pause),
            event -> {
                boolean isRunning = dijkstra.doStep(to);
                settled.setText(String.valueOf(dijkstra.getVisited().size()));
                if (!isRunning) {
                    timeline.stop();
                    ShortestPath sp = dijkstra.shortestPath(to);
                    for (String e : sp.getEdges()) {
                        drawEdge(graph.getEdges().get(e), gc, 4, Color.BLUE);
                    }
                    length.setText(String.valueOf((int) sp.getLength()));
                }
                drawNode(dijkstra.getLastVisited(), gc, 2 * nodeSize, Color.RED);
            }
        ));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

    }

    void bidijkstra(GraphicsContext gc, Node from, Node to, int pause, TextField settled, TextField length) {

        Dijkstra dijkstraFwd = new Dijkstra(Dijkstra.Direction.FORWARD, n -> 0.0);
        dijkstraFwd.initRouting(from);
        Dijkstra dijkstraBckg = new Dijkstra(Dijkstra.Direction.BACKWARD, n -> 0.0);
        dijkstraBckg.initRouting(to);

        timeline = new Timeline(new KeyFrame(
            Duration.millis(pause),
            event -> {

                double min = Double.MAX_VALUE;
                Node join = null;

                dijkstraFwd.doStep(to);
                dijkstraBckg.doStep(from);

                Set<Node> visitedByFwd = dijkstraFwd.getVisited();
                Set<Node> visitedByBckg = dijkstraBckg.getVisited();
                Set<Node> overlap = visitedByFwd.stream()
                    .filter(visitedByBckg::contains)
                    .collect(Collectors.toSet());

                if (overlap.size() > 100) {
                    timeline.stop();

                    for (Node node : overlap) {
                        double d1 = dijkstraFwd.getDistances().get(node);
                        double d2 = dijkstraBckg.getDistances().get(node);
                        double d = d1 + d2;
                        if (d < min) {
                            min = d;
                            join = node;
                        }
                    }

                    ShortestPath shortestPathFwd = dijkstraFwd.findShortestPath(from, join);
                    ShortestPath shortestPathBckg = dijkstraBckg.findShortestPath(to, join);
                    ShortestPath sp = shortestPathFwd.add(shortestPathBckg);

                    for (String e : sp.getEdges()) {
                        drawEdge(graph.getEdges().get(e), gc, 4, Color.BLUE);
                    }

                    length.setText(String.valueOf((int) sp.getLength()));

                }

                settled.setText(String.valueOf(dijkstraFwd.getVisited().size() + dijkstraBckg.getVisited().size()));
                drawNode(dijkstraFwd.getLastVisited(), gc, 2 * nodeSize, Color.RED);
                drawNode(dijkstraBckg.getLastVisited(), gc, 2 * nodeSize, Color.YELLOW);

            }
        ));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

    }

    @Getter
    @Setter
    @AllArgsConstructor
    @ToString
    class Mins {
        private double min;
        private Node node;
    }

    void chDijkstra(GraphicsContext gc, int pause, TextField settled, TextField length) {

        Graph chGraph = chData.getGraph();
        Node from = chGraph.getNode(fromId);
        Node to = chGraph.getNode(toId);

        Dijkstra dijkstraFwd = new Dijkstra(Dijkstra.Direction.FORWARD, n -> 0.0);
        dijkstraFwd.initRouting(from);
        dijkstraFwd.setNodeRestriction((node, neightbor) -> chData.getHierarchies().get(node) >= chData.getHierarchies()
                                                                                                       .get(neightbor));

        Dijkstra dijkstraBckg = new Dijkstra(Dijkstra.Direction.BACKWARD, n -> 0.0);
        dijkstraBckg.initRouting(to);
        dijkstraBckg.setNodeRestriction((node, neightbor) -> chData.getHierarchies()
                                                                   .get(node) >= chData.getHierarchies()
                                                                                       .get(neightbor));


        final Mins mins = new Mins(Double.MAX_VALUE, null);
        timeline = new Timeline(new KeyFrame(
            Duration.millis(pause),
            event -> {

                dijkstraFwd.doStep(to);
                dijkstraBckg.doStep(from);

                Set<Node> visitedByFwd = dijkstraFwd.getVisited();
                Set<Node> visitedByBckg = dijkstraBckg.getVisited();
                Set<Node> overlap = visitedByFwd.stream()
                    .filter(visitedByBckg::contains)
                    .collect(Collectors.toSet());

                drawNode(dijkstraFwd.getLastVisited(), gc, 2 * nodeSize, Color.RED);
                drawNode(dijkstraBckg.getLastVisited(), gc, 2 * nodeSize, Color.YELLOW);
                settled.setText(String.valueOf(dijkstraFwd.getVisited().size() + dijkstraBckg.getVisited().size()));

                if (!overlap.isEmpty()) {

                    log.info("found overlap: {}", overlap);
                    for (Node node : overlap) {
                        double d1 = dijkstraFwd.getDistances().get(node);
                        double d2 = dijkstraBckg.getDistances().get(node);
                        double d = d1 + d2;
                        if (d < mins.getMin()) {
                            mins.setMin(d);
                            mins.setNode(node);
                            log.info("updating min: {} to {}, {}", mins, d, node);
                        }
                    }

                    double fwdMinKey = dijkstraFwd.getCandidateMinKey();
                    double bckgMinKey = dijkstraBckg.getCandidateMinKey();
                    log.info("fwdMinKey: {}, bckgMinKey: {}", fwdMinKey, bckgMinKey);

                    if (fwdMinKey >= mins.getMin() || bckgMinKey >= mins.getMin()) {
                        timeline.stop();
                        log.info("stopping timeline");
                        log.info("mins: {}", mins);

                        ShortestPath shortestPathFwd = dijkstraFwd.findShortestPath(from, mins.getNode());
                        ShortestPath shortestPathBckg = dijkstraBckg.findShortestPath(to, mins.getNode());
                        ShortestPath sp = shortestPathFwd.add(shortestPathBckg);

                        for (String e : sp.getEdges()) {
                            drawEdge(chGraph.getEdges().get(e), gc, 7, Color.BLUE);
                        }

                        List<Edge> originals = chData.getOriginalEdges(sp.getEdges().stream()
                            .map(id -> chGraph.getEdges().get(id))
                            .toList());
                        for (Edge e : originals) {
                            drawEdge(e, gc, 4, Color.RED, 10d);
                        }

                        length.setText(String.valueOf((int) sp.getLength()));
                    }

                }


            }
        ));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

    }


    void initAlgo(GraphicsContext gc, TextField settled, TextField length) throws Exception {
        initCanvas(gc);
        settled.setText("0");
        length.setText("0");
    }

    void showShortcuts(GraphicsContext gc) throws Exception {
        drawShortcuts(gc);
        drawGraph(gc);
    }

    void initAlgo(String algo, GraphicsContext gc, TextField settled, TextField length) throws Exception {

        this.algo = algo;
        settled.setText("0");
        length.setText("0");
        initCanvas(gc);
        switch (algo) {
            case "ALT":
                drawALT(gc);
                break;
            default:
        }
    }

    private void drawShortcuts(GraphicsContext gc) {
        for (Edge e : chData.getGraph().getEdges().values()) {
            drawEdge(e, gc, 1, Color.BLUE);
        }
    }

    void initGraph() throws Exception {
        CsvGraphLoader loader = new CsvGraphLoader(
            "/Users/marian.rosko/Projects/home/routing/data/sk_nodes.csv",
            "/Users/marian.rosko/Projects/home/routing/data/sk_edges.csv"
        );

        altData = new ALTData("/Users/marian.rosko/Projects/home/routing/data/alt.dat");
        altData.load();

        CHLoader chLoader = new CHLoader(
            "/Users/marian.rosko/Projects/home/routing/data/shortcuts.dat",
            "/Users/marian.rosko/Projects/home/routing/data/hierarchy.dat",
            "/Users/marian.rosko/Projects/home/routing/data/definitions.dat",
            loader
        );
        chData = chLoader.load();

        graph = loader.load();
        lonMin = graph.lonMin();
        lonMax = graph.lonMax();
        latMin = graph.latMin();
        latMax = graph.latMax();

        dlon = lonMax - lonMin;
        dlat = latMax - latMin;
    }

    void initCanvas(GraphicsContext gc) throws Exception {

        gc.clearRect(0, 0, width, height);
        drawGraph(gc);
        drawFromTo(gc);
    }

    private void drawFromTo(GraphicsContext gc) {

        drawNode(graph.getNode(fromId), gc, 3 * nodeSize, Color.VIOLET);
        drawNode(graph.getNode(toId), gc, 3 * nodeSize, Color.VIOLET);

    }

    void drawNode(Node node, GraphicsContext gc, int nodeSize, Color color) {

        gc.setFill(color);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1);
        gc.fillOval(
            offset + calculateX(node.getLon()) - nodeSize / 2,
            offset + calculateY(node.getLat()) - nodeSize / 2,
            nodeSize,
            nodeSize
        );
        gc.strokeOval(
            offset + calculateX(node.getLon()) - nodeSize / 2,
            offset + calculateY(node.getLat()) - nodeSize / 2,
            nodeSize,
            nodeSize
        );
    }

    void drawEdge(Edge e, GraphicsContext gc, int lineWidth, Color color) {

       drawEdge(e, gc, lineWidth, color, null);
    }

    void drawEdge(Edge e, GraphicsContext gc, int lineWidth, Color color, Double dash) {

        gc.setLineWidth(lineWidth);
        gc.setStroke(color);
        gc.setLineDashes(dash == null ? new double[]{} : new double[]{dash, dash});
        gc.strokeLine(
            offset + calculateX(e.getFrom().getLon()),
            offset + calculateY(e.getFrom().getLat()),
            offset + calculateX(e.getTo().getLon()),
            offset + calculateY(e.getTo().getLat())
        );
    }

    void drawALT(GraphicsContext gc) {

        for (Long landmark : altData.getLandmarks()) {
            Node n = graph.getNode(landmark);
            if (n != null) {
                drawNode(n, gc, 3 * nodeSize, Color.GREENYELLOW);
            }
        }

    }

    void drawGraph(GraphicsContext gc) throws Exception {

        for (Node node : graph.getNodes().values()) {

            drawNode(node, gc, nodeSize, Color.gray(0.8));
            for (Edge e : node.getOutEdges()) {
                drawEdge(e, gc, 1, Color.BLACK);
            }

        }


    }

    double calculateX(double lon) {
        return (lon - lonMin) * (width - 2 * offset) / dlon;
    }

    double calculateY(double lat) {
        return (lat - latMin) * (height - 2 * offset) / dlat;
    }

}
