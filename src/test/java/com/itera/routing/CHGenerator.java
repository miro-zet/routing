package com.itera.routing;

import com.itera.routing.algos.Dijkstra;
import com.itera.routing.algos.ch.Shortcut;
import com.itera.routing.loaders.CsvGraphLoader;
import com.itera.routing.model.ShortestPath;
import com.itera.routing.topology.Edge;
import com.itera.routing.topology.Graph;
import com.itera.routing.topology.Node;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class CHGenerator {

    private static final double EPS = 1e-9;

    private final Graph graph;
    private final Map<Node, Integer> nodeHierarchy = new HashMap<>();
    private final Dijkstra dijkstra = new Dijkstra(Dijkstra.Direction.FORWARD, n -> 0.0);

    private final String pathShortcuts;
    private final String pathHierarchy;
    private final String pathDefinitions;

    private List<Edge> shortcuts = new ArrayList<>();
    private List<Shortcut> shortcutDefinitions = new ArrayList<>();
    private Map<Pair<Long, Long>, Double> shortestDistances = new HashMap<>();

    public static void main(String[] args) throws Exception {

        CsvGraphLoader loader = new CsvGraphLoader(
            "/Users/marian.rosko/Projects/home/routing/data/sk_nodes.csv",
            "/Users/marian.rosko/Projects/home/routing/data/sk_edges.csv"
        );
        Graph graph = loader.load();
        CHGenerator generator = new CHGenerator(
            graph,
            "/Users/marian.rosko/Projects/home/routing/data/shortcuts.dat",
            "/Users/marian.rosko/Projects/home/routing/data/hierarchy.dat",
            "/Users/marian.rosko/Projects/home/routing/data/definitions.dat"
        );
        generator.generate();

    }

    public void generate() throws Exception {
        List<Node> orderedNodes = makeNodeHierarchy();
        for (int i = 0; i < orderedNodes.size(); i++) {
            nodeHierarchy.put(orderedNodes.get(i), i);
        }

        int processed = 0;
        for (Node n : orderedNodes) {
            contractNode(n);
            processed++;
            double pct = (processed * 1.0) / orderedNodes.size();
            if (processed % 1000 == 0) {
                log.info("Processed {}/{} nodes ({}%)", processed, orderedNodes.size(), (int) (pct * 100));
                log.info("Shortcuts: {}", shortcuts.size());
                log.info("nodes: {}", graph.getNodes().size());
                log.info("==========");
            }
        }

        log.info("Shortcuts: {}", shortcuts.size());
        log.info("Definitions: {}", shortcutDefinitions.size());

        writeShortcuts();
        writeHierarchy();
        writeDefinitions();
    }

    private void writeShortcuts() throws Exception {
        try (var out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(pathShortcuts)))) {
            out.writeInt(shortcuts.size());
            for (Edge e : shortcuts) {
                out.writeUTF(e.getId());
                out.writeLong(e.getFrom().getId());
                out.writeLong(e.getTo().getId());
                out.writeDouble(e.getWeight());
                out.writeInt(e.getMaxSpeed());
            }
        }
    }

    private void writeHierarchy() throws Exception {
        try (var out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(pathHierarchy)))) {
            out.writeInt(nodeHierarchy.size());
            for (Node n : nodeHierarchy.keySet()) {
                out.writeLong(n.getId());
                out.writeInt(nodeHierarchy.get(n));
            }
        }
    }

    private void writeDefinitions() throws Exception {
        try (var out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(pathDefinitions)))) {
            out.writeInt(shortcutDefinitions.size());
            for (Shortcut shortcut : shortcutDefinitions) {
                out.writeUTF(shortcut.getId());
                out.writeUTF(shortcut.getInEdgeId());
                out.writeUTF(shortcut.getOutEdgeId());
            }
        }
    }

    private void contractNode(Node n) {

        int nodePrio = nodeHierarchy.get(n);
        //log.info("Contracting node {}, prio: {}", n.getId(), nodePrio);

        List<Edge> inEdges = new ArrayList<>(n.getInEdges());
        List<Edge> outEdges = new ArrayList<>(n.getOutEdges());
        List<Edge> newShortcuts = new ArrayList<>();

        for (Edge in : inEdges) {

            Node from = in.getOtherNode(n);
            if (nodeHierarchy.get(from) <= nodePrio) {
                continue;
            }

            for (Edge out : outEdges) {

                Node to = out.getOtherNode(n);
                if (from.equals(to)) {
                    continue;
                }
                if (nodeHierarchy.get(to) <= nodePrio) {
                    continue;
                }

                //log.info("Contracting edge {}-{}", from.getId(), to.getId());
                dijkstra.setLengthLimit(in.getWeight() + out.getWeight());
                dijkstra.setNodeRestriction((node, neighbor) -> neighbor.equals(n) || nodeHierarchy.get(neighbor) <= nodePrio);
                ShortestPath sp = dijkstra.findShortestPath(from, to);

                if (sp.getLength() > (in.getWeight() + out.getWeight())) {

                    //create shortcut
                    Edge shortcut = new Edge(
                        UUID.randomUUID().toString(),
                        from,
                        to,
                        in.getWeight() + out.getWeight(),
                        0
                    );
                    Shortcut definition = new Shortcut(shortcut.getId(), in.getId(), out.getId());

                    if (shouldAdd(shortcut)) {
                        newShortcuts.add(shortcut);
                        shortcutDefinitions.add(definition);
                        shortestDistances.put(Pair.of(from.getId(), to.getId()), shortcut.getWeight());
                    }
                }
            }

        }

        //add shortcuts
        for (Edge sc : newShortcuts) {
            shortcuts.add(sc);
            sc.getFrom().getOutEdges().add(sc);
            sc.getTo().getInEdges().add(sc);
            graph.getEdges().put(sc.getId(), sc);
        }

        //remove edges
        for (Edge e : inEdges)
            removeEdge(e);
        for (Edge e : outEdges)
            removeEdge(e);
        graph.getNodes().remove(n.getId());
    }

    private boolean shouldAdd(Edge sc) {
        var key = Pair.of(sc.getFrom().getId(), sc.getTo().getId());
        Double best = shortestDistances.get(key);
        if (best == null) {
            return true;
        }
        return sc.getWeight() + EPS < best;
    }

    private void removeEdge(Edge e) {
        graph.getEdges().remove(e.getId());
        e.getFrom().getOutEdges().remove(e);
        e.getTo().getInEdges().remove(e);
    }


    List<Node> makeNodeHierarchy() {

        List<Edge> edges = new ArrayList<>(graph.getEdges().values());
        Collections.sort(edges, Comparator.comparingInt(Edge::getMaxSpeed));
        List<Node> nodesHierarchy = new ArrayList<>();

        Set<Node> processedNodes = new HashSet<>();
        for (Edge e : edges) {
            if (!processedNodes.contains(e.getFrom())) {
                nodesHierarchy.add(e.getFrom());
                processedNodes.add(e.getFrom());
            }
            if (!processedNodes.contains(e.getTo())) {
                nodesHierarchy.add(e.getTo());
                processedNodes.add(e.getTo());
            }

        }

        return nodesHierarchy;
    }
}
