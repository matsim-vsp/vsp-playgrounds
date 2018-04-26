/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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
  
package playground.jbischoff.examples;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class CleanTransitStops {
	public static void main(String[] args) {
	
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new TransitScheduleReader(scenario).readFile("D:/melbourne2046/25pct/lastmile_robotaxi_2046_25pct_input/output_transitSchedule.xml.gz");
		final Set<Id<TransitStopFacility>> stops2keep = new HashSet<>();
		for (TransitLine l : scenario.getTransitSchedule().getTransitLines().values()) {
			for (TransitRoute r : l.getRoutes().values()) {
				r.getStops().forEach(s ->stops2keep.add( s.getStopFacility().getId()));
			}
		}
		
		System.out.println("Schedule has "+ scenario.getTransitSchedule().getFacilities().size() + " stops. Of these, "+stops2keep.size() +" are used." );
		final Set<Id<TransitStopFacility>> stops2remove = new HashSet<>();
		for (Id<TransitStopFacility> stop : scenario.getTransitSchedule().getFacilities().keySet()) {
			if (!stops2keep.contains(stop)) {
				stops2remove.add(stop);
			}
		}
		for (Id<TransitStopFacility> stop : stops2remove) {
			TransitStopFacility s = scenario.getTransitSchedule().getFacilities().get(stop);
			scenario.getTransitSchedule().removeStopFacility(s);
		}
		new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile("D:/melbourne2046/25pct/lastmile_robotaxi_2046_25pct_input/output_transitSchedule.xml.gz");
	}
}
