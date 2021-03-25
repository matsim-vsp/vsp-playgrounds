/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2021 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package playground.michalm.dvrp;

import java.net.MalformedURLException;
import java.util.Set;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.zone.skims.DvrpTravelTimeMatrix;
import org.matsim.contrib.zone.skims.DvrpTravelTimeMatrixParams;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DvrpTravelTimeMatrixSpeedTest {

	//	public static void main(String[] args) throws MalformedURLException {
	//		Network network = NetworkUtils.createNetwork();
	//		MatsimNetworkReader networkReader = new MatsimNetworkReader(network);
	//		networkReader.readFile("d:/matsim-repos/shared-svn/projects/audi_av/scenario/networkc.xml.gz");
	//
	//		List<PreparedGeometry> areaGeoms = ShpGeometryUtils.loadPreparedGeometries(
	//				new URL("file:///d:/matsim-repos/shared-svn/projects/audi_av/shp/Prognoseraum.shp"));
	//		Network filteredNetwork = NetworkAreaFiltering.filterNetworkUsingShapefile(network, areaGeoms, true);
	//
	//		new NetworkWriter(filteredNetwork).write(
	//				"d:/matsim-repos/shared-svn/projects/audi_av/scenario/network_filtered.xml.gz");
	//
	//		//		DvrpTravelTimeMatrixParams params = new DvrpTravelTimeMatrixParams();
	//		//		params.setCellSize(200);
	//		//
	//		//		new DvrpTravelTimeMatrix(network, params, 12);
	//	}

	public static void main(String[] args) throws MalformedURLException {
		Network network = NetworkUtils.createNetwork();
		MatsimNetworkReader networkReader = new MatsimNetworkReader(network);
		//		networkReader.readFile("d:/matsim-repos/shared-svn/projects/audi_av/scenario/network_reduced.xml.gz");
		networkReader.readFile("C:\\Users\\michal\\Downloads\\berlin-v5.5-network.xml.gz");

		//		new NetworkCleaner().run(network);
		//
		//		new NetworkWriter(network).write(
		//				"d:/matsim-repos/shared-svn/projects/audi_av/scenario/network_reduced_cleaned.xml.gz");

		Network carNetwork = NetworkUtils.createNetwork();
		new TransportModeNetworkFilter(network).filter(carNetwork, Set.of("car"));

		DvrpTravelTimeMatrixParams params = new DvrpTravelTimeMatrixParams();
		params.setCellSize(200);

		new DvrpTravelTimeMatrix(carNetwork, params, 12);

	}
}
