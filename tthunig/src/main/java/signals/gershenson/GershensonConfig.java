package signals.gershenson;

public class GershensonConfig {
	private double storageCapacityOutlinkJam = 0.8;
	//protected double maxRedTime = 15.0;
	private double interGreenTime = 5.; //TODO default should be at least 2
	private double threshold = 250; //TODO should be higher
	
	private int lengthOfPlatoonTails = 2;
	private double minimumGREENtime = 5;  //TODO should be higher maybe 20
	
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
		this.interGreenTime = interGreenTime;
	}
	public double getInterGreenTime() {
		return interGreenTime;
	}
	
	public void setThreshold(double threshold){
		this.threshold = threshold;
	}
	public double getThreshold() {
		return threshold;
	}
	
	public void setLengthOfPlatoonTails(int lengthOfPlatoonTails){
		this.lengthOfPlatoonTails = lengthOfPlatoonTails;
	}
	public int getLengthOfPlatoonTails() {
		return lengthOfPlatoonTails;
	}
	
	public void setMinimumGREENtime(double minimumGREENtime){
		this.minimumGREENtime = minimumGREENtime;
	}
	public double getMinimumGREENtime() {
		return minimumGREENtime;
	}
	
	//Attributes for the Sensor
	public void setMonitoredPlatoonTail(double monitoredPlatoonTail){
		this.monitoredPlatoonTail = monitoredPlatoonTail;
	}
	public double getMonitoredPlatoonTail() {
		return monitoredPlatoonTail;
	}
	
	public void setMonitoredDistance(double monitoredDistance){
		this.monitoredDistance = monitoredDistance;
	}
	public double getMonitoredDistance() {
		return monitoredDistance;
	}
	
	public void setMinmumDistanceBehindIntersection(double minmumDistanceBehindIntersection){
		this.minmumDistanceBehindIntersection = minmumDistanceBehindIntersection;
	}
	public double getMinmumDistanceBehindIntersection() {
		return minmumDistanceBehindIntersection;
	}
}
