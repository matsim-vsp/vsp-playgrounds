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

import java.util.Map;

import org.matsim.contrib.greedo.datastructures.SpaceTimeCounts;
import org.matsim.contrib.greedo.datastructures.SpaceTimeIndicators;

import floetteroed.utilities.DynamicData;
import floetteroed.utilities.Tuple;

/**
 * The "score" this class refers to is the anticipated change of the search
 * acceleration objective function resulting from setting a single agent's
 * (possibly space-weighted) 0/1 re-planning indicator.
 * 
 * Implements the score used in the greedy heuristic of Merz, P. and Freisleben,
 * B. (2002). "Greedy and local search heuristics for unconstrained binary
 * quadratic programming." Journal of Heuristics 8:197–213.
 * 
 * @author Gunnar Flötteröd
 * 
 * @param L
 *            the space coordinate type
 *
 */
public class ScoreUpdater<L> {

	// -------------------- MEMBERS --------------------

	private final DynamicData<L> interactionResiduals;

	private double inertiaResidual;

	private double regularizationResidual;

	private final double individualUtilityChange;

	private final SpaceTimeCounts<L> individualWeightedChanges;

	// private final SpaceTimeCounts<L> individualUnweightedChanges;

	private final double scoreChangeIfZero;

	private final double scoreChangeIfOne;

	private boolean residualsUpdated = false;

	private double sumOfInteractionResiduals2;

	final double meanLambda;
	// final double targetLambda;

	final double ageWeight;

	// -------------------- CONSTRUCTION --------------------

	public ScoreUpdater(final SpaceTimeIndicators<L> currentIndicators, final SpaceTimeIndicators<L> upcomingIndicators,
			final double meanLambda, final double ageWeight, final double beta, final double delta,
			final DynamicData<L> interactionResiduals, final double inertiaResidual,
			final double regularizationResidual, final GreedoConfigGroup replParams,
			final double individualUtilityChange, final double totalUtilityChange, Double sumOfInteractionResiduals2,
			final double ageStratumSize) {

		this.meanLambda = meanLambda;
		// this.targetLambda = replParams.getTargetReplanningRate();
		this.ageWeight = ageWeight;

		this.interactionResiduals = interactionResiduals;
		this.inertiaResidual = inertiaResidual;
		this.regularizationResidual = regularizationResidual;

		this.individualUtilityChange = individualUtilityChange;

		/*
		 * One has to go beyond 0/1 indicator arithmetics in the following because the
		 * same vehicle may enter the same link multiple times during one time bin.
		 */
		this.individualWeightedChanges = new SpaceTimeCounts<L>(upcomingIndicators, true);
		this.individualWeightedChanges.subtract(new SpaceTimeCounts<>(currentIndicators, true));
		// {
		// final Tuple<SpaceTimeCounts<L>, SpaceTimeCounts<L>>
		// upcomingWeighedAndUnweighted = SpaceTimeCounts
		// .newWeightedAndUnweighted(upcomingIndicators);
		// this.individualWeightedChanges = upcomingWeighedAndUnweighted.getA();
		// this.individualUnweightedChanges = upcomingWeighedAndUnweighted.getB();
		// final Tuple<SpaceTimeCounts<L>, SpaceTimeCounts<L>>
		// currentWeighedAndUnweighted = SpaceTimeCounts
		// .newWeightedAndUnweighted(currentIndicators);
		// this.individualWeightedChanges.subtract(currentWeighedAndUnweighted.getA());
		// this.individualUnweightedChanges.subtract(currentWeighedAndUnweighted.getB());
		// }

		// Update the residuals.

		this.sumOfInteractionResiduals2 = sumOfInteractionResiduals2;
		sumOfInteractionResiduals2 = null; // only use the (updated) member variable!

		for (Map.Entry<Tuple<L, Integer>, Double> entry : this.individualWeightedChanges.entriesView()) {
			final L spaceObj = entry.getKey().getA();
			final int timeBin = entry.getKey().getB();
			final double weightedIndividualChange = entry.getValue();
			double oldResidual = this.interactionResiduals.getBinValue(spaceObj, timeBin);
			double newResidual = oldResidual - meanLambda * weightedIndividualChange;
			this.interactionResiduals.put(spaceObj, timeBin, newResidual);
			this.sumOfInteractionResiduals2 += newResidual * newResidual - oldResidual * oldResidual;
		}

		this.inertiaResidual -= (1.0 - meanLambda) * this.individualUtilityChange;

		// this.regularizationResidual -= (meanLambda - this.targetLambda) * (1.0 -
		// ageWeight);

		// Compute individual score terms.

		double sumOfWeightedIndividualChanges2 = 0.0;
		double sumOfWeightedIndividualChangesTimesInteractionResiduals = 0.0;

		for (Map.Entry<Tuple<L, Integer>, Double> entry : this.individualWeightedChanges.entriesView()) {
			final L spaceObj = entry.getKey().getA();
			final int timeBin = entry.getKey().getB();
			final double weightedIndividualChange = entry.getValue();
			sumOfWeightedIndividualChanges2 += weightedIndividualChange * weightedIndividualChange;
			sumOfWeightedIndividualChangesTimesInteractionResiduals += weightedIndividualChange
					* this.interactionResiduals.getBinValue(spaceObj, timeBin);
		}

		// Compute score components and score changes.

		final double interactionIfOne = this.expectedInteraction(1.0, sumOfWeightedIndividualChanges2,
				sumOfWeightedIndividualChangesTimesInteractionResiduals, this.sumOfInteractionResiduals2);
		final double interactionIfMean = this.expectedInteraction(meanLambda, sumOfWeightedIndividualChanges2,
				sumOfWeightedIndividualChangesTimesInteractionResiduals, this.sumOfInteractionResiduals2);
		final double interactionIfZero = this.expectedInteraction(0.0, sumOfWeightedIndividualChanges2,
				sumOfWeightedIndividualChangesTimesInteractionResiduals, this.sumOfInteractionResiduals2);
		final double inertiaIfOne = this.expectedInertia(1.0, individualUtilityChange, inertiaResidual);
		final double inertiaIfMean = this.expectedInertia(meanLambda, individualUtilityChange, inertiaResidual);
		final double inertiaIfZero = this.expectedInertia(0.0, individualUtilityChange, inertiaResidual);
		final double regularizationIfOne = this.expectedRegularization(1.0, regularizationResidual, ageStratumSize);
		final double regularizationIfMean = this.expectedRegularization(meanLambda, regularizationResidual,
				ageStratumSize);
		final double regularizationIfZero = this.expectedRegularization(0.0, regularizationResidual, ageStratumSize);

		this.scoreChangeIfOne = (interactionIfOne - interactionIfMean) + beta * (inertiaIfOne - inertiaIfMean)
				+ delta * (regularizationIfOne - regularizationIfMean);
		this.scoreChangeIfZero = (interactionIfZero - interactionIfMean) + beta * (inertiaIfZero - inertiaIfMean)
				+ delta * (regularizationIfZero - regularizationIfMean);
	}

	private double expectedInteraction(final double lambda, final double sumOfWeightedIndividualChanges2,
			final double sumOfWeightedIndividualChangesTimesInteractionResiduals,
			final double sumOfInteractionResiduals2) {
		return lambda * lambda * sumOfWeightedIndividualChanges2
				+ 2.0 * lambda * sumOfWeightedIndividualChangesTimesInteractionResiduals + sumOfInteractionResiduals2;
	}

	private double expectedInertia(final double lambda, final double individualUtilityChange,
			final double inertiaResidual) {
		return (1.0 - lambda) * individualUtilityChange + inertiaResidual;
	}

	private double expectedRegularization(final double lambda, final double regularizationResidual,
			final double ageStratumSize) {
		return (regularizationResidual * regularizationResidual
				+ 2.0 * regularizationResidual * (lambda - this.meanLambda) * (1.0 - this.ageWeight)
				+ Math.pow((lambda - this.meanLambda) * (1.0 - this.ageWeight), 2.0)) / ageStratumSize;
	}

	// -------------------- IMPLEMENTATION --------------------

	public void updateResiduals(final double newLambda) {
		if (this.residualsUpdated) {
			throw new RuntimeException("Residuals have already been updated.");
		}
		this.residualsUpdated = true;

		for (Map.Entry<Tuple<L, Integer>, Double> entry : this.individualWeightedChanges.entriesView()) {
			final L spaceObj = entry.getKey().getA();
			final int timeBin = entry.getKey().getB();
			final double oldResidual = this.interactionResiduals.getBinValue(spaceObj, timeBin);
			final double newResidual = oldResidual + newLambda * entry.getValue();
			this.interactionResiduals.put(spaceObj, timeBin, newResidual);
			this.sumOfInteractionResiduals2 += newResidual * newResidual - oldResidual * oldResidual;
		}
		this.inertiaResidual += (1.0 - newLambda) * this.individualUtilityChange;
		// this.regularizationResidual += (newLambda - this.meanLambda) * (1.0 -
		// this.ageWeight);
		this.regularizationResidual += (newLambda - this.meanLambda) * (1.0 - this.ageWeight);
	}

	// -------------------- GETTERS --------------------

	// public SpaceTimeCounts<L> getIndividualWeightedChanges() {
	// return this.individualWeightedChanges;
	// }

	// public SpaceTimeCounts<L> getIndividualUnweightedChanges() {
	// return this.individualUnweightedChanges;
	// }

	public double getUpdatedInertiaResidual() {
		if (!this.residualsUpdated) {
			throw new RuntimeException("Residuals have not yet updated.");
		}
		return this.inertiaResidual;
	}

	public double getUpdatedRegularizationResidual() {
		if (!this.residualsUpdated) {
			throw new RuntimeException("Residuals have not yet updated.");
		}
		return this.regularizationResidual;
	}

	public double getUpdatedSumOfInteractionResiduals2() {
		if (!this.residualsUpdated) {
			throw new RuntimeException("Residuals have not yet updated.");
		}
		return this.sumOfInteractionResiduals2;
	}

	public double getScoreChangeIfOne() {
		return this.scoreChangeIfOne;
	}

	public double getScoreChangeIfZero() {
		return this.scoreChangeIfZero;
	}
}
