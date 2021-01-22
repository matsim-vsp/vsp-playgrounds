package playground.lu.congestionAwareDrt;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableInt;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.MobsimScopeEventHandler;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.vehicles.Vehicle;

public class CongestionAvertingTravelDisutility
		implements TravelDisutility, LinkEnterEventHandler, LinkLeaveEventHandler, MobsimScopeEventHandler,
		VehicleLeavesTrafficEventHandler, VehicleEntersTrafficEventHandler {
	private final Map<Id<Link>, MutableInt> linksOccupationMap = new HashMap<>();

	private final double discountFactor = 0.7;
	private final double penaltyFacotr = 4.0;
	private final double vehicleLength = 7.5;

	public CongestionAvertingTravelDisutility() {
		// Currently empty
	}

	@Override
	public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
		System.out.println("There are " + linksOccupationMap.keySet().size() + " entries in link Occupation Map");
		//Always zero!!!!!!
		
		double freeFlowTravelTIme = link.getLength() / link.getFreespeed();
		if (linksOccupationMap.containsKey(link.getId())) {
			double criticalValue = (link.getLength() * link.getNumberOfLanes() / vehicleLength) * discountFactor;
			int occupation = linksOccupationMap.get(link.getId()).intValue();
			if (occupation < criticalValue) {
				return freeFlowTravelTIme;
			}
			double slope = 1 / link.getFlowCapacityPerSec() * penaltyFacotr;
			return freeFlowTravelTIme + (occupation - criticalValue) * slope;
		}
		return freeFlowTravelTIme;
	}

	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		return link.getLength() / link.getFreespeed();
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		linksOccupationMap.putIfAbsent(event.getLinkId(), new MutableInt());
		linksOccupationMap.get(event.getLinkId()).increment();
		if (event.getLinkId().toString().equals("147")) {
			System.out.println("number of vehicles at link 147 = "
					+ linksOccupationMap.get(Id.create(147, Link.class)).intValue()); // TODO
			// delete
			System.out.println("*** number of vehicles at link 147 = "
					+ linksOccupationMap.getOrDefault(Id.create(147, Link.class), new MutableInt()).intValue());
		}
		System.out.println("There are " + linksOccupationMap.keySet().size() + " entries in link Occupation Map");
		// The output from this part is normal!
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		linksOccupationMap.get(event.getLinkId()).decrement();
	}

	@Override
	public void cleanupAfterMobsim(int iteration) {
		linksOccupationMap.clear();
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		linksOccupationMap.putIfAbsent(event.getLinkId(), new MutableInt());
		linksOccupationMap.get(event.getLinkId()).increment();
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		linksOccupationMap.get(event.getLinkId()).decrement();
	}

	// TODO delete this
	public Map<Id<Link>, MutableInt> getLinkOccupationMap() {
		return linksOccupationMap; //This one is always emtpy!!!!!!
	}

}
