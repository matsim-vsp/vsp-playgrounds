/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
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

package playground.gleich.analysis.experiencedTrips;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.av.intermodal.router.VariableAccessTransitRouterModule;
import org.matsim.contrib.av.intermodal.router.config.VariableAccessConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigConsistencyChecker;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

public class RunExperiencedTripsAnalysis {
	public static void main(String[] args) {
		if (args.length != 4) {
			throw new RuntimeException("Wrong number of args to main method. " + ""
					+ "Should be path to config, path to output_events file, "+
					"monitoredModes (separated by ,) and path to monitoredStartAndEndLinks. " + 
					"Use null for the latter if you do not want this in the analysis.");
		}
		Config config = ConfigUtils.loadConfig(args[0]);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		// Analysis
		EventsManager events = EventsUtils.createEventsManager();
		Set<String> monitoredModes = new HashSet<>();
		for (String mode: args[2].split(",")) {
			monitoredModes.add(mode);
		}
		
		Set<Id<Link>> monitoredStartAndEndLinks = new HashSet<>();
		if (args[1] != "null") {
				try {
					BufferedReader reader = new BufferedReader(new FileReader(IOUtils.newUrl(config.getContext(), args[3]).getFile()));
					if (reader.readLine().startsWith("id")) {
						for (String line = reader.readLine(); line != null; line = reader.readLine()) {
							monitoredStartAndEndLinks.add(Id.createLinkId(line.split(",")[0]));
						}
						reader.close();
					} else {
						reader.close();
						throw new RuntimeException("linksInArea.csv : first column in header should be id.");
					}
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		
		DrtPtTripEventHandler eventHandler = new DrtPtTripEventHandler(scenario.getNetwork(), scenario.getTransitSchedule(), 
				monitoredModes, monitoredStartAndEndLinks);
		events.addHandler(eventHandler);
		new DrtEventsReader(events).readFile(args[1] + "output_events.xml.gz");
		System.out.println("Start writing trips of " + eventHandler.getPerson2ExperiencedTrips().size() + " agents.");
		ExperiencedTripsWriter tripsWriter = new ExperiencedTripsWriter(args[1] +
				"/experiencedTrips.csv", 
				eventHandler.getPerson2ExperiencedTrips(), monitoredModes);
		tripsWriter.writeExperiencedTrips();
		ExperiencedTripsWriter legsWriter = new ExperiencedTripsWriter(args[1] + 
				"/experiencedLegs.csv", 
				eventHandler.getPerson2ExperiencedTrips(), monitoredModes);
		legsWriter.writeExperiencedLegs();
	}

}
