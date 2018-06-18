/* *********************************************************************** *
 * project: org.matsim.*
 * TestControler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.ikaddoura.decongestion;


import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.decongestion.DecongestionConfigGroup;
import org.matsim.contrib.decongestion.DecongestionConfigGroup.DecongestionApproach;
import org.matsim.contrib.decongestion.DecongestionConfigGroup.IntegralApproach;
import org.matsim.contrib.decongestion.DecongestionModule;
import org.matsim.contrib.decongestion.routing.TollTimeDistanceTravelDisutilityFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;

import playground.ikaddoura.analysis.detailedPersonTripAnalysis.old.PersonTripBasicAnalysisRun;

/**
 * Starts an interval-based decongestion pricing simulation run.
 * 
 * @author ikaddoura
 *
 */
public class DecongestionRun {

	private static final Logger log = Logger.getLogger(DecongestionRun.class);

	private static String configFile;
	private static String outputBaseDirectory;
	
	public static void main(String[] args) throws IOException {
		log.info("Starting simulation run with the following arguments:");
		
		if (args.length > 0) {

			configFile = args[0];		
			log.info("config file: "+ configFile);
			
			outputBaseDirectory = args[1];		
			log.info("output directory: "+ outputBaseDirectory);

		} else {
			configFile = "/Users/ihab/Documents/workspace/runs-svn/vickrey-decongestion/input/config.xml";
			outputBaseDirectory = "/Users/ihab/Documents/workspace/runs-svn/vickrey-decongestion/output-FINAL/";
		}
		
		DecongestionRun main = new DecongestionRun();
		main.run();
		
	}

	private void run() throws IOException {

		final DecongestionConfigGroup decongestionSettings = new DecongestionConfigGroup();
		decongestionSettings.setToleratedAverageDelaySec(30.);
		decongestionSettings.setFractionOfIterationsToEndPriceAdjustment(1.0);
		decongestionSettings.setFractionOfIterationsToStartPriceAdjustment(0.0);
		decongestionSettings.setUpdatePriceInterval(1);
		decongestionSettings.setMsa(false);
		decongestionSettings.setTollBlendFactor(1.0);
		
		decongestionSettings.setDecongestionApproach(DecongestionApproach.P_MC);
//		decongestionSettings.setDecongestionApproach(DecongestionApproach.PID);
		
		decongestionSettings.setKd(0.005);
		decongestionSettings.setKi(0.005);
		decongestionSettings.setKp(0.005);
		decongestionSettings.setIntegralApproach(IntegralApproach.UnusedHeadway);
		decongestionSettings.setIntegralApproachUnusedHeadwayFactor(10.0);
		decongestionSettings.setIntegralApproachAverageAlpha(0.0);
		
		decongestionSettings.setTollAdjustment(0.0);
		decongestionSettings.setInitialToll(0.0);
		
		Config config = ConfigUtils.loadConfig(configFile);
		config.addModule(decongestionSettings);
		
		double weight = Double.NEGATIVE_INFINITY;
		for (StrategySettings settings : config.strategy().getStrategySettings()) {
			if (settings.getStrategyName().equals("TimeAllocationMutator")) {
				weight = settings.getWeight();
			}
		}
		
		String outputDirectory = outputBaseDirectory +
				"iter" + config.controler().getLastIteration() +
				"_plans" + config.strategy().getMaxAgentPlanMemorySize() +
				"_scoreMSA" + config.planCalcScore().getFractionOfIterationsToStartScoreMSA() +
				"_disInnovStrat" + config.strategy().getFractionOfIterationsToDisableInnovation() +
				"_timeBin" + config.travelTimeCalculator().getTraveltimeBinSize() +
				"_brain" + config.planCalcScore().getBrainExpBeta() +
				"_timeWeight" + weight +
				"_timeRange" + config.timeAllocationMutator().getMutationRange();
		
		outputDirectory = outputDirectory
					+ "_priceUpdate" + decongestionSettings.getUpdatePriceInterval() + "_it"
					+ "_toleratedDelay" + decongestionSettings.getToleratedAverageDelaySec()
					+ "_start" + decongestionSettings.getFractionOfIterationsToStartPriceAdjustment()
					+ "_end" + decongestionSettings.getFractionOfIterationsToEndPriceAdjustment()
					+ "_tollMSA" + decongestionSettings.isMsa()
					+ "_blendFactor" + decongestionSettings.getTollBlendFactor();
		
		if (decongestionSettings.getDecongestionApproach().toString().equals(DecongestionApproach.BangBang.toString())) {
			outputDirectory = outputDirectory + 
					"_init" + decongestionSettings.getInitialToll() +
					"_adj" + decongestionSettings.getTollAdjustment();
		
		} else if (decongestionSettings.getDecongestionApproach().toString().equals(DecongestionApproach.PID.toString())) {
			outputDirectory = outputDirectory +
					"_Kp" + decongestionSettings.getKp() +
					"_Ki" + decongestionSettings.getKi() +
					"_Kd" + decongestionSettings.getKd() +
					"_integral" + decongestionSettings.getIntegralApproach() + 
					"_alpha" + decongestionSettings.getIntegralApproachAverageAlpha() + 
					"_factor" + decongestionSettings.getIntegralApproachUnusedHeadwayFactor();
			
		} else if (decongestionSettings.getDecongestionApproach().toString().equals(DecongestionApproach.P_MC.toString())) {
			outputDirectory = outputDirectory +
					"_KpVTTSxDemandLevel" +
					"_Ki0" +
					"_Kd0";
		}
							
		log.info("Output directory: " + outputDirectory);
		
		config.controler().setOutputDirectory(outputDirectory + "/");
				
		// ---
		
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		
		// #############################################################
		
		// congestion toll computation
		
		controler.addOverridingModule(new DecongestionModule(scenario));
		
		// toll-adjusted routing
		
		final TollTimeDistanceTravelDisutilityFactory travelDisutilityFactory = new TollTimeDistanceTravelDisutilityFactory();
		travelDisutilityFactory.setSigma(0.);
		
		controler.addOverridingModule(new AbstractModule(){
			@Override
			public void install() {
				this.bindCarTravelDisutilityFactory().toInstance( travelDisutilityFactory );
			}
		});	
		
		// #############################################################
	
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists);
//      controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

        controler.run();
        
        PersonTripBasicAnalysisRun analysis = new PersonTripBasicAnalysisRun(outputDirectory);
		analysis.run();
	}
}

