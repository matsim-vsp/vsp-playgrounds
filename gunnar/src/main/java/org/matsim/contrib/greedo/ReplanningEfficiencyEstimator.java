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

import floetteroed.utilities.math.Covariance;
import floetteroed.utilities.math.Matrix;
import floetteroed.utilities.math.Regression;
import floetteroed.utilities.math.Vector;
import floetteroed.utilities.statisticslogging.Statistic;

/**
 * Estimates a model of the following form:
 * 
 * (1 / beta) * deltaX2 + (-delta / beta) = deltaU - deltaU*
 * 
 * The regression coefficients are given by:
 * 
 * coeff0 = 1 / beta
 * 
 * coeff1 = -delta / beta
 * 
 * One hence obtains:
 * 
 * beta = 1 / coeff0
 * 
 * delta = -coeff1 * beta = -coeff1 / coeff0
 * 
 *
 * @author Gunnar Flötteröd
 *
 */
class ReplanningEfficiencyEstimator {

	// -------------------- CONSTANTS --------------------

	private final int minObservationCnt;

	// -------------------- MEMBERS --------------------

	private final Regression regression;

	private final Covariance deltaX2vsDeltaDeltaUCov;

	private Double lastPredictedTotalUtilityImprovement = null;

	private int observationCnt = 0;

	// -------------------- CONSTRUCTION --------------------

	ReplanningEfficiencyEstimator(final double inertia, final int minObservationCnt) {
		this.regression = new Regression(inertia, 2);
		this.deltaX2vsDeltaDeltaUCov = new Covariance(2, 2);
		this.minObservationCnt = minObservationCnt;
	}

	// -------------------- IMPLEMENTATION --------------------

	void update(final Double anticipatedUtilityChange, final Double realizedUtilityChange,
			final Double anticipatedSlotUsageChange2) {
		if ((anticipatedUtilityChange != null) && (realizedUtilityChange != null)
				&& (anticipatedSlotUsageChange2 != null) && (anticipatedUtilityChange - realizedUtilityChange >= 0)) {
			{
				final Vector regressionInput = new Vector(anticipatedSlotUsageChange2, 1.0);
				this.lastPredictedTotalUtilityImprovement = anticipatedUtilityChange
						- this.regression.predict(regressionInput);
				this.regression.update(regressionInput, anticipatedUtilityChange - realizedUtilityChange);
			}
			{
				final Vector x = new Vector(anticipatedSlotUsageChange2,
						anticipatedUtilityChange - realizedUtilityChange);
				this.deltaX2vsDeltaDeltaUCov.add(x, x);
			}
			this.observationCnt++;
		}
	}

	boolean hadEnoughData() {
		return (this.observationCnt >= this.minObservationCnt);
	}

	Double getBeta() {
		return (1.0 / this.regression.getCoefficients().get(0));
	}

	Double getDelta() {
		return (-this.regression.getCoefficients().get(1) / this.regression.getCoefficients().get(0));
	}

	// --------------- IMPLEMENTATION OF Statistic FACTORIES ---------------

	public Statistic<LogDataWrapper> newDeltaXvsDeltaDeltaUStatistic() {
		return new Statistic<LogDataWrapper>() {

			@Override
			public String label() {
				return "Corr(DeltaX2,DeltaU-DeltaU*)";
			}

			@Override
			public String value(LogDataWrapper arg0) {
				final Matrix _C = deltaX2vsDeltaDeltaUCov.getCovariance();
				return Statistic.toString(_C.get(1, 0) / Math.sqrt(_C.get(0, 0) * _C.get(1, 1)));
			}
		};
	}

	public Statistic<LogDataWrapper> newAvgPredictedDeltaUtility() {
		return new Statistic<LogDataWrapper>() {

			@Override
			public String label() {
				return "PredictedMeanUtilityImprovement";
			}

			@Override
			public String value(LogDataWrapper arg0) {
				if (lastPredictedTotalUtilityImprovement != null) {
					return Statistic.toString(lastPredictedTotalUtilityImprovement / arg0.getPopulationSize());
				} else {
					return Statistic.toString(null);
				}
			}
		};
	}

}
