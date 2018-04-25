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
package searchacceleration.datastructures;

import java.util.Map;

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

	private final SpaceTimeCounts<L> currentIndividualCounts;

	private final SpaceTimeCounts<L> individualChanges;

	private final DynamicData<L> currentWeightedTotalCounts;

	private final DynamicData<L> interactionResiduals;

	private final DynamicData<L> inertiaResiduals;

	private double regularizationResidual;

	private final double scoreChangeIfZero;

	private final double scoreChangeIfOne;

	private boolean residualsUpdated = false;

	// -------------------- CONSTRUCTION --------------------

	public ScoreUpdater(final SpaceTimeIndicators<L> currentIndicators, final SpaceTimeIndicators<L> upcomingIndicators,
			final double meanLambda, final DynamicData<L> currentWeightedTotalCounts,
			final double sumOfCurrentWeightedTotalCounts2, final double w, final double delta,
			final DynamicData<L> interactionResiduals, final DynamicData<L> inertiaResiduals,
			final double regularizationResidual, final Map<L, Double> linkWeights) {

		this.currentWeightedTotalCounts = currentWeightedTotalCounts;
		this.interactionResiduals = interactionResiduals;
		this.inertiaResiduals = inertiaResiduals;
		this.regularizationResidual = regularizationResidual;

		/*
		 * One has to go beyond 0/1 indicator arithmetics in the following
		 * because the same vehicle may enter the same link multiple times
		 * during one time bin.
		 */

		this.currentIndividualCounts = new SpaceTimeCounts<>(currentIndicators);
		this.individualChanges = new SpaceTimeCounts<>(upcomingIndicators);
		this.individualChanges.subtract(this.currentIndividualCounts);

		// Update the residuals.

		for (Map.Entry<Tuple<L, Integer>, Integer> entry : this.individualChanges.entriesView()) {
			final L spaceObj = entry.getKey().getA();
			final int timeBin = entry.getKey().getB();
			final double weightedIndividualChange = linkWeights.get(spaceObj) * entry.getValue();
			this.interactionResiduals.add(spaceObj, timeBin, -meanLambda * weightedIndividualChange);
		}

		for (Map.Entry<Tuple<L, Integer>, Integer> entry : this.currentIndividualCounts.entriesView()) {
			final L spaceObj = entry.getKey().getA();
			final Integer timeBin = entry.getKey().getB();
			final double weightedCurrentIndividualCount = linkWeights.get(spaceObj) * entry.getValue();
			this.inertiaResiduals.add(spaceObj, timeBin, -(1.0 - meanLambda) * weightedCurrentIndividualCount);
			this.regularizationResidual -= meanLambda * this.currentWeightedTotalCounts.getBinValue(spaceObj, timeBin)
					* weightedCurrentIndividualCount;
		}

		// Compute individual score terms.

		double sumOfWeightedIndividualChanges2 = 0.0;
		double sumOfWeightedIndividualChangesTimesInteractionResiduals = 0.0;

		for (Map.Entry<Tuple<L, Integer>, Integer> entry : this.individualChanges.entriesView()) {
			final L spaceObj = entry.getKey().getA();
			final int timeBin = entry.getKey().getB();
			final double weightedIndividualChange = linkWeights.get(spaceObj) * entry.getValue();
			sumOfWeightedIndividualChanges2 += weightedIndividualChange * weightedIndividualChange;
			sumOfWeightedIndividualChangesTimesInteractionResiduals += weightedIndividualChange
					* this.interactionResiduals.getBinValue(spaceObj, timeBin);
		}

		double sumOfWeightedCurrentIndividualCounts2 = 0.0;
		double sumOfWeightedCurrentIndividualCountsTimesWeightedCurrentTotalCounts = 0.0;
		double sumOfWeightedCurrentIndividualCountsTimesInertiaResiduals = 0.0;

		for (Map.Entry<Tuple<L, Integer>, Integer> entry : this.currentIndividualCounts.entriesView()) {
			final L spaceObj = entry.getKey().getA();
			final Integer timeBin = entry.getKey().getB();
			final double weightedCurrentIndividualCount = linkWeights.get(spaceObj) * entry.getValue();
			sumOfWeightedCurrentIndividualCounts2 += weightedCurrentIndividualCount * weightedCurrentIndividualCount;
			sumOfWeightedCurrentIndividualCountsTimesWeightedCurrentTotalCounts += weightedCurrentIndividualCount
					* this.currentWeightedTotalCounts.getBinValue(spaceObj, timeBin);
			sumOfWeightedCurrentIndividualCountsTimesInertiaResiduals += weightedCurrentIndividualCount
					* this.inertiaResiduals.getBinValue(spaceObj, timeBin);
		}

		// Compose the actual score change.

		final double factor1 = (sumOfWeightedIndividualChanges2 + w * sumOfWeightedCurrentIndividualCounts2
				+ delta * (sumOfWeightedCurrentIndividualCountsTimesWeightedCurrentTotalCounts
						* sumOfWeightedCurrentIndividualCountsTimesWeightedCurrentTotalCounts)
						/ (sumOfCurrentWeightedTotalCounts2 * sumOfCurrentWeightedTotalCounts2));

		final double factor2 = 2.0 * (sumOfWeightedIndividualChangesTimesInteractionResiduals - w
				* (sumOfWeightedCurrentIndividualCounts2 + sumOfWeightedCurrentIndividualCountsTimesInertiaResiduals)
				+ delta * this.regularizationResidual
						* sumOfWeightedCurrentIndividualCountsTimesWeightedCurrentTotalCounts
						/ (sumOfCurrentWeightedTotalCounts2 * sumOfCurrentWeightedTotalCounts2));

		this.scoreChangeIfOne = (1.0 - meanLambda * meanLambda) * factor1 + (1.0 - meanLambda) * factor2;
		this.scoreChangeIfZero = (0.0 - meanLambda * meanLambda) * factor1 + (0.0 - meanLambda) * factor2;
	}

	// -------------------- GETTERS --------------------

	public double getUpdatedRegularizationResidual() {
		if (!this.residualsUpdated) {
			throw new RuntimeException("Residuals have not yet updated.");
		}
		return this.regularizationResidual;
	}

	public double getScoreChangeIfOne() {
		return this.scoreChangeIfOne;
	}

	public double getScoreChangeIfZero() {
		return this.scoreChangeIfZero;
	}

	// -------------------- IMPLEMENTATION --------------------

	public void updateResiduals(final double newLambda) {
		if (this.residualsUpdated) {
			throw new RuntimeException("Residuals have already been updated.");
		}
		this.residualsUpdated = true;

		for (Map.Entry<Tuple<L, Integer>, Integer> entry : this.individualChanges.entriesView()) {
			final L spaceObj = entry.getKey().getA();
			final int timeBin = entry.getKey().getB();
			this.interactionResiduals.add(spaceObj, timeBin, newLambda * entry.getValue());
		}

		for (Map.Entry<Tuple<L, Integer>, Integer> entry : this.currentIndividualCounts.entriesView()) {
			final L spaceObj = entry.getKey().getA();
			final int timeBin = entry.getKey().getB();
			this.inertiaResiduals.add(spaceObj, timeBin, (1.0 - newLambda) * entry.getValue());
			this.regularizationResidual += newLambda * this.currentWeightedTotalCounts.getBinValue(spaceObj, timeBin)
					* entry.getValue();
		}
	}
}
