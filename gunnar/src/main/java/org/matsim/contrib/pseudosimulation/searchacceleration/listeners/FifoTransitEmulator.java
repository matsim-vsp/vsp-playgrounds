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
package org.matsim.contrib.pseudosimulation.searchacceleration.listeners;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.contrib.pseudosimulation.mobsim.transitperformance.TransitEmulator;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;

import floetteroed.utilities.Tuple;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class FifoTransitEmulator implements TransitEmulator {

	private final FifoTransitPerformance fifoTransitPerformance;

	private final Map<Id<TransitLine>, TransitLine> transitLines;

	private final Map<Id<TransitStopFacility>, TransitStopFacility> stopFacilities;

	@Inject
	public FifoTransitEmulator(final FifoTransitPerformance fifoTransitPerformance, final Scenario scenario) {
		this.fifoTransitPerformance = fifoTransitPerformance;
		this.transitLines = scenario.getTransitSchedule().getTransitLines();
		this.stopFacilities = scenario.getTransitSchedule().getFacilities();
	}

	@Override
	public Trip findTrip(Leg prevLeg, double earliestDepartureTime_s) {

		final ExperimentalTransitRoute route = (ExperimentalTransitRoute) prevLeg.getRoute();
		final Id<TransitStopFacility> accessStopId = route.getAccessStopId();
		final Id<TransitStopFacility> egressStopId = route.getEgressStopId();
		final TransitLine line = this.transitLines.get(route.getLineId());
		final TransitRoute transitRoute = line.getRoutes().get(route.getRouteId());

		final Tuple<Departure, Double> accessDepartureAndTime_s = fifoTransitPerformance
				.getNextDepartureAndTime_s(line.getId(), transitRoute, accessStopId, earliestDepartureTime_s);
		if (accessDepartureAndTime_s == null) {
			return new Trip(null, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
		} else {
			Id<Vehicle> vehicleId = accessDepartureAndTime_s.getA().getVehicleId();
			double egressTime_s = accessDepartureAndTime_s.getA().getDepartureTime()
					+ transitRoute.getStop(stopFacilities.get(egressStopId)).getArrivalOffset();
			return new Trip(vehicleId, accessDepartureAndTime_s.getB(), egressTime_s);
		}
	}
}
