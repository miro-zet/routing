package com.itera.routing.algos;

import com.itera.routing.model.ShortestPath;
import com.itera.routing.topology.Edge;
import com.itera.routing.topology.Node;
import com.itera.routing.utils.FibonacciHeap;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;

@Slf4j
@RequiredArgsConstructor
public class Dijkstra {

    public enum Direction {
        FORWARD,
        BACKWARD
    }

    private final Direction direction;
    private final Function<Node, Double> h;
    private Map<Node, Edge> predecessors = new HashMap<>();
    @Getter
    private Set<Node> visited = new HashSet<>();
    @Getter
    private Map<Node, Double> distances = new HashMap<>();
    // private PriorityQueue<Node> candidates = new PriorityQueue<>((n1, n2) -> priority(n1).compareTo(priority(n2))); //can be replaced by fibonacci heap for better performance
    private FibonacciHeap<Double, Node> candidates = new FibonacciHeap<>();

    @Getter
    @Setter
    private double lengthLimit = Double.MAX_VALUE;

    @Getter
    @Setter
    private BiPredicate<Node, Node> nodeRestriction = (n, neighbor) -> false;

    @Getter
    @Setter
    private int limit = Integer.MAX_VALUE;

    @Getter
    private Node lastVisited;


    @PostConstruct
    public void init() {

        visited.clear();
        candidates.clear();
        predecessors.clear();
        distances.clear();

    }

    public double getCandidateMinKey() {
        return candidates.getMinKey();
    }

    public void initRouting(Node node) {
        candidates.add(0.0, node);
        distances.put(node, 0.0);
    }


    public boolean doStep(Node to) {

        Node node = candidates.extractMin();
        if (visited.size() > limit || node == null || node.equals(to) || (!candidates.isEmpty() && candidates.getMinKey() > lengthLimit)) {
            return false;
        }

        visited.add(node);
        lastVisited = node;
        relaxNeighbors(node);

        return true;

    }

    public void route(Node from) {
        init();
        initRouting(from);
        Node nonexisting = new Node(-1, 0.0, 0.0);

        while (doStep(nonexisting)) {
        }
    }

    public ShortestPath findShortestPath(Node from, Node to) {
        init();
        initRouting(from);

        while (doStep(to)) {
        }

        //logSearch();
        return shortestPath(to);
    }

    private void logSearch() {

        List<Node> visitedList = new ArrayList<>(visited);
        log.info(
            "visited: {}", IntStream.range(0, visited.size())
                .filter(i -> i % 5 == 0)
                .mapToObj(i -> visitedList.get(i).getId())
                .toList()
        );
        log.info("visited size: {}", visited.size());
    }

    public ShortestPath shortestPath(Node to) {
        Double dist = distances.get(to);
        List<String> edges = new ArrayList<>();
        List<Long> nodes = new ArrayList<>();

        Node n = to;
        Edge e;

        nodes.add(n.getId());
        while ((e = predecessors.get(n)) != null) {
            edges.add(e.getId());
            n = e.getOtherNode(n);
            nodes.add(n.getId());
        }

        Collections.reverse(nodes);
        Collections.reverse(edges);
        ShortestPath sp = new ShortestPath();
        sp.setLength(dist != null ? dist : Double.MAX_VALUE);
        sp.setNodes(nodes);
        sp.setEdges(edges);

        //log.info("nodes: {}", nodes);
        return sp;

    }

    private void relaxNeighbors(Node node) {
        for (Edge edge : getEdges(node)) {
            Node neighbour = edge.getOtherNode(node);
            if (visited.contains(neighbour)) {
                continue;
            }
            if (nodeRestriction.test(node, neighbour)) {
                continue;
            }

            double newNeighbourCost = d(node) + edge.getWeight();
            if (d(neighbour) == null || d(neighbour) > newNeighbourCost) {
                distances.put(neighbour, newNeighbourCost);
                predecessors.put(neighbour, edge);
                candidates.add(newNeighbourCost + h.apply(neighbour), neighbour);
            }
        }
    }

    private Set<Edge> getEdges(Node node) {
        if (direction == Direction.FORWARD) {
            return node.getOutEdges();
        } else {
            return node.getInEdges();
        }
    }

    private Double d(Node node) {
        return distances.get(node);
    }


}
