/* *********************************************************************** *
 * project: org.matsim.*
 * DgTaController
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package signals.laemmer.model;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;

import org.hsqldb.lib.HashMap;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.signals.model.AbstractSignalController;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalController;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.qsim.interfaces.SignalGroupState;
import org.matsim.lanes.data.Lane;
import org.matsim.lanes.data.Lanes;
import org.matsim.lanes.data.LanesToLinkAssignment;

import com.google.inject.Provider;

import jogamp.graph.font.typecast.ot.table.LtshTable;
import playground.dgrether.koehlerstrehlersignal.analysis.TtTotalDelay;
import signals.Analyzable;
import signals.downstreamSensor.DownstreamSensor;
import signals.laemmer.model.util.PermutateSignalGroups;
import signals.sensor.LinkSensorManager;


/**
 * @author dgrether
 * @author tthunig
 * @author nkuehnel
 */
//TODO Change all signalgroups to phases 

public class LaemmerSignalController2 extends AbstractSignalController implements SignalController, Analyzable {

    public static final String IDENTIFIER = "LaemmerSignalController";

    Queue<LaemmerPhase> regulationQueue = new LinkedList<>();

    final LaemmerConfig laemmerConfig;
    private final List<LaemmerPhase> laemmerPhases = new ArrayList<>();

    double desiredPeriod;

    final double MIN_G;
    double maxPeriod;

    Request activeRequest = null;
    LinkSensorManager sensorManager;
    
    DownstreamSensor downstreamSensor;

    final Network network;
    final Lanes lanes;
    final Config config;

    final double DEFAULT_INTERGREEN;
    double tIdle;

    double flowSum;

    private TtTotalDelay delayCalculator;

	private ArrayList<SignalPhase> signalPhases;


    public final static class SignalControlProvider implements Provider<SignalController> {
        private final LaemmerConfig laemmerConfig;
        private final LinkSensorManager sensorManager;
        private final TtTotalDelay delayCalculator;
		private final DownstreamSensor downstreamSensor;
		private final Scenario scenario;

        public SignalControlProvider(LaemmerConfig laemmerConfig, LinkSensorManager sensorManager, Scenario scenario, TtTotalDelay delayCalculator, DownstreamSensor downstreamSensor) {
            this.laemmerConfig = laemmerConfig;
            this.sensorManager = sensorManager;
            this.scenario = scenario;
            this.delayCalculator = delayCalculator;
            this.downstreamSensor = downstreamSensor;
        }

        @Override
        public SignalController get() {
            return new LaemmerSignalController2(laemmerConfig, sensorManager, scenario, delayCalculator, downstreamSensor);
        }
    }


    private LaemmerSignalController2(LaemmerConfig laemmerConfig, LinkSensorManager sensorManager, Scenario scenario, TtTotalDelay delayCalculator, DownstreamSensor downstreamSensor) {
        this.laemmerConfig = laemmerConfig;
        this.sensorManager = sensorManager;
        this.network = scenario.getNetwork();
        this.lanes = scenario.getLanes();
        this.config = scenario.getConfig();
        this.delayCalculator = delayCalculator;
        desiredPeriod = laemmerConfig.getDesiredCycleTime();
        maxPeriod = laemmerConfig.getMaxCycleTime();
        this.MIN_G = laemmerConfig.getMinGreenTime();
        DEFAULT_INTERGREEN = laemmerConfig.getDefaultIntergreenTime();
        this.downstreamSensor = downstreamSensor;
    }

    @Override
    public void simulationInitialized(double simStartTimeSeconds) {
    	java.util.Map<Id<Lane>, Lane> lanemap = new java.util.HashMap<>();

    	this.initializeSensoring();
    	
    	for (LanesToLinkAssignment ltl : lanes.getLanesToLinkAssignments().values())
   			lanemap.putAll(ltl.getLanes());
    		
        //Phasen kombinatorisch erstellen
    	this.signalPhases = PermutateSignalGroups.createPhasesFromSignalGroups(system, lanemap);
        
        for (SignalPhase signalPhase : signalPhases) {
        	LaemmerPhase laemmerPhase = new LaemmerPhase(this, signalPhase);
        	for (Id<SignalGroup> group : signalPhase.getGreenSignalGroups()) {
        		this.system.scheduleDropping(simStartTimeSeconds, group);
        	}
        	laemmerPhases.add(laemmerPhase);
		}
    }

    @Override
    public boolean isAnalysisEnabled() {
        return this.laemmerConfig.isAnalysisEnabled();
    }

    @Override
    public void updateState(double now) {
        updateRepresentativeDriveways(now);
        if (!laemmerConfig.getActiveRegime().equals(LaemmerConfig.Regime.OPTIMIZING)) {
            updateActiveRegulation(now);
        }
        updateSignals(now);
        if(activeRequest != null && activeRequest.signal.phase.getState().equals(SignalGroupState.GREEN)) {
            double remainingMinG = activeRequest.time + MIN_G - now;
//            double remainingInBetweenTime = Math.max(activeRequest.time - now, 0);
//            double remainingMinG = Math.max(activeRequest.time - now + MIN_G - remainingInBetweenTime, 0);
            if (remainingMinG > 0) {
                return;
            }
        }
        LaemmerPhase selection = selectSignal();
        processSelection(now, selection);
    }

    private void updateActiveRegulation(double now) {
        if (activeRequest != null && !regulationQueue.isEmpty() && regulationQueue.peek().equals(activeRequest.signal)) {
            LaemmerPhase phase = regulationQueue.peek();
            int n;
            if (phase.determiningLane != null) {
                n = getNumberOfExpectedVehiclesOnLane(now, phase.determiningLink, phase.determiningLane);
            } else {
                n = getNumberOfExpectedVehiclesOnLink(now, phase.determiningLink);
            }
            if (activeRequest.signal.regulationTime + activeRequest.time - now <= 0 || n == 0) {
                regulationQueue.poll();
            }
        }
    }

    private LaemmerPhase selectSignal() {
        LaemmerPhase max = null;
        if (!laemmerConfig.getActiveRegime().equals(LaemmerConfig.Regime.OPTIMIZING)) {
            max = regulationQueue.peek();
        }
        if (!laemmerConfig.getActiveRegime().equals(LaemmerConfig.Regime.STABILIZING)) {
            if (max == null) {
                double index = 0;
                for (LaemmerPhase signal : laemmerPhases) {
                    if (signal.index > index) {
                    	// if downstream check enabled, only select signals that do not lead to occupied links
                    	if (!laemmerConfig.isCheckDownstream()){
	                    	boolean isAllDownstramLinksEmpty = true;
	                    	for (Id<SignalGroup> sg : signal.phase.getGreenSignalGroups())
	                    		isAllDownstramLinksEmpty &= downstreamSensor.allDownstreamLinksEmpty(system.getId(), sg);
                    		if (isAllDownstramLinksEmpty) {
	                    		max = signal;
	                        	index = signal.index;
	                        	}
                    	}
                    }
                }
            }
        }
        return max;
    }

    private void processSelection(double now, LaemmerPhase max) {

        if (activeRequest != null && activeRequest.signal != max) {
            activeRequest.signal.phase.getGreenSignalGroups().forEach(sg->this.system.scheduleDropping(now, sg));
            activeRequest = null;
        }

        if (activeRequest == null && max != null || activeRequest != null && max != null && !max.equals(activeRequest.signal)) {
            activeRequest = new Request(now + DEFAULT_INTERGREEN, max);
        }

        if (activeRequest != null) {
            if (activeRequest.isDue(now)) {
            	activeRequest.signal.phase.getGreenSignalGroups().forEach(sg->this.system.scheduleOnset(now, sg));
            } else if (activeRequest.time > now) {
            }
        }
    }

    private void updateSignals(double now) {
        for (LaemmerPhase signal : laemmerPhases) {
            signal.update(now);
            // this is already done in updateStabilization in LaemmerSignal called by the above, theresa jul'17
//            if (signal.stabilize && !regulationQueue.contains(signal)) {
//                regulationQueue.add(signal);
//            }
        }
    }

    private void updateRepresentativeDriveways(double now) {
        flowSum = 0;
        tIdle = desiredPeriod;
        for (LaemmerPhase signal : laemmerPhases) {
            signal.determineRepresentativeDriveway(now);
            flowSum += signal.outflowSum;
            tIdle -= Math.max(signal.determiningLoad * desiredPeriod + DEFAULT_INTERGREEN, MIN_G);
        }
        tIdle = Math.max(0, tIdle);
    }

    int getNumberOfExpectedVehiclesOnLink(double now, Id<Link> linkId) {
        return this.sensorManager.getNumberOfCarsInDistance(linkId, 0., now);
    }

    int getNumberOfExpectedVehiclesOnLane(double now, Id<Link> linkId, Id<Lane> laneId) {
        if (lanes.getLanesToLinkAssignments().get(linkId).getLanes().size() == 1) {
            return getNumberOfExpectedVehiclesOnLink(now, linkId);
        } else {
            return this.sensorManager.getNumberOfCarsInDistanceOnLane(linkId, laneId, 0., now);
        }
    }

    double getAverageArrivalRate(double now, Id<Link> linkId) {
        if (this.laemmerConfig.getLinkArrivalRate(linkId) != null) {
            return this.laemmerConfig.getLinkArrivalRate(linkId);
        } else {
            return this.sensorManager.getAverageArrivalRateOnLink(linkId, now);
        }
    }

    double getAverageLaneArrivalRate(double now, Id<Link> linkId, Id<Lane> laneId) {
        if (lanes.getLanesToLinkAssignments().get(linkId).getLanes().size() > 1) {
            if (this.laemmerConfig.getLaneArrivalRate(linkId, laneId) != null) {
                return this.laemmerConfig.getLaneArrivalRate(linkId, laneId);
            } else {
                return this.sensorManager.getAverageArrivalRateOnLane(linkId, laneId, now);
            }
        } else {
            return getAverageArrivalRate(now, linkId);
        }
    }


    @Override
    public void reset(Integer iterationNumber) {
    }

    private void initializeSensoring() {
        for (SignalGroup group : this.system.getSignalGroups().values()) {
            for (Signal signal : group.getSignals().values()) {
                if (signal.getLaneIds() != null && !(signal.getLaneIds().isEmpty())) {
                    for (Id<Lane> laneId : signal.getLaneIds()) {
                        this.sensorManager.registerNumberOfCarsOnLaneInDistanceMonitoring(signal.getLinkId(), laneId, 0.);
                        this.sensorManager.registerAverageNumberOfCarsPerSecondMonitoringOnLane(signal.getLinkId(), laneId);
                    }
                }
                //always register link in case only one lane is specified (-> no LaneEnter/Leave-Events?)
                this.sensorManager.registerNumberOfCarsInDistanceMonitoring(signal.getLinkId(), 0.);
                this.sensorManager.registerAverageNumberOfCarsPerSecondMonitoring(signal.getLinkId());
            }
        }
        if (laemmerConfig.isCheckDownstream()){
			downstreamSensor.registerDownstreamSensors(system);
        }
    }

    @Override
    public String getStatFields() {

        StringBuilder builder = new StringBuilder();
        builder.append("T_idle;selected;total delay;");
        for (LaemmerPhase laemmerSignal : laemmerPhases) {
            laemmerSignal.getStatFields(builder);
        }
        return builder.toString();
    }

    @Override
    public String getStepStats(double now) {

        StringBuilder builder = new StringBuilder();
        String selected = "none";
        if (activeRequest != null) {
            selected = activeRequest.signal.phase.getId().toString();
        }
        builder.append(tIdle + ";" + selected + ";" + delayCalculator.getTotalDelay() + ";");
        for (LaemmerPhase laemmerSignal : laemmerPhases) {
           //TODO implement getStepStats
           //laemmerSignal.getStepStats(builder, now);
        }
        return builder.toString();
    }

    class Request {
        final double time;
        final LaemmerPhase signal;

        Request(double time, LaemmerPhase laemmerSignal) {
            this.signal = laemmerSignal;
            this.time = time;
        }

        private boolean isDue(double timeSeconds) {
            return timeSeconds == this.time;
        }
    }

	public SignalSystem getSystem() {
		return this.system;
	}
}
