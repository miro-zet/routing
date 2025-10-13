package com.itera.routing.algos.alt;


import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
@Slf4j
@Getter
@Setter
@RequiredArgsConstructor
public class ALTData {

    private Map<Pair<Long, Long>, Double> distances = new HashMap<>();
    private Set<Long> landmarks = new HashSet<>();
    private boolean useAlt = true;

    @Value("${path.alt}")
    private final String fileName;

    @PostConstruct
    public void load() {

        DataInputStream dis = null;
        if (fileName == null || fileName.isEmpty()) {
            log.warn("no filename specified ALT loading is skipped!");
            return;
        }

        try {
            dis = new DataInputStream(new BufferedInputStream(new FileInputStream(fileName)));
            long altMemory = -1;
            while (true) {

                long altNodeId = dis.readLong();
                if (altNodeId == -1) {
                    break;
                }
                landmarks.add(altNodeId);

                if (altMemory != altNodeId) {
                    log.info("ALT node: {}", altNodeId);
                    altMemory = altNodeId;
                }

                long nodeId = dis.readLong();
                double distanceAltToNode = dis.readDouble();
                double distanceNodeToAlt = dis.readDouble();


                distances.put(Pair.of(altNodeId, nodeId), distanceAltToNode);
                distances.put(Pair.of(nodeId, altNodeId), distanceNodeToAlt);

            }

        } catch (Exception e) {
            log.warn("Cannot read ALT data. Reason: {}", e);
        } finally {
            if (dis != null) {
                try {
                    dis.close();
                } catch (IOException e) {
                    log.warn("Cannot close ALT reader. Reason: {}", e);
                }
            }
        }

    }

}
