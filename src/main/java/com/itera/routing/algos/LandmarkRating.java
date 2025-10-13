package com.itera.routing.algos;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LandmarkRating {

    private final long landmarkId;
    private double rating;

    public void incRating(double ratingInc) {
        this.rating = rating + ratingInc;
    }

}
