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

package playground.ikaddoura.analysis.vtts;

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

/**
 *
 * Analyze the actual VTTS for each trip (applying a linearization for each activity) 
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
			runDirectory = "/Users/ihab/Documents/workspace/runs-svn/optAV_new/output/output_v0_SAVuserOpCostPricingF_SAVuserExtCostPricingF_SAVdriverExtCostPricingF_CCuserExtCostPricingT/";
			runId = "run1";
		}
		
		VTTSanalysisMain analysis = new VTTSanalysisMain();
		analysis.run();
	}

	private void run() {
		
		String configFile;
		if (runId == null) {
			configFile = runDirectory + "output_config.xml";
		} else {
			configFile = runDirectory + runId + ".output_config.xml";
		}

		Config config = ConfigUtils.loadConfig(configFile);	
				
		String populationFile = null;
		String networkFile = null;
//		String networkFile = runDirectory + "output_network.xml.gz";
		
		config.plans().setInputFile(populationFile);
		config.network().setInputFile(networkFile);
		config.plans().setInputPersonAttributeFile(null);
		config.transit().setTransitScheduleFile(null);
		config.transit().setVehiclesFile(null);
		config.vehicles().setVehiclesFile(null);
		
		MutableScenario scenario = (MutableScenario) ScenarioUtils.loadScenario(config);
		EventsManager events = EventsUtils.createEventsManager();
		
		VTTSHandler vttsHandler = new VTTSHandler(scenario);
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
	}
			 
}
		

