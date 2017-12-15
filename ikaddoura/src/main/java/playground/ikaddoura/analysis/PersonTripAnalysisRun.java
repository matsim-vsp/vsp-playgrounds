/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.ikaddoura.analysis;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import playground.ikaddoura.analysis.detailedPersonTripAnalysis.PersonTripNoiseAnalysis;
import playground.ikaddoura.analysis.detailedPersonTripAnalysis.handler.BasicPersonTripAnalysisHandler;
import playground.ikaddoura.analysis.linkDemand.LinkDemandEventHandler;
import playground.ikaddoura.analysis.shapes.Network2Shape;
import playground.ikaddoura.decongestion.handler.DelayAnalysis;

/**
 * 
 * Provides the following analysis: 
 * 
 * aggregated results: number of trips, number of stuck trips, travel time, travel distance, caused/affected noise cost, toll payments, user benefits, welfare
 * 
 * trip-based information
 * person ; trip no.; leg mode ; stuckAbort (trip) ; departure time (trip) ; trip arrival time (trip) ; travel time (trip) ; travel distance (trip) ; toll payment (trip)
 * 
 * person-based information
 * person ; total no. of trips (day) ; travel time (day) ; travel distance (day) ; toll payments (day) ; affected noise cost (day)
 * 
 * 
 */
public class PersonTripAnalysisRun {
	private static final Logger log = Logger.getLogger(PersonTripAnalysisRun.class);

	private final String runDirectory;
	private final String runId;
	private final Scenario scenario;
	
	private String crs = TransformationFactory.DHDN_GK4;
			
	public static void main(String[] args) {
			
		String runDirectory;
		
		if (args.length > 0) {
			runDirectory = args[0];
			log.info("Run directory: " + runDirectory);
		
		} else {
			
			runDirectory = "/Users/ihab/Documents/workspace/runs-svn/incidents/berlin/output/output_2016-02-11_networkChangeEvents-true_withinDayReplanning-true-5minutes";
			log.info("Run directory " + runDirectory);
		}
		
		PersonTripAnalysisRun analysis = new PersonTripAnalysisRun(runDirectory, null);
		analysis.run();
	}
	
	public PersonTripAnalysisRun(String runDirectory, String runId) {
		
		if (!runDirectory.endsWith("/")) runDirectory = runDirectory + "/";
		
		this.runDirectory = runDirectory;
		this.runId = runId;
		
		String networkFile;
		String populationFile;
		
		if (runId == null) {
			networkFile = runDirectory + "output_network.xml.gz";
			populationFile = runDirectory + "output_plans.xml.gz";	
		} else {
			networkFile = runDirectory + runId + ".output_network.xml.gz";
			populationFile = runDirectory + runId + ".output_plans.xml.gz";	
		}

		Config config = ConfigUtils.createConfig();	
		config.plans().setInputFile(populationFile);
		config.network().setInputFile(networkFile);
		
		this.scenario = ScenarioUtils.loadScenario(config);
	}
	
	public PersonTripAnalysisRun(Scenario scenario) {
		
		String runDirectory = scenario.getConfig().controler().getOutputDirectory();
		if (!runDirectory.endsWith("/")) runDirectory = runDirectory + "/";

		this.runDirectory = runDirectory;
		this.scenario = scenario;
		this.runId = scenario.getConfig().controler().getRunId();
	}

	public void run() {
		
		String eventsFile;
		if (runId == null) {
			eventsFile = runDirectory + "output_events.xml.gz";
		} else {
			eventsFile = runDirectory + runId + ".output_events.xml.gz";
		}
		
		String analysisOutputDirectory = runDirectory + "analysis/";
		File folder = new File(analysisOutputDirectory);			
		folder.mkdirs();
		
		OutputDirectoryLogging.catchLogEntries();
		try {
			OutputDirectoryLogging.initLoggingWithOutputDirectory(analysisOutputDirectory);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	
		BasicPersonTripAnalysisHandler basicHandler = new BasicPersonTripAnalysisHandler();
		basicHandler.setScenario(scenario);

		DelayAnalysis delayAnalysis = new DelayAnalysis();
		delayAnalysis.setScenario(scenario);
		
		LinkDemandEventHandler trafficVolumeAnalysis = new LinkDemandEventHandler(scenario.getNetwork());
		
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(basicHandler);
		events.addHandler(delayAnalysis);
		events.addHandler(trafficVolumeAnalysis);
		
		log.info("Reading the events file...");
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
		log.info("Reading the events file... Done.");
				
		// plans
		
		Map<Id<Person>, Double> personId2userBenefit = new HashMap<>();
		for (Person person : scenario.getPopulation().getPersons().values()) {
			personId2userBenefit.put(person.getId(), person.getSelectedPlan().getScore() / scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney());
		}
		
		// print the results
		
		PersonTripNoiseAnalysis analysis = new PersonTripNoiseAnalysis();
		
		log.info("Print trip information...");
		analysis.printTripInformation(analysisOutputDirectory, TransportMode.car, basicHandler, null, null);
		log.info("Print trip information... Done.");

		log.info("Print person information...");
		analysis.printPersonInformation(analysisOutputDirectory, TransportMode.car, personId2userBenefit, basicHandler, null);	
		log.info("Print person information... Done.");

		analysis.printAggregatedResults(analysisOutputDirectory, TransportMode.car, personId2userBenefit, basicHandler, null);
		analysis.printAggregatedResults(analysisOutputDirectory, null, personId2userBenefit, basicHandler, null);
		
		analysis.printAggregatedResults(analysisOutputDirectory, personId2userBenefit, basicHandler, null, null, delayAnalysis, null);
		
		SortedMap<Double, List<Double>> departureTime2tolls = analysis.getParameter2Values(TransportMode.car, basicHandler, basicHandler.getPersonId2tripNumber2departureTime(), basicHandler.getPersonId2tripNumber2payment(), 3600., 30 * 3600.);
		analysis.printAvgValuePerParameter(analysisOutputDirectory + "tollsPerDepartureTime_car_3600.csv", departureTime2tolls);
		
		SortedMap<Double, List<Double>> departureTime2traveldistance = analysis.getParameter2Values(TransportMode.car, basicHandler, basicHandler.getPersonId2tripNumber2departureTime(), basicHandler.getPersonId2tripNumber2tripDistance(), 3600., 30 * 3600.);
		analysis.printAvgValuePerParameter(analysisOutputDirectory + "distancePerDepartureTime_car_3600.csv", departureTime2traveldistance);
		
		SortedMap<Double, List<Double>> departureTime2travelTime = analysis.getParameter2Values(TransportMode.car, basicHandler, basicHandler.getPersonId2tripNumber2departureTime(), basicHandler.getPersonId2tripNumber2travelTime(), 3600., 30 * 3600.);
		analysis.printAvgValuePerParameter(analysisOutputDirectory + "travelTimePerDepartureTime_car_3600.csv", departureTime2travelTime);
	
		trafficVolumeAnalysis.printResults(analysisOutputDirectory + "link_dailyTrafficVolume.csv");
		Network2Shape.exportNetwork2Shp1(scenario, crs, TransformationFactory.getCoordinateTransformation(crs, crs));

	}

	public void setCrs(String crs) {
		this.crs = crs;
	}
}
		

