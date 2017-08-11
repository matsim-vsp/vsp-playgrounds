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

/**
 * 
 */
package playground.jbischoff.wobscenario.run;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.analysis.zonal.GridUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.utils.gis.PolygonFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class TestGrid {
public static void main(String[] args) {
	
	Network network = NetworkUtils.createNetwork();
	new MatsimNetworkReader(network).readFile("C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/projekt2/input/network/networkpt-av-jun17.xml.gz");
	Map<String,Geometry> grid = GridUtils.createGridFromNetwork(network, 2000);
	
  	String gk4 = "PROJCS[\"DHDN / 3-degree Gauss-Kruger zone 4\", GEOGCS[\"DHDN\", DATUM[\"Deutsches Hauptdreiecksnetz\", SPHEROID[\"Bessel 1841\", 6377397.155, 299.1528128, AUTHORITY[\"EPSG\",\"7004\"]], TOWGS84[612.4, 77.0, 440.2, -0.054, 0.057, -2.797, 2.55], AUTHORITY[\"EPSG\",\"6314\"]], PRIMEM[\"Greenwich\", 0.0, AUTHORITY[\"EPSG\",\"8901\"]], UNIT[\"degree\", 0.017453292519943295], AXIS[\"Geodetic longitude\", EAST], AXIS[\"Geodetic latitude\", NORTH], AUTHORITY[\"EPSG\",\"4314\"]], PROJECTION[\"Transverse_Mercator\", AUTHORITY[\"EPSG\",\"9807\"]], PARAMETER[\"central_meridian\", 12.0], PARAMETER[\"latitude_of_origin\", 0.0], PARAMETER[\"scale_factor\", 1.0], PARAMETER[\"false_easting\", 4500000.0], PARAMETER[\"false_northing\", 0.0], UNIT[\"m\", 1.0], AXIS[\"Easting\", EAST], AXIS[\"Northing\", NORTH], AUTHORITY[\"EPSG\",\"31468\"]]";
  	//some weird problems with that, when running on cluster
  	CoordinateReferenceSystem crs;
	try {
		crs = CRS.parseWKT(gk4);
		
		PolygonFeatureFactory factory = new PolygonFeatureFactory.Builder().addAttribute("ID", String.class)
				.setCrs(crs).create();
		
		List<SimpleFeature> features = new ArrayList<>();
		
		
		for (Entry<String,Geometry> z :grid.entrySet()) {
			Object[] attribs = new Object[1];
			
			features.add(factory.createPolygon(z.getValue().getCoordinates(), attribs, z.getKey()));
		}
		
		ShapeFileWriter.writeGeometries(features, "C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/projekt2/input/network/netshp.shp");
}  catch (FactoryException e) {
	// TODO Auto-generated catch block
	e.printStackTrace();
}
	
	}}

