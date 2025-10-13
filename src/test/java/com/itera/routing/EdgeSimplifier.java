package com.itera.routing;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

import java.io.FileWriter;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EdgeSimplifier {

    public static void main(String[] args) throws Exception {

        final Path edgesPath = Paths.get("/home/marian/Projects/routing/data/DC/edges.csv");
        final Path nodesPath = Paths.get("/home/marian/Projects/routing/data/DC/nodes.csv");
        final FileWriter edgeWriter = new FileWriter("/home/marian/Projects/routing/data/DC/edges-4.csv");
        final FileWriter nodeWriter = new FileWriter("/home/marian/Projects/routing/data/DC/nodes-4.csv");


        try (Reader edgeReader = Files.newBufferedReader(edgesPath); Reader nodeReader = Files.newBufferedReader(nodesPath)) {

            CSVParser parser = new CSVParserBuilder()
                    .withSeparator(',')
                    .withQuoteChar('"')
                    .build();

            CSVReader edgesReader = new CSVReaderBuilder(edgeReader)
                    .withSkipLines(0)
                    .withCSVParser(parser)
                    .build();

            CSVReader nodesReader = new CSVReaderBuilder(nodeReader)
                    .withSkipLines(0)
                    .withCSVParser(parser)
                    .build();


            String[] line;
            Set<String> types = new HashSet<>();
            Set<String> allowedTypes = Set.of("Motorway", "Primary", "Secondary", "Tertiary", "Residential");
            Map<Long, Node> allNodes = new HashMap<>();
            Set<Long> nodeIds = new HashSet<>();

            int i = 0;
            while ((line = nodesReader.readNext()) != null) {
                if (i == 0) {
                    i++;
                    continue;
                }
                Node node = new Node(Long.parseLong(line[0]), Double.parseDouble(line[1]), Double.parseDouble(line[2]));
                allNodes.put(node.id(), node);
            }

            i = 0;
            while ((line = edgesReader.readNext()) != null) {
                if (i == 0) {
                    i++;
                    continue;
                }
                String simpleLine = "%s;%s;%s;%s;%s\n".formatted(line[0], line[2], line[3], line[4], line[11]);
                if (allowedTypes.contains(line[6]) || allowedTypes.contains(line[7])) {
                    edgeWriter.write(simpleLine);
                    nodeIds.add(Long.parseLong(line[2]));
                    nodeIds.add(Long.parseLong(line[3]));
                }
                types.add(line[6]);
            }

            for (Long nodeId : nodeIds) {
                Node node = allNodes.get(nodeId);
                nodeWriter.write("%s;%s;%s\n".formatted(node.id(), node.lon(), node.lat()));
            }

            edgesReader.close();
            edgeWriter.flush();
            edgeWriter.close();
            nodeWriter.flush();
            nodeWriter.close();


            System.out.println(types);

        }

    }

    record Node(long id, double lon, double lat) {
    }
}
