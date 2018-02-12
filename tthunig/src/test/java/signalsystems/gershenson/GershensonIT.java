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
package signalsystems.gershenson;

import java.util.Map;

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
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlDataFactory;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalSystemControllerData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsDataFactory;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultSelector;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import analysis.signals.TtSignalAnalysisTool;
import signals.CombinedSignalsModule;
import signals.gershenson.DgRoederGershensonSignalController;

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
	private static final Id<SignalSystem> SIGNALSYSTEMID = Id.create("SignalSystem1", SignalSystem.class);
	
	
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();

	/**
	 * conflicting streams
	 */
	@Test
	public void testSingleCrossingUniformDemandAB() {
		double[] noPersons = { 3600, 3600, 0, 0 };
		TtSignalAnalysisTool signalAnalyzer = runScenario(noPersons);

		// check signal results
		Map<Id<SignalGroup>, Double> totalSignalGreenTimes = signalAnalyzer.getTotalSignalGreenTime(); // should be more or less equal (OW direction is always favored as the first phase)
		Map<Id<SignalGroup>, Double> avgSignalGreenTimePerCycle = signalAnalyzer.calculateAvgSignalGreenTimePerFlexibleCycle(); // should be more or less equal and around 25
		Map<Id<SignalSystem>, Double> avgCycleTimePerSystem = signalAnalyzer.calculateAvgFlexibleCycleTimePerSignalSystem(); // should be 60

		

		log.info("total signal green times: " + totalSignalGreenTimes.get(SIGNALGROUPID1) + ", " + totalSignalGreenTimes.get(SIGNALGROUPID2));
		log.info("avg signal green times per cycle: " + avgSignalGreenTimePerCycle.get(SIGNALGROUPID1) + ", " + avgSignalGreenTimePerCycle.get(SIGNALGROUPID2));
		log.info("avg cycle time per system: " + avgCycleTimePerSystem.get(SIGNALSYSTEMID));
		
		Assert.assertEquals("total signal green times of both groups are not similiar enough", 0.0, totalSignalGreenTimes.get(SIGNALGROUPID1) - totalSignalGreenTimes.get(SIGNALGROUPID2), 3.);
		Assert.assertEquals("avg green time per cycle of signal group 1 is wrong", 3.9889380530973453, avgSignalGreenTimePerCycle.get(SIGNALGROUPID1), MatsimTestUtils.EPSILON);
		Assert.assertEquals("avg green time per cycle of signal group 2 is wrong", 3.993362831858407, avgSignalGreenTimePerCycle.get(SIGNALGROUPID2), MatsimTestUtils.EPSILON);
		Assert.assertEquals("avg cycle time of the system is wrong", 8.639380530973451, avgCycleTimePerSystem.get(SIGNALSYSTEMID), MatsimTestUtils.EPSILON);
	}
	@Test
	public void testSingleCrossingDifferentDemandAB() {
		double[] noPersons = { 6000, 600, 0, 0 };
		TtSignalAnalysisTool signalAnalyzer = runScenario(noPersons);

		// check signal results
		Map<Id<SignalGroup>, Double> totalSignalGreenTimes = signalAnalyzer.getTotalSignalGreenTime(); // should be more or less equal (OW direction is always favored as the first phase)
		Map<Id<SignalGroup>, Double> avgSignalGreenTimePerCycle = signalAnalyzer.calculateAvgSignalGreenTimePerFlexibleCycle(); // should be more or less equal and around 25
		Map<Id<SignalSystem>, Double> avgCycleTimePerSystem = signalAnalyzer.calculateAvgFlexibleCycleTimePerSignalSystem(); // should be 60

		

		log.info("total signal green times: " + totalSignalGreenTimes.get(SIGNALGROUPID1) + ", " + totalSignalGreenTimes.get(SIGNALGROUPID2));
		log.info("avg signal green times per cycle: " + avgSignalGreenTimePerCycle.get(SIGNALGROUPID1) + ", " + avgSignalGreenTimePerCycle.get(SIGNALGROUPID2));
		log.info("avg cycle time per system: " + avgCycleTimePerSystem.get(SIGNALSYSTEMID));
		
		Assert.assertTrue("total signal green time should be higher for Group1", totalSignalGreenTimes.get(SIGNALGROUPID1) > totalSignalGreenTimes.get(SIGNALGROUPID2));
		Assert.assertTrue("The ratio of demands should higher than the ratio of total green times", 6000/600 > totalSignalGreenTimes.get(SIGNALGROUPID1)/totalSignalGreenTimes.get(SIGNALGROUPID2));
	}
	
	@Test
	public void testSingleCrossingDifferentDemandABCD() {
		double[] noPersons = { 1200, 600, 600, 1200 };
		TtSignalAnalysisTool signalAnalyzer = runScenario(noPersons);

		// check signal results
		Map<Id<SignalGroup>, Double> totalSignalGreenTimes = signalAnalyzer.getTotalSignalGreenTime(); // should be more or less equal (OW direction is always favored as the first phase)
		Map<Id<SignalGroup>, Double> avgSignalGreenTimePerCycle = signalAnalyzer.calculateAvgSignalGreenTimePerFlexibleCycle(); // should be more or less equal and around 25
		Map<Id<SignalSystem>, Double> avgCycleTimePerSystem = signalAnalyzer.calculateAvgFlexibleCycleTimePerSignalSystem(); // should be 60

		log.info("total signal green times: " + totalSignalGreenTimes.get(SIGNALGROUPID1) + ", " + totalSignalGreenTimes.get(SIGNALGROUPID2));
		log.info("avg signal green times per cycle: " + avgSignalGreenTimePerCycle.get(SIGNALGROUPID1) + ", " + avgSignalGreenTimePerCycle.get(SIGNALGROUPID2));
		log.info("avg cycle time per system: " + avgCycleTimePerSystem.get(SIGNALSYSTEMID));
		
	//	Assert.assertTrue("total signal green time should be higher for Group1", totalSignalGreenTimes.get(SIGNALGROUPID1) > totalSignalGreenTimes.get(SIGNALGROUPID2));
	//	Assert.assertTrue("The ratio of demands should higher than the ratio of total green times", 6000/600 > totalSignalGreenTimes.get(SIGNALGROUPID1)/totalSignalGreenTimes.get(SIGNALGROUPID2));
	}
	

	/**
	 * demand crossing only in east-west direction
	 */
	@Test
	public void testSingleCrossingUniformDemandA() {
		double[] noPersons = { 3600, 0, 0, 0 };
		TtSignalAnalysisTool signalAnalyzer = runScenario(noPersons);

		// check signal results
		Map<Id<SignalGroup>, Double> totalSignalGreenTimes = signalAnalyzer.getTotalSignalGreenTime(); // group 1 should have more total green time than group 2
		Map<Id<SignalGroup>, Double> avgSignalGreenTimePerCycle = signalAnalyzer.calculateAvgSignalGreenTimePerFlexibleCycle();
		Map<Id<SignalSystem>, Double> avgCycleTimePerSystem = signalAnalyzer.calculateAvgFlexibleCycleTimePerSignalSystem();


		log.info("total signal green times: " + totalSignalGreenTimes.get(SIGNALGROUPID1) + ", " + totalSignalGreenTimes.get(SIGNALGROUPID2));
		log.info("avg signal green times per cycle: " + avgSignalGreenTimePerCycle.get(SIGNALGROUPID1) + ", " + avgSignalGreenTimePerCycle.get(SIGNALGROUPID2));
		log.info("avg cycle time per system: " + avgCycleTimePerSystem.get(SIGNALSYSTEMID));
		
		Assert.assertTrue("total signal green time of group 1 is not bigger than of group 2", totalSignalGreenTimes.get(SIGNALGROUPID1) > totalSignalGreenTimes.get(SIGNALGROUPID2));
		Assert.assertTrue("total signal green time of group 2 should be zero", totalSignalGreenTimes.get(SIGNALGROUPID2)==0);
		Assert.assertEquals("avg green time per cycle of signal group 1 is wrong", 1802.5, avgSignalGreenTimePerCycle.get(SIGNALGROUPID1), 5.);
		Assert.assertEquals("avg cycle time of the system is wrong", 1951, avgCycleTimePerSystem.get(SIGNALSYSTEMID), 2.);
	}
	@Test
	public void testScenario1DemandABCD() {
		double[] noPersons = { 3600, 2400, 1200, 2400 };
		TtSignalAnalysisTool signalAnalyzer = runScenario(noPersons);

		// check signal results
		Map<Id<SignalGroup>, Double> totalSignalGreenTimes = signalAnalyzer.getTotalSignalGreenTime(); // group 1 should have more total green time than group 2
		Map<Id<SignalGroup>, Double> avgSignalGreenTimePerCycle = signalAnalyzer.calculateAvgSignalGreenTimePerFlexibleCycle();
		Map<Id<SignalSystem>, Double> avgCycleTimePerSystem = signalAnalyzer.calculateAvgFlexibleCycleTimePerSignalSystem();


		log.info("total signal green times: " + totalSignalGreenTimes.get(SIGNALGROUPID1) + ", " + totalSignalGreenTimes.get(SIGNALGROUPID2));
		log.info("avg signal green times per cycle: " + avgSignalGreenTimePerCycle.get(SIGNALGROUPID1) + ", " + avgSignalGreenTimePerCycle.get(SIGNALGROUPID2));
		log.info("avg cycle time per system: " + avgCycleTimePerSystem.get(SIGNALSYSTEMID));
		
		Assert.assertEquals("total signal time of group 1 is wrong",2051.,totalSignalGreenTimes.get(SIGNALGROUPID1),MatsimTestUtils.EPSILON);
		Assert.assertEquals("total signal time of group 1 is wrong",1655.,totalSignalGreenTimes.get(SIGNALGROUPID2),MatsimTestUtils.EPSILON);
		Assert.assertEquals("avg green time per cycle of signal group 1 is wrong", 4.954106280193237, avgSignalGreenTimePerCycle.get(SIGNALGROUPID1), MatsimTestUtils.EPSILON);
		Assert.assertEquals("avg green time per cycle of signal group 2 is wrong", 3.997584541062802, avgSignalGreenTimePerCycle.get(SIGNALGROUPID2), MatsimTestUtils.EPSILON);
		Assert.assertEquals("avg cycle time of the system is wrong", 9.91304347826087, avgCycleTimePerSystem.get(SIGNALSYSTEMID), MatsimTestUtils.EPSILON);
	}

	@Ignore
	public void testScenario2DemandABCD() {
		double[] noPersons = { 3600, 3600, 3600, 3600 };
		TtSignalAnalysisTool signalAnalyzer = runScenario2(noPersons);

		// check signal results
		Map<Id<SignalGroup>, Double> totalSignalGreenTimes = signalAnalyzer.getTotalSignalGreenTime(); // should be more or less equal (OW direction is always favored as the first phase)
		Map<Id<SignalGroup>, Double> avgSignalGreenTimePerCycle = signalAnalyzer.calculateAvgSignalGreenTimePerFlexibleCycle(); // should be more or less equal and around 25
		Map<Id<SignalSystem>, Double> avgCycleTimePerSystem = signalAnalyzer.calculateAvgFlexibleCycleTimePerSignalSystem(); // should be 60

		

		log.info("total signal green times: " + totalSignalGreenTimes.get(SIGNALGROUPID1) + ", " + totalSignalGreenTimes.get(SIGNALGROUPID2));
		log.info("avg signal green times per cycle: " + avgSignalGreenTimePerCycle.get(SIGNALGROUPID1) + ", " + avgSignalGreenTimePerCycle.get(SIGNALGROUPID2));
		log.info("avg cycle time per system: " + avgCycleTimePerSystem.get(SIGNALSYSTEMID));
		
		
		
//		Assert.assertEquals("total signal green times of both groups are not similiar enough", 0.0, totalSignalGreenTimes.get(SIGNALGROUPID1) - totalSignalGreenTimes.get(SIGNALGROUPID2), 3.);
//		Assert.assertEquals("avg green time per cycle of signal group 1 is wrong", 3.9870689655172415, avgSignalGreenTimePerCycle.get(SIGNALGROUPID1), MatsimTestUtils.EPSILON);
//		Assert.assertEquals("avg green time per cycle of signal group 2 is wrong", 3.9913793103448274, avgSignalGreenTimePerCycle.get(SIGNALGROUPID2), MatsimTestUtils.EPSILON);
//		Assert.assertEquals("avg cycle time of the system is wrong", 8.413793103448276, avgCycleTimePerSystem.get(SIGNALSYSTEMID), MatsimTestUtils.EPSILON);
	}
	
	
	
	
//------------------------------------------	
	private TtSignalAnalysisTool runScenario(double[] noPersons) {
		Config config = defineConfig();

		Scenario scenario = ScenarioUtils.loadScenario(config);
		// add missing scenario elements
		scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());

		createScenarioElements(scenario, noPersons);

		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new CombinedSignalsModule());

		// add signal analysis tool
		TtSignalAnalysisTool signalAnalyzer = new TtSignalAnalysisTool();
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.addEventHandlerBinding().toInstance(signalAnalyzer);
				this.addControlerListenerBinding().toInstance(signalAnalyzer);
			}
		});

		controler.run();

		return signalAnalyzer;
	}

	private void createScenarioElements(Scenario scenario, double[] noPersons) {
		createNetwork(scenario.getNetwork());
		createPopulationScenario1(scenario.getPopulation(), noPersons);
		createSignals(scenario);
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
	private static void createNetwork(Network net) {
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
			link.setCapacity(7200);
			link.setLength(1000);
			link.setFreespeed(10);
			net.addLink(link);
		}
	}

	private static void createPopulationScenario1(Population population, double[] noPersons) {
		String[] odRelations = { "1_2-4_5", "6_7-8_9", "4_5-1_2", "8_9-6_7"};
		int odIndex = 0;

		for (String od : odRelations) {
			String fromLinkId = od.split("-")[0];
			String toLinkId = od.split("-")[1];
			
			double demand = noPersons[odIndex];
			
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
		}
	}

	private void createSignals(Scenario scenario) {
		SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		SignalSystemsData signalSystems = signalsData.getSignalSystemsData();
		SignalSystemsDataFactory sysFac = signalSystems.getFactory();
		SignalGroupsData signalGroups = signalsData.getSignalGroupsData();
		SignalControlData signalControl = signalsData.getSignalControlData();
		SignalControlDataFactory conFac = signalControl.getFactory();

		// create signal system
		//Id<SignalSystem> signalSystemId = Id.create("SignalSystem1", SignalSystem.class);
		SignalSystemData signalSystem = sysFac.createSignalSystemData(SIGNALSYSTEMID);
		signalSystems.addSignalSystemData(signalSystem);

		// create a signal for every inLink
		for (Id<Link> inLinkId : scenario.getNetwork().getNodes().get(Id.createNodeId(3)).getInLinks().keySet()) {
			SignalData signal = sysFac.createSignalData(Id.create("Signal" + inLinkId, Signal.class));
			signalSystem.addSignalData(signal);
			signal.setLinkId(inLinkId);
		}

		// group signals with non conflicting streams
		//Id<SignalGroup> signalGroupId1 = Id.create("SignalGroup1", SignalGroup.class);
		SignalGroupData signalGroup1 = signalGroups.getFactory().createSignalGroupData(SIGNALSYSTEMID, SIGNALGROUPID1);
		signalGroup1.addSignalId(Id.create("Signal2_3", Signal.class));
		signalGroup1.addSignalId(Id.create("Signal4_3", Signal.class));
		signalGroups.addSignalGroupData(signalGroup1);

		//Id<SignalGroup> signalGroupId2 = Id.create("SignalGroup2", SignalGroup.class);
		SignalGroupData signalGroup2 = signalGroups.getFactory().createSignalGroupData(SIGNALSYSTEMID, SIGNALGROUPID2);
		signalGroup2.addSignalId(Id.create("Signal7_3", Signal.class));
		signalGroup2.addSignalId(Id.create("Signal8_3", Signal.class));
		signalGroups.addSignalGroupData(signalGroup2);

		// create the signal control
		SignalSystemControllerData signalSystemControl = conFac.createSignalSystemControllerData(SIGNALSYSTEMID);
		signalSystemControl.setControllerIdentifier(DgRoederGershensonSignalController.IDENTIFIER);
		signalControl.addSignalSystemControllerData(signalSystemControl);

		
		// no plan is needed for GershensonController
//		// create a plan for the signal system (with defined cycle time and offset 0)
//		SignalPlanData signalPlan = SignalUtils.createSignalPlan(conFac, 60, 0, Id.create("SignalPlan1", SignalPlan.class));
//		signalSystemControl.addSignalPlanData(signalPlan);
//
//		// specify signal group settings for both signal groups
//		signalPlan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(conFac, signalGroupId1, 0, 5));
//		signalPlan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(conFac, signalGroupId2, 10, 55));
//		signalPlan.setOffset(0);
	}

	private Config defineConfig() {
		Config config = ConfigUtils.createConfig();
		config.controler().setOutputDirectory(testUtils.getOutputDirectory());

		// set number of iterations
		config.controler().setLastIteration(0);

		// able or enable signals and lanes
		SignalSystemsConfigGroup signalConfigGroup = ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
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
	private TtSignalAnalysisTool runScenario2(double[] noPersons) {
		Config config = defineConfig();

		Scenario scenario = ScenarioUtils.loadScenario(config);
		// add missing scenario elements
		scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());

		createScenarioElements2(scenario, noPersons);

		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new CombinedSignalsModule());

		// add signal analysis tool
		TtSignalAnalysisTool signalAnalyzer = new TtSignalAnalysisTool();
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.addEventHandlerBinding().toInstance(signalAnalyzer);
				this.addControlerListenerBinding().toInstance(signalAnalyzer);
			}
		});

		controler.run();

		return signalAnalyzer;
	}
	
	private void createScenarioElements2(Scenario scenario, double[] noPersons) {
		createNetworkScenario2(scenario.getNetwork());
		createPopulationScenario2(scenario.getPopulation(), noPersons);
		createSignalsScenario2(scenario);
	}	
	
	/**
	 * creates a network like this:
	 * 
	 * 					 15				   20
	 * 					 ^				   ^
	 * 					 |				   |
	 * 					 v				   v
	 * 					 16				   21
	 * 					 ^				   ^
	 * 					 |				   |
	 * 					 v				   v
	 * 1 <----> 2 <----> 3 <----> 4 <----> 5 <----> 6 <----> 7
	 * 					 ^				   ^
	 * 					 |				   |
	 * 					 v				   v
	 * 					 17				   22
	 * 					 ^				   ^
	 * 					 |				   |
	 * 					 v				   v
	 * 8 <----> 9 <----> 10<----> 11<----> 12<----> 13 <----> 14
	 * 					 ^ 				   ^
	 * 					 | 				   |
	 * 					 v 				   v
	 * 					 18 			   23
	 * 					 ^ 				   ^
	 * 					 | 				   |
	 * 					 v 				   v
	 * 					 19 			   24
	 * 
	 * 
	 * @param net
	 *            the object where the network should be stored
	 */
	private static void createNetworkScenario2(Network net) {
		NetworkFactory fac = net.getFactory();

		net.addNode(fac.createNode(Id.createNodeId(1), new Coord(-2000, 0)));
		net.addNode(fac.createNode(Id.createNodeId(2), new Coord(-1000, 0)));
		net.addNode(fac.createNode(Id.createNodeId(3), new Coord(0, 0)));
		net.addNode(fac.createNode(Id.createNodeId(4), new Coord(1000, 0)));
		net.addNode(fac.createNode(Id.createNodeId(5), new Coord(2000, 0)));
		net.addNode(fac.createNode(Id.createNodeId(6), new Coord(3000, 0)));
		net.addNode(fac.createNode(Id.createNodeId(7), new Coord(4000, 0)));
		net.addNode(fac.createNode(Id.createNodeId(8), new Coord(-2000, -2000)));
		net.addNode(fac.createNode(Id.createNodeId(9), new Coord(-1000, -2000)));
		net.addNode(fac.createNode(Id.createNodeId(10), new Coord(0, -2000)));
		net.addNode(fac.createNode(Id.createNodeId(11), new Coord(1000, -2000)));
		net.addNode(fac.createNode(Id.createNodeId(12), new Coord(2000, -2000)));
		net.addNode(fac.createNode(Id.createNodeId(13), new Coord(3000, -2000)));
		net.addNode(fac.createNode(Id.createNodeId(14), new Coord(4000, -2000)));
				
		net.addNode(fac.createNode(Id.createNodeId(15), new Coord(0, -2000)));
		net.addNode(fac.createNode(Id.createNodeId(16), new Coord(0, -1000)));
		net.addNode(fac.createNode(Id.createNodeId(17), new Coord(0, 1000)));
		net.addNode(fac.createNode(Id.createNodeId(18), new Coord(0, 3000)));
		net.addNode(fac.createNode(Id.createNodeId(19), new Coord(0, 4000)));
		net.addNode(fac.createNode(Id.createNodeId(20), new Coord(2000, -2000)));
		net.addNode(fac.createNode(Id.createNodeId(21), new Coord(2000, -1000)));
		net.addNode(fac.createNode(Id.createNodeId(22), new Coord(2000, 1000)));
		net.addNode(fac.createNode(Id.createNodeId(23), new Coord(2000, 3000)));
		net.addNode(fac.createNode(Id.createNodeId(24), new Coord(2000, 4000)));
		
		
		String[] links = { "1_2", "2_1", "2_3", "3_2", "3_4", "4_3", "4_5", "5_4", "5_6", "6_5", "6_7", "7_6",
				"8_9","9_8","9_10","10_9","10_11","11_10","11_12","12_11","12_13","13_14","14_13",
				"15_16","16_15","16_3","3_16","3_17","17_3","17_10","10_17","10_18","18_10","18_19","19_18",
				"20_21","21_20","21_5","5_21","5_22","22_5","22_12","12_22","12_23","23_12","23_24","24_12"};

		for (String linkId : links) {
			String fromNodeId = linkId.split("_")[0];
			String toNodeId = linkId.split("_")[1];
			Link link = fac.createLink(Id.createLinkId(linkId), net.getNodes().get(Id.createNodeId(fromNodeId)), net.getNodes().get(Id.createNodeId(toNodeId)));
			link.setCapacity(7200);
			link.setLength(1000);
			link.setFreespeed(10);
			net.addLink(link);
		}
	}
	
	private static void createPopulationScenario2(Population population, double[] noPersons) {
		String[] odRelations = { "1_2-6_7", "8_9-13_14", "15_16-18_19", "20_21-23_24"};
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
	
	private void createSignalsScenario2(Scenario scenario) {
		SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		SignalSystemsData signalSystems = signalsData.getSignalSystemsData();
		SignalSystemsDataFactory sysFac = signalSystems.getFactory();
		SignalGroupsData signalGroups = signalsData.getSignalGroupsData();
		SignalControlData signalControl = signalsData.getSignalControlData();
		SignalControlDataFactory conFac = signalControl.getFactory();

		// create signal system
		Id<SignalSystem> signalSystemId1 = Id.create("SignalSystem1", SignalSystem.class);
		Id<SignalSystem> signalSystemId2 = Id.create("SignalSystem2", SignalSystem.class);
		Id<SignalSystem> signalSystemId3 = Id.create("SignalSystem3", SignalSystem.class);
		Id<SignalSystem> signalSystemId4 = Id.create("SignalSystem4", SignalSystem.class);
		SignalSystemData signalSystem1 = sysFac.createSignalSystemData(signalSystemId1);
		SignalSystemData signalSystem2 = sysFac.createSignalSystemData(signalSystemId2);
		SignalSystemData signalSystem3 = sysFac.createSignalSystemData(signalSystemId3);
		SignalSystemData signalSystem4 = sysFac.createSignalSystemData(signalSystemId4);
		
		signalSystems.addSignalSystemData(signalSystem1);
		signalSystems.addSignalSystemData(signalSystem2);
		signalSystems.addSignalSystemData(signalSystem3);
		signalSystems.addSignalSystemData(signalSystem4);
		

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
		for (Id<Link> inLinkId : scenario.getNetwork().getNodes().get(Id.createNodeId(10)).getInLinks().keySet()) {
			SignalData signal = sysFac.createSignalData(Id.create("Signal" + inLinkId, Signal.class));
			signalSystem3.addSignalData(signal);
			signal.setLinkId(inLinkId);
		}
		for (Id<Link> inLinkId : scenario.getNetwork().getNodes().get(Id.createNodeId(12)).getInLinks().keySet()) {
			SignalData signal = sysFac.createSignalData(Id.create("Signal" + inLinkId, Signal.class));
			signalSystem4.addSignalData(signal);
			signal.setLinkId(inLinkId);
		}
		
		//SignalSystem1
		// group signals with non conflicting streams
		Id<SignalGroup> signalGroupId1Sys1 = Id.create("SignalGroup1", SignalGroup.class);
		SignalGroupData signalGroup1Sys1 = signalGroups.getFactory().createSignalGroupData(signalSystemId1, signalGroupId1Sys1);
		signalGroup1Sys1.addSignalId(Id.create("Signal2_3", Signal.class));
		signalGroup1Sys1.addSignalId(Id.create("Signal4_3", Signal.class));
		signalGroups.addSignalGroupData(signalGroup1Sys1);

		Id<SignalGroup> signalGroupId2Sys1 = Id.create("SignalGroup2", SignalGroup.class);
		SignalGroupData signalGroup2Sys1 = signalGroups.getFactory().createSignalGroupData(signalSystemId1, signalGroupId2Sys1);
		signalGroup2Sys1.addSignalId(Id.create("Signal16_3", Signal.class));
		signalGroup2Sys1.addSignalId(Id.create("Signal17_3", Signal.class));
		signalGroups.addSignalGroupData(signalGroup2Sys1);

		//SignalSystem2
		// group signals with non conflicting streams
		Id<SignalGroup> signalGroupId1Sys2 = Id.create("SignalGroup1", SignalGroup.class);
		SignalGroupData signalGroup1Sys2 = signalGroups.getFactory().createSignalGroupData(signalSystemId2, signalGroupId1Sys2);
		signalGroup1Sys2.addSignalId(Id.create("Signal4_5", Signal.class));
		signalGroup1Sys2.addSignalId(Id.create("Signal8_5", Signal.class));
		signalGroups.addSignalGroupData(signalGroup1Sys2);

		Id<SignalGroup> signalGroupId2Sys2 = Id.create("SignalGroup2", SignalGroup.class);
		SignalGroupData signalGroup2Sys2 = signalGroups.getFactory().createSignalGroupData(signalSystemId2, signalGroupId2Sys2);
		signalGroup2Sys2.addSignalId(Id.create("Signal21_5", Signal.class));
		signalGroup2Sys2.addSignalId(Id.create("Signal22_5", Signal.class));
		signalGroups.addSignalGroupData(signalGroup2Sys2);
	
		//SignalSystem3
		// group signals with non conflicting streams
		Id<SignalGroup> signalGroupId1Sys3 = Id.create("SignalGroup1", SignalGroup.class);
		SignalGroupData signalGroup1Sys3 = signalGroups.getFactory().createSignalGroupData(signalSystemId3, signalGroupId1Sys3);
		signalGroup1Sys3.addSignalId(Id.create("Signal9_10", Signal.class));
		signalGroup1Sys3.addSignalId(Id.create("Signal11_10", Signal.class));
		signalGroups.addSignalGroupData(signalGroup1Sys3);

		Id<SignalGroup> signalGroupId2Sys3 = Id.create("SignalGroup2", SignalGroup.class);
		SignalGroupData signalGroup2Sys3 = signalGroups.getFactory().createSignalGroupData(signalSystemId3, signalGroupId2Sys3);
		signalGroup2Sys3.addSignalId(Id.create("Signal17_10", Signal.class));
		signalGroup2Sys3.addSignalId(Id.create("Signal18_10", Signal.class));
		signalGroups.addSignalGroupData(signalGroup2Sys3);
		
		//SignalSystem4
		// group signals with non conflicting streams
		Id<SignalGroup> signalGroupId1Sys4 = Id.create("SignalGroup1", SignalGroup.class);
		SignalGroupData signalGroup1Sys4 = signalGroups.getFactory().createSignalGroupData(signalSystemId4, signalGroupId1Sys4);
		signalGroup1Sys4.addSignalId(Id.create("Signal11_12", Signal.class));
		signalGroup1Sys4.addSignalId(Id.create("Signal13_12", Signal.class));
		signalGroups.addSignalGroupData(signalGroup1Sys4);

		Id<SignalGroup> signalGroupId2Sys4 = Id.create("SignalGroup2", SignalGroup.class);
		SignalGroupData signalGroup2Sys4 = signalGroups.getFactory().createSignalGroupData(signalSystemId4, signalGroupId2Sys4);
		signalGroup2Sys4.addSignalId(Id.create("Signal22_12", Signal.class));
		signalGroup2Sys4.addSignalId(Id.create("Signal23_12", Signal.class));
		signalGroups.addSignalGroupData(signalGroup2Sys4);
		
		
		
		// create the signal control System1
		SignalSystemControllerData signalSystemControl1 = conFac.createSignalSystemControllerData(signalSystemId1);
		signalSystemControl1.setControllerIdentifier(DgRoederGershensonSignalController.IDENTIFIER);
		signalControl.addSignalSystemControllerData(signalSystemControl1);
		// create the signal control System2
		SignalSystemControllerData signalSystemControl2 = conFac.createSignalSystemControllerData(signalSystemId2);
		signalSystemControl2.setControllerIdentifier(DgRoederGershensonSignalController.IDENTIFIER);
		signalControl.addSignalSystemControllerData(signalSystemControl2);
		// create the signal control System3
		SignalSystemControllerData signalSystemControl3 = conFac.createSignalSystemControllerData(signalSystemId3);
		signalSystemControl3.setControllerIdentifier(DgRoederGershensonSignalController.IDENTIFIER);
		signalControl.addSignalSystemControllerData(signalSystemControl3);
		// create the signal control System1
		SignalSystemControllerData signalSystemControl4 = conFac.createSignalSystemControllerData(signalSystemId4);
		signalSystemControl4.setControllerIdentifier(DgRoederGershensonSignalController.IDENTIFIER);
		signalControl.addSignalSystemControllerData(signalSystemControl4);
	}
}
