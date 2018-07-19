/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DefaultControlerModules.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package scenarios.cottbus.football;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.SignalSystemsConfigGroup.IntersectionLogic;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import playground.dgrether.signalsystems.cottbus.CottbusFansControlerListener;
import playground.dgrether.signalsystems.cottbus.CottbusFootballAnalysisControllerListener;
import playground.dgrether.signalsystems.cottbus.footballdemand.CottbusFanCreator;
import playground.dgrether.signalsystems.cottbus.footballdemand.SimpleCottbusFanCreator;
import signals.CombinedSignalsModule;
import signals.laemmer.model.LaemmerConfig;
import signals.laemmer.model.LaemmerConfig.Regime;
import signals.laemmer.model.LaemmerConfig.StabilizationStrategy;
import signals.sylvia.controler.DgSylviaConfig;
import utils.ModifyNetwork;

/**
 * @author tthunig, based on dgrether CottbusFootballBatch
 *
 */
public class TTRunCottbusFootball {
	private static final Logger LOG = Logger.getLogger(TTRunCottbusFootball.class);
	
	private enum SignalControl {FIXED, FIXED_IDEAL, SYLVIA, SYLVIA_IDEAL, LAEMMER_NICO, LAEMMER_DOUBLE, LAEMMER_NICO_GROUPS_14RE, LAEMMER_FLEXIBLE, NONE};
	private static final SignalControl CONTROL_TYPE = SignalControl.SYLVIA;
	private static final boolean CHECK_DOWNSTREAM = false;
	
	private static final boolean LONG_LANES = true;	
	private static final double FLOW_CAP = .7;
	private static final int STUCK_TIME = 120;
	private static final int TBS = 900;
	private static final String SIGNALS_BASE_CASE = "MS"; // the signals that were used for the base case plans
//	private static final String SIGNALS_BASE_CASE = "MSideal"; // the signals that were used for the base case plans
	private static final String NETWORK = "V4"; //"V1-2";
	private static final double EARLIEST_ARRIVAL_TIME_AT_STADIUM = 17 * 3600; // studies by DG and JB were made with 17. in contrast 16.5 will result in more interaction with the evening peak.
	
	public static void main(String[] args) throws FileNotFoundException, IOException {		
		Config baseConfig;
		if (args != null && args.length != 0){
			baseConfig = ConfigUtils.loadConfig(args[0]);
		} else {
			String configFileName = "../../shared-svn/projects/cottbus/data/scenarios/cottbus_scenario/config.xml";
			baseConfig = ConfigUtils.loadConfig(configFileName);
			String scenarioDescription = "flowCap" + FLOW_CAP + "_longLanes" + LONG_LANES + "_stuckTime" + STUCK_TIME + "_tbs" + TBS + "_basePlans" + SIGNALS_BASE_CASE + NETWORK;
			if (EARLIEST_ARRIVAL_TIME_AT_STADIUM != 17 * 3600) {
				scenarioDescription += "_earliestArrivalTimeOfFans" + EARLIEST_ARRIVAL_TIME_AT_STADIUM;
			}
			baseConfig.controler().setOutputDirectory("../../runs-svn/cottbus/football/" + scenarioDescription + "/");
			baseConfig.controler().setRunId("1200");
		}
		baseConfig.controler().setLastIteration(0);
		SignalSystemsConfigGroup signalsConfigGroup = ConfigUtils.addOrGetModule(baseConfig, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
		
		if (NETWORK.equals("V1-2")) {
			// modify the network and lanes file
			baseConfig.network().setInputFile("network_wgs84_utm33n_link10284andReverseMerged.xml");
			baseConfig.network().setLaneDefinitionsFile("lanes_link10284merged.xml");
		} else if (NETWORK.equals("V4")) {
			// modify network and lanes:
			baseConfig.network().setInputFile("network_wgs84_utm33n_v4.xml");
			baseConfig.network().setLaneDefinitionsFile("lanes_v1-4.xml");
			
			// base signal files:
			signalsConfigGroup.setSignalControlFile("signal_control_no_13_v4.xml");
			signalsConfigGroup.setSignalGroupsFile("signal_groups_no_13_v4.xml");
			signalsConfigGroup.setSignalSystemFile("signal_systems_no_13_v4.xml");
			signalsConfigGroup.setConflictingDirectionsFile("conflictData_fromBtu2018-05-03_basedOnMSconflicts_v4_modifiedBasedOnMS.xml");
			signalsConfigGroup.setIntersectionLogic(IntersectionLogic.CONFLICTING_DIRECTIONS_NO_TURN_RESTRICTIONS);
		}
		
		switch (CONTROL_TYPE){
		case FIXED_IDEAL:
			signalsConfigGroup.setSignalControlFile("signal_control_no_13_idealized.xml");
			break;
		case SYLVIA:
			if (NETWORK.equals("V4")) {
				signalsConfigGroup.setSignalControlFile("signal_control_sylvia_no_13_v4.xml");
			} else { // V1 or V1-2
				signalsConfigGroup.setSignalControlFile("signal_control_sylvia_no_13.xml");
			}
			break;
		case SYLVIA_IDEAL:
			signalsConfigGroup.setSignalControlFile("signal_control_sylvia_no_13_idealized.xml");
			break;
		case LAEMMER_NICO:
			signalsConfigGroup.setSignalControlFile("signal_control_laemmer.xml");
			signalsConfigGroup.setSignalGroupsFile("signal_groups_laemmer.xml");
//			signalsConfigGroup.setSignalGroupsFile("signal_groups_laemmerLinkBased.xml");
//			signalsConfigGroup.setSignalGroupsFile("signal_groups_laemmer2phases.xml");
			break;
		case LAEMMER_DOUBLE:
			signalsConfigGroup.setSignalControlFile("signal_control_laemmer.xml");
			signalsConfigGroup.setSignalGroupsFile("signal_groups_laemmer_doublePhases.xml");
			break;
		case LAEMMER_NICO_GROUPS_14RE:
			if (NETWORK.equals("V4")) {
				signalsConfigGroup.setSignalControlFile("signal_control_laemmer.xml");
				signalsConfigGroup.setSignalGroupsFile("signal_groups_laemmerNico_14restructurePhases_v4.xml");
			} else {
				throw new UnsupportedOperationException("not yet supported");
			}
			break;
		case LAEMMER_FLEXIBLE:
			if (NETWORK.equals("V4")) {
				signalsConfigGroup.setSignalControlFile("signal_control_laemmer_flexible.xml");
			} else {
				throw new UnsupportedOperationException("not yet supported. conflicts would need to be adapted for other network versions.");
			}
			break;
		case NONE:
			signalsConfigGroup.setUseSignalSystems(false);
			signalsConfigGroup.setSignalControlFile(null);
			signalsConfigGroup.setSignalGroupsFile(null);
			signalsConfigGroup.setSignalSystemFile(null);
			break;
		default:
			break;
		}
		
		
		String plansFile = "cb_spn_gemeinde_nachfrage_landuse_woMines/"
				+ "commuter_population_wgs84_utm33n_car_only_100it_"+SIGNALS_BASE_CASE+"_cap"+FLOW_CAP;
		if (TBS!=10) {
			plansFile += "_tbs"+TBS;
		}
		if (NETWORK!="V1") {
			plansFile += "_net"+NETWORK;
		}
		baseConfig.plans().setInputFile(plansFile+".xml.gz");
		
		baseConfig.qsim().setFlowCapFactor(FLOW_CAP);
		baseConfig.qsim().setStorageCapFactor( FLOW_CAP / Math.pow(FLOW_CAP,1/4.) );
		
		baseConfig.qsim().setStuckTime(STUCK_TIME);
		baseConfig.qsim().setEndTime(36*3600);
		
		Scenario baseScenario = ScenarioUtils.loadScenario(baseConfig);
		if (LONG_LANES){
			// extend short lanes (needed for laemmer)
			ModifyNetwork.lengthenAllLanes(baseScenario);
		}
		
		// add missing scenario elements
		if (!CONTROL_TYPE.equals(SignalControl.NONE))
			baseScenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(baseConfig).loadSignalsData());
		//create the output directoy
		String baseOutputDirectory = baseConfig.controler().getOutputDirectory();
		if (! baseOutputDirectory.endsWith("/")){
			baseOutputDirectory = baseOutputDirectory.concat("/");
		}
		switch (CONTROL_TYPE){
		case FIXED_IDEAL:
			baseOutputDirectory+= "fixedTimeIdeal";
			break;
		case FIXED:
			baseOutputDirectory+= "fixedTime"; 
			break;
		case SYLVIA:
			baseOutputDirectory+= "sylvia_maxExt1.5_noFixedCycle";
			break;
		case SYLVIA_IDEAL:
			baseOutputDirectory+= "sylviaIdeal_maxExt1.5_noFixedCycle";
			break;
		case LAEMMER_NICO:
			baseOutputDirectory+= "laemmer_nicoGroups";
			break;
		case LAEMMER_DOUBLE:
			baseOutputDirectory+= "laemmer_doubleGroups";
			break;
		case LAEMMER_NICO_GROUPS_14RE:
			baseOutputDirectory+= "laemmer_nicoGroups14re";
			break;
		case LAEMMER_FLEXIBLE:
			baseOutputDirectory+= "laemmer_flexible";
			break;
		case NONE:
			baseOutputDirectory+= "noSignals";
			break;
		}
		baseOutputDirectory += "_"+SIGNALS_BASE_CASE+"Plans";
		baseOutputDirectory+= "/";
		LOG.info("using base output directory: " + baseOutputDirectory);
		Population fanPop = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation();
		//initialize variables needed in the loop
		String runId = baseConfig.controler().getRunId();
		Map<Integer, Double> percentageOfFans2AverageTTMap = new HashMap<>();
		Map<Integer, Double> percentageOfFans2TotalTTMap = new HashMap<>();
		Map<Integer, Double> percentageOfFans2noStuckedAgents = new HashMap<>();
		//fan creator
		String kreisShapeFile = "../../shared-svn/studies/countries/de/brandenburg_gemeinde_kreisgrenzen/kreise/dlm_kreis.shp";
		CottbusFanCreator fanCreator = new SimpleCottbusFanCreator(kreisShapeFile);
		fanCreator.setEarliestArrivalTimeAtStadium(EARLIEST_ARRIVAL_TIME_AT_STADIUM);
		//start the runs
		int increment = 5;
		for (int numberOfFootballFans = 0; numberOfFootballFans <= 100; numberOfFootballFans = numberOfFootballFans + increment){
			if (numberOfFootballFans != 0) {
				// create additional football fans (from 0 to 2000)
				Population p = fanCreator.createAndAddFans(baseScenario, 20 * increment);
				for (Person pers : p.getPersons().values()){
					fanPop.addPerson(pers);
				}
			}
		
			baseConfig.controler().setOutputDirectory(baseOutputDirectory + numberOfFootballFans + "_football_fans/");
			baseConfig.controler().setRunId(runId + "_" + numberOfFootballFans + "_football_fans");
			Controler controler = new Controler(baseScenario);
			controler.addControlerListener(new CottbusFansControlerListener(fanPop));
			//add average tt handler for football fans
			CottbusFootballAnalysisControllerListener cbfbControllerListener = new CottbusFootballAnalysisControllerListener();
			controler.addControlerListener(cbfbControllerListener);
			//add the signals module
			if (!CONTROL_TYPE.equals(SignalControl.NONE)) {
				//TODO adapt here
				CombinedSignalsModule signalsModule = new CombinedSignalsModule();
				DgSylviaConfig sylviaConfig = new DgSylviaConfig();
				sylviaConfig.setUseFixedTimeCycleAsMaximalExtension(false);
				sylviaConfig.setSignalGroupMaxGreenScale(1.5);
				sylviaConfig.setCheckDownstream(CHECK_DOWNSTREAM);
				signalsModule.setSylviaConfig(sylviaConfig);
				LaemmerConfig laemmerConfig = new LaemmerConfig();
				laemmerConfig.setDesiredCycleTime(90);
		        laemmerConfig.setMaxCycleTime(135);
				laemmerConfig.setMinGreenTime(5);
//				laemmerConfig.setAnalysisEnabled(true);
//		        laemmerConfig.setActiveRegime(Regime.STABILIZING);		       
//		        laemmerConfig.setActiveRegime(Regime.OPTIMIZING);
				laemmerConfig.setActiveStabilizationStrategy(StabilizationStrategy.USE_MAX_LANECOUNT);
				laemmerConfig.setCheckDownstream(CHECK_DOWNSTREAM);
				signalsModule.setLaemmerConfig(laemmerConfig);
				controler.addOverridingModule(signalsModule);
			}
			
			controler.run();
			if (cbfbControllerListener.getAverageTraveltime() != null){
				percentageOfFans2AverageTTMap.put(numberOfFootballFans, cbfbControllerListener.getAverageTraveltime());
				percentageOfFans2TotalTTMap.put(numberOfFootballFans, cbfbControllerListener.getTotalTraveltime());
				percentageOfFans2noStuckedAgents.put(numberOfFootballFans, cbfbControllerListener.getNumberOfStuckedPersons() + 0.);
			}
		}
		
//		try {
//			new SelectedPlans2ESRIShape(baseScenario.getPopulation(), baseScenario.getNetwork(), MGC.getCRS(TransformationFactory.WGS84_UTM33N), "/media/data/work/matsimOutput/run1219/" ).write();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		
		writeAnalysis(percentageOfFans2AverageTTMap, baseOutputDirectory + "average_traveltimes_last_iteration.csv", "Average travel time");
		writeAnalysis(percentageOfFans2TotalTTMap, baseOutputDirectory + "total_traveltimes_last_iteration.csv", "Total travel time");
		writeAnalysis(percentageOfFans2noStuckedAgents, baseOutputDirectory + "numberOfStuckedAgents_last_iteration.csv", "Number of stucked agents");
	}
		
	private static void writeAnalysis(Map<Integer, Double> map, String filename, String headerColumn2) throws FileNotFoundException, IOException{
		SortedMap<Integer, Double> sorted = new TreeMap<>();
		sorted.putAll(map);
		BufferedWriter writer = IOUtils.getBufferedWriter(filename);
		writer.write("Football fans %\t" + headerColumn2);
		writer.newLine();
		for (Entry<Integer, Double> e : sorted.entrySet()){
			writer.write(e.getKey().toString() + "\t" + e.getValue().toString());
			writer.newLine();
		}
		writer.close();
	}

	
}
