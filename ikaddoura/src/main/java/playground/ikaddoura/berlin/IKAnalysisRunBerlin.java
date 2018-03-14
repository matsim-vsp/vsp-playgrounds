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

package playground.ikaddoura.berlin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import playground.ikaddoura.analysis.IKAnalysisRun;
import playground.ikaddoura.analysis.modalSplitUserType.AgentAnalysisFilter;


public class IKAnalysisRunBerlin {
	private static final Logger log = Logger.getLogger(IKAnalysisRunBerlin.class);
			
	public static void main(String[] args) throws IOException {
			
		final String runId = "be300mt_47";
		final String runDirectory = "/Users/ihab/Documents/workspace/runs-svn/open_berlin_scenario/" + runId + "/";
		final String scenarioCRS = TransformationFactory.DHDN_GK4;	
		final String shapeFileZones = null;
		final String zonesCRS = null;
		final String homeActivity = "home";
		final int scalingFactor = 10;
		final String personAttributesFile = "/Users/ihab/Documents/workspace/shared-svn/studies/countries/de/open_berlin_scenario/be_3/population/personAttributes_300_person_freight_10pct.xml.gz";

		Scenario scenario1 = loadScenario(runDirectory, runId, personAttributesFile);
		
		List<AgentAnalysisFilter> filters1 = new ArrayList<>();

		AgentAnalysisFilter filter1a = new AgentAnalysisFilter(scenario1);
		filter1a.setSubpopulation("person");
		filter1a.setPersonAttribute("berlin");
		filter1a.setPersonAttributeName("home-activity-zone");
		filter1a.preProcess(scenario1);
		filters1.add(filter1a);
		
		AgentAnalysisFilter filter1b = new AgentAnalysisFilter(scenario1);
		filter1b.preProcess(scenario1);
		filters1.add(filter1b);
		
		AgentAnalysisFilter filter1c = new AgentAnalysisFilter(scenario1);
		filter1c.setSubpopulation("person");
		filter1c.setPersonAttribute("brandenburg");
		filter1c.setPersonAttributeName("home-activity-zone");
		filter1c.preProcess(scenario1);
		filter1c.preProcess(scenario1);
		filters1.add(filter1c);

		IKAnalysisRun analysis = new IKAnalysisRun(
				scenario1,
				null,
				scenarioCRS,
				shapeFileZones,
				zonesCRS,
				homeActivity,
				scalingFactor,
				filters1,
				null);
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
			String configFile;
			
			if (new File(runDirectory + runId + ".output_config.xml").exists()) {
				
				networkFile = runDirectory + runId + ".output_network.xml.gz";
				populationFile = runDirectory + runId + ".output_plans.xml.gz";
				configFile = runDirectory + runId + ".output_config.xml";
				
				if (personAttributesFileToReplaceOutputFile == null) {
					personAttributesFile = runDirectory + runId + ".output_personAttributes.xml.gz";
				} else {
					personAttributesFile = personAttributesFileToReplaceOutputFile;
				}
				
			} else {
				
				networkFile = runDirectory + "output_network.xml.gz";
				populationFile = runDirectory + "output_plans.xml.gz";
				configFile = runDirectory + "output_config.xml";
				
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
		

