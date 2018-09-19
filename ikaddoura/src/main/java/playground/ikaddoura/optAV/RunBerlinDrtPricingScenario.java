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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.contrib.decongestion.DecongestionConfigGroup;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.contrib.noise.utils.MergeNoiseCSVFile;
import org.matsim.contrib.noise.utils.ProcessNoiseImmissions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.run.RunBerlinDrtScenario;

import playground.ikaddoura.analysis.IKAnalysisRun;
import playground.ikaddoura.analysis.modalSplitUserType.AgentAnalysisFilter;
import playground.ikaddoura.analysis.modalSplitUserType.ModalSplitUserTypeControlerListener;

/**
* @author ikaddoura
*/

public class RunBerlinDrtPricingScenario {
	private static final Logger log = Logger.getLogger(RunBerlinDrtPricingScenario.class);

	public static void main(String[] args) {
		
		String configFileName;
		String overridingConfigFileName;
		String berlinShapeFile;
		String drtServiceAreaShapeFile;
		String transitStopCoordinatesSFile;
		String transitStopCoordinatesRBFile;
		
		String runId;
		String outputDirectory;

		String visualizationScriptDirectory;
		
		final boolean turnDefaultScenarioIntoDrtScenario = true;

		if (args.length > 0) {
			
			configFileName = args[0];
			overridingConfigFileName = args[1];
			berlinShapeFile = args[2];
			drtServiceAreaShapeFile = args[3];
			transitStopCoordinatesSFile = args[4];
			transitStopCoordinatesRBFile = args[5];
			
			runId = args[6];
			outputDirectory = args[7];

			visualizationScriptDirectory = args[8];

		} else {
			
			String baseDirectory = "/Users/ihab/Documents/workspace/matsim-berlin/";
			
			configFileName = baseDirectory + "scenarios/berlin-v5.2-1pct/input/berlin-drt-v5.2-1pct.config_2agents_drtPricing.xml";
			
			overridingConfigFileName = null;
			
			berlinShapeFile = baseDirectory + "scenarios/berlin-v5.2-10pct/input/berlin-shp/berlin.shp";
			drtServiceAreaShapeFile = baseDirectory + "scenarios/berlin-v5.2-10pct/input/berlin_bb_area/service-area.shp";

			transitStopCoordinatesSFile = baseDirectory + "scenarios/berlin-v5.2-10pct/input/berlin-v5.2.transit-stop-coordinates_S-zoneC.csv";
			transitStopCoordinatesRBFile = baseDirectory + "scenarios/berlin-v5.2-10pct/input/berlin-v5.2.transit-stop-coordinates_RB-zoneC.csv";
			
			runId = "drt-opt-1";
			outputDirectory = "/Users/ihab/Documents/workspace/runs-svn/drtPricing/output/output-local-run_" + runId + "/";
			
			visualizationScriptDirectory = "./visualization-scripts/";
		}
			
		log.info("run Id: " + runId);
		log.info("output directory: " + outputDirectory); 

		RunBerlinDrtScenario berlin = new RunBerlinDrtScenario(configFileName, overridingConfigFileName, turnDefaultScenarioIntoDrtScenario, berlinShapeFile, drtServiceAreaShapeFile, transitStopCoordinatesSFile, transitStopCoordinatesRBFile);
		
		ConfigGroup[] modulesToAdd = {new OptAVConfigGroup(), new DecongestionConfigGroup(), new NoiseConfigGroup()};
		Config config = berlin.prepareConfig(modulesToAdd);
		config.controler().setRunId(runId);
		config.controler().setOutputDirectory(outputDirectory);
						
		Controler controler = berlin.prepareControler();		
//		controler.addOverridingModule(new DRTpricingModule(controler.getScenario()));
		
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
		final int scalingFactor = 10;
		
		List<AgentAnalysisFilter> filters = new ArrayList<>();

		AgentAnalysisFilter filter1 = new AgentAnalysisFilter(controler.getScenario());
		filter1.setPersonAttribute("berlin");
		filter1.setPersonAttributeName("home-activity-zone");
		filter1.preProcess(controler.getScenario());
		filters.add(filter1);
		
		AgentAnalysisFilter filter2 = new AgentAnalysisFilter(controler.getScenario());
		filter2.preProcess(controler.getScenario());
		filters.add(filter2);
		
		AgentAnalysisFilter filter3 = new AgentAnalysisFilter(controler.getScenario());
		filter3.setPersonAttribute("brandenburg");
		filter3.setPersonAttributeName("home-activity-zone");
		filter3.preProcess(controler.getScenario());
		filters.add(filter3);

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
				null);
		analysis.run();
		
		// noise post-analysis
		
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
		
		log.info("Done.");
	}

}

