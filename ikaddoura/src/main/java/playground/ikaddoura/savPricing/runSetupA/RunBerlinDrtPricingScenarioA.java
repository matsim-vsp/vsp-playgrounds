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

package playground.ikaddoura.savPricing.runSetupA;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.decongestion.DecongestionConfigGroup;
import org.matsim.contrib.noise.MergeNoiseCSVFile;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.contrib.noise.ProcessNoiseImmissions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;

import playground.ikaddoura.savPricing.RunBerlinDrtScenarioA;
import playground.ikaddoura.savPricing.SAVPricingConfigGroup;
import playground.ikaddoura.savPricing.SAVPricingModule;

/**
* @author ikaddoura
*/

public class RunBerlinDrtPricingScenarioA {
	private static final Logger log = Logger.getLogger(RunBerlinDrtPricingScenarioA.class);

	private static String configFileName;
	private static String overridingConfigFileName;
	private static String drtServiceAreaShapeFile;
	private static double dailyRewardDrtInsteadOfPrivateCar;
	private static String runId;
	private static String outputDirectory;

	public static void main(String[] args) {

		if (args.length > 0) {		
			configFileName = args[0];
			overridingConfigFileName = args[1];
			drtServiceAreaShapeFile = args[2];
			dailyRewardDrtInsteadOfPrivateCar = Double.parseDouble(args[3]);
			runId = args[4];
			outputDirectory = args[5];
			
		} else {		
			String baseDirectory = "/Users/ihab/Documents/workspace/matsim-berlin/";	
			configFileName = baseDirectory + "scenarios/berlin-v5.2-1pct/input/berlin-drtB-v5.2-1pct.config.xml";
			overridingConfigFileName = null;
			drtServiceAreaShapeFile = "http://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-sav-v5.2-10pct/input/shp-inner-city-area/inner-city-area.shp";
			dailyRewardDrtInsteadOfPrivateCar = 0.;
			runId = "drtB-test-1";
			outputDirectory = "/Users/ihab/Documents/workspace/runs-svn/savPricing/output/output-local-run_" + runId + "/";
		}
			
		log.info("run Id: " + runId);
		log.info("output directory: " + outputDirectory);
		
		RunBerlinDrtPricingScenarioA drtPricing = new RunBerlinDrtPricingScenarioA();
		drtPricing.run();
	}
	
	private void run() {
		RunBerlinDrtScenarioA berlin = new RunBerlinDrtScenarioA(configFileName, overridingConfigFileName, drtServiceAreaShapeFile, dailyRewardDrtInsteadOfPrivateCar);
		
		ConfigGroup[] modulesToAdd = {new SAVPricingConfigGroup(), new DecongestionConfigGroup(), new NoiseConfigGroup()};
		Config config = berlin.prepareConfig(modulesToAdd);
		config.controler().setRunId(runId);
		config.controler().setOutputDirectory(outputDirectory);
			
		SAVPricingConfigGroup optAVParamsDrt = ConfigUtils.addOrGetModule(config, SAVPricingConfigGroup.class);
		optAVParamsDrt.setSavMode(TransportMode.drt);
		
		Scenario scenario = berlin.prepareScenario();
		
		Controler controler = berlin.prepareControler();		

		// sav pricing
		controler.addOverridingModule(new SAVPricingModule(scenario, RunBerlinDrtScenarioA.modeToReplaceCarTripsInBrandenburg));
		
		controler.run();
		
		log.info("Done.");
		
		// noise post-analysis
		
		SAVPricingConfigGroup optAVParams = ConfigUtils.addOrGetModule(config, SAVPricingConfigGroup.class);
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
		
		log.info("Done.");
	}

}

