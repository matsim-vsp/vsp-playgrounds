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

import java.util.SortedSet;
import java.util.TreeSet;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class Travel implements Comparable<Travel> {

	final Integer travelId;
	final SortedSet<TravelSegment> segments;

	Travel(final Integer travelId) {
		this.travelId = travelId;
		this.segments = new TreeSet<>();
	}

	void add(TravelSegment segment) {
		this.segments.add(segment);
	}

	boolean isSimpleTour() {
		return ((this.segments.size() == 2)
				&& (this.segments.first().startLocation.equals(this.segments.last().endLocation))
				&& (this.segments.first().endTime_s < this.segments.last().startTime_s));
	}
	
	String purpose() {
		if (this.segments == null) {
			return null;
		} else {
			return this.segments.last().purpose;
		}
	}
	
	String startLocation() {
		if (this.segments == null) {
			return null;
		} else {
			return this.segments.first().startLocation;
		}
	}
	
	String endLocation() {
		if (this.segments == null) {
			return null;			
		} else {
			return this.segments.last().endLocation;
		}
	}

	@Override
	public String toString() {
		final StringBuffer result = new StringBuffer();
		result.append("  Travel " + travelId + "\n");
		for (TravelSegment segment : this.segments) {
			result.append(segment);
		}
		return result.toString();
	}

	@Override
	public int compareTo(Travel o) {
		return this.travelId.compareTo(o.travelId);
	}

}
