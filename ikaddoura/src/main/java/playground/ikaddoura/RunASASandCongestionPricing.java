/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.ikaddoura;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.decongestion.DecongestionConfigGroup;
import org.matsim.contrib.decongestion.DecongestionModule;
import org.matsim.contrib.decongestion.routing.TollTimeDistanceTravelDisutilityFactory;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import playground.ikaddoura.agentSpecificActivityScheduling.AgentSpecificActivitySchedulingConfigGroup;
import playground.ikaddoura.agentSpecificActivityScheduling.AgentSpecificActivitySchedulingModule;

/**
* @author ikaddoura
*/

public class RunASASandCongestionPricing {

	private static final Logger log = Logger.getLogger(RunASASandCongestionPricing.class);

	private static String configFile;
	private static String outputDirectory = "";
	private static String runId = "";
		
	private static boolean otfvis;
	
	public static void main(String[] args) {
		if (args.length > 0) {
			
			configFile = args[0];		
			log.info("configFile: "+ configFile);
			
			outputDirectory = args[1];
			log.info("outputDirectory: "+ outputDirectory);
			
			runId = args[2];
			log.info("runId: "+ runId);
			
			otfvis = false;
			
		} else {
			configFile = "/Users/ihab/Documents/workspace/runs-svn/optAV/input/config-from-server_1agent.xml";
			outputDirectory = "/Users/ihab/Documents/workspace/runs-svn/optAV/output/optAV_config-from-server_1agent_test1/";
			runId = null;
			otfvis = false;
		}
		
		RunASASandCongestionPricing runBerlinOptAV = new RunASASandCongestionPricing();
		runBerlinOptAV.run();
	}

	private void run() {
		
		Config config = ConfigUtils.loadConfig(
				configFile,
				new OTFVisConfigGroup(),
				new DecongestionConfigGroup(),
				new AgentSpecificActivitySchedulingConfigGroup()
				);
				
		if (outputDirectory.isEmpty()) {
			log.info("Using output directory provided in config file.");
		} else {
			config.controler().setOutputDirectory(outputDirectory);
		}
		
		if (runId.isEmpty()) {
			log.info("Using runId provided in config file.");
		} else {
			config.controler().setRunId(runId);
		}
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		
		// #############################
        // activity fares
		// #############################
		
		controler.addOverridingModule(new AgentSpecificActivitySchedulingModule(scenario));
		
		// #############################
		// decongestion
		// #############################
		
		controler.addOverridingModule(new DecongestionModule(scenario));
		final TollTimeDistanceTravelDisutilityFactory travelDisutilityFactory = new TollTimeDistanceTravelDisutilityFactory();
		travelDisutilityFactory.setSigma(0.);
		
		controler.addOverridingModule(new AbstractModule(){
			@Override
			public void install() {
				this.bindCarTravelDisutilityFactory().toInstance( travelDisutilityFactory );
			}
		});

		// #############################
		// otfvis
		// #############################
		
		if (otfvis) controler.addOverridingModule(new OTFVisLiveModule());
		
		// #############################
		// run
		// #############################
		
		controler.run();
	}
}

