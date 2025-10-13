package com.itera.routing.topology;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashSet;
import java.util.Set;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Node {

    @EqualsAndHashCode.Include
    private final long id;
    private final double lon;
    private final double lat;
    private final Set<Edge> inEdges = new HashSet<>();
    private final Set<Edge> outEdges = new HashSet<>();

    public String toString() {
        return "%s".formatted(id);
    }

    public String toPositionString() {
        return String.format("(%s,%s)", lon, lat);
    }
}
