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
package org.matsim.contrib.greedo;

import org.apache.log4j.Logger;

import floetteroed.utilities.math.Regression;
import floetteroed.utilities.math.Vector;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
class ReplanningEfficiencyEstimator {

	// -------------------- CONSTANTS --------------------

	private final int minObservationCnt;

	// -------------------- MEMBERS --------------------

	private final Regression regression;

	private int observationCnt = 0;

	// -------------------- CONSTRUCTION --------------------

	ReplanningEfficiencyEstimator(final double inertia, final int minObservationCnt) {
		this.regression = new Regression(inertia, 2);
		this.minObservationCnt = minObservationCnt;
	}

	// -------------------- IMPLEMENTATION --------------------

	void update(final Double anticipatedUtilityChange, final Double realizedUtilityChange,
			final Double anticipatedSlotUsageChange2) {
		Logger.getLogger(this.getClass())
				.info("update with anticipatedUtilityChange = " + anticipatedUtilityChange
						+ ", realizedUtilityChange = " + realizedUtilityChange + ", anticipatedSlotUsageChange2 = "
						+ anticipatedSlotUsageChange2 + ", observationCnt = " + this.observationCnt);
		if ((anticipatedUtilityChange != null) && (realizedUtilityChange != null)
				&& (anticipatedSlotUsageChange2 != null) && (anticipatedUtilityChange - realizedUtilityChange >= 0)) {
			this.regression.update(new Vector(anticipatedSlotUsageChange2, 1.0),
					anticipatedUtilityChange - realizedUtilityChange);
			this.observationCnt++;
			Logger.getLogger(this.getClass()).info("update accepted, leading to 1/beta = " + (1.0 / this.getBeta())
					+ " based on in total " + this.observationCnt + " observations.");
		} else {
			Logger.getLogger(this.getClass()).info("update rejected");
		}
	}

	boolean hadEnoughData() {
		return (this.observationCnt >= this.minObservationCnt);
	}

	Double getBeta() {
		return (1.0 / this.regression.getCoefficients().get(0));
	}

	Double getDelta() {
		return this.regression.getCoefficients().get(1);
	}
}
