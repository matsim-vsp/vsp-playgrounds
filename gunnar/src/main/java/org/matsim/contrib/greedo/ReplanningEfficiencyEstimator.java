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

/**
 *
 * @author Gunnar Flötteröd
 *
 */
class ReplanningEfficiencyEstimator {

	// -------------------- CONSTANTS --------------------

	private final int minObservationCnt;

	// -------------------- MEMBERS --------------------

	// private final Regression regression;

	private double sumOfLogOfDeltaX2 = 0.0;
	private double sumOfLogOfDeltaDeltaU = 0.0;

	private int observationCnt = 0;

	// -------------------- CONSTRUCTION --------------------

	ReplanningEfficiencyEstimator(final double inertia, final int minObservationCnt) {
		// this.regression = new Regression(inertia, 1);
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
				&& (anticipatedSlotUsageChange2 != null) && (anticipatedSlotUsageChange2 >= 1e-8)
				&& (anticipatedUtilityChange - realizedUtilityChange >= 1e-8)) {
			this.sumOfLogOfDeltaX2 += Math.log(anticipatedSlotUsageChange2);
			this.sumOfLogOfDeltaDeltaU += Math.log(anticipatedUtilityChange - realizedUtilityChange);
			// this.regression.update(new Vector(anticipatedUtilityChange -
			// realizedUtilityChange),
			// anticipatedSlotUsageChange2);
			this.observationCnt++;
			Logger.getLogger(this.getClass()).info("update accepted, leading to sumOfLogOfDeltaX2 = " + this.sumOfLogOfDeltaX2 + ", sumOfLogOfDeltaDeltaU = " + this.sumOfLogOfDeltaDeltaU + ", resulting in beta = " + this.getBeta());
		} else {
			Logger.getLogger(this.getClass()).info("update rejected");
		}
	}

	boolean hadEnoughData() {
		return (this.observationCnt >= this.minObservationCnt);
	}

	Double getBeta() {
		return Math.exp(this.sumOfLogOfDeltaX2 - this.sumOfLogOfDeltaDeltaU);
	}

	@Deprecated
	Double getDelta() {
		return 0.0;
		// if (this.hadEnoughData()) {
		// return this.regression.getCoefficients().get(1);
		// } else {
		// return null;
		// }
	}
}
