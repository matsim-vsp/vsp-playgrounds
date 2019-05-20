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

package playground.ikaddoura.analysis.detailedPersonTripAnalysis;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
import org.matsim.core.scenario.ScenarioUtils;

import playground.ikaddoura.analysis.detailedPersonTripAnalysis.handler.BasicPersonTripAnalysisHandler;

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
			
	public static void main(String[] args) {
			
		String runDirectory;
		String runId;
		
		if (args.length > 0) {
			runDirectory = args[0];
			runId = args[1];
			log.info("Run directory: " + runDirectory);
			log.info("Run Id: " + runId);
		
		} else {		
			runDirectory = "/Users/ihab/Desktop/ils4a/kaddoura/sav-pricing/scenarios/berlin-v5.2-10pct/output_tx1-10_0c";
			runId = "tx1-10_0c";
		}
		
		PersonTripAnalysisRun analysis = new PersonTripAnalysisRun(runDirectory, runId);
		analysis.run();
	}
	
	public PersonTripAnalysisRun(String runDirectory, String runId) {
		
		if (!runDirectory.endsWith("/")) runDirectory = runDirectory + "/";
		
		this.runDirectory = runDirectory;
		this.runId = runId;
	}

	public void run() {
		
		String networkFile = runDirectory + runId + ".output_network.xml.gz";
		String populationFile = runDirectory + runId + ".output_plans.xml.gz";
		String eventsFile = runDirectory + runId + ".output_events.xml.gz";
		String configFile = runDirectory + runId + ".output_config.xml";

		Config config = ConfigUtils.loadConfig(configFile);	
		config.plans().setInputFile(populationFile);
		config.network().setInputFile(networkFile);
		
		String outputPath = runDirectory + "detailed-person-trip-analysis/";
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		File folder = new File(outputPath);			
		folder.mkdirs();
		
		OutputDirectoryLogging.catchLogEntries();
		try {
			OutputDirectoryLogging.initLoggingWithOutputDirectory(outputPath);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	
		BasicPersonTripAnalysisHandler basicHandler = new BasicPersonTripAnalysisHandler();
		basicHandler.setScenario(scenario);

		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(basicHandler);
		
		log.info("Reading the events file...");
		IKEventsReader reader = new IKEventsReader(events);
		reader.readFile(eventsFile);
		log.info("Reading the events file... Done.");
				
		// plans
		
		Map<Id<Person>, Double> personId2userBenefit = new HashMap<>();
		for (Person person : scenario.getPopulation().getPersons().values()) {
			personId2userBenefit.put(person.getId(), person.getSelectedPlan().getScore() / scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney());
		}
		
		// print the results
		
		PersonTripAnalysis analysis = new PersonTripAnalysis();
		
		log.info("Print trip information...");
		analysis.printTripInformation(outputPath, TransportMode.car, basicHandler, null, null);
		log.info("Print trip information... Done.");

		log.info("Print person information...");
		analysis.printPersonInformation(outputPath, TransportMode.car, personId2userBenefit, basicHandler, null);	
		log.info("Print person information... Done.");

		analysis.printAggregatedResults(outputPath, TransportMode.car, personId2userBenefit, basicHandler, null);
		analysis.printAggregatedResults(outputPath, null, personId2userBenefit, basicHandler, null);
		
		analysis.printAggregatedResults(outputPath, personId2userBenefit, basicHandler, null, null, null);
	}
}
		

