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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.facilities.ActivityFacilities;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class OnlyDepartureArrivalLegEmulator implements LegEmulator {

	protected final EventsManager eventsManager;
	protected final ActivityFacilities activityFacilities;

	public OnlyDepartureArrivalLegEmulator(final EventsManager eventsManager, ActivityFacilities activityFacilities) {
		this.eventsManager = eventsManager;
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
	public final double emulateLegAndReturnEndTime_s(final Leg leg, final Person person, final Activity previousActivity,
			final Activity nextActivity, double time_s) {

		// Every leg starts with a departure.
		this.eventsManager.processEvent(
				new PersonDepartureEvent(time_s, person.getId(), getLinkId(previousActivity), leg.getMode()));

		time_s = this.emulateBetweenDepartureAndArrival(leg, person, time_s);

		// Every leg ends with an arrival.
		this.eventsManager
				.processEvent(new PersonArrivalEvent(time_s, person.getId(), getLinkId(nextActivity), leg.getMode()));

		return time_s;
	}

	// Hook for stuff that happens between departure and arrival.
	public double emulateBetweenDepartureAndArrival(final Leg leg, final Person person, double time_s) {
		return (time_s + leg.getTravelTime());
	}
}
