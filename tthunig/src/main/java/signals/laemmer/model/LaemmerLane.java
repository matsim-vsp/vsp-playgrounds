package signals.laemmer.model;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.core.mobsim.qsim.interfaces.SignalGroupState;
import org.matsim.lanes.data.Lane;

/**
 * 
 * @author pschade
 */
//TODO I consider renaming this to "LammerInDriveway" or "LammerNodeApproach" since is should also work without Lanes and shoud later also work for stabilization, pschade Jan '18

public class LaemmerLane {

	private Lane physicalLane;
	private FullyAdaptiveLaemmerSignalController fullyAdaptiveLaemmerSignalController;
	private double determiningLoad;
	private double maximumOutflow;
	private Signal signal;
	private double determiningArrivalRate;
	private Link link;
	private double stabilizationPressure_a;
	private double regulationTime;
	private boolean needStabilization;
	private SignalGroup signalGroup;

	//TODO laemmerLane should also work without lanes, pschade, Jan'18
	public LaemmerLane (Link link, Lane physicalLane, SignalGroup signalGroup, Signal signal, FullyAdaptiveLaemmerSignalController fullyAdaptiveLaemmerSignalController) {
		this.physicalLane = physicalLane;
		this.fullyAdaptiveLaemmerSignalController = fullyAdaptiveLaemmerSignalController;
		this.signal = signal;
		this.link = link;
		this.signalGroup = signalGroup;
		if (this.physicalLane != null)
			this.maximumOutflow = physicalLane.getCapacityVehiclesPerHour() * this.fullyAdaptiveLaemmerSignalController.config.qsim().getFlowCapFactor() / 3600;
		else
			this.maximumOutflow = this.link.getCapacity() * this.fullyAdaptiveLaemmerSignalController.config.qsim().getFlowCapFactor() / 3600;
		this.fullyAdaptiveLaemmerSignalController.sensorManager.registerAverageNumberOfCarsPerSecondMonitoring(signal.getLinkId(), this.fullyAdaptiveLaemmerSignalController.getLaemmerConfig().getLookBackTime(), this.fullyAdaptiveLaemmerSignalController.getLaemmerConfig().getTimeBucketSize());
	}
	
    public LaemmerLane(Link link, SignalGroup signalGroup, Signal signal,
			FullyAdaptiveLaemmerSignalController fullyAdaptiveLaemmerSignalController) {
    	this(link, null, signalGroup, signal, fullyAdaptiveLaemmerSignalController);
    }

	//TODO this should be combined with update stabilization, since there isn't a representive driveway any more
	void calcLoadAndArrivalrate(double now) {
		this.determiningLoad = 0;
		double arrivalRate = 0.0;
		if (this.physicalLane != null) {
			arrivalRate = this.fullyAdaptiveLaemmerSignalController.getAverageLaneArrivalRate(now, link.getId(),
					physicalLane.getId());
		} else {
			arrivalRate = this.fullyAdaptiveLaemmerSignalController.getAverageArrivalRate(now, link.getId());
		}
		this.determiningLoad = arrivalRate / maximumOutflow;
		this.determiningArrivalRate = arrivalRate;
	}
	
	//TODO In my opinion this can be merged with updateStabilization(), pschade Jan'18
    void updateStabilizationAndAddToQueueIfNeeded(double now) {
    	if (!fullyAdaptiveLaemmerSignalController.needStabilization(this)) {
	    		updateStabilization(now);
	    	if (this.needStabilization) {
	    	   fullyAdaptiveLaemmerSignalController.addLaneForStabilization(this);
	    	}
    	}
    }
    
	//TODO i'm unsure if there are some advantages to have this split from update(), pschade Dec 17
    private void updateStabilization(double now) {
    	this.regulationTime = 0;
    	this.needStabilization = false;

        if (determiningArrivalRate == 0) {
            return;
        }

        double n = 0;
        if (this.physicalLane != null) {
            n = this.fullyAdaptiveLaemmerSignalController.getNumberOfExpectedVehiclesOnLane(now, link.getId(), physicalLane.getId());
        } else {
            n = this.fullyAdaptiveLaemmerSignalController.getNumberOfExpectedVehiclesOnLink(now, link.getId());
        }

        if (n == 0) {
            stabilizationPressure_a = this.fullyAdaptiveLaemmerSignalController.DEFAULT_INTERGREEN;
        } else {
			// IMO a shouldn't be incremented but calculated since 1 sec
			// stepwidth isn't a prerequisite which you can trust.
			// pschade, Jan'18
        	stabilizationPressure_a++;
        }

 
        if (this.fullyAdaptiveLaemmerSignalController.needStabilization(this) || this.signalGroup.getState().equals(SignalGroupState.GREEN)) {
            return;
        }

        double nCrit = determiningArrivalRate * this.fullyAdaptiveLaemmerSignalController.laemmerConfig.getDesiredCycleTime()
                * ((this.fullyAdaptiveLaemmerSignalController.laemmerConfig.getMaxCycleTime() - (stabilizationPressure_a / (1 - determiningLoad)))
                / (this.fullyAdaptiveLaemmerSignalController.laemmerConfig.getMaxCycleTime() - this.fullyAdaptiveLaemmerSignalController.laemmerConfig.getDesiredCycleTime()));

        if (n >= nCrit) {
        	/* TODO actually, this is the wrong place to check downstream conditions, since situation can change until the group has moved up to the queue front. 
        	 * a better moment would be while polling from the queue: poll the first element with downstream empty. but we would need a linked list instead of queue for this
        	 * and could no longer check for empty regulationQueue to decide for stabilization vs optimization... I would prefer to have some tests before! theresa, jul'17 */
        	//TODO What are we going to do with the downstream checks?
			if (!this.fullyAdaptiveLaemmerSignalController.laemmerConfig.isCheckDownstream() ||
					this.fullyAdaptiveLaemmerSignalController.downstreamSensor.allDownstreamLinksEmpty(this.fullyAdaptiveLaemmerSignalController.getSystem().getId(), this.signalGroup.getId())) {
				this.fullyAdaptiveLaemmerSignalController.addLaneForStabilization(this);
				// signalLog.debug("Regulation time parameters: lambda: " + determiningLoad + " | T: " + desiredPeriod + " | qmax: " + determiningOutflow + " | qsum: " + flowSum + " | T_idle:" +
				// tIdle);
				this.regulationTime = Math.max(Math.rint(determiningLoad * this.fullyAdaptiveLaemmerSignalController.laemmerConfig.getDesiredCycleTime() + (maximumOutflow / this.fullyAdaptiveLaemmerSignalController.flowSum) * Math.max(this.fullyAdaptiveLaemmerSignalController.tIdle, 0)), this.fullyAdaptiveLaemmerSignalController.laemmerConfig.getMinGreenTime());
				this.needStabilization = true;
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
    	String identifier;
    	if (this.physicalLane == null) {
    		identifier = this.link.getId().toString();
    	} else {
    		identifier = this.physicalLane.getId().toString();
    	}
    	
        builder.append("isGreen_" + identifier +";");
        builder.append("load_" + identifier + ";");
        builder.append("a_" + identifier + ";");
        builder.append("regTime_" + identifier + ";");
        builder.append("nTotal_" + identifier + ";");
    }

    public void getStepStats(StringBuilder builder, double now) {
    	int totalN;
    	if (this.physicalLane == null) {
    		totalN = fullyAdaptiveLaemmerSignalController.getNumberOfExpectedVehiclesOnLink(now, signal.getLinkId());
    	} else {
    		totalN = fullyAdaptiveLaemmerSignalController.getNumberOfExpectedVehiclesOnLane(now, signal.getLinkId(), physicalLane.getId());
    	}
        builder.append(this.signal.getSignalizeableItems().iterator().next().hasGreenForAllToLinks()+ ";")
                .append(this.determiningLoad + ";")
                .append(this.stabilizationPressure_a + ";")
                .append(this.regulationTime + ";")
                .append(totalN + ";");
    }

	public double getMaxOutflow() {
		return maximumOutflow;
	}

	public Link getLink() {
		return this.link;
	}
	public double getDeterminingLoad() {
		return this.determiningLoad;
	}

	public void shortenRegulationTime(double passedRegulationTime) {
		this.regulationTime -= passedRegulationTime;
	}

	public Signal getSignal() {
		return signal;
	}

	public SignalGroup getSignalGroup() {
		return signalGroup;
	}
    
}
