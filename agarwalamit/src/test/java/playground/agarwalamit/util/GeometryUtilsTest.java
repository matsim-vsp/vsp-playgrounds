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

package playground.agarwalamit.util;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by amit on 02.08.17.
 */

public class GeometryUtilsTest {

    @Test
    public void testRandomPointInsideGeometries(){
        GeometryFactory geometryFactory = new GeometryFactory();

        // create one big geom from different geoms

        Geometry geometry1 = geometryFactory.createPolygon(new Coordinate[] {
                new Coordinate(0,0),
                new Coordinate(50,0),
                new Coordinate(50,-50),
                new Coordinate(0,-50),
                new Coordinate(0,0) } );
        Geometry geometry2 = geometryFactory.createPolygon(new Coordinate[] {
                new Coordinate(0,10),
                new Coordinate(0,40),
                new Coordinate(-50,40),
                new Coordinate(-50,10),
                new Coordinate(0,10) } );

        Geometry combinedGeom = geometry1.union(geometry2);

        // first check if any point outside geometry1 and geometry2 lies inside combined geom
        {
            Point point = geometryFactory.createPoint(new Coordinate(0,1));
            Assert.assertFalse("The point must be outside combined geom.", combinedGeom.contains(point));
        }

        // another instersecting geom
        Geometry intersectingGeom = geometryFactory.createPolygon(new Coordinate[] {
                new Coordinate(20,-5),
                new Coordinate(20,5),
                new Coordinate(-20,5),
                new Coordinate(-20,-5),
                new Coordinate(20,-5) } );

        Point point = playground.vsp.demandde.corineLandcover.GeometryUtils.getPointInteriorToGeometries(combinedGeom, intersectingGeom);
        System.out.println(point.toString());

        // this point must be within (geometry 1 OR geometry2) AND withing intersecting geom
        boolean wrongPoint = false;

        if ( ! geometry1.covers(point) && ! geometry2.covers(point) ) {
            wrongPoint = true; // not in any of the geoms because geom1 and geom2 are ONE combined geom
        }
        Assert.assertFalse("Point is covered by any of the two seperate geometries.", wrongPoint);

        if (! intersectingGeom.covers(point)) {
            wrongPoint = true;
        }

        Assert.assertFalse("Point is not covered by intersecting geometry.", wrongPoint);
    }
}
