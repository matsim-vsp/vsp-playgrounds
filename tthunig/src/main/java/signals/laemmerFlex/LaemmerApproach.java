package signals.laemmerFlex;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.core.mobsim.qsim.interfaces.SignalGroupState;
import org.matsim.lanes.Lane;

/**
 * 
 * @author pschade
 */
//TODO I consider renaming this to "LammerInDriveway" or "LammerNodeApproach" since is should also work without Lanes and shoud later also work for stabilization, pschade Jan '18

class LaemmerApproach {

	private Lane physicalLane;
	private FullyAdaptiveLaemmerSignalController fullyAdaptiveLaemmerSignalController;
	private double determiningLoad;
	private double maximumOutflow;
	private Signal signal;
	private double determiningArrivalRate;
	private Link link;
	private double stabilizationPressure_a;
	private double regulationTime;
	private SignalGroup signalGroup;
	private double queueLength;

	//TODO laemmerLane should also work without lanes, pschade, Jan'18
	public LaemmerApproach (Link link, Lane physicalLane, SignalGroup signalGroup, Signal signal, FullyAdaptiveLaemmerSignalController fullyAdaptiveLaemmerSignalController) {
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
	
    public LaemmerApproach(Link link, SignalGroup signalGroup, Signal signal,
			FullyAdaptiveLaemmerSignalController fullyAdaptiveLaemmerSignalController) {
    	this(link, null, signalGroup, signal, fullyAdaptiveLaemmerSignalController);
    }

	void calcLoadAndArrivalrate(double now) {
		this.determiningLoad = 0;
		double arrivalRate = 0.0;
		if (this.physicalLane != null) {
			arrivalRate = this.fullyAdaptiveLaemmerSignalController.getAverageLaneArrivalRate(now, link.getId(), physicalLane.getId());
			this.queueLength = fullyAdaptiveLaemmerSignalController.getNumberOfExpectedVehiclesOnLane(now, link.getId(), physicalLane.getId());
			//try to scale queue length on small links, pschade Feb'18
			if (this.physicalLane.getStartsAtMeterFromLinkEnd() < 250 && this.queueLength >= this.physicalLane.getStartsAtMeterFromLinkEnd()/6.0)
				this.queueLength *= 250/physicalLane.getStartsAtMeterFromLinkEnd();
		} else {
			arrivalRate = this.fullyAdaptiveLaemmerSignalController.getAverageArrivalRate(now, link.getId());
			this.queueLength = fullyAdaptiveLaemmerSignalController.getNumberOfExpectedVehiclesOnLink(now, link.getId());
			//try to scale queue length on small links, pschade Feb'18
			if (this.link.getLength() < 250 && this.queueLength >= this.link.getCapacity(now))
				queueLength *= 250/link.getLength();
		}
		this.determiningLoad = arrivalRate / maximumOutflow;
		this.determiningArrivalRate = arrivalRate;
	}
	   
    void updateStabilization(double now) {
        double n = 0;
        if (this.physicalLane != null) {
            n = this.fullyAdaptiveLaemmerSignalController.getNumberOfExpectedVehiclesOnLane(now, link.getId(), physicalLane.getId());
        } else {
            n = this.fullyAdaptiveLaemmerSignalController.getNumberOfExpectedVehiclesOnLink(now, link.getId());
        }

        if (n == 0) {
            stabilizationPressure_a = this.fullyAdaptiveLaemmerSignalController.laemmerConfig.getIntergreenTime();
        } else {
			// IMO a shouldn't be incremented but calculated since 1 sec
			// stepwidth isn't a prerequisite which you can trust.
			// pschade, Jan'18
        	stabilizationPressure_a++;
        }

        //Moved check for determineArrivaleRate == 0 down because "a" should also reseted if determined arrival rate is 0.
        if (fullyAdaptiveLaemmerSignalController.needStabilization(this) || this.signalGroup.getState().equals(SignalGroupState.GREEN)) {
        	return;
        }
        //reset stabilization time for recalculating
        this.regulationTime = 0;
        
        //don't stabilize if arrivale rate is 0 and (a) no green time for existing queues or (b) lane is empty and greentime for exisiting queue should granted
        // for (b) if queue length is > 0 and greentime should granted this flow will get MinGreenTime
        if (determiningArrivalRate == 0
        		&& (
        			!fullyAdaptiveLaemmerSignalController.getLaemmerConfig().isMinGreenTimeForNonGrowingQueues()
        			|| (n == 0 && fullyAdaptiveLaemmerSignalController.getLaemmerConfig().isMinGreenTimeForNonGrowingQueues() )
        		))  {
        	return;
        }
        //if there is not any queue, stabilization is not needed in any case
        if (n == 0) {
        	return;
        }
        

        double nCrit = determiningArrivalRate * this.fullyAdaptiveLaemmerSignalController.laemmerConfig.getDesiredCycleTime()
                * ((this.fullyAdaptiveLaemmerSignalController.laemmerConfig.getMaxCycleTime() - (stabilizationPressure_a / (1.0 - determiningLoad)))
                / (this.fullyAdaptiveLaemmerSignalController.laemmerConfig.getMaxCycleTime() - this.fullyAdaptiveLaemmerSignalController.laemmerConfig.getDesiredCycleTime()));

        if (n >= nCrit) {
        	/* TODO actually, this is the wrong place to check downstream conditions, since situation can change until the group has moved up to the queue front. 
        	 * a better moment would be while polling from the queue: poll the first element with downstream empty. but we would need a linked list instead of queue for this
        	 * and could no longer check for empty regulationQueue to decide for stabilization vs optimization... I would prefer to have some tests before! theresa, jul'17 */
        	//TODO What are we going to do with the downstream checks?
			if (!this.fullyAdaptiveLaemmerSignalController.laemmerConfig.isCheckDownstream() ||
					this.fullyAdaptiveLaemmerSignalController.downstreamSensor.allDownstreamLinksEmpty(this.fullyAdaptiveLaemmerSignalController.getSystem().getId(), this.signalGroup.getId())) {
				// signalLog.debug("Regulation time parameters: lambda: " + determiningLoad + " | T: " + desiredPeriod + " | qmax: " + determiningOutflow + " | qsum: " + flowSum + " | T_idle:" +
				// tIdle);
				this.regulationTime = Math.max(Math.rint(determiningLoad * this.fullyAdaptiveLaemmerSignalController.laemmerConfig.getDesiredCycleTime()),
						this.fullyAdaptiveLaemmerSignalController.laemmerConfig.getMinGreenTime());
				//approach to extend stabilisation time when vehicles are waiting but no new vehicles are approaching:
				//this.regulationTime = Math.max(this.regulationTime, Math.rint(this.queueLength*(this.maximumOutflow/3600.0)));
		    	fullyAdaptiveLaemmerSignalController.addLaneForStabilization(this);
				if (fullyAdaptiveLaemmerSignalController.getDebug())
					System.out.println("Stabilising "+this.getLink().getId()+"-"+this.getLaneId()+" n="+n+" nCrit="+nCrit+" a="+stabilizationPressure_a);
			}
        }
    }

	private Id<Lane> getLaneId() {
		if (this.physicalLane == null)
			return null;
		else
			return this.physicalLane.getId();
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
		if (this.regulationTime < 0) {
			//negative regulation time is not possible
			this.regulationTime = 0.0;
		}
	}

	public void extendRegulationTime(double extraTime) {
		this.regulationTime += extraTime;
	}

	public Signal getSignal() {
		return signal;
	}

	public SignalGroup getSignalGroup() {
		return signalGroup;
	}

    
}
