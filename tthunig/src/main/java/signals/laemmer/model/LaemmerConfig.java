package signals.laemmer.model;

//import com.sun.istack.internal.NotNull;
//import com.sun.istack.internal.Nullable;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.lanes.data.Lane;

import java.util.HashMap;
import java.util.Map;

/**
 * @author nkuehnel, tthunig
 */
public class LaemmerConfig {
	
    public enum Regime {COMBINED, OPTIMIZING, STABILIZING};
    private Regime activeRegime = Regime.COMBINED;

    private double maxCycleTime = 120;
    private double desiredCycleTime = 70;

    private double defaultIntergreenTime = 5.0;
    private double minGreenTime = 0.0;
    
    //size of timeBuckets for LaneSensor and LinkSensor
    private double timeBucketSize = 15.0;
    //lookBackTime for LaneSensor and LinkSensor
    private double lookBackTime = 300.0;

    private Map<Id<Link>, Double> linkArrivalRates = new HashMap<>();
    private Map<Id<Link>, Map<Id<Lane>,Double>> laneArrivalRates = new HashMap<>();

    private boolean useDefaultIntergreenTime = true;
    private boolean analysisEnabled = false;
    
	/** activate the phase only if downstream links are empty. */
	private boolean checkDownstream = false;

    //    @Nullable
    public Double getLaneArrivalRate(Id<Link> linkId, Id<Lane> laneId) {
        if(laneArrivalRates.containsKey(linkId)) {
            return this.laneArrivalRates.get(linkId).get(laneId);
        } else {
            return null;
        }
    }

    public double getMinGreenTime() {
        return minGreenTime;
    }

    public void setMinGreenTime(double minGreenTime) {
        this.minGreenTime = minGreenTime;
    }

    public Regime getActiveRegime() {
        return activeRegime;
    }

    public void setActiveRegime(Regime activeRegime) {
        this.activeRegime = activeRegime;
    }

    public void addArrivalRateForLink(Id<Link> linkId, double arrivalRate) {
        this.linkArrivalRates.put(linkId, arrivalRate);
    }

//    @Nullable
    public Double getLinkArrivalRate(Id<Link> linkId) {
        return linkArrivalRates.get(linkId);
    }

    public void addArrivalRateForLane(Id<Link> linkId, Id<Lane> laneId, double arrivalRate) {
        if(!this.laneArrivalRates.containsKey(linkId)) {
            this.laneArrivalRates.put(linkId, new HashMap<>());
        }
        this.laneArrivalRates.get(linkId).put(laneId, arrivalRate);
    }

    public double getMaxCycleTime() {
        return maxCycleTime;
    }

    public void setMaxCycleTime(double maxCycleTime) {
        this.maxCycleTime = maxCycleTime;
    }

    public double getDesiredCycleTime() {
        return desiredCycleTime;
    }

    public void setDesiredCycleTime(double desiredCycleTime) {
        this.desiredCycleTime = desiredCycleTime;
    }

    public boolean isUseDefaultIntergreenTime() {
        return useDefaultIntergreenTime;
    }

    public void setUseDefaultIntergreenTime(boolean useDefaulttIntergreenTime) {
        this.useDefaultIntergreenTime = useDefaulttIntergreenTime;
    }

    public double getDefaultIntergreenTime() {
        return defaultIntergreenTime;
    }

    public void setDefaultIntergreenTime(double intergreen) {
        this.defaultIntergreenTime = intergreen;
    }


    public boolean isAnalysisEnabled() {
        return analysisEnabled;
    }
    
    public void setAnalysisEnabled(boolean enabled){
    	this.analysisEnabled = enabled;
    }
	
	public void setCheckDownstream(boolean checkDownstream) {
		this.checkDownstream = checkDownstream;
	}
	
	public boolean isCheckDownstream() {
		return checkDownstream;
	}

	public boolean useHeuristicPhaseGeneration() {
		// TODO Auto-generated method stub
		return false;
	}

	public double getTimeBucketSize() {
		return timeBucketSize;
	}

	public void setTimeBucketSize(double timeBucketSize) {
		this.timeBucketSize = timeBucketSize;
	}

	public double getLookBackTime() {
		return lookBackTime;
	}

	public void setLookBackTime(double lookBackTime) {
		this.lookBackTime = lookBackTime;
	}
}
