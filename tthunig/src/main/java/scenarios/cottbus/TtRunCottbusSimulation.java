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
package scenarios.cottbus;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.contrib.decongestion.DecongestionConfigGroup;
import org.matsim.contrib.decongestion.DecongestionConfigGroup.DecongestionApproach;
import org.matsim.contrib.decongestion.DecongestionModule;
import org.matsim.contrib.decongestion.routing.TollTimeDistanceTravelDisutilityFactory;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.SignalSystemsConfigGroup.IntersectionLogic;
import org.matsim.contrib.signals.analysis.SignalAnalysisTool;
import org.matsim.contrib.signals.builder.Signals.Configurator;
import org.matsim.contrib.signals.controller.laemmerFix.LaemmerConfigGroup;
import org.matsim.contrib.signals.controller.laemmerFix.LaemmerConfigGroup.StabilizationStrategy;
import org.matsim.contrib.signals.controller.sylvia.SylviaConfigGroup;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlWriter20;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupSettingsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsWriter20;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalSystemControllerData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsWriter20;
import org.matsim.contrib.signals.otfvis.OTFVisWithSignalsLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup.TravelTimeCalculatorType;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultSelector;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.lanes.LanesUtils;
import org.matsim.lanes.LanesWriter;
import org.matsim.roadpricing.RoadPricingConfigGroup;
import org.matsim.roadpricing.RoadPricingModule;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import analysis.TtAnalyzedGeneralResultsWriter;
import analysis.TtGeneralAnalysis;
import analysis.TtListenerToBindGeneralAnalysis;
import analysis.TtSubnetworkAnalysisWriter;
import analysis.TtSubnetworkAnalyzer;
import analysis.cten.TtCommodityTravelTimeAnalyzer;
import analysis.cten.TtWriteComAnalysis;
import analysis.signals.SignalAnalysisListener;
import analysis.signals.SignalAnalysisWriter;
import analysis.signals.TtQueueLengthAnalysisTool;
import playground.vsp.congestion.controler.MarginalCongestionPricingContolerListener;
import playground.vsp.congestion.handlers.CongestionHandlerImplV10;
import playground.vsp.congestion.handlers.CongestionHandlerImplV3;
import playground.vsp.congestion.handlers.CongestionHandlerImplV4;
import playground.vsp.congestion.handlers.CongestionHandlerImplV7;
import playground.vsp.congestion.handlers.CongestionHandlerImplV8;
import playground.vsp.congestion.handlers.CongestionHandlerImplV9;
import playground.vsp.congestion.handlers.TollHandler;
import playground.vsp.congestion.routing.CongestionTollTimeDistanceTravelDisutilityFactory;
import signals.downstreamSensor.DownstreamPlanbasedSignalController;
import signals.gershenson.GershensonConfig;
import signals.gershenson.GershensonSignalController;
import signals.laemmerFlex.FullyAdaptiveLaemmerSignalController;
import utils.ModifyNetwork;
import utils.ModifyPopulation;
import utils.OutputUtils;
import utils.SignalizeScenario;

/**
 * Class to run a cottbus simulation.
 * 
 * @author tthunig
 *
 */
public class TtRunCottbusSimulation {

	private static final Logger LOG = Logger.getLogger(TtRunCottbusSimulation.class);
	
	private final static String RUN_ID = "1000";
	
	private final static NetworkType NETWORK_TYPE = NetworkType.BTU_NET;
	public enum NetworkType {
		BTU_NET, // "network small simplified" in BTU_BASE_DIR
		V1, // network of the public-svn scenario from 2016-03-18 (same as from DG)
		V1_1, // same as V1 except merged links 6724 and 6708. should only have effect on sensor prediction for adaptive signals but not on fixed time signals
		V1_2, // same as V1 except merged links 10284-8747-8745 and reverse
		V1_3, // some links infront of signals merged (see mergedLinksForV1-34.xls)
		V1_4, // 6 more links merged than in V1_3 (see mergedLinksForV1-34.xls)
		V2, // add missing highway part, add missing links, correct directions, add missing signal
		V21, // add missing lanes
		V3, // double flow capacities of all signalized links and lanes
		V4, // V1-4 plus: move link at 5-approach-intersection system 24; add missing signal and link at system 19
		V4_1, // V4 but with simplified/corrected to-links of lanes, such that it is more similar to the cten model. some lanes also had to be created newly, such that signal systems and groups also had to be adjusted.
	}
	private final static boolean LONG_LANES = true;
	private final static boolean LANE_CAP_FROM_NETWORK = false;
	
	private final static PopulationType POP_TYPE = PopulationType.BTU_POP_MATSIM_ROUTES;
	public enum PopulationType {
		GRID_LOCK_BTU, // artificial demand: from every ingoing link to every outgoing link of the inner city ring
		BTU_POP_MATSIM_ROUTES,
		BTU_POP_BTU_ROUTES, BTU_POP_BTU_ROUTES_90,
		WMines, // with mines as working places. causes an oversized number of working places in the south west of Cottbus.
		WoMines, // without mines as working places
		WoMines100itcap1MS, // without mines. iterated for 100it with capacity 1.0 and signals MS
		WoMines100itcap07MS, // without mines. iterated for 100it with capacity 0.7 and signals MS
		WoMines100itcap07MStbs300, // same as above just with tbs 300 instead of 10
		WoMines100itcap1MStbs300,
		WoMines100itcap07MStbs900, // without mines. 100it. capacity 0.7. MS signals. tbs 900. stuckTime 120
		WoMines100itcap07MStbs900stuck600, // same as above, just with stuckTime 600
		WoMines100itcap07MSRand, // same as prev, but with signals MS_RANDOM_OFFSETS
		WoMines100itcap07MSideal, // without mines. iterated for 100it with capacity 0.7 and adapted MS signals (fixed intergreen... comparable to laemmer)
		WoMines100itcap1MSideal,
		WoMines100itcap07MSidealNetV1_1, // stuck120 tbs900 network V1_1 with merged links at sys23.
		WoMines100itcap1MSidealNetV1_2, // stuck120 tbs900 networkV1_2 with merged links around 10284. cap1.0
		WoMines100itcap07MSidealNetV1_2, // stuck120 tbs900 networkV1_2 with merged links around 10284. cap0.7
		WoMines100itcap1MSNetV1_2, // stuck120 tbs900 networkV1_2 with merged links around 10284. cap1.0
		WoMines100itcap07MSNetV1_2, // stuck120 tbs900 networkV1_2 with merged links around 10284. cap0.7
		WoMines100itcap1MSNetV1_3,
		WoMines100itcap07MSNetV1_3,
		WoMines100itcap05MSNetV1_3,
		WoMines100itcap13MSNetV1_4,
		WoMines100itcap1MSNetV1_4,
		WoMines100itcap07MSNetV1_4,
		WoMines100itcap05MSNetV1_4,
		WoMines100itcap05MSNetV4,
		WoMines100itcap07MSNetV4,
		WoMines100itcap09MSNetV4,
		WoMines100itcap10MSNetV4,
		WoMines100itcap07MSNetV4_1,
		WoMines1000itcap03MSNetV4_1,
		WoMines1000itcap04MSNetV4_1,
		WoMines1000itcap05MSNetV4_1,
		WoMines1000itcap06MSNetV4_1,
		WoMines1000itcap07MSNetV4_1, WoMines1000itcap07MSNetV4_1_morning,
		WoMines1000itcap08MSNetV4_1,
		WoMines1000itcap09MSNetV4_1,
		WoMines1000itcap10MSNetV4_1,
		NicoOutputPlans // the plans that nico used in his MA: netV1, MS, 100it
	}
	private final static int POP_SCALE = 1;
	private final static boolean DELETE_ROUTES = false;
	
	private static SignalType SIGNAL_TYPE = SignalType.MS_RANDOM_GREENSPLITS;
	public enum SignalType {
		NONE, MS, MS_RANDOM_OFFSETS, MS_RANDOM_GREENSPLITS, MS_SYLVIA, MS_BTU_OPT, MS_BTU_OPT_SYLVIA, DOWNSTREAM_MS, DOWNSTREAM_BTUOPT, DOWNSTREAM_ALLGREEN, 
		MS_INTG0, MS_INTG0_SYLVIA, // MS with modified end times, such that zero intergreen times are used
		ALL_NODES_ALL_GREEN, ALL_NODES_DOWNSTREAM, ALL_GREEN_INSIDE_ENVELOPE, 
		ALL_MS_INSIDE_ENVELOPE_REST_GREEN, // all MS systems fixed-time, rest all green
		ALL_MS_AS_SYLVIA_INSIDE_ENVELOPE_REST_GREEN, // all MS systems as sylvia with MS basis, rest all green. note: green basis for sylvia does not work
		ALL_MS_AS_DOWNSTREAM_INSIDE_ENVELOPE_REST_GREEN, // all MS systems as downstream with MS basis, rest all green
		ALL_DOWNSTREAM_INSIDE_ENVELOPE_BASIS_MS, // all MS systems as downstream with MS basis, rest downstream with green basis
		ALL_DOWNSTREAM_INSIDE_ENVELOPE_BASIS_GREEN, // all systems inside envelope downstream with green basis
		ALL_MS_AS_DOWNSTREAM_BASIS_GREEN_INSIDE_ENVELOPE_REST_GREEN, // all MS systems as downstream with green basis, rest all green
		LAEMMER_NICO_GROUPS, // laemmer with the fixed signal groups, that nico defined in his MA. except: bug fix in system 1 and 5 (1905 was included twice, 1902 forgotten; 1802 included twice, 1803 forgotten)
		LAEMMER_NICO_GROUPS_14GREEN, // the same as LAEMMER_NICO_GROUPS but without signal 1107 at system 14 (i.e. all green)
		LAEMMER_NICO_GROUPS_14RE, // same as LAEMMER_NICO_GROUPS but with different phases at system 14: left turns together, straight together
		LAEMMER_NICO_GROUPS_14RE_6RE, // same as LAEMMER_NICO_GROUPS_14RE but with restructured phases at system 6: 1509 together with 1511 and 1512
		LAEMMER_NICO_GROUPS_14RE_3RE_7RE, // same as LAEMMER_NICO_GROUPS_14RE but with separate signal groups for conflicting left turns at sys 3 and 7
		LAEMMER_DOUBLE_GROUPS, // laemmer with fixed signal groups, where signals can be included more than once, i.e. alternative groups can be modeled
		LAEMMER_DOUBLE_GROUPS_SYS17, // as above but two additional possible groups at system 17, such that opposing traffic can have green at the same time
		LAEMMER_DOUBLE_GROUPS_14GREEN, // the same as LAEMMER_DOUBLE_GROUPS but without signal 1107 at system 14 (i.e. all green)
		MS_IDEAL, // fixed-time signals based on MS optimization but with idealized signal timings to be more comparable: intergreen time of 5 seconds always, phases like for laemmer double groups
		LAEMMER_FLEXIBLE, // version implemented by pierre schade in his thesis
		GERSHENSON,
		QUEUE_LEARNING
	}
	
	// parameters for specific signal control
	private final static boolean SYLVIA_FIXED_CYCLE = false;
	private final static double SYLVIA_MAX_EXTENSION = 1.5;
	private final static StabilizationStrategy LAEMMER_FLEX_STAB_STRATEGY = StabilizationStrategy.USE_MAX_LANECOUNT;
	private final static int LAEMMER_MIN_G = 6;
	private final static int INTERGREEN = 3;
	
//	private static final IntersectionLogic INTERSECTION_LOGIC = IntersectionLogic.CONFLICTING_DIRECTIONS_NO_TURN_RESTRICTIONS;
	private static final IntersectionLogic INTERSECTION_LOGIC = IntersectionLogic.NONE;
	
	// defines which kind of pricing should be used
	private static final PricingType PRICING_TYPE = PricingType.INTERVAL_BASED;
	private enum PricingType {
		NONE, CP_V3, CP_V4, CP_V7, CP_V8, CP_V9, CP_V10, FLOWBASED, CORDON_INNERCITY, CORDON_RING, INTERVAL_BASED
	}
	
	private static final boolean USE_OPDYTS = false;
	private static final boolean VIS = false;
	
	// choose a sigma for the randomized router
	// (higher sigma cause more randomness. use 0.0 for no randomness.)
	private static final double SIGMA = 0.0;
	
	private static String OUTPUT_BASE_DIR = "../../runs-svn/cottbus/randomGreensplits/";
	private static String INPUT_BASE_DIR = "../../shared-svn/projects/cottbus/data/scenarios/cottbus_scenario/";
//	private static final String BTU_BASE_DIR = "../../shared-svn/projects/cottbus/data/optimization/cb2ks2010/2015-02-25_minflow_50.0_morning_peak_speedFilter15.0_SP_tt_cBB50.0_sBB500.0/";
//	private static final String BTU_BASE_DIR = "../../shared-svn/projects/cottbus/data/optimization/cb2ks2010/2018-06-7_minflow_50.0_time19800.0-34200.0_speedFilter15.0_SP_tt_cBB50.0_sBB500.0/";
//	private static final String BTU_BASE_DIR = "../../shared-svn/projects/cottbus/data/optimization/cb2ks2010/2018-09-20_minflow_50.0_time19800.0-34200.0_speedFilter15.0_SP_tt_cBB50.0_sBB500.0/";
	private static final String BTU_BASE_DIR = "../../shared-svn/projects/cottbus/data/optimization/cb2ks2010/2018-11-13_minflow_50.0_time19800.0-34200.0_speedFilter15.0_SP_tt_cBB50.0_sBB500.0/"; // random green splits
//	private static final String BTU_BASE_DIR = "../../shared-svn/projects/cottbus/data/optimization/cb2ks2010/2018-11-20-v1_minflow_50.0_time19800.0-34200.0_speedFilter15.0_SP_tt_cBB50.0_sBB500.0/";
//	private static final String BTU_BASE_DIR = "../../shared-svn/projects/cottbus/data/optimization/cb2ks2010/2018-11-20-v2_minflow_50.0_time19800.0-34200.0_speedFilter15.0_SP_tt_cBB50.0_sBB500.0/";
//	private static final String BTU_BASE_DIR = "../../shared-svn/projects/cottbus/data/optimization/cb2ks2010/2018-11-20-v3_minflow_50.0_time19800.0-34200.0_speedFilter15.0_SP_tt_cBB50.0_sBB500.0/";
	private static final NetworkType BTU_BASE_NET = NetworkType.V4_1;
	private static String RUNS_SVN = "../../runs-svn/cottbus/";
	
	private static final boolean WRITE_INITIAL_FILES = false;
	private static final boolean USE_COUNTS = false;
	private static double SCALING_FACTOR = 1.;
	
	private static TtGeneralAnalysis ANALYSIS_TOOL;
	
	public static void main(String[] args) throws IOException {
		
//		boolean useMSA = false;
//		int opdytsIt = 30;
//		int stepSize = 20;
//		double selfTunWt = 1;
//		int warmUpIt = 2;
//		if (args != null && args.length > 0) {
//			OUTPUT_BASE_DIR = args[0];
//			useMSA = Boolean.valueOf(args[1]);
//			opdytsIt = Integer.valueOf(args[2]);
//			stepSize = Integer.valueOf(args[3]);
//			selfTunWt = Double.valueOf(args[4]);
//			warmUpIt = Integer.valueOf(args[5]);
//		}

		// for runs on the cluster. note: start from jobfiles directory in shared-svn, ie. 5x to runs-svn, 4x to projects
		if (args != null && args.length > 0) {
			OUTPUT_BASE_DIR = args[0];
			INPUT_BASE_DIR = args[1];
			RUNS_SVN = args[2];
			SCALING_FACTOR = Double.valueOf(args[3]);
			SIGNAL_TYPE = SignalType.valueOf(args[4]);
		}
		
		// prepare output for random greensplits
		FileWriter fw = null;
//		Integer[] signalCoords = {12, 14, 19, 20, 26, 31, 34, 37, 43, 45, 47};
//		Integer[] signalCoords = {37, 43, 45, 47};
//		Integer[] signalCoords = {25, 35};
//		Integer[] signalCoords = {20, 14};
//		Integer[] signalCoords = {20, 14, 25, 35};
		Integer[] signalCoords = {5, 14, 19, 20, 25};
		List<Integer> signalCoordsList = Arrays.asList(signalCoords);
		if (SIGNAL_TYPE.equals(SignalType.MS_RANDOM_GREENSPLITS)) {
			// TODO
			fw = new FileWriter(new File( BTU_BASE_DIR + "randoms/tt_matsim_bla_cap10_200it_beta2_btuRoutes.txt"));
			fw.write("coord \ttotal_tt[s] \ttotal_delay[s] \ttotal_dist[m]\n");
		}

		// loop only needed for multiple runs, e.g. random signal coordinations (greensplits)
//		for (int signalCoordIndex = 0; signalCoordIndex <= 49; signalCoordIndex++) {
//		for (int i=0; i<signalCoords.length; i++) {
//			if (signalCoordsList.contains(signalCoordIndex)) {
//				continue;
//			}
//			int signalCoordIndex = signalCoords[i];
			int signalCoordIndex = 14;

			Config config = defineConfig(signalCoordIndex);

			if (VIS) {
				OTFVisConfigGroup otfvisConfig = ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.class);
				otfvisConfig.setDrawTime(true);
				otfvisConfig.setAgentSize(80f);
				config.qsim().setSnapshotStyle(QSimConfigGroup.SnapshotStyle.withHoles);
			}

			Scenario scenario = prepareScenario(config, signalCoordIndex);

			if (USE_OPDYTS) {
				throw new UnsupportedOperationException("this code has to be adapted to changes in opdyts");
				// OpdytsConfigGroup opdytsConfigGroup =
				// ConfigUtils.addOrGetModule(scenario.getConfig(), OpdytsConfigGroup.class);
				//
				// opdytsConfigGroup.setNumberOfIterationsForAveraging(5); // 2
				// opdytsConfigGroup.setNumberOfIterationsForConvergence(10); // 5
				//
				// // TODO set useMSA flag
				// opdytsConfigGroup.setMaxIteration(opdytsIt);
				// opdytsConfigGroup.setOutputDirectory(scenario.getConfig().controler().getOutputDirectory());
				// opdytsConfigGroup.setDecisionVariableStepSize(stepSize);
				// opdytsConfigGroup.setUseAllWarmUpIterations(false);
				// opdytsConfigGroup.setWarmUpIterations(warmUpIt); // 1 this should be tested
				// (parametrized).
				// opdytsConfigGroup.setPopulationSize(1);
				// opdytsConfigGroup.setSelfTuningWeight(selfTunWt);
				//
				// MATSimOpdytsControler<OffsetDecisionVariable> runner = new
				// MATSimOpdytsControler<>(scenario);
				//
				// MATSimSimulator2<OffsetDecisionVariable> simulator = new
				// MATSimSimulator2<>(new MATSimStateFactoryImpl<>(), scenario);
				// simulator.addOverridingModule(new AbstractModule() {
				// @Override
				// public void install() {
				// // this can later be accessed by TravelTimeObjectiveFunction, because it is
				// bind inside MATSimSimulator2
				// bind(TtTotalTravelTime.class).asEagerSingleton();
				// addEventHandlerBinding().to(TtTotalTravelTime.class);
				//
				// // bind amits analysis
				// bind(ModalTripTravelTimeHandler.class);
				// addControlerListenerBinding().to(ModalTravelTimeControlerListener.class);
				//
				// // bind general analysis
				// this.bind(TtGeneralAnalysis.class);
				// this.bind(TtAnalyzedGeneralResultsWriter.class);
				// this.addControlerListenerBinding().to(TtListenerToBindGeneralAnalysis.class);
				//
				// // bind tool to analyze signals
				// this.bind(TtSignalAnalysisTool.class);
				// this.bind(TtSignalAnalysisWriter.class);
				// this.addControlerListenerBinding().to(TtSignalAnalysisListener.class);
				//
				// this.addControlerListenerBinding().to(OpdytsOffsetStatsControlerListener.class);
				//
				// // plot only after one opdyts transition:
				// addControlerListenerBinding().toInstance(new ShutdownListener() {
				// @Override
				// public void notifyShutdown(ShutdownEvent event) {
				// // post-process analysis
				// String opdytsConvergenceFile = config.controler().getOutputDirectory() +
				// "/opdyts.con";
				// if (new File(opdytsConvergenceFile).exists()) {
				// OpdytsConvergenceChart opdytsConvergencePlotter = new
				// OpdytsConvergenceChart();
				// opdytsConvergencePlotter.readFile(config.controler().getOutputDirectory() +
				// "/opdyts.con");
				// opdytsConvergencePlotter.plotData(config.controler().getOutputDirectory() +
				// "/convergence.png");
				// }
				// }
				// });
				// }
				// });
				// simulator.addOverridingModule(new SignalsModule());
				// runner.addNetworkModeOccupancyAnalyzr(simulator);
				//
				// runner.run(simulator, new OffsetRandomizer(scenario), new
				// OffsetDecisionVariable(
				// ((SignalsData)
				// scenario.getScenarioElement(SignalsData.ELEMENT_NAME)).getSignalControlData(),
				// scenario), new TravelTimeObjectiveFunction());
			} else {
				// start a normal matsim run without opdyts:
				Controler controler = prepareController(scenario);

				if (VIS) {
					controler.addOverridingModule(new OTFVisWithSignalsLiveModule());
				}

				controler.run();

				// write output for random greensplits
				if (SIGNAL_TYPE.equals(SignalType.MS_RANDOM_GREENSPLITS)) {
					fw.write("coord" + signalCoordIndex + "\t" + ANALYSIS_TOOL.getTotalTt() + "\t"
							+ ANALYSIS_TOOL.getTotalDelay() + "\t" + ANALYSIS_TOOL.getTotalDistance() + "\n");
				}
			}
//		} // loop for multiple runs, e.g. random greensplits
		if (SIGNAL_TYPE.equals(SignalType.MS_RANDOM_GREENSPLITS)) {
			fw.close();
		}
		System.out.println("done :)");
	}

	private static Config defineConfig(int signalCoordIndex) {
		Config config = ConfigUtils.createConfig();

		switch (NETWORK_TYPE) {
		case BTU_NET:
			config.network().setInputFile(BTU_BASE_DIR + "network_small_simplified.xml.gz");
			config.network().setLaneDefinitionsFile(BTU_BASE_DIR + "lanes_network_small.xml.gz");
			break;
		case V1:
			config.network().setInputFile(INPUT_BASE_DIR + "network_wgs84_utm33n.xml.gz");
			config.network().setLaneDefinitionsFile(INPUT_BASE_DIR + "lanes.xml");
			break;
		case V1_1:
			config.network().setInputFile(INPUT_BASE_DIR + "network_wgs84_utm33n_sys23linksMerged.xml");
			config.network().setLaneDefinitionsFile(INPUT_BASE_DIR + "lanes_sys23linksMerged.xml");
			break;
		case V1_2:
			config.network().setInputFile(INPUT_BASE_DIR + "network_wgs84_utm33n_link10284andReverseMerged.xml");
			config.network().setLaneDefinitionsFile(INPUT_BASE_DIR + "lanes_link10284merged.xml");
			break;
		case V1_3:
			config.network().setInputFile(INPUT_BASE_DIR + "network_wgs84_utm33n_v1-3.xml");
			config.network().setLaneDefinitionsFile(INPUT_BASE_DIR + "lanes_v1-3.xml");
			break;
		case V1_4:
			config.network().setInputFile(INPUT_BASE_DIR + "network_wgs84_utm33n_v1-4.xml");
			config.network().setLaneDefinitionsFile(INPUT_BASE_DIR + "lanes_v1-4.xml");
			break;
		case V2:
			config.network().setInputFile(INPUT_BASE_DIR + "network_wgs84_utm33n_v2.xml.gz");
			config.network().setLaneDefinitionsFile(INPUT_BASE_DIR + "lanes.xml");
			break;
		case V21:
			config.network().setInputFile(INPUT_BASE_DIR + "network_wgs84_utm33n_v2.xml");
			config.network().setLaneDefinitionsFile(INPUT_BASE_DIR + "lanes_v2.1.xml");
			break;
		case V3:
			config.network().setInputFile(INPUT_BASE_DIR + "network_wgs84_utm33n_v3.xml.gz");
			config.network().setLaneDefinitionsFile(INPUT_BASE_DIR + "lanes_v3.xml");
			break;
		case V4:
			config.network().setInputFile(INPUT_BASE_DIR + "network_wgs84_utm33n_v4.xml");
			config.network().setLaneDefinitionsFile(INPUT_BASE_DIR + "lanes_v1-4.xml");
			break;
		case V4_1:
			config.network().setInputFile(INPUT_BASE_DIR + "network_wgs84_utm33n_v4.xml");
			config.network().setLaneDefinitionsFile(INPUT_BASE_DIR + "lanes_v4-1.xml");
			break;
		default:
			throw new RuntimeException("Network type not specified!");
		}
		if (SIGNAL_TYPE.toString().startsWith("ALL") && !SIGNAL_TYPE.toString().contains("MS")){
			// if signal type 'All...' without 'MS' is used, lanes are defined later in 'prepareScenario'
			config.network().setLaneDefinitionsFile(null);
		}
		
		switch (POP_TYPE) {
		case BTU_POP_MATSIM_ROUTES:
			// TODO
			config.plans().setInputFile(BTU_BASE_DIR + "trip_plans_from_morning_peak_ks_commodities_minFlow50.0.xml");
			
//			switch (signalCoordIndex) {
//			case 5:
//				config.plans().setInputFile(OUTPUT_BASE_DIR + "2018-11-30-0-18-55_coord5_3000it/ITERS/it.2400/1000.2400.plans.xml.gz");
//				break;
//			case 12:
////				config.plans().setInputFile(OUTPUT_BASE_DIR + "2018-11-27-14-9-15_coord12_1000it/ITERS/it.800/1000.800.plans.xml.gz");
//				config.plans().setInputFile(OUTPUT_BASE_DIR + "2018-11-27-14-9-15_coord12_1000it/1000.output_plans.xml.gz");
//				break;
//			case 14:
//				config.plans().setInputFile(OUTPUT_BASE_DIR + "2018-11-22-15-10-47_coord14_2000it/1000.output_plans.xml.gz");
//				break;
//			case 19:
//				config.plans().setInputFile(OUTPUT_BASE_DIR + "2018-11-28-20-53-21_coord19_1000it/1000.output_plans.xml.gz");
//				break;
//			case 20:
////				config.plans().setInputFile(OUTPUT_BASE_DIR + "2018-11-28-23-10-52_coord20_1000it/ITERS/it.800/1000.800.plans.xml.gz");
//				config.plans().setInputFile(OUTPUT_BASE_DIR + "2018-11-30-2-18-8_coord20_add1000it/1000.output_plans.xml.gz");
//				break;
//			case 26:
//				config.plans().setInputFile(OUTPUT_BASE_DIR + "2018-11-29-8-9-3_coord26_1000it/1000.output_plans.xml.gz");
//				break;
//			case 28:
////				config.plans().setInputFile(OUTPUT_BASE_DIR + "2018-11-29-11-45-44_coord28_1000it/ITERS/it.800/1000.800.plans.xml.gz");
//				config.plans().setInputFile(OUTPUT_BASE_DIR + "2018-11-29-11-45-44_coord28_1000it/1000.output_plans.xml.gz");
//				break;
//			case 31:
//				config.plans().setInputFile(OUTPUT_BASE_DIR + "2018-11-29-2-13-4_coord31_1000it/1000.output_plans.xml.gz");
//				break;
//			case 34:
//				config.plans().setInputFile(OUTPUT_BASE_DIR + "2018-11-29-6-11-55_coord34_1000it/1000.output_plans.xml.gz");
//				break;
//			case 37:
//				config.plans().setInputFile(OUTPUT_BASE_DIR + "2018-11-29-11-21-46_coord37_1000it/1000.output_plans.xml.gz");
//				break;
//			case 43:
//				config.plans().setInputFile(OUTPUT_BASE_DIR + "2018-11-29-19-51-39_coord43_1000it/1000.output_plans.xml.gz");
//				break;
//			case 45:
//				config.plans().setInputFile(OUTPUT_BASE_DIR + "2018-11-29-22-54-11_coord45_1000it/1000.output_plans.xml.gz");
//				break;
//			case 47:
//				config.plans().setInputFile(OUTPUT_BASE_DIR + "2018-11-29-17-21-53_coord47_1000it/1000.output_plans.xml.gz");
//				break;
//			default:
//				throw new RuntimeException("not implemented");
//			}
			break;
		case BTU_POP_BTU_ROUTES:
			// TODO
//			config.plans().setInputFile(BTU_BASE_DIR + "routeComparison/2015-03-10_sameEndTimes_ksOptRouteChoice_paths.xml");
//			config.plans().setInputFile(BTU_BASE_DIR + "btu/2018-05-17_sameEndTimes_ksOptTripPlans_btu_solution.xml");
//			config.plans().setInputFile(BTU_BASE_DIR + "btu/2018-07-09_sameEndTimes_ksOptTripPlans_agent2com_solution.xml");
//			config.plans().setInputFile(BTU_BASE_DIR + "btu/2018-08-14_sameEndTimes_ksOptTripPlans_agent2com_solution_splits_expanded.xml");
			config.plans().setInputFile(BTU_BASE_DIR + "randoms/2018-11-30_ksRandomTripPlans_coord" + signalCoordIndex + ".xml");
//			config.plans().setInputFile(BTU_BASE_DIR + "btu/sameEndTimes_ksOptTripPlans_agent2com_optimized.xml");
//			config.plans().setInputFile(BTU_BASE_DIR + "btu/2018-11-20_sameEndTimes_ksOptTripPlans_agent2com_optimized.xml");
			break;
		case BTU_POP_BTU_ROUTES_90:
			// TODO
//			config.plans().setInputFile(BTU_BASE_DIR + "btu/2018-08-16_ksOptTripPlans_scale90_solution_splits_expanded.xml");
			config.plans().setInputFile(BTU_BASE_DIR + "btu/2018-10-30_ksTripPlans_scale90_coord0.xml");
			break;
		case WMines:
			config.plans().setInputFile(INPUT_BASE_DIR + "cb_spn_gemeinde_nachfrage_landuse/commuter_population_wgs84_utm33n_car_only.xml.gz");
			break;
		case WoMines:
			// TODO choose one
			// BaseCase plans, no routes
//			config.plans().setInputFile(INPUT_BASE_DIR + "cb_spn_gemeinde_nachfrage_landuse_woMines/commuter_population_wgs84_utm33n_car_only.xml.gz");
			// BaseCase plans, no links for activities, no routes
			config.plans().setInputFile(INPUT_BASE_DIR + "cb_spn_gemeinde_nachfrage_landuse_woMines/commuter_population_wgs84_utm33n_car_only_woLinks.xml.gz");
			break;
		case WoMines100itcap1MS:
			config.plans().setInputFile(INPUT_BASE_DIR + "cb_spn_gemeinde_nachfrage_landuse_woMines/commuter_population_wgs84_utm33n_car_only_100it_MS_cap1.0.xml.gz");
			break;
		case WoMines100itcap07MS:
			config.plans().setInputFile(INPUT_BASE_DIR + "cb_spn_gemeinde_nachfrage_landuse_woMines/commuter_population_wgs84_utm33n_car_only_100it_MS_cap0.7.xml.gz");
//			config.plans().setInputFile("../../runs-svn/cottbus/opdyts/2017-12-12-11-5-55_100it_cap07_MS/1000.output_plans.xml.gz");
			break;
		case WoMines100itcap07MStbs300:
			config.plans().setInputFile(RUNS_SVN + "laemmer/2018-02-7-11-58-38_100it_MS_cap07_stuck600_tbs300/1000.output_plans.xml.gz");
			break;
		case WoMines100itcap1MStbs300:
			config.plans().setInputFile(RUNS_SVN + "laemmer/2018-02-7-12-9-49_100it_MS_cap10_stuck600_tbs300/1000.output_plans.xml.gz");
			break;
		case WoMines100itcap07MStbs900:
			config.plans().setInputFile(RUNS_SVN + "laemmer/2018-02-8-11-59-30_100it_MS_cap07_stuck120_tbs900/1000.output_plans.xml.gz");
			break;
		case WoMines100itcap07MStbs900stuck600:
			config.plans().setInputFile(RUNS_SVN + "laemmer/2018-02-8-11-58-32_100it_MS_cap07_stuck600_tbs900/1000.output_plans.xml.gz");
			break;
		case WoMines100itcap07MSRand:
			config.plans().setInputFile(RUNS_SVN + "opdyts/2017-12-12-11-10-15_100it_cap07_MSrand/1000.output_plans.xml.gz");
			break;	
		case WoMines100itcap07MSideal:
			config.plans().setInputFile(RUNS_SVN + "laemmer/2018-01-17-13-14-14_100it_MSideal_cap07_stuck600/1000.output_plans.xml.gz");
			break;
		case WoMines100itcap1MSideal:
			config.plans().setInputFile(RUNS_SVN + "laemmer/2018-01-17-12-1-45_100it_MSideal_cap10_stuck600/1000.output_plans.xml.gz");
			break;
		case WoMines100itcap07MSidealNetV1_1:
			config.plans().setInputFile(RUNS_SVN + "laemmer/2018-02-13-13-58-41_100it_MS-ideal_cap07_stuck120_tbs900_networkV1-1/1000.output_plans.xml.gz");
			break;
		case WoMines100itcap07MSidealNetV1_2:
			config.plans().setInputFile(RUNS_SVN + "laemmer/2018-02-22-12-18-28_100it_MSideal_cap07_stuck120_tbs900_netV1-2/1000.output_plans.xml.gz");
			break;
		case WoMines100itcap1MSidealNetV1_2:
			config.plans().setInputFile(RUNS_SVN + "laemmer/2018-02-22-10-52-5_100it_MSideal_cap10_stuck120_tbs900_netV1-2/1000.output_plans.xml.gz");
			break;
		case WoMines100itcap1MSNetV1_2:
			config.plans().setInputFile(RUNS_SVN + "laemmer/2018-03-3-18-0-58_100it_MS_cap10_stuck120_tbs900_netV1-2/1000.output_plans.xml.gz");
			break;
		case WoMines100itcap07MSNetV1_2:
			config.plans().setInputFile(RUNS_SVN + "laemmer/2018-03-3-17-58-41_100it_MS_cap07_stuck120_tbs900_netV1-2/1000.output_plans.xml.gz");
			break;
		case WoMines100itcap1MSNetV1_3:
			config.plans().setInputFile(RUNS_SVN + "ewgt/2018-04-13-12-56-38_v1-3_MS_100it_BaseCase_cap10/1000.output_plans.xml.gz");
			break;
		case WoMines100itcap07MSNetV1_3:
			config.plans().setInputFile(RUNS_SVN + "ewgt/2018-04-13-12-57-3_v1-3_MS_100it_BaseCase_cap07/1000.output_plans.xml.gz");
			break;
		case WoMines100itcap05MSNetV1_3:
			config.plans().setInputFile(RUNS_SVN + "ewgt/2018-04-13-17-37-25_v1-3_MS_100it_BaseCase_cap05/1000.output_plans.xml.gz");
			break;
		case WoMines100itcap13MSNetV1_4:
			config.plans().setInputFile(RUNS_SVN + "ewgt/2018-04-15-20-59-8_v1-4_MS_100it_BaseCase_cap13/1000.output_plans.xml.gz");
			break;
		case WoMines100itcap1MSNetV1_4:
			config.plans().setInputFile(RUNS_SVN + "ewgt/2018-04-15-14-54-29_v1-4_MS_100it_BaseCase_cap10/1000.output_plans.xml.gz");
			break;
		case WoMines100itcap07MSNetV1_4:
			config.plans().setInputFile(RUNS_SVN + "ewgt/2018-04-15-14-57-21_v1-4_MS_100it_BaseCase_cap07/1000.output_plans.xml.gz");
			break;
		case WoMines100itcap05MSNetV1_4:
			config.plans().setInputFile(RUNS_SVN + "ewgt/2018-04-15-19-16-28_v1-4_MS_100it_BaseCase_cap05/1000.output_plans.xml.gz");
			break;
		case WoMines100itcap07MSNetV4:
			config.plans().setInputFile(RUNS_SVN + "createNewBC/2018-04-27-14-50-32_100it_netV4_tbs900_stuck120_beta2_MS_cap07/1000.output_plans.xml.gz");
			break;
		case WoMines100itcap05MSNetV4:
			config.plans().setInputFile(RUNS_SVN + "createNewBC/2018-07-25-0-16-48_100it_netV4_tbs900_stuck120_beta2_MS_cap05/1000.output_plans.xml.gz");
			break;
		case WoMines100itcap09MSNetV4:
			config.plans().setInputFile(RUNS_SVN + "createNewBC/2018-07-25-8-13-26_100it_netV4_tbs900_stuck120_beta2_MS_cap09/1000.output_plans.xml.gz");
			break;
		case WoMines100itcap10MSNetV4:
			config.plans().setInputFile(RUNS_SVN + "createNewBC/2018-07-29-13-53-2_100it_netV4_tbs900_stuck120_beta2_MS_cap10/1000.output_plans.xml.gz");
			break;	
		case WoMines100itcap07MSNetV4_1:
			config.plans().setInputFile(RUNS_SVN + "createNewBC/2018-08-24-12-40-20_100it_netV4-1_tbs900_stuck120_beta2_MS_cap07/1000.output_plans.xml.gz");
			break;
		case WoMines1000itcap03MSNetV4_1:
			config.plans().setInputFile(RUNS_SVN + "createNewBC/2018-11-26-10-37-50_1000it_netV4-1_tbs900_stuck120_beta2_MS_cap03/1000.output_plans.xml.gz");
			break;	
		case WoMines1000itcap04MSNetV4_1:
			config.plans().setInputFile(RUNS_SVN + "createNewBC/2018-12-6-11-38-42_1000it_netV4-1_tbs900_stuck120_beta2_MS_cap04/1000.output_plans.xml.gz");
			break;	
		case WoMines1000itcap05MSNetV4_1:
			config.plans().setInputFile(RUNS_SVN + "createNewBC/2018-11-19-12-1-43_1000it_netV4-1_tbs900_stuck120_beta2_MS_cap05/1000.output_plans.xml.gz");
			break;
		case WoMines1000itcap06MSNetV4_1:
			config.plans().setInputFile(RUNS_SVN + "createNewBC/2019-01-30-17-29-40_1000it_netV4-1_tbs900_stuck120_beta2_MS_cap06/1000.output_plans.xml.gz");
			break;	
		case WoMines1000itcap07MSNetV4_1:
			config.plans().setInputFile(RUNS_SVN + "createNewBC/2018-11-19-12-14-4_1000it_netV4-1_tbs900_stuck120_beta2_MS_cap07/1000.output_plans.xml.gz");
			break;
		case WoMines1000itcap07MSNetV4_1_morning:
			config.plans().setInputFile(RUNS_SVN + "createNewBC/2018-11-19-12-14-4_1000it_netV4-1_tbs900_stuck120_beta2_MS_cap07/1000.output_plans_morningPeak.xml.gz");
			break;
		case WoMines1000itcap08MSNetV4_1:
			config.plans().setInputFile(RUNS_SVN + "createNewBC/2019-02-4-13-50-58_1000it_netV4-1_tbs900_stuck120_beta2_MS_cap08/1000.output_plans.xml.gz");
			break;	
		case WoMines1000itcap09MSNetV4_1:
			config.plans().setInputFile(RUNS_SVN + "createNewBC/2019-01-28-22-12-37_1000it_netV4-1_tbs900_stuck120_beta2_MS_cap09/1000.output_plans.xml.gz");
			break;
		case WoMines1000itcap10MSNetV4_1:
			config.plans().setInputFile(RUNS_SVN + "createNewBC/2019-02-18-10-3-21_1000it_netV4-1_tbs900_stuck120_beta2_MS_cap10/1000.output_plans.xml.gz");
			break;	
		case NicoOutputPlans:
			config.plans().setInputFile(RUNS_SVN + "NicoMA/OutputFixedLongLanes/output_plans.xml.gz");
			break;
		case GRID_LOCK_BTU:
			// take these as initial plans
			if (SIGNAL_TYPE.equals(SignalType.MS) || SIGNAL_TYPE.equals(SignalType.DOWNSTREAM_MS)){
				config.plans().setInputFile(OUTPUT_BASE_DIR + "2017-02-2_100it_ReRoute0.1_tbs10_ChExp0.9_beta2_lanes_2link_MS_5plans_GRID_LOCK_BTU_BTU_NET_3600'12'6/output_plans.xml.gz");
			} else if (SIGNAL_TYPE.toString().startsWith("ALL")){
				config.plans().setInputFile(OUTPUT_BASE_DIR + "2017-02-3_100it_ReRoute0.1_tbs10_ChExp0.9_beta2_lanes_2link_ALL_NODES_ALL_GREEN_5plans_GRID_LOCK_BTU_BTU_NET_3600'12'3/output_plans.xml.gz");
//				config.plans().setInputFile(OUTPUT_BASE_DIR + "2017-02-3_100it_ReRoute0.1_tbs10_ChExp0.9_beta2_lanes_2link_ALL_NODES_DOWNSTREAM_5plans_GRID_LOCK_BTU_BTU_NET_3600'12'3/output_plans.xml.gz");						
			}
			break;
		default:
			throw new RuntimeException("Population type not specified!");
		}
		// // pt scenario
		// config.network().setInputFile(INPUT_BASE_DIR + "Cottbus-pt/INPUT_mod/public/input/network_improved.xml");
		// config.plans().setInputFile(INPUT_BASE_DIR + "Cottbus-pt/INPUT_mod/public/input/plans_scale1.4false.xml");

		// set number of iterations
		// TODO
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(1000);
		
		config.qsim().setUsingFastCapacityUpdate(false);

		LaemmerConfigGroup laemmerConfigGroup = ConfigUtils.addOrGetModule(config, LaemmerConfigGroup.class);
		// TODO adapt here
		laemmerConfigGroup.setDesiredCycleTime(90);
		laemmerConfigGroup.setMaxCycleTime(135);
		laemmerConfigGroup.setMinGreenTime(LAEMMER_MIN_G);
		laemmerConfigGroup.setIntergreenTime(INTERGREEN);
		laemmerConfigGroup.setCheckDownstream(false);
		laemmerConfigGroup.setActiveStabilizationStrategy(LAEMMER_FLEX_STAB_STRATEGY);
		
		SylviaConfigGroup sylviaConfig = ConfigUtils.addOrGetModule(config, SylviaConfigGroup.class);
		sylviaConfig.setUseFixedTimeCycleAsMaximalExtension(SYLVIA_FIXED_CYCLE);
		sylviaConfig.setSignalGroupMaxGreenScale(SYLVIA_MAX_EXTENSION);
//		sylviaConfig.setCheckDownstream(true);
		
		// able or enable signals and lanes
		// if signal type 'All...' is used without 'MS', lanes and signals are defined later in 'prepareScenario'
		if (!SIGNAL_TYPE.equals(SignalType.NONE) && (SIGNAL_TYPE.toString().contains("MS") || !SIGNAL_TYPE.toString().startsWith("ALL"))) {
			SignalSystemsConfigGroup signalConfigGroup = ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUP_NAME, SignalSystemsConfigGroup.class);
			signalConfigGroup.setUseSignalSystems(true);
			config.qsim().setUseLanes(SIGNAL_TYPE.toString().startsWith("ALL") && !SIGNAL_TYPE.toString().contains("MS") ? false : true);
			// set signal systems
			switch (NETWORK_TYPE) {
			case V1:
			case V1_1:
			case V1_2:
			case V1_3:
			case V1_4:
				signalConfigGroup.setSignalSystemFile(INPUT_BASE_DIR + "signal_systems_no_13.xml");
				break;
			case BTU_NET:
//				signalConfigGroup.setSignalSystemFile(BTU_BASE_DIR + "output_signal_systems_v2.0.xml.gz"); // gives SAXParseException: Content is not allowed in prolog
//				signalConfigGroup.setSignalSystemFile(BTU_BASE_DIR + "signal_systems_no_13_btuNet.xml"); // this is the same file as output_signal_systems_v2.0.xml.gz but unpacked
				signalConfigGroup.setSignalSystemFile(BTU_BASE_DIR + "output_signal_systems_v2.0.xml");
				break;
			case V2:
				signalConfigGroup.setSignalSystemFile(INPUT_BASE_DIR + "signal_systems_no_13_v2.xml");
				break;
			case V21:
			case V3:
				signalConfigGroup.setSignalSystemFile(INPUT_BASE_DIR + "signal_systems_no_13_v2.1.xml");
				break;
			case V4:
				signalConfigGroup.setSignalSystemFile(INPUT_BASE_DIR + "signal_systems_no_13_v4.xml");
				break;
			case V4_1:
				signalConfigGroup.setSignalSystemFile(INPUT_BASE_DIR + "signal_systems_no_13_v4-1.xml");
				break;
			default:
				throw new RuntimeException("Network type not specified!");
			}			
			// set signal group
			if (NETWORK_TYPE.toString().startsWith("V1")) {
				signalConfigGroup.setSignalGroupsFile(INPUT_BASE_DIR + "signal_groups_no_13.xml");
			} else if (NETWORK_TYPE.equals(NetworkType.BTU_NET)) {
				signalConfigGroup.setSignalGroupsFile(BTU_BASE_DIR + "output_signal_groups_v2.0.xml");
			} else if (NETWORK_TYPE.equals(NetworkType.V4)){
				signalConfigGroup.setSignalGroupsFile(INPUT_BASE_DIR + "signal_groups_no_13_v4.xml");
			} else if (NETWORK_TYPE.equals(NetworkType.V4_1)){
				signalConfigGroup.setSignalGroupsFile(INPUT_BASE_DIR + "signal_groups_no_13_v4-1.xml");
			} else {
				signalConfigGroup.setSignalGroupsFile(INPUT_BASE_DIR + "signal_groups_no_13_v2.xml");
			}
			// set signal control
			switch (SIGNAL_TYPE) {
			case MS:
			case DOWNSTREAM_MS: // will be changed to downstream later
			case DOWNSTREAM_ALLGREEN: // will be changed to all day green and downstream later
			case ALL_MS_AS_DOWNSTREAM_BASIS_GREEN_INSIDE_ENVELOPE_REST_GREEN: // will be changed to all day green and downstream later; additional signal systems will be added later
			case ALL_MS_INSIDE_ENVELOPE_REST_GREEN: // additional signal systems will be added later
			case ALL_MS_AS_DOWNSTREAM_INSIDE_ENVELOPE_REST_GREEN: // will be changed to downstream later; additional signal systems will be added later
			case ALL_DOWNSTREAM_INSIDE_ENVELOPE_BASIS_MS: // additional signal systems will be added later
				if (NETWORK_TYPE.toString().startsWith("V1") || 
						(NETWORK_TYPE.equals(NetworkType.BTU_NET) && BTU_BASE_NET.toString().startsWith("V1"))) {
					signalConfigGroup.setSignalControlFile(INPUT_BASE_DIR + "signal_control_no_13.xml");
				} else if (NETWORK_TYPE.toString().startsWith("V4") || 
						(NETWORK_TYPE.equals(NetworkType.BTU_NET) && BTU_BASE_NET.toString().startsWith("V4"))){
					signalConfigGroup.setSignalControlFile(INPUT_BASE_DIR + "signal_control_no_13_v4.xml");
				} else if (NETWORK_TYPE.toString().startsWith("V2") || 
						(NETWORK_TYPE.equals(NetworkType.BTU_NET) && BTU_BASE_NET.toString().startsWith("V2"))){
					signalConfigGroup.setSignalControlFile(INPUT_BASE_DIR + "signal_control_no_13_v2.xml");
				} else {
					throw new UnsupportedOperationException("It is not yet supported to combine " + SIGNAL_TYPE + " and " + NETWORK_TYPE);
				}
				break;
			case MS_INTG0:
				if (NETWORK_TYPE.toString().startsWith("V4") || 
						(NETWORK_TYPE.equals(NetworkType.BTU_NET) && BTU_BASE_NET.toString().startsWith("V4"))){
					signalConfigGroup.setSignalControlFile(INPUT_BASE_DIR + "signal_control_no_13_v4_intG0.xml");
				} else {
					throw new UnsupportedOperationException("It is not yet supported to combine " + SIGNAL_TYPE + " and " + NETWORK_TYPE);
				}
				break;
			case MS_INTG0_SYLVIA:
				if (NETWORK_TYPE.toString().startsWith("V4") || 
						(NETWORK_TYPE.equals(NetworkType.BTU_NET) && BTU_BASE_NET.toString().startsWith("V4"))){
					signalConfigGroup.setSignalControlFile(INPUT_BASE_DIR + "signal_control_sylvia_no_13_v4_intG0.xml");
				} else {
					throw new UnsupportedOperationException("It is not yet supported to combine " + SIGNAL_TYPE + " and " + NETWORK_TYPE);
				}
				break;
			case MS_RANDOM_OFFSETS:
				if (NETWORK_TYPE.toString().startsWith("V1") || 
						(NETWORK_TYPE.equals(NetworkType.BTU_NET) && BTU_BASE_NET.toString().startsWith("V1"))) {
					signalConfigGroup.setSignalControlFile(INPUT_BASE_DIR + "signal_control_no_13_random_offsets.xml");
				} else {
					throw new UnsupportedOperationException("It is not yet supported to combine " + SIGNAL_TYPE + " and " + NETWORK_TYPE);
				}
				break;
			case MS_RANDOM_GREENSPLITS:
				if (NETWORK_TYPE.equals(NetworkType.V4_1) || (NETWORK_TYPE.equals(NetworkType.BTU_NET) && BTU_BASE_NET.equals(NetworkType.V4_1))) {
					signalConfigGroup.setSignalControlFile(BTU_BASE_DIR + "randoms/signal_control_coord"+signalCoordIndex+".xml");
				} else {
					throw new UnsupportedOperationException("It is not yet supported to combine " + SIGNAL_TYPE + " and " + NETWORK_TYPE);
				}
				break;
			case MS_SYLVIA:
			case ALL_MS_AS_SYLVIA_INSIDE_ENVELOPE_REST_GREEN:
				if (NETWORK_TYPE.toString().startsWith("V1") || 
						(NETWORK_TYPE.equals(NetworkType.BTU_NET) && BTU_BASE_NET.toString().startsWith("V1"))) {
					signalConfigGroup.setSignalControlFile(INPUT_BASE_DIR + "signal_control_sylvia_no_13.xml");
				} else if (NETWORK_TYPE.toString().startsWith("V4") || 
						(NETWORK_TYPE.equals(NetworkType.BTU_NET) && BTU_BASE_NET.toString().startsWith("V4"))){
					signalConfigGroup.setSignalControlFile(INPUT_BASE_DIR + "signal_control_sylvia_no_13_v4.xml");					
				} else {
					throw new UnsupportedOperationException("It is not yet supported to combine " + SIGNAL_TYPE + " and " + NETWORK_TYPE);
				}
				break;
			case MS_BTU_OPT:
			case DOWNSTREAM_BTUOPT:
				if (NETWORK_TYPE.equals(BTU_BASE_NET) || NETWORK_TYPE.equals(NetworkType.BTU_NET)) {
					// TODO
//					signalConfigGroup.setSignalControlFile(BTU_BASE_DIR + "btu/signal_control_opt.xml");
					signalConfigGroup.setSignalControlFile(BTU_BASE_DIR + "btu/signal_control_optimized.xml");
//					signalConfigGroup.setSignalControlFile(BTU_BASE_DIR + "btu/signal_control_optimized_2019_01_04.xml");
//					signalConfigGroup.setSignalControlFile(BTU_BASE_DIR + "btu/signal_control_opt_expanded.xml");
//					signalConfigGroup.setSignalControlFile(BTU_BASE_DIR + "btu_new/signal_control_sol.xml");
//					signalConfigGroup.setSignalControlFile(BTU_BASE_DIR + "btu_new/signal_control_sol_green_exp.xml");
				} else {
					throw new UnsupportedOperationException("It is not yet supported to combine " + SIGNAL_TYPE + " and " + NETWORK_TYPE);
				}
				break;
			case MS_BTU_OPT_SYLVIA:
				if (NETWORK_TYPE.equals(BTU_BASE_NET) || NETWORK_TYPE.equals(NetworkType.BTU_NET)) {
					// TODO
//					signalConfigGroup.setSignalControlFile(BTU_BASE_DIR + "btu/signal_control_opt_expanded_sylvia.xml");
					signalConfigGroup.setSignalControlFile(BTU_BASE_DIR + "btu/signal_control_optimized_sylvia.xml");
				} else {
					throw new UnsupportedOperationException("It is not yet supported to combine " + SIGNAL_TYPE + " and " + NETWORK_TYPE);
				}
				break;
			case LAEMMER_DOUBLE_GROUPS:
				if (NETWORK_TYPE.toString().startsWith("V1") ){
					// overwrite signal groups
					signalConfigGroup.setSignalGroupsFile(INPUT_BASE_DIR + "signal_groups_laemmer_doublePhases.xml");
					signalConfigGroup.setSignalControlFile(INPUT_BASE_DIR + "signal_control_laemmer.xml");
				} else {
					throw new UnsupportedOperationException("It is not yet supported to combine " + SIGNAL_TYPE + " and " + NETWORK_TYPE);
				}
				break;
			case LAEMMER_DOUBLE_GROUPS_14GREEN:
				if (NETWORK_TYPE.toString().startsWith("V1") || 
						(NETWORK_TYPE.equals(NetworkType.BTU_NET) && BTU_BASE_NET.toString().startsWith("V1"))) {
					signalConfigGroup.setSignalGroupsFile(INPUT_BASE_DIR + "signal_groups_laemmer_doublePhases_14allGreen1107.xml");
					signalConfigGroup.setSignalControlFile(INPUT_BASE_DIR + "signal_control_laemmer.xml");
				} else {
					throw new UnsupportedOperationException("It is not yet supported to combine " + SIGNAL_TYPE + " and " + NETWORK_TYPE);
				}
				break;
			case LAEMMER_DOUBLE_GROUPS_SYS17:
				if (NETWORK_TYPE.toString().startsWith("V1") || 
						(NETWORK_TYPE.equals(NetworkType.BTU_NET) && BTU_BASE_NET.toString().startsWith("V1"))) {
					signalConfigGroup.setSignalGroupsFile(INPUT_BASE_DIR + "signal_groups_laemmer_doublePhases17.xml");
					signalConfigGroup.setSignalControlFile(INPUT_BASE_DIR + "signal_control_laemmer.xml");
				} else {
					throw new UnsupportedOperationException("It is not yet supported to combine " + SIGNAL_TYPE + " and " + NETWORK_TYPE);
				}
				break;
			case LAEMMER_NICO_GROUPS:
				if (NETWORK_TYPE.toString().startsWith("V1") || 
						(NETWORK_TYPE.equals(NetworkType.BTU_NET) && BTU_BASE_NET.toString().startsWith("V1"))) {
					// signalConfigGroup.setSignalGroupsFile(INPUT_BASE_DIR + "signal_groups_laemmer2phases.xml");
					// signalConfigGroup.setSignalGroupsFile(INPUT_BASE_DIR + "signal_groups_laemmer2phases_6.xml");
					signalConfigGroup.setSignalGroupsFile(INPUT_BASE_DIR + "signal_groups_laemmer.xml");
					signalConfigGroup.setSignalControlFile(INPUT_BASE_DIR + "signal_control_laemmer.xml");
				} else {
					throw new UnsupportedOperationException("It is not yet supported to combine " + SIGNAL_TYPE + " and " + NETWORK_TYPE);
				}
				break;
			case LAEMMER_NICO_GROUPS_14GREEN:
				if (NETWORK_TYPE.toString().startsWith("V1") || 
						(NETWORK_TYPE.equals(NetworkType.BTU_NET) && BTU_BASE_NET.toString().startsWith("V1"))) {
					signalConfigGroup.setSignalGroupsFile(INPUT_BASE_DIR + "signal_groups_laemmerNico_14allGreen1107.xml");
					signalConfigGroup.setSignalControlFile(INPUT_BASE_DIR + "signal_control_laemmer.xml");
				} else {
					throw new UnsupportedOperationException("It is not yet supported to combine " + SIGNAL_TYPE + " and " + NETWORK_TYPE);
				}
				break;
			case LAEMMER_NICO_GROUPS_14RE:
				signalConfigGroup.setSignalControlFile(INPUT_BASE_DIR + "signal_control_laemmer.xml");
				if (NETWORK_TYPE.toString().startsWith("V1") || 
						(NETWORK_TYPE.equals(NetworkType.BTU_NET) && BTU_BASE_NET.toString().startsWith("V1"))) {
					signalConfigGroup.setSignalGroupsFile(INPUT_BASE_DIR + "signal_groups_laemmerNico_14restructurePhases.xml");
				} else if (NETWORK_TYPE.toString().startsWith("V4") || 
						(NETWORK_TYPE.equals(NetworkType.BTU_NET) && BTU_BASE_NET.toString().startsWith("V4"))) {
					signalConfigGroup.setSignalGroupsFile(INPUT_BASE_DIR + "signal_groups_laemmerNico_14restructurePhases_v4.xml");
				} else {
					throw new UnsupportedOperationException("It is not yet supported to combine " + SIGNAL_TYPE + " and " + NETWORK_TYPE);
				}
				break;
			case LAEMMER_NICO_GROUPS_14RE_6RE:
				signalConfigGroup.setSignalControlFile(INPUT_BASE_DIR + "signal_control_laemmer.xml");
				if (NETWORK_TYPE.toString().startsWith("V4") || 
						(NETWORK_TYPE.equals(NetworkType.BTU_NET) && BTU_BASE_NET.toString().startsWith("V4"))) {
					signalConfigGroup.setSignalGroupsFile(INPUT_BASE_DIR + "signal_groups_laemmerNico_14re_6re_v4.xml");
				} else {
					throw new UnsupportedOperationException("It is not yet supported to combine " + SIGNAL_TYPE + " and " + NETWORK_TYPE);
				}
				break;
			case LAEMMER_NICO_GROUPS_14RE_3RE_7RE:
				signalConfigGroup.setSignalControlFile(INPUT_BASE_DIR + "signal_control_laemmer.xml");
				if (NETWORK_TYPE.equals(NetworkType.V4_1) || 
						(NETWORK_TYPE.equals(NetworkType.BTU_NET) && BTU_BASE_NET.equals(NetworkType.V4_1))) {
					signalConfigGroup.setSignalGroupsFile(INPUT_BASE_DIR + "signal_groups_laemmerNico_14re_3re_7re_v4-1.xml");
				} else if (NETWORK_TYPE.equals(NetworkType.V4) || 
						(NETWORK_TYPE.equals(NetworkType.BTU_NET) && BTU_BASE_NET.equals(NetworkType.V4))) {
					signalConfigGroup.setSignalGroupsFile(INPUT_BASE_DIR + "signal_groups_laemmerNico_14re_3re_7re_v4.xml");
				} else {
					throw new UnsupportedOperationException("It is not yet supported to combine " + SIGNAL_TYPE + " and " + NETWORK_TYPE);
				}
				break;
			case MS_IDEAL:
				if (NETWORK_TYPE.toString().startsWith("V1") || 
						(NETWORK_TYPE.equals(NetworkType.BTU_NET) && BTU_BASE_NET.toString().startsWith("V1"))) {
					signalConfigGroup.setSignalControlFile(INPUT_BASE_DIR + "signal_control_no_13_idealized.xml");
				} else {
					throw new UnsupportedOperationException("It is not yet supported to combine " + SIGNAL_TYPE + " and " + NETWORK_TYPE);
				}
				break;
			case LAEMMER_FLEXIBLE:
				signalConfigGroup.setSignalControlFile(INPUT_BASE_DIR + "signal_control_laemmer_flexible.xml");
				break;
			case GERSHENSON:
				signalConfigGroup.setSignalControlFile(INPUT_BASE_DIR + "signal_control_gershenson.xml");
				if (NETWORK_TYPE.equals(NetworkType.V4_1) || 
						(NETWORK_TYPE.equals(NetworkType.BTU_NET) && BTU_BASE_NET.equals(NetworkType.V4_1))) {
					signalConfigGroup.setSignalGroupsFile(INPUT_BASE_DIR + "signal_groups_laemmerNico_14re_3re_7re_v4-1.xml");
				} else if (NETWORK_TYPE.equals(NetworkType.V4) || 
						(NETWORK_TYPE.equals(NetworkType.BTU_NET) && BTU_BASE_NET.equals(NetworkType.V4))) {
					signalConfigGroup.setSignalGroupsFile(INPUT_BASE_DIR + "signal_groups_laemmerNico_14re_3re_7re_v4.xml");
				} else {
					throw new UnsupportedOperationException("It is not yet supported to combine " + SIGNAL_TYPE + " and " + NETWORK_TYPE);
				}
				break;
			case QUEUE_LEARNING:
				signalConfigGroup.setSignalControlFile(INPUT_BASE_DIR + "signal_control_queueLearning.xml");
				if (NETWORK_TYPE.equals(NetworkType.V4_1) || 
						(NETWORK_TYPE.equals(NetworkType.BTU_NET) && BTU_BASE_NET.equals(NetworkType.V4_1))) {
					signalConfigGroup.setSignalGroupsFile(INPUT_BASE_DIR + "signal_groups_laemmerNico_14re_3re_7re_v4-1.xml");
				} else if (NETWORK_TYPE.equals(NetworkType.V4) || 
						(NETWORK_TYPE.equals(NetworkType.BTU_NET) && BTU_BASE_NET.equals(NetworkType.V4))) {
					signalConfigGroup.setSignalGroupsFile(INPUT_BASE_DIR + "signal_groups_laemmerNico_14re_3re_7re_v4.xml");
				} else {
					throw new UnsupportedOperationException("It is not yet supported to combine " + SIGNAL_TYPE + " and " + NETWORK_TYPE);
				}
				break;
			}
			
			// add data about conflicting directions
			if (INTERSECTION_LOGIC.toString().startsWith("CONFLICTING_DIRECTIONS")) {
				if (NETWORK_TYPE.toString().startsWith("V4") || 
						(NETWORK_TYPE.equals(NetworkType.BTU_NET) && BTU_BASE_NET.toString().startsWith("V4"))) {
					signalConfigGroup.setConflictingDirectionsFile(INPUT_BASE_DIR
							+ "conflictData_fromBtu2018-05-03_basedOnMSconflicts_v4_modifiedBasedOnMS.xml");
					// TODO konflikte erstellen, die information enthalten über must-yield/with-right-of-way
				} else {
					throw new UnsupportedOperationException("no conflict data defined expect for network type V4");
				}
			}
			signalConfigGroup.setIntersectionLogic(INTERSECTION_LOGIC);
		}
		
		if (PRICING_TYPE.toString().startsWith("CORDON_")){
			RoadPricingConfigGroup roadPricingCG = ConfigUtils.addOrGetModule(config, RoadPricingConfigGroup.GROUP_NAME, RoadPricingConfigGroup.class);
			// TODO adapt toll value here
			if (PRICING_TYPE.equals(PricingType.CORDON_INNERCITY)){
				roadPricingCG.setTollLinksFile(INPUT_BASE_DIR + "cordonToll/tollLinksFile_innerCityWoRing_100.xml");
			} else { // PricingType.CORDON_RING
				roadPricingCG.setTollLinksFile(INPUT_BASE_DIR + "cordonToll/tollLinksFile_innerCityWRing_100.xml");				
			}
		}
		
		// set brain exp beta
//		TODO
		config.planCalcScore().setBrainExpBeta( 2 );
//		config.planCalcScore().setBrainExpBeta( 5 );
//		config.planCalcScore().setBrainExpBeta( 10 );
//		config.planCalcScore().setBrainExpBeta( 20 );

		// choose between link to link and node to node routing
		// (only has effect if lanes are used)
		boolean link2linkRouting = true;
		config.controler().setLinkToLinkRoutingEnabled(link2linkRouting);
		config.travelTimeCalculator().setCalculateLinkToLinkTravelTimes(link2linkRouting);
		config.travelTimeCalculator().setCalculateLinkTravelTimes(true);

		// set travelTimeBinSize (only has effect if reRoute is used)
		config.travelTimeCalculator().setTraveltimeBinSize( 900 );
//		config.travelTimeCalculator().setTraveltimeBinSize( 300 );
//		config.travelTimeCalculator().setTraveltimeBinSize( 10 );
// 		config.travelTimeCalculator().setTraveltimeBinSize( 30 );
//		config.travelTimeCalculator().setTraveltimeBinSize( 60 );
		// TODO

		config.travelTimeCalculator().setTravelTimeCalculatorType(TravelTimeCalculatorType.TravelTimeCalculatorHashMap.toString());
		// hash map and array produce same results. only difference: memory and time.
		// for small time bins and sparse values hash map is better. theresa, may'15

		// define strategies:
		// TODO
		config.strategy().setFractionOfIterationsToDisableInnovation(.8);
		{
			StrategySettings strat = new StrategySettings();
			strat.setStrategyName(DefaultStrategy.ReRoute.toString());
			if (POP_TYPE.toString().startsWith("BTU_POP_BTU_ROUTES") || POP_TYPE.equals(PopulationType.NicoOutputPlans))
				strat.setWeight(0.0); // no ReRoute, fix route choice set
			else
				strat.setWeight(0.1);
			config.strategy().addStrategySettings(strat);
		}
		{
			StrategySettings strat = new StrategySettings();
			strat.setStrategyName(DefaultStrategy.TimeAllocationMutator.toString());
			strat.setWeight(0.0);
			config.strategy().addStrategySettings(strat);
			config.timeAllocationMutator().setMutationRange(1800); // 1800 is default
		}
		{
			StrategySettings strat = new StrategySettings();
			strat.setStrategyName(DefaultSelector.ChangeExpBeta.toString());
			strat.setWeight(0.9);
			config.strategy().addStrategySettings(strat);
		}
		{
			StrategySettings strat = new StrategySettings();
			strat.setStrategyName(DefaultSelector.BestScore.toString());
			strat.setWeight(0.0);
			config.strategy().addStrategySettings(strat);
		}
		{
			StrategySettings strat = new StrategySettings();
			strat.setStrategyName(DefaultSelector.KeepLastSelected.toString());
			strat.setWeight(0.0);
			config.strategy().addStrategySettings(strat);
		}

		// choose maximal number of plans per agent. 0 means unlimited
		if (POP_TYPE.equals(PopulationType.BTU_POP_BTU_ROUTES))
			config.strategy().setMaxAgentPlanMemorySize(0); //unlimited because ReRoute is switched off anyway
		else 
			config.strategy().setMaxAgentPlanMemorySize( 5 );

		// TODO
//		config.qsim().setStuckTime( 10 );
		config.qsim().setStuckTime( 120 );
//		config.qsim().setStuckTime( 600 );
//		config.qsim().setStuckTime( 3600 ); // default ist 10s
		config.qsim().setRemoveStuckVehicles(false);
		config.qsim().setStartTime(3600 * 5); 
		// TODO change to a higher value for congested scenarios
		config.qsim().setEndTime(30.*3600.);
		config.qsim().setInsertingWaitingVehiclesBeforeDrivingVehicles(false); // false is default
//		config.qsim().setTrafficDynamics(TrafficDynamics.withHoles); // queue is default
		
		if (NETWORK_TYPE.equals(NetworkType.BTU_NET) && SCALING_FACTOR != 1.){
//			LOG.warn("Keep in mind that the btu network capacity has already been scaled");
			throw new RuntimeException("the btu network capacity has already been scaled");
		}
//		config.qsim().setStorageCapFactor( SCALING_FACTOR );
		/* storage cap should be scaled less than flow capacity factor. 
		 * read in NicolaiNagelHiResAccessibilityMethod (2014), p.75f. (or p.9 in preprint), they mention RieserNagel2008NetworkBreakdown as reference */
		config.qsim().setStorageCapFactor( POP_SCALE * SCALING_FACTOR / Math.pow(SCALING_FACTOR,1/4.) );
		config.qsim().setFlowCapFactor( POP_SCALE * SCALING_FACTOR );
		
		// adapt monetary distance cost rate
		// (should be negative. the smaller it is, the more counts the distance.
		// use -12.0 to balance time [h] and distance [m].
		// use -0.0033 to balance [s] and [m], -0.012 to balance [h] and [km], -0.0004 to balance [h] and 30[km]...
		// use -0.0 to use only time.)
		config.planCalcScore().getModes().get(TransportMode.car).setMonetaryDistanceRate( -0.0 ); // Ihab: 20Cent=0.2Eur guter Wert pro km -> 0.0002 pro m

		config.planCalcScore().setMarginalUtilityOfMoney(1.0); // default is 1.0

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		// note: overwriteExistingFiles necessary when 'writeInputFiles' is true
		// note: the output directory is defined in createRunNameAndOutputDir(...) after all adaptations are done

		config.vspExperimental().setWritingOutputEvents(true);
		config.planCalcScore().setWriteExperiencedPlans(false);
		config.controler().setCreateGraphs(true);

		int lastIt = config.controler().getLastIteration();
		config.controler().setWriteEventsInterval(lastIt<200? (lastIt==0? 1 : lastIt) : 200);
		config.controler().setWritePlansInterval(lastIt<200? (lastIt==0? 1 : lastIt) : 200);

		// define activity types
		{
			ActivityParams dummyAct = new ActivityParams("dummy");
			dummyAct.setTypicalDuration(12 * 3600);
			dummyAct.setOpeningTime(5 * 3600);
			dummyAct.setLatestStartTime(10 * 3600);
			config.planCalcScore().addActivityParams(dummyAct);
		}
		{
			ActivityParams homeAct = new ActivityParams("home");
			homeAct.setTypicalDuration(15.5 * 3600);
			config.planCalcScore().addActivityParams(homeAct);
		}
		{
			ActivityParams workAct = new ActivityParams("work");
			workAct.setTypicalDuration(8.5 * 3600);
			workAct.setOpeningTime(7 * 3600);
			workAct.setClosingTime(17.5 * 3600);
			config.planCalcScore().addActivityParams(workAct);
		}
		
		config.global().setCoordinateSystem("EPSG:25833"); //UTM33
		
		// TODO
		// decongestion relevant parameters
		DecongestionConfigGroup decongestionSettings = new DecongestionConfigGroup();
		decongestionSettings.setWriteOutputIteration(200);
		decongestionSettings.setUpdatePriceInterval(1);
//		decongestionSettings.setTollBlendFactor(1.0);
		decongestionSettings.setFractionOfIterationsToStartPriceAdjustment(0.0);
		decongestionSettings.setFractionOfIterationsToEndPriceAdjustment(1.0);
		decongestionSettings.setToleratedAverageDelaySec(1);
		decongestionSettings.setWriteLinkInfoCharts(true);
		decongestionSettings.setMsa(true); // does this also work for BangBang?

		decongestionSettings.setDecongestionApproach(DecongestionApproach.BangBang);
//		decongestionSettings.setTollAdjustment(1);
//		decongestionSettings.setInitialToll(1);
		decongestionSettings.setKp(0.003); // TODO 0.0067 (2*VTTS/3600)
		decongestionSettings.setKi(0.);
		decongestionSettings.setKd(0.);

		config.addModule(decongestionSettings);
		
		// add counts module
		if (USE_COUNTS) {
			if (!NETWORK_TYPE.equals(NetworkType.V1)){
				throw new UnsupportedOperationException("In this scenario, counts can only be used together with NetworkType.V1"
						+ " because they are not available for other simplified networks.");
			}
//			config.counts().setCountsFileName(INPUT_BASE_DIR + "CottbusCounts/counts_matsim/counts_final_shifted.xml");
			config.counts().setInputFile(INPUT_BASE_DIR + "CottbusCounts/counts_matsim/counts_final_shifted_v2.xml");
			config.counts().setCountsScaleFactor(1.0 / SCALING_FACTOR); // sample size
			config.counts().setWriteCountsInterval(config.controler().getLastIteration());
//			config.counts().setWriteCountsInterval(1);
			config.counts().setOutputFormat("all");
//			config.counts().setInputCRS(inputCRS);
			config.counts().setAverageCountsOverIterations(10);
		}
		
		return config;
	}

	private static Scenario prepareScenario(Config config, int signalCoordIndex) {
		Scenario scenario = ScenarioUtils.loadScenario(config);	
	
		// scale population if necessary
		if (POP_SCALE != 1 && !POP_TYPE.equals(PopulationType.BTU_POP_BTU_ROUTES_90)) {
			// if BTU_POP_BTU_ROUTES_90 is used, population is already scaled (only flow/storage capacitiy has still to be scaled)
			ModifyPopulation.copyEachPerson(scenario.getPopulation(), POP_SCALE - 1);
		}
		if (DELETE_ROUTES) {
			ModifyPopulation.removeRoutesLeaveFirstPlan(scenario.getPopulation());
		}

		// TODO delete bottleneck links for coord14
//		scenario.getNetwork().getLinks().get(Id.createLinkId("1356-5867-7871")).setCapacity(1);
//		scenario.getNetwork().getLinks().get(Id.createLinkId("5771-5772")).setCapacity(1);

		if (LONG_LANES){
			// lengthen all lanes
			ModifyNetwork.lengthenAllLanes(scenario);
		}
		if (LANE_CAP_FROM_NETWORK) {
			LanesUtils.overwriteLaneCapacitiesByNetworkCapacities(scenario.getNetwork(), scenario.getLanes());
		}
		
		// add missing scenario elements
		SignalSystemsConfigGroup signalsConfigGroup = ConfigUtils.addOrGetModule(config,
				SignalSystemsConfigGroup.GROUP_NAME, SignalSystemsConfigGroup.class);
		if (signalsConfigGroup.isUseSignalSystems()) {
			scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());
		}

		// adoptions for some signal types necessary:
		switch (SIGNAL_TYPE) {
		case DOWNSTREAM_BTUOPT:
		case DOWNSTREAM_MS:
		case DOWNSTREAM_ALLGREEN:
		case ALL_MS_AS_DOWNSTREAM_BASIS_GREEN_INSIDE_ENVELOPE_REST_GREEN:
			// adapt signal controller for downstream signal control
			SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
			for (SignalSystemControllerData controllerData : signalsData.getSignalControlData().getSignalSystemControllerDataBySystemId().values()) {
				controllerData.setControllerIdentifier(DownstreamPlanbasedSignalController.IDENTIFIER);
				if (SIGNAL_TYPE.toString().contains("GREEN")){
					// change to all day green
					for (SignalPlanData planData : controllerData.getSignalPlanData().values()) {
						for (SignalGroupSettingsData groupSetting : planData.getSignalGroupSettingsDataByGroupId().values()) {
							groupSetting.setOnset(0);
							groupSetting.setDropping(planData.getCycleTime()-1);
						}
					}
				}
			}
			if (SIGNAL_TYPE.equals(SignalType.ALL_MS_AS_DOWNSTREAM_BASIS_GREEN_INSIDE_ENVELOPE_REST_GREEN)){
				// add additional all day green signals inside envelope
				SignalizeScenario signalizer = new SignalizeScenario(scenario);
				signalizer.setOverwriteSignals(false);
				signalizer.setBoundingBox(INPUT_BASE_DIR + "shape_files/signal_systems/bounding_box.shp");
				// note: no specific signal controller identifier - additional signals should show green all day
				signalizer.createSignalsAndLanesForAllTurnings();
			}
			break;
		case ALL_DOWNSTREAM_INSIDE_ENVELOPE_BASIS_MS:
			// change all signal controller to downstream
			signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
			for (SignalSystemControllerData controllerData : signalsData.getSignalControlData().getSignalSystemControllerDataBySystemId().values()) {
				controllerData.setControllerIdentifier(DownstreamPlanbasedSignalController.IDENTIFIER);
			}
			// note: no break - should continue! (signals of following types are replaced anyway)
		case ALL_NODES_ALL_GREEN:
		case ALL_NODES_DOWNSTREAM:
		case ALL_GREEN_INSIDE_ENVELOPE:
		case ALL_DOWNSTREAM_INSIDE_ENVELOPE_BASIS_GREEN:
			// signalize all intersections (or all inside envelope or all that are not signalized yet), create corresponding lanes
			SignalizeScenario signalizer = new SignalizeScenario(scenario);
			if (SIGNAL_TYPE.toString().contains("BASIS_MS"))
				signalizer.setOverwriteSignals(false);
			if (SIGNAL_TYPE.toString().contains("DOWNSTREAM"))
				signalizer.setSignalControlIdentifier(DownstreamPlanbasedSignalController.IDENTIFIER);
			if (SIGNAL_TYPE.toString().contains("ENVELOPE"))
				signalizer.setBoundingBox(INPUT_BASE_DIR + "shape_files/signal_systems/bounding_box.shp");
			signalizer.createSignalsAndLanesForAllTurnings();
			break;		
		case ALL_MS_AS_DOWNSTREAM_INSIDE_ENVELOPE_REST_GREEN:
			// change all signal controller to downstream
			signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
			for (SignalSystemControllerData controllerData : signalsData.getSignalControlData().getSignalSystemControllerDataBySystemId().values()) {
				controllerData.setControllerIdentifier(DownstreamPlanbasedSignalController.IDENTIFIER);
			} 
			// note: no break - should continue! (following sylvia and ms already have correct signal controller)
		case ALL_MS_AS_SYLVIA_INSIDE_ENVELOPE_REST_GREEN:
		case ALL_MS_INSIDE_ENVELOPE_REST_GREEN:
			// add additional all day green signals inside envelope
			signalizer = new SignalizeScenario(scenario);
			signalizer.setOverwriteSignals(false);
			signalizer.setBoundingBox(INPUT_BASE_DIR + "shape_files/signal_systems/bounding_box.shp");
			// note: no specific signal controller identifier - additional signals should show green all day
			signalizer.createSignalsAndLanesForAllTurnings();
			break;
		default:
			break;
		}
		
//		// adoptions for artificial population (GRID_LOCK_BTU)
//		if (POP_TYPE.equals(PopulationType.GRID_LOCK_BTU)){
//			createArificialGridLockPopulation(scenario);
//		}
		
		createRunNameAndOutputDir(scenario, signalCoordIndex);
		if (WRITE_INITIAL_FILES){ 
			writeInitFiles(scenario);
		}
		
		return scenario;
	}

	private static void createArificialGridLockPopulation(Scenario scenario) {
		String[] inLinks = {"7919", "4909", "1281", "506", "3411", "2100-2098-5377", "6230",
				"40", "3503", "6663", "2759", "2475"};
		String[] outLinks = {"7918", "4908", "1282", "4511", "3418", "5376-2097-2099", "6213",
				"8632", "3490", "6662", "2758", "6747"};
		List<Id<Link>> inLinkIds = new LinkedList<>();
		List<Id<Link>> outLinkIds = new LinkedList<>();
		for (int i=0; i<inLinks.length; i++){
			inLinkIds.add(Id.createLinkId(inLinks[i]));
			outLinkIds.add(Id.createLinkId(outLinks[i]));
		}
		
		Population pop = scenario.getPopulation();
		
		int numberOfPersonsPerODPerH = 3600/12/3;
		double simulationPeriod_h = 3;
		double simulationStartTime_s = 6*3600;
		for (Id<Link> inLinkId : inLinkIds){
			for (Id<Link> outLinkId : outLinkIds){
				for (int i = 0; i < numberOfPersonsPerODPerH * simulationPeriod_h; i++) {
					// create a person
					Person person = pop.getFactory().createPerson(Id.createPersonId(inLinkId + "_" + outLinkId + "_" + i));

					// create a start activity at the inLink
					Activity startAct = pop.getFactory().createActivityFromLinkId("dummy", inLinkId);
					// distribute agents uniformly between simulationStartTime and (simulationStartTime + simulationPeriod) am.
					startAct.setEndTime(simulationStartTime_s + (double)(i)/(numberOfPersonsPerODPerH * simulationPeriod_h) * simulationPeriod_h * 3600);
				
					// create a drain activity at outLink
					Activity drainAct = pop.getFactory().createActivityFromLinkId("dummy", Id.createLinkId(outLinkId));
					
					// create a dummy leg
					Leg leg = pop.getFactory().createLeg(TransportMode.car);
					
					// create a plan for the person that contains all this information
					Plan plan = pop.getFactory().createPlan();
					plan.addActivity(startAct);
					plan.addLeg(leg);
					plan.addActivity(drainAct);
					
					// store information in population
					person.addPlan(plan);
					
					pop.addPerson(person);
				}
			}
		}		
	}

	private static Controler prepareController(Scenario scenario) {
		Config config = scenario.getConfig();
		Controler controler = new Controler(scenario);

		// add the signals module if signal systems are used
		SignalSystemsConfigGroup signalsConfigGroup = ConfigUtils.addOrGetModule(config,
				SignalSystemsConfigGroup.GROUP_NAME, SignalSystemsConfigGroup.class);
		
		// the signals extensions works for planbased, sylvia and laemmer signal controller
        // by default and is pluggable for your own signal controller like this:        
        Configurator signalsConfigurator = new Configurator(controler);
		signalsConfigurator.addSignalControllerFactory(DownstreamPlanbasedSignalController.IDENTIFIER,
				DownstreamPlanbasedSignalController.DownstreamFactory.class);
        signalsConfigurator.addSignalControllerFactory(FullyAdaptiveLaemmerSignalController.IDENTIFIER,
				FullyAdaptiveLaemmerSignalController.LaemmerFlexFactory.class);
        signalsConfigurator.addSignalControllerFactory(GershensonSignalController.IDENTIFIER,
				GershensonSignalController.GershensonFactory.class);
//        signalsConfigurator.addSignalControllerFactory(QueueLearningSignalControler.IDENTIFIER,
//        			QueueLearningSignalControler.QueueLearningFactory.class);

		// bind gershenson config
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				GershensonConfig gershensonConfig = new GershensonConfig();
				bind(GershensonConfig.class).toInstance(gershensonConfig);
			}
		});
		
		if (PRICING_TYPE.toString().startsWith("CP_")){
			// add tolling
			TollHandler tollHandler = new TollHandler(scenario);
			
			// add correct TravelDisutilityFactory for tolls if ReRoute is used
			StrategySettings[] strategies = config.strategy().getStrategySettings().toArray(new StrategySettings[0]);
			for (int i = 0; i < strategies.length; i++) {
				if (strategies[i].getStrategyName().equals(DefaultStrategy.ReRoute.toString())){
					if (strategies[i].getWeight() > 0.0){ // ReRoute is used
						final CongestionTollTimeDistanceTravelDisutilityFactory factory =
								new CongestionTollTimeDistanceTravelDisutilityFactory(
										new RandomizingTimeDistanceTravelDisutilityFactory( TransportMode.car, config.planCalcScore() ),
								tollHandler, config.planCalcScore()
							) ;
						factory.setSigma(SIGMA);
						controler.addOverridingModule(new AbstractModule(){
							@Override
							public void install() {
								this.bindCarTravelDisutilityFactory().toInstance( factory );
							}
						});
					}
				}
			}		
			
			// choose the correct congestion handler and add it
			EventHandler congestionHandler = null;
			switch (PRICING_TYPE){
			case CP_V3:
				congestionHandler = new CongestionHandlerImplV3(controler.getEvents(), controler.getScenario());
				break;
			case CP_V4:
				congestionHandler = new CongestionHandlerImplV4(controler.getEvents(), controler.getScenario());
				break;
			case CP_V7:
				congestionHandler = new CongestionHandlerImplV7(controler.getEvents(), controler.getScenario());
				break;
			case CP_V8:
				congestionHandler = new CongestionHandlerImplV8(controler.getEvents(), controler.getScenario());
				break;
			case CP_V9:
				congestionHandler = new CongestionHandlerImplV9(controler.getEvents(), controler.getScenario());
				break;
			case CP_V10:
				congestionHandler = new CongestionHandlerImplV10(controler.getEvents(), controler.getScenario());
				break;
			default:
				break;
			}
			controler.addControlerListener(new MarginalCongestionPricingContolerListener(controler.getScenario(),
					tollHandler, congestionHandler));

		} else if (PRICING_TYPE.equals(PricingType.FLOWBASED)) {
			
			throw new UnsupportedOperationException("Not yet implemented!");
//			Initializer initializer = new Initializer();
//			controler.addControlerListener(initializer);		
		} else if (PRICING_TYPE.toString().startsWith("CORDON_")){
			// (loads the road pricing scheme, uses custom travel disutility including tolls, etc.)
			controler.addOverridingModule(new RoadPricingModule());
		} else if (PRICING_TYPE.equals(PricingType.INTERVAL_BASED)) {
			
			controler.addOverridingModule(new DecongestionModule(scenario));
			
			// toll-adjusted routing
			
			final TollTimeDistanceTravelDisutilityFactory travelDisutilityFactory = new TollTimeDistanceTravelDisutilityFactory();
			travelDisutilityFactory.setSigma(SIGMA);
			
			controler.addOverridingModule(new AbstractModule(){
				@Override
				public void install() {
					this.bindCarTravelDisutilityFactory().toInstance( travelDisutilityFactory );
				}
			});
						
		} else { // no pricing
			
			// adapt sigma for randomized routing
			final RandomizingTimeDistanceTravelDisutilityFactory builder =
					new RandomizingTimeDistanceTravelDisutilityFactory( TransportMode.car, config.planCalcScore() );
			builder.setSigma(SIGMA);
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					bindCarTravelDisutilityFactory().toInstance(builder);
				}
			});
		}
		
		// TODO
//		controler.addOverridingModule(new AbstractModule() {
//			@Override
//			public void install() {
//				DiversityGeneratingPlansRemover.Builder builder = new DiversityGeneratingPlansRemover.Builder();
//				final double ccc = 0.03;
//				builder.setSameLocationPenalty(0.);
//				builder.setSameActivityTypePenalty(0.);
//				builder.setSameActivityEndTimePenalty(0.);
//				builder.setSameModePenalty(0.);
//				builder.setSameRoutePenalty(ccc);
//				// builder.setStageActivityTypes( tripRouter.getStageActivityTypes() ) ;
//				this.bindPlanSelectorForRemoval().toProvider(builder);
//			}
//		});
		
		controler.addOverridingModule(new AbstractModule() {			
			@Override
			public void install() {
				
				// bind subnetwork analysis
				if (!NETWORK_TYPE.equals(NetworkType.BTU_NET)) {
					String filterFeatureFilename = "../../shared-svn/projects/cottbus/data/scenarios/cottbus_scenario/shape_files/signal_systems/bounding_box.shp";
					TtSubnetworkAnalyzer subNetAnalyzer = new TtSubnetworkAnalyzer(filterFeatureFilename,
							scenario.getNetwork());
					this.addEventHandlerBinding().toInstance(subNetAnalyzer);
					this.bind(TtSubnetworkAnalyzer.class).toInstance(subNetAnalyzer);
					this.addControlerListenerBinding().to(TtSubnetworkAnalysisWriter.class);
				}
				
//				this.bind(TtGeneralAnalysis.class);
				ANALYSIS_TOOL = new TtGeneralAnalysis(scenario.getNetwork());
				this.addEventHandlerBinding().toInstance(ANALYSIS_TOOL);
				this.bind(TtGeneralAnalysis.class).toInstance(ANALYSIS_TOOL);
				this.bind(TtAnalyzedGeneralResultsWriter.class);
				this.addControlerListenerBinding().to(TtListenerToBindGeneralAnalysis.class);
				
				if (signalsConfigGroup.isUseSignalSystems()) {
					// bind tool to analyze signals
					this.bind(SignalAnalysisTool.class);
					this.bind(SignalAnalysisWriter.class);
					this.addControlerListenerBinding().to(SignalAnalysisListener.class);
					this.addControlerListenerBinding().to(TtQueueLengthAnalysisTool.class);
					this.addMobsimListenerBinding().to(TtQueueLengthAnalysisTool.class);
				}
				
				if (POP_TYPE.toString().startsWith("BTU_POP")) {
					// bind commodity-based analysis
					this.bind(TtCommodityTravelTimeAnalyzer.class);
					this.addControlerListenerBinding().to(TtWriteComAnalysis.class);
				}
			}
		});
		
		return controler;
	}
	
	private static void createRunNameAndOutputDir(Scenario scenario, int signalCoordIndex) {

		Config config = scenario.getConfig();
		
		String runName = OutputUtils.getCurrentDate();
		runName += "_" + config.controler().getLastIteration() + "it";
		
		// create info about capacities
		double storeCap = config.qsim().getStorageCapFactor();
		double flowCap = config.qsim().getFlowCapFactor();
		if (storeCap == flowCap && storeCap != 1.0){
			runName += "_cap" + storeCap;
		} else { 
			if (storeCap != 1.0)
				runName += "_storeCap" + storeCap;
			if (flowCap != 1.0)
				runName += "_flowCap" + flowCap;
		}
		
		if (POP_SCALE != 1) {
			runName += "_popScale" + POP_SCALE;
		}
		if (DELETE_ROUTES) {
			runName += "_noInitRoutes";
		}
		
		if (LONG_LANES){
			runName += "_longLanes";
		}
		if (LANE_CAP_FROM_NETWORK) {
			runName += "_laneCapFromNet";
		}
		
		StrategySettings[] strategies = config.strategy().getStrategySettings().toArray(new StrategySettings[0]);
		for (int i = 0; i < strategies.length; i++) {
			double weight = strategies[i].getWeight();
			if (weight != 0.0){
				String name = strategies[i].getStrategyName();
				if (name.equals(DefaultSelector.ChangeExpBeta.toString())){
					runName += "_ChExp" + weight;
					runName += "_beta" + (int)config.planCalcScore().getBrainExpBeta();
				} else if (name.equals(DefaultSelector.KeepLastSelected.toString())){
					runName += "_KeepLast" + weight;
				} else if (name.equals(DefaultStrategy.ReRoute.toString())){
					runName += "_ReRoute" + weight;
					runName += "_tbs" + config.travelTimeCalculator().getTraveltimeBinSize();
				} else if (name.equals(DefaultStrategy.TimeAllocationMutator.toString())){
					runName += "_TimeMut" + weight;
				} else {
					runName += "_" + name + weight;
				}
			}
		}
		
		if (SIGMA != 0.0)
			runName += "_sigma" + SIGMA;
		if (config.planCalcScore().getModes().get(TransportMode.car).getMonetaryDistanceRate() != 0.0)
			runName += "_distCost" + config.planCalcScore().getModes().get(TransportMode.car).getMonetaryDistanceRate();

		if (config.qsim().isUseLanes()){
			runName += "_lanes";
			// link 2 link vs node 2 node routing. this only has an effect if lanes are used
			if (config.controler().isLinkToLinkRoutingEnabled())
				runName += "_2link";
			else
				runName += "_2node";
		}			

		if (ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUP_NAME,
				SignalSystemsConfigGroup.class).isUseSignalSystems()) {
			switch (SIGNAL_TYPE){
			case MS_BTU_OPT:
				runName += "_BtuOpt";
				if (!NETWORK_TYPE.equals(BTU_BASE_NET))
					runName += "_BtuBaseNet" + BTU_BASE_DIR;
				break;
			case MS_RANDOM_OFFSETS:
				runName += "_rdmOff";
				break;
			case DOWNSTREAM_MS:
				runName += "_dwnBC";
				break;
			case DOWNSTREAM_ALLGREEN:
				runName += "_dwnGreen";
				break;
			case DOWNSTREAM_BTUOPT:
				runName += "_dwnOPT";
				break;
			case LAEMMER_FLEXIBLE:
				runName += "_laemmerFlex_" + LAEMMER_FLEX_STAB_STRATEGY;
				break;
			default:
				runName += "_" + SIGNAL_TYPE;
				break;
			}
			if (SIGNAL_TYPE.toString().contains("LAEMMER")) {
				runName += "_minG" + LAEMMER_MIN_G;
			} else if (SIGNAL_TYPE.toString().contains("SYLVIA")) {
				if (SYLVIA_FIXED_CYCLE)
					runName += "_fixedCycle";
				else
					runName += "_noFixedCycle";
				runName += "_maxExt" + SYLVIA_MAX_EXTENSION;
			}
			
			switch (INTERSECTION_LOGIC) {
			case CONFLICTING_DIRECTIONS_AND_TURN_RESTRICTIONS:
				runName += "_restrictLeftTurns";
				break;
			default:
				break;
			}
		}
		
		if (!PRICING_TYPE.equals(PricingType.NONE)){
			runName += "_" + PRICING_TYPE.toString();
		}
		
		if (config.strategy().getMaxAgentPlanMemorySize() != 0)
			runName += "_" + config.strategy().getMaxAgentPlanMemorySize() + "plans";

		if (USE_COUNTS){
			runName += "_counts";
		}
		
		runName += "_" + POP_TYPE;
		runName += "_" + NETWORK_TYPE;
		
		String outputDir = OUTPUT_BASE_DIR + OutputUtils.getCurrentDateIncludingTime();
		if (SIGNAL_TYPE.equals(SignalType.MS_RANDOM_GREENSPLITS)) {
			runName += "_coord" + signalCoordIndex;
			outputDir += "_coord" + signalCoordIndex;
			if (config.travelTimeCalculator().getTraveltimeBinSize() != 900) outputDir += "_tbs" + (int)config.travelTimeCalculator().getTraveltimeBinSize();
			if (config.planCalcScore().getBrainExpBeta() != 2) outputDir += "_beta" + config.planCalcScore().getBrainExpBeta();
			if (LANE_CAP_FROM_NETWORK) outputDir += "_linkCap";
			if (POP_SCALE == 90 && POP_TYPE.equals(PopulationType.BTU_POP_BTU_ROUTES_90)) outputDir += "_90uD";
			if (config.controler().getLastIteration() != 100) outputDir += "_" + config.controler().getLastIteration() + "it";
			outputDir += "/";
		}
		outputDir += "/";
//		String outputDir = OUTPUT_BASE_DIR + "run" + RUN_ID + "/"; 
//		String outputDir = OUTPUT_BASE_DIR + runName + "/"; 
		// create directory
		new File(outputDir).mkdirs();

		config.controler().setOutputDirectory(outputDir);
		LOG.info("The output will be written to " + outputDir);
		
		config.controler().setRunId(RUN_ID);
		
		// write run description
		PrintStream stream;
		String filename = outputDir + "runDescription.txt";
		try {
			stream = new PrintStream(new File(filename));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
		stream.println(runName);
		stream.close();
	}

	private static void writeInitFiles(Scenario scenario) {
		String outputDir = scenario.getConfig().controler().getOutputDirectory() + "initialFiles/";
		// create directory
		new File(outputDir).mkdirs();
		
		// write network and lanes
		new NetworkWriter(scenario.getNetwork()).write(outputDir + "network.xml");
		// correct paths before writing config
		scenario.getConfig().network().setInputFile("network.xml");
		if (scenario.getConfig().qsim().isUseLanes()) {
			new LanesWriter(scenario.getLanes()).write(outputDir + "lanes.xml");
			scenario.getConfig().network().setLaneDefinitionsFile("lanes.xml");
		}
		String outputDirTmp = scenario.getConfig().controler().getOutputDirectory();
		// adapt output dir to be able to run it on the cluster
		scenario.getConfig().controler().setOutputDirectory("/net/ils3/thunig/runs-svn/cottbus/createGridLock/run" + RUN_ID + "/");
		
		// write population
		new PopulationWriter(scenario.getPopulation()).write(outputDir + "plans.xml");
		scenario.getConfig().plans().setInputFile("plans.xml");
		
		// write signal files
		if (!SIGNAL_TYPE.equals(SignalType.NONE)) {
			SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
			new SignalSystemsWriter20(signalsData.getSignalSystemsData()).write(outputDir + "signalSystems.xml");
			new SignalControlWriter20(signalsData.getSignalControlData()).write(outputDir + "signalControl.xml");
			new SignalGroupsWriter20(signalsData.getSignalGroupsData()).write(outputDir + "signalGroups.xml");
			
			SignalSystemsConfigGroup signalsConfigGroup = ConfigUtils.addOrGetModule(scenario.getConfig(),
					SignalSystemsConfigGroup.GROUP_NAME, SignalSystemsConfigGroup.class);
			signalsConfigGroup.setSignalSystemFile("signalSystems.xml");
			signalsConfigGroup.setSignalGroupsFile("signalGroups.xml");
			signalsConfigGroup.setSignalControlFile("signalControl.xml");
		}
		
		// write config
		new ConfigWriter(scenario.getConfig()).write(outputDir + "config.xml");
		
		// restore output dir
		scenario.getConfig().controler().setOutputDirectory(outputDirTmp);
	}

}
