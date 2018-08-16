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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.decongestion.handler.DelayAnalysis;
import org.matsim.contrib.noise.personLinkMoneyEvents.CombinedPersonLinkMoneyEventsReader;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import playground.ikaddoura.analysis.actDurations.ActDurationHandler;
import playground.ikaddoura.analysis.detailedPersonTripAnalysis.PersonTripAnalysis;
import playground.ikaddoura.analysis.detailedPersonTripAnalysis.handler.BasicPersonTripAnalysisHandler;
import playground.ikaddoura.analysis.detailedPersonTripAnalysis.handler.PersonMoneyLinkHandler;
import playground.ikaddoura.analysis.dynamicLinkDemand.DynamicLinkDemandEventHandler;
import playground.ikaddoura.analysis.gisAnalysis.GISAnalyzer;
import playground.ikaddoura.analysis.gisAnalysis.MoneyExtCostHandler;
import playground.ikaddoura.analysis.linkDemand.LinkDemandEventHandler;
import playground.ikaddoura.analysis.modalSplitUserType.AgentAnalysisFilter;
import playground.ikaddoura.analysis.modalSplitUserType.ModeAnalysis;
import playground.ikaddoura.analysis.modeSwitchAnalysis.PersonTripScenarioComparison;
import playground.ikaddoura.analysis.pngSequence2Video.MATSimVideoUtils;
import playground.ikaddoura.analysis.shapes.Network2Shape;
import playground.ikaddoura.analysis.visualizationScripts.VisualizationScriptAdjustment;
import playground.ikaddoura.analysis.vtts.VTTSHandler;

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
 * modal split for different population filters
 * 
 * writes out the network as a shapefile.
 * 
 *
 * used packages: linkDemand, dynamicLinkDemand, detailedPersonTripAnalysis, decongestion.delayAnalysis, gisAnalysis, modeSwitchAnalysis, modalSplitUserType, vtts
 * 
 */
public class IKAnalysisRun {
	private static final Logger log = Logger.getLogger(IKAnalysisRun.class);

	private final String scenarioCRS;	
	private final String shapeFileZones;
	private final String zonesCRS;
	private final String homeActivityPrefix;
	private final int scalingFactor;
		
	// policy case
	private final String runDirectory;
	private final String runId;
	private final Scenario scenario1;
	private final List<AgentAnalysisFilter> filters1;
	
	// base case (optional)
	private final String runDirectoryToCompareWith;
	private final String runIdToCompareWith;
	private final Scenario scenario0;
	private final List<AgentAnalysisFilter> filters0;

	private final String outputDirectoryName = "analysis-ik-v1.1";

	private final String visualizationScriptInputDirectory;
			
	public static void main(String[] args) throws IOException {
			
		String runDirectory = null;
		String runId = null;
		String runDirectoryToCompareWith = null;
		String runIdToCompareWith = null;
		String visualizationScriptInputDirectory = null;
		String scenarioCRS = null;	
		String shapeFileZones = null;
		String zonesCRS = null;
		String homeActivityPrefix = null;
		int scalingFactor;
		
		if (args.length > 0) {
			if (!args[0].equals("null")) runDirectory = args[0];
			log.info("Run directory: " + runDirectory);
			
			if (!args[1].equals("null")) runId = args[1];
			log.info("Run Id: " + runDirectory);
			
			if (!args[2].equals("null")) runDirectoryToCompareWith = args[2];
			log.info("Run directory to compare with: " + runDirectoryToCompareWith);
			
			if (!args[3].equals("null")) runIdToCompareWith = args[3];
			log.info("Run Id to compare with: " + runDirectory);
			
			if (!args[4].equals("null")) scenarioCRS = args[4];	
			log.info("Scenario CRS: " + scenarioCRS);
			
			if (!args[5].equals("null")) shapeFileZones = args[5];
			log.info("Shape file zones: " + shapeFileZones);

			if (!args[6].equals("null")) zonesCRS = args[6];
			log.info("Zones CRS: " + zonesCRS);
			
			if (!args[7].equals("null")) homeActivityPrefix = args[7];
			log.info("Home activity: " + homeActivityPrefix);

			scalingFactor = Integer.valueOf(args[8]);
			log.info("Scaling factor: " + scalingFactor);
		
			if (!args[9].equals("null")) visualizationScriptInputDirectory = args[9];
			log.info("Visualization script input directory: " + visualizationScriptInputDirectory);

		} else {
			
//			runDirectory = "/Users/ihab/Documents/workspace/runs-svn/b5_optAV_congestion/output/run_oAV_B1_1pct/";
//			runId = "oAV_B1_1pct";
//			runDirectoryToCompareWith = "/Users/ihab/Documents/workspace/runs-svn/b5_optAV_congestion/output/run_oAV_0_1pct/";
//			runIdToCompareWith = "oAV_0_1pct";
			
			runDirectory = "/Users/ihab/Documents/workspace/runs-svn/b5_optAV_congestion/output/oAV_C7/";
			runId = "oAV_C7";		
			runDirectoryToCompareWith = "/Users/ihab/Documents/workspace/runs-svn/b5_optAV_congestion/output/oAV_0/";
			runIdToCompareWith = "oAV_0";
			
			visualizationScriptInputDirectory = "./visualization-scripts/";
			
			scenarioCRS = TransformationFactory.DHDN_GK4;	
			
			shapeFileZones = "/Users/ihab/Documents/workspace/shared-svn/studies/ihab/berlin/shapeFiles/berlin_grid_2500/berlin_grid_2500.shp";
			zonesCRS = TransformationFactory.DHDN_GK4;

//			shapeFileZones = "/Users/ihab/Documents/workspace/shared-svn/studies/ihab/berlin/shapeFiles/greater-berlin-area_3000/greater-berlin-area_3000.shp";
//			zonesCRS = TransformationFactory.DHDN_GK4;
			
//			shapeFileZones = "/Users/ihab/Documents/workspace/shared-svn/studies/ihab/berlin/shapeFiles/berlin_LOR_SHP_EPSG_3068/Planungsraum_EPSG_3068.shp";
//			zonesCRS = TransformationFactory.DHDN_SoldnerBerlin;
			
			homeActivityPrefix = "home";
			scalingFactor = 10;			
		}
		
		Scenario scenario1 = loadScenario(runDirectory, runId, null);
		Scenario scenario0 = loadScenario(runDirectoryToCompareWith, runIdToCompareWith, null);
		
		List<AgentAnalysisFilter> filter1 = new ArrayList<>();

		AgentAnalysisFilter filter1a = new AgentAnalysisFilter(scenario1);
		filter1a.setSubpopulation("person");
		filter1a.setPersonAttribute("berlin");
		filter1a.setPersonAttributeName("home-activity-zone");
		filter1a.preProcess(scenario1);
		filter1.add(filter1a);
		
		AgentAnalysisFilter filter1b = new AgentAnalysisFilter(scenario1);
		filter1b.preProcess(scenario1);
		filter1.add(filter1b);
		
		AgentAnalysisFilter filter1c = new AgentAnalysisFilter(scenario1);
		filter1c.setSubpopulation("person");
		filter1c.setPersonAttribute("brandenburg");
		filter1c.setPersonAttributeName("home-activity-zone");
		filter1c.preProcess(scenario1);
		filter1.add(filter1c);
		
		List<AgentAnalysisFilter> filter0 = null;

		IKAnalysisRun analysis = new IKAnalysisRun(
				scenario1,
				scenario0,
				visualizationScriptInputDirectory,
				scenarioCRS,
				shapeFileZones,
				zonesCRS,
				homeActivityPrefix,
				scalingFactor,
				filter1,
				filter0);
		analysis.run();
	}
	
	@Deprecated
	public IKAnalysisRun(Scenario scenario, String scenarioCRS) {
		this(scenario, scenarioCRS, 1);
	}
	
	@Deprecated
	public IKAnalysisRun(Scenario scenario, String scenarioCRS, int scalingFactor) {
		this(scenario, "./visualization-scripts/", scenarioCRS, scalingFactor);
	}
	
	public IKAnalysisRun(Scenario scenario, String visualizationScriptInputDirectory, String scenarioCRS, int scalingFactor) {
		
		String runDirectory = scenario.getConfig().controler().getOutputDirectory();
		if (!runDirectory.endsWith("/")) runDirectory = runDirectory + "/";

		this.scenario1 = scenario;
		this.runDirectory = runDirectory;
		this.runId = scenario.getConfig().controler().getRunId();
		
		// scenario 0 will not be analyzed
		this.scenario0 = null;
		this.runDirectoryToCompareWith = null;
		this.runIdToCompareWith = null;
		
		this.visualizationScriptInputDirectory = visualizationScriptInputDirectory;
		
		this.scenarioCRS = scenarioCRS;
		this.shapeFileZones = null;
		this.zonesCRS = null;
		this.homeActivityPrefix = null;
		this.scalingFactor = scalingFactor;
		
		this.filters0 = null;
		this.filters1 = null;
	}
	
	@Deprecated
	public IKAnalysisRun(Scenario scenario1, Scenario scenario0,
			String scenarioCRS, String shapeFileZones, String zonesCRS, String homeActivityPrefix, int scalingFactor,
			List<AgentAnalysisFilter> filters1, List<AgentAnalysisFilter> filters0) {
		
		this(scenario1, scenario0, "./visualization-scripts/", scenarioCRS, shapeFileZones, zonesCRS, homeActivityPrefix, scalingFactor, filters1, filters0);
	}
	
	public IKAnalysisRun(Scenario scenario1, Scenario scenario0,
			String visualizationScriptInputDirectory, String scenarioCRS, String shapeFileZones, String zonesCRS, String homeActivityPrefix, int scalingFactor,
			List<AgentAnalysisFilter> filters1, List<AgentAnalysisFilter> filters0) {

		String runDirectory = scenario1.getConfig().controler().getOutputDirectory();
		if (!runDirectory.endsWith("/")) runDirectory = runDirectory + "/";

		String runDirectoryToCompareWith = null;
		if (scenario0 != null) {
			runDirectoryToCompareWith = scenario0.getConfig().controler().getOutputDirectory();
			if (!runDirectoryToCompareWith.endsWith("/")) runDirectoryToCompareWith = runDirectoryToCompareWith + "/";
		}
		
		String runIdToCompareWith = null;
		if (scenario0 != null) runIdToCompareWith = scenario0.getConfig().controler().getRunId();

		this.scenario1 = scenario1;
		this.runDirectory = runDirectory;
		this.runId = scenario1.getConfig().controler().getRunId();

		this.scenario0 = scenario0;
		this.runDirectoryToCompareWith = runDirectoryToCompareWith;
		this.runIdToCompareWith = runIdToCompareWith;
		
		this.visualizationScriptInputDirectory = visualizationScriptInputDirectory;
		
		this.scenarioCRS = scenarioCRS;
		this.shapeFileZones = shapeFileZones;
		this.zonesCRS = zonesCRS;
		this.homeActivityPrefix = homeActivityPrefix;
		this.scalingFactor = scalingFactor;
		
		this.filters0 = filters0;
		this.filters1 = filters1;
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
		// Create and add the event handlers
		// #####################################

		EventsManager events1 = null;
		
		BasicPersonTripAnalysisHandler basicHandler1 = null;
		DelayAnalysis delayAnalysis1 = null;
		LinkDemandEventHandler trafficVolumeAnalysis1 = null;
		DynamicLinkDemandEventHandler dynamicTrafficVolumeAnalysis1 = null;
		PersonMoneyLinkHandler personTripMoneyHandler1 = null;
		MoneyExtCostHandler personMoneyHandler1 = null;
		ActDurationHandler actHandler1 = null;
		VTTSHandler vttsHandler1 = null;

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
			
			actHandler1 = new ActDurationHandler();
			
			vttsHandler1 = new VTTSHandler(scenario1);
			
			events1 = EventsUtils.createEventsManager();
			events1.addHandler(basicHandler1);
			events1.addHandler(delayAnalysis1);
			events1.addHandler(trafficVolumeAnalysis1);
			events1.addHandler(dynamicTrafficVolumeAnalysis1);
			events1.addHandler(personTripMoneyHandler1);
			events1.addHandler(personMoneyHandler1);
			events1.addHandler(actHandler1);
			events1.addHandler(vttsHandler1);
		}
		
		EventsManager events0 = null;
		
		BasicPersonTripAnalysisHandler basicHandler0 = null;
		DelayAnalysis delayAnalysis0 = null;
		LinkDemandEventHandler trafficVolumeAnalysis0 = null;
		DynamicLinkDemandEventHandler dynamicTrafficVolumeAnalysis0 = null;
		PersonMoneyLinkHandler personTripMoneyHandler0 = null;
		MoneyExtCostHandler personMoneyHandler0 = null;
		ActDurationHandler actHandler0 = null;
		VTTSHandler vttsHandler0 = null;
		
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
			
			actHandler0 = new ActDurationHandler();
			
			vttsHandler0 = new VTTSHandler(scenario0);

			events0 = EventsUtils.createEventsManager();
			events0.addHandler(basicHandler0);
			events0.addHandler(delayAnalysis0);
			events0.addHandler(trafficVolumeAnalysis0);
			events0.addHandler(dynamicTrafficVolumeAnalysis0);
			events0.addHandler(personTripMoneyHandler0);
			events0.addHandler(personMoneyHandler0);
			events0.addHandler(actHandler0);
			events0.addHandler(vttsHandler0);
		}

		// #####################################
		// Read the events file
		// #####################################
		
		if (scenario1 != null) readEventsFile(runDirectory, runId, events1);
		if (scenario0 != null) readEventsFile(runDirectoryToCompareWith, runIdToCompareWith, events0);
				
		// #####################################
		// Post process and read plans file
		// #####################################
		
		Map<Id<Person>, Double> personId2userBenefit1 = null;
		Map<Id<Person>, Double> personId2userBenefit0 = null;
		
		List<ModeAnalysis> modeAnalysisList1 = new ArrayList<>();
		List<ModeAnalysis> modeAnalysisList0 = new ArrayList<>();
								
		if (scenario1 != null) {
			
			personId2userBenefit1 = getPersonId2UserBenefit(scenario1);
			
			if (filters1 != null) {
				for (AgentAnalysisFilter filter : filters1) {
					ModeAnalysis modeAnalysis1 = new ModeAnalysis(scenario1, filter);
					modeAnalysis1.run();
					modeAnalysisList1.add(modeAnalysis1);
				}
			}
			
			vttsHandler1.computeFinalVTTS();
		}	
		
		if (scenario0 != null) {
			
			personId2userBenefit0 = getPersonId2UserBenefit(scenario0);
			
			if (filters0 != null) {
				for (AgentAnalysisFilter filter : filters0) {
					ModeAnalysis modeAnalysis0 = new ModeAnalysis(scenario0, filter);
					modeAnalysis0.run();
					modeAnalysisList0.add(modeAnalysis0);
				}
			}
			
			vttsHandler0.computeFinalVTTS();
		}	
		
		// #####################################
		// Print the results
		// #####################################

		log.info("Printing results...");
		if (scenario1 != null) printResults(
				scenario1,
				analysisOutputDirectory,
				personId2userBenefit1, basicHandler1,
				delayAnalysis1,
				personTripMoneyHandler1,
				trafficVolumeAnalysis1,
				dynamicTrafficVolumeAnalysis1,
				personMoneyHandler1,
				actHandler1,
				modeAnalysisList1,
				vttsHandler1);
		
		log.info("Printing results...");
		if (scenario0 != null) printResults(scenario0,
				analysisOutputDirectory,
				personId2userBenefit0,
				basicHandler0,
				delayAnalysis0,
				personTripMoneyHandler0,
				trafficVolumeAnalysis0,
				dynamicTrafficVolumeAnalysis0,
				personMoneyHandler0,
				actHandler0,
				modeAnalysisList0,
				vttsHandler0);

		// #####################################
		// Scenario comparison
		// #####################################
		
		String personTripScenarioComparisonOutputDirectory = null;
		
		if (scenario1 != null & scenario0 != null) {
			
			personTripScenarioComparisonOutputDirectory = analysisOutputDirectory + "scenario-comparison_" + runId + "-vs-" + runIdToCompareWith + "/";
			createDirectory(personTripScenarioComparisonOutputDirectory);

			PersonTripScenarioComparison scenarioComparison = new PersonTripScenarioComparison(this.homeActivityPrefix, personTripScenarioComparisonOutputDirectory, scenario1, basicHandler1, scenario0, basicHandler0);
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
			String visScriptTemplateFile = visualizationScriptInputDirectory + "traffic-volume_absolute-difference_noCRS.qgs";
			String visScriptOutputFile = analysisOutputDirectory + "link-volume-analysis/" + "traffic-volume_absolute-difference_" + runId + "-vs-" + runIdToCompareWith + ".qgs";
			
			VisualizationScriptAdjustment script = new VisualizationScriptAdjustment(visScriptTemplateFile, visScriptOutputFile);
			script.setRunId(this.runId);
			script.setRunIdToCompareWith(this.runIdToCompareWith);
			script.setScalingFactor(String.valueOf(this.scalingFactor));
			script.setCRS(this.scenarioCRS);
			script.write();
		}
		
		// absolute traffic volumes policy case
		if (scenario1 != null) {
			String visScriptTemplateFile = visualizationScriptInputDirectory + "traffic-volume_absolute_noCRS.qgs";
			String visScriptOutputFile = analysisOutputDirectory + "link-volume-analysis/" + runId + ".traffic-volume_absolute.qgs";
			
			VisualizationScriptAdjustment script = new VisualizationScriptAdjustment(visScriptTemplateFile, visScriptOutputFile);
			script.setRunId(this.runId);
			script.setScalingFactor(String.valueOf(this.scalingFactor));
			script.setCRS(this.scenarioCRS);
			script.write();
		}
		
		// spatial zone-based analysis
		if (scenario1 != null & scenario0 != null) {
			String visScriptTemplateFile = visualizationScriptInputDirectory + "zone-based-analysis_welfare_modes.qgs";
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
			String visScriptTemplateFile = visualizationScriptInputDirectory + "scenario-comparison_person-specific-mode-switch-effects.qgs";
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
			String visScriptTemplateFile = visualizationScriptInputDirectory + "scenario-comparison_person-specific-winner-loser.qgs";
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
			String visScriptTemplateFile = visualizationScriptInputDirectory + "extCostPerTimeOfDay-cne_percentages.R";
			String visScriptOutputFile = analysisOutputDirectory + "person-trip-welfare-analysis/" + "extCostPerTimeOfDay-cne_percentages_" + runId + ".R";
					
			VisualizationScriptAdjustment script = new VisualizationScriptAdjustment(visScriptTemplateFile, visScriptOutputFile);
			script.setRunId(this.runId);
			script.setRunIdToCompareWith(this.runIdToCompareWith);
			script.setScalingFactor(String.valueOf(this.scalingFactor));
			script.setCRS(this.scenarioCRS);
			script.write();
		} 
		
		log.info("Analysis completed.");
	}

	private void printResults(Scenario scenario,
			String analysisOutputDirectory,
			Map<Id<Person>, Double> personId2userBenefit,
			BasicPersonTripAnalysisHandler basicHandler,
			DelayAnalysis delayAnalysis,
			PersonMoneyLinkHandler personTripMoneyHandler,
			LinkDemandEventHandler trafficVolumeAnalysis,
			DynamicLinkDemandEventHandler dynamicTrafficVolumeAnalysis,
			MoneyExtCostHandler personMoneyHandler,
			ActDurationHandler actHandler,
			List<ModeAnalysis> modeAnalysisList,
			VTTSHandler vttsHandler) {
		
		// #####################################
		// Print results: person / trip analysis
		// #####################################
		
		String personTripAnalysOutputDirectory = analysisOutputDirectory + "person-trip-welfare-analysis/";
		createDirectory(personTripAnalysOutputDirectory);
		String personTripAnalysisOutputDirectoryWithPrefix = personTripAnalysOutputDirectory + scenario.getConfig().controler().getRunId() + ".";
		
		PersonTripAnalysis analysis = new PersonTripAnalysis();

		// trip-based analysis
		analysis.printTripInformation(personTripAnalysisOutputDirectoryWithPrefix, TransportMode.car, basicHandler, null, null);
		analysis.printTripInformation(personTripAnalysisOutputDirectoryWithPrefix, null, basicHandler, null, null);

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
			
			GISAnalyzer gisAnalysis = new GISAnalyzer(scenario, shapeFileZones, scalingFactor, homeActivityPrefix, zonesCRS, scenarioCRS);
			gisAnalysis.analyzeZoneTollsUserBenefits(spatialAnalysisOutputDirectoryWithPrefix, "tolls_userBenefits_travelTime_modes_zones.shp", personId2userBenefit, personMoneyHandler.getPersonId2toll(), personMoneyHandler.getPersonId2congestionToll(), personMoneyHandler.getPersonId2noiseToll(), personMoneyHandler.getPersonId2airPollutionToll(), basicHandler);
		}
		
		// #####################################
		// Print results: network shape file
		// #####################################

		if (scenarioCRS != null) {
			String networkOutputDirectory = analysisOutputDirectory + "network-shp/";
			String outputDirectoryWithPrefix = networkOutputDirectory + scenario.getConfig().controler().getRunId() + ".";
			Network2Shape.exportNetwork2Shp(scenario, outputDirectoryWithPrefix, scenarioCRS, TransformationFactory.getCoordinateTransformation(scenarioCRS, scenarioCRS));
		}
		
		// #####################################
		// Print results: activity durations
		// #####################################
		
		String actDurationsOutputDirectory = analysisOutputDirectory + "activity-durations/";
		createDirectory(actDurationsOutputDirectory);
		
		List<String> skippedPersonIdStrings = new ArrayList<>();
		skippedPersonIdStrings.add("freight");
		actHandler.process(scenario.getPopulation(), skippedPersonIdStrings);
		
		actHandler.writeOutput(scenario.getPopulation(), actDurationsOutputDirectory + scenario.getConfig().controler().getRunId() + "." + "activity-durations.csv", Double.POSITIVE_INFINITY);
		actHandler.writeOutput(scenario.getPopulation(), actDurationsOutputDirectory + scenario.getConfig().controler().getRunId() + "." + "activity-durations_below-900-sec.csv", 900.);
		actHandler.writeSummary(scenario.getPopulation(), actDurationsOutputDirectory + scenario.getConfig().controler().getRunId() + "." + "activity-durations_summary.csv");

		// #####################################
		// Print results: mode statistics
		// #####################################
		
		String modeAnalysisOutputDirectory = analysisOutputDirectory + "mode-statistics/";
		createDirectory(modeAnalysisOutputDirectory);
		for (ModeAnalysis modeAnalysis : modeAnalysisList) {
			
			modeAnalysis.writeModeShares(modeAnalysisOutputDirectory + scenario.getConfig().controler().getRunId() + ".");
			modeAnalysis.writeTripRouteDistances(modeAnalysisOutputDirectory + scenario.getConfig().controler().getRunId() + ".");
			modeAnalysis.writeTripEuclideanDistances(modeAnalysisOutputDirectory + scenario.getConfig().controler().getRunId() + ".");
			
			final List<Tuple<Double, Double>> distanceGroups = new ArrayList<>();
			distanceGroups.add(new Tuple<>(0., 1000.));
			distanceGroups.add(new Tuple<>(1000., 3000.));
			distanceGroups.add(new Tuple<>(3000., 5000.));
			distanceGroups.add(new Tuple<>(5000., 10000.));
			distanceGroups.add(new Tuple<>(10000., 20000.));
			distanceGroups.add(new Tuple<>(20000., 100000.));
			modeAnalysis.writeTripRouteDistances(modeAnalysisOutputDirectory + scenario.getConfig().controler().getRunId() + ".", distanceGroups);
			modeAnalysis.writeTripEuclideanDistances(modeAnalysisOutputDirectory + scenario.getConfig().controler().getRunId() + ".", distanceGroups);
		}
		
		// #####################################
		// Print results: VTTS
		// #####################################
		
		String vttsOutputDirectory = analysisOutputDirectory + "vtts/";
		createDirectory(vttsOutputDirectory);

		vttsHandler.printVTTS(vttsOutputDirectory + scenario.getConfig().controler().getRunId() + "." + "VTTS_allTrips.csv");
		vttsHandler.printCarVTTS(vttsOutputDirectory + scenario.getConfig().controler().getRunId() + "." + "VTTS_carTrips.csv");
		vttsHandler.printAvgVTTSperPerson(vttsOutputDirectory + scenario.getConfig().controler().getRunId() + "." + "VTTS_avgPerPerson.csv"); 
		
		vttsHandler.printVTTSstatistics(vttsOutputDirectory + scenario.getConfig().controler().getRunId() + "." + "VTTS_statistics_all-modes.csv", null, null);
		vttsHandler.printVTTSstatistics(vttsOutputDirectory + scenario.getConfig().controler().getRunId() + "." + "VTTS_statistics_car.csv", "car", null);
		vttsHandler.printVTTSstatistics(vttsOutputDirectory + scenario.getConfig().controler().getRunId() + "." + "VTTS_statistics_pt.csv", "pt", null);
		vttsHandler.printVTTSstatistics(vttsOutputDirectory + scenario.getConfig().controler().getRunId() + "." + "VTTS_statistics_walk.csv", "walk", null);
		vttsHandler.printVTTSstatistics(vttsOutputDirectory + scenario.getConfig().controler().getRunId() + "." + "VTTS_statistics_bicycle.csv", "bicycle", null);
		vttsHandler.printVTTSstatistics(vttsOutputDirectory + scenario.getConfig().controler().getRunId() + "." + "VTTS_statistics_ride.csv", "ride", null);

		vttsHandler.printVTTSstatistics(vttsOutputDirectory + scenario.getConfig().controler().getRunId() + "." + "VTTS_statistics_car_7-9.csv", "car", new Tuple<Double, Double>(7.0 * 3600., 9. * 3600.));
		vttsHandler.printVTTSstatistics(vttsOutputDirectory + scenario.getConfig().controler().getRunId() + "." + "VTTS_statistics_car_11-13.csv", "car", new Tuple<Double, Double>(11.0 * 3600., 13. * 3600.));
		vttsHandler.printVTTSstatistics(vttsOutputDirectory + scenario.getConfig().controler().getRunId() + "." + "VTTS_statistics_car_16-18.csv", "car", new Tuple<Double, Double>(16.0 * 3600., 18. * 3600.));
		
		// #####################################
		// Print leg histogram videos
		// #####################################
		
		String legHistogramOutputDirectory = analysisOutputDirectory + "legHistograms/";
		createDirectory(legHistogramOutputDirectory);
		
		log.info("Creating leg histogram video for all modes...");
		try {
			MATSimVideoUtils.createVideo(scenario.getConfig().controler().getOutputDirectory(), scenario.getConfig().controler().getRunId(), legHistogramOutputDirectory, 1, "legHistogram_all");
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		log.info("Creating leg histogram video for all modes... Done.");

		for (String mode : scenario.getConfig().plansCalcRoute().getNetworkModes()) {
			try {
				log.info("Creating leg histogram video for mode " + mode);
				MATSimVideoUtils.createVideo(scenario.getConfig().controler().getOutputDirectory(), scenario.getConfig().controler().getRunId(), legHistogramOutputDirectory, 1, "legHistogram_" + mode);
				log.info("Creating leg histogram video for mode " + mode + " Done.");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}
	
	private void createDirectory(String directory) {
		File file = new File(directory);
		file.mkdirs();
	}

	private static Scenario loadScenario(String runDirectory, String runId, String personAttributesFileToReplaceOutputFile) {
		log.info("Loading scenario...");
		
		if (runDirectory == null) {
			return null;	
		}
		
		if (runDirectory.equals("")) {
			return null;	
		}
		
		if (runDirectory.equals("null")) {
			return null;	
		}
		
		if (!runDirectory.endsWith("/")) runDirectory = runDirectory + "/";

		String networkFile;
		String populationFile;
		String personAttributesFile;
		String configFile;
		
		if (new File(runDirectory + runId + ".output_config.xml").exists()) {
			
			configFile = runDirectory + runId + ".output_config.xml";

//			networkFile = runDirectory + runId + ".output_network.xml.gz";
//			populationFile = runDirectory + runId + ".output_plans.xml.gz";
			
			networkFile = runId + ".output_network.xml.gz";
			populationFile = runId + ".output_plans.xml.gz";
			
			if (personAttributesFileToReplaceOutputFile == null) {
//				personAttributesFile = runDirectory + runId + ".output_personAttributes.xml.gz";
				personAttributesFile = runId + ".output_personAttributes.xml.gz";
			} else {
				personAttributesFile = personAttributesFileToReplaceOutputFile;
			}
			
		} else {
			
			configFile = runDirectory + "output_config.xml";

//			networkFile = runDirectory + "output_network.xml.gz";
//			populationFile = runDirectory + "output_plans.xml.gz";
			
			networkFile = "output_network.xml.gz";
			populationFile = "output_plans.xml.gz";
			
			if (personAttributesFileToReplaceOutputFile == null) {
//				personAttributesFile = runDirectory + "output_personAttributes.xml.gz";
				personAttributesFile = "output_personAttributes.xml.gz";
			} else {
				personAttributesFile = personAttributesFileToReplaceOutputFile;
			}
		}

		Config config = ConfigUtils.loadConfig(configFile);

		if (config.controler().getRunId() != null) {
			if (!runId.equals(config.controler().getRunId())) throw new RuntimeException("Given run ID " + runId + " doesn't match the run ID given in the config file. Aborting...");
		} else {
			config.controler().setRunId(runId);
		}

		config.controler().setOutputDirectory(runDirectory);
		config.plans().setInputFile(populationFile);
		config.plans().setInputPersonAttributeFile(personAttributesFile);
		config.network().setInputFile(networkFile);
		config.vehicles().setVehiclesFile(null);
		config.transit().setTransitScheduleFile(null);
		config.transit().setVehiclesFile(null);
		
		return ScenarioUtils.loadScenario(config);
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
		log.info("Trying to read " + runDirectory + runId + ".output_events.xml.gz" + "...");
		if (new File(runDirectory + runId + ".output_events.xml.gz").exists()) {
			eventsFile = runDirectory + runId + ".output_events.xml.gz";
		} else {
			log.info(runDirectory + runId + ".output_events.xml.gz not found. Trying to read file without runId prefix...");
			eventsFile = runDirectory + "output_events.xml.gz";
			log.info("Trying to read " + eventsFile + "...");
		}
		new CombinedPersonLinkMoneyEventsReader(events).readFile(eventsFile);
	}

}
		

