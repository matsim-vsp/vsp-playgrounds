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
package signals.gershenson;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.analysis.SignalAnalysisTool;
import org.matsim.contrib.signals.builder.Signals;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlData;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlDataFactory;
import org.matsim.contrib.signals.data.signalgroups.v20.*;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemControllerData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsDataFactory;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultSelector;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import scenarios.illustrative.singleCrossing.SingleCrossingScenario;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Test gershenson logic at an intersection with four incoming links and one signal each. No lanes are used.
 * 
 * Copied from SylviaTest.
 * 
 * @author tthunig
 *
 */
public class GershensonIT {

	private static final Logger log = Logger.getLogger(GershensonIT.class);
	private static final Id<SignalGroup> SIGNALGROUPID1 = Id.create("SignalGroup1", SignalGroup.class);
	private static final Id<SignalGroup> SIGNALGROUPID2 = Id.create("SignalGroup2", SignalGroup.class);
	private static final Id<SignalGroup> SIGNALGROUPID3 = Id.create("SignalGroup3", SignalGroup.class);
	private static final Id<SignalGroup> SIGNALGROUPID4 = Id.create("SignalGroup4", SignalGroup.class);
	private static final Id<SignalSystem> SIGNALSYSTEMID1 = Id.create("SignalSystem1", SignalSystem.class);
	private static final Id<SignalSystem> SIGNALSYSTEMID2 = Id.create("SignalSystem2", SignalSystem.class);
	
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();

	/**
	 * conflicting streams
	 */
	@Test
	public void testSingleCrossingUniformDemandAB() {
		String scenarioType = "singleCrossingNoBottlenecks";
		double[] noPersons = { 3600, 3600, 0, 0 };
		SignalAnalysisTool signalAnalyzer = runScenario(noPersons,scenarioType);

		// check signal results
		Map<Id<SignalGroup>, Double> totalSignalGreenTimes = signalAnalyzer.getTotalSignalGreenTime(); // should be more or less equal (OW direction is always favored as the first phase)
		Map<Id<SignalGroup>, Double> avgSignalGreenTimePerCycle = signalAnalyzer.calculateAvgSignalGreenTimePerFlexibleCycle(); // should be more or less equal and around 25
		Map<Id<SignalSystem>, Double> avgCycleTimePerSystem = signalAnalyzer.calculateAvgFlexibleCycleTimePerSignalSystem(); // should be 60

		

		log.info("total signal green times: " + totalSignalGreenTimes.get(SIGNALGROUPID1) + ", " + totalSignalGreenTimes.get(SIGNALGROUPID2));
		log.info("avg signal green times per cycle: " + avgSignalGreenTimePerCycle.get(SIGNALGROUPID1) + ", " + avgSignalGreenTimePerCycle.get(SIGNALGROUPID2));
		log.info("avg cycle time per system: " + avgCycleTimePerSystem.get(SIGNALSYSTEMID1));
		
		Assert.assertEquals("avg. signal green times of both groups are not similiar enough", 0.0, avgSignalGreenTimePerCycle.get(SIGNALGROUPID1) - avgSignalGreenTimePerCycle.get(SIGNALGROUPID2), 1.);
	}
	@Test
	public void testSingleCrossingDifferentUniformDemandAB() {
		String scenarioType = "singleCrossingNoBottlenecks";
		double[] noPersons = { 2000, 600, 0, 0 };
		SignalAnalysisTool signalAnalyzer = runScenario(noPersons,scenarioType);

		// check signal results
		Map<Id<SignalGroup>, Double> totalSignalGreenTimes = signalAnalyzer.getTotalSignalGreenTime(); // should be more or less equal (OW direction is always favored as the first phase)
		Map<Id<SignalGroup>, Double> avgSignalGreenTimePerCycle = signalAnalyzer.calculateAvgSignalGreenTimePerFlexibleCycle(); // should be more or less equal and around 25
		Map<Id<SignalSystem>, Double> avgCycleTimePerSystem = signalAnalyzer.calculateAvgFlexibleCycleTimePerSignalSystem(); // should be 60

		

		log.info("total signal green times: " + totalSignalGreenTimes.get(SIGNALGROUPID1) + ", " + totalSignalGreenTimes.get(SIGNALGROUPID2));
		log.info("avg signal green times per cycle: " + avgSignalGreenTimePerCycle.get(SIGNALGROUPID1) + ", " + avgSignalGreenTimePerCycle.get(SIGNALGROUPID2));
		log.info("avg cycle time per system: " + avgCycleTimePerSystem.get(SIGNALSYSTEMID1));
		
		Assert.assertTrue("total signal green time should be higher for Group1", totalSignalGreenTimes.get(SIGNALGROUPID1) > totalSignalGreenTimes.get(SIGNALGROUPID2));
		Assert.assertTrue("The ratio of demands should higher than the ratio of total green times", 2000/600 > totalSignalGreenTimes.get(SIGNALGROUPID1)/totalSignalGreenTimes.get(SIGNALGROUPID2));
	}
	
	
	@Test
	public void testSingleCrossingDifferentDemandABCD() {
		String scenarioType = "singleCrossingNoBottlenecks";
		double[] noPersons = { 1200, 600, 1200, 600 };
		SignalAnalysisTool signalAnalyzer = runScenario(noPersons,scenarioType);

		// check signal results
		Map<Id<SignalGroup>, Double> totalSignalGreenTimes = signalAnalyzer.getTotalSignalGreenTime(); // should be more or less equal (OW direction is always favored as the first phase)
		Map<Id<SignalGroup>, Double> avgSignalGreenTimePerCycle = signalAnalyzer.calculateAvgSignalGreenTimePerFlexibleCycle(); // should be more or less equal and around 25
		Map<Id<SignalSystem>, Double> avgCycleTimePerSystem = signalAnalyzer.calculateAvgFlexibleCycleTimePerSignalSystem(); // should be 60

		log.info("total signal green times: " + totalSignalGreenTimes.get(SIGNALGROUPID1) + ", " + totalSignalGreenTimes.get(SIGNALGROUPID2));
		log.info("avg signal green times per cycle: " + avgSignalGreenTimePerCycle.get(SIGNALGROUPID1) + ", " + avgSignalGreenTimePerCycle.get(SIGNALGROUPID2));
		log.info("avg cycle time per system: " + avgCycleTimePerSystem.get(SIGNALSYSTEMID1));
		
		Assert.assertTrue("average signal green time should be higher for Group1", avgSignalGreenTimePerCycle.get(SIGNALGROUPID1) > avgSignalGreenTimePerCycle.get(SIGNALGROUPID2));
		Assert.assertEquals("The ratio of total signal green times is", 1778./1590.,totalSignalGreenTimes.get(SIGNALGROUPID1)/totalSignalGreenTimes.get(SIGNALGROUPID2),0.00001);
	}
	

	/**
	 * demand crossing only in east-west direction
	 */
	@Ignore
	public void testSingleCrossingUniformDemandA() {
		String scenarioType = "singleCrossingNoBottlenecks";
		double[] noPersons = { 3600, 0, 0, 0 };
		SignalAnalysisTool signalAnalyzer = runScenario(noPersons,scenarioType);

		// check signal results
		Map<Id<SignalGroup>, Double> totalSignalGreenTimes = signalAnalyzer.getTotalSignalGreenTime(); // group 1 should have more total green time than group 2
		Map<Id<SignalGroup>, Double> avgSignalGreenTimePerCycle = signalAnalyzer.calculateAvgSignalGreenTimePerFlexibleCycle();
		Map<Id<SignalSystem>, Double> avgCycleTimePerSystem = signalAnalyzer.calculateAvgFlexibleCycleTimePerSignalSystem();


		log.info("total signal green times: " + totalSignalGreenTimes.get(SIGNALGROUPID1) + ", " + totalSignalGreenTimes.get(SIGNALGROUPID2));
		log.info("avg signal green times per cycle: " + avgSignalGreenTimePerCycle.get(SIGNALGROUPID1) + ", " + avgSignalGreenTimePerCycle.get(SIGNALGROUPID2));
		log.info("avg cycle time per system: " + avgCycleTimePerSystem.get(SIGNALSYSTEMID1));
		
		Assert.assertTrue("total signal green time of group 2 should be zero", totalSignalGreenTimes.get(SIGNALGROUPID2)==0);
		Assert.assertEquals("avg green time per cycle of signal group 1 is wrong", 3605, avgSignalGreenTimePerCycle.get(SIGNALGROUPID1), 5.);
		Assert.assertEquals("avg cycle time of the system is wrong", 3800, avgCycleTimePerSystem.get(SIGNALSYSTEMID1), 2.);
	}
	
	//TODO
	@Test
	public void testSingleCrossingwithOutboundCongestionFromA() {
		String scenarioTypeTestCase = "singleCrossingOneBottlenecks";
		String scenarioTypeNullCase = "singleCrossingNoBottlenecks";
		double[] noPersons = { 3600, 3600, 0, 0 };
		SignalAnalysisTool signalAnalyzerTestCase = runScenario(noPersons,scenarioTypeTestCase);
		SignalAnalysisTool signalAnalyzerNullCase = runScenario(noPersons,scenarioTypeNullCase);

		// check signal results TestCase
		Map<Id<SignalGroup>, Double> totalSignalGreenTimesTestCase = signalAnalyzerTestCase.getTotalSignalGreenTime(); 
		Map<Id<SignalGroup>, Double> avgSignalGreenTimePerCycleTestCase = signalAnalyzerTestCase.calculateAvgSignalGreenTimePerFlexibleCycle();
		Map<Id<SignalSystem>, Double> avgCycleTimePerSystemTestCase = signalAnalyzerTestCase.calculateAvgFlexibleCycleTimePerSignalSystem();
		Map<Id<SignalGroup>, Double> greenTimeRatiosTestCase = signalAnalyzerTestCase.calculateSignalGreenTimeRatios();
		// check signal results NullCase
		Map<Id<SignalGroup>, Double> totalSignalGreenTimesNullCase = signalAnalyzerNullCase.getTotalSignalGreenTime(); 
		Map<Id<SignalGroup>, Double> avgSignalGreenTimePerCycleNullCase = signalAnalyzerNullCase.calculateAvgSignalGreenTimePerFlexibleCycle();
		Map<Id<SignalSystem>, Double> avgCycleTimePerSystemNullCase = signalAnalyzerNullCase.calculateAvgFlexibleCycleTimePerSignalSystem();
		
		log.info("Data of TestCase");
		log.info("total signal green times: " + totalSignalGreenTimesTestCase.get(SIGNALGROUPID1) + ", " + totalSignalGreenTimesTestCase.get(SIGNALGROUPID2));
		log.info("avg signal green times per cycle: " + avgSignalGreenTimePerCycleTestCase.get(SIGNALGROUPID1) + ", " + avgSignalGreenTimePerCycleTestCase.get(SIGNALGROUPID2));
		log.info("avg cycle time per system: " + avgCycleTimePerSystemTestCase.get(SIGNALSYSTEMID1));
		log.info("Data of NullCase");
		log.info("total signal green times: " + totalSignalGreenTimesNullCase.get(SIGNALGROUPID1) + ", " + totalSignalGreenTimesNullCase.get(SIGNALGROUPID2));
		log.info("avg signal green times per cycle: " + avgSignalGreenTimePerCycleNullCase.get(SIGNALGROUPID1) + ", " + avgSignalGreenTimePerCycleNullCase.get(SIGNALGROUPID2));
		log.info("avg cycle time per system: " + avgCycleTimePerSystemNullCase.get(SIGNALSYSTEMID1));
		
		
		Assert.assertTrue("avg greentime cycle of group 2 should be higher then group1 ", avgSignalGreenTimePerCycleTestCase.get(SIGNALGROUPID2)>avgSignalGreenTimePerCycleTestCase.get(SIGNALGROUPID1));
		Assert.assertTrue("The total green Time of Group 2 should become larger if Rule 5 triggers for Group 1", totalSignalGreenTimesTestCase.get(SIGNALGROUPID2)>totalSignalGreenTimesNullCase.get(SIGNALGROUPID2));
	}

	
	
	@Test
	public void testSingleCrossingwithOutboundCongestionFromAB() {
		String scenarioType = "singleCrossingTwoBottlenecks";
		double[] noPersons = { 3600, 3600, 0, 0 };
		SignalAnalysisTool signalAnalyzer = runScenario(noPersons,scenarioType);

		// check signal results
		Map<Id<SignalGroup>, Double> totalSignalGreenTimes = signalAnalyzer.getTotalSignalGreenTime(); 
		Map<Id<SignalGroup>, Double> avgSignalGreenTimePerCycle = signalAnalyzer.calculateAvgSignalGreenTimePerFlexibleCycle();
		Map<Id<SignalSystem>, Double> avgCycleTimePerSystem = signalAnalyzer.calculateAvgFlexibleCycleTimePerSignalSystem();
		
		Map<Id<SignalGroup>, Double> greenTimeRatios = signalAnalyzer.calculateSignalGreenTimeRatios();
		

		log.info("total signal green times: " + totalSignalGreenTimes.get(SIGNALGROUPID1) + ", " + totalSignalGreenTimes.get(SIGNALGROUPID2));
		log.info("avg signal green times per cycle: " + avgSignalGreenTimePerCycle.get(SIGNALGROUPID1) + ", " + avgSignalGreenTimePerCycle.get(SIGNALGROUPID2));
		log.info("avg cycle time per system: " + avgCycleTimePerSystem.get(SIGNALSYSTEMID1));
		
		Assert.assertEquals("total greentime in respect to the average cycle time should be very similar ",0.0 , (totalSignalGreenTimes.get(SIGNALGROUPID2)-totalSignalGreenTimes.get(SIGNALGROUPID1))/avgCycleTimePerSystem.get(SIGNALSYSTEMID1),0.1);
		Assert.assertEquals("avg cycle time should be ", 232, avgCycleTimePerSystem.get(SIGNALSYSTEMID1), 1);

	}

	
		
	@Test
	public void testSingleCrossingStochasticDemandAB() {
		String scenarioType = "singleCrossingStochasticDemandAB";
		double[] noPersons = { 2800., 2800., 0., 0. };
		SignalAnalysisTool signalAnalyzerPlan = runScenario(noPersons,scenarioType);
		scenarioType = "singleCrossingNoBottlenecks";
		SignalAnalysisTool signalAnalyzerNull = runScenario(noPersons,scenarioType);
		
		
		// check signal results Plan
		Map<Id<SignalGroup>, Double> totalSignalGreenTimesPlan = signalAnalyzerPlan.getTotalSignalGreenTime(); 
		Map<Id<SignalGroup>, Double> avgSignalGreenTimePerCyclePlan = signalAnalyzerPlan.calculateAvgSignalGreenTimePerFlexibleCycle();
		Map<Id<SignalSystem>, Double> avgCycleTimePerSystemPlan = signalAnalyzerPlan.calculateAvgFlexibleCycleTimePerSignalSystem();
		
		// check signal results Null
		Map<Id<SignalGroup>, Double> totalSignalGreenTimesNull = signalAnalyzerNull.getTotalSignalGreenTime(); 
		Map<Id<SignalGroup>, Double> avgSignalGreenTimePerCycleNull = signalAnalyzerNull.calculateAvgSignalGreenTimePerFlexibleCycle();
		Map<Id<SignalSystem>, Double> avgCycleTimePerSystemNull = signalAnalyzerNull.calculateAvgFlexibleCycleTimePerSignalSystem();
		
		

		log.info("total signal green times: Plan " + totalSignalGreenTimesPlan.get(SIGNALGROUPID1) + ", Null " + totalSignalGreenTimesNull.get(SIGNALGROUPID1));
		log.info("total signal green times: Plan " + totalSignalGreenTimesPlan.get(SIGNALGROUPID2) + ", Null " + totalSignalGreenTimesNull.get(SIGNALGROUPID2));
		
		log.info("avg cycle time per system: " + avgCycleTimePerSystemPlan.get(SIGNALSYSTEMID1));
		
		Assert.assertEquals("The total green times in the stochastic case should be more or less equal (+/- 7 %)", 1.0, totalSignalGreenTimesPlan.get(SIGNALGROUPID2)/totalSignalGreenTimesPlan.get(SIGNALGROUPID1), 0.07);
		Assert.assertTrue("The total green time of Group 1 in the stochastic should higher then in the Non-Stochastic", totalSignalGreenTimesPlan.get(SIGNALGROUPID1)>totalSignalGreenTimesNull.get(SIGNALGROUPID1));
	}
	
	@Ignore
	public void testDoubleCrossingUniformDemandABC() {
		String scenarioType = "doubleCrossingUniformDemandABC";
		double[] noPersons = { 2800., 2800., 0., 0. ,0.,0.};
		SignalAnalysisTool signalAnalyzerPlan = runScenario(noPersons,scenarioType);
		
		// check signal results Plan
		Map<Id<SignalGroup>, Double> totalSignalGreenTimesPlan = signalAnalyzerPlan.getTotalSignalGreenTime(); 
		Map<Id<SignalGroup>, Double> avgSignalGreenTimePerCyclePlan = signalAnalyzerPlan.calculateAvgSignalGreenTimePerFlexibleCycle();
		Map<Id<SignalSystem>, Double> avgCycleTimePerSystemPlan = signalAnalyzerPlan.calculateAvgFlexibleCycleTimePerSignalSystem();
			

		log.info("total signal green times: Plan " + totalSignalGreenTimesPlan.get(SIGNALGROUPID1) + ", Null " );
		log.info("total signal green times: Plan " + totalSignalGreenTimesPlan.get(SIGNALGROUPID2) + ", Null ");
		
		log.info("avg cycle time per system: " + avgCycleTimePerSystemPlan.get(SIGNALSYSTEMID1));
		
		
	}
	
	@Test
	
	public void testGershensonWithLanes() {
		double flowNS = 360;
		double flowWE = 1440;
		SingleCrossingScenario scenarioCreation = new SingleCrossingScenario(flowNS, flowWE, SingleCrossingScenario.SignalControl.GERSHENSON, false, false, true, true, false);
		Controler controler = scenarioCreation.defineControler();
		
		// add signal analysis tool
		SignalAnalysisTool signalAnalyzer = new SignalAnalysisTool();
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.addEventHandlerBinding().toInstance(signalAnalyzer);
				this.addControlerListenerBinding().toInstance(signalAnalyzer);
			}
		});
		controler.run();

		// check signal results
		Map<Id<SignalGroup>, Double> totalSignalGreenTimes = signalAnalyzer.getTotalSignalGreenTime();
		Map<Id<SignalSystem>, Double> avgCycleTimePerSystem = signalAnalyzer
				.calculateAvgFlexibleCycleTimePerSignalSystem();

		log.info("total signal green times: Group 1 "
				+ totalSignalGreenTimes.get(SingleCrossingScenario.signalGroupId1));
		log.info("total signal green times: Group 2 "
				+ totalSignalGreenTimes.get(SingleCrossingScenario.signalGroupId2));
		log.info("total signal green times: Group 3 "
				+ totalSignalGreenTimes.get(SingleCrossingScenario.signalGroupId3));
		log.info("total signal green times: Group 4 "
				+ totalSignalGreenTimes.get(SingleCrossingScenario.signalGroupId4));
		log.info("avg cycle time: " + avgCycleTimePerSystem.get(SIGNALSYSTEMID1));

		// TODO test for correct results
		
		
		Assert.assertEquals("Total Signal Green Time Group1 is wrong", 3554.0,
				totalSignalGreenTimes.get(SingleCrossingScenario.signalGroupId1), 0.00001);
		Assert.assertEquals("Total Signal Green Time Group2 is wrong", 1079.0,
				totalSignalGreenTimes.get(SingleCrossingScenario.signalGroupId2), 0.00001);
		Assert.assertEquals("Avg CycleTime should be ", 71.15384615384616, avgCycleTimePerSystem.get(SIGNALSYSTEMID1),
				0.00001);
	}
	
	
	
//------------------------------------------	
	private SignalAnalysisTool runScenario(double[] noPersons, String scenarioType) {
		Config config = defineConfig();

		// ---------- VIS
		OTFVisConfigGroup otfvisConfig = ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.class);
		otfvisConfig.setDrawTime(true);
		otfvisConfig.setAgentSize(80f);
		config.qsim().setSnapshotStyle(QSimConfigGroup.SnapshotStyle.withHoles);
		// ---------- VIS
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		
		// add missing scenario elements
		scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());

		createScenarioElements(scenario, noPersons, scenarioType);
		

		Controler controler = new Controler(scenario);
		// add the signals module with gershenson controller
//		SignalsModule signalsModule = new SignalsModule();
		Signals.Configurator configurator = new Signals.Configurator( controler ) ;
		configurator.addSignalControllerFactory(GershensonSignalController.IDENTIFIER,
				GershensonSignalController.GershensonFactory.class);
//		controler.addOverridingModule(signalsModule);
        
        // bind gershenson config
        controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				GershensonConfig gershensonConfig = new GershensonConfig();
				bind(GershensonConfig.class).toInstance(gershensonConfig);
			}
		});
		
		//controler.addOverridingModule( new OTFVisWithSignalsLiveModule() ) ;
		
		// add signal analysis tool
		SignalAnalysisTool signalAnalyzer = new SignalAnalysisTool();
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.addEventHandlerBinding().toInstance(signalAnalyzer);
				this.addControlerListenerBinding().toInstance(signalAnalyzer);
			}
		});
		
		// ---------- VIS
//		controler.addOverridingModule(new OTFVisWithSignalsLiveModule());
		// This was recently commented in by sbraun.  But you can't have vis in regression tests.  It produces
		// spurious failures with output such as
		//		libEGL warning: DRI2: xcb_connect failed
		//		libEGL warning: DRI2: xcb_connect failed
		//		libEGL warning: GLX: XOpenDisplay failed
		// I.e. the JVM tries to connect to the xserver, but that is not there.  You may be able to avoid such
		// issues with the "headless" option in the pom.xml (I put this in there), but better to not use
		// otfvis at all in tests.  kai, aug'18
		// ---------- VIS

		controler.run();

		return signalAnalyzer;
	}

	private void createScenarioElements(Scenario scenario, double[] noPersons, String scenarioType) {
		boolean[] stochasticDemand = new boolean[noPersons.length];
		for(int i = 0; i < stochasticDemand.length; i++) stochasticDemand[i] = false;
		
		if (scenarioType.equals("singleCrossingNoBottlenecks")) {
			createNetworkSingleCrossing(scenario.getNetwork(), 0);
			createPopulationScenario(scenario.getPopulation(), noPersons, stochasticDemand);
			createSignalsSingleCrossing(scenario, true);
		}
		if (scenarioType.equals("singleCrossingOneBottlenecks")) {
			createNetworkSingleCrossing(scenario.getNetwork(), 1);
			createPopulationScenario(scenario.getPopulation(), noPersons, stochasticDemand);
			createSignalsSingleCrossing(scenario, true);
		}
		if (scenarioType.equals("singleCrossingTwoBottlenecks")) {
			createNetworkSingleCrossing(scenario.getNetwork(), 2);
			createPopulationScenario(scenario.getPopulation(), noPersons, stochasticDemand);
			createSignalsSingleCrossing(scenario, false);
		}
		if (scenarioType.equals("singleCrossingStochasticDemandAB")) {
			stochasticDemand[0]=true;
			stochasticDemand[1]=true;
			createNetworkSingleCrossing(scenario.getNetwork(), 0);
			createPopulationScenario(scenario.getPopulation(), noPersons, stochasticDemand);
			createSignalsSingleCrossing(scenario, false);
		}
		if(scenarioType.equals("doubleCrossingUniformDemandABC")) {
			createNetworkScenario2(scenario.getNetwork(),0);
			createPopulationDoubleCrossing(scenario.getPopulation(), noPersons);
			createSignalsSingleCrossing(scenario, false);
		}	

		
	}

	/**
	 * creates a network like this:
	 * 
	 * 					 6
	 * 					 ^
	 * 					 |
	 * 					 v
	 * 					 7
	 * 					 ^
	 * 					 |
	 * 					 v
	 * 1 <----> 2 <----> 3 <----> 4 <----> 5
	 * 					 ^
	 * 					 |
	 * 					 v
	 * 					 8
	 * 					 ^
	 * 					 |
	 * 					 v
	 * 					 9 
	 * 
	 * @param net
	 *            the object where the network should be stored
	 */
	private static void createNetworkSingleCrossing(Network net, int numberOfBottlenecks) {
		NetworkFactory fac = net.getFactory();

		net.addNode(fac.createNode(Id.createNodeId(1), new Coord(-2000, 0)));
		net.addNode(fac.createNode(Id.createNodeId(2), new Coord(-1000, 0)));
		net.addNode(fac.createNode(Id.createNodeId(3), new Coord(0, 0)));
		net.addNode(fac.createNode(Id.createNodeId(4), new Coord(1000, 0)));
		net.addNode(fac.createNode(Id.createNodeId(5), new Coord(2000, 0)));
		net.addNode(fac.createNode(Id.createNodeId(6), new Coord(0, 2000)));
		net.addNode(fac.createNode(Id.createNodeId(7), new Coord(0, 1000)));
		net.addNode(fac.createNode(Id.createNodeId(8), new Coord(0, -1000)));
		net.addNode(fac.createNode(Id.createNodeId(9), new Coord(0, -2000)));

		String[] links = { "1_2", "2_1", "2_3", "3_2", "3_4", "4_3", "4_5", "5_4", "6_7", "7_6", "7_3", "3_7", "3_8", "8_3", "8_9", "9_8" };

		for (String linkId : links) {
			String fromNodeId = linkId.split("_")[0];
			String toNodeId = linkId.split("_")[1];
			Link link = fac.createLink(Id.createLinkId(linkId), net.getNodes().get(Id.createNodeId(fromNodeId)), net.getNodes().get(Id.createNodeId(toNodeId)));
			//link.setCapacity(7200);
			link.setLength(1000);
			link.setFreespeed(10);
			link.setCapacity(4800);
		
			
			//Reset Capacity if Bottleneck	
			if (numberOfBottlenecks==1 && (fromNodeId.equals("3") && toNodeId.equals("4")) ) {
				link.setCapacity(20);				
			}
			if (numberOfBottlenecks==2 && ((fromNodeId.equals("3") && toNodeId.equals("4"))|| fromNodeId.equals("3") && toNodeId.equals("8"))) {
				link.setCapacity(20);				
			}
			
			net.addLink(link);
		}
	}
	

		

	private static void createPopulationScenario(Population population, double[] noPersons, boolean[] stochasticDemand) {
		String[] odRelations = { "1_2-4_5", "6_7-8_9", "4_5-1_2", "8_9-6_7"};
		int odIndex = 0;

		for (String od : odRelations) {
			String fromLinkId = od.split("-")[0];
			String toLinkId = od.split("-")[1];
			
			double demand = noPersons[odIndex];

			if (!stochasticDemand[odIndex]) {
				for (int i = 0; i < noPersons[odIndex]; i++) {
					// create a person
					Person person = population.getFactory().createPerson(Id.createPersonId(od + "-" + i));
					population.addPerson(person);
	
					// create a plan for the person that contains all this information
					Plan plan = population.getFactory().createPlan();
					person.addPlan(plan);
	
					// create a start activity at the from link
					Activity startAct = population.getFactory().createActivityFromLinkId("dummy", Id.createLinkId(fromLinkId));
					// distribute agents uniformly during one hour.
					startAct.setEndTime(i*(3600/demand));
					plan.addActivity(startAct);
	
					// create a dummy leg
					plan.addLeg(population.getFactory().createLeg(TransportMode.car));
	
					// create a drain activity at the to link
					Activity drainAct = population.getFactory().createActivityFromLinkId("dummy", Id.createLinkId(toLinkId));
					plan.addActivity(drainAct);
				}
				odIndex++;
			} else {
				Random rnd = new Random(odIndex);
				Map<Integer, Integer> startAtTime = new HashMap<Integer, Integer>();
				for (int i = 0; i < demand; i++) {
					//if demand ==3600 this becomes 0.5
                    double expT = 1 - Math.exp(-demand*Math.log(2.)/3600);
                    double p1 = rnd.nextDouble();
                    if (p1 < expT) {
                        double p2 = rnd.nextDouble();
                        int randomDemand = 0;   
                        while(true) {
                        	p2 = rnd.nextDouble();
                        	double expN = Math.exp(-Math.log(1.1)*(randomDemand++));          
                        	if(p2>expN) break;
                        }
                        startAtTime.put(i, randomDemand++);
                    }   
                }
				
				//Plans for stochastic demands
				for (Integer i :startAtTime.keySet()) {
					for(int j =0; j < (int)startAtTime.get(i); j++) {
						// create a person
						Person person = population.getFactory().createPerson(Id.createPersonId(od + "-" + i.toString()+"-"+j));
						population.addPerson(person);
		
						// create a plan for the person that contains all this information
						Plan plan = population.getFactory().createPlan();
						person.addPlan(plan);
		
						// create a start activity at the from link
						Activity startAct = population.getFactory().createActivityFromLinkId("dummy", Id.createLinkId(fromLinkId));
						// distribute agents uniformly during one hour.
						startAct.setEndTime(i);
						plan.addActivity(startAct);
		
						// create a dummy leg
						plan.addLeg(population.getFactory().createLeg(TransportMode.car));
		
						// create a drain activity at the to link
						Activity drainAct = population.getFactory().createActivityFromLinkId("dummy", Id.createLinkId(toLinkId));
						plan.addActivity(drainAct);	
					}	
				}
				odIndex++;
			}
			
		}
	}

	private void createSignalsSingleCrossing(Scenario scenario, boolean allowedDirectionStraight) {
		SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		SignalSystemsData signalSystems = signalsData.getSignalSystemsData();
		SignalSystemsDataFactory sysFac = signalSystems.getFactory();
		SignalGroupsData signalGroups = signalsData.getSignalGroupsData();
		SignalControlData signalControl = signalsData.getSignalControlData();
		SignalControlDataFactory conFac = signalControl.getFactory();

		// create signal system
		//Id<SignalSystem> signalSystemId = Id.create("SignalSystem1", SignalSystem.class);
		SignalSystemData signalSystem = sysFac.createSignalSystemData(SIGNALSYSTEMID1);
		signalSystems.addSignalSystemData(signalSystem);

		// create a signal for every inLink
		for (Id<Link> inLinkId : scenario.getNetwork().getNodes().get(Id.createNodeId(3)).getInLinks().keySet()) {
			SignalData signal = sysFac.createSignalData(Id.create("Signal" + inLinkId, Signal.class));
			signalSystem.addSignalData(signal);
			signal.setLinkId(inLinkId);
			if(allowedDirectionStraight) {
				if(inLinkId.equals(Id.createLinkId("2_3"))||inLinkId.equals(Id.createLinkId("4_3"))) {
					if(inLinkId.equals(Id.createLinkId("2_3"))) {
						signal.addTurningMoveRestriction(Id.createLinkId("3_4"));
					} else signal.addTurningMoveRestriction(Id.createLinkId("3_2"));
				} else {
					if (inLinkId.equals(Id.createLinkId("7_3"))) {
						signal.addTurningMoveRestriction(Id.createLinkId("3_8"));
					} else signal.addTurningMoveRestriction(Id.createLinkId("3_7"));
				}
			}
		}

		// group signals with non conflicting streams
		//Id<SignalGroup> signalGroupId1 = Id.create("SignalGroup1", SignalGroup.class);
		SignalGroupData signalGroup1 = signalGroups.getFactory().createSignalGroupData(SIGNALSYSTEMID1, SIGNALGROUPID1);
		signalGroup1.addSignalId(Id.create("Signal2_3", Signal.class));
		signalGroup1.addSignalId(Id.create("Signal4_3", Signal.class));
		signalGroups.addSignalGroupData(signalGroup1);

		//Id<SignalGroup> signalGroupId2 = Id.create("SignalGroup2", SignalGroup.class);
		SignalGroupData signalGroup2 = signalGroups.getFactory().createSignalGroupData(SIGNALSYSTEMID1, SIGNALGROUPID2);
		signalGroup2.addSignalId(Id.create("Signal7_3", Signal.class));
		signalGroup2.addSignalId(Id.create("Signal8_3", Signal.class));
		signalGroups.addSignalGroupData(signalGroup2);

		// create the signal control
		SignalSystemControllerData signalSystemControl = conFac.createSignalSystemControllerData(SIGNALSYSTEMID1);
		signalSystemControl.setControllerIdentifier(GershensonSignalController.IDENTIFIER);
		signalControl.addSignalSystemControllerData(signalSystemControl);

		// note: no signal plans are needed for GershensonController
	}

	private Config defineConfig() {
		Config config = ConfigUtils.createConfig();
		
		config.controler().setOutputDirectory(testUtils.getOutputDirectory());

		// set number of iterations
		config.controler().setLastIteration(0);

		// able or enable signals and lanes
		SignalSystemsConfigGroup signalConfigGroup = ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUP_NAME, SignalSystemsConfigGroup.class);
		signalConfigGroup.setUseSignalSystems(true);
		
		
		config.qsim().setUsingFastCapacityUpdate(false);

		// define strategies:
		{
			StrategySettings strat = new StrategySettings();
			strat.setStrategyName(DefaultSelector.KeepLastSelected.toString());
			strat.setWeight(0.9);
			strat.setDisableAfter(config.controler().getLastIteration());
			config.strategy().addStrategySettings(strat);
		}

		config.qsim().setStuckTime(3600);
		config.qsim().setRemoveStuckVehicles(false);

		config.qsim().setStartTime(0);
		config.qsim().setEndTime(3 * 3600);

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		config.vspExperimental().setWritingOutputEvents(false);
		config.planCalcScore().setWriteExperiencedPlans(false);
		config.controler().setCreateGraphs(false);

		// define activity types
		{
			ActivityParams dummyAct = new ActivityParams("dummy");
			dummyAct.setTypicalDuration(12 * 3600);
			config.planCalcScore().addActivityParams(dummyAct);
		}

		return config;
	}
//-------------------------------------------------------------------

	
	/**
	 * creates a network like this:
	 * 
	 * 					 8				   12
	 * 					 ^				   ^
	 * 					 |				   |
	 * 					 v				   v
	 * 					 9				   13
	 * 					 ^				   ^
	 * 					 |				   |
	 * 					 v				   v
	 * 1 <----> 2 <----> 3 <----> 4 <----> 5 <----> 6 <----> 7
	 * 					 ^				   ^
	 * 					 |				   |
	 * 					 v				   v
	 * 					 10				   14
	 * 					 ^				   ^
	 * 					 |				   |
	 * 					 v				   v
	 * 					 11				   15
	 * 
	 * 
	 * @param net
	 *            the object where the network should be stored
	 */
	private static void createNetworkScenario2(Network net, int numberOfBottlenecks) {
		NetworkFactory fac = net.getFactory();

		net.addNode(fac.createNode(Id.createNodeId(1), new Coord(-2000, 0)));
		net.addNode(fac.createNode(Id.createNodeId(2), new Coord(-1000, 0)));
		net.addNode(fac.createNode(Id.createNodeId(3), new Coord(0, 0)));
		net.addNode(fac.createNode(Id.createNodeId(4), new Coord(1000, 0)));
		net.addNode(fac.createNode(Id.createNodeId(5), new Coord(2000, 0)));
		net.addNode(fac.createNode(Id.createNodeId(6), new Coord(3000, 0)));
		net.addNode(fac.createNode(Id.createNodeId(7), new Coord(4000, 0)));
		net.addNode(fac.createNode(Id.createNodeId(8), new Coord(0,-2000)));
		net.addNode(fac.createNode(Id.createNodeId(9), new Coord(0,-1000)));
		net.addNode(fac.createNode(Id.createNodeId(10), new Coord(0,1000)));
		net.addNode(fac.createNode(Id.createNodeId(11), new Coord(0,2000)));
		net.addNode(fac.createNode(Id.createNodeId(12), new Coord(2000,-2000)));
		net.addNode(fac.createNode(Id.createNodeId(13), new Coord(2000,-1000)));
		net.addNode(fac.createNode(Id.createNodeId(14), new Coord(2000,1000)));			
		net.addNode(fac.createNode(Id.createNodeId(15), new Coord(2000, 2000)));

		
		
		String[] links = { "1_2", "2_1", "2_3", "3_2", "3_4", "4_3", "4_5", "5_4", "5_6", "6_5", "6_7", "7_6",
				"8_9","9_8","9_3","3_9","3_10","10_3","10_11","11_10","12_13","13_12","13_5","5_13","5_14","14_5","14_15","15_14"};

		for (String linkId : links) {
			String fromNodeId = linkId.split("_")[0];
			String toNodeId = linkId.split("_")[1];
			Link link = fac.createLink(Id.createLinkId(linkId), net.getNodes().get(Id.createNodeId(fromNodeId)), net.getNodes().get(Id.createNodeId(toNodeId)));
			link.setLength(1000);
			link.setFreespeed(10);
			link.setCapacity(4800);
			
			//Reset Capacity if Bottleneck	
			if (numberOfBottlenecks==1 && (fromNodeId.equals("3") && toNodeId.equals("4")) ) {
				link.setCapacity(1);				
			}
			if (numberOfBottlenecks>=1 && numberOfBottlenecks<=2
					&& fromNodeId.equals("5") && toNodeId.equals("6")) {
				link.setCapacity(1);				
			}
			
			net.addLink(link);
			
			
			
		}
	}
	
	private static void createPopulationDoubleCrossing(Population population, double[] noPersons) {
		String[] odRelations = { "1_2-6_7", "8_9-10_11", "12_13-14_15", "7_6-2_1", "11_10-9_8" ,"15_14-13_12"};
		int odIndex = 0;

		for (String od : odRelations) {
			String fromLinkId = od.split("-")[0];
			String toLinkId = od.split("-")[1];

			for (int i = 0; i < noPersons[odIndex]; i++) {
				// create a person
				Person person = population.getFactory().createPerson(Id.createPersonId(od + "-" + i));
				population.addPerson(person);

				// create a plan for the person that contains all this information
				Plan plan = population.getFactory().createPlan();
				person.addPlan(plan);

				// create a start activity at the from link
				Activity startAct = population.getFactory().createActivityFromLinkId("dummy", Id.createLinkId(fromLinkId));
				// distribute agents uniformly during one hour.
				startAct.setEndTime(i);
				plan.addActivity(startAct);

				// create a dummy leg
				plan.addLeg(population.getFactory().createLeg(TransportMode.car));

				// create a drain activity at the to link
				Activity drainAct = population.getFactory().createActivityFromLinkId("dummy", Id.createLinkId(toLinkId));
				plan.addActivity(drainAct);
			}
			odIndex++;
		}
	}
	
	private void createSignalsDoubleCrossing(Scenario scenario, boolean bottleneck) {
		SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		SignalSystemsData signalSystems = signalsData.getSignalSystemsData();
		SignalSystemsDataFactory sysFac = signalSystems.getFactory();
		SignalGroupsData signalGroups = signalsData.getSignalGroupsData();
		SignalControlData signalControl = signalsData.getSignalControlData();
		SignalControlDataFactory conFac = signalControl.getFactory();

		// create signal system
		SignalSystemData signalSystem1 = sysFac.createSignalSystemData(SIGNALSYSTEMID1);
		SignalSystemData signalSystem2 = sysFac.createSignalSystemData(SIGNALSYSTEMID2);

		
		signalSystems.addSignalSystemData(signalSystem1);
		signalSystems.addSignalSystemData(signalSystem2);

		

		// create a signal for every inLink
		for (Id<Link> inLinkId : scenario.getNetwork().getNodes().get(Id.createNodeId(3)).getInLinks().keySet()) {
			SignalData signal = sysFac.createSignalData(Id.create("Signal" + inLinkId, Signal.class));
			signalSystem1.addSignalData(signal);
			signal.setLinkId(inLinkId);
		}
		for (Id<Link> inLinkId : scenario.getNetwork().getNodes().get(Id.createNodeId(5)).getInLinks().keySet()) {
			SignalData signal = sysFac.createSignalData(Id.create("Signal" + inLinkId, Signal.class));
			signalSystem2.addSignalData(signal);
			signal.setLinkId(inLinkId);
		}

		
		//SignalSystem1
		// group signals with non conflicting streams
		SignalGroupData signalGroup1Sys1 = signalGroups.getFactory().createSignalGroupData(SIGNALSYSTEMID1, SIGNALGROUPID1);
		signalGroup1Sys1.addSignalId(Id.create("Signal2_3", Signal.class));
		signalGroup1Sys1.addSignalId(Id.create("Signal4_3", Signal.class));
		signalGroups.addSignalGroupData(signalGroup1Sys1);

		SignalGroupData signalGroup2Sys1 = signalGroups.getFactory().createSignalGroupData(SIGNALSYSTEMID1, SIGNALGROUPID2);
		signalGroup2Sys1.addSignalId(Id.create("Signal19_3", Signal.class));
		signalGroup2Sys1.addSignalId(Id.create("Signal110_3", Signal.class));
		signalGroups.addSignalGroupData(signalGroup2Sys1);

		//SignalSystem2
		// group signals with non conflicting streams
		SignalGroupData signalGroup1Sys2 = signalGroups.getFactory().createSignalGroupData(SIGNALSYSTEMID2, SIGNALGROUPID3);
		signalGroup1Sys2.addSignalId(Id.create("Signal4_5", Signal.class));
		signalGroup1Sys2.addSignalId(Id.create("Signal6_5", Signal.class));
		signalGroups.addSignalGroupData(signalGroup1Sys2);

		SignalGroupData signalGroup2Sys2 = signalGroups.getFactory().createSignalGroupData(SIGNALSYSTEMID2, SIGNALGROUPID4);
		signalGroup2Sys2.addSignalId(Id.create("Signal213_5", Signal.class));
		signalGroup2Sys2.addSignalId(Id.create("Signal214_5", Signal.class));
		signalGroups.addSignalGroupData(signalGroup2Sys2);
	
		
		// create the signal control System1
		SignalSystemControllerData signalSystemControl1 = conFac.createSignalSystemControllerData(SIGNALSYSTEMID1);
		signalSystemControl1.setControllerIdentifier(GershensonSignalController.IDENTIFIER);
		signalControl.addSignalSystemControllerData(signalSystemControl1);
		// create the signal control System2
		SignalSystemControllerData signalSystemControl2 = conFac.createSignalSystemControllerData(SIGNALSYSTEMID2);
		signalSystemControl2.setControllerIdentifier(GershensonSignalController.IDENTIFIER);
		signalControl.addSignalSystemControllerData(signalSystemControl2);
	}
}
