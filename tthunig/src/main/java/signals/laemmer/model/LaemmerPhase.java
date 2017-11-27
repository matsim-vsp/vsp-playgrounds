package signals.laemmer.model;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.lanes.data.Lane;

import com.jogamp.opengl.util.awt.AWTGLPixelBuffer.SingleAWTGLPixelBufferProvider;

class LaemmerPhase {

    /**
	 * 
	 */
	private final LaemmerSignalController2 laemmerSignalController2;

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

    LaemmerPhase(LaemmerSignalController2 laemmerSignalController2, SignalPhase signalPhase) {
        this.laemmerSignalController2 = laemmerSignalController2;
		this.phase = signalPhase;
		this.intergreenTime_a = this.laemmerSignalController2.DEFAULT_INTERGREEN;
    }

    //TODO Overthink, if it's a good idea to have a determining driveway for a laemmerPhase since there can be multiple phases with this driveway
    void determineRepresentativeDriveway(double now) {
        this.determiningLoad = 0;
        this.determiningLink = null;
        this.determiningLane = null;
        this.outflowSum = 0;
        
        for (Id<SignalGroup> signalGroup : this.phase.getGreenSignalGroups()) {
			for (Signal signal : this.laemmerSignalController2.getSystem().getSignalGroups().get(signalGroup).getSignals().values()) {
				if (signal.getLaneIds() != null && !signal.getLaneIds().isEmpty()) {
					for (Id<Lane> laneId : signal.getLaneIds()) {
						double arrivalRate = this.laemmerSignalController2.getAverageLaneArrivalRate(now,
								signal.getLinkId(), laneId);
						double outflow = this.laemmerSignalController2.lanes.getLanesToLinkAssignments()
								.get(signal.getLinkId()).getLanes().get(laneId).getCapacityVehiclesPerHour()
								* this.laemmerSignalController2.config.qsim().getFlowCapFactor() / 3600;
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
					this.laemmerSignalController2.sensorManager
							.registerAverageNumberOfCarsPerSecondMonitoring(signal.getLinkId());
					double outflow = this.laemmerSignalController2.network.getLinks().get(signal.getLinkId())
							.getCapacity() * this.laemmerSignalController2.config.qsim().getFlowCapFactor() / 3600;
					outflowSum += outflow;
					double arrivalRate = this.laemmerSignalController2.getAverageArrivalRate(now, signal.getLinkId());
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

        if (!this.laemmerSignalController2.laemmerConfig.getActiveRegime().equals(LaemmerConfig.Regime.OPTIMIZING)) {
            updateStabilization(now);
        }
        calculatePriorityIndex(now);
    }

    private void updateAbortionPenalty(double now) {
        this.abortionPenalty = 0;
        if (this.laemmerSignalController2.activeRequest != null && this.equals(this.laemmerSignalController2.activeRequest.signal)) {
            double waitingTimeSum = 0;
            double remainingInBetweenTime = Math.max(this.laemmerSignalController2.activeRequest.time - now, 0);
            for (double i = remainingInBetweenTime; i < this.laemmerSignalController2.DEFAULT_INTERGREEN; i++) {
                for (Id<SignalGroup> signalGroup : phase.getGreenSignalGroups()) {
					for (Signal signal : laemmerSignalController2.getSystem().getSignalGroups().get(signalGroup).getSignals().values()) {
						if (signal.getLaneIds() != null && !signal.getLaneIds().isEmpty()) {
							for (Id<Lane> laneId : signal.getLaneIds()) {
								waitingTimeSum += this.laemmerSignalController2
										.getNumberOfExpectedVehiclesOnLane(now + i, signal.getLinkId(), laneId);
							}
						} else {
							waitingTimeSum += this.laemmerSignalController2.getNumberOfExpectedVehiclesOnLink(now + i,
									signal.getLinkId());
						}
					} 
				}
            }
            double n = 0;
            for (Id<SignalGroup> signalGroup : phase.getGreenSignalGroups()) {
				for (Signal signal : laemmerSignalController2.getSystem().getSignalGroups().get(signalGroup).getSignals().values()) {
					if (signal.getLaneIds() != null && !signal.getLaneIds().isEmpty()) {
						for (Id<Lane> laneId : signal.getLaneIds()) {
							n += this.laemmerSignalController2.getNumberOfExpectedVehiclesOnLane(
									now + this.laemmerSignalController2.DEFAULT_INTERGREEN, signal.getLinkId(), laneId);
						}
					} else {
						n += this.laemmerSignalController2.getNumberOfExpectedVehiclesOnLink(
								now + this.laemmerSignalController2.DEFAULT_INTERGREEN, signal.getLinkId());
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
        if (this.laemmerSignalController2.activeRequest != null && this.laemmerSignalController2.activeRequest.signal == this) {
            double remainingInBetweenTime = Math.max(this.laemmerSignalController2.activeRequest.time - now, 0);
            double remainingMinG = Math.max(this.laemmerSignalController2.activeRequest.time - now + this.laemmerSignalController2.MIN_G - remainingInBetweenTime, 0);
            for (double i = remainingInBetweenTime; i <= this.laemmerSignalController2.DEFAULT_INTERGREEN; i++) {
                double nExpected = 0;
                double reqGreenTime = remainingMinG;
                for (Id<SignalGroup> signalGroup : phase.getGreenSignalGroups()) {
					for (Signal signal : laemmerSignalController2.getSystem().getSignalGroups().get(signalGroup).getSignals().values()) {
						if (signal.getLaneIds() != null && !signal.getLaneIds().isEmpty()) {
							for (Id<Lane> laneId : signal.getLaneIds()) {
								double nTemp = this.laemmerSignalController2.getNumberOfExpectedVehiclesOnLane(
										now + i + remainingMinG, signal.getLinkId(), laneId);
								nExpected += nTemp;
								double laneFlow = this.laemmerSignalController2.lanes.getLanesToLinkAssignments()
										.get(signal.getLinkId()).getLanes().get(laneId).getCapacityVehiclesPerHour()
										* this.laemmerSignalController2.config.qsim().getFlowCapFactor() / 3600;
								double tempGreenTime = nTemp / laneFlow;
								if (tempGreenTime > reqGreenTime) {
									reqGreenTime = tempGreenTime;
								}
							}
						} else {
							double nTemp = this.laemmerSignalController2
									.getNumberOfExpectedVehiclesOnLink(now + i + remainingMinG, signal.getLinkId());
							nExpected += nTemp;
							double linkFlow = this.laemmerSignalController2.network.getLinks().get(signal.getLinkId())
									.getCapacity() * this.laemmerSignalController2.config.qsim().getFlowCapFactor()
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
            double reqGreenTime = this.laemmerSignalController2.MIN_G;
            for (Id<SignalGroup> signalGroup : phase.getGreenSignalGroups()) {
				for (Signal signal : laemmerSignalController2.getSystem().getSignalGroups().get(signalGroup).getSignals().values()) {
					if (signal.getLaneIds() != null && !signal.getLaneIds().isEmpty()) {
						for (Id<Lane> laneId : signal.getLaneIds()) {
							double nTemp = this.laemmerSignalController2.getNumberOfExpectedVehiclesOnLane(
									now + this.laemmerSignalController2.DEFAULT_INTERGREEN
											+ this.laemmerSignalController2.MIN_G,
									signal.getLinkId(), laneId);
							nExpected += nTemp;
							double laneFlow = this.laemmerSignalController2.lanes.getLanesToLinkAssignments()
									.get(signal.getLinkId()).getLanes().get(laneId).getCapacityVehiclesPerHour()
									* this.laemmerSignalController2.config.qsim().getFlowCapFactor() / 3600;
							double tempGreenTime = nTemp / laneFlow;
							if (tempGreenTime > reqGreenTime) {
								reqGreenTime = tempGreenTime;
							}
						}
					} else {
						double nTemp = this.laemmerSignalController2.getNumberOfExpectedVehiclesOnLink(
								now + this.laemmerSignalController2.DEFAULT_INTERGREEN
										+ this.laemmerSignalController2.MIN_G,
								signal.getLinkId());
						nExpected += nTemp;
						double linkFlow = this.laemmerSignalController2.network.getLinks().get(signal.getLinkId())
								.getCapacity() * this.laemmerSignalController2.config.qsim().getFlowCapFactor() / 3600;
						double tempGreenTime = nTemp / linkFlow;
						if (tempGreenTime > reqGreenTime) {
							reqGreenTime = tempGreenTime;
						}
					}
				} 
			}
			double penalty = 0;
            if (this.laemmerSignalController2.activeRequest != null) {
                penalty = this.laemmerSignalController2.activeRequest.signal.abortionPenalty;
            }
            index = nExpected / (penalty + this.laemmerSignalController2.DEFAULT_INTERGREEN + reqGreenTime);
        }
    }

    private void updateStabilization(double now) {

        if (determiningArrivalRate == 0) {
            return;
        }

        double n = 0;
        if (determiningLane != null) {
            n = this.laemmerSignalController2.getNumberOfExpectedVehiclesOnLane(now, determiningLink, determiningLane);
        } else {
            n = this.laemmerSignalController2.getNumberOfExpectedVehiclesOnLink(now, determiningLink);
        }

        if (n == 0) {
            intergreenTime_a = this.laemmerSignalController2.DEFAULT_INTERGREEN;
        } else {
            intergreenTime_a++;
        }

        if (this.laemmerSignalController2.regulationQueue.contains(this)) {
            return;
        }

        this.regulationTime = 0;
        this.stabilize = false;
        double nCrit = determiningArrivalRate * this.laemmerSignalController2.desiredPeriod
                * ((this.laemmerSignalController2.maxPeriod - (intergreenTime_a / (1 - determiningLoad)))
                / (this.laemmerSignalController2.maxPeriod - this.laemmerSignalController2.desiredPeriod));

        if (n >= nCrit) {
        	/* TODO actually, this is the wrong place to check downstream conditions, since situation can change until the group has moved up to the queue front. 
        	 * a better moment would be while polling from the queue: poll the first element with downstream empty. but we would need a linked list instead of queue for this
        	 * and could no longer check for empty regulationQueue to decide for stabilization vs optimization... I would prefer to have some tests before! theresa, jul'17 */
        	//TODO allDownstreamLinksEmpty should be able to check for a phase. for now we are checking for an arbitrary signalGroup in it. pschade, nov 17
			if (!this.laemmerSignalController2.laemmerConfig.isCheckDownstream() || this.laemmerSignalController2.downstreamSensor.allDownstreamLinksEmpty(this.laemmerSignalController2.getSystem().getId(), phase.getGreenSignalGroups().iterator().next())) {
				this.laemmerSignalController2.regulationQueue.add(this);
				// signalLog.debug("Regulation time parameters: lambda: " + determiningLoad + " | T: " + desiredPeriod + " | qmax: " + determiningOutflow + " | qsum: " + flowSum + " | T_idle:" +
				// tIdle);
				this.regulationTime = Math.max(Math.rint(determiningLoad * this.laemmerSignalController2.desiredPeriod + (outflowSum / this.laemmerSignalController2.flowSum) * Math.max(this.laemmerSignalController2.tIdle, 0)), this.laemmerSignalController2.MIN_G);
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
	        for (Signal signal : laemmerSignalController2.getSystem().getSignalGroups().get(sg).getSignals().values()) {
	            if (signal.getLaneIds() != null && !signal.getLaneIds().isEmpty()) {
	                for (Id<Lane> laneId : signal.getLaneIds()) {
	                    totalN += this.laemmerSignalController2.getNumberOfExpectedVehiclesOnLane(now, signal.getLinkId(), laneId);
	                }
	            } else {
	                totalN += this.laemmerSignalController2.getNumberOfExpectedVehiclesOnLink(now, signal.getLinkId());
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
}