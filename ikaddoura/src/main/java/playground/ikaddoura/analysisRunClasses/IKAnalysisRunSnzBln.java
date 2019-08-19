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
import org.matsim.analysis.MatsimAnalysis;
import org.matsim.analysis.modalSplitUserType.AgentAnalysisFilter;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.scenario.ScenarioUtils;

public class IKAnalysisRunSnzBln {
	private static final Logger log = Logger.getLogger(IKAnalysisRunSnzBln.class);
			
	public static void main(String[] args) throws IOException {
			
		String runDirectory = null;
		String runId = null;
		String runDirectoryToCompareWith = null;
		String runIdToCompareWith = null;
		String visualizationScriptInputDirectory = null;
		String scenarioCRS = null;	
		String shapeFileZones = null;
		String zonesCRS = null;
		String zoneFile = null;
		String homeActivityPrefix = null;
		int scalingFactor;
		String modesString = null;
		String analyzeSubpopulation = null;
		
		final String[] helpLegModes = {TransportMode.transit_walk, TransportMode.access_walk, TransportMode.egress_walk, TransportMode.non_network_walk};
		final String stageActivitySubString = "interaction";
		final StageActivityTypes stageActivities = new StageActivityTypesImpl("pt interaction", "car interaction", "ride interaction", "bike interaction", "bicycle interaction", "drt interaction");
		final String zoneId = "NO";
		
		if (args.length > 0) {

			runDirectory = args[0];
			runId = args[1];
			
			runDirectoryToCompareWith = args[2];
			runIdToCompareWith = args[3];
			
			visualizationScriptInputDirectory = args[4];
			
			scenarioCRS = args[5];
			
			shapeFileZones = args[6];
			zonesCRS = args[7];
			
			zoneFile = args[8];

			homeActivityPrefix = "home";
			scalingFactor = 4;
			
			modesString = TransportMode.car + "," + TransportMode.pt + "," + TransportMode.bike + "," + TransportMode.walk + "," + TransportMode.ride + "," + TransportMode.drt;
			
			analyzeSubpopulation = null;
			
		} else {
			
			runDirectory = "../../runs-svn/avoev/2019-05/output_2019-05-08_snz-bc-0/";
			runId = "snz-bc-0";		
			
			runDirectoryToCompareWith = null;
			runIdToCompareWith = null;
			
			visualizationScriptInputDirectory = "./visualization-scripts/";
			
			scenarioCRS = "EPSG:25832";
			
			shapeFileZones = "../../shared-svn/projects/avoev/data/berlkoenig-od-trips/Bezirksregionen_zone_UTM32N/Bezirksregionen_zone_UTM32N_fixed.SHP";
			zonesCRS = "EPSG:25832";
			
			zoneFile = "../../shared-svn/projects/avoev/data/berlin-area/berlin-area_EPSG25832.shp";

			homeActivityPrefix = "home";
			scalingFactor = 4;
			
			modesString = TransportMode.car + "," + TransportMode.pt + "," + TransportMode.bike + "," + TransportMode.walk + "," + TransportMode.ride + "," + TransportMode.drt;
			
			analyzeSubpopulation = null;
		}
		
		Scenario scenario1 = loadScenario(runDirectory, runId);
		Scenario scenario0 = loadScenario(runDirectoryToCompareWith, runIdToCompareWith);
		
		List<AgentAnalysisFilter> filter1 = new ArrayList<>();
		
		AgentAnalysisFilter filter1a = new AgentAnalysisFilter();
		filter1a.preProcess(scenario1);
		filter1.add(filter1a);
		
		AgentAnalysisFilter filter1b = new AgentAnalysisFilter();
		filter1b.setZoneFile(zoneFile);
		filter1b.setRelevantActivityType(homeActivityPrefix);
		filter1b.preProcess(scenario1);
		filter1.add(filter1b);
		
		List<AgentAnalysisFilter> filter0 = new ArrayList<>();
				
		AgentAnalysisFilter filter0a = new AgentAnalysisFilter();
		filter0a.preProcess(scenario0);
		filter0.add(filter0a);
		
		AgentAnalysisFilter filter0b = new AgentAnalysisFilter();
		filter0b.setZoneFile(zoneFile);
		filter0b.setRelevantActivityType(homeActivityPrefix);
		filter0b.preProcess(scenario0);
		filter0.add(filter0b);
		
		List<String> modes = new ArrayList<>();
		for (String mode : modesString.split(",")) {
			modes.add(mode);
		}

		MatsimAnalysis analysis = new MatsimAnalysis(
				scenario1,
				scenario0,
				visualizationScriptInputDirectory,
				scenarioCRS,
				shapeFileZones,
				zonesCRS,
				homeActivityPrefix,
				scalingFactor,
				filter1,
				filter0,
				modes,
				analyzeSubpopulation,
				zoneId, helpLegModes, stageActivitySubString, stageActivities
				);
		analysis.run();
	}
	
	private static Scenario loadScenario(String runDirectory, String runId) {
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
		String configFile;
		String personAttributesFile;
		
		if (new File(runDirectory + runId + ".output_config.xml").exists()) {
			
			configFile = runDirectory + runId + ".output_config.xml";	
			networkFile = runId + ".output_network.xml.gz";
			populationFile = runId + ".output_plans.xml.gz";
			personAttributesFile = runId + ".output_personAttributes.xml.gz";
			
		} else {
			
			configFile = runDirectory + "output_config.xml";	
			networkFile = "output_network.xml.gz";
			populationFile = "output_plans.xml.gz";
			personAttributesFile = "output_personAttributes.xml.gz";

		}

		Config config = ConfigUtils.loadConfig(configFile);

		if (config.controler().getRunId() != null) {
			if (!runId.equals(config.controler().getRunId())) throw new RuntimeException("Given run ID " + runId + " doesn't match the run ID given in the config file. Aborting...");
		} else {
			config.controler().setRunId(runId);
		}

		config.controler().setOutputDirectory(runDirectory);
		config.plans().setInputFile(populationFile);
		config.network().setInputFile(networkFile);
		config.vehicles().setVehiclesFile(null);
		config.transit().setTransitScheduleFile(null);
		config.transit().setVehiclesFile(null);
		config.facilities().setInputFile(null);
		config.plans().setInputPersonAttributeFile(personAttributesFile);
		
		return ScenarioUtils.loadScenario(config);
	}

}
		

