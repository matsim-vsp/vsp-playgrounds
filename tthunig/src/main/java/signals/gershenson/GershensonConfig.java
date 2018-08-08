package signals.gershenson;

public class GershensonConfig {
	private double storageCapacityOutlinkJam = 0.8;
	//protected double maxRedTime = 15.0;
	private double interGreenTime = 5.; //TODO default should be at least 2
	//TODO this leads to reasonable results (try and error). Why did Gershenson think 13.33 was a good threshold?
	//in the tests this led to inefficient switching after the minimum green-Time.
	private double threshold = 250;
	private int lengthOfPlatoonTails = 2;
	private double minimumGREENtime = 5;  //TODO must not be negative
	
	private int minimumCycleTime = 80;
	
	//These are attributes for the Sensor
	private double monitoredPlatoonTail = 25.;
	private double monitoredDistance = 50.;
	private double minmumDistanceBehindIntersection =10.;
	
	
	
	
	public void setStorageCapacityOutlink(double storageCapacityOutlinkJam){
		this.storageCapacityOutlinkJam = storageCapacityOutlinkJam;
	}
	public double getStorageCapacityOutlinkJam() {
		return storageCapacityOutlinkJam;
	}
	
	public void setInterGreenTime(double interGreenTime){
		
	//Prevents negative intergreen times, if set by user set to 0	
		if (interGreenTime < 0){
			this.interGreenTime = 0.0;
		} else this.interGreenTime = interGreenTime;
	}
	
	public double getInterGreenTime() {
		return interGreenTime;
	}
	
	public void setThreshold(double threshold){
		this.threshold = threshold;
	}
	public double getThreshold() {
		return this.threshold;
	}
	
	public void setLengthOfPlatoonTails(int lengthOfPlatoonTails){
		this.lengthOfPlatoonTails = lengthOfPlatoonTails;
	}
	public int getLengthOfPlatoonTails() {
		return this.lengthOfPlatoonTails;
	}
	
	public void setMinimumGREENtime(double minimumGREENtime){
		this.minimumGREENtime = minimumGREENtime;
	}
	public double getMinimumGREENtime() {
		return this.minimumGREENtime;
	}
	
	//Attributes for the Sensor
	public void setMonitoredPlatoonTail(double monitoredPlatoonTail){
		this.monitoredPlatoonTail = monitoredPlatoonTail;
	}
	public double getMonitoredPlatoonTail() {
		return this.monitoredPlatoonTail;
	}
	
	public void setMonitoredDistance(double monitoredDistance){
		this.monitoredDistance = monitoredDistance;
	}
	public double getMonitoredDistance() {
		return this.monitoredDistance;
	}
	
	public void setMinmumDistanceBehindIntersection(double minmumDistanceBehindIntersection){
		this.minmumDistanceBehindIntersection = minmumDistanceBehindIntersection;
	}
	public double getMinmumDistanceBehindIntersection() {
		return this.minmumDistanceBehindIntersection;
	}
	
	public void setMinmumCycleTime(int minimumCycleTime){
		this.minimumCycleTime = minimumCycleTime;
	}
	public int getMinmumCycleTime() {
		return this.minimumCycleTime;
	}
	
	
}
