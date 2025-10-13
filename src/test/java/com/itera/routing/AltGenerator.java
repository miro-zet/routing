package com.itera.routing;

import com.itera.routing.algos.Dijkstra;
import com.itera.routing.loaders.CsvGraphLoader;
import com.itera.routing.topology.Graph;
import com.itera.routing.topology.Node;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
public class AltGenerator {

    private final Graph graph;
    private final String altPath;

    private static final double MAX = 900000000.0;
    private static final double MIN = 40000.0;

    public static void main(String[] args) throws Exception {

        CsvGraphLoader loader = new CsvGraphLoader(
            "/Users/marian.rosko/Projects/home/routing/data/sk_nodes.csv",
            "/Users/marian.rosko/Projects/home/routing/data/sk_edges.csv"
        );
        Graph graph = loader.load();
        AltGenerator generator = new AltGenerator(graph, "/Users/marian.rosko/Projects/home/routing/data/alt.dat");

        generator.generate(List.of(graph.getRandomNode().getId()), 14);

    }

    public void generate(Collection<Long> nodeIds, int landmarkNum) throws Exception {

        Dijkstra fwd = new Dijkstra(Dijkstra.Direction.FORWARD, n -> 0.0);
        Dijkstra bckg = new Dijkstra(Dijkstra.Direction.BACKWARD, n -> 0.0);

        DataOutputStream das = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(altPath)));

        int localLandmarkNum = landmarkNum / nodeIds.size();
        if (localLandmarkNum == 0) {
            localLandmarkNum = 1;
        }
        int totalNodesFound = 0;
        Set<Long> foundNodes = new HashSet<>();
        Set<Node> forbiddenNodes = new HashSet<>();

        for (long nodeId : nodeIds) {

            Map<Node, Map<Node, Double>> altDistancesTo = new HashMap<>();
            Map<Node, Map<Node, Double>> altDistancesFrom = new HashMap<>();

            Node altNode = graph.getNode(nodeId);
            int localAltFound = 0;

            while (true) {

                altDistancesTo.put(altNode, new HashMap<>());
                altDistancesFrom.put(altNode, new HashMap<>());

                log.info("{}th ATL node: {}, GPS: {}", totalNodesFound, altNode, altNode.toPositionString());
                fwd.route(altNode);
                bckg.route(altNode);

                boolean isNodeOk = isAltNodeOk(altNode);
                if (isNodeOk) {
                    log.debug("ALT Node: {} is OK. Writing...", altNode);
                    totalNodesFound++;
                    localAltFound++;

                    for (Node node : graph.getNodes().values()) {
                        Double distanceTo = check("fwd", fwd.getDistances().get(node), altNode, node);
                        Double distanceFrom = check("bckg", bckg.getDistances().get(node), altNode, node);


                        altDistancesTo.get(altNode).put(node, distanceTo);
                        altDistancesFrom.get(altNode).put(node, distanceFrom);

                        das.writeLong(altNode.getId());
                        das.writeLong(node.getId());
                        das.writeDouble(distanceTo);
                        das.writeDouble(distanceFrom);
                    }
                }

                if (localAltFound >= localLandmarkNum) {
                    log.info("next input ALT will be processed");
                    break;
                }

                boolean isSearching = true;
                while (isSearching) {
                    altNode = chooseNextAltNode(altDistancesFrom, forbiddenNodes);
                    if (!isAltNodeOk(altNode)) {
                        forbiddenNodes.add(altNode);
                    } else {
                        isSearching = false;
                        foundNodes.add(altNode.getId());
                    }
                }

            }

        }

        System.out.println(foundNodes);
    }

    private Double check(String dijkstra, Double dist, Node from, Node to) {
        if (dist == null) {
            //log.error("dijkstra: {}, from: {}, to: {} is null", dijkstra, from, to);
            return Double.MAX_VALUE;
        }
        return dist;
    }

    private boolean isAltNodeOk(Node altNode) {

        Dijkstra dijkstaB = new Dijkstra(Dijkstra.Direction.BACKWARD, n -> 0.0);
        dijkstaB.route(altNode);

        for (Node node : graph.getNodes().values()) {
            Double distance = dijkstaB.getDistances().get(node);
            if (distance!=null && distance < MAX && distance > MIN) {
                log.info("next ALT candidate: {} is OK node", altNode);
                return true;
            }
        }

        log.warn("next ALT candidate: {} is NOT OK, search will be repeated", altNode);
        return false;
    }

    private Node chooseNextAltNode(Map<Node, Map<Node, Double>> altDistancesFrom, Set<Node> forbiddenNodes) {

        Map<Node, List<Double>> distances = new HashMap<>();
        for (Node altNode : altDistancesFrom.keySet()) {
            for (Node node : altDistancesFrom.get(altNode).keySet()) {

                double dist = altDistancesFrom.get(altNode).get(node);
                if (dist > MAX) {
                    continue;
                }
                if (distances.containsKey(node)) {
                    distances.get(node).add(dist);
                } else {
                    List<Double> dists = new ArrayList<>();
                    dists.add(dist);
                    distances.put(node, dists);
                }
            }
        }

        double maxDistance = 0;
        Node newAltNode = null;
        for (Node node : distances.keySet()) {

            if (altDistancesFrom.keySet().contains(node)) {
                continue;
            }
            if (distances.get(node) == null || distances.get(node).size() < altDistancesFrom.keySet().size()) {
                continue;
            }
            if (forbiddenNodes.contains(node)) {
                continue;
            }

            double avg = avg(distances.get(node));
            if (avg > maxDistance) {
                maxDistance = avg;
                newAltNode = node;
            }
        }

        log.info("max. dist: {}, distances: {}", maxDistance, altDistancesFrom.keySet().size());
        return newAltNode;
    }


    private double avg(List<Double> values) {

        double sum = 0;
        boolean isTooClose = false;
        for (double value : values) {
            if (value < MIN) {
                isTooClose = true;
            }
            sum = sum + value;
        }
        if (!isTooClose) {
            return sum / values.size();
        } else {
            return -1;
        }

    }


}
