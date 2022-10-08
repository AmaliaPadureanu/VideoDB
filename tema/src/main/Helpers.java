package main;

import fileio.SerialInputData;

import java.util.*;
import java.util.stream.Collectors;

public class Helpers {

    static Comparator<Map.Entry<String, Integer>> ascComparator = new Comparator<>() {
        @Override public int compare(Map.Entry<String, Integer> e1, Map.Entry<String, Integer> e2) {
            String v1 = e1.getKey();
            String v2 = e2.getKey();

            if (e1.getValue() == e2.getValue()) {
                return v1.compareTo(v2);
            }

            return e1.getValue() - e2.getValue();
        }
    };

    static Comparator<Map.Entry<String, Double>> ascDoubleComparator = new Comparator<>() {
        @Override
        public int compare(Map.Entry<String, Double> e1, Map.Entry<String, Double> e2) {
            String v1 = e1.getKey();
            String v2 = e2.getKey();

            if (e1.getValue().equals(e2.getValue())) {
                return v1.compareTo(v2);
            }

            return e1.getValue().compareTo(e2.getValue());
        }
    };

    static double getRatingAverageForShow(Map<Integer, List<Double>> seasonRatings) {
        if (seasonRatings == null) { return 0; }

        List<Double> seasonAverage = new ArrayList<>();

        for (var season : seasonRatings.entrySet()) {
            if (season.getValue().size() > 0) {
                seasonAverage.add(season.getValue().stream().reduce(0.0, Double::sum) / seasonRatings.size());
            }
        }

        seasonAverage = seasonAverage.stream().filter( a -> a != 0).collect(Collectors.toList());

        return seasonAverage.stream().reduce(0.0, Double::sum) / seasonAverage.size();
    }

    static Map<String, Double> getMapKeyTitleValueAverageRatings(Map<String, Map<Integer, List<Double>>> moviesRatings) {
        Map<String, Double> averageRatings = new HashMap<>();

        for(var entry : moviesRatings.entrySet()) {
            Double average = getRatingAverageForShow(entry.getValue());
            averageRatings.put(entry.getKey(), average);
        }

        return averageRatings;
    }

}
