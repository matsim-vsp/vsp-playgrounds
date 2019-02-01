package playground.kturner.freightKt.analyse;

//TODO: warum wird dies hier genutzt -> Nutze doch einfach CarrierVehicleType (freight contrib) kmt, mai 18
public class VehicleTypeSpezificCapabilities{
	private double fixCosts;
	private double costsPerMeter;
	private double costsPerSecond;
	private double fuelConsumtion;
	private double emissionsPerMeter; //TODO: Wird nicht mit eingelesen in CarrierVehicleTypeReader -> Eigenen Reader und eigene Klassen bauen; Zur Not erstmal per Hand setzen...
	private int capacity;
	
	VehicleTypeSpezificCapabilities(double fixCosts, double costsPerMeter,
			double costsPerSecond, double fuelConsumtion, double emissionsPerMeter, int capacity) {
		this.fixCosts = fixCosts;
		this.costsPerMeter = costsPerMeter;
		this.costsPerSecond = costsPerSecond;
		this.fuelConsumtion = fuelConsumtion;
		this.emissionsPerMeter = emissionsPerMeter;
		this.capacity = capacity;
	}

	double getFixCosts() {
		return fixCosts;
	}

	double getCostsPerMeter() {
		return costsPerMeter;
	}

	double getCostsPerSecond() {
		return costsPerSecond;
	}

	double getCapacity() {
		return capacity;
	}	
	
	double getFuelConsumtion() {
		return fuelConsumtion;
	}	
	
	double getEmissionsPerMeter() {
		return emissionsPerMeter;
	}	
}