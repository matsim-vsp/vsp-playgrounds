package searchacceleration.datastructures;

import java.util.Map;

import floetteroed.utilities.DynamicData;
import floetteroed.utilities.Tuple;

/**
 * The "score" this class refers to is the anticipated change of the search
 * acceleration objective function resulting from setting a single agent's 0/1
 * re-planning indicator.
 * 
 * Implements the greedy heuristic of Merz, P. and Freisleben, B. (2002).
 * "Greedy and local search heuristics for unconstrained binary quadratic
 * programming." Journal of Heuristics 8:197–213.
 * 
 * @author Gunnar Flötteröd
 * 
 * @param L
 *            the space coordinate type
 *
 */
public class ScoreUpdater<L> {

	// -------------------- MEMBERS --------------------

	private final SpaceTimeIndicators<L> currentIndicators;

	private final SpaceTimeIndicators<L> upcomingIndicators;

	private final SpaceTimeCounts<L> currentIndividualCounts;

	private final SpaceTimeCounts<L> deltaIndividualCounts;

	private final double meanLambda;

	private final DynamicData<L> currentCounts;

	private final DynamicData<L> interactionResidual;

	private final DynamicData<L> inertiaResidual;

	private double regularizationResidual;

	private final double scoreChangeIfZero;

	private final double scoreChangeIfOne;

	private boolean residualsUpdated = false;

	// -------------------- CONSTRUCTION --------------------

	public ScoreUpdater(final SpaceTimeIndicators<L> currentIndicators, final SpaceTimeIndicators<L> upcomingIndicators,
			final double meanLambda, final DynamicData<L> currentTotalCounts,
			final double sumOfCurrentTotalCountsSquare, final double w, final double delta,
			final DynamicData<L> interactionResidual, final DynamicData<L> inertiaResidual,
			final double regularizationResidual) {

		this.currentIndicators = currentIndicators;
		this.upcomingIndicators = upcomingIndicators;

		this.meanLambda = meanLambda;
		this.currentCounts = currentTotalCounts;

		this.interactionResidual = interactionResidual;
		this.inertiaResidual = inertiaResidual;
		this.regularizationResidual = regularizationResidual;

		/*
		 * One has to go beyond 0/1 indicator arithmetics in the following
		 * because the same vehicle may enter the same link multiple times
		 * during one time bin.
		 */

		double sumOfIndividualChangeSquare = 0.0;
		double sumOfCurrentIndividualCntSquare = 0.0;
		double sumOfCurrentIndividualCntTimesCurrentTotalCnt = 0.0;
		double sumOfIndividualChangeTimesInteractionResidual = 0.0;
		double sumOfCurrentIndividualCntTimesInertiaResidual = 0.0;
		{
			this.currentIndividualCounts = new SpaceTimeCounts<>(this.currentIndicators);
			this.deltaIndividualCounts = new SpaceTimeCounts<>(this.upcomingIndicators);
			this.deltaIndividualCounts.subtract(this.currentIndividualCounts);

			for (Map.Entry<Tuple<L, Integer>, Integer> changeEntry : this.deltaIndividualCounts.entriesView()) {
				final double changeValue = changeEntry.getValue();
				sumOfIndividualChangeSquare += changeValue * changeValue;
				sumOfIndividualChangeTimesInteractionResidual += changeValue
						* interactionResidual.getBinValue(changeEntry.getKey().getA(), changeEntry.getKey().getB());
			}

			for (Map.Entry<Tuple<L, Integer>, Integer> currentEntry : this.currentIndividualCounts.entriesView()) {
				final double currentIndividualCnt = currentEntry.getValue();
				final L currentSpaceObj = currentEntry.getKey().getA();
				final Integer currentTimeBin = currentEntry.getKey().getB();
				sumOfCurrentIndividualCntSquare += currentIndividualCnt * currentIndividualCnt;
				sumOfCurrentIndividualCntTimesCurrentTotalCnt += currentIndividualCnt
						* currentTotalCounts.getBinValue(currentSpaceObj, currentTimeBin);
				sumOfCurrentIndividualCntTimesInertiaResidual += currentIndividualCnt
						* inertiaResidual.getBinValue(currentSpaceObj, currentTimeBin);
			}
		}

		final double factor1 = (sumOfIndividualChangeSquare + w * sumOfCurrentIndividualCntSquare + delta
				* (sumOfCurrentIndividualCntTimesCurrentTotalCnt * sumOfCurrentIndividualCntTimesCurrentTotalCnt)
				/ (sumOfCurrentTotalCountsSquare * sumOfCurrentTotalCountsSquare));

		final double factor2 = 2.0 * (sumOfIndividualChangeTimesInteractionResidual
				- w * (sumOfCurrentIndividualCntSquare + sumOfCurrentIndividualCntTimesInertiaResidual)
				+ delta * regularizationResidual * sumOfCurrentIndividualCntTimesCurrentTotalCnt
						/ (sumOfCurrentTotalCountsSquare * sumOfCurrentTotalCountsSquare));

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

	public void updateDynamicDataResiduals(final double newLambda) {
		if (this.residualsUpdated) {
			throw new RuntimeException("Residuals have already been updated.");
		}
		this.residualsUpdated = true;

		final double deltaLambda = newLambda - this.meanLambda;

		for (Map.Entry<Tuple<L, Integer>, Integer> entry : this.deltaIndividualCounts.entriesView()) {
			final L spaceObj = entry.getKey().getA();
			final int timeBin = entry.getKey().getB();
			this.interactionResidual.add(spaceObj, timeBin, deltaLambda * entry.getValue());

		}
		for (Map.Entry<Tuple<L, Integer>, Integer> entry : this.currentIndividualCounts.entriesView()) {
			final L spaceObj = entry.getKey().getA();
			final int timeBin = entry.getKey().getB();
			this.inertiaResidual.add(spaceObj, timeBin, -deltaLambda * entry.getValue());
			this.regularizationResidual += deltaLambda * this.currentCounts.getBinValue(spaceObj, timeBin)
					* entry.getValue();
		}
	}
}
