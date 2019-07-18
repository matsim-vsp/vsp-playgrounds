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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;

/**
 * Created by amit on 21.02.18.
 */

public class DistanceBin implements Identifiable<DistanceBin>, Comparable<DistanceBin>{

    private final DistanceRange distanceRange;
    private double value = 0.;
    private final double stdDev;
    private final Id<DistanceBin> id;
    private final String mode;

    public DistanceBin(DistanceRange distanceRange) {
        this("no-mode", distanceRange, 10000, 0);
    }

    public DistanceBin(String mode, DistanceRange distanceRange, double standardDeviation, double value) {
        this.distanceRange = distanceRange;
        this.stdDev = standardDeviation;
        this.value = value;
        this.mode = mode;
        this.id = Id.create(mode + "_" + String.valueOf(distanceRange.lowerLimit) + "_" + String.valueOf(distanceRange.upperLimit), DistanceBin.class);
    }

    public DistanceRange getDistanceRange() {
        return distanceRange;
    }

    public double getValue() {
        return value;
    }

    @Override
    public Id<DistanceBin> getId() { return id; }

    public double getStandardDeviation() {
        return stdDev;
    }

    public String getMode() {
        return mode;
    }

    public synchronized void addToCount(double val){
        this.value += val;
    }

    @Override
    public String toString() {
        return "DistanceBin{" +
                "distanceRange=" + distanceRange +
                ", value=" + value +
                '}';
    }

    @Override
    public int compareTo(DistanceBin distanceBin){
        return distanceRange.compareTo(distanceBin.getDistanceRange());
    }

    public boolean equals(Object object) {
        if (object == this) return true;

        if (object instanceof DistanceBin) {
            DistanceBin otherBin = (DistanceBin)object;
            return this.id.equals(otherBin.id);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
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

        public boolean isWithinRange(double distance) {
            return lowerLimit <= distance && distance <= upperLimit;
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
