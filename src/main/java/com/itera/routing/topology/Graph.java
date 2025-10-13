package com.itera.routing.topology;

import lombok.Getter;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
public class Graph {

    final Map<Long, Node> nodes = new HashMap<>();
    final Map<String, Edge> edges = new HashMap<>();

    public Node getNode(Long id) {
        return nodes.get(id);
    }

    public Node getRandomNode() {
        return nodes.values().stream().findAny().get();
    }

    public double lonMin(){
        return min(n -> n.getLon());
    }

    public double lonMax(){
       return max(n -> n.getLon());
    }

    public double latMin(){
       return min(n -> n.getLat());
    }

    public double latMax(){
        return max(n -> n.getLat());
    }

    private double min(Function<Node, Double> f){
        double min = Double.MAX_VALUE;
        for(Node n : nodes.values()){
            if(f.apply(n) < min){
                min = f.apply(n);
            }
        }
        return min;
    }

    private double max(Function<Node, Double> f){
        double max = 0.0;
        for(Node n : nodes.values()){
            if(f.apply(n) > max){
                max = f.apply(n);
            }
        }
        return max;
    }


}
