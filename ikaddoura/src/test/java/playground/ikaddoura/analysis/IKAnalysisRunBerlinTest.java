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
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import playground.ikaddoura.analysis.modalSplitUserType.AgentAnalysisFilter;

public class IKAnalysisRunBerlinTest {
	private static final Logger log = Logger.getLogger(IKAnalysisRunBerlinTest.class);
	
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();

	@Test
	public final void test1() {
		
		{
			Config config = ConfigUtils.loadConfig(testUtils.getPackageInputDirectory() + "ik-analysis/config.xml");
			config.controler().setOutputDirectory(testUtils.getOutputDirectory() + "output1");
			config.controler().setRunId("run1");
			config.strategy().setFractionOfIterationsToDisableInnovation(1.0);
			config.controler().setLastIteration(1);
			Scenario scenario = ScenarioUtils.loadScenario(config) ;
			Controler controler = new Controler( scenario ) ;
			controler.run();
		}
		
		{
			Config config = ConfigUtils.loadConfig(testUtils.getPackageInputDirectory() + "ik-analysis/config.xml");
			config.controler().setOutputDirectory(testUtils.getOutputDirectory() + "output0");
			config.controler().setRunId("run0");
			config.controler().setLastIteration(0);
			Scenario scenario = ScenarioUtils.loadScenario(config) ;
			Controler controler = new Controler( scenario ) ;
			controler.run();
		}
		
		final String runId;
		final String runDirectory;
		final String runIdBaseCase;
		final String runDirectoryBaseCase;
		final String shapeFileZones;
		final String visualizationScriptInputDirectory;
		
		runId = "run1";
		runDirectory = testUtils.getOutputDirectory() +  "output1/";
		
		runIdBaseCase = "run0";
		runDirectoryBaseCase = testUtils.getOutputDirectory() + "output0/";

		shapeFileZones = null;
		visualizationScriptInputDirectory = "./visualization-scripts/";
			
		final String scenarioCRS = null;
		final String zonesCRS = null;
		final String homeActivityPrefix = "h";
		final int scalingFactor = 100;
		
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
		
		
		List<AgentAnalysisFilter> filters0 = new ArrayList<>();
		
		AgentAnalysisFilter filter0a = new AgentAnalysisFilter(scenario0);
		filter0a.setPersonAttribute("berlin");
		filter0a.setPersonAttributeName("home-activity-zone");
		filter0a.preProcess(scenario0);
		filters0.add(filter0a);
		
		AgentAnalysisFilter filter0b = new AgentAnalysisFilter(scenario0);
		filter0b.preProcess(scenario0);
		filters0.add(filter0b);
		
		List<String> modes = new ArrayList<>();
		modes.add(TransportMode.car);

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
				modes);
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

				configFile = runDirectory + runId + ".output_config.xml";

//				networkFile = runDirectory + runId + ".output_network.xml.gz";
//				populationFile = runDirectory + runId + ".output_plans.xml.gz";
				
				networkFile = runId + ".output_network.xml.gz";
				populationFile = runId + ".output_plans.xml.gz";
				
				if (personAttributesFileToReplaceOutputFile == null) {
//					personAttributesFile = runDirectory + runId + ".output_personAttributes.xml.gz";
					personAttributesFile = runId + ".output_personAttributes.xml.gz";
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
		

