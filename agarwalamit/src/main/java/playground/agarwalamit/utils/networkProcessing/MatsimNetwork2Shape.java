/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.utils.networkProcessing;

import java.util.ArrayList;
import java.util.Collection;
import com.vividsolutions.jts.geom.Coordinate;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * @author amit
 */
public class MatsimNetwork2Shape {

	private static String matsimNetwork = "/Users/aagarwal/Desktop/ils/agarwal/siouxFalls/output/run0/output_network.xml.gz";
	private static String outputShapeDir = "./clusterOutput/networkShape/";
	private static String epsg = "EPSG:3459";

	public static void main(String[] args) {

		if(args.length>0) {
			matsimNetwork = args[0];
			outputShapeDir = args[1];
			epsg = args[2];
		}

		Network network = LoadMyScenarios.loadScenarioFromNetwork(matsimNetwork).getNetwork();
        
        CoordinateReferenceSystem crs = MGC.getCRS(epsg);//i have tried 2842 3659,2455  32035 and  32135
        Collection<SimpleFeature> features = new ArrayList<>();
        PolylineFeatureFactory linkFactory = new PolylineFeatureFactory.Builder().setCrs(crs).
        		setName("link").
                addAttribute("ID", String.class).
                addAttribute("fromID", String.class).
                addAttribute("toID", String.class).
                addAttribute("length", Double.class).
                addAttribute("type", String.class).
                addAttribute("capacity", Double.class).
                addAttribute("freespeed", Double.class).
                create();
        for(Link link :network.getLinks().values()){
        	Coordinate fromNodeCoordinate = new Coordinate(link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY());
        	Coordinate toNodeCoordinate = new Coordinate(link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY());
        	Coordinate linkCoordinate = new Coordinate(link.getCoord().getX(), link.getCoord().getY());
        	SimpleFeature ft = linkFactory.createPolyline(new Coordinate [] {fromNodeCoordinate, linkCoordinate, toNodeCoordinate},
					new Object [] {link.getId().toString(), link.getFromNode().getId().toString(),link.getToNode().getId().toString(), link.getLength(), NetworkUtils.getType(
                            link), link.getCapacity(), link.getFreespeed()}, null);
			features.add(ft);
        }
        ShapeFileWriter.writeGeometries(features, outputShapeDir+"network_links.shp");
        features = new ArrayList<>();
		PointFeatureFactory nodeFactory = new PointFeatureFactory.Builder().
				setCrs(crs).
				setName("nodes").
				addAttribute("ID", String.class).
				create();

		for (Node node : network.getNodes().values()) {
			SimpleFeature ft = nodeFactory.createPoint(node.getCoord(), new Object[] {node.getId().toString()}, null);
			features.add(ft);
		}
		ShapeFileWriter.writeGeometries(features, outputShapeDir+"network_nodes.shp");
	}
}