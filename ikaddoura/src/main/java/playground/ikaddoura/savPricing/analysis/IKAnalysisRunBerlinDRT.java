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

package playground.ikaddoura.savPricing.analysis;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import playground.ikaddoura.analysis.IKAnalysisRun;
import playground.ikaddoura.analysis.modalSplitUserType.AgentAnalysisFilter;


public class IKAnalysisRunBerlinDRT {
	private static final Logger log = Logger.getLogger(IKAnalysisRunBerlinDRT.class);
			
	public static void main(String[] args) throws IOException {
		
		String runId;
		String runDirectory;
		
		String runIdBaseCase;
		String runDirectoryBaseCase;
		
		String shapeFileZones;
		
		String visualizationScriptInputDirectory;
		
		if (args.length > 0) {
			
			runId = args[0];
			runDirectory = args[1];
			
			runIdBaseCase = args[2];
			runDirectoryBaseCase = args[3];
			
			shapeFileZones = args[4];
			visualizationScriptInputDirectory = args[5];
			
		} else {
			
			runId = "berlin-drtA-v5.2-1pct-Berlkoenig";
			runDirectory = "/Users/ihab/Documents/workspace/public-svn/matsim/scenarios/countries/de/berlin/projects/avoev/berlin-sav-v5.2-1pct/berlin-drtA-v5.2-1pct-Berlkoenig/";
			
			runIdBaseCase = "berlin-v5.2-1pct";
			runDirectoryBaseCase = "/Users/ihab/Documents/workspace/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.2-1pct/output-berlin-v5.2-1pct/";

			shapeFileZones = "/Users/ihab/Documents/workspace/shared-svn/studies/ihab/berlin/shapeFiles/berlin_grid_2500/berlin_grid_2500.shp";
			visualizationScriptInputDirectory = "./visualization-scripts/";
		}
			
		final String scenarioCRS = TransformationFactory.DHDN_GK4;
		final String zonesCRS = TransformationFactory.DHDN_GK4;
		final String homeActivityPrefix = "home";
		final int scalingFactor = 10;
		final String taxiMode = TransportMode.drt;
		final String carMode = TransportMode.car;
		final double rewardSAVformerCarUser = 0.;
		
		// optional: person attributes file to replace the output person attributes file
		final String personAttributesFile = null;

		Scenario scenario1 = loadScenario(runDirectory, runId, personAttributesFile);
		Scenario scenario0 = loadScenario(runDirectoryBaseCase, runIdBaseCase, personAttributesFile);
		
		List<AgentAnalysisFilter> filters1 = new ArrayList<>();

		AgentAnalysisFilter filter1a = new AgentAnalysisFilter(scenario1);
		filter1a.setPersonAttribute("berlin");
		filter1a.setPersonAttributeName("home-activity-zone");
		filter1a.preProcess(scenario1);
		filters1.add(filter1a);
		
		AgentAnalysisFilter filter1b = new AgentAnalysisFilter(scenario1);
		filter1b.preProcess(scenario1);
		filters1.add(filter1b);
		
		AgentAnalysisFilter filter1c = new AgentAnalysisFilter(scenario1);
		filter1c.setPersonAttribute("brandenburg");
		filter1c.setPersonAttributeName("home-activity-zone");
		filter1c.preProcess(scenario1);
		filters1.add(filter1c);
		
		List<AgentAnalysisFilter> filters0 = new ArrayList<>();

		AgentAnalysisFilter filter0a = new AgentAnalysisFilter(scenario0);
		filter0a.setPersonAttribute("berlin");
		filter0a.setPersonAttributeName("home-activity-zone");
		filter0a.preProcess(scenario0);
		filters0.add(filter0a);
		
		AgentAnalysisFilter filter0b = new AgentAnalysisFilter(scenario0);
		filter0b.preProcess(scenario0);
		filters0.add(filter0b);
		
		AgentAnalysisFilter filter0c = new AgentAnalysisFilter(scenario0);
		filter0c.setPersonAttribute("brandenburg");
		filter0c.setPersonAttributeName("home-activity-zone");
		filter0c.preProcess(scenario0);
		filters0.add(filter0c);
		
		List<String> modes = new ArrayList<>();
		modes.add(TransportMode.car);
		modes.add(TransportMode.drt);
		modes.add(TransportMode.pt);
		modes.add(TransportMode.walk);
		modes.add(TransportMode.transit_walk);
		modes.add("bicycle");
		modes.add(TransportMode.ride);

		IKAnalysisRun analysis = new IKAnalysisRun(
				scenario1,
				scenario0,
				visualizationScriptInputDirectory,
				scenarioCRS,
				shapeFileZones,
				zonesCRS,
				homeActivityPrefix,
				scalingFactor,
				filters1,
				filters0,
				modes,
				taxiMode,
				carMode,
				rewardSAVformerCarUser, null);
		analysis.run();
	
		log.info("Done.");
	}
	
	private static Scenario loadScenario(String runDirectory, String runId, String personAttributesFileToReplaceOutputFile) {
		
		if (runDirectory == null) {
			return null;
			
		} else {
			
			if (!runDirectory.endsWith("/")) runDirectory = runDirectory + "/";

			String networkFile;
			String populationFile;
			String personAttributesFile;
			String configFile = runDirectory + runId + ".output_config.xml";
			log.info("Setting config file to " + configFile);
			
			if (new File(configFile).exists()) {
				
				networkFile = runDirectory + runId + ".output_network.xml.gz";
				populationFile = runDirectory + runId + ".output_plans.xml.gz";
				configFile = runDirectory + runId + ".output_config.xml";
				
				if (personAttributesFileToReplaceOutputFile == null) {
					personAttributesFile = runDirectory + runId + ".output_personAttributes.xml.gz";
				} else {
					personAttributesFile = personAttributesFileToReplaceOutputFile;
				}
				
			} else {

				configFile = runDirectory + "output_config.xml";
				log.info("Setting config file to " + configFile);
				
				networkFile = runDirectory + "output_network.xml.gz";
				populationFile = runDirectory + "output_plans.xml.gz";
				
				log.info("Trying to load config file " + configFile);
				
				if (personAttributesFileToReplaceOutputFile == null) {
					personAttributesFile = runDirectory + "output_personAttributes.xml.gz";
				} else {
					personAttributesFile = personAttributesFileToReplaceOutputFile;
				}
			}

			Config config = ConfigUtils.loadConfig(configFile);
			
			log.info("Setting run directory to " + runDirectory);
			config.controler().setOutputDirectory(runDirectory);

			if (config.controler().getRunId() != null) {
				if (!runId.equals(config.controler().getRunId())) throw new RuntimeException("Given run ID " + runId + " doesn't match the run ID given in the config file. Aborting...");
			} else {
				log.info("Setting run Id to " + runId);
				config.controler().setRunId(runId);
			}

			config.plans().setInputFile(populationFile);
			config.plans().setInputPersonAttributeFile(personAttributesFile);
			config.network().setInputFile(networkFile);
			config.vehicles().setVehiclesFile(null);
			config.transit().setTransitScheduleFile(null);
			config.transit().setVehiclesFile(null);
			
			return ScenarioUtils.loadScenario(config);
		}
	}
}
		

