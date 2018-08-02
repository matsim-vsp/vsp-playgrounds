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
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.model.AbstractSignalController;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalController;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.qsim.interfaces.SignalGroupState;
import org.matsim.lanes.Lane;
import org.matsim.lanes.Lanes;

import com.google.inject.Provider;

import playground.dgrether.koehlerstrehlersignal.analysis.TtTotalDelay;
import signals.Analyzable;
import signals.downstreamSensor.DownstreamSensor;
import signals.laemmer.model.LaemmerConfig.Regime;
import signals.laemmer.model.LaemmerConfig.StabilizationStrategy;
import signals.laemmer.model.stabilizationStrategies.AbstractStabilizationStrategy;
import signals.laemmer.model.stabilizationStrategies.HeuristicStrategy;
import signals.laemmer.model.util.SignalCombinationBasedOnConflicts;
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

	final LaemmerConfig laemmerConfig;
	private final List<LaemmerPhase> laemmerPhases = new ArrayList<>();

	Request activeRequest = null;
	LinkSensorManager sensorManager;

	DownstreamSensor downstreamSensor;

	final Network network;
	final Lanes lanes;
	final Config config;
	final SignalsData signalsData;

	final double DEFAULT_INTERGREEN;
	double tIdle;

	double maximumSystemsOutflowSum;

	private TtTotalDelay delayCalculator;

	private ArrayList<SignalPhase> signalPhases;
	private List<LaemmerApproach> laemmerApproaches = new LinkedList<>();
	private Queue<LaemmerApproach> approachesForStabilization = new LinkedList<>();

	private LaemmerPhase regulationPhase;

	private double averageWaitingCarCount = 0.0;

	private double lastAvgCarNumUpdate = 30.0*60.0;

	private boolean isAvgQueueLengthNumWritten = false;

	private boolean debug = false;

	private AbstractStabilizationStrategy stabilisator;

	private int estNumOfPhases;

	private boolean switchSignal; //schedules an update of activeRequest if stabilization selects same phase that was already selected 
	
	private SignalCombinationBasedOnConflicts signalCombinationConflicts;


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
		this.signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		this.delayCalculator = delayCalculator;
		if (laemmerConfig.isUseDefaultIntergreenTime()) {
			DEFAULT_INTERGREEN = laemmerConfig.getDefaultIntergreenTime();
		} else {
			throw new UnsupportedOperationException("Laemmer with signal specific intergreen times is not yet implemented.");
		}
		this.downstreamSensor = downstreamSensor;
		try {
			this.stabilisator = (AbstractStabilizationStrategy) Class.forName(laemmerConfig.getStabilizationClassName()).getConstructor(FullyAdaptiveLaemmerSignalController.class, Network.class, Lanes.class).newInstance(this, network, lanes);
			if (laemmerConfig.getStabilizationStrategy().equals(StabilizationStrategy.HEURISTIC)) {
				((HeuristicStrategy) stabilisator).setSignalCombinationTool(this.signalCombinationConflicts);
			}
		} catch (Exception e) {	e.printStackTrace(); }
	}

	@Override
	public void simulationInitialized(double simStartTimeSeconds) {
		List<Double> maximumLaneOutflows = new LinkedList<>();
		laemmerApproaches = new LinkedList<>();
		this.estNumOfPhases = SignalUtils.estimateNumberOfPhases(system, network, lanes);
		this.initializeSensoring();

		for (SignalGroup sg : this.system.getSignalGroups().values()) {
			this.system.scheduleDropping(simStartTimeSeconds, sg.getId());
			//schedule dropping will only schedule a dropping but not process it intermediately 
			sg.setState(SignalGroupState.RED);
			for (Signal signal : sg.getSignals().values()) {
				if (signal.getLaneIds() == null || signal.getLaneIds().isEmpty()) {
					LaemmerApproach laemmerLane = new LaemmerApproach(network.getLinks().get(signal.getLinkId()), sg, signal, this);
					laemmerApproaches.add(laemmerLane);
					maximumLaneOutflows.add(laemmerLane.getMaxOutflow());
				} else {
					Link link = network.getLinks().get(signal.getLinkId());
					for (Id<Lane> laneId : signal.getLaneIds()) {
						LaemmerApproach laemmerLane = new LaemmerApproach(link, lanes.getLanesToLinkAssignments().get(link.getId()).getLanes().get(laneId),
								sg, signal, this);
						laemmerApproaches.add(laemmerLane);
						maximumLaneOutflows.add(laemmerLane.getMaxOutflow());
					}
				}
			}
		}
		//sum the maximum n lanes for systems outflow maximum
		maximumSystemsOutflowSum = maximumLaneOutflows.stream().sorted(Comparator.comparingDouble(Double::doubleValue).reversed()).limit(this.estNumOfPhases).collect(Collectors.summingDouble(Double::doubleValue));
		// create all possible signal combinations based on conflict data
		this.signalCombinationConflicts = new SignalCombinationBasedOnConflicts(signalsData, system, network, lanes);
		this.signalPhases = signalCombinationConflicts.createSignalCombinations();

		if (laemmerConfig.isRemoveSubPhases()) {
			this.signalPhases = SignalUtils.removeRedundantSubPhases(this.signalPhases);
			if(debug)
				System.out.println("after remove subphases: " + this.signalPhases.size());
		}

		for (SignalPhase signalPhase : signalPhases) {
			LaemmerPhase laemmerPhase = new LaemmerPhase(this, signalPhase);
			laemmerPhases.add(laemmerPhase);
		}
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
	public void updateState(double now) {
		if (laemmerConfig.getActiveRegime().equals(LaemmerConfig.Regime.COMBINED) ||
				laemmerConfig.getActiveRegime().equals(LaemmerConfig.Regime.STABILIZING)) {
			updateActiveRegulation(now);
		}
		updatePhasesAndLanes(now);
		// TODO test what happens, when I move this up to the first line of this method. should save runtime. tt, dez'17
		// note: stabilization has still to be done to increment 'a'... tt, dez'17
		// another note: if we move this up, new lanes which need to be stabilized will only be
		// added to stabilization queue after processing a new request and won't be in
		// the same order as they were added during the process. But the influence of it
		// shouldn't be that big…, pschade, Dec'17
		if(activeRequest != null && activeRequest.laemmerPhase.phase.getState().equals(SignalGroupState.GREEN)) {
			double remainingMinG = activeRequest.onsetTime + laemmerConfig.getMinGreenTime() - now;
			if (remainingMinG > 0) {
				if (debug) {
					System.out.println("Not selecting new signal, remainingMinG="+remainingMinG);
				}
				return;
			}
		}
		if ((laemmerConfig.getActiveRegime().equals(LaemmerConfig.Regime.COMBINED) ||
				laemmerConfig.getActiveRegime().equals(LaemmerConfig.Regime.STABILIZING))
				&& regulationPhase == null && approachesForStabilization.size() > 0) {
			switchSignal = true;
			regulationPhase = findBestPhaseForStabilization();
			calculateTidle(now);
			double extraTime = (approachesForStabilization.peek().getMaxOutflow() / this.maximumSystemsOutflowSum) * this.tIdle;
			approachesForStabilization.peek().extendRegulationTime(extraTime);
		}
		LaemmerPhase selection = selectSignal();
		processSelection(now, selection);
		if (isAnalysisEnabled()) {
			logQueueLengthToFile(now);
		}
	}

	/**
	 * Calculate tIdle from
	 * 	(a) lane with highest load in regulationPhase and
	 *  (b) lanes with the highest load.
	 * Number of (b) according to the estimated number of phases for this signalSystem
	 * @param now
	 */
	private void calculateTidle(double now) {
		tIdle = laemmerConfig.getDesiredCycleTime();
		//representive Lane for current selection
		LaemmerApproach representiveLane = laemmerApproaches.parallelStream()
				.filter(ll -> regulationPhase.getPhase().getGreenSignalGroups().contains(ll.getSignalGroup().getId()))
				.max(Comparator.comparingDouble(LaemmerApproach::getDeterminingLoad).reversed())
				.get();
		tIdle -= Math.max(representiveLane.getDeterminingLoad() * laemmerConfig.getDesiredCycleTime() + laemmerConfig.getDefaultIntergreenTime(), laemmerConfig.getMinGreenTime());
		//get all laemmerLanes and keep only the lanes which are not in the current phase
		if (laemmerConfig.isDetermineMaxLoadForTIdleGroupedBySignals()) {
			//implementation of grouping by values adapted from https://marcin-chwedczuk.github.io/grouping-using-java-8-streams
			Collection<Double> maxLoadFromLaneForEachSignal = laemmerApproaches.parallelStream()
					//filter by phases whos signalgroups are in current regulation phase
					.filter(ll->!regulationPhase.getPhase().getGreenSignalGroups().contains(ll.getSignalGroup().getId()))
					//grouping by signal and keeps the maximum determined load for each signal
					.collect(Collectors.groupingBy(LaemmerApproach::getSignal,
							Collectors.mapping(LaemmerApproach::getDeterminingLoad,
									//collect the max values and unbox Doubles from Optional
									Collectors.collectingAndThen(Collectors.maxBy(Double::compare), Optional::get))))
					.values();
			tIdle -= maxLoadFromLaneForEachSignal.parallelStream()
					//sort the loads desc and keep only numOfPhases-1 values
					.sorted(Comparator.reverseOrder())
					.limit(this.estNumOfPhases-1)
					//sum the parts of tIdle
					.collect(Collectors.summingDouble(determinigLoad->
					Math.max(determinigLoad * laemmerConfig.getDesiredCycleTime()+laemmerConfig.getDefaultIntergreenTime(),laemmerConfig.getMinGreenTime())
							))
					.doubleValue();
		}
		else {
			tIdle -= laemmerApproaches.parallelStream()
					//filter by phases whos signalgroups are in current regulation phase
					.filter(ll->!regulationPhase.getPhase().getGreenSignalGroups().contains(ll.getSignalGroup().getId()))
					//sort by determining load and keep only the upper half of the list of lammerlanes
					.sorted(Comparator.comparingDouble(LaemmerApproach::getDeterminingLoad).reversed())
					.limit(this.estNumOfPhases-1)
					//sum the parts of tIdle
					.collect(Collectors.summingDouble(ll->
					Math.max(ll.getDeterminingLoad() * laemmerConfig.getDesiredCycleTime()+laemmerConfig.getDefaultIntergreenTime(),laemmerConfig.getMinGreenTime())
							))
					.doubleValue();
		}
		tIdle = Math.max(0, tIdle);
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
		switchSignal = false;
		if (this.debug && approachesForStabilization.size() > 0) {
			System.out.println("regTime: "+approachesForStabilization.peek().getRegulationTime() + ", passed: "+ (now -activeRequest.onsetTime));
		}
		if (activeRequest != null && regulationPhase != null && regulationPhase.equals(activeRequest.laemmerPhase)) {
			int n = 1;
			//only end stabilization if at least minG has passed
			if (now - activeRequest.onsetTime >= laemmerConfig.getMinGreenTime()) {
				if (approachesForStabilization.peek().getLane() != null) {
					n = getNumberOfExpectedVehiclesOnLane(now, approachesForStabilization.peek().getLink().getId(), approachesForStabilization.peek().getLane().getId());
				} else {
					n = getNumberOfExpectedVehiclesOnLink(now, approachesForStabilization.peek().getLink().getId());
				} 
			}
			if (approachesForStabilization.peek().getRegulationTime() + activeRequest.onsetTime - now <= 0 || n == 0) {
				if(debug) {
					System.out.println("regulation time over or link/lane empty(n="+n+"), ending stabilization.");
				}
				endStabilization(now);
			}
		}
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
		List<LaemmerApproach> markedForRemove = new ArrayList<>(approachesForStabilization.size());
		for (LaemmerApproach ll : approachesForStabilization) {
			if (activeRequest.laemmerPhase.phase.containsGreenLane(ll.getLink().getId(), (ll.getLane() == null ? null : ll.getLane().getId()))) {
				if(ll.getRegulationTime() <= passedRegulationTime
						|| (ll.getLane() != null && getNumberOfExpectedVehiclesOnLane(now, ll.getLink().getId(), ll.getLane().getId()) == 0)
						|| (ll.getLane() == null && getNumberOfExpectedVehiclesOnLink(now, ll.getLink().getId()) == 0)) {
					//remove all Lanes from regulationQueue when their regulation time > regulationTime of regulationPhase
					markedForRemove.add(ll);
					if(debug) {
						System.out.println("removing "+ll.getLink().getId().toString()+"-"+(ll.getLane() != null ? ll.getLane().getId().toString() : "null"));
					}
				} else {
					//Subtract regulationTime of all lanes if regulation time not > regulation time of current phase
					ll.shortenRegulationTime(passedRegulationTime);
					if(debug) {
						System.out.println("shorten time for "+ll.getLink()+"-"+ll.getLane()+" by "+passedRegulationTime);
					}
				}
			}
		}
		approachesForStabilization.removeAll(markedForRemove);
		//set regulationPhase null
		regulationPhase = null;
		switchSignal = true;
	}

	private void updatePhasesAndLanes(double now) {
		if(regulationPhase == null && (laemmerConfig.getActiveRegime().equals(Regime.COMBINED) || laemmerConfig.getActiveRegime().equals(Regime.OPTIMIZING))) {
			for (LaemmerPhase phase : laemmerPhases) {
				phase.updateAbortationPenaltyAndPriorityIndex(now);
			}
		}

		if(laemmerConfig.getActiveRegime().equals(Regime.COMBINED) || laemmerConfig.getActiveRegime().equals(Regime.STABILIZING)) {
			for (LaemmerApproach l : laemmerApproaches) {
				l.calcLoadAndArrivalrate(now);
				l.updateStabilization(now);
			}
		}
	}


	private LaemmerPhase findBestPhaseForStabilization() {
		if (this.debug ) {
			System.out.println("tIdle: " + tIdle);
			System.out.print("stabilizationQueue: ");
			for (LaemmerApproach ll : approachesForStabilization) {
				System.out.print(ll.getLink().getId()+"-"+(ll.getLane() != null ? ll.getLane().getId(): "null") + "(" + ll.getRegulationTime() + "), ");
			}
			System.out.print("\n");
		}
		LaemmerPhase max = stabilisator.determinePhase(approachesForStabilization, laemmerPhases, debug);
		return max;
	}

	private LaemmerPhase selectSignal() {
		LaemmerPhase max = null;

		//selection if stabilization is needed
		if (laemmerConfig.getActiveRegime().equals(LaemmerConfig.Regime.COMBINED)
				|| laemmerConfig.getActiveRegime().equals(LaemmerConfig.Regime.STABILIZING)) {
			max = regulationPhase;
		}

		//selection for optimizing
		if ((laemmerConfig.getActiveRegime().equals(LaemmerConfig.Regime.COMBINED)
				|| laemmerConfig.getActiveRegime().equals(LaemmerConfig.Regime.OPTIMIZING)) && max == null) {
			double index = 0;
			for (LaemmerPhase phase : laemmerPhases) {
				if (phase.index > index) {
					// if downstream check enabled, only select signals that do not lead to occupied
					// links
					boolean isAllDownstramLinksEmpty = true;
					if (laemmerConfig.isCheckDownstream()) {
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
		/* quit the active request, when the next selection is different from the current (activeRequest)
		 * or, when the next selection is null
		 */
		if (activeRequest != null && ( selection == null || (!selection.equals(activeRequest.laemmerPhase) || switchSignal))) {
			// only schedule a dropping when the signals are already green, tthunig(?)
			// Since signals can show green during the intergreen-time when they are in two stabilization phases, they need to be dropped if their queue
			// is emptied before the intergreen time passed. So its necessary to schedule a drop always. pschade, Feb'18
			//        	if(activeRequest.onsetTime < now) {
			for (Id<SignalGroup> sg : activeRequest.laemmerPhase.phase.getGreenSignalGroups()) {
				// if there is an selection drop only the signals which not included in current (new) selection
				if (selection == null || !(selection.equals(regulationPhase) && selection.phase.getGreenSignalGroups().contains(sg))) {
					this.system.scheduleDropping(now, sg);
				} else if (debug) {
					System.err.println("not dropping "+sg.toString()+", "+(selection==null?"selection=null, ":"")+((selection == regulationPhase && selection.phase.getGreenSignalGroups().contains(sg))?"is in next regulation phase":""));
				}
			}
			//        	}
			activeRequest = null;
		}

		if (activeRequest == null && selection != null) {
			activeRequest = new Request(now + DEFAULT_INTERGREEN, selection);
		}

		if (activeRequest != null && activeRequest.isDue(now)) {
			//shorten regulationTime by intergreenTime if signal was green during intergreenTime
			if (laemmerConfig.getShortenStabilizationAfterIntergreenTime() && activeRequest.laemmerPhase.equals(regulationPhase)) {
				List<LaemmerApproach> markForRemove = new LinkedList<>();
				for (LaemmerApproach l : approachesForStabilization) {
					if (l.getSignalGroup().getState().equals(SignalGroupState.GREEN)) {
						if (laemmerConfig.isUseDefaultIntergreenTime()) {
							//regulation time here will be shorten only, approaches will not removed. since they included in the next phase, they will be removed after minG
							l.shortenRegulationTime(laemmerConfig.getDefaultIntergreenTime());
						} else {
							throw new IllegalStateException("Shorten stabilization time with specific intergreen times is not yet implemented.");
						}
					}
				}
				approachesForStabilization.removeAll(markForRemove);
			}

			for (Id<SignalGroup> sg : activeRequest.laemmerPhase.phase.getGreenSignalGroups()) {
				this.system.scheduleOnset(now, sg);
			}
		}
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

	@Override
	public boolean isAnalysisEnabled() {
		return this.laemmerConfig.isAnalysisEnabled();
	}

	@Override
	public String getStatFields() {

		StringBuilder builder = new StringBuilder();
		builder.append("T_idle;selected;total delay;numOfLanesNeedsStabilize;stabilizationQueue;");
		for (LaemmerPhase laemmerPhase : laemmerPhases) {
			laemmerPhase.getStatFields(builder);
		}
		for (LaemmerApproach laemmerLane : laemmerApproaches) {
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
		builder.append(tIdle + ";" + selected + ";" + delayCalculator.getTotalDelay() + ";"+this.approachesForStabilization.size()+";");
		for (LaemmerApproach ll : approachesForStabilization) {
			builder.append(ll.getLink().getId());
			if (ll.getLane() != null) {
				builder.append("-");
				builder.append(ll.getLane().getId());
			}
			builder.append(" ");
		}
		builder.append(";");
		for (LaemmerPhase laemmerPhase : laemmerPhases) {
			laemmerPhase.getStepStats(builder, now);
		}

		for (LaemmerApproach laemmerLane : laemmerApproaches) {
			laemmerLane.getStepStats(builder, now);
		}

		return builder.toString();
	}

	public SignalSystem getSystem() {
		return this.system;
	}

	public void addLaneForStabilization(LaemmerApproach laemmerLane) {
		if (!needStabilization(laemmerLane)) {
			approachesForStabilization.add(laemmerLane);
		}
	}

	public boolean needStabilization(LaemmerApproach laemmerLane) {
		return approachesForStabilization.contains(laemmerLane);
	}

	public LaemmerConfig getLaemmerConfig() {
		return this.laemmerConfig;
	}

	public boolean getDebug() {
		return debug;
	}

	//helper functions
	private void logQueueLengthToFile(double now) {
		double logStartTime = 30.0*60.0; //for illustrative 
		//    	double logStartTime = 16.5*3600.0; //for CB
		double logEndTime = 90.0*60.0; //for illustrative
		//    	double logEndTime = 17.5*3600.0; //for CB with football
		double currentQueueLengthSum = 0.0;
		if (now > logStartTime && now <= logEndTime) {
			for (LaemmerApproach l : laemmerApproaches) {
				if (l.getLane() == null) {
					currentQueueLengthSum += this.getNumberOfExpectedVehiclesOnLink(now, l.getLink().getId());
				} else {
					currentQueueLengthSum += this.getNumberOfExpectedVehiclesOnLane(now, l.getLink().getId(), l.getLane().getId());
				}
			}
			this.averageWaitingCarCount *= (lastAvgCarNumUpdate-logStartTime+1.0);
			this.averageWaitingCarCount	+= currentQueueLengthSum;
			this.averageWaitingCarCount /= (now - logStartTime+1.0);
			this.lastAvgCarNumUpdate = now; 
		} else if (now > logEndTime && !this.isAvgQueueLengthNumWritten) {
			StringBuilder filename = new StringBuilder();
			filename.append("avgQueueLength_");
			filename.append("fullyAdaptive_");
			filename.append(this.laemmerConfig.getActiveRegime()+"_");
			filename.append(this.laemmerConfig.getActiveStabilizationStrategy()+"_");
			filename.append("signalSystem-"+this.system.getId());
			filename.append(".csv");
			try {
				if (Files.notExists(Paths.get(this.config.controler().getOutputDirectory().concat("/../"+filename)))){
					Files.createFile(Paths.get(this.config.controler().getOutputDirectory().concat("/../"+filename)));
				}
				Files.write(Paths.get(this.config.controler().getOutputDirectory().concat("/../"+filename)), Double.toString(averageWaitingCarCount).concat("\n").getBytes(), StandardOpenOption.APPEND);
				this.isAvgQueueLengthNumWritten  = true;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/** helper class for scheduling a new request 
	 * @author nkuehnel **/
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

}
