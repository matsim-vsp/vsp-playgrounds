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
import utils.RecursiveMovingAverage;

/**
 * Estimates a model of the following form:
 * 
 * (1 / beta) * deltaX2 + (-delta / beta) = deltaU - deltaU*
 * 
 * 
 * The regression coefficients are given by:
 * 
 * coeff0 = 1 / beta
 * 
 * coeff1 = -delta / beta
 * 
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

	private final RecursiveMovingAverage anticipatedSlotUsageChanges2;

	private final RecursiveMovingAverage anticipatedMinusRealizedUtilityChanges;
	
	private final boolean onlyShortTerm;

	private Double betaShortTerm = null;

	private Double deltaShortTerm = null;

	private Double beta = null;

	private Double delta = null;

	private final Regression betaDeltaGeneratingRegression;

	private Double correlationShortTerm = null;

	private Double currentPredictedTotalUtilityImprovement = null;

	private Double upcomingPredictedTotalUtilityImprovement = null;

	// -------------------- CONSTRUCTION --------------------

	ReplanningEfficiencyEstimator(final int minObservationCnt, final int memoryLength,
			final boolean constrainDeltaToZero, final boolean onlyShortTerm) {
		this.betaDeltaGeneratingRegression = new Regression(1.0, constrainDeltaToZero ? 1 : 2);
		this.minObservationCnt = minObservationCnt;
		this.anticipatedSlotUsageChanges2 = new RecursiveMovingAverage(memoryLength);
		this.anticipatedMinusRealizedUtilityChanges = new RecursiveMovingAverage(memoryLength);
		this.onlyShortTerm = onlyShortTerm;
	}

	// -------------------- INTERNALS --------------------

	private boolean constrainDeltaToZero() {
		return (this.betaDeltaGeneratingRegression.getDimension() == 1);
	}

	private Vector regrInput(final double deltaX2) {
		if (this.constrainDeltaToZero()) {
			return new Vector(deltaX2);
		} else {
			return new Vector(deltaX2, 1.0);
		}

	}

	// -------------------- IMPLEMENTATION --------------------

	void update(final Double anticipatedUtilityChange, final Double realizedUtilityChange,
			final Double anticipatedSlotUsageChange2) {

		if ((anticipatedUtilityChange != null) && (realizedUtilityChange != null)
				&& (anticipatedSlotUsageChange2 != null) && (anticipatedUtilityChange - realizedUtilityChange >= 0)) {

			// Short-term statistics.

			this.anticipatedSlotUsageChanges2.add(anticipatedSlotUsageChange2);
			this.anticipatedMinusRealizedUtilityChanges.add(anticipatedUtilityChange - realizedUtilityChange);
			final double[] deltaX2 = this.anticipatedSlotUsageChanges2.getDataAsPrimitiveDoubleArray();
			final double[] deltaDeltaU = this.anticipatedMinusRealizedUtilityChanges.getDataAsPrimitiveDoubleArray();
			final Regression regr = new Regression(1.0, this.constrainDeltaToZero() ? 1 : 2);
			final Covariance cov = new Covariance(2, 2);
			for (int i = 0; i < deltaX2.length; i++) {
				regr.update(this.regrInput(deltaX2[i]), deltaDeltaU[i]);
				final Vector covInput = new Vector(deltaX2[i], deltaDeltaU[i]);
				cov.add(covInput, covInput);
			}
			if (this.hadEnoughData()) {
				this.betaShortTerm = (1.0 / regr.getCoefficients().get(0));
				this.deltaShortTerm = (this.constrainDeltaToZero() ? 0.0
						: (-regr.getCoefficients().get(1) * this.betaShortTerm));
				final Matrix _C = cov.getCovariance();
				this.correlationShortTerm = _C.get(1, 0) / Math.sqrt(_C.get(0, 0) * _C.get(1, 1));
			}

			// Long-term statistics.

			this.upcomingPredictedTotalUtilityImprovement = this.betaDeltaGeneratingRegression
					.predict(this.regrInput(anticipatedSlotUsageChange2));
			this.betaDeltaGeneratingRegression.update(this.regrInput(anticipatedSlotUsageChange2),
					anticipatedUtilityChange - realizedUtilityChange);
			if (this.hadEnoughData()) {
				this.beta = (1.0 / this.betaDeltaGeneratingRegression.getCoefficients().get(0));
				this.delta = (this.constrainDeltaToZero() ? 0.0
						: (-this.betaDeltaGeneratingRegression.getCoefficients().get(1) * this.beta));
				this.currentPredictedTotalUtilityImprovement = this.upcomingPredictedTotalUtilityImprovement;
			}
		}
	}

	private boolean hadEnoughData() {
		return (this.anticipatedSlotUsageChanges2.size() >= this.minObservationCnt);
	}

	Double getBetaShortTerm() {
		return this.betaShortTerm;
	}

	Double getDeltaShortTerm() {
		return this.deltaShortTerm;
	}

	Double getBeta() {
		return (this.onlyShortTerm ? this.betaShortTerm : this.beta);
	}

	Double getDelta() {
		return (this.onlyShortTerm ? this.deltaShortTerm : this.delta);
	}

	// --------------- IMPLEMENTATION OF Statistic FACTORIES ---------------

	public Statistic<LogDataWrapper> newDeltaX2vsDeltaDeltaUStatistic() {
		return new Statistic<LogDataWrapper>() {

			@Override
			public String label() {
				return "Corr(DeltaX2,DeltaU-DeltaU*)";
			}

			@Override
			public String value(LogDataWrapper arg0) {
				return Statistic.toString(correlationShortTerm);
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
				if (currentPredictedTotalUtilityImprovement != null) {
					return Statistic.toString(currentPredictedTotalUtilityImprovement / arg0.getPopulationSize());
				} else {
					return Statistic.toString(null);
				}
			}
		};
	}

	public Statistic<LogDataWrapper> newBetaShortTerm() {
		return new Statistic<LogDataWrapper>() {
			@Override
			public String label() {
				return "BetaShortTerm";
			}

			@Override
			public String value(LogDataWrapper arg0) {
				return Statistic.toString(betaShortTerm);
			}
		};
	}

	public Statistic<LogDataWrapper> newDeltaShortTerm() {
		return new Statistic<LogDataWrapper>() {
			@Override
			public String label() {
				return "DeltaShortTerm";
			}

			@Override
			public String value(LogDataWrapper arg0) {
				return Statistic.toString(deltaShortTerm);
			}
		};
	}
}
