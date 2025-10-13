package com.itera.routing.api;

import com.itera.routing.algos.Dijkstra;
import com.itera.routing.model.ShortestPath;
import com.itera.routing.services.RoutingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("route/find")
@RequiredArgsConstructor
public class RoutingController {

    private final RoutingService routingService;

    @GetMapping(value = "/dijkstra/fwd")
    public ShortestPath findDijkstra(@RequestParam("from") long from, @RequestParam("to") long to) {
        return routingService.findByDijkstra(from, to, Dijkstra.Direction.FORWARD);
    }

    @GetMapping(value = "/dijkstra/bckg")
    public ShortestPath findDijkstraBackward(@RequestParam("from") long from, @RequestParam("to") long to) {
        return routingService.findByDijkstra(from, to, Dijkstra.Direction.BACKWARD);
    }

    @GetMapping(value = "/dijkstra/bidir")
    public ShortestPath findDijkstraBidirectional(@RequestParam("from") long from, @RequestParam("to") long to) {
        return routingService.findByBidirectionalDijkstra(from, to);
    }

    @GetMapping(value = "/dijkstra/ch")
    public ShortestPath findDijkstraCH(@RequestParam("from") long from, @RequestParam("to") long to) {
        return routingService.findByCHDijkstra(from, to);
    }

    @GetMapping(value = "/astar")
    public ShortestPath findAstar(@RequestParam("from") long from, @RequestParam("to") long to) {
        return routingService.findByAstar(from, to);
    }

    @GetMapping(value = "/alt")
    public ShortestPath findAlt(@RequestParam("from") long from, @RequestParam("to") long to) {
        return routingService.findByAlt(from, to);
    }

}
