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
import org.matsim.contrib.decongestion.DecongestionConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.contrib.noise.utils.MergeNoiseCSVFile;
import org.matsim.contrib.noise.utils.ProcessNoiseImmissions;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.run.TaxiModule;
import org.matsim.contrib.taxi.run.examples.TaxiDvrpModules;
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

/**
* @author ikaddoura
*/

public class RunExampleOptAV {

	private static final Logger log = Logger.getLogger(RunExampleOptAV.class);

	private static String configFile;
	private static String outputDirectory;
	private static String runId;
		
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
			
			configFile = "/Users/ihab/Documents/workspace/runs-svn/optAV/input/config-from-server3.xml";
			outputDirectory = "/Users/ihab/Documents/workspace/runs-svn/optAV/output/optAV_config-from-server3/";
			
			runId = null;
			otfvis = false;
		}
		
		RunExampleOptAV runBerlinOptAV = new RunExampleOptAV();
		runBerlinOptAV.run(configFile, outputDirectory, runId);
	}

	public void run(String configFile, String outputDirectory, String runId) {
		
		// #############################
		// prepare controler
		// #############################
		
		Controler controler = prepareControler(configFile, outputDirectory, runId);

		// #############################
		// run
		// #############################
				
		if (otfvis) controler.addOverridingModule(new OTFVisLiveModule());	
		controler.run();
		
		// #############################
		// post processing
		// #############################
		
		OptAVConfigGroup optAVParams = ConfigUtils.addOrGetModule(controler.getConfig(), OptAVConfigGroup.class);
		if (optAVParams.isAccountForNoise()) {
			String immissionsDir = controler.getConfig().controler().getOutputDirectory() + "/ITERS/it." + controler.getConfig().controler().getLastIteration() + "/immissions/";
			String receiverPointsFile = controler.getConfig().controler().getOutputDirectory() + "/receiverPoints/receiverPoints.csv";
				
			NoiseConfigGroup noiseParams = ConfigUtils.addOrGetModule(controler.getConfig(), NoiseConfigGroup.class);

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

	public Controler prepareControler(String configFile, String outputDirectory, String runId) {
		
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
		
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		
		config.controler().setOutputDirectory(outputDirectory);
		config.controler().setRunId(runId);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		
		controler.addOverridingModule(TaxiDvrpModules.create());
		controler.addOverridingModule(new TaxiModule());
		controler.addOverridingModule(new OptAVModule(scenario));
		
		controler.addOverridingModule(new AgentSpecificActivitySchedulingModule(scenario));
		
		controler.addOverridingModule(new AbstractModule() {
							
			@Override
			public void install() {
				
				final Provider<TripRouter> tripRouterProvider = binder().getProvider(TripRouter.class);
				
				String subpopulation = "noPotentialSAVuser";
				addPlanStrategyBinding("SubtourModeChoice_" + subpopulation).toProvider(new javax.inject.Provider<PlanStrategy>() {
					
					final String[] availableModes = {"car", "bicycle", "pt", "ptSlow", "walk"};
					
					@Inject
					Scenario sc;

					@Override
					public PlanStrategy get() {
						
						log.info("SubtourModeChoice_" + subpopulation + " - available modes: " + availableModes.toString());
						final String[] chainBasedModes = {"car", "bicycle"};

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
		
		return controler;
	}
}

