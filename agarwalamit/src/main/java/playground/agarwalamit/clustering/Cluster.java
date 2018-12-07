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

import java.util.ArrayList;
import java.util.List;
import org.matsim.api.core.v01.Id;

/**
 * Created by amit on 15.07.17.
 */

public class Cluster {

    public List<Point> points;
    public Point centroid;
    public Id<Cluster> id;

    public Cluster(final int id) {
        this.id = Id.create(id,Cluster.class);
        this.points = new ArrayList<>();
        this.centroid = null;
    }

    public List<Point> getPoints() {
        return points;
    }

    public void addPoint(Point point) {
        points.add(point);
    }

    public Point getCentroid() {
        return centroid;
    }

    public void setCentroid(Point centroid) {
        this.centroid = centroid;
    }

    public Id<Cluster> getId() {
        return id;
    }

    public void clear() {
        points.clear();
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[Cluster: ").append(id.toString()).append("]\n");
        stringBuilder.append("[Centroid: ").append(centroid).append("]\n");
        stringBuilder.append("[Points: \n");
        for(Point p : points) {
            stringBuilder.append(p.toString()).append("\t");
        }
        stringBuilder.append("]");
        return stringBuilder.toString();
    }
}
