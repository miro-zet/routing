package com.itera.routing.algos.ch;

import com.itera.routing.topology.Edge;
import com.itera.routing.topology.Graph;
import com.itera.routing.topology.Node;
import lombok.Data;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class CHData {

    private final Graph graph;
    private final Map<Node, Integer> hierarchies;
    private final Map<Edge, Pair<Edge, Edge>> shortcuts;

    public List<Edge> getOriginalEdges(List<Edge> shortcuts) {

        List<Edge> result = new ArrayList<>();
        for (Edge shortcut : shortcuts) {
            List<Edge> path = decode(shortcut);
            result.addAll(path);
        }

        return result;
    }

    List<Edge> decode(Edge edge) {

        Pair<Edge, Edge> definitions = shortcuts.get(edge);
        if (definitions == null) {
            return List.of(edge);
        }

        List<Edge> result = new ArrayList<>();
        result.addAll(decode(definitions.getLeft()));
        result.addAll(decode(definitions.getRight()));

        return result;

    }
}
