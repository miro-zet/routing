package com.itera.routing.utils;

import com.itera.routing.topology.Node;

public class DistanceUtils {

    public static final long EARTH_R = 6371 * 1000;

    public static double getSphericalDistance(Node n1, Node n2) {

        double lat1 = n1.getLat() / 180 * Math.PI;
        double lat2 = n2.getLat() / 180 * Math.PI;

        double dlng = (n1.getLon() - n2.getLon()) / 180 * Math.PI;

        if (!(lat1 == lat2 && dlng == 0)) {
            double distance = Math.acos(Math.sin(lat1) * Math.sin(lat2) + Math.cos(lat1) * Math.cos(lat2) * Math.cos(dlng)) * EARTH_R;
            return distance;
        } else {
            return 0;
        }
    }


    public static double getApprxSphericalDistance(Node n1, Node n2) {

        double lat1 = n1.getLat() / 180 * Math.PI;
        double lat2 = n2.getLat() / 180 * Math.PI;
        double dlng = (n1.getLon() - n2.getLon()) / 180 * Math.PI;

        double x = dlng * Math.cos((lat1 + lat2) / 2);
        double y = lat2 - lat1;

        double distance = Math.sqrt(x * x + y * y) * EARTH_R;
        return distance;

    }

    public static double getFlatDistance(Node n1, Node n2) {
        return Math.sqrt(Math.pow(n1.getLat() - n2.getLat(), 2) + Math.pow(n1.getLon() - n2.getLon(), 2));
    }
}
