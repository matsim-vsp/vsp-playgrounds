/*
 * Copyright 2018 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.flotterod@gmail.com
 *
 */
package org.matsim.contrib.ier.emulator;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class FifoTransitLegEmulator implements LegEmulator {

	// -------------------- MEMBERS --------------------

	private final EventsManager eventsManager;

	private final FifoTransitPerformance fifoTransitPerformance;

	private final Map<Id<TransitLine>, TransitLine> transitLines;

	private final Map<Id<TransitStopFacility>, TransitStopFacility> stopFacilities;

	// -------------------- CONSTRUCTION --------------------

	public FifoTransitLegEmulator(final EventsManager eventsManager, final FifoTransitPerformance fifoTransitPerformance,
			final Scenario scenario) {
		this.eventsManager = eventsManager;
		this.fifoTransitPerformance = fifoTransitPerformance;
		this.transitLines = scenario.getTransitSchedule().getTransitLines();
		this.stopFacilities = scenario.getTransitSchedule().getFacilities();
	}

	// -------------------- IMPLEMENTATION OF LegEmulator --------------------

	@Override
	public double emulateLegAndReturnEndTime_s(Leg leg, Person person, Activity previousActivity,
			Activity followingActivity, double time_s) {

		final ExperimentalTransitRoute route = (ExperimentalTransitRoute) leg.getRoute();
		final Id<TransitStopFacility> accessStopId = route.getAccessStopId();
		final Id<TransitStopFacility> egressStopId = route.getEgressStopId();
		final TransitLine line = this.transitLines.get(route.getLineId());
		final TransitRoute transitRoute = line.getRoutes().get(route.getRouteId());

		final Tuple<Departure, Double> accessDepartureAndTime_s = this.fifoTransitPerformance
				.getNextDepartureAndTime_s(line.getId(), transitRoute, accessStopId, time_s);
		if (accessDepartureAndTime_s == null) {
			return Double.POSITIVE_INFINITY;
		} else {
			final double accessTime_s = accessDepartureAndTime_s.getFirst().getDepartureTime()
					+ transitRoute.getStop(this.stopFacilities.get(accessStopId)).getDepartureOffset();
			final double egressTime_s = accessDepartureAndTime_s.getFirst().getDepartureTime()
					+ transitRoute.getStop(this.stopFacilities.get(egressStopId)).getArrivalOffset();
			this.eventsManager.processEvent(new PersonEntersVehicleEvent(accessTime_s, person.getId(), null));
			this.eventsManager.processEvent(new PersonLeavesVehicleEvent(egressTime_s, person.getId(), null));
			return egressTime_s;
		}
	}
}
