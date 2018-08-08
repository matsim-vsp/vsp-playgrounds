package signals.laemmer;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.lanes.Lane;

/**
 * 
 * @author pschade
 */
public class LaemmerPhase {

	private final FullyAdaptiveLaemmerSignalController fullyAdaptiveLaemmerSignalController;

	SignalPhase phase;

	double index = 0;
	private double abortionPenalty = 0;

	private double intergreenTime_a;
	private double regulationTime = 0;

	Id<Lane> determiningLane;
	Id<Link> determiningLink;
	double determiningLoad;
	double outflowSum;

	public LaemmerPhase(FullyAdaptiveLaemmerSignalController fullyAdaptiveLaemmerSignalController, SignalPhase signalPhase) {
		this.fullyAdaptiveLaemmerSignalController = fullyAdaptiveLaemmerSignalController;
		this.phase = signalPhase;
		this.intergreenTime_a = this.fullyAdaptiveLaemmerSignalController.DEFAULT_INTERGREEN;
	}


	void updateAbortationPenaltyAndPriorityIndex(double now) {
		if (this.fullyAdaptiveLaemmerSignalController.laemmerConfig.getActiveRegime().equals(LaemmerConfig.Regime.OPTIMIZING) ||
				this.fullyAdaptiveLaemmerSignalController.laemmerConfig.getActiveRegime().equals(LaemmerConfig.Regime.COMBINED)) {
			updateAbortionPenalty(now);
			calculatePriorityIndex(now);
		}        
	}

	private void updateAbortionPenalty(double now) {
		this.abortionPenalty = 0;
		if (this.fullyAdaptiveLaemmerSignalController.activeRequest != null && this.equals(this.fullyAdaptiveLaemmerSignalController.activeRequest.laemmerPhase)) {
			double waitingTimeSum = 0;
			double remainingInBetweenTime = Math.max(this.fullyAdaptiveLaemmerSignalController.activeRequest.onsetTime - now, 0);
			for (double i = remainingInBetweenTime; i < this.fullyAdaptiveLaemmerSignalController.DEFAULT_INTERGREEN; i++) {
				for (Id<SignalGroup> signalGroup : phase.getGreenSignalGroups()) {
					for (Signal signal : fullyAdaptiveLaemmerSignalController.getSystem().getSignalGroups().get(signalGroup).getSignals().values()) {
						if (signal.getLaneIds() != null && !signal.getLaneIds().isEmpty()) {
							for (Id<Lane> laneId : signal.getLaneIds()) {
								waitingTimeSum += this.fullyAdaptiveLaemmerSignalController
										.getNumberOfExpectedVehiclesOnLane(now + i, signal.getLinkId(), laneId);
							}
						} else {
							waitingTimeSum += this.fullyAdaptiveLaemmerSignalController.getNumberOfExpectedVehiclesOnLink(now + i,
									signal.getLinkId());
						}
					} 
				}
			}
			double n = 0;
			for (Id<SignalGroup> signalGroup : phase.getGreenSignalGroups()) {
				for (Signal signal : fullyAdaptiveLaemmerSignalController.getSystem().getSignalGroups().get(signalGroup).getSignals().values()) {
					if (signal.getLaneIds() != null && !signal.getLaneIds().isEmpty()) {
						for (Id<Lane> laneId : signal.getLaneIds()) {
							n += this.fullyAdaptiveLaemmerSignalController.getNumberOfExpectedVehiclesOnLane(
									now + this.fullyAdaptiveLaemmerSignalController.DEFAULT_INTERGREEN, signal.getLinkId(), laneId);
						}
					} else {
						n += this.fullyAdaptiveLaemmerSignalController.getNumberOfExpectedVehiclesOnLink(
								now + this.fullyAdaptiveLaemmerSignalController.DEFAULT_INTERGREEN, signal.getLinkId());
					}
				} 
			}
			if (n > 0) {
				this.abortionPenalty += waitingTimeSum / n;
			}
		}
	}

	private void calculatePriorityIndex(double now) {
		this.index = 0;
		if (this.fullyAdaptiveLaemmerSignalController.activeRequest != null && this.fullyAdaptiveLaemmerSignalController.activeRequest.laemmerPhase.equals(this)) {
			double remainingInBetweenTime = Math.max(this.fullyAdaptiveLaemmerSignalController.activeRequest.onsetTime - now, 0);
			double remainingMinG = Math.max(this.fullyAdaptiveLaemmerSignalController.activeRequest.onsetTime - now + this.fullyAdaptiveLaemmerSignalController.laemmerConfig.getMinGreenTime() - remainingInBetweenTime, 0);
			for (double i = remainingInBetweenTime; i <= this.fullyAdaptiveLaemmerSignalController.DEFAULT_INTERGREEN; i++) {
				double nExpected = 0;
				double reqGreenTime = remainingMinG;
				for (Id<SignalGroup> signalGroup : phase.getGreenSignalGroups()) {
					for (Signal signal : fullyAdaptiveLaemmerSignalController.getSystem().getSignalGroups().get(signalGroup).getSignals().values()) {
						if (signal.getLaneIds() != null && !signal.getLaneIds().isEmpty()) {
							for (Id<Lane> laneId : signal.getLaneIds()) {
								double nTemp = this.fullyAdaptiveLaemmerSignalController.getNumberOfExpectedVehiclesOnLane(
										now + i + remainingMinG, signal.getLinkId(), laneId);
								nExpected += nTemp;
								double laneFlow = this.fullyAdaptiveLaemmerSignalController.lanes.getLanesToLinkAssignments()
										.get(signal.getLinkId()).getLanes().get(laneId).getCapacityVehiclesPerHour()
										* this.fullyAdaptiveLaemmerSignalController.config.qsim().getFlowCapFactor() / 3600;
								double tempGreenTime = nTemp / laneFlow;
								if (tempGreenTime > reqGreenTime) {
									reqGreenTime = tempGreenTime;
								}
							}
						} else {
							double nTemp = this.fullyAdaptiveLaemmerSignalController
									.getNumberOfExpectedVehiclesOnLink(now + i + remainingMinG, signal.getLinkId());
							nExpected += nTemp;
							double linkFlow = this.fullyAdaptiveLaemmerSignalController.network.getLinks().get(signal.getLinkId())
									.getCapacity() * this.fullyAdaptiveLaemmerSignalController.config.qsim().getFlowCapFactor()
									/ 3600;
							double tempGreenTime = nTemp / linkFlow;
							if (tempGreenTime > reqGreenTime) {
								reqGreenTime = tempGreenTime;
							}
						}
						double tempIndex = 0;
						if (nExpected > 0) {
							tempIndex = nExpected / (i + reqGreenTime);
						}
						if (tempIndex > index) {
							index = tempIndex;
						}
					} 
				}
			}
		} else {
			double nExpected = 0;
			double reqGreenTime = this.fullyAdaptiveLaemmerSignalController.laemmerConfig.getMinGreenTime();
			for (Id<SignalGroup> signalGroup : phase.getGreenSignalGroups()) {
				for (Signal signal : fullyAdaptiveLaemmerSignalController.getSystem().getSignalGroups().get(signalGroup).getSignals().values()) {
					if (signal.getLaneIds() != null && !signal.getLaneIds().isEmpty()) {
						for (Id<Lane> laneId : signal.getLaneIds()) {
							double nTemp = this.fullyAdaptiveLaemmerSignalController.getNumberOfExpectedVehiclesOnLane(
									now + this.fullyAdaptiveLaemmerSignalController.DEFAULT_INTERGREEN
									+ this.fullyAdaptiveLaemmerSignalController.laemmerConfig.getMinGreenTime(),
									signal.getLinkId(), laneId);
							nExpected += nTemp;
							double laneFlow = this.fullyAdaptiveLaemmerSignalController.lanes.getLanesToLinkAssignments()
									.get(signal.getLinkId()).getLanes().get(laneId).getCapacityVehiclesPerHour()
									* this.fullyAdaptiveLaemmerSignalController.config.qsim().getFlowCapFactor() / 3600;
							double tempGreenTime = nTemp / laneFlow;
							if (tempGreenTime > reqGreenTime) {
								reqGreenTime = tempGreenTime;
							}
						}
					} else {
						double nTemp = this.fullyAdaptiveLaemmerSignalController.getNumberOfExpectedVehiclesOnLink(
								now + this.fullyAdaptiveLaemmerSignalController.DEFAULT_INTERGREEN
								+ this.fullyAdaptiveLaemmerSignalController.laemmerConfig.getMinGreenTime(),
								signal.getLinkId());
						nExpected += nTemp;
						double linkFlow = this.fullyAdaptiveLaemmerSignalController.network.getLinks().get(signal.getLinkId())
								.getCapacity() * this.fullyAdaptiveLaemmerSignalController.config.qsim().getFlowCapFactor() / 3600;
						double tempGreenTime = nTemp / linkFlow;
						if (tempGreenTime > reqGreenTime) {
							reqGreenTime = tempGreenTime;
						}
					}
				} 
			}
			double penalty = 0;
			if (this.fullyAdaptiveLaemmerSignalController.activeRequest != null) {
				penalty = this.fullyAdaptiveLaemmerSignalController.activeRequest.laemmerPhase.abortionPenalty;
			}
			index = nExpected / (penalty + this.fullyAdaptiveLaemmerSignalController.DEFAULT_INTERGREEN + reqGreenTime);
		}
	}

	public boolean equals(LaemmerPhase other) {
		if (other == null) {
			return false;
		}
		else {
			return this.getPhase().equals(other.getPhase());
		}
	}

	public void getStatFields(StringBuilder builder) {
		builder.append("state_" + this.phase.getId() +";");
		builder.append("index_" + this.phase.getId() + ";");
		builder.append("load_" + this.phase.getId() + ";");
		builder.append("a_" + this.phase.getId() + ";");
		builder.append("abortionPen_" + this.phase.getId() + ";");
		builder.append("regTime_" + this.phase.getId() + ";");
		builder.append("nTotal_" + this.phase.getId() + ";");
	}

	public void getStepStats(StringBuilder builder, double now) {
		int totalN = 0;
		for(Id<SignalGroup> sg : this.phase.getGreenSignalGroups()) {
			for (Signal signal : fullyAdaptiveLaemmerSignalController.getSystem().getSignalGroups().get(sg).getSignals().values()) {
				if (signal.getLaneIds() != null && !signal.getLaneIds().isEmpty()) {
					for (Id<Lane> laneId : signal.getLaneIds()) {
						totalN += this.fullyAdaptiveLaemmerSignalController.getNumberOfExpectedVehiclesOnLane(now, signal.getLinkId(), laneId);
					}
				} else {
					totalN += this.fullyAdaptiveLaemmerSignalController.getNumberOfExpectedVehiclesOnLink(now, signal.getLinkId());
				}
			}
		}
		builder.append(this.phase.getState().name()+ ";")
		.append(this.index + ";")
		.append(this.determiningLoad + ";")
		.append(this.intergreenTime_a + ";")
		.append(this.abortionPenalty + ";")
		.append(this.regulationTime + ";")
		.append(totalN + ";");
	}

	public SignalPhase getPhase() {
		return this.phase;
	}
}
