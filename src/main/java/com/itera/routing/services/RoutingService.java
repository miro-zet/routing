package com.itera.routing.services;

import ch.qos.logback.classic.Logger;
import com.itera.routing.algos.alt.ALTData;
import com.itera.routing.algos.alt.ALTHeuristics;
import com.itera.routing.algos.Dijkstra;
import com.itera.routing.algos.ch.CHData;
import com.itera.routing.model.ShortestPath;
import com.itera.routing.topology.Graph;
import com.itera.routing.topology.Node;
import com.itera.routing.utils.DistanceUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoutingService {

    private final Graph graph;
    private final ALTData data;
    private final CHData chData;


    public ShortestPath findByDijkstra(long fromId, long toId, Dijkstra.Direction dir) {
        Node from = graph.getNode(fromId);
        Node to = graph.getNode(toId);

        Dijkstra dijkstra;
        switch (dir) {
            case FORWARD:
                dijkstra = new Dijkstra(Dijkstra.Direction.FORWARD, n -> 0.0);
                return dijkstra.findShortestPath(from, to);
            case BACKWARD:
                dijkstra = new Dijkstra(Dijkstra.Direction.BACKWARD, n -> 0.0);
                return dijkstra.findShortestPath(to, from);
            default:
                throw new IllegalArgumentException("Unsupported direction: " + dir);
        }
    }

    public ShortestPath findByBidirectionalDijkstra(long fromId, long toId) {

        Node from = graph.getNode(fromId);
        Node to = graph.getNode(toId);

        Dijkstra dijkstraFwd = new Dijkstra(Dijkstra.Direction.FORWARD, n -> 0.0);
        dijkstraFwd.initRouting(from);
        Dijkstra dijkstraBckg = new Dijkstra(Dijkstra.Direction.BACKWARD, n -> 0.0);
        dijkstraBckg.initRouting(to);

        double min = Double.MAX_VALUE;
        Node join = null;
        boolean hasFound = false;
        while (!hasFound) {
            dijkstraFwd.doStep(to);
            dijkstraBckg.doStep(from);

            Set<Node> visitedByFwd = dijkstraFwd.getVisited();
            Set<Node> visitedByBckg = dijkstraBckg.getVisited();
            Set<Node> overlap = visitedByFwd.stream()
                .filter(visitedByBckg::contains)
                .collect(Collectors.toSet());

            if (overlap.size() < 100) {
                continue;
            }

            for (Node node : overlap) {
                double d1 = dijkstraFwd.getDistances().get(node);
                double d2 = dijkstraBckg.getDistances().get(node);
                double d = d1 + d2;
                if (d < min) {
                    min = d;
                    join = node;
                    hasFound = true;
                }
            }
        }

        ShortestPath shortestPathFwd = dijkstraFwd.findShortestPath(from, join);
        ShortestPath shortestPathBckg = dijkstraBckg.findShortestPath(to, join);

        log.info("Joined visited: {}", dijkstraFwd.getVisited().size() + dijkstraBckg.getVisited().size());
        return shortestPathFwd.add(shortestPathBckg);
    }

    public ShortestPath findByCHDijkstra(long fromId, long toId) {

        Graph chGraph = chData.getGraph();
        Node from = chGraph.getNode(fromId);
        Node to = chGraph.getNode(toId);

        Dijkstra dijkstraFwd = new Dijkstra(Dijkstra.Direction.FORWARD, n -> 0.0);
        dijkstraFwd.initRouting(from);
        dijkstraFwd.setNodeRestriction((node, neightbor) -> chData.getHierarchies().get(node) >= chData.getHierarchies().get(neightbor));

        Dijkstra dijkstraBckg = new Dijkstra(Dijkstra.Direction.BACKWARD, n -> 0.0);
        dijkstraBckg.initRouting(to);
        dijkstraBckg.setNodeRestriction((node, neightbor) -> chData.getHierarchies().get(node) >= chData.getHierarchies().get(neightbor));

        double min = Double.MAX_VALUE;
        Node join = null;
        boolean isOkToStop = false;
        while (!isOkToStop) {
            dijkstraFwd.doStep(to);
            dijkstraBckg.doStep(from);

            Set<Node> visitedByFwd = dijkstraFwd.getVisited();
            Set<Node> visitedByBckg = dijkstraBckg.getVisited();
            Set<Node> overlap = visitedByFwd.stream()
                .filter(visitedByBckg::contains)
                .collect(Collectors.toSet());

            if (overlap.isEmpty()) {
                continue;
            }

            for (Node node : overlap) {
                double d1 = dijkstraFwd.getDistances().get(node);
                double d2 = dijkstraBckg.getDistances().get(node);
                double d = d1 + d2;
                if (d < min) {
                    min = d;
                    join = node;
                }

                double fwdMinKey = dijkstraFwd.getCandidateMinKey();
                double bckgMinKey = dijkstraBckg.getCandidateMinKey();

                if(fwdMinKey>=min ||  bckgMinKey >= min){
                    isOkToStop = true;
                }
            }
        }

        ShortestPath shortestPathFwd = dijkstraFwd.findShortestPath(from, join);
        ShortestPath shortestPathBckg = dijkstraBckg.findShortestPath(to, join);

        log.info("Joined visited: {}", dijkstraFwd.getVisited().size() + dijkstraBckg.getVisited().size());
        return shortestPathFwd.add(shortestPathBckg);
    }


    public ShortestPath findByAstar(long fromId, long toId) {
        Node from = graph.getNode(fromId);
        Node to = graph.getNode(toId);

        Dijkstra dijkstra = new Dijkstra(
            Dijkstra.Direction.FORWARD,
            n -> DistanceUtils.getSphericalDistance(n, to)
        );
        return dijkstra.findShortestPath(from, to);
    }

    public ShortestPath findByAlt(long fromId, long toId) {
        Node from = graph.getNode(fromId);
        Node to = graph.getNode(toId);
        ALTHeuristics heuristics = new ALTHeuristics(to, data);

        Dijkstra dijkstra = new Dijkstra(
            Dijkstra.Direction.FORWARD,
            n -> heuristics.heuristics(n)
        );
        return dijkstra.findShortestPath(from, to);
    }

}
