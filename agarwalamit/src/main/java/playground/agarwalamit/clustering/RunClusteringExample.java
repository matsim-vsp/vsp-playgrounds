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

/**
 * Created by amit on 15.07.17.
 */


public class RunClusteringExample {

    public static void main(String[] args) {

        // get bounding box
       BoundingBox boundingBox = new BoundingBox(0,0,100,100);

        // let's say, we have 100 points
        int numberOfPoints = 1000;

        List<Point> sites = new ArrayList<>(numberOfPoints);
        for (int i =0 ; i< numberOfPoints ; i++) {
            sites.add(ClusterUtils.getRandomPoint(boundingBox));
        }

        int numberOfCluster = 10;

        ClusterAlgo clusterAlgo = new ClusterAlgo(numberOfCluster, boundingBox);
        clusterAlgo.process(sites);

        clusterAlgo.plotClusters();
    }
}
