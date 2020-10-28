package unitCapacityMatching;

import java.util.Collection;

import org.matsim.contrib.drt.optimizer.VehicleData.Entry;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

public interface VehicleSelector {
	DvrpVehicle selectVehicleForRequest(DrtRequest request, Collection<Entry> vehicleEntries, double timeOfTheDay);
}
