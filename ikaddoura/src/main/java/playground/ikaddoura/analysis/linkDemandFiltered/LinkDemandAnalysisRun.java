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

package playground.ikaddoura.analysis.linkDemandFiltered;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;

public class LinkDemandAnalysisRun {

	private static String runId = "run0_bc-ohne-RSV";
	private static String OUTPUT_BASE_DIR = "/Users/ihab/Documents/workspace/runs-svn/nemo/wissenschaftsforum2019/"+ runId +"/output/";

	private String outputDirectory;

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
		config = ConfigUtils.createConfig();
		config.network().setInputCRS("EPSG:25832");
		config.global().setCoordinateSystem("EPSG:25832");
		
		if (runId != null) {
			config.network().setInputFile(outputDirectory + runId + ".output_network.xml.gz");
		} else {
			config.network().setInputFile(outputDirectory + "output_network.xml.gz");
		}
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		EventsManager events = EventsUtils.createEventsManager();
				
		Set<String> modesToInclude = new HashSet<>();
		modesToInclude.add("bike");
		ModeFilter filter = new ModeFilter(modesToInclude);
		
		LinkDemandEventHandler handler = new LinkDemandEventHandler(scenario.getNetwork(), filter);
		events.addHandler(handler);
		
		String eventsFile;
		String analysis_output_file;
		if (runId != null) {
			eventsFile = outputDirectory + runId + ".output_events.xml.gz";
			analysis_output_file = outputDirectory + runId + ".link_dailyDemand_" + filter.toFileName() + ".csv";

		} else {
			eventsFile = outputDirectory + "output_events.xml.gz";
			analysis_output_file = outputDirectory  + "link_dailyDemand_" + filter.toFileName() + ".csv";
		}
		
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
		
		handler.printResults(analysis_output_file);
	}
			 
}
		

