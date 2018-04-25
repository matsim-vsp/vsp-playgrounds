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

package playground.ikaddoura.berlin.networkCounts;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;

/**
* @author ikaddoura
*/

public class AdjustNetwork {
	
	public static void main(String[] args) {
		
		String input = "/Users/ihab/Documents/workspace/shared-svn/studies/countries/de/open_berlin_scenario/be_3/network/network_with-PT-GTFS_400.xml.gz";
		String output = "/Users/ihab/Documents/workspace/shared-svn/studies/countries/de/open_berlin_scenario/be_3/network/network_with-PT-GTFS_400_reducedCapacity-0.5.xml.gz";
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(input);
				
		int counter = 0;
		for (Link link : scenario.getNetwork().getLinks().values()) {
			if (link.getAllowedModes().contains(TransportMode.car)) {
				if (link.getFreespeed() < 14.) {
					link.setCapacity(link.getCapacity() * 0.5);
					counter++;
				}
			}
		}
		
		new NetworkWriter(scenario.getNetwork()).write(output);
		System.out.println("Adjusted links: " + counter);
	}
}

