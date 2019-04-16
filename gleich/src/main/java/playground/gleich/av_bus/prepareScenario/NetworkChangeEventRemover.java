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
package playground.gleich.av_bus.prepareScenario;

import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkChangeEventsParser;
import org.matsim.core.network.io.NetworkChangeEventsWriter;
import org.matsim.core.utils.geometry.geotools.MGC;

import playground.gleich.av_bus.FilePaths;
import playground.gleich.utilsFromOthers.jbischoff.JbUtils;

/**
 * @author  jbischoff
 * @author  gleich
 */
/**
 *
 */
public class NetworkChangeEventRemover {

	Network network;
	Geometry geo;
	private List<NetworkChangeEvent> networkChangeEvents = new ArrayList<>();
	private List<NetworkChangeEvent> newNetworkChangeEvents = new ArrayList<>();

	private final String NETWORKFILE = FilePaths.PATH_BASE_DIRECTORY + FilePaths.PATH_NETWORK_BERLIN_100PCT_ACCESS_LOOPS;
	private final String CHANGEFILE = FilePaths.PATH_BASE_DIRECTORY + FilePaths.PATH_NETWORK_CHANGE_EVENTS_BERLIN_100PCT;
	private final String CHANGEOUTFILE = FilePaths.PATH_BASE_DIRECTORY + FilePaths.PATH_NETWORK_CHANGE_EVENTS_BERLIN_100PCT_WITHOUT_STUDY_AREA;

	private final String SHAPEFILE = FilePaths.PATH_BASE_DIRECTORY + FilePaths.PATH_STUDY_AREA_SHP;
	private final String KEY = FilePaths.STUDY_AREA_SHP_KEY;
	private final String ELEMENT = FilePaths.STUDY_AREA_SHP_ELEMENT;

	public static void main(String[] args) {
		new NetworkChangeEventRemover().run();
	}

	/**
	 * 
	 */
	private void run() {
		network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(NETWORKFILE);
		geo = JbUtils.readShapeFileAndExtractGeometry(SHAPEFILE, KEY).get(ELEMENT);
		new NetworkChangeEventsParser(network, networkChangeEvents).readFile(CHANGEFILE);
		for (NetworkChangeEvent e : networkChangeEvents){
			for (Link l : e.getLinks()){
				Point fromCoord = MGC.coord2Point(l.getFromNode().getCoord());
				Point toCoord = MGC.coord2Point(l.getToNode().getCoord());
				if (!(geo.contains(fromCoord) || geo.contains(toCoord))){
					newNetworkChangeEvents.add(e);
				}

			}
		}
		new NetworkChangeEventsWriter().write(CHANGEOUTFILE, newNetworkChangeEvents);
	}
}
