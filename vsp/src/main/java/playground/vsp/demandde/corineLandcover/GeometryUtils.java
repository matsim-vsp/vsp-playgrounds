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

package playground.vsp.demandde.corineLandcover;

import java.util.List;
import java.util.Random;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygonal;
import com.vividsolutions.jts.shape.random.RandomPointsBuilder;
import com.vividsolutions.jts.simplify.TopologyPreservingSimplifier;
import org.matsim.core.gbl.MatsimRandom;

/**
 * Created by amit on 10.10.17.
 */

public class GeometryUtils {

    private GeometryUtils(){}
    private static final Random RAND = MatsimRandom.getRandom();
    private static final GeometryFactory geometryFactory = new GeometryFactory();

    /**
     * @param geom
     * @return a simplified geometry by increasing tolerance until number of vertices are less than 1000.
     */
    public static Geometry getSimplifiedGeom(final Geometry geom){
        Geometry outGeom = geom;
        double distanceTolerance = 1;
        int numberOfVertices = getNumberOfVertices(geom);
        while (numberOfVertices > 1000){
            outGeom = getSimplifiedGeom(outGeom, distanceTolerance);
            numberOfVertices = getNumberOfVertices(outGeom);
            distanceTolerance *= 10;
        }
        return outGeom;
    }

    public static int getNumberOfVertices(final Geometry geom){
        return geom.getNumPoints();
    }

    /**
     * simplify the geometry based on given tolerance
     */
    public static Geometry getSimplifiedGeom(final Geometry geom, final double distanceTolerance){
        return TopologyPreservingSimplifier.simplify(geom, distanceTolerance);
    }

    /**
     * It perform "union" for each geometry and return one geometry.
     */
    public static Geometry combine(final List<Geometry> geoms){
        Geometry geom = null;
        for(Geometry g : geoms){
            if(geom==null) geom = g;
            else {
                geom = geom.union(g);
            }
        }
        return geom;
    }

    /**
     * @return a random point which is covered by all the geometries
     */
    public static Point getPointInteriorToGeometries(final Geometry landuseGeom, final Geometry zoneGeom) {
        if (landuseGeom.isEmpty() || zoneGeom.isEmpty() ) throw new RuntimeException("No geometries.");

        if (landuseGeom.intersection(zoneGeom).getArea()==0) {
            throw new RuntimeException("There is no common area for the given geoms.");
        }

        Point commonPoint = null;
        do {
            //assuming that zoneGeom is a subset of landuseGeom, it would be better to first find a point in a subset and then look if it's inside landuseGeom
            Coordinate coordinate = getRandomInteriorPoints(zoneGeom,1)[0];
            commonPoint = geometryFactory.createPoint(coordinate);
            if (landuseGeom.contains(commonPoint)) return commonPoint;
        } while(true);
    }

    /**
     * Return a random Coordinate in the geometry or null if
     * ({@link Geometry#isEmpty()} || !(g instanceof {@linkPolygonal})).
     *
     * @param g
     * @return
     */
    public static final Coordinate[] getRandomInteriorPoints(Geometry g, int numPoints){
        if(!(g instanceof Polygonal) || g.isEmpty()) throw new RuntimeException("Given geometry is not an instance of polygon or is empty.");

        RandomPointsBuilder rnd = new RandomPointsBuilder(geometryFactory);
        rnd.setNumPoints(numPoints);
        rnd.setExtent(g);
        return rnd.getGeometry().getCoordinates();
    }
}
