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
package scenarios.illustrative.singleCrossing;

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
import org.matsim.contrib.signals.SignalSystemsConfigGroup.IntersectionLogic;
import org.matsim.contrib.signals.builder.Signals;
import org.matsim.contrib.signals.controller.fixedTime.DefaultPlanbasedSignalSystemController;
import org.matsim.contrib.signals.controller.laemmerFix.LaemmerConfigGroup;
import org.matsim.contrib.signals.controller.laemmerFix.LaemmerConfigGroup.Regime;
import org.matsim.contrib.signals.controller.laemmerFix.LaemmerConfigGroup.StabilizationStrategy;
import org.matsim.contrib.signals.controller.laemmerFix.LaemmerSignalController;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.contrib.signals.data.conflicts.ConflictData;
import org.matsim.contrib.signals.data.conflicts.IntersectionDirections;
import org.matsim.contrib.signals.data.signalgroups.v20.*;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsDataFactory;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalPlan;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.contrib.signals.otfvis.OTFVisWithSignalsLiveModule;
import org.matsim.contrib.signals.utils.SignalUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.lanes.*;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import signals.downstreamSensor.DownstreamPlanbasedSignalController;
import signals.gershenson.GershensonConfig;
import signals.gershenson.GershensonSignalController;
import signals.laemmerFlex.FullyAdaptiveLaemmerSignalController;

import java.util.*;

/**
 * @author tthunig
 *
 */
public class SingleCrossingScenario {

	private String OUTPUT_BASE_DIR = "../../runs-svn/singleCrossingScenario/";
    private static final int LANE_CAPACITY = 1800;
	public static final Id<SignalGroup> signalGroupId1 = Id.create("SignalGroup1", SignalGroup.class);
	public static final Id<SignalGroup> signalGroupId1l = Id.create("SignalGroup1l", SignalGroup.class);
	public static final Id<SignalGroup> signalGroupId2 = Id.create("SignalGroup2", SignalGroup.class);
	public static final Id<SignalGroup> signalGroupId3 = Id.create("SignalGroup3", SignalGroup.class);
	public static final Id<SignalGroup> signalGroupId4 = Id.create("SignalGroup4", SignalGroup.class);
	public static final Id<SignalSystem> signalSystemId = Id.create("SignalSystem1", SignalSystem.class);

	private final double flowNS;
	private final double flowWE;
	public enum SignalControl {NONE, FIXED, FIXED_PROTECTED_LEFT_TURN, 
		LAEMMER_NICO, LAEMMER_FULLY_ADAPTIVE, GERSHENSON};
	SignalControl signalControl = SignalControl.LAEMMER_NICO;
	private Regime laemmerRegime = Regime.COMBINED;
	private LaemmerConfigGroup laemmerConfig;
	private GershensonConfig gershensonConfig;
	private boolean vis = false;
	private boolean stochasticDemand = false;
	private boolean useLanes = true;
	private boolean liveArrivalRates = true;
	private boolean groupedSignals = true;
	private double minG = 0;
	private boolean temporalCrowd = false;
	private StabilizationStrategy stabilizationStrategy;
	private IntersectionLogic intersectionLogic = IntersectionLogic.NONE;
	private boolean createLeftTurnDemand = false;
	private double leftTurningFactorWE = 0.2;
	
	public void setCreateLeftTurnDemand(boolean createLeftTurnDemand) {
		this.createLeftTurnDemand = createLeftTurnDemand;
	}


	public void setSignalType(Regime laemmerRegime) {
		this.laemmerRegime = laemmerRegime;
	}
	
	/**
	 * @param signalControl to use in illustrative single crossing scenario
	 */
	public void setSignalControl(SignalControl signalControl){
		this.signalControl = signalControl;
	}

	public void setVis(boolean vis) {
		this.vis = vis;
	}

	public void setStochastic(boolean stochastic) {
		this.stochasticDemand = stochastic;
	}

	public void setLanes(boolean lanes) {
		this.useLanes = lanes;
	}

	public void setLiveArrivalRates(boolean liveArrivalRates) {
		this.liveArrivalRates = liveArrivalRates;
	}

	public void setGrouped(boolean grouped) {
		this.groupedSignals = grouped;
	}

	public void setMinG(double minG) {
		this.minG = minG;
	}

	public void setTemporalCrowd(boolean temporalCrowd) {
		this.temporalCrowd = temporalCrowd;
	}
	
	public void setLaemmerConfig(LaemmerConfigGroup laemmerConfig) {
		this.laemmerConfig = laemmerConfig;
	}
	
	public void setGershensonConfig(GershensonConfig gershensonConfig) {
		this.gershensonConfig = gershensonConfig;
	}
	
	/**
	 * constructor useful for scenarios without laemmer signals
	 */
	public SingleCrossingScenario(double flowNS, double flowWE, SignalControl signalControl, boolean vis, boolean stochastic, boolean lanes, boolean grouped, boolean temporalCrowd) {
		this.flowNS = flowNS;
		this.flowWE = flowWE;
		this.signalControl = signalControl;
		this.vis = vis;
		this.stochasticDemand = stochastic;
		this.useLanes = lanes;
		this.groupedSignals = grouped;
		this.temporalCrowd = temporalCrowd;
	}
	
	public SingleCrossingScenario(double flowNS, double flowWE, SignalControl signalControl, Regime laemmerRegime, StabilizationStrategy stabilizationStrategy, boolean vis, boolean stochastic, boolean lanes,
			boolean liveArrivalRates, boolean grouped, double minG, boolean temporalCrowd) {
		this(flowNS, flowWE, signalControl, vis, stochastic, lanes, grouped, temporalCrowd);
		this.laemmerRegime = laemmerRegime;
		this.stabilizationStrategy = stabilizationStrategy;
		this.liveArrivalRates = liveArrivalRates;
		this.minG = minG;
	}
	
	public Controler defineControler() {
        if (gershensonConfig == null) {
        		useDefaultGershensonConfig();
        }

        final Scenario scenario = defineScenario();
        Controler controler = new Controler(scenario);

		Signals.Configurator signalsModule = Signals.configure( controler );;
		// the signals module works for planbased, sylvia and laemmer signal controller
		// by default and is pluggable for your own signal controller like this:
		signalsModule.addSignalControllerFactory(DownstreamPlanbasedSignalController.IDENTIFIER,
				DownstreamPlanbasedSignalController.DownstreamFactory.class);
		signalsModule.addSignalControllerFactory(FullyAdaptiveLaemmerSignalController.IDENTIFIER,
				FullyAdaptiveLaemmerSignalController.LaemmerFlexFactory.class);
		signalsModule.addSignalControllerFactory(GershensonSignalController.IDENTIFIER,
				GershensonSignalController.GershensonFactory.class);

		// bind gershenson config
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(GershensonConfig.class).toInstance(gershensonConfig);
			}
		});

        if (vis) {
            scenario.getConfig().qsim().setSnapshotStyle(QSimConfigGroup.SnapshotStyle.withHoles);
            scenario.getConfig().qsim().setNodeOffset(5.);
            OTFVisConfigGroup otfvisConfig = ConfigUtils.addOrGetModule(scenario.getConfig(), OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class);
            otfvisConfig.setScaleQuadTreeRect(true);
//            otfvisConfig.setColoringScheme(OTFVisConfigGroup.ColoringScheme.byId);
//            otfvisConfig.setAgentSize(240);
            controler.addOverridingModule(new OTFVisWithSignalsLiveModule());
        }
        return controler;
	}

	private void useDefaultGershensonConfig() {
		this.gershensonConfig = new GershensonConfig();
		// TODO modify this here, if you think for this scenario a specific setup (e.g. threshold) should be used
	}

	private Scenario defineScenario() {
        Scenario scenario = ScenarioUtils.loadScenario(defineConfig(createOutputPath()));
        // add missing scenario elements
		if (!signalControl.equals(SignalControl.NONE)) {
			scenario.addScenarioElement(SignalsData.ELEMENT_NAME,
					new SignalsDataLoader(scenario.getConfig()).loadSignalsData());
		}
        createNetwork(scenario.getNetwork());
        if (useLanes) {
        		SingleCrossingScenario.createLanes(scenario.getLanes());
        }
        createPopulation(scenario.getPopulation());
        if (!signalControl.equals(SignalControl.NONE)) {
        		createSignals(scenario);
        }
		if (intersectionLogic.toString().startsWith("CONFLICTING_DIRECTIONS")) {
			createConflictData(scenario);
		}
        return scenario;
    }
	
	private void createConflictData(Scenario scenario) {
		SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		ConflictData conflictData = signalsData.getConflictingDirectionsData();
		IntersectionDirections directionsForTheIntersection = conflictData.getFactory().
				createConflictingDirectionsContainerForIntersection(signalSystemId, Id.createNodeId(3));
		conflictData.addConflictingDirectionsForIntersection(signalSystemId, Id.createNodeId(3), directionsForTheIntersection);
		SignalUtils.fillIntersectionDirectionsForSingleCrossingScenario(directionsForTheIntersection, signalSystemId, conflictData);
	}
	
    private String createOutputPath() {
		String outputPath = OUTPUT_BASE_DIR;
		
		switch (signalControl) {
		case LAEMMER_NICO:
			outputPath += "laemmer" + laemmerRegime.name();
			break;
		case LAEMMER_FULLY_ADAPTIVE:
			outputPath += "laemmerFullyAdaptive" + laemmerRegime.name() + "-" + stabilizationStrategy.name();
			break;
		case FIXED:
			outputPath += "fixedTime";
			break;
		case FIXED_PROTECTED_LEFT_TURN:
			outputPath += "fixedTime_protectedLeftTurn_" + leftTurningFactorWE;
			break;
		case GERSHENSON:
			outputPath += "gershenson" + gershensonConfig.getThreshold();
			break;
		case NONE:
			outputPath += "noSignals";
			break;
		}
		
		if (stochasticDemand) {
			outputPath += "_stochastic";
		} else {
			outputPath += "_constant";
		}
		if (useLanes) {
			outputPath += "_lanes";
		} else {
			outputPath += "_noLanes";
		}
		if (liveArrivalRates) {
			outputPath += "_liveArrival";
		}
		
		outputPath += "/_ew" + flowWE + "_ns" + flowNS;
		
		return outputPath;
	}

	private Config defineConfig(String outputPath) {
        Config config = ConfigUtils.createConfig();
        config.controler().setOutputDirectory(outputPath);

        config.controler().setLastIteration(0);
        config.travelTimeCalculator().setMaxTime(60 * 120);
        config.qsim().setStartTime(0);
        config.qsim().setEndTime(60 * 120);
        config.qsim().setUsingFastCapacityUpdate(false);
        // set this very high, because stucked agents are not allowed together with link2link travel times
        config.qsim().setStuckTime(24*3600);

        if (useLanes) {
            config.qsim().setUseLanes(true);
            config.controler().setLinkToLinkRoutingEnabled(true);
            config.travelTimeCalculator().setCalculateLinkToLinkTravelTimes(true);
        }

        SignalSystemsConfigGroup signalConfigGroup = ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUP_NAME, SignalSystemsConfigGroup.class);
        if (signalControl.equals(SignalControl.NONE)) {
        		signalConfigGroup.setUseSignalSystems(false);
        } else {
        		signalConfigGroup.setUseSignalSystems(true);
        }
        signalConfigGroup.setIntersectionLogic(intersectionLogic);
        
		if (laemmerConfig == null) {
			// define specific default parameter for this scenario
			this.laemmerConfig = ConfigUtils.addOrGetModule(config, LaemmerConfigGroup.GROUP_NAME,
					LaemmerConfigGroup.class);
			laemmerConfig.setActiveRegime(laemmerRegime);
			if (groupedSignals) {
				laemmerConfig.setDesiredCycleTime(60);
				laemmerConfig.setMaxCycleTime(90);
			} else {
				laemmerConfig.setDesiredCycleTime(120);
				laemmerConfig.setMaxCycleTime(180);
			}
			laemmerConfig.setMinGreenTime(minG);
			laemmerConfig.setIntergreenTime(5);
			laemmerConfig.setActiveStabilizationStrategy(stabilizationStrategy);
		}

        PlanCalcScoreConfigGroup.ActivityParams dummyAct = new PlanCalcScoreConfigGroup.ActivityParams("dummy");
        dummyAct.setTypicalDuration(12 * 3600);
        config.planCalcScore().addActivityParams(dummyAct);

        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        config.controler().setWriteEventsInterval(config.controler().getLastIteration());
        config.controler().setWritePlansInterval(config.controler().getLastIteration());
        config.vspExperimental().setWritingOutputEvents(true);
        config.planCalcScore().setWriteExperiencedPlans(false);
        config.controler().setCreateGraphs(true);

        return config;
    }
    
    /**
     * creates a network like this:
     * 
     * 					6
     * 					^
     * 					|
     * 					v
     * 					7
     * 					^
     * 					|
     * 					v
     *  1 <===> 2 <===> 3 <===> 4 <===> 5
     * 					^
     * 					|
     * 					v
     * 					8
     * 					^
     * 					|
     * 					v
     * 					9
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

        String[] linksNorthSouth = {"6_7", "7_6", "7_3", "3_7", "3_8", "8_3", "8_9", "9_8"};
        String[] linksWestEast = {"1_2", "2_1", "2_3", "3_2", "3_4", "4_3", "4_5", "5_4"};

        for (String linkId : linksNorthSouth) {
            String fromNodeId = linkId.split("_")[0];
            String toNodeId = linkId.split("_")[1];
            Link link = fac.createLink(Id.createLinkId(linkId),
                    net.getNodes().get(Id.createNodeId(fromNodeId)),
                    net.getNodes().get(Id.createNodeId(toNodeId)));
            link.setCapacity(1800);
            link.setLength(1000);
            link.setFreespeed(13.889);
            Set<String> modes = new HashSet<>();
            modes.add("car");
            link.setAllowedModes(modes);
            net.addLink(link);
        }
        for (String linkId : linksWestEast) {
            String fromNodeId = linkId.split("_")[0];
            String toNodeId = linkId.split("_")[1];
            Link link = fac.createLink(Id.createLinkId(linkId),
                    net.getNodes().get(Id.createNodeId(fromNodeId)),
                    net.getNodes().get(Id.createNodeId(toNodeId)));
            link.setCapacity(3600);
            link.setNumberOfLanes(2);
            link.setLength(1000);
            link.setFreespeed(13.889);
            Set<String> modes = new HashSet<>();
            modes.add("car");
            link.setAllowedModes(modes);
            net.addLink(link);
        }     
    }
    
    private static void createLanes(Lanes lanes) {
        LanesFactory factory = lanes.getFactory();

        // create lanes for link 2_3
        LanesToLinkAssignment lanesForLink2_3 = factory
                .createLanesToLinkAssignment(Id.createLinkId("2_3"));
        lanes.addLanesToLinkAssignment(lanesForLink2_3);

        // original lane, i.e. lane that starts at the link from node and leads to all other lanes of the link
        LanesUtils.createAndAddLane(lanesForLink2_3, factory,
                Id.create("2_3.ol", Lane.class), 3600, 1000, 0, 2,
                null, Arrays.asList(Id.create("2_3.l", Lane.class), Id.create("2_3.r", Lane.class)));


        // left turning lane (alignment 1)
        LanesUtils.createAndAddLane(lanesForLink2_3, factory,
                Id.create("2_3.l", Lane.class), LANE_CAPACITY, 500, 1, 1,
                Arrays.asList(Id.create("3_7", Link.class)), null);

        // straight and right turning lane (alignment -1)
        LanesUtils.createAndAddLane(lanesForLink2_3, factory,
                Id.create("2_3.r", Lane.class), 2*LANE_CAPACITY, 500, -1, 1,
                Arrays.asList(Id.create("3_4", Link.class), Id.create("3_8", Link.class)), null);


        // create lanes for link 4_3
        LanesToLinkAssignment lanesForLink4_3 = factory
                .createLanesToLinkAssignment(Id.create("4_3", Link.class));
        lanes.addLanesToLinkAssignment(lanesForLink4_3);

        // original lane, i.e. lane that starts at the link from node and leads to all other lanes of the link
        LanesUtils.createAndAddLane(lanesForLink4_3, factory,
                Id.create("4_3.ol", Lane.class), 3600, 1000, 0, 2,
                null, Arrays.asList(Id.create("4_3.l", Lane.class), Id.create("4_3.r", Lane.class)));

        // left turning lane (alignment 1)
        LanesUtils.createAndAddLane(lanesForLink4_3, factory,
                Id.create("4_3.l", Lane.class), LANE_CAPACITY, 500, 1, 1,
                Arrays.asList(Id.create("3_8", Link.class)), null);

        // straight and right turning lane (alignment -1)
        LanesUtils.createAndAddLane(lanesForLink4_3, factory,
                Id.create("4_3.r", Lane.class), 2*LANE_CAPACITY, 500, -1, 1,
                Arrays.asList(Id.create("3_2", Link.class), Id.create("3_7", Link.class)), null);


        // create lanes for link 7_3
        LanesToLinkAssignment lanesForLink7_3 = factory
                .createLanesToLinkAssignment(Id.create("7_3", Link.class));
        lanes.addLanesToLinkAssignment(lanesForLink7_3);

        // original lane, i.e. lane that starts at the link from node. 
        // In this case it leads to all outgoing links of the link, i.e. the link only has one lane
        LanesUtils.createAndAddLane(lanesForLink7_3, factory,
                Id.create("7_3.ol", Lane.class), LANE_CAPACITY, 1000, 0, 1,
                Arrays.asList(Id.create("3_4", Link.class), Id.create("3_2", Link.class), Id.create("3_8", Link.class)), null);

        // create lanes for link 8_3
        LanesToLinkAssignment lanesForLink8_3 = factory
                .createLanesToLinkAssignment(Id.create("8_3", Link.class));
        lanes.addLanesToLinkAssignment(lanesForLink8_3);

        // original lane, i.e. lane that starts at the link from node. 
        // In this case it leads to all outgoing links of the link, i.e. the link only has one lane
        LanesUtils.createAndAddLane(lanesForLink8_3, factory,
                Id.create("8_3.ol", Lane.class), LANE_CAPACITY, 1000, 0, 1,
                Arrays.asList(Id.create("3_2", Link.class), Id.create("3_7", Link.class), Id.create("3_4", Link.class)), null);
    }
    
    private void createPopulation(Population pop) {
        String[] linksNS = {"6_7-8_9", "9_8-7_6"};
        String[] linksWE = {"5_4-2_1", "1_2-4_5"};
        String[] linksWEleftTurning = {"1_2-7_6", "5_4-8_9"};

        Random rnd = new Random(14);
        createPopulationForRelation(flowNS, pop, linksNS, rnd);
        createPopulationForRelation(flowWE, pop, linksWE, rnd);
		if (createLeftTurnDemand) {
			createPopulationForRelation(flowWE * leftTurningFactorWE, pop, linksWEleftTurning, rnd);
		}
    }
    
    private void createPopulationForRelation(double flow, Population population, String[] links, Random rnd) {

        double lambdaT = (flow / 3600 ) / 5;
        double lambdaN = 1./5.;

        if(flow == 0) {
            return;
        }


        for (String od : links) {
            String fromLinkId = od.split("-")[0];
            String toLinkId = od.split("-")[1];
            Map<Double, Integer> insertNAtSecond = new HashMap<>();
            if (stochasticDemand) {
                for (double i = 0; i < 5400; i++) {

                    double expT = 1 - Math.exp(-lambdaT);
                    double p1 = rnd.nextDouble();
                    if (p1 < expT) {
                        double p2 = rnd.nextDouble();
                        for(int n = 0; ; n++) {
                            double expN = Math.exp(-lambdaN * n);
                            if((p2 > expN)) {
                                insertNAtSecond.put(i, n);
                                break;
                            }
                        }
                    }
                }
            } else {
                double nthSecond = (3600 / flow);
                for (double i = 0; i < 5400; i += nthSecond) {
                    if(temporalCrowd && i>1800 && i<2400) {
                        insertNAtSecond.put(i, 2);
                    } else {
                        insertNAtSecond.put(i, 1);
                    }
                }
            }

            for (Map.Entry<Double, Integer> entry : insertNAtSecond.entrySet()) {
                for (int i = 0; i < entry.getValue(); i++) {
                    // create a person
                    Person person = population.getFactory().createPerson(Id.createPersonId(od + "-" + entry.getKey()+"("+i+")"));
                    population.addPerson(person);

                    // create a plan for the person that contains all this information
                    Plan plan = population.getFactory().createPlan();
                    person.addPlan(plan);

                    // create a start activity at the from link
                    Activity startAct = population.getFactory().createActivityFromLinkId("dummy", Id.createLinkId(fromLinkId));
                    // distribute agents uniformly during one hour.
                    startAct.setEndTime(entry.getKey());
                    plan.addActivity(startAct);

                    // create a dummy leg
                    plan.addLeg(population.getFactory().createLeg(TransportMode.car));

                    // create a drain activity at the to link
                    Activity drainAct = population.getFactory().createActivityFromLinkId("dummy", Id.createLinkId(toLinkId));
                    plan.addActivity(drainAct);
                }
            }
        }
    }
    
    private void createSignals(Scenario scenario) {
        SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
        SignalSystemsData signalSystems = signalsData.getSignalSystemsData();
        SignalSystemsDataFactory sysFac = signalSystems.getFactory();
        SignalGroupsData signalGroups = signalsData.getSignalGroupsData();
        SignalControlData signalControl = signalsData.getSignalControlData();
        
        // create the signal system for the single intersection
        SignalSystemData signalSystem1 = sysFac.createSignalSystemData(signalSystemId);
        signalSystems.addSignalSystemData(signalSystem1);

        // create a signal for every inLink/inLane
		for (Link inLink : scenario.getNetwork().getNodes().get(Id.createNodeId(3)).getInLinks().values()) {
			if (!useLanes) {
				SignalData signal = sysFac.createSignalData(Id.create("Signal" + inLink.getId(), Signal.class));
				signal.setLinkId(inLink.getId());
				signalSystem1.addSignalData(signal);
			} else {
				for (Lane lane : scenario.getLanes().getLanesToLinkAssignments().get(inLink.getId()).getLanes().values()) {
					if (lane.getToLinkIds() != null && !lane.getToLinkIds().isEmpty()) {
						SignalData signal = sysFac.createSignalData(Id.create("Signal" + lane.getId(), Signal.class));
						signal.addLaneId(lane.getId());
						signal.setLinkId(inLink.getId());
						signalSystem1.addSignalData(signal);
					}
				}
			}
		}
        
		// create groups
        SignalGroupData signalGroup1 = signalGroups.getFactory().createSignalGroupData(signalSystemId, signalGroupId1);
        SignalGroupData signalGroup1l = signalGroups.getFactory().createSignalGroupData(signalSystemId, signalGroupId1l);
        SignalGroupData signalGroup2 = signalGroups.getFactory().createSignalGroupData(signalSystemId, signalGroupId2);
        SignalGroupData signalGroup3 = signalGroups.getFactory().createSignalGroupData(signalSystemId, signalGroupId3);
        SignalGroupData signalGroup4 = signalGroups.getFactory().createSignalGroupData(signalSystemId, signalGroupId4);
		signalGroups.addSignalGroupData(signalGroup1);
		signalGroups.addSignalGroupData(signalGroup2);
		if (!groupedSignals) {
			signalGroups.addSignalGroupData(signalGroup3);
			signalGroups.addSignalGroupData(signalGroup4);
		} else if (useLanes) {
			// create separate group for left turns only if lanes are used and signals are grouped
			signalGroups.addSignalGroupData(signalGroup1l);
		}

		// fill groups with signals
        if (!useLanes) {
	        	signalGroup1.addSignalId(Id.create("Signal2_3", Signal.class));
	        	signalGroup2.addSignalId(Id.create("Signal7_3", Signal.class));
	        	if(groupedSignals) {
	        		signalGroup1.addSignalId(Id.create("Signal4_3", Signal.class));
	        		signalGroup2.addSignalId(Id.create("Signal8_3", Signal.class));
	        	} else{
	        		signalGroup3.addSignalId(Id.create("Signal4_3", Signal.class));
	        		signalGroup4.addSignalId(Id.create("Signal8_3", Signal.class));
	        	}
		} else { // lanes are used - we have a signal per lane! (2 lanes r+l for WE, 1 lane ol for NS)
			signalGroup1.addSignalId(Id.create("Signal2_3.r", Signal.class));
			signalGroup2.addSignalId(Id.create("Signal7_3.ol", Signal.class));
			if (groupedSignals) {
				signalGroup1.addSignalId(Id.create("Signal4_3.r", Signal.class));
				signalGroup2.addSignalId(Id.create("Signal8_3.ol", Signal.class));
				// separate group for left turns
				signalGroup1l.addSignalId(Id.create("Signal2_3.l", Signal.class));
				signalGroup1l.addSignalId(Id.create("Signal4_3.l", Signal.class));
			} else {
				signalGroup3.addSignalId(Id.create("Signal4_3.r", Signal.class));
				signalGroup4.addSignalId(Id.create("Signal8_3.ol", Signal.class));
				// add left turns to other groups according to their from link
				signalGroup1.addSignalId(Id.create("Signal2_3.l", Signal.class));
				signalGroup3.addSignalId(Id.create("Signal4_3.l", Signal.class));
			}
		}

        // create signal control
        switch (this.signalControl) {
		case LAEMMER_NICO:
			createLaemmerSignalControl(signalControl);
			break;
		case FIXED:
		case FIXED_PROTECTED_LEFT_TURN:
			createFixedTimeSignalControl(signalControl);
			break;
		case LAEMMER_FULLY_ADAPTIVE:
			createFullyAdaptiveLaemmerSignalControl(signalControl);
			break;
		case GERSHENSON:
			createGershensonSignalControl(signalControl);
			break;
		default:
			throw new RuntimeException("something went wrong with signal control " + this.signalControl);
		}
    }

	private void createFixedTimeSignalControl(SignalControlData signalControl) {
		SignalControlDataFactory conFac = signalControl.getFactory();

		SignalSystemControllerData signalSystemControl = conFac.createSignalSystemControllerData(signalSystemId);
		signalSystemControl.setControllerIdentifier(DefaultPlanbasedSignalSystemController.IDENTIFIER);
		signalControl.addSignalSystemControllerData(signalSystemControl);

		// create a plan for the signal system
		SignalPlanData signalPlan;

		double lambda1 = flowWE / (2 * LANE_CAPACITY);
		double lambda2 = flowNS / LANE_CAPACITY;
		double lambda1l = (flowWE * leftTurningFactorWE) / LANE_CAPACITY;

		if (groupedSignals) {
			int T_CYC = 60;
			signalPlan = SignalUtils.createSignalPlan(conFac, T_CYC, 0, Id.create("SignalPlan1", SignalPlan.class));
			int intergreen = 5;

			int TAU_SUM;
			if (this.signalControl.equals(SignalControl.FIXED_PROTECTED_LEFT_TURN)) {
				if (!this.useLanes) {
					throw new RuntimeException("Fixed-time signals with protected left turns and no lanes is not supported, "
							+ "as it does not make sense to have different signal settings but the same link queue.");
				}
				// three signal phases
				TAU_SUM = 3 * intergreen;

				double lambdaSum = lambda1 + lambda2 + lambda1l;
				int g1 = (int) Math.rint((lambda1 / lambdaSum) * ((T_CYC) - TAU_SUM));
				int g2 = (int) Math.rint((lambda2 / lambdaSum) * ((T_CYC) - TAU_SUM));
				int g1l = (int) Math.rint((lambda1l / lambdaSum) * ((T_CYC) - TAU_SUM));

				// specify signal group settings for all signal groups
				signalPlan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(conFac, signalGroupId2, 0, g2));
				signalPlan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(conFac, signalGroupId1,
						g2 + intergreen, g2 + intergreen + g1));
				signalPlan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(conFac, signalGroupId1l,
						g2 + g1 + 2 * intergreen, g2 + g1 + 2 * intergreen + g1l));

			} else {
				// two signal phases
				TAU_SUM = 2 * intergreen;

				double lambdaSum = Math.max(lambda1, lambda1l) + lambda2;
				int g1 = (int) Math.rint((Math.max(lambda1l, lambda1) / lambdaSum) * ((T_CYC) - TAU_SUM));
				int g2 = (int) Math.rint((lambda2 / lambdaSum) * ((T_CYC) - TAU_SUM));

				// specify signal group settings for all signal groups
				signalPlan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(conFac, signalGroupId2, 0, g2));
				signalPlan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(conFac, signalGroupId1,
						g2 + intergreen, g2 + intergreen + g1));
				signalPlan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(conFac, signalGroupId1l,
						g2 + intergreen, g2 + intergreen + g1));
			}
		} else { 
			if (this.signalControl.equals(SignalControl.FIXED_PROTECTED_LEFT_TURN)) {
				throw new RuntimeException("Fixed-time signals with protected left turns and no grouped signals is not "
						+ "supported in this scenario. Use grouped signals, if you want to have protected left turns");
			}
			// no left turns, no groups ...
			int T_CYC = 120;
			signalPlan = SignalUtils.createSignalPlan(conFac, T_CYC, 0, Id.create("SignalPlan1", SignalPlan.class));
			// four phases
			int TAU_SUM = 20;

			double lambda3 = flowWE / (2 * LANE_CAPACITY);
			double lambda4 = flowNS / LANE_CAPACITY;

			double lambdaSum = lambda1 + lambda2 + lambda3 + lambda4;
			int g1 = (int) Math.rint((lambda1 / lambdaSum) * ((T_CYC) - TAU_SUM));
			int g2 = (int) Math.rint((lambda2 / lambdaSum) * ((T_CYC) - TAU_SUM));
			int g3 = (int) Math.rint((lambda3 / lambdaSum) * ((T_CYC) - TAU_SUM));
			int g4 = (int) Math.rint((lambda4 / lambdaSum) * ((T_CYC) - TAU_SUM));

			// specify signal group settings for all signal groups
			signalPlan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(conFac, signalGroupId1, 0, g1));
			signalPlan.addSignalGroupSettings(
					SignalUtils.createSetting4SignalGroup(conFac, signalGroupId2, g1 + 5, g1 + 5 + g2));
			signalPlan.addSignalGroupSettings(
					SignalUtils.createSetting4SignalGroup(conFac, signalGroupId3, g1 + g2 + 10, g1 + g2 + 10 + g3));
			signalPlan.addSignalGroupSettings(SignalUtils.createSetting4SignalGroup(conFac, signalGroupId4,
					g1 + g2 + g3 + 15, g1 + g2 + g3 + 15 + g4));
		}
		signalPlan.setOffset(0);
		signalSystemControl.addSignalPlanData(signalPlan);
	}
	
	private void createGershensonSignalControl(SignalControlData signalControl) {
		SignalControlDataFactory conFac = signalControl.getFactory();
		SignalSystemControllerData signalSystemControl = conFac.createSignalSystemControllerData(signalSystemId);
        signalSystemControl.setControllerIdentifier(GershensonSignalController.IDENTIFIER);
        signalControl.addSignalSystemControllerData(signalSystemControl);
	}

	private void createLaemmerSignalControl(SignalControlData signalControl){
		SignalControlDataFactory conFac = signalControl.getFactory();

		if (!liveArrivalRates) {
            if (useLanes) {
                laemmerConfig.addArrivalRateForLane(Id.createLinkId("2_3"), Id.create("2_3.l", Lane.class), (flowWE / 3600) / 2);
                laemmerConfig.addArrivalRateForLane(Id.createLinkId("2_3"), Id.create("2_3.r", Lane.class), (flowWE / 3600) / 2);

                laemmerConfig.addArrivalRateForLane(Id.createLinkId("7_3"), Id.create("7_3.ol", Lane.class), (flowNS / 3600));

                laemmerConfig.addArrivalRateForLane(Id.createLinkId("4_3"), Id.create("4_3.l", Lane.class), (flowWE / 3600) / 2);
                laemmerConfig.addArrivalRateForLane(Id.createLinkId("4_3"), Id.create("4_3.r", Lane.class), (flowWE / 3600) / 2);

                laemmerConfig.addArrivalRateForLane(Id.createLinkId("8_3"), Id.create("8_3.ol", Lane.class), (flowNS / 3600));
            } else {
                laemmerConfig.addArrivalRateForLink(Id.createLinkId("2_3"), flowWE / 3600);
                laemmerConfig.addArrivalRateForLink(Id.createLinkId("7_3"), flowNS / 3600);
                laemmerConfig.addArrivalRateForLink(Id.createLinkId("4_3"), flowWE / 3600);
                laemmerConfig.addArrivalRateForLink(Id.createLinkId("8_3"), flowNS / 3600);
            }
        }

		SignalSystemControllerData signalSystemControl = conFac.createSignalSystemControllerData(signalSystemId);
        signalSystemControl.setControllerIdentifier(LaemmerSignalController.IDENTIFIER);
        signalControl.addSignalSystemControllerData(signalSystemControl);
	}
	
	private void createFullyAdaptiveLaemmerSignalControl(SignalControlData signalControl) {
		SignalControlDataFactory conFac = signalControl.getFactory();

		if (!liveArrivalRates) {
            if (useLanes) {
                laemmerConfig.addArrivalRateForLane(Id.createLinkId("2_3"), Id.create("2_3.l", Lane.class), (flowWE / 3600) / 2);
                laemmerConfig.addArrivalRateForLane(Id.createLinkId("2_3"), Id.create("2_3.r", Lane.class), (flowWE / 3600) / 2);

                laemmerConfig.addArrivalRateForLane(Id.createLinkId("7_3"), Id.create("7_3.ol", Lane.class), (flowNS / 3600));

                laemmerConfig.addArrivalRateForLane(Id.createLinkId("4_3"), Id.create("4_3.l", Lane.class), (flowWE / 3600) / 2);
                laemmerConfig.addArrivalRateForLane(Id.createLinkId("4_3"), Id.create("4_3.r", Lane.class), (flowWE / 3600) / 2);

                laemmerConfig.addArrivalRateForLane(Id.createLinkId("8_3"), Id.create("8_3.ol", Lane.class), (flowNS / 3600));
            } else {
                laemmerConfig.addArrivalRateForLink(Id.createLinkId("2_3"), flowWE / 3600);
                laemmerConfig.addArrivalRateForLink(Id.createLinkId("7_3"), flowNS / 3600);
                laemmerConfig.addArrivalRateForLink(Id.createLinkId("4_3"), flowWE / 3600);
                laemmerConfig.addArrivalRateForLink(Id.createLinkId("8_3"), flowNS / 3600);
            }
        }

		SignalSystemControllerData signalSystemControl = conFac.createSignalSystemControllerData(signalSystemId);
        signalSystemControl.setControllerIdentifier(FullyAdaptiveLaemmerSignalController.IDENTIFIER);
        signalControl.addSignalSystemControllerData(signalSystemControl);		
	}
	
	public void setIntersectionLogic(IntersectionLogic intersectionLogic) {
		this.intersectionLogic = intersectionLogic;
	}
}
