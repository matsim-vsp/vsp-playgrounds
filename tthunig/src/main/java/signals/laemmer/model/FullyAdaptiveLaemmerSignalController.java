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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;

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

import com.google.inject.Provider;

import playground.dgrether.koehlerstrehlersignal.analysis.TtTotalDelay;
import signals.Analyzable;
import signals.downstreamSensor.DownstreamSensor;
import signals.laemmer.model.util.SignalUtils;
import signals.sensor.LinkSensorManager;


/**
 * @author dgrether
 * @author tthunig
 * @author nkuehnel
 * @author pschade
 */

public class FullyAdaptiveLaemmerSignalController extends AbstractSignalController implements SignalController, Analyzable {

    public static final String IDENTIFIER = "FullyAdaptiveLaemmerSignalController";

//    Queue<LaemmerPhase> regulationQueue = new LinkedList<>();

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
	private Queue<LaemmerLane> lanesForStabilization = new LinkedList<>();

	private LaemmerPhase regulationPhase;

	private double averageWaitingCarCount = 0.0;

	private double lastAvgCarNumUpdate = 30.0*60.0;

	private boolean isAvgQueueLengthNumWritten = false;

	private boolean debug = false;


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
            return new FullyAdaptiveLaemmerSignalController(laemmerConfig, sensorManager, scenario, delayCalculator, downstreamSensor);
        }
    }


    private FullyAdaptiveLaemmerSignalController(LaemmerConfig laemmerConfig, LinkSensorManager sensorManager, Scenario scenario, TtTotalDelay delayCalculator, DownstreamSensor downstreamSensor) {
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
        if(system.getId().equals(Id.create("23", SignalSystem.class))){
        	this.debug = true;
        }
    	laemmerLanes = new LinkedList<>();

    	this.initializeSensoring();

    	for (SignalGroup sg : this.system.getSignalGroups().values()) {
	    	for (Signal signal : sg.getSignals().values()) {
	    		if (signal.getLaneIds() == null || signal.getLaneIds().isEmpty()) {
	    			LaemmerLane laemmerLane = new LaemmerLane(network.getLinks().get(signal.getLinkId()), sg, signal, this);
	    			laemmerLanes.add(laemmerLane);
        			flowSum += laemmerLane.getMaxOutflow();
	    		} else {
	    			Link link = network.getLinks().get(signal.getLinkId());
	    			for (Id<Lane> laneId : signal.getLaneIds()) {
	    				LaemmerLane laemmerLane = new LaemmerLane(link, lanes.getLanesToLinkAssignments().get(link.getId()).getLanes().get(laneId),
	    						sg, signal, this);
	    				laemmerLanes.add(laemmerLane);
	    				flowSum += laemmerLane.getMaxOutflow();
	    			}
	    		}
	    	}
    	}

        //Phasen kombinatorisch erstellen
    	this.signalPhases = SignalUtils.createSignalPhasesFromSignalGroups(system, network, lanes);
        
    	if (laemmerConfig.isRemoveSubPhases()) {
    		this.signalPhases = SignalUtils.removeSubPhases(this.signalPhases);
    		System.out.println("after remove subphases: " + this.signalPhases.size());
    	}
    	
        for (SignalPhase signalPhase : signalPhases) {
        	LaemmerPhase laemmerPhase = new LaemmerPhase(this, signalPhase);
        	for (Id<SignalGroup> group : signalPhase.getGreenSignalGroups()) {
        		this.system.scheduleDropping(simStartTimeSeconds, group);
        	}
        	laemmerPhases.add(laemmerPhase);
		}
    }

    
    @Override
    public void updateState(double now) {
        updateRepresentativeDriveways(now);
        if (laemmerConfig.getActiveRegime().equals(LaemmerConfig.Regime.COMBINED) ||
        		laemmerConfig.getActiveRegime().equals(LaemmerConfig.Regime.STABILIZING)) {
            updateActiveRegulation(now);
        }
        updatePhasesAndLanes(now);
        if ((laemmerConfig.getActiveRegime().equals(LaemmerConfig.Regime.COMBINED) ||
        		laemmerConfig.getActiveRegime().equals(LaemmerConfig.Regime.STABILIZING))
        				&& regulationPhase == null && lanesForStabilization.size() > 0)
        	regulationPhase = findBestPhaseForStabilization();
        
        // TODO test what happens, when I move this up to the first line of this method. should save runtime. tt, dez'17
        // note: stabilization has still to be done to increment 'a'... tt, dez'17
		// another note: if we move this up, new lanes which need to be stabilized will only be
		// added to stabilization queue after processing a new request and won't be in
		// the same order as they were added during the process. But the influence of it
		// shouldn't be that big…, pschade, Dec'17
        if(activeRequest != null && activeRequest.laemmerPhase.phase.getState().equals(SignalGroupState.GREEN)) {
            double remainingMinG = activeRequest.onsetTime + laemmerConfig.getMinGreenTime() - now;
            if (remainingMinG > 0) {
            	//TODO	remove debug output
            	//System.err.println("Not selecting new signal, remainingMinG="+remainingMinG);
                return;
            }
        }
        LaemmerPhase selection = selectSignal();
        processSelection(now, selection);
        logQueueLengthToFile(now);
    }

    private void logQueueLengthToFile(double now) {
		double currentQueueLengthSum = 0.0;
    	if (now > 30.0*60.0 && now <= 90.0*60.0) {
    		for (LaemmerLane l : laemmerLanes) {
    			currentQueueLengthSum += this.getNumberOfExpectedVehiclesOnLane(now, l.getLink().getId(), l.getLane().getId());
    		}
    		this.averageWaitingCarCount *= (lastAvgCarNumUpdate-30.0*60.0+1.0); 
    		this.averageWaitingCarCount	+= currentQueueLengthSum;
    		this.averageWaitingCarCount /= (now - 30.0*60.0+1.0);
    		this.lastAvgCarNumUpdate = now; 
		} else if (now > 90.0*60.0 && !this.isAvgQueueLengthNumWritten) {
		    try {
		    	if (Files.notExists(Paths.get(this.config.controler().getOutputDirectory().concat("/../avgQueueLength.csv")))){
		    		Files.createFile(Paths.get(this.config.controler().getOutputDirectory().concat("/../avgQueueLength.csv")));
		    	}
				Files.write(Paths.get(this.config.controler().getOutputDirectory().concat("/../avgQueueLength.csv")), Double.toString(averageWaitingCarCount).concat("\n").getBytes(), StandardOpenOption.APPEND);
				this.isAvgQueueLengthNumWritten  = true;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
    public boolean isAnalysisEnabled() {
    	return this.laemmerConfig.isAnalysisEnabled();
    }
    
    /**
	 * Method will check if (a) currently a request is processed, (b) there is
	 * currently a need for stabilization and (c) the current request equals the
	 * peek of the regulation queue. <br>
	 * If these prerequisites are fulfilled, the current phase will be removed from
	 * the regulation queue, if the regulationTime is already passed or there aren't
	 * any more cars waiting to be processed.
	 * 
	 * @param now
	 */
    private void updateActiveRegulation(double now) {
    	if (this.debug && lanesForStabilization.size() > 0) {
    		System.out.println("regTime: "+lanesForStabilization.peek().getRegulationTime() + ", passed: "+ (now -activeRequest.onsetTime));
    	}
        if (activeRequest != null && regulationPhase != null && regulationPhase.equals(activeRequest.laemmerPhase)) {
            int n;
            if (lanesForStabilization.peek().getLane() != null) {
                n = getNumberOfExpectedVehiclesOnLane(now, lanesForStabilization.peek().getLink().getId(), lanesForStabilization.peek().getLane().getId());
            } else {
                n = getNumberOfExpectedVehiclesOnLink(now, lanesForStabilization.peek().getLink().getId());
            }
            if (lanesForStabilization.peek().getRegulationTime() + activeRequest.onsetTime - now <= 0 || n == 0) {
                //TODO remove debug output
            	if(debug) {
            		System.out.println("regulation time over or link/lane empty, ending stabilization.");
            	}
           		endStabilization(now);
            }
        }
    }

    private LaemmerPhase selectSignal() {
        LaemmerPhase max = null;
        
        //selection if stabilization is needed
        if (laemmerConfig.getActiveRegime().equals(LaemmerConfig.Regime.COMBINED)
        		|| laemmerConfig.getActiveRegime().equals(LaemmerConfig.Regime.STABILIZING)) {
        	max = regulationPhase;
        }

        //selection for optimizing
		if (laemmerConfig.getActiveRegime().equals(LaemmerConfig.Regime.COMBINED)
				|| laemmerConfig.getActiveRegime().equals(LaemmerConfig.Regime.OPTIMIZING) && max == null) {
			double index = 0;
			for (LaemmerPhase phase : laemmerPhases) {
				if (phase.index > index) {
					// if downstream check enabled, only select signals that do not lead to occupied
					// links
					boolean isAllDownstramLinksEmpty = true;
					if (!laemmerConfig.isCheckDownstream()) {
						for (Id<SignalGroup> sg : phase.phase.getGreenSignalGroups()) {
							isAllDownstramLinksEmpty &= downstreamSensor.allDownstreamLinksEmpty(system.getId(), sg);
						}
					}
					if (isAllDownstramLinksEmpty) {
						max = phase;
						index = phase.index;
					}
				}
			}
		}
		return max;
	}

    private void processSelection(double now, LaemmerPhase selection) {
        if (activeRequest != null && ( selection == null || !selection.equals(activeRequest.laemmerPhase))) {
        	/* quit the active request, when the next selection (max) is different from the current (activeRequest)
    		 * or, when the next selection (max) is null
    		 */
        	if(activeRequest.onsetTime < now) {
    			// do not schedule a dropping when the signal does not yet show green
        		for (Id<SignalGroup> sg : activeRequest.laemmerPhase.phase.getGreenSignalGroups()) {
        			// if there is an selection drop only the signals which not included in current (new) selection
        			if (selection == null || selection != null && !selection.phase.getGreenSignalGroups().contains(sg)) {
        				this.system.scheduleDropping(now, sg);
        			}
        		}
        	} else {
				// if selection changes before the former scheduled selection showed green the
				// signals which were showing green before the former selection and still
				// showing green, because they were included in the former selection has to be
				// dropped
         		for (Id<SignalGroup> sg : activeRequest.laemmerPhase.phase.getGreenSignalGroups()) {
        			if (selection == null || selection != null && !selection.phase.getGreenSignalGroups().contains(sg) && system.getSignalGroups().get(sg).getState().equals(SignalGroupState.GREEN)) {
        				this.system.scheduleDropping(now, sg);
        			}
         		}
        	}
        	
            activeRequest = null;
        }

        if (activeRequest == null && selection != null) {
            activeRequest = new Request(now + DEFAULT_INTERGREEN, selection);
        }

        if (activeRequest != null && activeRequest.isDue(now)) {
           	for (Id<SignalGroup> sg : activeRequest.laemmerPhase.phase.getGreenSignalGroups()) {
           		this.system.scheduleOnset(now, sg);
           	}
        }
    }
    
    private LaemmerPhase findBestPhaseForStabilization() {
    	if (this.debug ) {
			System.out.println("tIdle: " + tIdle);
			System.out.print("stabilizationQueue: ");
			for (LaemmerLane ll : lanesForStabilization) {
				System.out.print(ll.getLane().getId() + "(" + ll.getRegulationTime() + "), ");
			}
			System.out.print("\n");
		}
		LaemmerPhase max = null;
    	 if (lanesForStabilization.size() > 0) {
    		 if (laemmerConfig.getActiveStabilizationStrategy().equals(LaemmerConfig.StabilizationStrategy.HEURISTIC)) {
					SignalPhase generatedPhase = new SignalPhase();
					generatedPhase.addGreenSignalGroup(lanesForStabilization.peek().getSignalGroup());
					if (lanesForStabilization.size() > 1) {
						stabilisationLaneLoop:
						for (LaemmerLane laemmerLane : ((List<LaemmerLane>) lanesForStabilization).subList(1, lanesForStabilization.size())) {
							boolean addLane = true;
							for (Id<SignalGroup> presentSignalGroup : generatedPhase.getGreenSignalGroups()) {
								if (!SignalUtils.isConflictFreeCombination(this.system.getSignalGroups().get(presentSignalGroup), laemmerLane.getSignalGroup(), network, lanes)) {
									addLane = false;
									continue stabilisationLaneLoop;
								}
//								for (Signal presentSignal : system.getSignalGroups().get(presentSignalGroup)
//										.getSignals().values()) {
//									for (Id<Lane> presentSignalsLaneId : presentSignal.getLaneIds()) {
//										Conflicts linkConflicts = (Conflicts) laemmerLane.getLink().getAttributes()
//												.getAttribute("conflicts");
//										Conflicts laneConflicts = (Conflicts) laemmerLane.getLane().getAttributes()
//												.getAttribute("conflicts");
//										if ((linkConflicts != null
//												&& (linkConflicts.hasConflict(presentSignal.getLinkId())
//														|| linkConflicts.hasConflict(presentSignal.getLinkId(),
//																presentSignalsLaneId)))
//												|| (laneConflicts != null
//														&& (laneConflicts.hasConflict(presentSignal.getLinkId())
//																|| laneConflicts.hasConflict(presentSignal.getLinkId(),
//																		presentSignalsLaneId)))) {
//											addLane = false;
//											break;
//										}
//									}
//								}
							}
							if (addLane) {
								generatedPhase.addGreenSignalGroup(laemmerLane.getSignalGroup());
							}
						}
					}
					//add non-stabilizing signal groups if non-conflicting
					//TODO add option to add nonPriorityAllowedConflicts
					allSignalGroups: for (SignalGroup systemSignalGroup : system.getSignalGroups().values()) {
						boolean addSg = true;
						for (Id<SignalGroup> presentSignalGroupId : generatedPhase.getGreenSignalGroups()) {
							if (systemSignalGroup.getId().equals(presentSignalGroupId))
								continue allSignalGroups;
							if(!SignalUtils.isConflictFreeCombination(systemSignalGroup, this.system.getSignalGroups().get(presentSignalGroupId), network, lanes)){
								addSg=false;
								continue allSignalGroups;
							}
//							should be replaced by PermutateSignalGroups.isConflictFreeCombination() above 
//							for (Signal systemsSignal : systemSignalGroup.getSignals().values()) {
//								for (Signal presentSignal : system.getSignalGroups().get(presentSignalGroupId).getSignals().values()) {
//									if (!(systemsSignal.getLaneIds() == null) && !systemsSignal.getLaneIds().isEmpty()) {
//										for (Id<Lane> systemsLane : systemsSignal.getLaneIds()) {
//											for (Id<Lane> presentLane : presentSignal.getLaneIds()) {
//												Conflicts systemsConflicts = (Conflicts) lanes.getLanesToLinkAssignments().get(systemsSignal.getLinkId()).getLanes()
//														.get(systemsLane).getAttributes().getAttribute("conflicts");
//												Conflicts presentConflicts = (Conflicts) lanes.getLanesToLinkAssignments().get(presentSignal.getLinkId()).getLanes()
//														.get(presentLane).getAttributes().getAttribute("conflicts");
//												if (systemsConflicts != null
//														&& (systemsConflicts.hasConflict(presentSignal.getLinkId(), presentLane)
//																|| systemsConflicts.hasAllowedConflictWithPriorityAgainst(presentSignal.getLinkId(), presentLane))
//														|| presentConflicts != null && (presentConflicts.hasConflict(systemsSignal.getLinkId(), systemsLane)
//																|| presentConflicts.hasAllowedConflictWithNonPriorityAgainst(systemsSignal.getLinkId(), systemsLane))) {
//													addSg = false;
//													break allSignalGroups;
//												}
//											}
//										} 
//									}
//								}
//							}
						}
						if(addSg)
							generatedPhase.addGreenSignalGroup(systemSignalGroup);
					}
					if (debug) {
						//TODO remove debug output
						System.out.print("generated Phase: ");
						for (Id<SignalGroup> sg : generatedPhase.getGreenSignalGroups()) {
							System.out.print(sg + ", ");
						}
						System.out.print("\n");
					}
					return new LaemmerPhase(this, generatedPhase);
				} else 	if (laemmerConfig.getActiveStabilizationStrategy().equals(LaemmerConfig.StabilizationStrategy.USE_MAX_LANECOUNT)) {
					Map<LaemmerPhase, java.lang.Integer> stabilizationCandidatePhases = new HashMap<>();			
//					Stream<LaemmerPhase> candidatePhases = laemmerPhases.stream().sequential()
//							.filter(e -> e.phase.getGreenLanes().contains(stabilizeLane));
					
					List<LaemmerPhase> candidatePhases = new LinkedList<>();
					for (LaemmerPhase laemmerPhase : laemmerPhases) {
						if (laemmerPhase.phase.containsGreenLane(lanesForStabilization.peek().getLink().getId(), lanesForStabilization.peek().getLane().getId())) {
							candidatePhases.add(laemmerPhase);
						}
					}
					//calculate scores of each phase and memorize the highest score
					//TODO Decide what to do with two phases with equal score, pschade Jan'18
					int maxSelectionScore = 0;
					for (LaemmerPhase candPhase : candidatePhases) {
						//Phase get one scorepoint for each lane they set green
						int selectionScore = candPhase.phase.getNumberOfGreenLanes();
						for (Entry<Id<Link>, List<Id<Lane>>>  lanesToLink : candPhase.phase.getGreenLanesToLinks().entrySet())
							for (LaemmerLane stabilizationLaneCandidate : lanesForStabilization) {
								//TODO probabry nullcheck for lanesToLink.getValue() is needed, but unclear, what to do, is stabilizationLaneCandidate is not null but signalgroups lanes are
								if (stabilizationLaneCandidate.getLink().getId().equals(lanesToLink.getKey()) &&
										(stabilizationLaneCandidate.getLane() == null || lanesToLink.getValue().contains(stabilizationLaneCandidate.getLane().getId())))
									/*
									 * for lanes which can stabilized phases get 1000 scorepoints (1 above for
									 * containing the lane, 999 here. So it's always better to containing a lane
									 * which has to be stabilized, or an additional lane which should be stabilized,
									 * than containing any (realistic) number of (green signaled) lanes which should
									 * not. Only if two competing phases containing the same number of lanes the
									 * lane with more lanes in total should be selected. Since above the lanes are
									 * filtered to only that lanes containing the peek of stabilization queue, it
									 * isn't needed to add 999 scorepoints for it. Alternatively, the stabilization
									 * lanes can be ranked according to their position in the queue, so that its
									 * always better to select the phase containing a higher positioning lane,
									 * regardless of the number of lower-position lanes which also could be
									 * stabilized in a competing phase. pschade, Dec'17
									 */
									selectionScore += 999;
							}
						stabilizationCandidatePhases.put(candPhase, new java.lang.Integer(selectionScore));
						if (selectionScore > maxSelectionScore) {
							max = candPhase;
							maxSelectionScore = selectionScore;
						}
					}
				}  else if (laemmerConfig.getActiveStabilizationStrategy().equals(LaemmerConfig.StabilizationStrategy.COMBINE_SIMILAR_REGULATIONTIME)) {
				double minRegulationTimeDifference = Double.POSITIVE_INFINITY;
//				Stream<LaemmerPhase> candidatePhases = laemmerPhases.stream().sequential()
//						.filter(e -> e.phase.getGreenLanes().contains(peekStabilizeLane));
				
				List<LaemmerPhase> candidatePhases = new LinkedList<>();
				for (LaemmerPhase laemmerPhase : laemmerPhases) {
					if (laemmerPhase.phase.containsGreenLane(lanesForStabilization.peek().getLink().getId(), lanesForStabilization.peek().getLane().getId())) {
						candidatePhases.add(laemmerPhase);
					}
				}
				for (LaemmerPhase candPhase : candidatePhases) {
					//Phase get one scorepoint for each lane they set green
					double regulationTimeDifferenceSum = 0;
					int laneCount = 0;
					for (Entry<Id<Link>, List<Id<Lane>>> laneToLink : candPhase.phase.getGreenLanesToLinks().entrySet())
						for (LaemmerLane stabilizationLaneCandidate : lanesForStabilization) {
							if (stabilizationLaneCandidate.equals(lanesForStabilization.peek())) {
								continue;
							}
							if (stabilizationLaneCandidate.getLink().getId().equals(laneToLink.getKey()) &&
									(stabilizationLaneCandidate.getLane() == null || laneToLink.getValue().contains(stabilizationLaneCandidate.getLane().getId()) )) {
								regulationTimeDifferenceSum += Math.abs(lanesForStabilization.peek().getRegulationTime()-stabilizationLaneCandidate.getRegulationTime());
								laneCount++;
							}
						}
					//determine the phase with the lowest average divergence
					if (regulationTimeDifferenceSum/laneCount < minRegulationTimeDifference) {
						minRegulationTimeDifference = regulationTimeDifferenceSum/laneCount;
						max = candPhase;
					}
				}
			}
         }
    	 return max;
    }
    
    /**
	 * removes all successfully stabilized lanes from stabilization queue and
	 * shorten the regulation time of lanes, which are stabilized alongside the peek
	 * of the stabilization queue but still have pending stabilization time left.
	 * Also clears the selected/generated regulation phase.
	 * 
	 * @param now
	 */
    private void endStabilization(double now) {
    	double passedRegulationTime = Math.max(now - activeRequest.onsetTime, 0.0);
    	List<LaemmerLane> markedForRemove = new ArrayList<>(lanesForStabilization.size());
    	for (LaemmerLane ll : lanesForStabilization) {
    		if (activeRequest.laemmerPhase.phase.containsGreenLane(ll.getLink().getId(), (ll.getLane() == null ? null : ll.getLane().getId()))) {
    			if(ll.getRegulationTime() <= passedRegulationTime
    				|| (ll.getLane() != null && getNumberOfExpectedVehiclesOnLane(now, ll.getLink().getId(), ll.getLane().getId()) == 0)
    				|| (ll.getLane() == null && getNumberOfExpectedVehiclesOnLink(now, ll.getLink().getId()) == 0)) {
    				//remove all Lanes from regulationQueue when their regulation time > regulationTime of regulationPhase
    				markedForRemove.add(ll);
    				if(debug) {
    					System.out.println("removing "+ll.getLink()+"-"+ll.getLane());
    				}
    			} else {
    				//substract regulationTime of all lanes if regulation time not > regulation time of current phase
    				ll.shortenRegulationTime(passedRegulationTime);
    				if(debug) {
    					System.out.println("shorten time for "+ll.getLink()+"-"+ll.getLane()+" by "+passedRegulationTime);
    				}
    			}
    		}
    	}
    	lanesForStabilization.removeAll(markedForRemove);
    	//set regulationPhase null
    	regulationPhase = null;
    }

    private void updatePhasesAndLanes(double now) {
        for (LaemmerPhase phase : laemmerPhases) {
            phase.updateAbortationPenaltyAndPriorityIndex(now);
        }
        for (LaemmerLane l : laemmerLanes) {
        	l.updateStabilizationAndAddToQueueIfNeeded(now);
        }
    }

    //TODO change name of method, probably "prepareStabilization", pschade Jan'18
    private void updateRepresentativeDriveways(double now) {
        tIdle = laemmerConfig.getDesiredCycleTime();
        
        for (LaemmerLane l : laemmerLanes) {	
        	l.calcLoadAndArrivalrate(now);
        	tIdle -= Math.max(l.getDeterminingLoad() * laemmerConfig.getDesiredCycleTime() + DEFAULT_INTERGREEN, laemmerConfig.getMinGreenTime());
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
                        this.sensorManager.registerAverageNumberOfCarsPerSecondMonitoringOnLane(signal.getLinkId(), laneId, this.laemmerConfig.getLookBackTime(), this.laemmerConfig.getTimeBucketSize());
                    }
                }
                //always register link in case only one lane is specified (-> no LaneEnter/Leave-Events?), xy
                //moved this to next for-loop, unsure, if this is still needed, pschade Nov'17 
                this.sensorManager.registerNumberOfCarsInDistanceMonitoring(signal.getLinkId(), 0.);
                this.sensorManager.registerAverageNumberOfCarsPerSecondMonitoring(signal.getLinkId(), this.laemmerConfig.getLookBackTime(), this.laemmerConfig.getTimeBucketSize());
            }
        }
        //moved here from above, pschade Nov'17
        for (Link link : this.network.getLinks().values()) {
            this.sensorManager.registerNumberOfCarsInDistanceMonitoring(link.getId(), 0.);
            this.sensorManager.registerAverageNumberOfCarsPerSecondMonitoring(link.getId(), this.laemmerConfig.getLookBackTime(), this.laemmerConfig.getTimeBucketSize());
        }
        if (laemmerConfig.isCheckDownstream()){
			downstreamSensor.registerDownstreamSensors(system);
        }
    }

    @Override
    public String getStatFields() {

        StringBuilder builder = new StringBuilder();
        builder.append("T_idle;selected;total delay;numOfLanesNeedsStabilize;");
        for (LaemmerPhase laemmerPhase : laemmerPhases) {
            laemmerPhase.getStatFields(builder);
        }
        for (LaemmerLane laemmerLane : laemmerLanes) {
        	laemmerLane.getStatFields(builder);
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
        builder.append(tIdle + ";" + selected + ";" + delayCalculator.getTotalDelay() + ";"+this.lanesForStabilization.size()+";");
        for (LaemmerPhase laemmerPhase : laemmerPhases) {
           //TODO test implementation of getStepStats
           laemmerPhase.getStepStats(builder, now);
        }
        
        for (LaemmerLane laemmerLane : laemmerLanes) {
        	laemmerLane.getStepStats(builder, now);
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
		if (!needStabilization(laemmerLane)) {
			lanesForStabilization.add(laemmerLane);
		}
	}

	public boolean needStabilization(LaemmerLane laemmerLane) {
		return lanesForStabilization.contains(laemmerLane);
	}

	@Deprecated
	//Lane should not be allowed to remove itself from stabilization. Method should be private or better removed. pschade, Jan'18
	public void removeLaneForStabilization(LaemmerLane laemmerLane) {
		lanesForStabilization.remove(laemmerLane);
	}
	
	public LaemmerConfig getLaemmerConfig() {
		return this.laemmerConfig;
	}
}
