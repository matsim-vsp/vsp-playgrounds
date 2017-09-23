/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.dziemke.other;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;

/**
 * @author dziemke
 */
public class CreateNetwork {
	final private static Logger LOG = Logger.getLogger(CreateNetwork.class);

	public static void main(String[] args) {
		// Input and output		
		String osmFile = "../../nemo/data/input/counts/verkehrszaehlung_2015/network/allWaysNRW.osm";
		String outputBase = "../../nemo/data/input/network/coarse/";
		String networkFile = outputBase + "network_coarse.xml.gz";

		// Parameters
		// EPSG:4326 = WGS84
		// EPSG:31468 = DHDN GK4, for Berlin; DE
		// EPSG:26918 = NAD83 / UTM zone 18N, for Maryland, US
		// EPSG:25832 = ETRS89 / UTM zone 32N, for Nordrhein-Westfalen
		String inputCRS = "EPSG:4326"; 
		String outputCRS = "EPSG:25832";
		
		createNetwork(osmFile, outputBase, networkFile, inputCRS, outputCRS);
	}

	public static void createNetwork(String osmFile, String outputBase, String networkFile, String inputCRS, String outputCRS) {
		try {
			OutputDirectoryLogging.initLoggingWithOutputDirectory(outputBase);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		LOG.info("Input CRS is " + inputCRS + "; output CRS is " + outputCRS);
		
		
		boolean keepPaths = false;
		boolean includeLowHierarchyWays = true;
		boolean onlyBiggerRoads = false; // "thinner" network; do not use this together with "includeLowHierarchyWays"
		LOG.info("Settings: includeLowHierarchyWays = " + includeLowHierarchyWays + "; keepPaths = " + keepPaths);
		
		// Infrastructure
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		Network network = scenario.getNetwork();
		CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(inputCRS, outputCRS);
		OsmNetworkReader osmNetworkReader = null;
		if (onlyBiggerRoads == true) {
			osmNetworkReader = new OsmNetworkReader(network, coordinateTransformation, false);
		} else {
			osmNetworkReader = new OsmNetworkReader(network, coordinateTransformation, true);
		}
		NetworkWriter networkWriter = new NetworkWriter(network);
		
		// Keeping the path means that links are not straightened between intersection nodes, but that also pure geometry-describing
		// nodes are kept. This makes the file (for the Nairobi case) three times as big (22.4MB vs. 8.7MB)
		if (keepPaths == true) {
			LOG.info("Detailed geometry of paths is kept.");
			osmNetworkReader.setKeepPaths(true);
		}
				
		// This block is for the low hierarchy roads
		if (includeLowHierarchyWays == true) {
			LOG.info("Low hierarchy ways are included.");
			// By defaults set for motorway, motorway_link, trunk, trunk_link, primary, primary_link, secondary, secondary_link,
			// tertiary, tertiary_link, minor, unclassified, residential, living_street; minor does not exist on the website anymore
			// Parameters for living_street: (6, "living_street", 1,  15.0/3.6, 1.0,  300);
			// (hierarchy, highwayType, lanes, freespeed, freespeedFactor, laneCapacity_vehPerHour)
			//
			// Other types in osm, see: http://wiki.openstreetmap.org/wiki/Key:highway
			// pedestrian, track, bus_guideway, raceway, road, footway, bridleway, steps, path
			osmNetworkReader.setHighwayDefaults(7, "pedestrian", 1, 15/3.6, 1.0, 0);
			osmNetworkReader.setHighwayDefaults(7, "track", 1, 15/3.6, 1.0, 0);
			osmNetworkReader.setHighwayDefaults(7, "road", 1, 15/3.6, 1.0, 300); // like "living_street"
			osmNetworkReader.setHighwayDefaults(7, "footway", 1, 15/3.6, 1.0, 0);
			osmNetworkReader.setHighwayDefaults(7, "bridleway", 1, 15/3.6, 1.0, 0);
			osmNetworkReader.setHighwayDefaults(7, "steps", 1, 15/3.6, 1.0, 0);
			osmNetworkReader.setHighwayDefaults(7, "path", 1, 15/3.6, 1.0, 0);
		}		
				
		// This block is to use only bigger roads; makes file (for the Maryland case) only a 14th as big (77.8MB vs. 1.04GB)
		if (onlyBiggerRoads == true) {
			LOG.info("Only bigger roads are included.");
			if (includeLowHierarchyWays == true) {
				throw new RuntimeException("It does not make sense to set both \"includeLowHierarchyWays\""
						+ " and \"onlyBiggerRoads\" to true");
			}
			osmNetworkReader.setHighwayDefaults(1, "motorway",      2, 120.0/3.6, 1.0, 2000, true);
			osmNetworkReader.setHighwayDefaults(1, "motorway_link", 1,  80.0/3.6, 1.0, 1500, true);
			osmNetworkReader.setHighwayDefaults(2, "trunk",         1,  80.0/3.6, 1.0, 2000);
			osmNetworkReader.setHighwayDefaults(2, "trunk_link",    1,  50.0/3.6, 1.0, 1500);
			osmNetworkReader.setHighwayDefaults(3, "primary",       1,  80.0/3.6, 1.0, 1500);
			osmNetworkReader.setHighwayDefaults(3, "primary_link",  1,  60.0/3.6, 1.0, 1500);
			osmNetworkReader.setHighwayDefaults(4, "secondary",     1,  30.0/3.6, 1.0, 1000);
			osmNetworkReader.setHighwayDefaults(4, "secondary_link",     1,  30.0/3.6, 1.0, 1000);
			osmNetworkReader.setHighwayDefaults(5, "tertiary",      1,  25.0/3.6, 1.0,  600);
			osmNetworkReader.setHighwayDefaults(5, "tertiary_link",      1,  25.0/3.6, 1.0,  600);
		}

		osmNetworkReader.parse(osmFile); 
		new NetworkCleaner().run(network);
		networkWriter.write(networkFile);
		LOG.info("Network file written to " + networkFile);
	}
}