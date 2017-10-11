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
package playground.dziemke.input;

import org.matsim.contrib.accessibility.utils.AccessibilityOsmNetworkReader;

/**
 * @author dziemke
 */
public class OsmNetworkCreatorDZ {

	public static void main(String[] args) {
		// String osmFileName = "../../nemo/data/input/counts/verkehrszaehlung_2015/network/allWaysNRW.osm";
		// String outputRoot = "../../nemo/data/input/network/motorway-tertiary/";
		// String networkFileName = outputRoot + "network_motorway-tertiary.xml.gz";
		String osmFileName = "../../capetown/data/osm/2017-10-03";
		String outputRoot = "../../capetown/data/network/";
		String networkFileName = outputRoot + "2017-10-03_network_motorway_secondary.xml.gz";

		// EPSG:4326 = WGS84
		// EPSG:31468 = DHDN GK4, for Berlin; DE
		// EPSG:26918 = NAD83 / UTM zone 18N, for Maryland, US
		// EPSG:25832 = ETRS89 / UTM zone 32N, for Nordrhein-Westfalen
		// EPSG:32734 = WGS 84 / UTM zone 34S, for South Africa
		String outputCRS = "EPSG:32734";
		
		AccessibilityOsmNetworkReader osmNetworkCreatorDZ = new AccessibilityOsmNetworkReader(osmFileName, outputCRS);
		osmNetworkCreatorDZ.setOnlyMotorwayToSecondary(true);
		osmNetworkCreatorDZ.createNetwork();
		
		osmNetworkCreatorDZ.writeNetwork(outputRoot, networkFileName);
	}
}