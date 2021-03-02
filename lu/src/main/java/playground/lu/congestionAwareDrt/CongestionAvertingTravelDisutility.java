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
import org.matsim.core.config.Config;
import org.matsim.core.events.MobsimScopeEventHandler;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.vehicles.Vehicle;

public class CongestionAvertingTravelDisutility
		implements TravelDisutility, LinkEnterEventHandler, LinkLeaveEventHandler, MobsimScopeEventHandler,
		VehicleLeavesTrafficEventHandler, VehicleEntersTrafficEventHandler {
	private final Config config;

	private final Map<Id<Link>, MutableInt> linksOccupationMap = new HashMap<>();
	private final Map<Id<Link>, Integer> previousLinksOccupationMap = new HashMap<>();
	private final Map<Id<Link>, Integer> differenceMap = new HashMap<>();

	private final double discountFactor = 0.9;
	private final double penaltyFactor = 2.0;

	private final double updatePeriod = 30;
	private final double overFlowPenaltyFactor = 50.0;

	public CongestionAvertingTravelDisutility(Config config) {
		this.config = config;
	}

	@Override
	public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
		double freeFlowTravelTIme = link.getLength() / link.getFreespeed();
		if (linksOccupationMap.containsKey(link.getId())) {
			// cost associate to the in flow
			double flowCapacityPerSec = link.getFlowCapacityPerSec() * config.qsim().getFlowCapFactor();
			double overFlow = differenceMap.getOrDefault(link.getId(), 0) / updatePeriod - flowCapacityPerSec;
			if (overFlow < 0) {
				overFlow = 0;
			}
			double overFlowPenalty = overFlow / flowCapacityPerSec * overFlowPenaltyFactor;

			// cost associate to high occupation of the link (i.e. link is almost full)
			double timeLength = link.getLength() / link.getFreespeed();
			double timeInterval = 1 / flowCapacityPerSec;
			double criticalValue = link.getNumberOfLanes() * (timeLength / timeInterval) * discountFactor;
			int occupation = linksOccupationMap.get(link.getId()).intValue();
			if (occupation < criticalValue) {
				return freeFlowTravelTIme + overFlowPenalty;
			}
			double slope = 1 / flowCapacityPerSec * penaltyFactor;
			return freeFlowTravelTIme + (occupation - criticalValue) * slope + overFlowPenalty;
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

	public void updateInFlow() {
		for (Id<Link> linkId : linksOccupationMap.keySet()) {
			int difference = linksOccupationMap.get(linkId).intValue()
					- previousLinksOccupationMap.getOrDefault(linkId, 0);
			differenceMap.put(linkId, difference);
			previousLinksOccupationMap.put(linkId, linksOccupationMap.get(linkId).intValue());
		}
	}

}
