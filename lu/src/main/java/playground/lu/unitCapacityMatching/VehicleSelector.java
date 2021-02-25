package playground.lu.unitCapacityMatching;

import java.util.Collection;

import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.passenger.DrtRequest;

public interface VehicleSelector {
	VehicleEntry selectVehicleEntryForRequest(DrtRequest request, Collection<VehicleEntry> vehicleEntries, double timeOfTheDay);
}
