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
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import playground.ikaddoura.analysis.modalSplitUserType.AgentAnalysisFilter;

public class IKAnalysisRunRuhr {
	private static final Logger log = Logger.getLogger(IKAnalysisRunRuhr.class);
			
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
		String taxiMode = null;
		String carMode = null;
		double rewardSAVuserFormerCarUser = 0.;
		String analyzeSubpopulation = null;
		
		if (args.length > 0) {
			throw new RuntimeException();
			
		} else {
			
			runDirectory = "/Users/ihab/Desktop/ils3a/kaddoura/output1/";
			runId = "nemo_baseCase_continued_001";		
			runDirectoryToCompareWith = "/Users/ihab/Desktop/ils3a/kaddoura/output0/";
			runIdToCompareWith = "nemo_baseCase_032";
			
			visualizationScriptInputDirectory = "./visualization-scripts/";
			
			scenarioCRS = "EPSG:25832";
			
			shapeFileZones = null;
			zonesCRS = null;
			
			homeActivityPrefix = "home";
			scalingFactor = 100;
			
			modesString = TransportMode.car + "," + TransportMode.pt + "," + TransportMode.bike + "," + TransportMode.walk + "," + TransportMode.ride;
			
			taxiMode = null;
			carMode = null;
			rewardSAVuserFormerCarUser = 0.0;
			
			analyzeSubpopulation = null;
		}
		
		Scenario scenario1 = loadScenario(runDirectory, runId);
		Scenario scenario0 = loadScenario(runDirectoryToCompareWith, runIdToCompareWith);
		
		List<AgentAnalysisFilter> filter1 = new ArrayList<>();
		
		AgentAnalysisFilter filter1b = new AgentAnalysisFilter(scenario1);
		filter1b.preProcess(scenario1);
		filter1.add(filter1b);
		
		List<AgentAnalysisFilter> filter0 = null;
		
		List<String> modes = new ArrayList<>();
		for (String mode : modesString.split(",")) {
			modes.add(mode);
		}

		IKAnalysisRun analysis = new IKAnalysisRun(
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
				taxiMode,
				carMode,
				rewardSAVuserFormerCarUser,
				analyzeSubpopulation);
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
		
		if (new File(runDirectory + runId + ".output_config.xml").exists()) {
			
			configFile = runDirectory + runId + ".output_config.xml";
			
			networkFile = runId + ".output_network.xml.gz";
			populationFile = runId + ".output_plans.xml.gz";
			
		} else {
			
			configFile = runDirectory + "output_config.xml";
			
			networkFile = "output_network.xml.gz";
			populationFile = "output_plans.xml.gz";
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
		
		return ScenarioUtils.loadScenario(config);
	}

}
		

