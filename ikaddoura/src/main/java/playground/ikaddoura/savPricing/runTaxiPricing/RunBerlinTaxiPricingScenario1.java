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

package playground.ikaddoura.savPricing.runTaxiPricing;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.decongestion.DecongestionConfigGroup;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.contrib.noise.utils.MergeNoiseCSVFile;
import org.matsim.contrib.noise.utils.ProcessNoiseImmissions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.sav.runTaxi.RunBerlinTaxiScenario1;

import playground.ikaddoura.analysis.IKAnalysisRun;
import playground.ikaddoura.analysis.modalSplitUserType.AgentAnalysisFilter;
import playground.ikaddoura.analysis.modalSplitUserType.ModalSplitUserTypeControlerListener;
import playground.ikaddoura.savPricing.SAVPricingConfigGroup;
import playground.ikaddoura.savPricing.SAVPricingModule;

/**
* @author ikaddoura
*/

public class RunBerlinTaxiPricingScenario1 {
	private static final Logger log = Logger.getLogger(RunBerlinTaxiPricingScenario1.class);

	private static String configFileName;
	private static String overridingConfigFileName;
	private static String restrictedCarAreaShapeFile;
	private static String drtServiceAreaShapeFile;
	private static String transitStopCoordinatesSFile;
	private static String runId;
	private static String outputDirectory;
	private static String visualizationScriptDirectory;
	private static Integer scaleFactor;
		
	public static void main(String[] args) {

		if (args.length > 0) {		
			configFileName = args[0];
			overridingConfigFileName = args[1];
			restrictedCarAreaShapeFile = args[2];
			drtServiceAreaShapeFile = args[3];
			transitStopCoordinatesSFile = args[4];
			runId = args[5];
			outputDirectory = args[6];
			visualizationScriptDirectory = args[7];
			scaleFactor = Integer.parseInt(args[8]);
			
		} else {		
			String baseDirectory = "/Users/ihab/Documents/workspace/matsim-berlin/";	
			configFileName = baseDirectory + "scenarios/berlin-v5.2-1pct/input/berlin-taxi1-v5.2-1pct.config.xml";
			overridingConfigFileName = null;
			restrictedCarAreaShapeFile = baseDirectory + "scenarios/berlin-v5.2-10pct/input/berlin-shp/berlin.shp";
			drtServiceAreaShapeFile = baseDirectory + "scenarios/berlin-v5.2-10pct/input/berliner-ring-area-shp/service-area.shp";
			transitStopCoordinatesSFile = baseDirectory + "scenarios/berlin-v5.2-10pct/input/berlin-v5.2.transit-stop-coordinates_S-zoneC.csv";
			runId = "taxi1-test-1";
			outputDirectory = "/Users/ihab/Documents/workspace/runs-svn/savPricing/output/output-local-run_" + runId + "/";
			visualizationScriptDirectory = "./visualization-scripts/";
			scaleFactor = 100;
		}
			
		log.info("run Id: " + runId);
		log.info("output directory: " + outputDirectory);
		
		RunBerlinTaxiPricingScenario1 drtPricing = new RunBerlinTaxiPricingScenario1();
		drtPricing.run();
	}
	
	private void run() {
		RunBerlinTaxiScenario1 berlin = new RunBerlinTaxiScenario1(configFileName, overridingConfigFileName, restrictedCarAreaShapeFile, drtServiceAreaShapeFile, transitStopCoordinatesSFile);
		
		ConfigGroup[] modulesToAdd = {new SAVPricingConfigGroup(), new DecongestionConfigGroup(), new NoiseConfigGroup()};
		Config config = berlin.prepareConfig(modulesToAdd);
		config.controler().setRunId(runId);
		config.controler().setOutputDirectory(outputDirectory);
						
		Scenario scenario = berlin.prepareScenario();
		
		Controler controler = berlin.prepareControler();		

		// taxi pricing
		controler.addOverridingModule(new SAVPricingModule(scenario, RunBerlinTaxiScenario1.modeToReplaceCarTripsInBrandenburg));
		
		// modal split analysis
		controler.addOverridingModule(new org.matsim.core.controler.AbstractModule() {	
			@Override
			public void install() {
				this.addControlerListenerBinding().to(ModalSplitUserTypeControlerListener.class);
			}
		});
		
		controler.run();
		
		log.info("Done.");
		
		log.info("Running offline analysis...");
		
		final String scenarioCRS = TransformationFactory.DHDN_GK4;	
		final String shapeFileZones = null;
		final String zonesCRS = null;
		final String homeActivity = "home";
		final int scalingFactor = scaleFactor;
		
		List<AgentAnalysisFilter> filters = new ArrayList<>();

		AgentAnalysisFilter filter1 = new AgentAnalysisFilter(scenario);
		filter1.setPersonAttribute("berlin");
		filter1.setPersonAttributeName("home-activity-zone");
		filter1.preProcess(controler.getScenario());
		filters.add(filter1);
		
		AgentAnalysisFilter filter2 = new AgentAnalysisFilter(scenario);
		filter2.preProcess(controler.getScenario());
		filters.add(filter2);
		
		AgentAnalysisFilter filter3 = new AgentAnalysisFilter(scenario);
		filter3.setPersonAttribute("brandenburg");
		filter3.setPersonAttributeName("home-activity-zone");
		filter3.preProcess(controler.getScenario());
		filters.add(filter3);
		
		List<String> modes = new ArrayList<>();
		modes.add(TransportMode.car);
		modes.add(RunBerlinTaxiScenario1.modeToReplaceCarTripsInBrandenburg);
		modes.add(TransportMode.taxi);

		IKAnalysisRun analysis = new IKAnalysisRun(
				controler.getScenario(),
				null,
				visualizationScriptDirectory,
				scenarioCRS,
				shapeFileZones,
				zonesCRS,
				homeActivity,
				scalingFactor,
				filters,
				null,
				modes);
		analysis.run();
		
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

