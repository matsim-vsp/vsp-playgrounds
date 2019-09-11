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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.analysis.AgentAnalysisFilter;
import org.matsim.analysis.MatsimAnalysis;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

public class IKAnalysisRunOpenBerlin {
	private static final Logger log = Logger.getLogger(IKAnalysisRunOpenBerlin.class);
			
	public static void main(String[] args) throws IOException {
			
		String runDirectory = null;
		String runId = null;
		String runDirectoryToCompareWith = null;
		String runIdToCompareWith = null;
		String visualizationScriptInputDirectory = null;
		String scenarioCRS = null;	
		String shapeFileZones = null;
		String zonesCRS = null;
		String homeActivityPrefix = null;
		int scalingFactor;
		String modesString = null;
		String analyzeSubpopulation = null;
		final String[] helpLegModes = {TransportMode.transit_walk, TransportMode.non_network_walk, TransportMode.access_walk, TransportMode.egress_walk};
		final StageActivityTypes stageActivities = new StageActivityTypesImpl("pt interaction", "car interaction", "ride interaction", "bike interaction", "bicycle interaction", "drt interaction");
		final String zoneId = "SCHLUESSEL";
		
		if (args.length > 0) {
			if (!args[0].equals("null")) runDirectory = args[0];
			log.info("Run directory: " + runDirectory);
			
			if (!args[1].equals("null")) runId = args[1];
			log.info("Run Id: " + runDirectory);
			
			if (!args[2].equals("null")) runDirectoryToCompareWith = args[2];
			log.info("Run directory to compare with: " + runDirectoryToCompareWith);
			
			if (!args[3].equals("null")) runIdToCompareWith = args[3];
			log.info("Run Id to compare with: " + runDirectory);
			
			if (!args[4].equals("null")) scenarioCRS = args[4];	
			log.info("Scenario CRS: " + scenarioCRS);
			
			if (!args[5].equals("null")) shapeFileZones = args[5];
			log.info("Shape file zones: " + shapeFileZones);

			if (!args[6].equals("null")) zonesCRS = args[6];
			log.info("Zones CRS: " + zonesCRS);
			
			if (!args[7].equals("null")) homeActivityPrefix = args[7];
			log.info("Home activity prefix: " + homeActivityPrefix);

			scalingFactor = Integer.valueOf(args[8]);
			log.info("Scaling factor: " + scalingFactor);
		
			if (!args[9].equals("null")) visualizationScriptInputDirectory = args[9];
			log.info("Visualization script input directory: " + visualizationScriptInputDirectory);
			
			if (!args[10].equals("null")) modesString = args[10];
			log.info("modes: " + modesString);
			
			analyzeSubpopulation = args[11];
			log.info("analyzeSubpopulation: " + analyzeSubpopulation);
			
		} else {
			
			runDirectory = "/Users/ihab/Documents/workspace/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.2-10pct/output-berlin-v5.2-10pct/";
			runId = "berlin-v5.2-10pct";		
			runDirectoryToCompareWith = null;
			runIdToCompareWith = null;
			
			visualizationScriptInputDirectory = "./visualization-scripts/";
			
			scenarioCRS = TransformationFactory.DHDN_GK4;	
			
			shapeFileZones = "/Users/ihab/Documents/workspace/shared-svn/studies/ihab/berlin/shapeFiles/berlin_grid_2500/berlin_grid_2500.shp";
			zonesCRS = TransformationFactory.DHDN_GK4;
			
			homeActivityPrefix = "home";
			scalingFactor = 10;
			
			modesString = TransportMode.car + "," + TransportMode.pt;
			
			analyzeSubpopulation = null;
		}
		
		Scenario scenario1 = loadScenario(runDirectory, runId, null);
		Scenario scenario0 = loadScenario(runDirectoryToCompareWith, runIdToCompareWith, null);
		
		List<AgentAnalysisFilter> filter1 = new ArrayList<>();

		AgentAnalysisFilter filter1a = new AgentAnalysisFilter();
		filter1a.setSubpopulation("person");
		filter1a.setPersonAttribute("berlin");
		filter1a.setPersonAttributeName("home-activity-zone");
		filter1a.preProcess(scenario1);
		filter1.add(filter1a);
		
		AgentAnalysisFilter filter1b = new AgentAnalysisFilter();
		filter1b.preProcess(scenario1);
		filter1.add(filter1b);
		
		AgentAnalysisFilter filter1c = new AgentAnalysisFilter();
		filter1c.setSubpopulation("person");
		filter1c.setPersonAttribute("brandenburg");
		filter1c.setPersonAttributeName("home-activity-zone");
		filter1c.preProcess(scenario1);
		filter1.add(filter1c);
		
		List<AgentAnalysisFilter> filter0 = null;
		
		List<String> modes = new ArrayList<>();
		for (String mode : modesString.split(",")) {
			modes.add(mode);
		}

		MatsimAnalysis analysis = new MatsimAnalysis(); // TODO: Set parameters via setters.
		analysis.run();
	}
	
	private static Scenario loadScenario(String runDirectory, String runId, String personAttributesFileToReplaceOutputFile) {
		log.info("Loading scenario...");
		
		if (runDirectory == null) {
			return null;	
		}
		
		if (runDirectory.equals("")) {
			return null;	
		}
		
		if (runDirectory.equals("null")) {
			return null;	
		}
		
		if (!runDirectory.endsWith("/")) runDirectory = runDirectory + "/";

		String networkFile;
		String populationFile;
		String personAttributesFile;
		String configFile;
		
		if (new File(runDirectory + runId + ".output_config.xml").exists()) {
			
			configFile = runDirectory + runId + ".output_config.xml";
			
			networkFile = runId + ".output_network.xml.gz";
			populationFile = runId + ".output_plans.xml.gz";
			
			if (personAttributesFileToReplaceOutputFile == null) {
				personAttributesFile = runId + ".output_personAttributes.xml.gz";
			} else {
				personAttributesFile = personAttributesFileToReplaceOutputFile;
			}
			
		} else {
			
			configFile = runDirectory + "output_config.xml";
			
			networkFile = "output_network.xml.gz";
			populationFile = "output_plans.xml.gz";
			
			if (personAttributesFileToReplaceOutputFile == null) {
				personAttributesFile = "output_personAttributes.xml.gz";
			} else {
				personAttributesFile = personAttributesFileToReplaceOutputFile;
			}
		}

		Config config = ConfigUtils.loadConfig(configFile);

		if (config.controler().getRunId() != null) {
			if (!runId.equals(config.controler().getRunId())) throw new RuntimeException("Given run ID " + runId + " doesn't match the run ID given in the config file. Aborting...");
		} else {
			config.controler().setRunId(runId);
		}

		config.controler().setOutputDirectory(runDirectory);
		config.plans().setInputFile(populationFile);
		config.plans().setInputPersonAttributeFile(personAttributesFile);
		config.network().setInputFile(networkFile);
		config.vehicles().setVehiclesFile(null);
		config.transit().setTransitScheduleFile(null);
		config.transit().setVehiclesFile(null);
		
		return ScenarioUtils.loadScenario(config);
	}

}
		

