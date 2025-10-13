package com.itera.routing;

import com.itera.routing.algos.ch.CHLoader;
import com.itera.routing.loaders.CsvGraphLoader;

public class CHLoaderTest {

    public static void main(String[] args) throws Exception {

        CsvGraphLoader loader = new CsvGraphLoader(
            "/Users/marian.rosko/Projects/home/routing/data/sk_nodes.csv",
            "/Users/marian.rosko/Projects/home/routing/data/sk_edges.csv"
        );

        CHLoader chLoader = new CHLoader(
            "/Users/marian.rosko/Projects/home/routing/data/shortcuts.dat",
            "/Users/marian.rosko/Projects/home/routing/data/hierarchy.dat",
            "/Users/marian.rosko/Projects/home/routing/data/definitions.dat",
            loader
        );

        chLoader.load();
    }
}
