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

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class TripTime {

	// -------------------- CONSTANTS --------------------

	final double dTT_dDptTime;

	final double ttOffset_s;

	final double minDptTime_s;

	final double maxDptTime_s;

	// -------------------- CONSTRUCTION --------------------

	TripTime(final double dTT_dDptTime, final double ttOffset_s, final double minDptTime_s, final double maxDptTime_s) {
		if (dTT_dDptTime <= -(1.0 - 1e-8)) {
			throw new RuntimeException("FIFO problem: dTT/dDptTime = " + dTT_dDptTime
					+ " is (almost) below -1.0. This means that a later departure implies an earlier "
					+ "arrival, which may cause the numerical solver problems.");
		}
		if (minDptTime_s > maxDptTime_s) {
			throw new RuntimeException(
					"Infeasible departure time interval [" + minDptTime_s + "s, " + maxDptTime_s + "s].");
		}
		this.dTT_dDptTime = dTT_dDptTime;
		this.ttOffset_s = ttOffset_s;
		this.minDptTime_s = minDptTime_s;
		this.maxDptTime_s = maxDptTime_s;
	}

	// -------------------- GETTERS --------------------

	double getTT_s(final double dptTime_s) {
		return this.dTT_dDptTime * dptTime_s + this.ttOffset_s;
	}

	double getArrTime_s(final double dptTime_s) {
		return dptTime_s + this.getTT_s(dptTime_s);
	}
}
