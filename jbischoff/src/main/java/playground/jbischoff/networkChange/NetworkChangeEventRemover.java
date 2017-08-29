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
package playground.jbischoff.networkChange;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkChangeEventsParser;
import org.matsim.core.network.io.NetworkChangeEventsWriter;
import org.matsim.core.utils.geometry.geotools.MGC;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

import playground.jbischoff.utils.JbUtils;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class NetworkChangeEventRemover {
	
	Network network;
	Geometry geo;
	private List<NetworkChangeEvent> networkChangeEvents;
	private List<NetworkChangeEvent> newNetworkChangeEvents = new ArrayList<>();

	private final String NETWORKFILE = "../../../shared-svn/projects/bmw_carsharing/data/network.xml.gz";
	private final String CHANGEFILE = "../../../shared-svn/projects/bmw_carsharing/data/changeEvents.xml.gz";
	private final String CHANGEOUTFILE = "../../../shared-svn/projects/bmw_carsharing/data/changeEvents.xml.gz";

	private final String SHAPEFILE = "../../../shared-svn/projects/bmw_carsharing/data/gis/untersuchungsraum.shp";

public static void main(String[] args) {
	new NetworkChangeEventRemover().run();
}

/**
 * 
 */
private void run() {
	network = NetworkUtils.createNetwork();
	new MatsimNetworkReader(network).readFile(NETWORKFILE);
	geo = JbUtils.readShapeFileAndExtractGeometry(SHAPEFILE, "ID").get(0);
	new NetworkChangeEventsParser(network, networkChangeEvents).readFile(CHANGEFILE);
	for (NetworkChangeEvent e : networkChangeEvents){
		for (Link l : e.getLinks()){
			Point p = MGC.coord2Point(l.getCoord());
			if (!geo.contains(p)){
				newNetworkChangeEvents.add(e);
			}
			
		}
	}
	new NetworkChangeEventsWriter().write(CHANGEOUTFILE, newNetworkChangeEvents);
}
}
