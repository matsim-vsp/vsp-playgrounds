package playground.lu.unitCapacityMatching;

import java.util.Collection;

import org.matsim.contrib.drt.optimizer.VehicleData.Entry;
import org.matsim.contrib.drt.passenger.DrtRequest;

public interface VehicleSelector {
	Entry selectVehicleEntryForRequest(DrtRequest request, Collection<Entry> vehicleEntries, double timeOfTheDay);
}
