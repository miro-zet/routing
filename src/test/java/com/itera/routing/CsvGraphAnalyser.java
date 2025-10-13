package com.itera.routing;

import com.itera.routing.loaders.CsvGraphLoader;
import com.itera.routing.topology.Edge;
import com.itera.routing.topology.Graph;
import com.itera.routing.topology.Node;
import com.itera.routing.utils.DistanceUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CsvGraphAnalyser {

    public static void main(String[] args) {

        CsvGraphLoader loader = new CsvGraphLoader(
            "/Users/marian.rosko/Projects/home/routing/data/sk_nodes.csv",
            "/Users/marian.rosko/Projects/home/routing/data/sk_edges.csv"
        );
        Graph graph = loader.load();

        log.info("edges: {}", graph.getEdges().size());

        int errors = 0;
        for (Edge edge : graph.getEdges().values()) {
            Node n1 = edge.getFrom();
            Node n2 = edge.getTo();

            double distance = edge.getWeight();
            double direct = DistanceUtils.getSphericalDistance(n1, n2);
            double diff = distance - direct;

            if (diff < -0.05) {
                log.error("edge: {}, distance: {}, direct: {}, diff: {}", edge, distance, direct, diff);
                errors++;
            }
        }
        log.info("all edges: {}, errors: {}", graph.getEdges().size(), errors);

    }
}
