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
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.contrib.signals.otfvis.OTFVisWithSignalsLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import playground.dgrether.signalsystems.cottbus.CottbusFansControlerListener;
import playground.dgrether.signalsystems.cottbus.CottbusFootballAnalysisControllerListener;
import playground.dgrether.signalsystems.cottbus.footballdemand.CottbusFanCreator;
import playground.dgrether.signalsystems.cottbus.footballdemand.SimpleCottbusFanCreator;
import signals.CombinedSignalsModule;
import signals.laemmer.model.LaemmerConfig;
import signals.laemmer.model.LaemmerConfig.Regime;
import signals.laemmer.model.LaemmerConfig.StabilizationStrategy;
import signals.laemmer.model.util.Conflicts;
import signals.laemmer.model.util.ConflictsConverter;
import signals.laemmer.model.util.PsObjectAttributes;
import signals.sylvia.controler.DgSylviaConfig;
import utils.ModifyNetwork;

/**
 * @author tthunig, based on dgrether CottbusFootballBatch
 *
 */
public class RunCottbusFootball {
	private static final Logger LOG = Logger.getLogger(RunCottbusFootball.class);
	
	private enum SignalControl {FIXED, FIXED_IDEAL, SYLVIA, SYLVIA_IDEAL, LAEMMER_NICO, LAEMMER_DOUBLE, NONE, LAEMMER_FULLY_ADAPTIVE};
	private static final SignalControl CONTROL_TYPE = SignalControl.LAEMMER_NICO;
	private static final boolean CHECK_DOWNSTREAM = false;
	
	private static final boolean LONG_LANES = true;
	private static final double FLOW_CAP = 1.0;
	private static final int STUCK_TIME = 900;
	private static final boolean VIS = false;
	private static final boolean OVERWRITE_FILES = true;
	private static final int TIME_BIN_SIZE = 900;
	private static final boolean USE_MS_IDEAL_BASE_PLANS = true;
	private static final boolean USE_FIXED_NETWORK = true;
	
	private static final boolean REMOVE_ALL_LEGS_FROM_PLANS_AND_MODIFIY_ACTIVITIES_ON_MERGED_LINK = true;
	private static final boolean LAEMMER_LOG_ENABLED = false;
	private static final StabilizationStrategy STABILIZATION_STRATEGY = StabilizationStrategy.HEURISTIC;
	
	public static void main(String[] args) throws FileNotFoundException, IOException {		
		Config baseConfig;
		if (args != null && args.length != 0){
			baseConfig = ConfigUtils.loadConfig(args[0]);
		} else {
			String configFileName = "../../shared-svn/projects/cottbus/data/scenarios/cottbus_scenario/config.xml";
			baseConfig = ConfigUtils.loadConfig(configFileName);
			String scenarioDescription = "fixedNetwork"+USE_FIXED_NETWORK+"_flowCap" + FLOW_CAP + "_longLanes" + LONG_LANES + "_stuckTime" + STUCK_TIME+"_timeBinSize"+TIME_BIN_SIZE+"_signalControl-"+CONTROL_TYPE;
			if (CONTROL_TYPE == SignalControl.LAEMMER_FULLY_ADAPTIVE) {
				scenarioDescription = scenarioDescription.concat("-"+STABILIZATION_STRATEGY.name());
			}
			baseConfig.controler().setOutputDirectory("../../runs-svn/cottbus/football/" + scenarioDescription + "/run1200/");
			baseConfig.controler().setRunId("1200");
		}
		if (OVERWRITE_FILES) {
			baseConfig.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		}
		if (USE_FIXED_NETWORK) {
			baseConfig.getModules().get("network").getParams().put("inputNetworkFile", "network_wgs84_utm33n_link10284andReverseMerged.xml.gz");
			baseConfig.network().setInputFile("network_wgs84_utm33n_link10284andReverseMerged.xml.gz");
			baseConfig.getModules().get("network").getParams().put("laneDefinitionsFile", "lanes_link10284merged.xml");
			baseConfig.network().setLaneDefinitionsFile("lanes_link10284merged.xml");
		}
		
		baseConfig.controler().setLastIteration(0);
		SignalSystemsConfigGroup signalsConfigGroup = ConfigUtils.addOrGetModule(baseConfig, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
		switch (CONTROL_TYPE){
		case FIXED_IDEAL:
			signalsConfigGroup.setSignalControlFile("signal_control_no_13_idealized.xml");
			break;
		case SYLVIA:
			signalsConfigGroup.setSignalControlFile("signal_control_sylvia_no_13.xml");
			break;
		case SYLVIA_IDEAL:
			signalsConfigGroup.setSignalControlFile("signal_control_sylvia_no_13_idealized.xml");
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
		case LAEMMER_FULLY_ADAPTIVE:
			signalsConfigGroup.setSignalControlFile("signal_control_laemmer_fully_adaptive.xml");
			signalsConfigGroup.setSignalGroupsFile("signal_groups_no_13_laemmer_fully_adaptive.xml");
			//signalsConfigGroup.setSignalControlFile("signal_control_no_13.xml");
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

		if (USE_MS_IDEAL_BASE_PLANS) {
			baseConfig.plans().setInputFile("cb_spn_gemeinde_nachfrage_landuse_woMines/"
					+ "commuter_population_wgs84_utm33n_car_only_100it_MSideal_cap"+FLOW_CAP+"_tbs"+TIME_BIN_SIZE+".xml.gz");
		} else { 
			baseConfig.plans().setInputFile("cb_spn_gemeinde_nachfrage_landuse_woMines/"
					+ "commuter_population_wgs84_utm33n_car_only_100it_MS_cap"+FLOW_CAP+"_tbs"+TIME_BIN_SIZE+".xml.gz");
		}
				
		baseConfig.qsim().setFlowCapFactor(FLOW_CAP);
		baseConfig.qsim().setStorageCapFactor( FLOW_CAP / Math.pow(FLOW_CAP,1/4.) );
		baseConfig.travelTimeCalculator().setTraveltimeBinSize(TIME_BIN_SIZE);
		baseConfig.qsim().setStuckTime(STUCK_TIME);
		
		baseConfig.qsim().setEndTime(36*3600);
		
		Scenario baseScenario = ScenarioUtils.loadScenario(baseConfig);
		
//		if (REMOVE_ALL_LEGS_FROM_PLANS_AND_MODIFIY_ACTIVITIES_ON_MERGED_LINK) {
//			baseScenario.getPopulation().getPersons().values().forEach(person->{
//				person.getPlans().forEach(plan->{
//					List<PlanElement> allPlanElements = new LinkedList<PlanElement>(plan.getPlanElements());
//					allPlanElements.forEach(planElement->{
//						if(planElement instanceof Leg)
//							((Leg)planElement).setRoute(null); //plan.getPlanElements().remove(planElement);
//						if(planElement instanceof Activity) {
//							if (((Activity)planElement).getLinkId().equals(Id.createLinkId("8747")) || ((Activity)planElement).getLinkId().equals(Id.createLinkId("8745"))) {
//								((Activity)planElement).setLinkId(Id.createLinkId("10284"));
//							} else if (((Activity)planElement).getLinkId().equals(Id.createLinkId("8744")) || ((Activity)planElement).getLinkId().equals(Id.createLinkId("8746"))) {
//								((Activity)planElement).setLinkId(Id.createLinkId("10283"));
//							}
//						}
//					});
//				});
//			});
//				
//		}
	
		if (LONG_LANES){
			// extend short lanes (needed for laemmer)
			ModifyNetwork.lengthenAllLanes(baseScenario);
		}
		
		if (CONTROL_TYPE.equals(SignalControl.LAEMMER_FULLY_ADAPTIVE)) {
			ObjectAttributes conflictsObjectAttributes = new PsObjectAttributes();
			ObjectAttributesXmlReader conflictsReader = new ObjectAttributesXmlReader(conflictsObjectAttributes);
			conflictsReader.putAttributeConverter(signals.laemmer.model.util.Conflicts.class, new ConflictsConverter());
			conflictsReader.readFile(baseConfig.getContext().getPath().replaceAll("config.xml", "")+"conflicts.xml"); //TODO find correct path
			for (Entry<String, Map<String, Object>> e : ((PsObjectAttributes)conflictsObjectAttributes).getAttriutesAsEntrySet()) {
				Conflicts conflicts = ((Conflicts) e.getValue().get("conflicts"));
				if(conflicts.getLaneId() == null) {
					baseScenario.getNetwork().getLinks().get(conflicts.getLinkId()).getAttributes().putAttribute("conflicts", conflicts);
				} else {
					System.out.println("processing conflict for link "+conflicts.getLinkId()+" and lane "+conflicts.getLaneId());
					baseScenario.getLanes().getLanesToLinkAssignments().get(
							conflicts.getLinkId()).getLanes().get(
									conflicts.getLaneId()).getAttributes().
					putAttribute("conflicts", conflicts);
				}
			}
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
			baseOutputDirectory+= "sylvia_maxExt1.5_fixedCycle";
			break;
		case SYLVIA_IDEAL:
			baseOutputDirectory+= "sylviaIdeal_maxExt1.5_fixedCycle";
			break;
		case LAEMMER_NICO:
			baseOutputDirectory+= "laemmer_nicoGroups";
			break;
		case LAEMMER_DOUBLE:
			baseOutputDirectory+= "laemmer_doubleGroups";
			break;
		case LAEMMER_FULLY_ADAPTIVE:
			baseOutputDirectory+= "laemmer_fullyAdaptive";
			break;
		case NONE:
			baseOutputDirectory+= "noSignals";
			break;
		}
		if (USE_MS_IDEAL_BASE_PLANS) {
			baseOutputDirectory += "_MSidealPlans";
		} else {
			baseOutputDirectory += "_MSPlans";
		}
		
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
				CombinedSignalsModule signalsModule = new CombinedSignalsModule();
				DgSylviaConfig sylviaConfig = new DgSylviaConfig();
				sylviaConfig.setUseFixedTimeCycleAsMaximalExtension(false);
				sylviaConfig.setSignalGroupMaxGreenScale(1.5);
				sylviaConfig.setCheckDownstream(CHECK_DOWNSTREAM);
				signalsModule.setSylviaConfig(sylviaConfig);
				LaemmerConfig laemmerConfig = new LaemmerConfig();
				laemmerConfig.setDesiredCycleTime(90);
		        laemmerConfig.setMaxCycleTime(135);
		        laemmerConfig.setAnalysisEnabled(LAEMMER_LOG_ENABLED);
//				laemmerConfig.setMinGreenTime(5);
//				laemmerConfig.setAnalysisEnabled(true);
		        laemmerConfig.setActiveRegime(Regime.COMBINED);
				laemmerConfig.setCheckDownstream(CHECK_DOWNSTREAM);
				laemmerConfig.setActiveStabilizationStrategy(STABILIZATION_STRATEGY);
				signalsModule.setLaemmerConfig(laemmerConfig);
				controler.addOverridingModule(signalsModule);
			}
			
	        if (VIS) {
	            baseScenario.getConfig().qsim().setSnapshotStyle(QSimConfigGroup.SnapshotStyle.withHoles);
	            baseScenario.getConfig().qsim().setNodeOffset(5.);
	            OTFVisConfigGroup otfvisConfig = ConfigUtils.addOrGetModule(baseScenario.getConfig(), OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class);
	            otfvisConfig.setScaleQuadTreeRect(true);
//	            otfvisConfig.setColoringScheme(OTFVisConfigGroup.ColoringScheme.byId);
//	            otfvisConfig.setAgentSize(240);
	            controler.addOverridingModule(new OTFVisWithSignalsLiveModule());
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
