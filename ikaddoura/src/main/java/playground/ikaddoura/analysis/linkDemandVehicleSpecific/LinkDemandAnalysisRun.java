/* *********************************************************************** *
* project: org.matsim.*
* firstControler
* *
* *********************************************************************** *
* *
* copyright : (C) 2007 by the members listed in the COPYING, *
* LICENSE and WARRANTY file. *
* email : info at matsim dot org *
* *
* *********************************************************************** *
* *
* This program is free software; you can redistribute it and/or modify *
* it under the terms of the GNU General Public License as published by *
* the Free Software Foundation; either version 2 of the License, or *
* (at your option) any later version. *
* See also COPYING, LICENSE and WARRANTY file *
* *
* *********************************************************************** */ 

package playground.ikaddoura.analysis.linkDemandVehicleSpecific;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;

public class LinkDemandAnalysisRun {
	
	private static String OUTPUT_BASE_DIR = "/Users/ihab/Documents/workspace/runs-svn/cne/munich/output-final/output_run4b_muc_cne_DecongestionPID";
	private String runId = null;
	private String outputDirectory;
	private String vehicleTypePrefix = "5";

	public LinkDemandAnalysisRun(String outputDirectory) {
		this.outputDirectory = outputDirectory;
	}

	public static void main(String[] args) {
		LinkDemandAnalysisRun anaMain = new LinkDemandAnalysisRun(OUTPUT_BASE_DIR);
		anaMain.run();
	}

	public void run() {
		
		if (!outputDirectory.endsWith("/")) {
			outputDirectory = outputDirectory + "/";
		}
	
		Config config;
		if (runId != null) {
			config = ConfigUtils.loadConfig(outputDirectory + runId + ".output_config.xml");
			config.plans().setInputFile(null);
			config.plans().setInputPersonAttributeFile(null);
			config.network().setChangeEventsInputFile(null);
			config.vehicles().setVehiclesFile(null);
			config.network().setInputFile(outputDirectory + runId + ".output_network.xml.gz");
		} else {
			config = ConfigUtils.loadConfig(outputDirectory + "output_config.xml");
			config.plans().setInputFile(null);
			config.plans().setInputPersonAttributeFile(null);
			config.network().setChangeEventsInputFile(null);
			config.vehicles().setVehiclesFile(null);
			config.network().setInputFile(outputDirectory + "output_network.xml.gz");
		}
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		EventsManager events = EventsUtils.createEventsManager();
				
		LinkDemandEventHandler handler = new LinkDemandEventHandler(scenario.getNetwork(), vehicleTypePrefix);
		events.addHandler(handler);
		
		String eventsFile;
		String analysis_output_file;
		if (runId != null) {
			eventsFile = outputDirectory + "ITERS/it." + config.controler().getLastIteration() + "/" + runId + "." + config.controler().getLastIteration() + ".events.xml.gz";
			analysis_output_file = outputDirectory + "ITERS/it." + config.controler().getLastIteration() + "/" + runId + ".link_dailyDemand_vehicleType.csv";
		} else {
			eventsFile = outputDirectory + "ITERS/it." + config.controler().getLastIteration() + "/" + config.controler().getLastIteration() + ".events.xml.gz";
			analysis_output_file = outputDirectory + "ITERS/it." + config.controler().getLastIteration() + "/"  + "link_dailyDemand_" + this.vehicleTypePrefix + ".csv";
		}
		
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
		
		handler.printResults(analysis_output_file);
	}
			 
}
		

