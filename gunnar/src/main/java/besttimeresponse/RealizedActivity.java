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
package besttimeresponse;

import static java.lang.Double.NEGATIVE_INFINITY;
import static java.lang.Double.POSITIVE_INFINITY;

import floetteroed.utilities.Units;
import floetteroed.utilities.math.MathHelpers;

/**
 * 
 * @author Gunnar Flötteröd
 *
 * @param L
 *            the location type (generic such that both link-to-link and
 *            zone-to-zone are supported)
 * @param M
 *            the mode type
 */
class RealizedActivity<L, M> {

	// -------------------- CONSTANTS --------------------

	final PlannedActivity<L, M> plannedActivity;

	final TripTime nextTripTravelTime;

	final double realizedArrTime_s;

	final double realizedDptTime_s;

	// -------------------- CONSTRUCTION --------------------

	RealizedActivity(final PlannedActivity<L, M> plannedActivity, final TripTime nextTripTimes,
			final double realizedArrTime_s, final double realizedDptTime_s) {
		this.plannedActivity = plannedActivity;
		this.nextTripTravelTime = nextTripTimes;
		this.realizedArrTime_s = realizedArrTime_s;
		this.realizedDptTime_s = realizedDptTime_s;
	}

	// -------------------- GETTERS --------------------

	boolean isLateArrival() {
		return this.plannedActivity.isLateArrival(this.realizedArrTime_s);
	}

	boolean isEarlyDeparture() {
		return this.plannedActivity.isEarlyDeparture(this.realizedDptTime_s);
	}

	boolean isClosedAtArrival() {
		return this.plannedActivity.isClosed(this.realizedArrTime_s);
	}

	boolean isClosedAtDeparture() {
		return this.plannedActivity.isClosed(this.realizedDptTime_s);
	}

	double effectiveDuration_s() {
		final double result;
		if (this.realizedArrTime_s > this.realizedDptTime_s) {
			// An overnight activity, always open.
			result = this.realizedDptTime_s + (Units.S_PER_D - this.realizedArrTime_s);
		} else {
			// A within-day activity, possibly closed.
			result = MathHelpers.overlap(this.realizedArrTime_s, this.realizedDptTime_s,
					(this.plannedActivity.openingTime_s != null) ? this.plannedActivity.openingTime_s
							: NEGATIVE_INFINITY,
					(this.plannedActivity.closingTime_s != null) ? this.plannedActivity.closingTime_s
							: POSITIVE_INFINITY);
		}
		return result;
	}
}
