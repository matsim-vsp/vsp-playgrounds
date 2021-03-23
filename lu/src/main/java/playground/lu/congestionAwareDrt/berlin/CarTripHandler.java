package playground.lu.congestionAwareDrt.berlin;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.population.Person;

public class CarTripHandler implements PersonArrivalEventHandler, VehicleEntersTrafficEventHandler {
	private final Map<String, Double> carTripDataMap = new HashMap<>();
	private final Map<Id<Person>, Double> enrouteTrips = new HashMap<>();

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		enrouteTrips.put(event.getPersonId(), event.getTime());
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		double travelTime = event.getTime() - enrouteTrips.get(event.getPersonId());
		enrouteTrips.remove(event.getPersonId());
		carTripDataMap.put(event.getPersonId().toString(), travelTime);
	}

	public Map<String, Double> getCarTripDataMap() {
		return carTripDataMap;
	}

}
