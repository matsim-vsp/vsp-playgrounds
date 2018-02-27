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

package playground.ikaddoura.analysis.network;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
* @author ikaddoura
*/

public class ComputeTotalLength {

	public static void main(String[] args) {

		Config config = ConfigUtils.createConfig();
		config.network().setInputFile("/Users/ihab/Documents/workspace/mercator-nemo/data/matsim_input/2018-02-16_scenario_detailedNet_Ruhr/detailedRuhr_Network_17022018filteredcleaned_network.xml.gz");
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		double totalLength = 0.;
		int linkCounter = 0;
		
		for (Link link : scenario.getNetwork().getLinks().values()) {
			totalLength = totalLength + link.getLength();
			linkCounter++;
		}
		
		System.out.println("Total length (km): " + totalLength / 1000.);
		System.out.println("Total number of road segments: " + linkCounter);
		
	}

}

