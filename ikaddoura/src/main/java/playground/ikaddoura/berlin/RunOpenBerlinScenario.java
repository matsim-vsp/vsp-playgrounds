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

package playground.ikaddoura.berlin;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import playground.ikaddoura.analysis.IKAnalysisRun;
import playground.ikaddoura.analysis.modalSplitUserType.AgentAnalysisFilter;
import playground.ikaddoura.durationBasedTimeAllocationMutator.DurationBasedTimeAllocationPlanStrategyProvider;

/**
* @author ikaddoura
*/

public class RunOpenBerlinScenario {

	private static final Logger log = Logger.getLogger(RunOpenBerlinScenario.class);

	private static String configFile;
	private static String outputDirectory;
	private static String runId;
	private static String visualizationScriptInputDirectory;
	
	private static boolean useCarTravelTimeForRide;
	private static boolean useSBBptRouter;
	private static boolean useDurationBasedTimeAllocationMutator;
	private static double probaForRandomSingleTripMode;
	
	public static void main(String[] args) {
		if (args.length > 0) {
			
			configFile = args[0];		
			log.info("configFile: "+ configFile);
			
			outputDirectory = args[1];
			log.info("outputDirectory: "+ outputDirectory);
			
			runId = args[2];
			log.info("runId: "+ runId);
			
			visualizationScriptInputDirectory = args[3];
			log.info("visualizationScriptInputDirectory: "+ visualizationScriptInputDirectory);
			
			useCarTravelTimeForRide = Boolean.parseBoolean(args[4]);
			log.info("useCarTravelTimeForRide: "+ useCarTravelTimeForRide);
			
			useSBBptRouter = Boolean.parseBoolean(args[5]);
			log.info("useSBBptRouter: "+ useSBBptRouter);
			
			useDurationBasedTimeAllocationMutator = Boolean.parseBoolean(args[6]);
			log.info("useDurationBasedTimeAllocationMutator: "+ useDurationBasedTimeAllocationMutator);
			
			probaForRandomSingleTripMode = Double.parseDouble(args[7]);
			log.info("probaForRandomSingleTripMode: "+ probaForRandomSingleTripMode);

		} else {
			
			configFile = "/Users/ihab/Desktop/ils4a/ziemke/open_berlin_scenario/input/be_3_ik/config_be_300_mode-choice_test.xml";
			outputDirectory = "/Users/ihab/Documents/workspace/runs-svn/open_berlin_scenario/be_300_test_7/";
			runId = "test-run";
		}
		
		RunOpenBerlinScenario runner = new RunOpenBerlinScenario();
		runner.run();
	}

	public void run() {
		
		Config config = ConfigUtils.loadConfig(configFile);
		
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.failIfDirectoryExists);
		config.controler().setOutputDirectory(outputDirectory);
		config.controler().setRunId(runId);
		
		final String STRATEGY_NAME = "durationBasedTimeMutator";

		if (useDurationBasedTimeAllocationMutator) {
			// add own time allocation mutator strategy to config
			StrategySettings stratSets = new StrategySettings();
			stratSets.setStrategyName(STRATEGY_NAME);
			stratSets.setWeight(0.1);
			stratSets.setSubpopulation("person");
			config.strategy().addStrategySettings(stratSets);
		}
		
		config.subtourModeChoice().setProbaForRandomSingleTripMode(probaForRandomSingleTripMode);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		
		if (useDurationBasedTimeAllocationMutator) {
			// add own time allocation mutator strategy to controler
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					addPlanStrategyBinding(STRATEGY_NAME).toProvider(DurationBasedTimeAllocationPlanStrategyProvider.class);
				}
			});
		}
		
		if (useSBBptRouter) {
			// use the sbb pt raptor router
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					install(new SwissRailRaptorModule());
				}
			});
		}
		
		if (useCarTravelTimeForRide) {
			// use the (congested) car travel time for the teleported ride mode
			controler.addOverridingModule(new AbstractModule(){
				@Override
				public void install() {
					addTravelTimeBinding(TransportMode.ride).to(networkTravelTime());
					addTravelDisutilityFactoryBinding(TransportMode.ride).to(carTravelDisutilityFactoryKey());        }
		    });
		}
				
		controler.run();
		
		log.info("Running analysis...");
				
		final String scenarioCRS = TransformationFactory.DHDN_GK4;	
		final String shapeFileZones = null;
		final String zonesCRS = null;
		final String homeActivity = "home";
		final int scalingFactor = 10;
		
		List<AgentAnalysisFilter> filters = new ArrayList<>();

		AgentAnalysisFilter filter1 = new AgentAnalysisFilter(scenario);
		filter1.setSubpopulation("person");
		filter1.setPersonAttribute("berlin");
		filter1.setPersonAttributeName("home-activity-zone");
		filter1.preProcess(scenario);
		filters.add(filter1);
		
		AgentAnalysisFilter filter2 = new AgentAnalysisFilter(scenario);
		filter2.preProcess(scenario);
		filters.add(filter2);
		
		AgentAnalysisFilter filter3 = new AgentAnalysisFilter(scenario);
		filter3.setSubpopulation("person");
		filter3.setPersonAttribute("brandenburg");
		filter3.setPersonAttributeName("home-activity-zone");
		filter3.preProcess(scenario);
		filters.add(filter3);

		IKAnalysisRun analysis = new IKAnalysisRun(
				scenario,
				null,
				visualizationScriptInputDirectory,
				scenarioCRS,
				shapeFileZones,
				zonesCRS,
				homeActivity,
				scalingFactor,
				filters,
				null);
		analysis.run();
	
		log.info("Done.");
		
	}

}

