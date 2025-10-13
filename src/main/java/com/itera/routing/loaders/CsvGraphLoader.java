package com.itera.routing.loaders;

import com.itera.routing.topology.Edge;
import com.itera.routing.topology.Graph;
import com.itera.routing.topology.Node;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = {"path.nodes", "path.edges"})
public class CsvGraphLoader implements GraphLoader {

    @Value("${path.nodes}")
    private final String nodesPath;

    @Value("${path.edges}")
    private final String edgesPath;

    private CSVParser parser = new CSVParserBuilder()
        .withSeparator(',')
        .withQuoteChar('"')
        .build();

    public Graph load() {

        Graph graph = new Graph();
        try {
            log.info("reading nodes from: {}", nodesPath);
            Map<Long, Node> nodes = Files.readAllLines(Paths.get(nodesPath)).stream()
                .skip(1)
                .map(line -> toNode(line))
                .collect(Collectors.toMap(n -> n.getId(), n -> n));
            graph.getNodes().putAll(nodes);

            log.info("reading edges from: {}", edgesPath);
            Map<String, Edge> edges = Files.readAllLines(Paths.get(edgesPath)).stream()
                .skip(1)
                .map(line -> toEdge(line, graph))
                .collect(Collectors.toMap(e -> e.getId(), e -> e));
            graph.getEdges().putAll(edges);

        } catch (Exception e) {
            log.error("cannot create graph", e);
        }

        return graph;
    }

    private Edge toEdge(String line, Graph graph) {
        String[] data = Try.of(() -> parser.parseLine(line)).get();

        Edge edge = new Edge(
            data[0],
            graph.getNode(Long.parseLong(data[1])),
            graph.getNode(Long.parseLong(data[2])),
            Double.parseDouble(data[6]),
            Double.valueOf(data[7]).intValue()
        );

        edge.getFrom().getOutEdges().add(edge);
        edge.getTo().getInEdges().add(edge);
        return edge;
    }

    private Node toNode(String line) {
        String[] data = Try.of(() -> parser.parseLine(line)).get();
        return new Node(Long.parseLong(data[0]), Double.parseDouble(data[2]), Double.parseDouble(data[1]));
    }
}
