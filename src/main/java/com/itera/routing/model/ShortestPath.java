package com.itera.routing.model;

import com.itera.routing.topology.Edge;
import com.itera.routing.topology.Node;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
public class ShortestPath {

    private double length;
    private List<String> edges;
    private List<Long> nodes;

    public ShortestPath add(ShortestPath other) {
        ShortestPath sum = new ShortestPath();
        sum.setLength(this.getLength() + other.getLength());

        sum.setEdges(new ArrayList<>(edges));
        sum.getEdges().addAll(other.getEdges());

        sum.setNodes(new ArrayList<>(nodes));
        sum.getNodes().addAll(other.getNodes());

        return sum;
    }

    public ShortestPath add(Edge edge) {
        ShortestPath sum = new ShortestPath();
        sum.setLength(this.getLength() + edge.getWeight());

        sum.setEdges(new ArrayList<>(edges));
        sum.getEdges().add(edge.getId());

        sum.setNodes(new ArrayList<>(nodes));
        return sum;
    }
}
