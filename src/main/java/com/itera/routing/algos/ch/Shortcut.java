package com.itera.routing.algos.ch;

import lombok.Data;

@Data
public class Shortcut {

    private final String id;
    private final String inEdgeId;
    private final String outEdgeId;
}
