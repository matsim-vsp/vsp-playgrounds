/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.jbischoff.av.accessibility.analysis;/*
 * created by jbischoff, 17.05.2018
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PolygonFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.jbischoff.utils.JbUtils;

public class CreateBerlinGrid {

    public static void main(String[] args) {
        Network network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile("D:/runs-svn/avsim/av_accessibility/input/berlin_only_net.xml.gz");
        Map<String, Geometry> geometryMap = JbUtils.readShapeFileAndExtractGeometry("C:/Users/Joschka/Documents/shared-svn/projects/accessibility_berlin/av/grid/squares.shp", "NO");
//        Map<String, Geometry> geometryMap = DrtGridUtils.createGridFromNetwork(network, 500);
        for (Link l : network.getLinks().values()) {

            Point p = MGC.coord2Point(l.getCoord());
            for (Map.Entry<String, Geometry> entry : geometryMap.entrySet()) {
                if (entry.getValue().contains(p)) {
                    l.getAttributes().putAttribute("zoneId", entry.getKey());
                    break;
                }
            }

        }

        new NetworkWriter(network).write("D:\\runs-svn\\avsim\\av_accessibility\\input\\berlin_only_net_dominik.xml.gz");
//        writeShape("d:/b5_22/gridzones_500.shp", geometryMap);


    }

    private static void writeShape(String outfile, Map<String, Geometry> currentZones) {

        String gk4 = "PROJCS[\"DHDN / 3-degree Gauss-Kruger zone 4\", GEOGCS[\"DHDN\", DATUM[\"Deutsches Hauptdreiecksnetz\", SPHEROID[\"Bessel 1841\", 6377397.155, 299.1528128, AUTHORITY[\"EPSG\",\"7004\"]], TOWGS84[612.4, 77.0, 440.2, -0.054, 0.057, -2.797, 2.55], AUTHORITY[\"EPSG\",\"6314\"]], PRIMEM[\"Greenwich\", 0.0, AUTHORITY[\"EPSG\",\"8901\"]], UNIT[\"degree\", 0.017453292519943295], AXIS[\"Geodetic longitude\", EAST], AXIS[\"Geodetic latitude\", NORTH], AUTHORITY[\"EPSG\",\"4314\"]], PROJECTION[\"Transverse_Mercator\", AUTHORITY[\"EPSG\",\"9807\"]], PARAMETER[\"central_meridian\", 12.0], PARAMETER[\"latitude_of_origin\", 0.0], PARAMETER[\"scale_factor\", 1.0], PARAMETER[\"false_easting\", 4500000.0], PARAMETER[\"false_northing\", 0.0], UNIT[\"m\", 1.0], AXIS[\"Easting\", EAST], AXIS[\"Northing\", NORTH], AUTHORITY[\"EPSG\",\"31468\"]]";
        //some weird problems with that, when running on cluster
        CoordinateReferenceSystem crs;
        try {
            crs = CRS.parseWKT(gk4);

            PolygonFeatureFactory factory = new PolygonFeatureFactory.Builder().addAttribute("ID", String.class)
                    .setCrs(crs).setName("zone").create();

            List<SimpleFeature> features = new ArrayList<>();
            for (Map.Entry<String, Geometry> z : currentZones.entrySet()) {
                Object[] attribs = new Object[1];
                attribs[0] = z.getKey();
                features.add(factory.createPolygon(z.getValue().getCoordinates(), attribs, z.getKey()));
            }

            ShapeFileWriter.writeGeometries(features, outfile);
        } catch (FactoryException e) {
            e.printStackTrace();
        }
    }

}
