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
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.noise.personLinkMoneyEvents.CombinedPersonLinkMoneyEventsReader;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import playground.ikaddoura.analysis.detailedPersonTripAnalysis.PersonTripAnalysis;
import playground.ikaddoura.analysis.detailedPersonTripAnalysis.handler.BasicPersonTripAnalysisHandler;
import playground.ikaddoura.analysis.detailedPersonTripAnalysis.handler.PersonMoneyLinkHandler;
import playground.ikaddoura.analysis.dynamicLinkDemand.DynamicLinkDemandEventHandler;
import playground.ikaddoura.analysis.gisAnalysis.GISAnalyzer;
import playground.ikaddoura.analysis.gisAnalysis.MoneyExtCostHandler;
import playground.ikaddoura.analysis.linkDemand.LinkDemandEventHandler;
import playground.ikaddoura.analysis.modeSwitchAnalysis.PersonTripScenarioComparison;
import playground.ikaddoura.analysis.shapes.Network2Shape;
import playground.ikaddoura.analysis.visualizationScripts.VisualizationScriptAdjustment;
import playground.ikaddoura.decongestion.handler.DelayAnalysis;

/**
 * 
 * Provides several analysis:
 * 
 * basic aggregated analysis 
 * --> aggregated results: number of trips, number of stuck trips, travel time, travel distance, caused/affected noise cost, toll payments, user benefits, welfare
 * 
 * basic person-specific information
 * person ; number of trips; ...
 * 
 * basic trip-specific information
 * --> person ; trip no.; leg mode ; stuckAbort (trip) ; departure time (trip) ; trip arrival time (trip) ; travel time (trip) ; travel distance (trip) ; toll payment (trip)
 * 
 * time-specific trip travel times, distances, toll payments, externality-specific toll payments (congestion tolls, noise tolls, air pollution tolls)
 * 
 * delay information
 * 
 * daily traffic volume per link
 * 
 * hourly traffic volume per link
 * 
 * spatial analysis: number of activities per zone, average toll payment, user benefit etc. per zone
 * 
 * mode switch analysis
 * 
 * writes out the network as a shapefile.
 * 
 *
 * used packages: linkDemand, dynamicLinkDemand, detailedPersonTripAnalysis, decongestion.delayAnalysis, gisAnalysis, modeSwitchAnalysis
 * 
 */
public class IKAnalysisRun {
	private static final Logger log = Logger.getLogger(IKAnalysisRun.class);

	private final String scenarioCRS;	
	private final String shapeFileZones;
	private final String zonesCRS;
	private final String homeActivity;
	private final int scalingFactor;
	
	// policy case
	private final String runDirectory;
	private final String runId;
	private final Scenario scenario1;
	
	// base case (optional)
	private final String runDirectoryToCompareWith;
	private final String runIdToCompareWith;
	private final Scenario scenario0;

	private final String outputDirectoryName = "analysis-IK-2018-02-09";
			
	public static void main(String[] args) throws IOException {
			
		String runDirectory;
		String runId;
		String runDirectoryToCompareWith;
		String runIdToCompareWith;
		String scenarioCRS;	
		String shapeFileZones;
		String zonesCRS;
		String homeActivity;
		int scalingFactor;
		
		if (args.length > 0) {
			runDirectory = args[0];
			log.info("Run directory: " + runDirectory);
			
			runId = args[1];
			
			runDirectoryToCompareWith = args[2];
			log.info("Run directory to compare with: " + runDirectoryToCompareWith);
			
			runIdToCompareWith = args[3];
			
			scenarioCRS = args[4];	
			shapeFileZones = args[5];
			zonesCRS = args[6];
			
			homeActivity = args[7];
			scalingFactor = Integer.valueOf(args[8]);
		
		} else {
			
//			runDirectory = "/Users/ihab/Documents/workspace/runs-svn/cne_berlin10pct/output/m_r_output_cne/";
			runDirectory = "/Users/ihab/Documents/workspace/runs-svn/cne/berlin-dz-1pct-simpleNetwork/output-FINAL/m_r_output_run3_bln_c_DecongestionPID/";
			runId = "policyCase";
			
//			runDirectoryToCompareWith = "/Users/ihab/Documents/workspace/runs-svn/cne_berlin10pct/output/m_r_output_run0_baseCase/";	
			runDirectoryToCompareWith = "/Users/ihab/Documents/workspace/runs-svn/cne/berlin-dz-1pct-simpleNetwork/output-FINAL/m_r_output_run0_bln_bc";
			runIdToCompareWith = "baseCase";
			
			scenarioCRS = TransformationFactory.DHDN_GK4;	
			
//			shapeFileZones = "/Users/ihab/Documents/workspace/shared-svn/studies/ihab/berlin/shapeFiles/berlin_grid_2500/berlin_grid_2500.shp";
//			zonesCRS = TransformationFactory.DHDN_GK4;

			shapeFileZones = "/Users/ihab/Documents/workspace/shared-svn/studies/ihab/berlin/shapeFiles/berlin_LOR_SHP_EPSG_3068/Planungsraum_EPSG_3068.shp";
			zonesCRS = TransformationFactory.DHDN_SoldnerBerlin;
			
			homeActivity = "home";
			scalingFactor = 10;			
		}
		
		IKAnalysisRun analysis = new IKAnalysisRun(runDirectory, runId, runDirectoryToCompareWith, runIdToCompareWith,
				scenarioCRS, shapeFileZones, zonesCRS, homeActivity, scalingFactor);
		analysis.run();
	}
	
	public IKAnalysisRun(Scenario scenario, String scenarioCRS) {
		
		String runDirectory = scenario.getConfig().controler().getOutputDirectory();
		if (!runDirectory.endsWith("/")) runDirectory = runDirectory + "/";

		this.scenario1 = scenario;
		this.runDirectory = runDirectory;
		this.runId = scenario.getConfig().controler().getRunId();
		
		// scenario 0 will not be analyzed
		this.scenario0 = null;
		this.runDirectoryToCompareWith = null;
		this.runIdToCompareWith = null;
		
		this.scenarioCRS = scenarioCRS;
		this.shapeFileZones = null;
		this.zonesCRS = null;
		this.homeActivity = null;
		this.scalingFactor = 0;
	}
	
	public IKAnalysisRun(Scenario scenario1, Scenario scenario2,
			String scenarioCRS, String shapeFileZones, String zonesCRS, String homeActivity, int scalingFactor) {

		String runDirectory = scenario1.getConfig().controler().getOutputDirectory();
		if (!runDirectory.endsWith("/")) runDirectory = runDirectory + "/";

		String runDirectoryToCompareWith = scenario2.getConfig().controler().getOutputDirectory();
		if (!runDirectoryToCompareWith.endsWith("/")) runDirectoryToCompareWith = runDirectoryToCompareWith + "/";
		
		this.scenario1 = scenario1;
		this.runDirectory = runDirectory;
		this.runId = scenario1.getConfig().controler().getRunId();

		this.scenario0 = scenario2;
		this.runDirectoryToCompareWith = runDirectoryToCompareWith;
		this.runIdToCompareWith = scenario2.getConfig().controler().getRunId();
		
		this.scenarioCRS = scenarioCRS;
		this.shapeFileZones = shapeFileZones;
		this.zonesCRS = zonesCRS;
		this.homeActivity = homeActivity;
		this.scalingFactor = scalingFactor;
	}

	public IKAnalysisRun(String runDirectory, String runId, String runDirectoryToCompareWith, String runIdToCompareWith,
			String scenarioCRS, String shapeFileZones, String zonesCRS, String homeActivity, int scalingFactor) {

		if (!runDirectory.endsWith("/")) runDirectory = runDirectory + "/";
		if (runDirectoryToCompareWith!= null && !runDirectoryToCompareWith.endsWith("/")) runDirectoryToCompareWith = runDirectoryToCompareWith + "/";

		this.runDirectory = runDirectory;
		this.runId = runId;
		this.scenario1 = loadScenario(runDirectory, runId);
						
		this.runDirectoryToCompareWith = runDirectoryToCompareWith;
		this.runIdToCompareWith = runIdToCompareWith;
		this.scenario0 = loadScenario(runDirectoryToCompareWith, runIdToCompareWith);
		
		this.scenarioCRS = scenarioCRS;
		this.shapeFileZones = shapeFileZones;
		this.zonesCRS = zonesCRS;
		this.homeActivity = homeActivity;
		this.scalingFactor = scalingFactor;
	}

	public void run() {
		
		String analysisOutputDirectory = runDirectory + outputDirectoryName + "/";
		File folder = new File(analysisOutputDirectory);			
		folder.mkdirs();
		
		OutputDirectoryLogging.catchLogEntries();
		try {
			OutputDirectoryLogging.initLoggingWithOutputDirectory(analysisOutputDirectory);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		log.info("Starting analysis...");
		
		log.info("Run directory: " + runDirectory);
		log.info("Run ID: " + runId);
		
		log.info("Run directory to compare with: " + runDirectoryToCompareWith);
		log.info("Run ID to compare with: " + runIdToCompareWith);
	
		// #####################################
		// Create and add the analysis handlers
		// #####################################

		EventsManager events1 = null;
		BasicPersonTripAnalysisHandler basicHandler1 = null;
		DelayAnalysis delayAnalysis1 = null;
		LinkDemandEventHandler trafficVolumeAnalysis1 = null;
		DynamicLinkDemandEventHandler dynamicTrafficVolumeAnalysis1 = null;
		PersonMoneyLinkHandler personTripMoneyHandler1 = null;
		MoneyExtCostHandler personMoneyHandler1 = null;

		if (scenario1 != null) {
			basicHandler1 = new BasicPersonTripAnalysisHandler();
			basicHandler1.setScenario(scenario1);

			delayAnalysis1 = new DelayAnalysis();
			delayAnalysis1.setScenario(scenario1);
			
			trafficVolumeAnalysis1 = new LinkDemandEventHandler(scenario1.getNetwork());
			dynamicTrafficVolumeAnalysis1 = new DynamicLinkDemandEventHandler(scenario1.getNetwork());
			
			personTripMoneyHandler1 = new PersonMoneyLinkHandler();
			personTripMoneyHandler1.setBasicHandler(basicHandler1);
			
			personMoneyHandler1 = new MoneyExtCostHandler();
			
			events1 = EventsUtils.createEventsManager();
			events1.addHandler(basicHandler1);
			events1.addHandler(delayAnalysis1);
			events1.addHandler(trafficVolumeAnalysis1);
			events1.addHandler(dynamicTrafficVolumeAnalysis1);
			events1.addHandler(personTripMoneyHandler1);
			events1.addHandler(personMoneyHandler1);
		}
		
		EventsManager events0 = null;
		BasicPersonTripAnalysisHandler basicHandler0 = null;
		DelayAnalysis delayAnalysis0 = null;
		LinkDemandEventHandler trafficVolumeAnalysis0 = null;
		DynamicLinkDemandEventHandler dynamicTrafficVolumeAnalysis0 = null;
		PersonMoneyLinkHandler personTripMoneyHandler0 = null;
		MoneyExtCostHandler personMoneyHandler0 = null;
		
		if (scenario0 != null) {
			basicHandler0 = new BasicPersonTripAnalysisHandler();
			basicHandler0.setScenario(scenario0);

			delayAnalysis0 = new DelayAnalysis();
			delayAnalysis0.setScenario(scenario0);
			
			trafficVolumeAnalysis0 = new LinkDemandEventHandler(scenario0.getNetwork());
			dynamicTrafficVolumeAnalysis0 = new DynamicLinkDemandEventHandler(scenario0.getNetwork());
			
			personTripMoneyHandler0 = new PersonMoneyLinkHandler();
			personTripMoneyHandler0.setBasicHandler(basicHandler0);
			
			personMoneyHandler0 = new MoneyExtCostHandler();

			events0 = EventsUtils.createEventsManager();
			events0.addHandler(basicHandler0);
			events0.addHandler(delayAnalysis0);
			events0.addHandler(trafficVolumeAnalysis0);
			events0.addHandler(dynamicTrafficVolumeAnalysis0);
			events0.addHandler(personTripMoneyHandler0);
			events0.addHandler(personMoneyHandler0);
		}

		// #####################################
		// Read the events file and plans file
		// #####################################
		
		if (scenario1 != null) readEventsFile(runDirectory, runId, events1);
		if (scenario0 != null) readEventsFile(runDirectoryToCompareWith, runIdToCompareWith, events0);
				
		Map<Id<Person>, Double> personId2userBenefit1 = getPersonId2UserBenefit(scenario1);
		Map<Id<Person>, Double> personId2userBenefit2 = getPersonId2UserBenefit(scenario0);
		
		// #####################################
		// Print the results
		// #####################################

		log.info("Printing results...");
		if (scenario1 != null) printResults(scenario1, analysisOutputDirectory, personId2userBenefit1, basicHandler1, delayAnalysis1, personTripMoneyHandler1, trafficVolumeAnalysis1, dynamicTrafficVolumeAnalysis1, personMoneyHandler1);
		
		log.info("Printing results...");
		if (scenario0 != null) printResults(scenario0, analysisOutputDirectory, personId2userBenefit2, basicHandler0, delayAnalysis0, personTripMoneyHandler0, trafficVolumeAnalysis0, dynamicTrafficVolumeAnalysis0, personMoneyHandler0);

		// #####################################
		// Scenario comparison
		// #####################################
		
		String personTripScenarioComparisonOutputDirectory = null;
		
		if (scenario1 != null & scenario0 != null) {
			
			personTripScenarioComparisonOutputDirectory = analysisOutputDirectory + "scenario-comparison_" + runId + "-vs-" + runIdToCompareWith + "/";
			createDirectory(personTripScenarioComparisonOutputDirectory);

			PersonTripScenarioComparison scenarioComparison = new PersonTripScenarioComparison(this.homeActivity, personTripScenarioComparisonOutputDirectory, scenario1, basicHandler1, scenario0, basicHandler0);
			try {
				scenarioComparison.analyzeByMode();
				scenarioComparison.analyzeByScore(0.0);
				scenarioComparison.analyzeByScore(1.0);
				scenarioComparison.analyzeByScore(10.0);
				scenarioComparison.analyzeByScore(100.0);
			} catch (IOException e) {
				e.printStackTrace();
			}
		
		}

		// #####################################
		// Write the visualization scripts
		// #####################################

		// traffic volumes
		if (scenario1 != null & scenario0 != null) {
			String visScriptTemplateFile = "./visualization-scripts/traffic-volume_absolute-difference_noCRS.qgs";
			String visScriptOutputFile = analysisOutputDirectory + "link-volume-analysis/" + "traffic-volume_absolute-difference_" + runId + "-vs-" + runIdToCompareWith + ".qgs";
			
			VisualizationScriptAdjustment script = new VisualizationScriptAdjustment(visScriptTemplateFile, visScriptOutputFile);
			script.setRunId(this.runId);
			script.setRunIdToCompareWith(this.runIdToCompareWith);
			script.setScalingFactor(String.valueOf(this.scalingFactor));
			script.setCRS(this.scenarioCRS);
			script.write();
		}
		
		// spatial zone-based analysis
		if (scenario1 != null & scenario0 != null) {
			String visScriptTemplateFile = "./visualization-scripts/zone-based-analysis_welfare_modes.qgs";
			String visScriptOutputFile = analysisOutputDirectory + "zone-based-analysis_welfare_modes/" + "zone-based-analysis_welfare_modes_" + runId + "-vs-" + runIdToCompareWith + ".qgs";
			
			VisualizationScriptAdjustment script = new VisualizationScriptAdjustment(visScriptTemplateFile, visScriptOutputFile);
			script.setRunId(this.runId);
			script.setRunIdToCompareWith(this.runIdToCompareWith);
			script.setScalingFactor(String.valueOf(this.scalingFactor));
			script.setCRS(this.zonesCRS);
			script.write();
		}
		
		// scenario comparison: person-specific mode-shift effects
		if (scenario1 != null & scenario0 != null) {
			String visScriptTemplateFile = "./visualization-scripts/scenario-comparison_person-specific-mode-switch-effects.qgs";
			String visScriptOutputFile = personTripScenarioComparisonOutputDirectory + "scenario-comparison_person-specific-mode-switch-effects_" + runId + "-vs-" + runIdToCompareWith + ".qgs";
			
			VisualizationScriptAdjustment script = new VisualizationScriptAdjustment(visScriptTemplateFile, visScriptOutputFile);
			script.setRunId(this.runId);
			script.setRunIdToCompareWith(this.runIdToCompareWith);
			script.setScalingFactor(String.valueOf(this.scalingFactor));
			script.setCRS(this.scenarioCRS);
			script.write();
		}
		
		// scenario comparison: person-specific winner-loser analysis
		if (scenario1 != null & scenario0 != null) {
			String visScriptTemplateFile = "./visualization-scripts/scenario-comparison_person-specific-winner-loser.qgs";
			String visScriptOutputFile = personTripScenarioComparisonOutputDirectory + "scenario-comparison_person-specific-winner-loser_" + runId + "-vs-" + runIdToCompareWith + ".qgs";
			
			VisualizationScriptAdjustment script = new VisualizationScriptAdjustment(visScriptTemplateFile, visScriptOutputFile);
			script.setRunId(this.runId);
			script.setRunIdToCompareWith(this.runIdToCompareWith);
			script.setScalingFactor(String.valueOf(this.scalingFactor));
			script.setCRS(this.scenarioCRS);
			script.write();
		}
	
		// externality-specific toll payments
		{
			String visScriptTemplateFile = "./visualization-scripts/extCostPerTimeOfDay-cne_percentages.R";
			String visScriptOutputFile = analysisOutputDirectory + "person-trip-welfare-analysis/" + "extCostPerTimeOfDay-cne_percentages_" + runId + ".R";
					
			VisualizationScriptAdjustment script = new VisualizationScriptAdjustment(visScriptTemplateFile, visScriptOutputFile);
			script.setRunId(this.runId);
			script.setRunIdToCompareWith(this.runIdToCompareWith);
			script.setScalingFactor(String.valueOf(this.scalingFactor));
			script.setCRS(this.scenarioCRS);
			script.write();
		} 
	}

	private void printResults(Scenario scenario,
			String analysisOutputDirectory,
			Map<Id<Person>, Double> personId2userBenefit,
			BasicPersonTripAnalysisHandler basicHandler,
			DelayAnalysis delayAnalysis,
			PersonMoneyLinkHandler personTripMoneyHandler,
			LinkDemandEventHandler trafficVolumeAnalysis,
			DynamicLinkDemandEventHandler dynamicTrafficVolumeAnalysis,
			MoneyExtCostHandler personMoneyHandler) {
		
		// #####################################
		// Print results: person / trip analysis
		// #####################################
		
		String personTripAnalysOutputDirectory = analysisOutputDirectory + "person-trip-welfare-analysis/";
		createDirectory(personTripAnalysOutputDirectory);
		String personTripAnalysisOutputDirectoryWithPrefix = personTripAnalysOutputDirectory + scenario.getConfig().controler().getRunId() + ".";
		
		PersonTripAnalysis analysis = new PersonTripAnalysis();

		// trip-based analysis
		analysis.printTripInformation(personTripAnalysisOutputDirectoryWithPrefix, TransportMode.car, basicHandler, null, null);

		// person-based analysis
		analysis.printPersonInformation(personTripAnalysisOutputDirectoryWithPrefix, TransportMode.car, personId2userBenefit, basicHandler, null);	

		// aggregated analysis
		analysis.printAggregatedResults(personTripAnalysisOutputDirectoryWithPrefix, TransportMode.car, personId2userBenefit, basicHandler, null);
		analysis.printAggregatedResults(personTripAnalysisOutputDirectoryWithPrefix, null, personId2userBenefit, basicHandler, null);
		analysis.printAggregatedResults(personTripAnalysisOutputDirectoryWithPrefix, personId2userBenefit, basicHandler, null, null, delayAnalysis, null);
		
		// time-specific trip distance analysis
		SortedMap<Double, List<Double>> departureTime2traveldistance = analysis.getParameter2Values(TransportMode.car, basicHandler, basicHandler.getPersonId2tripNumber2departureTime(), basicHandler.getPersonId2tripNumber2tripDistance(), 3600., 30 * 3600.);
		analysis.printAvgValuePerParameter(personTripAnalysisOutputDirectoryWithPrefix + "distancePerDepartureTime_car_3600.csv", departureTime2traveldistance);
		
		// time-specific trip travel time analysis
		SortedMap<Double, List<Double>> departureTime2travelTime = analysis.getParameter2Values(TransportMode.car, basicHandler, basicHandler.getPersonId2tripNumber2departureTime(), basicHandler.getPersonId2tripNumber2travelTime(), 3600., 30 * 3600.);
		analysis.printAvgValuePerParameter(personTripAnalysisOutputDirectoryWithPrefix + "travelTimePerDepartureTime_car_3600.csv", departureTime2travelTime);
	
		// time-specific toll payments analysis
		SortedMap<Double, List<Double>> departureTime2tolls = analysis.getParameter2Values(TransportMode.car, basicHandler, basicHandler.getPersonId2tripNumber2departureTime(), basicHandler.getPersonId2tripNumber2payment(), 3600., 30 * 3600.);
		analysis.printAvgValuePerParameter(personTripAnalysisOutputDirectoryWithPrefix + "tollsPerDepartureTime_car_3600.csv", departureTime2tolls);
			
		// time-specific congestion toll payments analysis
		SortedMap<Double, List<Double>> departureTime2congestionTolls = analysis.getParameter2Values(TransportMode.car, basicHandler, basicHandler.getPersonId2tripNumber2departureTime(), personTripMoneyHandler.getPersonId2tripNumber2congestionPayment(), 3600., 30 * 3600.);
		analysis.printAvgValuePerParameter(personTripAnalysisOutputDirectoryWithPrefix + "congestionTollsPerDepartureTime_car_3600.csv", departureTime2congestionTolls);
		
		// time-specific noise toll payments analysis
		SortedMap<Double, List<Double>> departureTime2noiseTolls = analysis.getParameter2Values(TransportMode.car, basicHandler, basicHandler.getPersonId2tripNumber2departureTime(), personTripMoneyHandler.getPersonId2tripNumber2noisePayment(), 3600., 30 * 3600.);
		analysis.printAvgValuePerParameter(personTripAnalysisOutputDirectoryWithPrefix + "noiseTollsPerDepartureTime_car_3600.csv", departureTime2noiseTolls);
		
		// time-specific air pollution toll payments analysis
		SortedMap<Double, List<Double>> departureTime2airPollutionTolls = analysis.getParameter2Values(TransportMode.car, basicHandler, basicHandler.getPersonId2tripNumber2departureTime(), personTripMoneyHandler.getPersonId2tripNumber2airPollutionPayment(), 3600., 30 * 3600.);
		analysis.printAvgValuePerParameter(personTripAnalysisOutputDirectoryWithPrefix + "airPollutionTollsPerDepartureTime_car_3600.csv", departureTime2airPollutionTolls);
		
		// #####################################
		// Print results: link traffic volumes
		// #####################################
		
		String trafficVolumeAnalysisOutputDirectory = analysisOutputDirectory + "link-volume-analysis/";
		createDirectory(trafficVolumeAnalysisOutputDirectory);
		String trafficVolumeAnalysisOutputDirectoryWithPrefix = trafficVolumeAnalysisOutputDirectory + scenario.getConfig().controler().getRunId() + ".";
		
		// daily traffic volumes
		trafficVolumeAnalysis.printResults(trafficVolumeAnalysisOutputDirectoryWithPrefix + "link_dailyTrafficVolume.csv");
		
		// hourly traffic volumes
		dynamicTrafficVolumeAnalysis.printResults(trafficVolumeAnalysisOutputDirectoryWithPrefix);

		// #####################################
		// Print results: spatial analysis
		// #####################################
		
		if (shapeFileZones != null && zonesCRS != null && scenarioCRS != null && scalingFactor != 0) {		
			
			String spatialAnalysisOutputDirectory = analysisOutputDirectory + "zone-based-analysis_welfare_modes/";
			createDirectory(spatialAnalysisOutputDirectory);
			String spatialAnalysisOutputDirectoryWithPrefix = spatialAnalysisOutputDirectory + scenario.getConfig().controler().getRunId() + ".";
			
			GISAnalyzer gisAnalysis = new GISAnalyzer(shapeFileZones, scalingFactor, homeActivity, zonesCRS, scenarioCRS);
			gisAnalysis.analyzeZoneTollsUserBenefits(scenario, spatialAnalysisOutputDirectoryWithPrefix, "tolls_userBenefits_travelTime_modes_zones.shp", personId2userBenefit, personMoneyHandler.getPersonId2toll(), personMoneyHandler.getPersonId2congestionToll(), personMoneyHandler.getPersonId2noiseToll(), personMoneyHandler.getPersonId2airPollutionToll(), basicHandler);
		}
		
		// #####################################
		// Print results: network shape file
		// #####################################

		if (scenarioCRS != null) {
			String networkOutputDirectory = analysisOutputDirectory + "network-shp/";
			String outputDirectoryWithPrefix = networkOutputDirectory + scenario.getConfig().controler().getRunId() + ".";
			Network2Shape.exportNetwork2Shp(scenario, outputDirectoryWithPrefix, scenarioCRS, TransformationFactory.getCoordinateTransformation(scenarioCRS, scenarioCRS));
		}
	}
	
	private void createDirectory(String directory) {
		File file = new File(directory);
		file.mkdirs();
	}

	private Scenario loadScenario(String runDirectory, String runId) {
		
		if (runDirectory == null) {
			return null;
			
		} else {
			
			String networkFile;
			String populationFile;
			String configFile;
			
			if (new File(runDirectory + runId + ".output_config.xml").exists()) {
				
				networkFile = runDirectory + runId + ".output_network.xml.gz";
				populationFile = runDirectory + runId + ".output_plans.xml.gz";
				configFile = runDirectory + runId + ".output_config.xml";
				
			} else {
				
				networkFile = runDirectory + "output_network.xml.gz";
				populationFile = runDirectory + "output_plans.xml.gz";
				configFile = runDirectory + "output_config.xml";
			}

			Config config = ConfigUtils.loadConfig(configFile);

			if (config.controler().getRunId() != null) {
				if (!runId.equals(config.controler().getRunId())) throw new RuntimeException("Given run ID " + runId + " doesn't match the run ID given in the config file. Aborting...");
			} else {
				config.controler().setRunId(runId);
			}

			config.plans().setInputFile(populationFile);
			config.network().setInputFile(networkFile);
			config.vehicles().setVehiclesFile(null);
			config.plans().setInputPersonAttributeFile(null);

			return ScenarioUtils.loadScenario(config);
		}
	}
	
	private Map<Id<Person>, Double> getPersonId2UserBenefit(Scenario scenario) {
		if (scenario != null) {
			int countWrn = 0;
			Map<Id<Person>, Double> personId2userBenefit = new HashMap<>();
			if (scenario != null) {
				for (Person person : scenario.getPopulation().getPersons().values()) {
					
					if (countWrn <= 5) {
						if (person.getSelectedPlan().getScore() == null || person.getSelectedPlan().getScore() < 0.) {
							log.warn("The score of person " + person.getId() + " is null or negative: " + person.getSelectedPlan().toString());
							if (countWrn == 5) {
								log.warn("Further warnings of this type are not printed out.");
							}
							countWrn++;
						}						
					}
					
					personId2userBenefit.put(person.getId(), person.getSelectedPlan().getScore() / scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney());
				}
			}
			return personId2userBenefit;
		} else {
			return null;
		}
	}

	private void readEventsFile(String runDirectory, String runId, EventsManager events) {
		String eventsFile;
		if (new File(runDirectory + runId + ".output_events.xml.gz").exists()) {
			eventsFile = runDirectory + runId + ".output_events.xml.gz";
		} else {
			eventsFile = runDirectory + "output_events.xml.gz";
		}
		new CombinedPersonLinkMoneyEventsReader(events).readFile(eventsFile);
	}

}
		

