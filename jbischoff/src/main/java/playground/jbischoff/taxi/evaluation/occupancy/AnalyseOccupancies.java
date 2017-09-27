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
package playground.jbischoff.taxi.evaluation.occupancy;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class AnalyseOccupancies {
	public static void main(String[] args) {
	
		Network network = NetworkUtils.createNetwork();
		String path = "D:/runs-svn/restored_old_IEEE_berlin_taxi/";
		new MatsimNetworkReader(network).readFile(path+"only_berlin.xml.gz");
		List<String> runs = new ArrayList<>();
		runs.add("output_ASSIGNMENT_TP_1.0");
		runs.add("output_ASSIGNMENT_TP_2.0");
		runs.add("output_ASSIGNMENT_TP_3.0");
		runs.add("output_ASSIGNMENT_TP_4.0");
		runs.add("output_ASSIGNMENT_TW_1.0");
		runs.add("output_ASSIGNMENT_TW_2.0");
		runs.add("output_ASSIGNMENT_TW_3.0");
		runs.add("output_ASSIGNMENT_TW_4.0");
		runs.add("output_RULE_BASED_DSE_1.0");
		runs.add("output_RULE_BASED_DSE_2.0");
		runs.add("output_RULE_BASED_DSE_3.0");
		runs.add("output_RULE_BASED_DSE_4.0");
		runs.add("output_RULE_BASED_TW_1.0");
		runs.add("output_RULE_BASED_TW_2.0");
		runs.add("output_RULE_BASED_TW_3.0");
		String output = "";
		for (String run : runs){
			String eventsfile = path+"/"+run+"/output_events.xml";
			EventsManager events = EventsUtils.createEventsManager();
			OccupancyDistanceEvaluator ev = new OccupancyDistanceEvaluator(network);
			events.addHandler(ev);
			new MatsimEventsReader(events).readFile(eventsfile);
			String result = run + "\t"+ev.getEmptyKm()+"\t"+ev.getOccupiedKm()+"\n";
			output = output + result;
		}
		System.out.println(output);
	}
}
