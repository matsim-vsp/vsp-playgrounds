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

import floetteroed.utilities.statisticslogging.Statistic;
import utils.RobustBivariateRegression;

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

	private final RobustBivariateRegression regression2;

	// private final Regression regression;
	// private final RecursiveMovingAverage deltaX2;
	// private final RecursiveMovingAverage anticipatedDeltaUMinusRealizedDeltaU;

	private Double lastPredictedTotalUtilityImprovement = null;

	private int observationCnt = 0;

	// -------------------- CONSTRUCTION --------------------

	ReplanningEfficiencyEstimator(final double inertia, final int minObservationCnt, final int covarianceMemoryLength) {
		// this.regression = new Regression(inertia, 2);
		this.regression2 = new RobustBivariateRegression(covarianceMemoryLength);
		this.minObservationCnt = minObservationCnt;
		// this.deltaX2 = new RecursiveMovingAverage(covarianceMemoryLength);
		// this.anticipatedDeltaUMinusRealizedDeltaU = new
		// RecursiveMovingAverage(covarianceMemoryLength);
	}

	// -------------------- IMPLEMENTATION --------------------

	void update(final Double anticipatedUtilityChange, final Double realizedUtilityChange,
			final Double anticipatedSlotUsageChange2) {
		if ((anticipatedUtilityChange != null) && (realizedUtilityChange != null)
				&& (anticipatedSlotUsageChange2 != null) && (anticipatedUtilityChange - realizedUtilityChange >= 0)) {

			// this.deltaX2.add(anticipatedSlotUsageChange2);
			// this.anticipatedDeltaUMinusRealizedDeltaU.add(anticipatedUtilityChange -
			// realizedUtilityChange);

			// final Vector regressionInput = new Vector(anticipatedSlotUsageChange2, 1.0);
			// this.lastPredictedTotalUtilityImprovement = anticipatedUtilityChange
			// - this.regression.predict(regressionInput);
			// this.regression.update(regressionInput, anticipatedUtilityChange -
			// realizedUtilityChange);

			this.lastPredictedTotalUtilityImprovement = anticipatedUtilityChange
					- (this.regression2.getSlope() * anticipatedSlotUsageChange2 + this.regression2.getOffset());
			this.regression2.add(anticipatedSlotUsageChange2, anticipatedUtilityChange - realizedUtilityChange);

			this.observationCnt++;
		}
	}

	boolean hadEnoughData() {
		return (this.observationCnt >= this.minObservationCnt);
	}

	Double getBeta() {
		// return (1.0 / this.regression.getCoefficients().get(0));
		return (1.0 / this.regression2.getSlope());

	}

	Double getDelta() {
		// return (-this.regression.getCoefficients().get(1) /
		// this.regression.getCoefficients().get(0));
		return (-this.regression2.getOffset() / this.regression2.getSlope());
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
				// final Covariance covariance = new Covariance();
				// final double[] deltaX2Data = deltaX2.getDataAsPrimitiveDoubleArray();
				// final double[] disappointmentData = anticipatedDeltaUMinusRealizedDeltaU
				// .getDataAsPrimitiveDoubleArray();
				//
				// final double cov12 = covariance.covariance(deltaX2Data, disappointmentData);
				// final double var1 = covariance.covariance(deltaX2Data, deltaX2Data);
				// final double var2 = covariance.covariance(disappointmentData,
				// disappointmentData);
				//
				// return Statistic.toString(cov12 / Math.sqrt(var1 * var2));
				return Statistic.toString(regression2.getCorrelation());
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
