package com.itera.routing.algos.ch;

import com.itera.routing.loaders.CsvGraphLoader;
import com.itera.routing.topology.Edge;
import com.itera.routing.topology.Graph;
import com.itera.routing.topology.Node;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CHLoader {

    @Value("${path.shortcuts}")
    private final String pathShortcuts;

    @Value("${path.hierarchy}")
    private final String pathHierarchies;

    @Value("${path.definitions}")
    private final String pathDefinitions;

    private final CsvGraphLoader basicLoader;

    public CHData load() throws Exception {

        Graph graph = basicLoader.load();
        Map<Node, Integer> hierarchies = new HashMap<>();
        Map<Edge, Pair<Edge, Edge>> definitions = new HashMap<>();

        try (var in = new DataInputStream(new BufferedInputStream(new FileInputStream(pathShortcuts)))) {
            int shortcuts = in.readInt();
            log.info("shortcuts: {}", shortcuts);

            for (int i = 0; i < shortcuts; i++) {
                String id = in.readUTF();
                long fromId = in.readLong();
                long toId = in.readLong();
                double weight = in.readDouble();
                int maxSpeed = in.readInt();

                log.debug("Shortcut [{}]: {} -> {} ({}m, {})", id, fromId, toId, weight, maxSpeed);
                Node from = graph.getNode(fromId);
                Node to = graph.getNode(toId);
                Edge shortcut = new Edge(id, from, to, weight, maxSpeed);

                from.getOutEdges().add(shortcut);
                to.getInEdges().add(shortcut);
                graph.getEdges().put(shortcut.getId(), shortcut);

            }

        }

        try (var in = new DataInputStream(new BufferedInputStream(new FileInputStream(pathHierarchies)))) {

            int levels = in.readInt();
            log.info("levels: {}", levels);

            for (int i = 0; i < levels; i++) {
                long nodeId = in.readLong();
                int level = in.readInt();

                log.debug("Node [{}]: {}", nodeId, level);
                hierarchies.put(graph.getNode(nodeId), level);
            }

        }

        try (var in = new DataInputStream(new BufferedInputStream(new FileInputStream(pathDefinitions)))) {
            int definitionsNum = in.readInt();
            log.info("definitions: {}", definitionsNum);
            for (int i = 0; i < definitionsNum; i++) {
                String id = in.readUTF();
                String inEdgeId = in.readUTF();
                String outEdgeId = in.readUTF();

                log.debug("Definition [{}]: {} -> {}", id, inEdgeId, outEdgeId);
                Edge shortcut = graph.getEdges().get(id);
                Edge inEdge = graph.getEdges().get(inEdgeId);
                Edge outEdge = graph.getEdges().get(outEdgeId);

                definitions.put(shortcut, Pair.of(inEdge, outEdge));
            }
        }

        return new CHData(graph, hierarchies, definitions);

    }


}
