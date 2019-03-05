/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.vsp.cadyts.marginals.prep;

/**
 * Created by amit on 21.02.18.
 */

public class DistanceBin implements Comparable<DistanceBin>{

    private final DistanceRange distanceRange;
    private double count = 0.;
    private final double stdDev;

    public DistanceBin(DistanceRange distanceRange) {
        this(distanceRange, 10000);
    }

    public DistanceBin(DistanceRange distanceRange, double standardDeviation) {
        this.distanceRange = distanceRange;
        this.stdDev = standardDeviation;
    }

    public DistanceRange getDistanceRange() {
        return distanceRange;
    }

    public double getCount() {
        return count;
    }

    public double getStandardDeviation() {
        return stdDev;
    }

    public void addToCount(double val){
        this.count += val;
    }

    @Override
    public String toString() {
        return "DistanceBin{" +
                "distanceRange=" + distanceRange +
                ", count=" + count +
                '}';
    }

    @Override
    public int compareTo(DistanceBin distanceBin){
        return distanceRange.compareTo(distanceBin.getDistanceRange());
    }

    public static class DistanceRange implements Comparable<DistanceRange>{
        private final double lowerLimit;
        private final double upperLimit; // allow infinity for upperLimit value

        public DistanceRange(double low, double high) {
            this.lowerLimit = low;
            this.upperLimit = high;
        }

        public double getLowerLimit() {
            return lowerLimit;
        }

        public double getUpperLimit() {
            return upperLimit;
        }

        @Override
        public String toString() {
            return "DistanceRange[" +
                    "lowerLimit=" + lowerLimit +
                    ", upperLimit=" + upperLimit +
                    ']';
        }

        @Override
        public int compareTo(DistanceRange range){
            if (this.getLowerLimit()==range.getLowerLimit()){
                return Double.compare(this.getUpperLimit(), range.getUpperLimit());
            } else {
                return Double.compare(this.getLowerLimit(), range.getLowerLimit());
            }
        }
    }
}
