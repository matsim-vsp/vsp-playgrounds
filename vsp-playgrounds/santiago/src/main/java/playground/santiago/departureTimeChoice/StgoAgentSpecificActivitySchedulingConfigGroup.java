package playground.santiago.departureTimeChoice;

import org.matsim.core.config.ReflectiveConfigGroup;


public class StgoAgentSpecificActivitySchedulingConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "agentSpecificActivityScheduling" ;

	public StgoAgentSpecificActivitySchedulingConfigGroup() {
		super(GROUP_NAME);
	}

	private boolean useAgentSpecificActivityScheduling = true;
	private double activityDurationBin = 1800.; //Be aware of this, it's different compared with the value of Ihab.
	//TODO: Should be different tolerances for different act. types ? ... this is the reason of an specific Config group for Santiago.
	private double tolerance = 900.;
	private boolean removeNetworkSpecificInformation = false;
	private boolean adjustPopulation = true; //only to write the start and end times as person attributes in the population.

	@StringGetter( "activityDurationBin" )
	public double getActivityDurationBin() {
		return activityDurationBin;
	}
	
	@StringSetter( "activityDurationBin" )
	public void setActivityDurationBin(double activityDurationBin) {
		this.activityDurationBin = activityDurationBin;
	}
	
	@StringGetter( "tolerance" )
	public double getTolerance() {
		return tolerance;
	}
	
	@StringSetter( "tolerance" )
	public void setTolerance(double tolerance) {
		this.tolerance = tolerance;
	}
	
	@StringGetter( "removeNetworkSpecificInformation" )
	public boolean isRemoveNetworkSpecificInformation() {
		return removeNetworkSpecificInformation;
	}
	
	@StringSetter( "removeNetworkSpecificInformation" )
	public void setRemoveNetworkSpecificInformation(boolean removeNetworkSpecificInformation) {
		this.removeNetworkSpecificInformation = removeNetworkSpecificInformation;
	}

	@StringGetter( "adjustPopulation" )
	public boolean isAdjustPopulation() {
		return adjustPopulation;
	}

	@StringSetter( "adjustPopulation" )
	public void setAdjustPopulation(boolean adjustPopulation) {
		this.adjustPopulation = adjustPopulation;
	}

	@StringGetter( "useAgentSpecificActivityScheduling" )
	public boolean isUseAgentSpecificActivityScheduling() {
		return useAgentSpecificActivityScheduling;
	}

	@StringSetter( "useAgentSpecificActivityScheduling" )
	public void setUseAgentSpecificActivityScheduling(boolean useAgentSpecificActivityScheduling) {
		this.useAgentSpecificActivityScheduling = useAgentSpecificActivityScheduling;
	}
	
	
	
	
}
