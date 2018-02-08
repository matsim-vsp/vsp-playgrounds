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
	
    //Probably consider to try also a combination of simmilar outflow rates
    public enum StabilizationStrategy {USE_MAX_LANECOUNT, PRIORIZE_HIGHER_POSITIONS, COMBINE_SIMILAR_REGULATIONTIME, HEURISTIC}; 
    private StabilizationStrategy activeStabilizationStrategy = StabilizationStrategy.HEURISTIC;
    
    public enum Regime {COMBINED, OPTIMIZING, STABILIZING};
    private Regime activeRegime = Regime.COMBINED;

    private double maxCycleTime = 180;
    private double desiredCycleTime = 120;

    private double defaultIntergreenTime = 5.0;
    //I think this shouldn't default to 0.0, pschade Jan'18
    private double minGreenTime = 5.0;
    
    //size of timeBuckets for LaneSensor and LinkSensor
    private double timeBucketSize = Double.POSITIVE_INFINITY; //15.0;
    //lookBackTime for LaneSensor and LinkSensor
    private double lookBackTime = Double.POSITIVE_INFINITY; //300.0;

    private Map<Id<Link>, Double> linkArrivalRates = new HashMap<>();
    private Map<Id<Link>, Map<Id<Lane>,Double>> laneArrivalRates = new HashMap<>();

    private boolean useDefaultIntergreenTime = true;
    private boolean analysisEnabled = false;
    
	/** activate the phase only if downstream links are empty. */
	private boolean checkDownstream = false;

	private boolean isRemoveSubPhases = true;

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
    
    public StabilizationStrategy getActiveStabilizationStrategy() {
		return activeStabilizationStrategy;
	}

	public void setActiveStabilizationStrategy(StabilizationStrategy activeStabilizationStrategy) {
		this.activeStabilizationStrategy = activeStabilizationStrategy;
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
	
	@Deprecated /** deprecated, use getStabilizationStrategy instead **/
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

	public boolean isRemoveSubPhases() {
		return isRemoveSubPhases ;
	}
}
