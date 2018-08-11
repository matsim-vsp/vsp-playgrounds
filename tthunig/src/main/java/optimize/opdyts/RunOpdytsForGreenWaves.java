/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
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
package optimize.opdyts;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.opdyts.MATSimSimulator2;
import org.matsim.contrib.opdyts.MATSimStateFactoryImpl;
import org.matsim.contrib.opdyts.utils.MATSimOpdytsControler;
import org.matsim.contrib.opdyts.utils.OpdytsConfigGroup;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.analysis.SignalAnalysisTool;
import org.matsim.contrib.signals.binder.SignalsModule;
import org.matsim.contrib.signals.controller.fixedTime.DefaultPlanbasedSignalSystemController;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalControlDataFactory;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalSystemControllerData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsDataFactory;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalPlan;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.contrib.signals.utils.SignalUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.scenario.ScenarioUtils;

import analysis.TtAnalyzedGeneralResultsWriter;
import analysis.TtGeneralAnalysis;
import analysis.TtListenerToBindGeneralAnalysis;
import analysis.TtTotalTravelTime;
import analysis.signals.SignalAnalysisListener;
import analysis.signals.SignalAnalysisWriter;
import playground.vsp.analysis.modules.modalAnalyses.modalTripTime.ModalTravelTimeControlerListener;
import playground.vsp.analysis.modules.modalAnalyses.modalTripTime.ModalTripTravelTimeHandler;
import playground.agarwalamit.opdyts.plots.OpdytsConvergenceChart;
import utils.OutputUtils;

/**
 * @author tthunig
 */
public class RunOpdytsForGreenWaves {

	private static final Logger LOG = Logger.getLogger(RunOpdytsForGreenWaves.class);
	
	private static final String OUTPUT_BASE_DIR = "../../runs-svn/opdytsForSignals/";
	private static String outputDir;

	private static final boolean USE_OPDYTS = true;
	
	private static final InitialOffsets INITIAL_OFFSETS = InitialOffsets.FIRST_OPT_REST_WORST;
	private enum InitialOffsets {
		OPTIMAL, WORST, FIRST_OPT_REST_WORST, ALL_ZERO
	}

	public static void main(String[] args) {

		Config config = createConfig();
		Scenario scenario = ScenarioUtils.loadScenario(config);
		createNetwork(scenario);
		createSignals(scenario);
		createDemand(scenario);

		if (USE_OPDYTS) {
			OpdytsConfigGroup opdytsConfigGroup = ConfigUtils.addOrGetModule(scenario.getConfig(), OpdytsConfigGroup.class);
			opdytsConfigGroup.setNumberOfIterationsForAveraging(2); // 2
			opdytsConfigGroup.setNumberOfIterationsForConvergence(2); // this system has no stochasticity (agents do not change their plan)
			
			// Gunnar: "true" now also works, meaning that opdyts identifies on its own the absence of noise.
			opdytsConfigGroup.setNoisySystem(true); 

			
			opdytsConfigGroup.setMaxIteration(30);
			opdytsConfigGroup.setOutputDirectory(scenario.getConfig().controler().getOutputDirectory());
			opdytsConfigGroup.setDecisionVariableStepSize(10);
			
			// Gunnar: Have a single warm-up iteration (and also use that one) is recommended default.
			opdytsConfigGroup.setUseAllWarmUpIterations(true);
			opdytsConfigGroup.setWarmUpIterations(1);
			
			// Gunnar: Two (+/-) variations for each of the tree intersections.
			// Makes sense together with (complete) axial variations in OffsetRandomizer.
			opdytsConfigGroup.setPopulationSize(3 * 2);

			opdytsConfigGroup.setSelfTuningWeight(1); //1, 4
			opdytsConfigGroup.setBinSize(10);

			// Gunnar: binCount times binSize must be simulation duration!
			opdytsConfigGroup.setBinCount((4 * 3600) / opdytsConfigGroup.getBinSize());
			
			
			MATSimOpdytsControler<OffsetDecisionVariable> runner = new MATSimOpdytsControler<>(scenario);

			MATSimSimulator2<OffsetDecisionVariable> simulator = new MATSimSimulator2<>(new MATSimStateFactoryImpl<>(), scenario);
			simulator.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					// this can later be accessed by TravelTimeObjectiveFunction, because it is bind inside MATSimSimulator2
					bind(TtTotalTravelTime.class).asEagerSingleton();
					addEventHandlerBinding().to(TtTotalTravelTime.class);

					// bind amits analysis
					bind(ModalTripTravelTimeHandler.class);
					addControlerListenerBinding().to(ModalTravelTimeControlerListener.class);

					// bind general analysis
					this.bind(TtGeneralAnalysis.class);
					this.bind(TtAnalyzedGeneralResultsWriter.class);
					this.addControlerListenerBinding().to(TtListenerToBindGeneralAnalysis.class);

					// bind tool to analyze signals
					this.bind(SignalAnalysisTool.class);
					this.bind(SignalAnalysisWriter.class);
					this.addControlerListenerBinding().to(SignalAnalysisListener.class);
					
					this.addControlerListenerBinding().to(OpdytsOffsetStatsControlerListener.class);

					// plot only after one opdyts transition:
					addControlerListenerBinding().toInstance(new ShutdownListener() { 
						@Override
						public void notifyShutdown(ShutdownEvent event) {
							// post-process analysis
							String opdytsConvergenceFile = outputDir + "/opdyts.con";
							if (new File(opdytsConvergenceFile).exists()) {
								OpdytsConvergenceChart opdytsConvergencePlotter = new OpdytsConvergenceChart();
								opdytsConvergencePlotter.readFile(outputDir + "/opdyts.con");
								opdytsConvergencePlotter.plotData(outputDir + "/convergence.png");
							}
						}
					});
				}
			});
			simulator.addOverridingModule(new SignalsModule());
			runner.addNetworkModeOccupancyAnalyzr(simulator);

			runner.run(simulator, new OffsetRandomizer(scenario), new OffsetDecisionVariable(
					((SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME)).getSignalControlData(),
					scenario), new TravelTimeObjectiveFunction());
		} else {
			// simply start a matsim simulation
			Controler controler = new Controler(scenario);
			controler.addOverridingModule(new SignalsModule());
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					// bind amits analysis
					bind(ModalTripTravelTimeHandler.class);
					addControlerListenerBinding().to(ModalTravelTimeControlerListener.class);

					// bind general analysis
					this.bind(TtGeneralAnalysis.class);
					this.bind(TtAnalyzedGeneralResultsWriter.class);
					this.addControlerListenerBinding().to(TtListenerToBindGeneralAnalysis.class);

					// bind tool to analyze signals
					this.bind(SignalAnalysisTool.class);
					this.bind(SignalAnalysisWriter.class);
					this.addControlerListenerBinding().to(SignalAnalysisListener.class);
				}
			});
			controler.run();
		}
	}

	private static void createSignals(Scenario scenario) {
		// add missing scenario elements
		scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(scenario.getConfig()).loadSignalsData());

		SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		SignalSystemsData signalSystems = signalsData.getSignalSystemsData();
		SignalSystemsDataFactory sysFac = signalSystems.getFactory();
		SignalGroupsData signalGroups = signalsData.getSignalGroupsData();
		SignalControlData signalControl = signalsData.getSignalControlData();
		SignalControlDataFactory conFac = signalControl.getFactory();

		String[] signalizedIntersections = { "2", "3", "4" };

		for (int i = 0; i < signalizedIntersections.length; i++) {
			// create the signal system for the intersection
			Id<SignalSystem> signalSystemId = Id.create("SignalSystem" + signalizedIntersections[i],
					SignalSystem.class);
			SignalSystemData signalSystem = sysFac.createSignalSystemData(signalSystemId);
			signalSystems.addSignalSystemData(signalSystem);

			// add the signal (there is only one inLink)
			for (Link inLink : scenario.getNetwork().getNodes().get(Id.createNodeId(signalizedIntersections[i]))
					.getInLinks().values()) {
				SignalData signal = sysFac.createSignalData(Id.create("Signal" + inLink.getId(), Signal.class));
				signal.setLinkId(inLink.getId());
				signalSystem.addSignalData(signal);

				// create a group for this single signal
				Id<SignalGroup> signalGroupId = Id.create("SignalGroup" + signal.getId(), SignalGroup.class);
				SignalGroupData signalGroup = signalGroups.getFactory().createSignalGroupData(signalSystemId,
						signalGroupId);
				signalGroup.addSignalId(signal.getId());
				signalGroups.addSignalGroupData(signalGroup);
			}

			// create signal control
			SignalSystemControllerData signalSystemControl = conFac.createSignalSystemControllerData(signalSystemId);
			signalSystemControl.setControllerIdentifier(DefaultPlanbasedSignalSystemController.IDENTIFIER);
			signalControl.addSignalSystemControllerData(signalSystemControl);

			// create a plan for the signal system (with cycle time 300, 100 seconds green time and defined offset)
			int offset = 0;
			switch (INITIAL_OFFSETS) {
			case OPTIMAL:
				offset = 11;
				if (i == 1) offset = 21;
				else if (i == 2) offset = 31;
				break;
			case WORST:
				offset = 211;
				if (i==1) offset = 121;
				else if (i==2) offset = 32;
				break;
			case FIRST_OPT_REST_WORST:
				offset = 11;
				if (i==1) offset = 221;
				else if (i==2) offset = 131;
				break;
			default:
				// offsets are all zero
				break;
			}
			SignalPlanData signalPlan = SignalUtils.createSignalPlan(conFac, 300, offset, Id.create("SignalPlan", SignalPlan.class));
			signalSystemControl.addSignalPlanData(signalPlan);
			for (Id<SignalGroup> signalGroupId : signalGroups.getSignalGroupDataBySystemId(signalSystemId).keySet()) {
				// there is only one element in this set
				signalPlan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(conFac, signalGroupId, 0, 100));
			}
		}

	}

	private static void createDemand(Scenario scenario) {
		Population pop = scenario.getPopulation();
		PopulationFactory popFac = pop.getFactory();

		int startTime = 0;
		int pulkInterval = 100;
		int cycleTime = 300;
		int endTime = 3600;
		int secondsPerPerson = 1;

		Id<Link> fromLinkId = Id.createLinkId("0_1");
		Id<Link> toLinkId = Id.createLinkId("4_5");

		int departure = startTime;
		while (departure < endTime) {
			for (int i = 0; i < pulkInterval; i += secondsPerPerson) {
				// create a person (the i-th person)
				Person person = popFac.createPerson(Id.createPersonId(fromLinkId + "-" + toLinkId + "-" + departure));
				pop.addPerson(person);

				// create a plan for the person that contains all this information
				Plan plan = popFac.createPlan();
				person.addPlan(plan);

				// create a start activity at the from link
				Activity startAct = popFac.createActivityFromLinkId("dummy", fromLinkId);
				startAct.setEndTime(departure);
				plan.addActivity(startAct);
				// create a dummy leg
				Leg leg = popFac.createLeg(TransportMode.car);

				// create routes for the agents
				List<Id<Link>> path = new ArrayList<>();
				path.add(Id.createLinkId("1_2"));
				path.add(Id.createLinkId("2_3"));
				path.add(Id.createLinkId("3_4"));
				leg.setRoute(RouteUtils.createLinkNetworkRouteImpl(fromLinkId, path, toLinkId));
				plan.addLeg(leg);

				// create a drain activity at the to link
				Activity drainAct = popFac.createActivityFromLinkId("dummy", toLinkId);
				plan.addActivity(drainAct);

				departure += secondsPerPerson;
			}
			departure += cycleTime - pulkInterval;
		}
	}

	private static void createNetwork(Scenario scenario) {
		Network net = scenario.getNetwork();
		NetworkFactory netFac = net.getFactory();

		net.addNode(netFac.createNode(Id.createNodeId(0), new Coord(0, 0)));
		net.addNode(netFac.createNode(Id.createNodeId(1), new Coord(1000, 0)));
		net.addNode(netFac.createNode(Id.createNodeId(2), new Coord(2000, 0)));
		net.addNode(netFac.createNode(Id.createNodeId(3), new Coord(3000, 0)));
		net.addNode(netFac.createNode(Id.createNodeId(4), new Coord(4000, 0)));
		net.addNode(netFac.createNode(Id.createNodeId(5), new Coord(5000, 0)));

		String[] links = { "0_1", "1_2", "2_3", "3_4", "4_5" };

		for (String linkId : links) {
			String fromNodeId = linkId.split("_")[0];
			String toNodeId = linkId.split("_")[1];
			Link link = netFac.createLink(Id.createLinkId(linkId), net.getNodes().get(Id.createNodeId(fromNodeId)),
					net.getNodes().get(Id.createNodeId(toNodeId)));
			link.setCapacity(3600);
			link.setLength(99);
			link.setFreespeed(10);
			net.addLink(link);
		}
	}

	private static Config createConfig() {
		Config config = ConfigUtils.createConfig();
		
		outputDir = OUTPUT_BASE_DIR + OutputUtils.getCurrentDateIncludingTime() + "/"; 
		// create directory
		new File(outputDir).mkdirs();
		config.controler().setOutputDirectory(outputDir);
		LOG.info("The output will be written to " + outputDir);
		
		int randomSeed = (new Random()).nextInt(9999);
		config.global().setRandomSeed(randomSeed);
		
		config.controler().setLastIteration(1);

		SignalSystemsConfigGroup signalConfigGroup = ConfigUtils.addOrGetModule(config,
				SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
		signalConfigGroup.setUseSignalSystems(true);

		// define strategies (there is only one route, so ReRoute always returns the same route)
		{
			StrategySettings strat = new StrategySettings();
			strat.setStrategyName(DefaultStrategy.ReRoute.toString());
			strat.setWeight(1);
			strat.setDisableAfter(config.controler().getLastIteration());
			config.strategy().addStrategySettings(strat);
		}

		config.travelTimeCalculator().setTraveltimeBinSize(10);

		config.qsim().setStuckTime(3600 * 4);
		config.qsim().setRemoveStuckVehicles(false);

		config.qsim().setUsingFastCapacityUpdate(false);

		config.qsim().setStartTime(0);
		config.qsim().setEndTime(4 * 3600);

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		config.vspExperimental().setWritingOutputEvents(true);
		config.planCalcScore().setWriteExperiencedPlans(true);
		config.controler().setCreateGraphs(true);

		config.controler().setWriteEventsInterval(1);
		config.controler().setWritePlansInterval(config.controler().getLastIteration());

		// define activity types
		{
			ActivityParams dummyAct = new ActivityParams("dummy");
			dummyAct.setTypicalDuration(12 * 3600);
			config.planCalcScore().addActivityParams(dummyAct);
			dummyAct.setScoringThisActivityAtAll(false);
		}

		return config;
	}

}
