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

package playground.agarwalamit.opdyts.patna.allModes;

import java.util.*;
import java.util.stream.Collectors;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.algorithms.CalcBoundingBox;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;
import playground.agarwalamit.analysis.spatial.GeneralGrid;
import playground.agarwalamit.clustering.BoundingBox;
import playground.agarwalamit.clustering.Cluster;
import playground.agarwalamit.clustering.ClusterAlgorithm;
import playground.agarwalamit.clustering.ClusterUtils;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;
import playground.agarwalamit.opdyts.teleportationModes.Zone;
import playground.agarwalamit.utils.FileUtils;
import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * Created by amit on 15.06.17.
 */

public final class PatnaZoneToLinkIdentifier {

    private static final Logger LOGGER = Logger.getLogger(PatnaZoneToLinkIdentifier.class);

    //BEGIN_EXAMPLE
    public static void main(String[] args) {
        String zoneFile = FileUtils.RUNS_SVN+"/opdyts/patna/input_allModes/Wards.shp";
        String networkFile = FileUtils.RUNS_SVN+"/opdyts/patna/input_allModes/network.xml.gz";
        new PatnaZoneToLinkIdentifier(LoadMyScenarios.loadScenarioFromNetwork(networkFile).getNetwork(), zoneFile);
    }
    //END_EXAMPLE

    private static final Set<Zone> zones = new LinkedHashSet<>();
    private final CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(PatnaUtils.EPSG, TransformationFactory.WGS84);


    /**
     *
     * This will create clusters depending on the departure locations.
     * @param population
     * @param boundingBox
     * @param numberOfClusters
     * @param clusterType
     */
    PatnaZoneToLinkIdentifier (final Population population, final BoundingBox boundingBox, final int numberOfClusters, final ClusterAlgorithm.ClusterType clusterType) {
        List<playground.agarwalamit.clustering.Point> listOfOrigins = new ArrayList<>();
        for (Person person : population.getPersons().values()) {
            List<PlanElement> planElementList = person.getSelectedPlan().getPlanElements();
            List<playground.agarwalamit.clustering.Point> list = planElementList.stream().filter(pe -> pe instanceof Activity).map(pe ->
            {
                Coord cord = ((Activity)pe).getCoord();
                return ClusterUtils.getPoint(cord);
            }
            ).collect(Collectors.toList());
            listOfOrigins.addAll(list);
        }

        ClusterAlgorithm clusterAlgorithm = new ClusterAlgorithm(numberOfClusters, boundingBox, clusterType);
        clusterAlgorithm.process(listOfOrigins);
        List<Cluster> clusters = clusterAlgorithm.getClusters();
        for (Cluster cluster : clusters) {
            Zone zone = new Zone(cluster.getId().toString());
            cluster.getPoints().stream().forEach(
                    p -> zone.addCoordsToZone(ClusterUtils.getCoord(p))
            );
            zones.add(zone);
        }
    }

    /**
     * First create the cells from the network and stores the origins in each zone.
     */
    PatnaZoneToLinkIdentifier (final Population population, final Network network, final double gridWidth) {
        // create polygon from bounding box
        CalcBoundingBox boundingBox = new CalcBoundingBox();
        boundingBox.run(network);
        GeometryFactory geometryFactory = new GeometryFactory();
        Geometry geometry = geometryFactory.createPolygon(new Coordinate[]{
                new Coordinate(boundingBox.getMinX(),boundingBox.getMinY()),
                new Coordinate(boundingBox.getMaxX(),boundingBox.getMinY()),
                new Coordinate(boundingBox.getMaxX(),boundingBox.getMaxY()),
                new Coordinate(boundingBox.getMinX(),boundingBox.getMaxY()),
                new Coordinate(boundingBox.getMinX(),boundingBox.getMinY()),
        });

        // create zones
        GeneralGrid generalGrid = new GeneralGrid(gridWidth, GeneralGrid.GridType.SQUARE);
        generalGrid.generateGrid(geometry);

        Collection<Point> points = generalGrid.getGrid().values();
        int index = 0;
        int numberOfOrigins = 0;
        for (Point point : points) {
            Zone zone = new Zone(String.valueOf(index++));

            for (Person p : population.getPersons().values()) {
                List<PlanElement> pes = p.getSelectedPlan().getPlanElements();
                for (PlanElement pe : pes ) {
                    if (pe instanceof Activity) {
                        Coord coord = ((Activity)pe).getCoord();
                        Point origin = MGC.xy2Point(coord.getX(), coord.getY());
                        if ( generalGrid.getCellGeometry(point).contains(origin) ) {
                            zone.addCoordsToZone(coord);
                        }
                    }
                }
            }

            if (zone.getCoordsInsideZone().isEmpty()) {
                LOGGER.warn("No coordinates found in the zone "+ zone.getZoneId());
            } else {
                numberOfOrigins += zone.getCoordsInsideZone().size();
                LOGGER.info(zone.getCoordsInsideZone().size() + " coords are inside the zone "+ zone.getZoneId());
                zones.add(zone);
            }
        }
        LOGGER.info("Total stored coordinates are "+ numberOfOrigins);
    }

    /**
     * Stores the coordinates of origins in each zone of the provided zone file.
     */
    PatnaZoneToLinkIdentifier (final Population population, final String zoneFile) {
        ShapeFileReader reader = new ShapeFileReader();
        Collection<SimpleFeature> features = reader.readFileAndInitialize(zoneFile);

        Iterator<SimpleFeature> iterator = features.iterator();
        while (iterator.hasNext()){
            SimpleFeature feature = iterator.next();
            int id = (Integer) feature.getAttribute("ID1");
            Zone zone = new Zone(String.valueOf(id));

            for (Person p : population.getPersons().values()) {
                List<PlanElement> pes = p.getSelectedPlan().getPlanElements();
                for (PlanElement pe : pes ) {
                    if (pe instanceof Activity) {
                        Coord coord = coordinateTransformation.transform( ((Activity)pe).getCoord() );
                        Point point = MGC.xy2Point(coord.getX(), coord.getY());
                        if ( ((Geometry) feature.getDefaultGeometry()).contains(point)) {
                            zone.addCoordsToZone(coord);
                        }
                    }
                }
            }

            if (zone.getCoordsInsideZone().isEmpty()) {
                LOGGER.warn("No coordinates found in the zone "+ zone.getZoneId());
            } else {
                LOGGER.info(zone.getCoordsInsideZone().size() + " coords are inside the zone "+ zone.getZoneId());
                zones.add(zone);
            }
        }
    }

    /**
     * Stores the link ids of network in each zone of the provided zone file. I think, this should not be used because using a link to identify the zone will be erroneous if link is longer.
     */
    @Deprecated
    PatnaZoneToLinkIdentifier (final Network network, final String zoneFile) {
        ShapeFileReader reader = new ShapeFileReader();
        Collection<SimpleFeature> features = reader.readFileAndInitialize(zoneFile);

        Iterator<SimpleFeature> iterator = features.iterator();
        while (iterator.hasNext()){
            SimpleFeature feature = iterator.next();
            int id = (Integer) feature.getAttribute("ID1");
            Zone zone = new Zone(String.valueOf(id));

            for (Link l : network.getLinks().values()) {
                Coord coord = coordinateTransformation.transform(l.getCoord());
                Point point = MGC.xy2Point(coord.getX(), coord.getY());
                if ( ((Geometry) feature.getDefaultGeometry()).contains(point)) {
                    zone.addLinksToZone(l.getId());
                }
            }

            if (zone.getLinksInsideZone().isEmpty()) {
                LOGGER.warn("No link found in the zone "+ zone.getZoneId());
            } else {
                LOGGER.info(zone.getLinksInsideZone().size() + " links are inside the zone "+ zone.getZoneId());
                zones.add(zone);
            }
        }
    }

    public Set<Zone> getZones(){
        return this.zones;
    }
}
