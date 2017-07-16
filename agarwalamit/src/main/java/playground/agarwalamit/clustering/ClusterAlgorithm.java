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

import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import org.apache.log4j.Logger;
import org.jfree.chart.ChartPanel;
import org.matsim.core.utils.charts.XYScatterChart;
import playground.agarwalamit.utils.NumberUtils;

/**
 *
 * If weight of all points is 1, it is called as K-Mean cluster algorithm
 *  @see <a href="https://home.deib.polimi.it/matteucc/Clustering/tutorial_html/kmeans.html">K-Means</a>.
 *
 * Created by amit on 15.07.17.
 */

public class ClusterAlgorithm {

    /**
     * Currently, two enums are implemented both are based on K-Means clustering algorithms.
     * However, EQUAL_POINTS offers same number of points in each cluster.
     */
    public enum ClusterType {
        /**
         * Clusters will be created such that number of points are same in each cluster.
         */
        EQUAL_POINTS,
        /**
         * Simply use of K-Means algorithm which means, cluster may have different number of points.
         */
        K_MEANS }

    private static final Logger LOGGER = Logger.getLogger(ClusterAlgorithm.class);

    private final int numberOfClusters;
    private final BoundingBox boundingBox;

    private final List<Cluster> clusters;
    private boolean terminate = false;

    private final ClusterType clusterType;

    public ClusterAlgorithm(final int numberOfClusters, final BoundingBox boundingBox, final ClusterType clusterType) {
        this.numberOfClusters = numberOfClusters;
        this.boundingBox = boundingBox;

        clusters = new ArrayList<>(numberOfClusters);
        this.clusterType = clusterType;
        LOGGER.info("Using clustering type "+ this.clusterType);
    }

    public void process(final List<Point> pointsForClustering) {
        LOGGER.info("Using K-Mean algorithm to cluster points.");
        for (int i =0; i< numberOfClusters ; i++) {
            Cluster cluster = new Cluster(i);
            Point centroid = ClusterUtils.getRandomPoint(boundingBox);
            cluster.setCentroid(centroid);
            this.clusters.add(cluster);
        }

        int iteration = 0;

        while ( ! terminate ) {
            LOGGER.info("Running "+String.valueOf(iteration++)+" iteration");
            clusters.stream().forEach(c -> c.clear());

            assignCluster(pointsForClustering);
            updateCentroids();
        }
    }

    public void plotClusters(){
        XYScatterChart scatterChart = new XYScatterChart("clusters", "x", "y");
        for (Cluster cluster : this.clusters) {
            double xs [] = new double [cluster.getPoints().size()];
            double ys [] = new double [cluster.getPoints().size()];
            for (int i =0; i < cluster.getPoints().size(); i++) {
                xs[i] = cluster.getPoints().get(i).getX();
                ys[i] = cluster.getPoints().get(i).getY();
            }
            scatterChart.addSeries("cluster id"+cluster.getId().toString(), xs, ys);
        }
        ChartPanel chartPanel = new ChartPanel(scatterChart.getChart(), false);
        chartPanel.setPreferredSize(new Dimension(600, 600));
        chartPanel.setVisible(true);

        JFrame jFrame = new JFrame();
        jFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        jFrame.add(chartPanel, BorderLayout.CENTER);
        jFrame.pack();
        jFrame.setVisible(true);
    }

    private void assignCluster(final List<Point> pointsForClustering) {

        if (pointsForClustering.size()%numberOfClusters != 0) {
            throw new RuntimeException("Number of points are "+ pointsForClustering.size() + " and number of required cluster are "+
            numberOfClusters + ". Don't know how to cluster these points equally in required clusters.");
        }
        int numberOfPointsPerCluster = pointsForClustering.size() / numberOfClusters ;

        double distance = 0.0;

        for(Point point : pointsForClustering) {
            SortedMap<Double, Integer> distanceToClusterIndexMap = new TreeMap<>();
            for(int clusterIndex = 0; clusterIndex < this.clusters.size() ; clusterIndex++ ) {
                Cluster cluster = this.clusters.get(clusterIndex);
                distance = ClusterUtils.euclideanDistance(cluster.getCentroid(), point);

                distanceToClusterIndexMap.put(distance, clusterIndex);
            }

            // assign cluster
            Iterator<Map.Entry<Double, Integer>> iterator = distanceToClusterIndexMap.entrySet().iterator();
            while ( iterator.hasNext() ) {
                Map.Entry<Double, Integer> entry = iterator.next();
                int clusterIndex = entry.getValue();
                Cluster cluster = clusters.get(clusterIndex);
                if (cluster.getPoints().size() == numberOfPointsPerCluster
                        && this.clusterType.equals(ClusterType.EQUAL_POINTS) ) {
                    continue;
                } else {
                    point.setCluster(cluster.getId());
                    cluster.addPoint(point);
                    break;
                }
            }
        }
    }

    // K-Means
//    private void assignClusterKMeans(final List<Point> pointsForClustering) {
//        double min = Double.MAX_VALUE;
//        int clusterIndex = 0;
//        double distance = 0.0;
//
//        for(Point point : pointsForClustering) {
//            min = Double.MAX_VALUE;
//            for(int index = 0; index < this.clusters.size() ; index++ ) {
//                Cluster cluster = this.clusters.get(index);
//                distance = ClusterUtils.euclideanDistance(cluster.getCentroid(), point);
//
//                if(distance < min){
//                    min = distance;
//                    clusterIndex = index;
//                }
//            }
//            Cluster cluster = clusters.get(clusterIndex);
//            point.setCluster(cluster.getId());
//            cluster.addPoint(point);
//        }
//    }

    private void updateCentroids() {
        for(Cluster cluster : clusters) {
            double sumX = 0;
            double sumY = 0;
            List<Point> list = cluster.getPoints();
            double sumWeight = list.stream().mapToDouble(site -> site.getWeight()).sum();

            // weighted sum
            for(Point point : list) {
                sumX += point.getWeight() * point.getX();
                sumY += point.getWeight() * point.getY();
            }

            Point oldCentroid = cluster.getCentroid();
            double newX = sumX / sumWeight;
            double newY = sumY / sumWeight;
            Point newCentroid = new Point(newX, newY, oldCentroid.getWeight());
            cluster.setCentroid(newCentroid);

            double distBetweenCentroids = ClusterUtils.euclideanDistance(oldCentroid, newCentroid);
            if (NumberUtils.round(distBetweenCentroids, 5)==0) {
                this.terminate = true;
            } else {
                this.terminate = false;
            }
        }
    }
}
