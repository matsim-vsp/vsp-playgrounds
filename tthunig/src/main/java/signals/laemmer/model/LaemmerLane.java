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
public class LaemmerLane {

	private Lane physicalLane;
	private FullyAdaptiveLaemmerSignalController laemmerSignalController2;
	private double determiningLoad;
	private int outflowSum;
	private Signal signal;
	private double determiningArrivalRate;
	private Link link;
	private double intergreenTime_a;
	private double regulationTime;
	private boolean stabilize;
	private SignalGroup signalGroup;

	//TODO laemmerLane should also work without lanes, pschade, Jan'18
	public LaemmerLane (Link link, Lane physicalLane, SignalGroup signalGroup, Signal signal, FullyAdaptiveLaemmerSignalController laemmerSignalControler) {
		this.physicalLane = physicalLane;
		this.laemmerSignalController2 = laemmerSignalControler;
		this.signal = signal;
		this.link = link;
		this.signalGroup = signalGroup;
	}
	
    //TODO this should be combined with update stabilization, since there isn't a representive driveway
	void determineRepresentativeDriveway(double now) {
        this.determiningLoad = 0;
        this.outflowSum = 0;
				if (signal.getLaneIds() != null && !signal.getLaneIds().isEmpty()) {
						double arrivalRate = this.laemmerSignalController2.getAverageLaneArrivalRate(now,
								link.getId(), physicalLane.getId());
						double outflowSum = physicalLane.getCapacityVehiclesPerHour()
								* this.laemmerSignalController2.config.qsim().getFlowCapFactor() / 3600;
						this.determiningLoad = arrivalRate / outflowSum;
						this.determiningArrivalRate = arrivalRate;
				} else {
					//TODO I think it isn't the best solution to register the Avg…Monitor every cycle again, pschade, Dec' 17
					this.laemmerSignalController2.sensorManager
							.registerAverageNumberOfCarsPerSecondMonitoring(signal.getLinkId(), this.laemmerSignalController2.getLaemmerConfig().getLookBackTime(), this.laemmerSignalController2.getLaemmerConfig().getTimeBucketSize());
					double outflow = this.link.getCapacity() * this.laemmerSignalController2.config.qsim().getFlowCapFactor() / 3600;
					double arrivalRate = this.laemmerSignalController2.getAverageArrivalRate(now, signal.getLinkId());
					this.determiningLoad = arrivalRate / outflow;
					this.determiningArrivalRate = arrivalRate;
				}
    }
	
    void update(double now) {
       updateStabilization(now);
       if (this.stabilize) {
    	   laemmerSignalController2.addLaneForStabilization(this);
       }
       else {
    	   laemmerSignalController2.removeLaneForStabilization(this);
       }
    }
    
	//TODO i'm unsure if there are some advantages to have this split from update(), pschade Dec 17
    private void updateStabilization(double now) {
    	this.regulationTime = 0;
    	this.stabilize = false;

        if (determiningArrivalRate == 0) {
            return;
        }

        double n = 0;
        if (this.physicalLane != null) {
            n = this.laemmerSignalController2.getNumberOfExpectedVehiclesOnLane(now, link.getId(), physicalLane.getId());
        } else {
            n = this.laemmerSignalController2.getNumberOfExpectedVehiclesOnLink(now, link.getId());
        }

        if (n == 0) {
            intergreenTime_a = this.laemmerSignalController2.DEFAULT_INTERGREEN;
        } else {
			// IMO intergreenTime_a++ shouldn't be incremented but calculated since 1 sec
			// stepwith isn't a prerequisite which you can trust.
			// Also intergreenTime is an ambiguous name, since intergreentime regulary
			// defines the time between the drop of one signal and the onset of another.
			// pschade, Jan'18
        	intergreenTime_a++;
        }

        //TODO Calculation of regTime and nCrit should be skipped if lane is already flagged for stabilization
        //or should not be skipped, because we probably should update their values when we create or call the phase
//        if (this.laemmerSignalController2.getLanesForStabilization().contains(this)) {
//            return;
//        }

        double nCrit = determiningArrivalRate * this.laemmerSignalController2.laemmerConfig.getDesiredCycleTime()
                * ((this.laemmerSignalController2.laemmerConfig.getMaxCycleTime() - (intergreenTime_a / (1 - determiningLoad)))
                / (this.laemmerSignalController2.laemmerConfig.getMaxCycleTime() - this.laemmerSignalController2.laemmerConfig.getDesiredCycleTime()));

        if (n >= nCrit) {
        	/* TODO actually, this is the wrong place to check downstream conditions, since situation can change until the group has moved up to the queue front. 
        	 * a better moment would be while polling from the queue: poll the first element with downstream empty. but we would need a linked list instead of queue for this
        	 * and could no longer check for empty regulationQueue to decide for stabilization vs optimization... I would prefer to have some tests before! theresa, jul'17 */
        	//TODO What are we going to do with the downstream checks?
			if (!this.laemmerSignalController2.laemmerConfig.isCheckDownstream() ||
					this.laemmerSignalController2.downstreamSensor.allDownstreamLinksEmpty(this.laemmerSignalController2.getSystem().getId(), this.signalGroup.getId())) {
				this.laemmerSignalController2.addLaneForStabilization(this);
				// signalLog.debug("Regulation time parameters: lambda: " + determiningLoad + " | T: " + desiredPeriod + " | qmax: " + determiningOutflow + " | qsum: " + flowSum + " | T_idle:" +
				// tIdle);
				this.regulationTime = Math.max(Math.rint(determiningLoad * this.laemmerSignalController2.laemmerConfig.getDesiredCycleTime() + (outflowSum / this.laemmerSignalController2.flowSum) * Math.max(this.laemmerSignalController2.tIdle, 0)), this.laemmerSignalController2.laemmerConfig.getMinGreenTime());
				this.stabilize = true;
			}
        }
    }

	public Lane getLane() {
		return this.physicalLane;
	}
	
    public double getRegulationTime() {
		return regulationTime;
	}
    
    public void getStatFields(StringBuilder builder) {
        builder.append("isGreen_" + this.physicalLane.getId() +";");
        builder.append("load_" + this.physicalLane.getId() + ";");
        builder.append("a_" + this.physicalLane.getId() + ";");
        builder.append("regTime_" + this.physicalLane.getId() + ";");
        builder.append("nTotal_" + this.physicalLane.getId() + ";");
    }

    public void getStepStats(StringBuilder builder, double now) {
        int totalN = laemmerSignalController2.getNumberOfExpectedVehiclesOnLane(now, signal.getLinkId(), physicalLane.getId());
        builder.append(this.signal.getSignalizeableItems().iterator().next().hasGreenForAllToLinks()+ ";")
                .append(this.determiningLoad + ";")
                .append(this.intergreenTime_a + ";")
                .append(this.regulationTime + ";")
                .append(totalN + ";");
    }
    
}
