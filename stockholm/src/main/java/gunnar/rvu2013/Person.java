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

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class Person {

	final String personId;
	final SortedMap<Integer, Travel> travels;

	Person(final String personId) {
		this.personId = personId;
		this.travels = new TreeMap<>();
	}

	void add(TravelSegment segment) {
		Travel trip = this.travels.get(segment.travelId);
		if (trip == null) {
			trip = new Travel(segment.travelId);
			this.travels.put(trip.travelId, trip);
		}
		trip.add(segment);
	}
	
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
	
	boolean hasOnlySimpleTours() {
		for (Travel travel : this.travels.values()) {
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
		for (Travel travel : this.travels.values()) {
			result.add(travel.purpose());
		}
		return result;
	}
	
	@Override
	public String toString() {
		final StringBuffer result = new StringBuffer();
		result.append("Person " + this.personId + "\n");
		for (Travel travel : this.travels.values()) {
			result.append(travel);
		}
		return result.toString();
	}

}
