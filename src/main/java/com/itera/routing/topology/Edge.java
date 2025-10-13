package com.itera.routing.topology;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Edge {

    @EqualsAndHashCode.Include
    private final String id;
    private final Node from;
    private final Node to;
    private final double weight;
    private final int maxSpeed;

    public Node getOtherNode(Node node) {
        if (node != null) {
            if (node.equals(from)) {
                return to;
            }
            if (node.equals(to)) {
                return from;
            }
        }
        return null;
    }

    public String toString() {
        return "%s (%s -> %s)".formatted(id, from.getId(), to.getId());
    }
}
