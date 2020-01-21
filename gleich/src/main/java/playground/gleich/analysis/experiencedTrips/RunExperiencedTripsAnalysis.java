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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

/**
 * Runner that guesses configuration based on output directory path (1 arg)
 * Otherwise detailed configuration (7 args[]) is also available.
 * 
 * 
 * 
 * TODO: run without transit schedule, move to contribs/analysis, multiple drt modes (-> stageActivities), test
 * 
 * @author gleich
 *
 */
public class RunExperiencedTripsAnalysis {
	private final static Logger log = Logger.getLogger(RunExperiencedTripsAnalysis.class);
	private final String eventsFile;
	private final String monitoredStartAndEndLinksFile;

	private final Scenario scenario;
	private final Set<String> monitoredModes;
	private final String drtModeName;

	public RunExperiencedTripsAnalysis(String string) {
		// got only output directory path, guess the details
		String networkFile = string + "output_network.xml.gz";
		String scheduleFile = string + "output_transitSchedule.xml.gz";
		eventsFile = string + "output_events.xml.gz";
		monitoredStartAndEndLinksFile = null;
		
        scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);
        new TransitScheduleReader(scenario).readFile(scheduleFile);

		monitoredModes = new HashSet<>();
		// add some common default monitored modes
		monitoredModes.add(TransportMode.pt);
		monitoredModes.add(TransportMode.drt);
		monitoredModes.add(TransportMode.walk);
		monitoredModes.add(TransportMode.car);
		monitoredModes.add("bicycle");
		monitoredModes.add("freight");
		// find transportModes in schedule
		for (TransitLine line : scenario.getTransitSchedule().getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				if (!monitoredModes.contains(route.getTransportMode())) {
					monitoredModes.add(route.getTransportMode());
				}
			}
		}
		drtModeName = TransportMode.drt;
	}

	public RunExperiencedTripsAnalysis(String[] args) {
		// got detailed args, set accordingly
		String networkFile = args[0];
		String scheduleFile = args[1];
		eventsFile = args[2];
		monitoredStartAndEndLinksFile = args[3];
		monitoredModes = new HashSet<>();
		for (String mode : args[4].split(",")) {
			monitoredModes.add(mode);
		}
		drtModeName = args[7];
        scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);
        new TransitScheduleReader(scenario).readFile(scheduleFile);
	}

	public static void main(String[] args) {
		String experiencedTripsFile = null;
		String experiencedLegsFile = null;
		RunExperiencedTripsAnalysis runner;

		if (args.length == 1) {
			runner = new RunExperiencedTripsAnalysis(args[0]);
			experiencedTripsFile = args[0] + "experiencedTrips.csv.gz";
			experiencedLegsFile = args[0] + "experiencedLegs.csv.gz";
		} else if (args.length == 8) {
			runner = new RunExperiencedTripsAnalysis(args);
			experiencedTripsFile = args[5];
			experiencedLegsFile = args[6];
		} else {
			throw new RuntimeException("Wrong number of args to main method. "
					+ "Should be either only path to output directory or \n" + "networkFile, scheduleFile, eventsFile, "
					+ "monitoredStartAndEndLinksFile (use null if you want all links in the analysis.), "
					+ "monitoredModes (separated by ,),"
					+ "experiencedTripsFile (use null to switch off, a path ending .gz will cause the output to be zipped), "
					+ "experiencedLegsFile (use null to switch off, a path ending .gz will cause the output to be zipped), "
					+ "drtModeName");
		}

		ExperiencedTripsWriter tripsWriter = new ExperiencedTripsWriter(runner.calcAgent2trips(),
				runner.monitoredModes);
		if (experiencedTripsFile != null && ! experiencedTripsFile.equals("")) {
			tripsWriter.writeExperiencedTrips(experiencedTripsFile);
		}
		if (experiencedLegsFile != null && ! experiencedLegsFile.equals("")) {
			tripsWriter.writeExperiencedLegs(experiencedLegsFile);
		}

	}

	public Map<Id<Person>, List<ExperiencedTrip>> calcAgent2trips() {
		// Analysis
		EventsManager events = EventsUtils.createEventsManager();
		Set<Id<Link>> monitoredStartAndEndLinks = readMonitoredStartAndEndLinks();

		DrtPtTripEventHandler eventHandler = new DrtPtTripEventHandler(scenario.getNetwork(),
				scenario.getTransitSchedule(), monitoredModes, monitoredStartAndEndLinks, drtModeName);
		events.addHandler(eventHandler);
		new DrtEventsReader(events).readFile(eventsFile);

		Map<Id<Person>, List<ExperiencedTrip>> agent2trips = eventHandler.getPerson2ExperiencedTrips();
		log.info("found " + eventHandler.getNumberOfExperiencedTrips() + " experienced trips of "
				+ eventHandler.getPerson2ExperiencedTrips().size() + " agents.");
		return agent2trips;
	}

	private Set<Id<Link>> readMonitoredStartAndEndLinks() {
		Set<Id<Link>> monitoredStartAndEndLinks = new HashSet<>();
		if (monitoredStartAndEndLinksFile != null && monitoredStartAndEndLinksFile != "null") {
			try {
				BufferedReader reader = new BufferedReader(new FileReader(monitoredStartAndEndLinksFile));
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
		return monitoredStartAndEndLinks;
	}

}
