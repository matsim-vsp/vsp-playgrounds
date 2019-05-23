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
package ch.ethz.matsim.ier.emulator;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.util.TravelTime;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.vehicles.Vehicle;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class CarLegEmulator implements LegEmulator {

	private final EventsManager eventsManager;

	private final Network network;
	private final ActivityFacilities activityFacilities;

	private final TravelTime travelTime;

	public CarLegEmulator(final EventsManager eventsManager, final Network network, final TravelTime travelTime,
			ActivityFacilities activityFacilities) {
		this.eventsManager = eventsManager;
		this.network = network;
		this.travelTime = travelTime;
		this.activityFacilities = activityFacilities;
	}

	private Id<Link> getLinkId(Activity activity) {
		if (activity.getFacilityId() != null) {
			return activityFacilities.getFacilities().get(activity.getFacilityId()).getLinkId();
		} else {
			return activity.getLinkId();
		}
	}

	@Override
	public double emulateLegAndReturnEndTime_s(final Leg leg, final Person person, final Activity previousActivity,
			final Activity nextActivity, double time_s) {

		// Every leg starts with a departure.
		this.eventsManager.processEvent(
				new PersonDepartureEvent(time_s, person.getId(), getLinkId(previousActivity), leg.getMode()));

		if (!(leg.getRoute() instanceof NetworkRoute)) {
			throw new RuntimeException(
					"Expecting a " + NetworkRoute.class.getSimpleName() + " when emulating a car leg.");
		}
		final NetworkRoute networkRoute = (NetworkRoute) leg.getRoute();
		if (!networkRoute.getStartLinkId().equals(networkRoute.getEndLinkId())) {

			final Id<Vehicle> vehicleId = Id.createVehicleId(person.getId());

			// First link of a network route.
			Link link = this.network.getLinks().get(networkRoute.getStartLinkId());
			this.eventsManager.processEvent(
					new VehicleEntersTrafficEvent(time_s, person.getId(), link.getId(), vehicleId, leg.getMode(), 0.0));
			time_s += this.travelTime.getLinkTravelTime(link, time_s, person, null);
			this.eventsManager.processEvent(new LinkLeaveEvent(time_s, vehicleId, link.getId()));

			// Intermediate links of a network route.
			for (Id<Link> linkId : networkRoute.getLinkIds()) {
				link = this.network.getLinks().get(linkId);
				this.eventsManager.processEvent(new LinkEnterEvent(time_s, vehicleId, link.getId()));
				time_s += this.travelTime.getLinkTravelTime(link, time_s, person, null);
				this.eventsManager.processEvent(new LinkLeaveEvent(time_s, vehicleId, link.getId()));
			}

			// Last link of a network route.
			this.eventsManager.processEvent(new LinkEnterEvent(time_s, vehicleId, networkRoute.getEndLinkId()));
			this.eventsManager.processEvent(new VehicleLeavesTrafficEvent(time_s, person.getId(),
					networkRoute.getEndLinkId(), vehicleId, leg.getMode(), 0.0));
		}

		// Every leg ends with an arrival.
		this.eventsManager
				.processEvent(new PersonArrivalEvent(time_s, person.getId(), getLinkId(nextActivity), leg.getMode()));

		return time_s;
	}
}
