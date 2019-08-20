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

package playground.ikaddoura.analysisRunClasses;

import java.io.File;

import org.apache.log4j.Logger;
import org.matsim.analysis.vtts.VTTSHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;

/**
 *
 * Computes the effective VTTS for each person and trip (applying a linearization for each trip/activity).
 * 
 * @author ikaddoura
 */
public class VTTSanalysisMain {
	private static final Logger log = Logger.getLogger(VTTSanalysisMain.class);

	private static String runDirectory;
	private static String runId;
	
	public static void main(String[] args) {
		
		if (args.length > 0) {
			runDirectory = args[0];
			runId = args[1];		
			log.info("run directory: " + runDirectory);
			log.info("run Id: " + runId);
			
		} else {
//			runDirectory = "/Users/ihab/Documents/workspace/runs-svn/vw_rufbus/vw219/";
//			runId = "vw219";
			
//			runDirectory = "/Users/ihab/Documents/workspace/runs-svn/berlin_equal_vs_different_VTTS/output/baseCase/";
//			runId = null;
			
//			runDirectory = "/Users/ihab/Desktop/v2b/v2b/santiago/output/baseCase10pct/";
//			runId = null;
			
//			runDirectory = "/Users/ihab/Documents/workspace/runs-svn/cottbus/laemmer/2018-02-8-11-59-30_100it_MS_cap07_stuck120_tbs900/";
//			runId = "1000";
			
//			runDirectory = "/Users/ihab/Documents/workspace/runs-svn/open_berlin_scenario/b5_w3a19/";
//			runId = "b5_w3a19";
			
			runDirectory = "/Users/ihab/Documents/workspace/matsim-berlin/scenarios/berlin-v5.0-1pct-2018-06-18/output_from-reduced-config_FlowStorageCapacityFactor0.015_2018-07-04/";
			runId = "b5_1";
		}
		
		VTTSanalysisMain analysis = new VTTSanalysisMain();
		analysis.run();
	}

	private void run() {
		
		String configFile;
		String attributesFile;
		if (runId == null) {
			configFile = runDirectory + "output_config.xml";
			attributesFile = runDirectory + "output_personAttributes.xml.gz";
		} else {
			configFile = runDirectory + runId + ".output_config.xml";
			attributesFile = runDirectory + runId + ".output_personAttributes.xml.gz";
		}

		Config config = ConfigUtils.loadConfig(configFile);	
				
		String populationFile = null;
		String networkFile = null;
		
		config.plans().setInputFile(populationFile);
		config.network().setInputFile(networkFile);
		if (new File(attributesFile).exists()) {
			config.plans().setInputPersonAttributeFile(attributesFile);
		} else {
			config.plans().setInputPersonAttributeFile(null);
		}
		config.transit().setTransitScheduleFile(null);
		config.transit().setVehiclesFile(null);
		config.vehicles().setVehiclesFile(null);
		config.network().setLaneDefinitionsFile(null);
		
		MutableScenario scenario = (MutableScenario) ScenarioUtils.loadScenario(config);
		EventsManager events = EventsUtils.createEventsManager();
		
		VTTSHandler vttsHandler = new VTTSHandler(scenario, new String[]{"transit_walk", "access_walk", "egress_walk", "non_network_walk"}, "interaction");
		events.addHandler(vttsHandler);
			
		String outputDirectoryWithRunId;
		if (runId == null) {
			outputDirectoryWithRunId = runDirectory;
		} else {
			outputDirectoryWithRunId = runDirectory + runId + ".";
		}
		
		String eventsFile = outputDirectoryWithRunId + "output_events.xml.gz";

		log.info("Reading the events file...");
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
		log.info("Reading the events file... Done.");
		
		vttsHandler.computeFinalVTTS();
		
		vttsHandler.printVTTS(outputDirectoryWithRunId + "VTTS_allTrips.csv");
		vttsHandler.printCarVTTS(outputDirectoryWithRunId + "VTTS_carTrips.csv");
		vttsHandler.printAvgVTTSperPerson(outputDirectoryWithRunId + "VTTS_avgPerPerson.csv");
		
		vttsHandler.printVTTSstatistics(outputDirectoryWithRunId + "VTTS_statistics_all-modes.csv", null, null);
		vttsHandler.printVTTSstatistics(outputDirectoryWithRunId + "VTTS_statistics_car.csv", "car", null);
		vttsHandler.printVTTSstatistics(outputDirectoryWithRunId + "VTTS_statistics_pt.csv", "pt", null);
		vttsHandler.printVTTSstatistics(outputDirectoryWithRunId + "VTTS_statistics_bicycle.csv", "bicycle", null);
		vttsHandler.printVTTSstatistics(outputDirectoryWithRunId + "VTTS_statistics_walk.csv", "walk", null);
		vttsHandler.printVTTSstatistics(outputDirectoryWithRunId + "VTTS_statistics_ride.csv", "ride", null);


		vttsHandler.printVTTSstatistics(outputDirectoryWithRunId + "VTTS_statistics_car_7-9.csv", "car", new Tuple<Double, Double>(7.0 * 3600., 9. * 3600.));
		vttsHandler.printVTTSstatistics(outputDirectoryWithRunId + "VTTS_statistics_car_11-13.csv", "car", new Tuple<Double, Double>(11.0 * 3600., 13. * 3600.));
		vttsHandler.printVTTSstatistics(outputDirectoryWithRunId + "VTTS_statistics_16-18.csv", "car", new Tuple<Double, Double>(16.0 * 3600., 18. * 3600.));
	}
			 
}
		

