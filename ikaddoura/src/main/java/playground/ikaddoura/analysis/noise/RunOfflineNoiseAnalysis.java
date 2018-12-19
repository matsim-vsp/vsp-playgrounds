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

package playground.ikaddoura.analysis.noise;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.contrib.noise.NoiseOfflineCalculation;
import org.matsim.contrib.noise.utils.MergeNoiseCSVFile;
import org.matsim.contrib.noise.utils.ProcessNoiseImmissions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

/**
 * 
 * @author ikaddoura
 *
 */
public class RunOfflineNoiseAnalysis {
	private static final Logger log = Logger.getLogger(RunOfflineNoiseAnalysis.class);
	
	private static String runDirectory;
	private static String outputDirectory;
	private static String runId;
	private static double receiverPointGap;
	private static double timeBinSize;				
	private static String tunnelLinkIdFile;	

	public static void main(String[] args) {
		
		if (args.length > 0) {
						
			runDirectory = args[0];		
			log.info("run directory: " + runDirectory);
						
			outputDirectory = args[1];		
			log.info("output directory: " + outputDirectory);
			
			runId = args[2];		
			log.info("run Id: " + runId);
			
			receiverPointGap = Double.valueOf(args[3]);		
			log.info("Receiver point gap: " + receiverPointGap);
			
			timeBinSize = Double.valueOf(args[4]);		
			log.info("Time bin size: " + timeBinSize);
			
			tunnelLinkIdFile = args[5];		
			log.info("tunnelLinkIdFile: " + tunnelLinkIdFile);
			
		} else {
			
			runDirectory = "/Users/ihab/Documents/workspace/runs-svn/sav-pricing-setupA/output_bc-0c/";
			outputDirectory = "/Users/ihab/Documents/workspace/runs-svn/sav-pricing-setupA/output_bc-0c/";
			runId = "bc-0c";
			
			tunnelLinkIdFile = "/Users/ihab/Documents/workspace/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.2-10pct/input/berlin-v5.1.tunnel-linkIDs.csv";
			receiverPointGap = 100.;
			timeBinSize = 3600.;
		}
		
		Config config = ConfigUtils.createConfig(new NoiseConfigGroup());
		config.global().setCoordinateSystem(TransformationFactory.GK4);
		config.network().setInputCRS(TransformationFactory.GK4);
		config.network().setInputFile(runDirectory + runId + ".output_network.xml.gz");
		config.plans().setInputFile(runDirectory + runId + ".output_plans.xml.gz");
		config.controler().setOutputDirectory(runDirectory);
		config.controler().setRunId(runId);
						
		// adjust the default noise parameters
		
		NoiseConfigGroup noiseParameters = (NoiseConfigGroup) config.getModules().get(NoiseConfigGroup.GROUP_NAME);
		noiseParameters.setReceiverPointGap(receiverPointGap);

      // Berlin Coordinates: Greater Berlin area
		double xMin = 4573258.;
		double yMin = 5801225.;
		double xMax = 4620323.;
		double yMax = 5839639.;
		
		noiseParameters.setReceiverPointsGridMinX(xMin);
		noiseParameters.setReceiverPointsGridMinY(yMin);
		noiseParameters.setReceiverPointsGridMaxX(xMax);
		noiseParameters.setReceiverPointsGridMaxY(yMax);
		
//		 Berlin Activity Types
		String[] consideredActivitiesForDamages = {"home*", "work*", "leisure*", "shopping*", "other*"};
		noiseParameters.setConsideredActivitiesForDamageCalculationArray(consideredActivitiesForDamages);
		
		// ################################
		
		noiseParameters.setUseActualSpeedLevel(true);
		noiseParameters.setAllowForSpeedsOutsideTheValidRange(false);
		noiseParameters.setScaleFactor(10.);
		noiseParameters.setComputePopulationUnits(true);
		noiseParameters.setComputeNoiseDamages(true);
		noiseParameters.setInternalizeNoiseDamages(false);
		noiseParameters.setComputeCausingAgents(false);
		noiseParameters.setThrowNoiseEventsAffected(true);
		noiseParameters.setThrowNoiseEventsCaused(false);
		
		String[] hgvIdPrefixes = { "freight" };
		noiseParameters.setHgvIdPrefixesArray(hgvIdPrefixes);
		
		noiseParameters.setTunnelLinkIdFile(tunnelLinkIdFile);		
		noiseParameters.setTimeBinSizeNoiseComputation(timeBinSize);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		NoiseOfflineCalculation noiseCalculation = new NoiseOfflineCalculation(scenario, outputDirectory);
		noiseCalculation.run();	
		
		// some processing of the output data
		String outputFilePath = outputDirectory + "nosie-analysis/";
		ProcessNoiseImmissions process = new ProcessNoiseImmissions(outputFilePath + "immissions/", outputFilePath + "receiverPoints/receiverPoints.csv", noiseParameters.getReceiverPointGap());
		process.run();
				
		final String[] labels = { "damages_receiverPoint" };
		final String[] workingDirectories = { outputFilePath + "/damages_receiverPoint/" };

		MergeNoiseCSVFile merger = new MergeNoiseCSVFile() ;
		merger.setReceiverPointsFile(outputFilePath + "receiverPoints/receiverPoints.csv");
		merger.setOutputDirectory(outputFilePath);
		merger.setTimeBinSize(noiseParameters.getTimeBinSizeNoiseComputation());
		merger.setWorkingDirectory(workingDirectories);
		merger.setLabel(labels);
		merger.run();
	}
}
		

