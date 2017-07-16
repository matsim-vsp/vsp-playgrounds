/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.agarwalamit.clustering;

import org.matsim.api.core.v01.Id;

/**
 * Created by amit on 16.07.17.
 */

public class Point {

    private final double x;
    private final double y;
    private final double weight;
    private Id<Cluster> clusterId;

    public Point(final double x, final double y, final double weight) {
        this.x = x;
        this.y = y;
        this.weight = weight;
    }

    public Point(final double x, final double y) {
        this.x = x;
        this.y = y;
        this.weight = 1;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getWeight() {
        return weight;
    }

    public void setCluster(final Id<Cluster> id) {
        this.clusterId = clusterId;
    }

    public Id<Cluster> getClusterId() {
        return clusterId;
    }

    @Override
    public String toString(){
        if (weight==1) {
            return "[x=" + this.x + ", y=" + this.y + "]";
        } else {
            return "[x=" + this.x + ", y=" + this.y + ", weight= "+this.weight+"]";
        }
    }
}
