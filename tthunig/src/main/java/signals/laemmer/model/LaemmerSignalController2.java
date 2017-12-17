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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import com.google.inject.Provider;

import org.matsim.api.core.v01.Id;
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

    public static final String IDENTIFIER = "LaemmerSignalController2";

    Queue<LaemmerPhase> regulationQueue = new LinkedList<>();

    final LaemmerConfig laemmerConfig;
    private final List<LaemmerPhase> laemmerPhases = new ArrayList<>();

    //TODO sichere Handhabung für Request mit laemmerphase
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
	private List<LaemmerLane> laemmerLanes = new LinkedList<>();
	private List<LaemmerLane> lanesForStabilization = new LinkedList<>();


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
        if (laemmerConfig.isUseDefaultIntergreenTime()) {
			DEFAULT_INTERGREEN = laemmerConfig.getDefaultIntergreenTime();
		} else {
			throw new UnsupportedOperationException("Laemmer with signal specific intergreen times is not yet implemented.");
		}
        this.downstreamSensor = downstreamSensor;
    }

    @Override
    public void simulationInitialized(double simStartTimeSeconds) {
    	java.util.Map<Id<Lane>, Lane> lanemap = new java.util.HashMap<>();
    	laemmerLanes = new LinkedList<>();

    	this.initializeSensoring();
    	
    	for (LanesToLinkAssignment ltl : lanes.getLanesToLinkAssignments().values()) {
   			lanemap.putAll(ltl.getLanes());	
    	}

    	System.err.println("SIGGROUPS SIZE: "+system.getSignalGroups().size());

        //Phasen kombinatorisch erstellen
    	this.signalPhases = PermutateSignalGroups.createPhasesFromSignalGroups(system, lanemap);
        
    	System.err.println("SIGPHASES SIZE: "+signalPhases.size());
    	
        for (SignalPhase signalPhase : signalPhases) {
        	LaemmerPhase laemmerPhase = new LaemmerPhase(this, signalPhase);
        	System.err.println("SIGPHASE: " +signalPhase.getId());
        	for (Id<SignalGroup> group : signalPhase.getGreenSignalGroups()) {
        		this.system.scheduleDropping(simStartTimeSeconds, group);
        	}
        	laemmerPhases.add(laemmerPhase);
		}
        
        //create a laemmerLane for each signalized lane
        for (SignalGroup sg : this.system.getSignalGroups().values()) {
        	for (Signal signal : sg.getSignals().values())
        		for (Id<Lane> laneId : signal.getLaneIds())
        			laemmerLanes.add(new LaemmerLane(this.network.getLinks().get(signal.getLinkId()), lanemap.get(laneId), sg, signal, this));
        }
        
        
    }

    @Override
    public void updateState(double now) {
        updateRepresentativeDriveways(now);
        if (laemmerConfig.getActiveRegime().equals(LaemmerConfig.Regime.COMBINED) ||
        		laemmerConfig.getActiveRegime().equals(LaemmerConfig.Regime.STABILIZING)) {
            updateActiveRegulation(now);
        }
        updateSignals(now);
        
        // TODO test what happens, when I move this up to the first line of this method. should save runtime. tt, dez'17
        if(activeRequest != null && activeRequest.laemmerPhase.phase.getState().equals(SignalGroupState.GREEN)) {
            double remainingMinG = activeRequest.onsetTime + laemmerConfig.getMinGreenTime() - now;
//            double remainingInBetweenTime = Math.max(activeRequest.time - now, 0);
//            double remainingMinG = Math.max(activeRequest.time - now + laemmerConfig.getMinGreenTime() - remainingInBetweenTime, 0);
            if (remainingMinG > 0) {
                return;
            }
        }
        
        LaemmerPhase selection = selectSignal();
        processSelection(now, selection);
    }

    @Override
    public boolean isAnalysisEnabled() {
    	return this.laemmerConfig.isAnalysisEnabled();
    }
    
    private void updateActiveRegulation(double now) {
        if (activeRequest != null && !regulationQueue.isEmpty() && regulationQueue.peek().equals(activeRequest.laemmerPhase)) {
            LaemmerPhase phase = regulationQueue.peek();
            int n;
            if (phase.determiningLane != null) {
                n = getNumberOfExpectedVehiclesOnLane(now, phase.determiningLink, phase.determiningLane);
            } else {
                n = getNumberOfExpectedVehiclesOnLink(now, phase.determiningLink);
            }
            if (activeRequest.laemmerPhase.regulationTime + activeRequest.onsetTime - now <= 0 || n == 0) {
                regulationQueue.poll();
            }
        }
    }

    private LaemmerPhase selectSignal() {
        LaemmerPhase max = null;
        
        //selection if stabilization is needed
        if ((laemmerConfig.getActiveRegime().equals(LaemmerConfig.Regime.COMBINED)
        		|| laemmerConfig.getActiveRegime().equals(LaemmerConfig.Regime.STABILIZING)
        		) && lanesForStabilization.size() > 0) {
            //max = regulationQueue.peek();
        	if (laemmerConfig.useHeuristicPhaseGeneration()) {
        		throw new IllegalStateException("Heuristic generation of phases not yet implemented.");
        	}
        	else {
        		Map<LaemmerPhase, java.lang.Integer> stabilizationCandidatePhases = new HashMap<>();
        		
        		//count how many lanes in each phase can be stabilized
        		for (LaemmerPhase laemmerPhase : laemmerPhases) {
        			int numOfLanesCanBeStabilized = 0;
        			for (Id<Lane> lane : laemmerPhase.phase.getGreenLanes())
        				for (LaemmerLane stabilizationLaneCandidate : lanesForStabilization) {
        					if (stabilizationLaneCandidate.getLane().getId().equals(lane))
        						numOfLanesCanBeStabilized++;
        				}
        			if (numOfLanesCanBeStabilized > 0)
        				stabilizationCandidatePhases.put(laemmerPhase, new java.lang.Integer(numOfLanesCanBeStabilized));
        		}
        		
        		//determine the phase which stabilizing the most lanes and select it
        		int maxStabilizationLanes = 0;
        		for (Entry<LaemmerPhase, Integer> stabilizingCandidatePhase : stabilizationCandidatePhases.entrySet()) {
        			if (stabilizingCandidatePhase.getValue().intValue() > maxStabilizationLanes) {
        				max = stabilizingCandidatePhase.getKey();
        				maxStabilizationLanes = stabilizingCandidatePhase.getValue().intValue();
        			}
        		}
        	}
        	System.err.print("\nStabilizing with phase ");
        	for (Id<SignalGroup> sg : max.phase.getGreenSignalGroups())
        		System.err.print(sg+"; ");
        }
        
        //selection for optimizing
        if (laemmerConfig.getActiveRegime().equals(LaemmerConfig.Regime.COMBINED) ||
        		laemmerConfig.getActiveRegime().equals(LaemmerConfig.Regime.OPTIMIZING)) {
            if (max == null) {
                double index = 0;
                for (LaemmerPhase phase : laemmerPhases) {
                    if (phase.index > index) {
                    	// if downstream check enabled, only select signals that do not lead to occupied links
                    	if (!laemmerConfig.isCheckDownstream()){
	                    	boolean isAllDownstramLinksEmpty = true;
	                    	for (Id<SignalGroup> sg : phase.phase.getGreenSignalGroups()) {
	                    		try {
	                    			isAllDownstramLinksEmpty &= downstreamSensor.allDownstreamLinksEmpty(system.getId(), sg);
	                    		}
	                    		catch (IllegalStateException e) {
	                    			System.out.println("error: "+e.toString());
								}
	                    	}
                    		if (isAllDownstramLinksEmpty) {
	                    		max = phase;
	                        	index = phase.index;
	                        	}
                    	}
                    }
                }
            }
        }
        return max;
    }

    private void processSelection(double now, LaemmerPhase max) {

        if (activeRequest != null && ( max == null || !max.equals(activeRequest.laemmerPhase))) {
        	/* quit the active request, when the next selection (max) is different from the current (activeRequest)
    		 * or, when the next selection (max) is null
    		 */
        	if(activeRequest.onsetTime < now) {
    			// do not schedule a dropping when the signal does not yet show green
        		activeRequest.laemmerPhase.phase.getGreenSignalGroups().forEach(sg->this.system.scheduleDropping(now, sg));
        	}
            activeRequest = null;
        }

        if (activeRequest == null && max != null) {
            activeRequest = new Request(now + DEFAULT_INTERGREEN, max);
        }

        if (activeRequest != null && activeRequest.isDue(now)) {
           	activeRequest.laemmerPhase.phase.getGreenSignalGroups().forEach(sg->this.system.scheduleOnset(now, sg));
        }
    }

    private void updateSignals(double now) {
        for (LaemmerPhase phase : laemmerPhases) {
            phase.update(now);
            // this is already done in updateStabilization in LaemmerSignal called by the above, theresa jul'17
//            if (signal.stabilize && !regulationQueue.contains(signal)) {
//                regulationQueue.add(signal);
//            }
        }
        laemmerLanes.forEach(l->l.update(now));
    }

    private void updateRepresentativeDriveways(double now) {
        flowSum = 0;
        tIdle = laemmerConfig.getDesiredCycleTime();
        for (LaemmerPhase phase : laemmerPhases) {
            phase.determineRepresentativeDriveway(now);
            flowSum += phase.outflowSum;
            tIdle -= Math.max(phase.determiningLoad * laemmerConfig.getDesiredCycleTime() + DEFAULT_INTERGREEN, laemmerConfig.getMinGreenTime());
        }
        tIdle = Math.max(0, tIdle);
        
        //TODO this shouldn't be done here since laemmerLane doesn't have a representive driveway, pschade Dec 17
        laemmerLanes.forEach(l->l.determineRepresentativeDriveway(now));
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
                        System.out.println("im here!");
                    }
                }
                //always register link in case only one lane is specified (-> no LaneEnter/Leave-Events?), xy
                //moved this to next for-loop, unsure, if this is still needed, pschade Nov'17 
                this.sensorManager.registerNumberOfCarsInDistanceMonitoring(signal.getLinkId(), 0.);
                this.sensorManager.registerAverageNumberOfCarsPerSecondMonitoring(signal.getLinkId());
            }
        }
        //moved here from above, pschade Nov'17
        for (Link link : this.network.getLinks().values()) {
            this.sensorManager.registerNumberOfCarsInDistanceMonitoring(link.getId(), 0.);
            this.sensorManager.registerAverageNumberOfCarsPerSecondMonitoring(link.getId());
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
            selected = activeRequest.laemmerPhase.phase.getId().toString();
        }
        builder.append(tIdle + ";" + selected + ";" + delayCalculator.getTotalDelay() + ";");
        for (LaemmerPhase laemmerSignal : laemmerPhases) {
           //TODO implement getStepStats
           //laemmerSignal.getStepStats(builder, now);
        }
        return builder.toString();
    }

    class Request {
		/** time at which the laemmer signal is planned to show green */
        final double onsetTime;
        final LaemmerPhase laemmerPhase;

        Request(double onsetTime, LaemmerPhase laemmerPhase) {
            this.laemmerPhase = laemmerPhase;
            this.onsetTime = onsetTime;
        }

        private boolean isDue(double now) {
            return now == this.onsetTime;
        }
    }

	public SignalSystem getSystem() {
		return this.system;
	}

	public void addLaneForStabilization(LaemmerLane laemmerLane) {
		// TODO implement lane stabilization: Find best phase for stabilization on selection (e.g. with most lanes for stabilization or phase highest priority containing this lane, pschade Dec '17
		if (!needStabilization(laemmerLane)) {
			lanesForStabilization.add(laemmerLane);
		}
	}

	public boolean needStabilization(LaemmerLane laemmerLane) {
		return lanesForStabilization.contains(laemmerLane);
	}
}
