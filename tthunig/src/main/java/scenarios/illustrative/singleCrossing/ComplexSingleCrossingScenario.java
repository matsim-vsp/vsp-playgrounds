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
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.contrib.signals.data.conflicts.ConflictData;
import org.matsim.contrib.signals.data.conflicts.IntersectionDirections;
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
import org.matsim.contrib.signals.otfvis.OTFVisWithSignalsLiveModule;
import org.matsim.contrib.signals.utils.SignalUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.lanes.Lane;
import org.matsim.lanes.Lanes;
import org.matsim.lanes.LanesFactory;
import org.matsim.lanes.LanesToLinkAssignment;
import org.matsim.lanes.LanesUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import signals.CombinedSignalsModule;
import signals.advancedPlanbased.AdvancedPlanBasedSignalSystemController;
import signals.laemmer.FullyAdaptiveLaemmerSignalController;
import signals.laemmer.LaemmerConfig;
import signals.laemmer.LaemmerSignalController;
import signals.laemmer.LaemmerConfig.Regime;
import signals.laemmer.LaemmerConfig.StabilizationStrategy;
import signals.laemmer.model.util.Conflicts;
import utils.OutputUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * @author tthunig, pschade
 *
 */
@Deprecated // use SingleCrossingScenario instead. Please report, if there are use cases where it does not work. tthunig, aug'18
public class ComplexSingleCrossingScenario {

	private String OUTPUT_BASE_DIR = "../../runs-svn/complexSingleCrossingScenario/";
    private static final int LANE_CAPACITY = 1800;
	public static final Id<SignalGroup> signalGroupId1 = Id.create("SignalGroup1", SignalGroup.class);
	public static final Id<SignalGroup> signalGroupId2 = Id.create("SignalGroup2", SignalGroup.class);
	public static final Id<SignalGroup> signalGroupId1l = Id.create("SignalGroup1l", SignalGroup.class);
	public static final Id<SignalGroup> signalGroupId3 = Id.create("SignalGroup3", SignalGroup.class);
	public static final Id<SignalGroup> signalGroupId4 = Id.create("SignalGroup4", SignalGroup.class);
	public static final Id<SignalSystem> signalSystemId = Id.create("SignalSystem1", SignalSystem.class);

	private double flowNS = 1200;
	private double flowWE = 0.5 * 2520;
	public enum SignalControl {FIXED, LAEMMER_NICO, LAEMMER_FULLY_ADAPTIVE};
	SignalControl signalControlSelect = SignalControl.LAEMMER_FULLY_ADAPTIVE;
	private Regime laemmerRegime = Regime.STABILIZING;
	private boolean vis = false;
	private boolean logEnabled = false;
	private boolean stochasticDemand = false;
	private boolean useLanes = true;
	private boolean liveArrivalRates = true;
	private boolean groupedSignals = true;
	private double minG = 0;
	private boolean temporalCrowd = false;
	private double leftTurningFactorNS;
	private double leftTurningFactorWE;
	private StabilizationStrategy stabilizationStrategy;
	private IntersectionLogic intersectionLogic = IntersectionLogic.NONE;
	private boolean protectedLeftTurn = true;

	public void setFlowNS(double flowNS) {
		this.flowNS = flowNS;
	}

	public void setFlowWE(double flowWE) {
		this.flowWE = flowWE;
	}

	public void setSignalType(Regime laemmerRegime) {
		this.laemmerRegime = laemmerRegime;
	}
	
	/**
	 * @param useLaemmer if false, fixed-time signals are used
	 */
	public void setSignalControl(SignalControl signalControl){
		this.signalControlSelect = signalControl;
	}

	public void setVis(boolean vis) {
		this.vis = vis;
	}

	public void setLogEnabled(boolean log) {
		this.logEnabled = log;
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
	
	public ComplexSingleCrossingScenario() {}
	
	/**
	 * constructor useful for scenarios without laemmer signals
	 */
	public ComplexSingleCrossingScenario(double flowNS, double flowWE, SignalControl signalControl, boolean vis, boolean logEnabled, 
			boolean stochastic, boolean lanes, boolean grouped, boolean temporalCrowd) {
		this.flowNS = flowNS;
		this.flowWE = flowWE;
		this.signalControlSelect = signalControl;
		this.vis = vis;
		this.logEnabled = logEnabled;
		this.stochasticDemand = stochastic;
		this.useLanes = lanes;
		this.groupedSignals = grouped;
		this.temporalCrowd = temporalCrowd;
	}
	
	public ComplexSingleCrossingScenario(double flowNS, double flowWE, SignalControl signalControl, Regime laemmerRegime, boolean vis, 
			boolean logEnabled, boolean stochastic, boolean lanes,
			boolean liveArrivalRates, boolean grouped, double minG, boolean temporalCrowd) {
		this(flowNS, flowWE, signalControl, vis, logEnabled, stochastic, lanes, grouped, temporalCrowd);
		this.laemmerRegime = laemmerRegime;
		this.liveArrivalRates = liveArrivalRates;
		this.minG = minG;
	}

	/** 
	 * @param flowNS
	 * @param leftTurningFactorNS Factor to calculate the flow from flowNS. Left-turning flow will not be subtracted from straight flow
	 * @param flowWE
	 * @param leftTurningFactorWE Factor to calculate the flow from flowWE. Left-turning flow will not be subtracted from straight flow
	 * @param useLaemmer
	 * @param laemmerRegime
	 * @param vis
	 * @param logEnabled
	 * @param stochastic
	 * @param lanes
	 * @param liveArrivalRates
	 * @param grouped
	 * @param minG
	 * @param temporalCrowd
	 */
	public ComplexSingleCrossingScenario(double flowNS, double leftTurningFactorNS, double flowWE, double leftTurningFactorWE, 
			SignalControl signalControl, Regime laemmerRegime, StabilizationStrategy stabilizationStrategy, boolean vis, boolean logEnabled, 
			boolean stochastic, boolean lanes,
			boolean liveArrivalRates, boolean grouped, double minG, boolean temporalCrowd) {
		this(flowNS, flowWE, signalControl, vis, logEnabled, stochastic, lanes, grouped, temporalCrowd);
		this.laemmerRegime = laemmerRegime;
		this.liveArrivalRates = liveArrivalRates;
		this.stabilizationStrategy = stabilizationStrategy;
		this.minG = minG;
		this.leftTurningFactorNS = leftTurningFactorNS;
		this.leftTurningFactorWE = leftTurningFactorWE;
	}
	
	public Controler defineControler() {
		CombinedSignalsModule signalsModule = new CombinedSignalsModule();
        LaemmerConfig laemmerConfig = new LaemmerConfig();
        laemmerConfig.setDefaultIntergreenTime(5);
        laemmerConfig.setActiveStabilizationStrategy(stabilizationStrategy);
        if(groupedSignals) {
            laemmerConfig.setDesiredCycleTime(60);
            laemmerConfig.setMaxCycleTime(90);
        } else {
            laemmerConfig.setDesiredCycleTime(120);
            laemmerConfig.setMaxCycleTime(180);
        }
        laemmerConfig.setMinGreenTime(minG);
        laemmerConfig.setActiveRegime(laemmerRegime);
        laemmerConfig.setAnalysisEnabled(logEnabled);
        //laemmerConfig.setAvgCarSensorBucketParameters(360.0, 90.0);
        signalsModule.setLaemmerConfig(laemmerConfig);

        final Scenario scenario = defineScenario(laemmerConfig);
        Controler controler = new Controler(scenario);
        
        controler.addOverridingModule(signalsModule);

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
	
	private Scenario defineScenario(LaemmerConfig laemmerConfig) {
        Scenario scenario = ScenarioUtils.loadScenario(defineConfig(createOutputPath()));
        // add missing scenario elements
        scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(scenario.getConfig()).loadSignalsData());
        createNetwork(scenario.getNetwork());
        if (useLanes) {
        		ComplexSingleCrossingScenario.createLanes(scenario.getLanes());
        } 
        createPopulation(scenario.getPopulation());
        createSignals(scenario, laemmerConfig);
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
    	String outputPath = OUTPUT_BASE_DIR + OutputUtils.getCurrentDate() + "/";
    	switch (signalControlSelect){
    	case LAEMMER_NICO:
    		outputPath += "laemmer" + laemmerRegime.name();
    		break;
    	case LAEMMER_FULLY_ADAPTIVE:
    		outputPath += "laemmerFullyAdaptive" + laemmerRegime.name()+"-"+stabilizationStrategy.name();
    		break;
    	case FIXED:
    		outputPath += "fixedTime";
    		break;
    	}
        if (stochasticDemand) {
        	outputPath += "_stochastic";
        } else {
        	outputPath += "_constant";
        }
        if (useLanes){
        	outputPath += "_lanes";
        } else {
        	outputPath += "_noLanes";
        }
        if (liveArrivalRates){
        	outputPath += "_liveArrival";
        }
    	outputPath += "/_ew" + flowWE +"-left"+leftTurningFactorWE+ "_ns" + flowNS+"-left"+leftTurningFactorNS;
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

        SignalSystemsConfigGroup signalConfigGroup = ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
        signalConfigGroup.setUseSignalSystems(true);
        signalConfigGroup.setIntersectionLogic(intersectionLogic);

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
//            link.setNumberOfLanes(2);
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
                null, Arrays.asList(Id.create("2_3.l", Lane.class), Id.create("2_3.s", Lane.class), Id.create("2_3.r", Lane.class)));

        
        //left turning lane (alignment 1)
        LanesUtils.createAndAddLane(lanesForLink2_3, factory,
        		Id.create("2_3.l", Lane.class), LANE_CAPACITY, 500, 1, 1,
        		Arrays.asList(Id.create("3_7", Link.class)), null);
         
        // straight lane (alignment 0)
        LanesUtils.createAndAddLane(lanesForLink2_3, factory,
                Id.create("2_3.s", Lane.class), LANE_CAPACITY, 500, 0, 1,
                Arrays.asList(Id.create("3_4", Link.class)), null);

        // right turning lane (alignment -1)
        LanesUtils.createAndAddLane(lanesForLink2_3, factory,
                Id.create("2_3.r", Lane.class), LANE_CAPACITY, 500, -1, 1,
                Arrays.asList(Id.create("3_4", Link.class), Id.create("3_8", Link.class)), null);
        
        // create lanes for link 4_3
        LanesToLinkAssignment lanesForLink4_3 = factory
                .createLanesToLinkAssignment(Id.create("4_3", Link.class));
        lanes.addLanesToLinkAssignment(lanesForLink4_3);

        // original lane, i.e. lane that starts at the link from node and leads to all other lanes of the link
        LanesUtils.createAndAddLane(lanesForLink4_3, factory,
                Id.create("4_3.ol", Lane.class), 3600, 1000, 0, 2,
                null, Arrays.asList(Id.create("4_3.l", Lane.class), Id.create("4_3.s", Lane.class), Id.create("4_3.r", Lane.class)));

        // left turning lane (alignment 1)
        LanesUtils.createAndAddLane(lanesForLink4_3, factory,
                Id.create("4_3.l", Lane.class), LANE_CAPACITY, 500, 1, 1,
                Arrays.asList(Id.create("3_8", Link.class)), null);

        // straight lane (alignment 0)
        LanesUtils.createAndAddLane(lanesForLink4_3, factory,
                Id.create("4_3.s", Lane.class), LANE_CAPACITY, 500, 0, 1,
                Arrays.asList(Id.create("3_2", Link.class)), null);
        
        // right turning lane (alignment -1)
        LanesUtils.createAndAddLane(lanesForLink4_3, factory,
                Id.create("4_3.r", Lane.class), LANE_CAPACITY, 500, -1, 1,
                Arrays.asList(Id.create("3_2", Link.class), Id.create("3_7", Link.class)), null);

        // create lanes for link 7_3
        LanesToLinkAssignment lanesForLink7_3 = factory
                .createLanesToLinkAssignment(Id.create("7_3", Link.class));
        lanes.addLanesToLinkAssignment(lanesForLink7_3);
        
        // original lane, i.e. lane that starts at the link from node and leads to all other lanes of the link
        LanesUtils.createAndAddLane(lanesForLink7_3, factory,
                Id.create("7_3.ol", Lane.class), LANE_CAPACITY, 1000, 0, 1,
                Arrays.asList(Id.create("3_4", Link.class), Id.create("3_2", Link.class), Id.create("3_8", Link.class)), null);

        // create lanes for link 8_3
        LanesToLinkAssignment lanesForLink8_3 = factory
                .createLanesToLinkAssignment(Id.create("8_3", Link.class));
        lanes.addLanesToLinkAssignment(lanesForLink8_3);

        // original lane, i.e. lane that starts at the link from node and leads to all other lanes of the link
        LanesUtils.createAndAddLane(lanesForLink8_3, factory,
                Id.create("8_3.ol", Lane.class), LANE_CAPACITY, 1000, 0, 1,
                Arrays.asList(Id.create("3_2", Link.class), Id.create("3_7", Link.class), Id.create("3_4", Link.class)), null);
    }
    
    private void createPopulation(Population pop) {
        String[] linksNS = {"6_7-8_9", "9_8-7_6"};
        String[] linksWE = {"5_4-2_1", "1_2-4_5"};
        String[] linksNSleftTurning = {"6_7-4_5", "8_9-2_1"};
        String[] linksWEleftTurning = {"1_2-7_6", "5_4-8_9"};

        Random rnd = new Random(14);
        createPopulationForRelation(flowNS, pop, linksNS, rnd);
        createPopulationForRelation(flowWE, pop, linksWE, rnd);
        createPopulationForRelation(flowNS * leftTurningFactorNS, pop, linksNSleftTurning, rnd);
        createPopulationForRelation(flowWE * leftTurningFactorWE, pop, linksWEleftTurning, rnd);
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
    
    private void createSignals(Scenario scenario, LaemmerConfig laemmerConfig) {
        SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
        SignalSystemsData signalSystems = signalsData.getSignalSystemsData();
        SignalSystemsDataFactory sysFac = signalSystems.getFactory();
        SignalGroupsData signalGroups = signalsData.getSignalGroupsData();
        SignalControlData signalControl = signalsData.getSignalControlData();
        
        // create the signal system for the single intersection
        SignalSystemData signalSystem1 = sysFac.createSignalSystemData(signalSystemId);
        signalSystems.addSignalSystemData(signalSystem1);

        // create a signal for every inLink
        for (Link inLink : scenario.getNetwork().getNodes().get(Id.createNodeId(3)).getInLinks().values()) {
        		SignalData signal = null;
        	
        		if (!useLanes) {
        			signal = sysFac.createSignalData(Id.create("Signal" + inLink.getId(), Signal.class));
        			signal.setLinkId(inLink.getId());
        			signalSystem1.addSignalData(signal);
        		} else {
                for (Lane lane : scenario.getLanes().getLanesToLinkAssignments().get(inLink.getId()).getLanes().values()) {
                	signal = null;
                    if(lane.getToLinkIds() != null && !lane.getToLinkIds().isEmpty()) {
                    	//here we need to find a signal which already has a lane leading to one of the outlinks of the currently processed lane for which we need to find/add a signal
                    	List<SignalData> signalDataForCurrentLink = new LinkedList<>();
                    	if (signalSystem1.getSignalData() != null) {
//                    			signalDataForCurrentLink = signalSystem1.getSignalData().values().stream().filter(sd->sd.getLinkId().equals(inLink.getId())).collect(Collectors.toList());
                    		for (SignalData sd : signalSystem1.getSignalData().values()) {
                    			if (sd.getLinkId().equals(inLink.getId())){
                    				signalDataForCurrentLink.add(sd);
                    			}
                    		}
                    	}
                    	if (signalDataForCurrentLink != null) {
                    		// (1) iterate over all signalDatas which have same link as the current lane is positioned on
							for (SignalData sd : signalDataForCurrentLink) {
								// (2) iterate over all lanes of this signal
								for (Id<Lane> l : sd.getLaneIds()) {
									//iterate over all toLinks of every lane of the signal from (2) 
									for (Id<Link> knownToLink : scenario.getLanes().getLanesToLinkAssignments()
											.get(inLink.getId()).getLanes().get(l).getToLinkIds()) {
										//iterate over all toLinks of the currently processed lane for which we need to find/add a signal
										for (Id<Link> toLink : lane.getToLinkIds()) {
											//finally we (hopefully) found one signal
											if (knownToLink.equals(toLink))
												signal = sd;
										}
									}
								}
							} 
						}
						if (signal == null) {
                    		signal = sysFac.createSignalData(Id.create("Signal" + inLink.getId() + "_" +lane.getId(), Signal.class));
                    	}
                    	signal.addLaneId(lane.getId());
                    	signal.setLinkId(inLink.getId());
                    	signalSystem1.addSignalData(signal);
                    }
                }
            }
        }
        
        
        // create one signal group for each signal
		if (this.signalControlSelect.equals(SignalControl.LAEMMER_FULLY_ADAPTIVE)) {
			SignalUtils.createAndAddSignalGroups4Signals(signalGroups, signalSystem1);
		} else {
			SignalGroupData signalGroup1 = signalGroups.getFactory().createSignalGroupData(signalSystemId,
					signalGroupId1);
			if (useLanes) {
				signalGroup1.addSignalId(Id.create("Signal2_3_2_3.r", Signal.class));
				signalGroup1.addSignalId(Id.create("Signal4_3_4_3.r", Signal.class));
			} else {
				signalGroup1.addSignalId(Id.create("Signal2_3", Signal.class));
				signalGroup1.addSignalId(Id.create("Signal4_3", Signal.class));
			}
			signalGroups.addSignalGroupData(signalGroup1);

			SignalGroupData signalGroup2 = signalGroups.getFactory().createSignalGroupData(signalSystemId,
					signalGroupId2);
			if (useLanes) {
				signalGroup2.addSignalId(Id.create("Signal7_3_7_3.ol", Signal.class));
				signalGroup2.addSignalId(Id.create("Signal8_3_8_3.ol", Signal.class));
			} else {
				signalGroup2.addSignalId(Id.create("Signal7_3", Signal.class));
				signalGroup2.addSignalId(Id.create("Signal8_3", Signal.class));
			}
			signalGroups.addSignalGroupData(signalGroup2);

			SignalGroupData signalGroup1l = signalGroups.getFactory().createSignalGroupData(signalSystemId,
					signalGroupId1l);
			if (useLanes) {
				signalGroup1l.addSignalId(Id.create("Signal2_3_2_3.l", Signal.class));
				signalGroup1l.addSignalId(Id.create("Signal4_3_4_3.l", Signal.class));
			} else {
				signalGroup1l.addSignalId(Id.create("Signal2_3", Signal.class));
				signalGroup1l.addSignalId(Id.create("Signal4_3", Signal.class));
			}
			signalGroups.addSignalGroupData(signalGroup1l);
		}
		if (this.signalControlSelect.equals(SignalControl.LAEMMER_NICO)
				|| this.signalControlSelect.equals(SignalControl.LAEMMER_FULLY_ADAPTIVE)) {
			createLaemmerSignalControl(signalControl, laemmerConfig);
		} else {
			createFixedTimeSignalControl(signalControl);
		}
    }

	private void createFixedTimeSignalControl(SignalControlData signalControl) {
		SignalControlDataFactory conFac = signalControl.getFactory();

        SignalSystemControllerData signalSystemControl = conFac.createSignalSystemControllerData(signalSystemId);
        signalSystemControl.setControllerIdentifier(AdvancedPlanBasedSignalSystemController.IDENTIFIER);
        signalControl.addSignalSystemControllerData(signalSystemControl);

        // create a plan for the signal system
        SignalPlanData signalPlan;
        
        double lambda1 = flowWE / (2*LANE_CAPACITY);
        double lambda2 = flowNS / LANE_CAPACITY;
        double lambda1l = (flowWE * leftTurningFactorWE) / LANE_CAPACITY;
        
		if (groupedSignals) {
			// TODO T_CYC should be synchronized with desiredCycleTime from laemmerConfig, pschade Jan'18
			int T_CYC = 60;
			signalPlan = SignalUtils.createSignalPlan(conFac, T_CYC, 0, Id.create("SignalPlan1", SignalPlan.class));
			int intergreen = 5;

			int TAU_SUM;
			if (protectedLeftTurn) {
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
		} else { // no left turns, no groups
			int T_CYC = 120;
			signalPlan = SignalUtils.createSignalPlan(conFac, T_CYC, 0, Id.create("SignalPlan1", SignalPlan.class));
			// four phases
			int TAU_SUM = 20;

			double lambda3 = flowWE / (2*LANE_CAPACITY);
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
	
	private void createLaemmerSignalControl(SignalControlData signalControl, LaemmerConfig laemmerConfig){
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
		if (this.signalControlSelect.equals(SignalControl.LAEMMER_FULLY_ADAPTIVE)) {
			signalSystemControl.setControllerIdentifier(FullyAdaptiveLaemmerSignalController.IDENTIFIER);
		} else if (this.signalControlSelect.equals(SignalControl.LAEMMER_NICO)) {
			signalSystemControl.setControllerIdentifier(LaemmerSignalController.IDENTIFIER);
		}
		signalControl.addSignalSystemControllerData(signalSystemControl);
	}

	public void setIntersectionLogic(IntersectionLogic intersectionLogic) {
		this.intersectionLogic = intersectionLogic;
	}
	
	public void setProtectedLeftTurnForFixedTimeSignals(boolean protectedLeftTurn) {
		this.protectedLeftTurn = protectedLeftTurn;
	}
	
}
