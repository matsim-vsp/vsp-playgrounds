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

package playground.ikaddoura.optAV;

import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.av.robotaxi.scoring.TaxiFareConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.contrib.noise.utils.MergeNoiseCSVFile;
import org.matsim.contrib.noise.utils.ProcessNoiseImmissions;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.run.TaxiModule;
import org.matsim.contrib.taxi.run.TaxiOutputModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl.Builder;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.SubtourModeChoice;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import playground.ikaddoura.agentSpecificActivityScheduling.AgentSpecificActivitySchedulingConfigGroup;
import playground.ikaddoura.agentSpecificActivityScheduling.AgentSpecificActivitySchedulingModule;
import playground.ikaddoura.decongestion.DecongestionConfigGroup;

/**
* @author ikaddoura
*/

public class RunExampleOptAV2 {

	private static final Logger log = Logger.getLogger(RunExampleOptAV2.class);

	private static String configFile;
	private static String outputDirectory;
	private static String runId;
	private static Boolean allowPotentialSAVusersToSwitchToTaxiMode;
		
	private static boolean otfvis;
	
	public static void main(String[] args) {
		if (args.length > 0) {
			
			configFile = args[0];		
			log.info("configFile: "+ configFile);
			
			outputDirectory = args[1];
			log.info("outputDirectory: "+ outputDirectory);
			
			runId = args[2];
			log.info("runId: "+ runId);
			
			allowPotentialSAVusersToSwitchToTaxiMode = Boolean.parseBoolean(args[3]);
			log.info("allowPotentialSAVusersToSwitchToTaxiMode: "+ allowPotentialSAVusersToSwitchToTaxiMode);		
			
			otfvis = false;
			
		} else {
			configFile = "/Users/ihab/Documents/workspace/runs-svn/optAV/input/config_test.xml";
			outputDirectory = "/Users/ihab/Documents/workspace/runs-svn/optAV/output/optAV_test_1agent/";
			runId = null;
			allowPotentialSAVusersToSwitchToTaxiMode = true;
			otfvis = false;
		}
		
		RunExampleOptAV2 runBerlinOptAV = new RunExampleOptAV2();
		runBerlinOptAV.run();
	}

	private void run() {
		
		Config config = ConfigUtils.loadConfig(
				configFile,
				new OptAVConfigGroup(),
				new TaxiConfigGroup(),
				new DvrpConfigGroup(),
				new TaxiFareConfigGroup(),
				new OTFVisConfigGroup(),
				new NoiseConfigGroup(),
				new DecongestionConfigGroup(),
				new AgentSpecificActivitySchedulingConfigGroup()
				);
		
//		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		
		config.controler().setOutputDirectory(outputDirectory);
		config.controler().setRunId(runId);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		
		controler.addOverridingModule(new TaxiOutputModule());
		controler.addOverridingModule(new TaxiModule());
		controler.addOverridingModule(new OptAVModule(scenario));
		
		controler.addOverridingModule(new AgentSpecificActivitySchedulingModule(scenario));
		
		final String[] modesWithoutTaxi = {"car", "bicycle", "pt", "ptSlow", "walk"};
		final String[] modesWithTaxi = {"car", "bicycle", "pt", "ptSlow", "walk", "taxi"};
		final String[] chainBasedModes = {"car", "bicycle"};

		controler.addOverridingModule(new AbstractModule() {
							
			@Override
			public void install() {
				final Provider<TripRouter> tripRouterProvider = binder().getProvider(TripRouter.class);
				
				final String[] availableModesForPotentialSAVusers;
				
				if (allowPotentialSAVusersToSwitchToTaxiMode) {
					availableModesForPotentialSAVusers = modesWithTaxi;
				} else {
					availableModesForPotentialSAVusers = modesWithoutTaxi;
				}
				
				String subpopPotentialSAVuser = "potentialSAVuser";
				addPlanStrategyBinding("SubtourModeChoice_" + subpopPotentialSAVuser).toProvider(new javax.inject.Provider<PlanStrategy>() {
					
					final String[] availableModes = availableModesForPotentialSAVusers;
					
					@Inject
					Scenario sc;

					@Override
					public PlanStrategy get() {
						
						log.info("SubtourModeChoice_" + subpopPotentialSAVuser + " - available modes: " + availableModes.toString());

						final Builder builder = new Builder(new RandomPlanSelector<>());
						builder.addStrategyModule(new SubtourModeChoice(sc.getConfig()
								.global()
								.getNumberOfThreads(), availableModes, chainBasedModes, false, tripRouterProvider));
						builder.addStrategyModule(new ReRoute(sc, tripRouterProvider));
						return builder.build();
					}
				});
				
				String subpopNoPotentialSAVuser = "noPotentialSAVuser";
				addPlanStrategyBinding("SubtourModeChoice_" + subpopNoPotentialSAVuser).toProvider(new javax.inject.Provider<PlanStrategy>() {
					final String[] availableModes = modesWithoutTaxi;
					
					@Inject
					Scenario sc;

					@Override
					public PlanStrategy get() {
						
						log.info("SubtourModeChoice_" + subpopNoPotentialSAVuser + " - available modes: " + availableModes.toString());
						
						final Builder builder = new Builder(new RandomPlanSelector<>());
						builder.addStrategyModule(new SubtourModeChoice(sc.getConfig()
								.global()
								.getNumberOfThreads(), availableModes, chainBasedModes, false, tripRouterProvider));
						builder.addStrategyModule(new ReRoute(sc, tripRouterProvider));
						return builder.build();
					}
				});
			}
		});

		// #############################
		// run
		// #############################
				
		if (otfvis) controler.addOverridingModule(new OTFVisLiveModule());	
		controler.run();
		
		// #############################
		// post processing
		// #############################
			
		String immissionsDir = controler.getConfig().controler().getOutputDirectory() + "/ITERS/it." + controler.getConfig().controler().getLastIteration() + "/immissions/";
		String receiverPointsFile = controler.getConfig().controler().getOutputDirectory() + "/receiverPoints/receiverPoints.csv";
			
		NoiseConfigGroup noiseParams = ConfigUtils.addOrGetModule(config, NoiseConfigGroup.class);

		ProcessNoiseImmissions processNoiseImmissions = new ProcessNoiseImmissions(immissionsDir, receiverPointsFile, noiseParams.getReceiverPointGap());
		processNoiseImmissions.run();
			
		final String[] labels = { "immission", "consideredAgentUnits" , "damages_receiverPoint" };
		final String[] workingDirectories = { controler.getConfig().controler().getOutputDirectory() + "/ITERS/it." + controler.getConfig().controler().getLastIteration() + "/immissions/" , controler.getConfig().controler().getOutputDirectory() + "/ITERS/it." + controler.getConfig().controler().getLastIteration() + "/consideredAgentUnits/" , controler.getConfig().controler().getOutputDirectory() + "/ITERS/it." + controler.getConfig().controler().getLastIteration()  + "/damages_receiverPoint/" };
	
		MergeNoiseCSVFile merger = new MergeNoiseCSVFile() ;
		merger.setReceiverPointsFile(receiverPointsFile);
		merger.setOutputDirectory(controler.getConfig().controler().getOutputDirectory() + "/ITERS/it." + controler.getConfig().controler().getLastIteration() + "/");
		merger.setTimeBinSize(noiseParams.getTimeBinSizeNoiseComputation());
		merger.setWorkingDirectory(workingDirectories);
		merger.setLabel(labels);
		merger.run();	
	}
}

