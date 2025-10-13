package com.itera.routing.algos.alt;


import com.itera.routing.algos.LandmarkRating;
import com.itera.routing.topology.Node;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class ALTHeuristics {

    private static final int STEPS = 10;
    private static final int SUBSET_SIZE = 4;
    private final Node target;
    private final Map<Pair<Long, Long>, Double> data;
    private Map<Long, Integer> usedLandmarks = new HashMap<>();
    private Map<Long, LandmarkRating> landmarkRatings = new HashMap<>();
    private Set<Long> landmarkIds;
    private int calibratingStep;

    public ALTHeuristics(Node target, ALTData data) {
        this.target = target;
        this.data = data.getDistances();
        this.landmarkIds = data.getLandmarks();
        init();
    }

    public void init() {
        landmarkRatings.clear();
        calibratingStep = 0;
    }

    public double heuristics(Node node) {
        return getLandmarkDistance(node, target);
    }

    private double getLandmarkDistance(Node from, Node to) {

        double max = 0;
        long usedLandmark = -1;

        for (long landmarkId : landmarkIds) {

            double candidateFNL = getDistanceFromNodeToLandmark(
                from.getId(),
                landmarkId
            ) - getDistanceFromNodeToLandmark(to.getId(), landmarkId);
            double candidateFLN = getDistanceFromLandmarkToNode(landmarkId, to.getId()) - getDistanceFromLandmarkToNode(
                landmarkId,
                from.getId()
            );
            double candidate = Math.max(candidateFLN, candidateFNL);

            if (candidate < 0) {
                //candidate = -1 * candidate;
            }
            if (candidate > max) {
                max = candidate;
                usedLandmark = landmarkId;
            }

            if (calibratingStep < STEPS) {
                parse(landmarkId, candidate);
            }

        }

        Integer landmarkCount = usedLandmarks.get(usedLandmark);
        if (landmarkCount == null) {
            usedLandmarks.put(usedLandmark, 1);
        } else {
            usedLandmarks.put(usedLandmark, landmarkCount + 1);
        }

        calibratingStep++;
        if (calibratingStep == STEPS) {
            //evaluateLandmarks();
        }

        return max;

    }

    private double getDistanceFromLandmarkToNode(long landmark, long node) {
        Double dist = data.get(Pair.of(landmark, node));
        return dist!=null?dist:Double.MAX_VALUE;
    }

    private double getDistanceFromNodeToLandmark(long node, long landmark) {
        Double dist = data.get(Pair.of(node, landmark));
        return dist!=null?dist:Double.MAX_VALUE;
    }

    private void evaluateLandmarks() {

        List<LandmarkRating> ratings = new ArrayList<>(landmarkRatings.values());
        Collections.sort(ratings, (LandmarkRating o1, LandmarkRating o2) -> (int) (o2.getRating() - o1.getRating()));

        ratings.stream().forEach((rating) -> {
            //log.info("landmark: {}, rating: {}", rating.getLandmarkId(), rating.getRating());
        });

        landmarkIds.clear();
        for (int i = 0; i < SUBSET_SIZE; i++) {
            landmarkIds.add(ratings.get(i).getLandmarkId());
        }

    }

    private void parse(long landmarkId, double candidate) {
        LandmarkRating rating = landmarkRatings.get(landmarkId);
        if (rating == null) {
            landmarkRatings.put(landmarkId, new LandmarkRating(landmarkId, candidate));
        } else {
            rating.incRating(candidate);
        }

    }

}
