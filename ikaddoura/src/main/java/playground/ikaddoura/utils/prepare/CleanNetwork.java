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

package playground.ikaddoura.utils.prepare;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.MultimodalNetworkCleaner;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;

/**
* @author ikaddoura
*/

public class CleanNetwork {

	public static void main(String[] args) {

		final String[] cleaningModesArray = {TransportMode.car, TransportMode.ride, TransportMode.bike};
		final String inputNetwork = "/Users/ihab/Documents/workspace/shared-svn/projects/nemo_mercator/data/matsim_input/network/zzz_fineNetworkPtAndCarCountsWithoutBikeOnlyGesundeNachhaltigeStadt/fineNetworkPtAndCarCountsWithoutBikeOnlyGesundeNachhaltigeStadt.xml.gz";
		final String outputFile = "/Users/ihab/Documents/workspace/shared-svn/projects/nemo_mercator/data/matsim_input/network/zzz_fineNetworkPtAndCarCountsWithoutBikeOnlyGesundeNachhaltigeStadt/fineNetworkPtAndCarCountsWithoutBikeOnlyGesundeNachhaltigeStadt_cleaned.xml.gz";
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(inputNetwork);
		
		for (String mode : new HashSet<>(Arrays.asList(cleaningModesArray))) {
			new MultimodalNetworkCleaner(scenario.getNetwork()).run(new HashSet<>(Collections.singletonList(mode)));
		}
		
		new NetworkWriter(scenario.getNetwork()).write(outputFile);
	}

}

