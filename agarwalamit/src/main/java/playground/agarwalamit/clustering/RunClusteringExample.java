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
        int numberOfPoints = 100;

        List<Point> sites = new ArrayList<>(numberOfPoints);
        for (int i =0 ; i< numberOfPoints ; i++) {
            Point point = ClusterUtils.getRandomPoint(boundingBox);
//            point.setWeight(new Random().nextInt(5));
            sites.add(point);
        }

        int numberOfCluster = 10;

        ClusterAlgorithm clusterAlgo = new ClusterAlgorithm(numberOfCluster, boundingBox, ClusterAlgorithm.ClusterType.EQUAL_POINTS);
//        ClusterAlgo clusterAlgo = new ClusterAlgo(numberOfCluster, boundingBox, ClusterAlgo.ClusterType.K_MEANS);
        clusterAlgo.process(sites);

        clusterAlgo.plotClusters();
    }
}
