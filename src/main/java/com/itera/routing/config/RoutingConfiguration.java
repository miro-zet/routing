package com.itera.routing.config;

import com.itera.routing.algos.ch.CHData;
import com.itera.routing.algos.ch.CHLoader;
import com.itera.routing.loaders.GraphLoader;
import com.itera.routing.topology.Graph;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class RoutingConfiguration {

    @Bean
    public Graph graph(GraphLoader loader) {
        return loader.load();
    }

    @Bean
    public CHData chData(CHLoader loader) throws Exception {
        return loader.load();
    }
}
