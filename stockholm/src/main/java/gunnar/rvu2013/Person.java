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
package gunnar.rvu2013;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import cadyts.utilities.misc.Time;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class Person {

	final String personId;
	final SortedMap<Integer, Trip> travels;

	Person(final String personId) {
		this.personId = personId;
		this.travels = new TreeMap<>();
	}

	void add(TravelSegment segment) {
		Trip trip = this.travels.get(segment.travelId);
		if (trip == null) {
			trip = new Trip(segment.travelId);
			this.travels.put(trip.tripId, trip);
		}
		trip.add(segment);
	}

	@Override
	public String toString() {
		final StringBuffer result = new StringBuffer();

		result.append("Person " + this.personId + "\n");

		List<Tour> tours = this.tours(this.startLocation());
		if (tours != null) {
			for (Tour tour : tours) {
				result.append("  Tour (unique purpose=" + tour.uniquePurpose() + ")\n");
				result.append(tour);
			}
		}
		// for (Trip travel : this.travels.values()) {
		// result.append(travel);
		// }

		return result.toString();
	}

	// -------------------- ANALYSIS BELOW --------------------

	String startLocation() {
		if (this.travels.size() == 0) {
			return null;
		} else {
			return this.travels.get(this.travels.firstKey()).startLocation();
		}
	}

	String endLocation() {
		if (this.travels.size() == 0) {
			return null;
		} else {
			return this.travels.get(this.travels.lastKey()).endLocation();
		}
	}

	List<TravelSegment> segments() {
		final List<TravelSegment> result = new ArrayList<>();
		for (Trip trip : this.travels.values()) {
			result.addAll(trip.segments);
		}
		return result;
	}

	List<Tour> tours(final String homeLocation) {

		final List<Tour> tours = new ArrayList<>();
		if (homeLocation == null) {
			return tours;
		}

		Tour currentTour = null;
		for (TravelSegment segment : this.segments()) {

			if (homeLocation.equals(segment.startLocation)) {

				if (currentTour != null) {
					return new ArrayList<>(0);
				}
				currentTour = new Tour();
				currentTour.segments.add(segment);

			} else if (homeLocation.equals(segment.endLocation)) {

				if (currentTour == null) {
					return new ArrayList<>(0);
				}
				currentTour.segments.add(segment);
				tours.add(currentTour);
				currentTour = null;

			} else {

				if (currentTour != null) {
					currentTour.segments.add(segment);
				}

			}
		}

		return tours;
	}

	boolean hasOnlySimpleTours() {
		for (Trip travel : this.travels.values()) {
			if (!travel.isSimpleTour()) {
				return false;
			}
		}
		return true;
	}

	boolean isRoundTrip() {
		return ((this.travels.size() > 0) && this.startLocation().equals(this.endLocation()));
	}

	List<String> purposeSeq() {
		final List<String> result = new ArrayList<>(this.travels.size());
		for (Trip travel : this.travels.values()) {
			result.add(travel.purpose());
		}
		return result;
	}

}
