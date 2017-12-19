package signals.laemmer.model;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.lanes.data.Lane;

/**
 * 
 * @author pschade
 */
class LaemmerPhase {

    private final FullyAdaptiveLaemmerSignalController fullyAdaptiveLaemmerSignalController;

	SignalPhase phase;

    double index = 0;
    private double abortionPenalty = 0;
    private boolean stabilize = false;

    private double intergreenTime_a;
    double regulationTime = 0;

    Id<Lane> determiningLane;
    Id<Link> determiningLink;
    private double determiningArrivalRate;
    double determiningLoad;
    double outflowSum;

    LaemmerPhase(FullyAdaptiveLaemmerSignalController laemmerSignalController2, SignalPhase signalPhase) {
        this.fullyAdaptiveLaemmerSignalController = laemmerSignalController2;
		this.phase = signalPhase;
		this.intergreenTime_a = this.fullyAdaptiveLaemmerSignalController.DEFAULT_INTERGREEN;
    }

    //TODO Overthink, if it's a good idea to have a determining driveway for a laemmerPhase since there can be multiple phases with this driveway
    @Deprecated //stabilization should be done lane-wise with LaemmerLane
    void determineRepresentativeDriveway(double now) {
        this.determiningLoad = 0;
        this.determiningLink = null;
        this.determiningLane = null;
        this.outflowSum = 0;
        
        for (Id<SignalGroup> signalGroup : this.phase.getGreenSignalGroups()) {
			for (Signal signal : this.fullyAdaptiveLaemmerSignalController.getSystem().getSignalGroups().get(signalGroup).getSignals().values()) {
				if (signal.getLaneIds() != null && !signal.getLaneIds().isEmpty()) {
					for (Id<Lane> laneId : signal.getLaneIds()) {
						double arrivalRate = this.fullyAdaptiveLaemmerSignalController.getAverageLaneArrivalRate(now,
								signal.getLinkId(), laneId);
						double outflow = this.fullyAdaptiveLaemmerSignalController.lanes.getLanesToLinkAssignments()
								.get(signal.getLinkId()).getLanes().get(laneId).getCapacityVehiclesPerHour()
								* this.fullyAdaptiveLaemmerSignalController.config.qsim().getFlowCapFactor() / 3600;
						outflowSum += outflow;
						double tempLoad = arrivalRate / outflow;
						if (tempLoad >= this.determiningLoad) {
							this.determiningLoad = tempLoad;
							this.determiningArrivalRate = arrivalRate;
							this.determiningLane = laneId;
							this.determiningLink = signal.getLinkId();
						}
					}
				} else {
					this.fullyAdaptiveLaemmerSignalController.sensorManager
							.registerAverageNumberOfCarsPerSecondMonitoring(signal.getLinkId());
					double outflow = this.fullyAdaptiveLaemmerSignalController.network.getLinks().get(signal.getLinkId())
							.getCapacity() * this.fullyAdaptiveLaemmerSignalController.config.qsim().getFlowCapFactor() / 3600;
					outflowSum += outflow;
					double arrivalRate = this.fullyAdaptiveLaemmerSignalController.getAverageArrivalRate(now, signal.getLinkId());
					double tempLoad = arrivalRate / outflow;
					if (tempLoad >= this.determiningLoad) {
						this.determiningLoad = tempLoad;
						this.determiningArrivalRate = arrivalRate;
						this.determiningLane = null;
						this.determiningLink = signal.getLinkId();
					}
				}
			} 
		}
    }

    void update(double now) {
        updateAbortionPenalty(now);

        if (!this.fullyAdaptiveLaemmerSignalController.laemmerConfig.getActiveRegime().equals(LaemmerConfig.Regime.OPTIMIZING)) {
            //updateStabilization(now);
        }
        //TODO check if it's a good idea to omit calculating the priority index if stabilization is needed
        // - I think we need it to find a good combination of vehicle flows which can drive together
        // - probably we'll have outdated values somewhere and make decitions on them, pschade Dec 17
        if (!this.stabilize)
        	calculatePriorityIndex(now);
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
        if (this.fullyAdaptiveLaemmerSignalController.activeRequest != null && this.fullyAdaptiveLaemmerSignalController.activeRequest.laemmerPhase == this) {
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

    @Deprecated //Stabilization should be done lane-wise with LaemmerLane
    private void updateStabilization(double now) {

        if (determiningArrivalRate == 0) {
            return;
        }

        double n = 0;
        if (determiningLane != null) {
            n = this.fullyAdaptiveLaemmerSignalController.getNumberOfExpectedVehiclesOnLane(now, determiningLink, determiningLane);
        } else {
            n = this.fullyAdaptiveLaemmerSignalController.getNumberOfExpectedVehiclesOnLink(now, determiningLink);
        }

        if (n == 0) {
            intergreenTime_a = this.fullyAdaptiveLaemmerSignalController.DEFAULT_INTERGREEN;
        } else {
            intergreenTime_a++;
        }

        if (this.fullyAdaptiveLaemmerSignalController.regulationQueue.contains(this)) {
            return;
        }

        this.regulationTime = 0;
        this.stabilize = false;
        double nCrit = determiningArrivalRate * this.fullyAdaptiveLaemmerSignalController.laemmerConfig.getDesiredCycleTime()
                * ((this.fullyAdaptiveLaemmerSignalController.laemmerConfig.getMaxCycleTime() - (intergreenTime_a / (1 - determiningLoad)))
                / (this.fullyAdaptiveLaemmerSignalController.laemmerConfig.getMaxCycleTime() - this.fullyAdaptiveLaemmerSignalController.laemmerConfig.getDesiredCycleTime()));

        if (n >= nCrit) {
        	/* TODO actually, this is the wrong place to check downstream conditions, since situation can change until the group has moved up to the queue front. 
        	 * a better moment would be while polling from the queue: poll the first element with downstream empty. but we would need a linked list instead of queue for this
        	 * and could no longer check for empty regulationQueue to decide for stabilization vs optimization... I would prefer to have some tests before! theresa, jul'17 */
        	//TODO allDownstreamLinksEmpty should be able to check for a phase. for now we are checking for an arbitrary signalGroup in it. pschade, nov 17
			if (!this.fullyAdaptiveLaemmerSignalController.laemmerConfig.isCheckDownstream() || this.fullyAdaptiveLaemmerSignalController.downstreamSensor.allDownstreamLinksEmpty(this.fullyAdaptiveLaemmerSignalController.getSystem().getId(), phase.getGreenSignalGroups().iterator().next())) {
				this.fullyAdaptiveLaemmerSignalController.regulationQueue.add(this);
				// signalLog.debug("Regulation time parameters: lambda: " + determiningLoad + " | T: " + desiredPeriod + " | qmax: " + determiningOutflow + " | qsum: " + flowSum + " | T_idle:" +
				// tIdle);
				this.regulationTime = Math.max(Math.rint(determiningLoad * this.fullyAdaptiveLaemmerSignalController.laemmerConfig.getDesiredCycleTime() + (outflowSum / this.fullyAdaptiveLaemmerSignalController.flowSum) * Math.max(this.fullyAdaptiveLaemmerSignalController.tIdle, 0)), this.fullyAdaptiveLaemmerSignalController.laemmerConfig.getMinGreenTime());
				this.stabilize = true;
			}
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

//	TODO decide how state is defined for a phase, pschade Nov 17
//	TODO this is an early implementation and is supposed to be reconsidered and has to be tested, pschade Nov 17 
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
        builder.append(this.phase.getState(this.fullyAdaptiveLaemmerSignalController.getSystem()).name()+ ";")
                .append(this.index + ";")
                .append(this.determiningLoad + ";")
                .append(this.intergreenTime_a + ";")
                .append(this.abortionPenalty + ";")
                .append(this.regulationTime + ";")
                .append(totalN + ";");
    }
}