package playground.mdziakowski.ODMatrixBerlin;

public class TripCounter {
	
	private District startDistrict;
	private District endDistrict;
	
	private int counter = 0;
	
	public TripCounter(District sD, District eD) {
		
		this.startDistrict = sD;
		this.endDistrict = eD;		
		
	}

	public String toString() {
		return startDistrict.getName() + ", " + endDistrict.getName() + ", " + counter;
		
	}
	
	public void count() {
		this.counter++;
	}
	
	public District getStartDistrict() {
		return startDistrict;
	}

	public District getEndDistrict() {
		return endDistrict;
	}

	public int getCounter() {
		return counter;
	}
	
}
