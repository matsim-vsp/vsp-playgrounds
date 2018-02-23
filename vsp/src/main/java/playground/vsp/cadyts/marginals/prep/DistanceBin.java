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

public class DistanceBin {

    private final DistanceRange distanceRange;
    private double count = 0.;

    public DistanceBin(DistanceRange distanceRange) {
        this.distanceRange = distanceRange;
    }

    public DistanceRange getDistanceRange() {
        return distanceRange;
    }

    public double getCount() {
        return count;
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

    public static class DistanceRange {
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
    }
}
