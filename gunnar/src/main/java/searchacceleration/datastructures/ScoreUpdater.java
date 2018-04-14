package searchacceleration.datastructures;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import floetteroed.utilities.DynamicData;
import floetteroed.utilities.TimeDiscretization;
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

	private final SpaceTimeIndicatorVectorListBased<L> currentIndicators;

	private final SpaceTimeIndicatorVectorListBased<L> upcomingIndicators;

	private final double meanLambda;

	private final DynamicData<L> currentCounts;

	private final DynamicData<L> interactionResidual;

	private final DynamicData<L> inertiaResidual;

	private double regularizationResidual;

	private final double scoreChangeIfZero;

	private final double scoreChangeIfOne;

	private boolean residualsUpdated = false;

	// -------------------- CONSTRUCTION --------------------

	public ScoreUpdater(final SpaceTimeIndicatorVectorListBased<L> currentIndicators,
			final SpaceTimeIndicatorVectorListBased<L> upcomingIndicators, final double meanLambda,
			final DynamicData<L> currentCounts, final double currentCountsSumOfSquares, final double w,
			final double delta, final DynamicData<L> interactionResidual, final DynamicData<L> inertiaResidual,
			final double regularizationResidual) {

		this.currentIndicators = currentIndicators;
		this.upcomingIndicators = upcomingIndicators;

		this.meanLambda = meanLambda;
		this.currentCounts = currentCounts;

		this.interactionResidual = interactionResidual;
		this.inertiaResidual = inertiaResidual;
		this.regularizationResidual = regularizationResidual;

		/*
		 * One has to go beyond 0/1 indicator arithmetics here because the same
		 * vehicle may enter the same link multiple times during one time bin.
		 * Hence the change of data structure.
		 */
		double changeSumOfSquares = 0.0;
		double currentSumOfSquares = 0.0;
		double currentTimesCounts = 0.0;
		double changeTimesInteractionResiduals = 0.0;
		double currentTimesCurrentPlusInertiaResiduals = 0.0;
		{
			final SpaceTimeVectorMapBased<L> currentTmp = new SpaceTimeVectorMapBased<>(this.currentIndicators);
			final SpaceTimeVectorMapBased<L> deltaTmp = new SpaceTimeVectorMapBased<>(this.upcomingIndicators);
			deltaTmp.subtract(currentTmp);

			for (Map.Entry<Tuple<L, Integer>, Integer> changeEntry : deltaTmp.getEntryView()) {
				final double changeValue = changeEntry.getValue();
				changeSumOfSquares += changeValue * changeValue;
				changeTimesInteractionResiduals += changeValue
						* interactionResidual.getBinValue(changeEntry.getKey().getA(), changeEntry.getKey().getB());
			}

			for (Map.Entry<Tuple<L, Integer>, Integer> currentEntry : currentTmp.getEntryView()) {
				final double currentValue = currentEntry.getValue();
				final L currentSpaceObj = currentEntry.getKey().getA();
				final Integer currentTimeBin = currentEntry.getKey().getB();
				currentSumOfSquares += currentValue * currentValue;
				currentTimesCounts += currentValue * currentCounts.getBinValue(currentSpaceObj, currentTimeBin);
				currentTimesCurrentPlusInertiaResiduals += currentValue
						* (currentValue + inertiaResidual.getBinValue(currentSpaceObj, currentTimeBin));
			}
		}

		final double fact1 = (changeSumOfSquares + w * currentSumOfSquares
				+ delta * currentTimesCounts / (currentCountsSumOfSquares * currentCountsSumOfSquares));
		final double fact2 = 2.0 * (changeTimesInteractionResiduals - w * currentTimesCurrentPlusInertiaResiduals
				+ delta * regularizationResidual * currentTimesCounts
						/ (currentCountsSumOfSquares * currentCountsSumOfSquares));

		this.scoreChangeIfOne = (1.0 - meanLambda * meanLambda) * fact1 + (1.0 - meanLambda) * fact2;
		this.scoreChangeIfZero = (0.0 - meanLambda * meanLambda) * fact1 + (0.0 - meanLambda) * fact2;
	}

	// -------------------- IMPLEMENTATION --------------------

	public static <L> DynamicData<L> newCounts(final TimeDiscretization timeDiscr,
			final Collection<SpaceTimeIndicatorVectorListBased<L>> allIndicators) {
		final DynamicData<L> result = new DynamicData<L>(timeDiscr);
		for (SpaceTimeIndicatorVectorListBased<L> indicators : allIndicators) {
			for (int bin = 0; bin < timeDiscr.getBinCnt(); bin++) {
				for (L locObj : indicators.getVisitedSpaceObjects(bin)) {
					result.add(locObj, bin, 1.0);
				}
			}
		}
		return result;
	}

	// TODO extract generic functionality into DynamicData
	public static <L> double sumOfSquareCounts(final DynamicData<L> counts) {
		double result = 0.0;
		for (L locObj : counts.keySet()) {
			for (int bin = 0; bin < counts.getBinCnt(); bin++) {
				final double val = counts.getBinValue(locObj, bin);
				result += val * val;
			}
		}
		return result;
	}

	public static <L> double sumOfSquareDeltaCounts(final DynamicData<L> counts1, final DynamicData<L> counts2) {
		double result = 0.0;
		final Set<L> allLocObj = new LinkedHashSet<>(counts1.keySet());
		allLocObj.addAll(counts2.keySet());
		for (L locObj : allLocObj) {
			for (int bin = 0; bin < counts1.getBinCnt(); bin++) {
				final double delta = counts1.getBinValue(locObj, bin) - counts2.getBinValue(locObj, bin);
				result += delta * delta;
			}
		}
		return result;
	}

	// TODO extract generic functionality into DynamicData
	public static <L> DynamicData<L> newInteractionResidual(final DynamicData<L> currentCounts,
			final DynamicData<L> newCounts, final double meanLambda) {
		final DynamicData<L> result = new DynamicData<L>(currentCounts.getStartTime_s(), currentCounts.getBinSize_s(),
				currentCounts.getBinCnt());
		final Set<L> allLocObjs = new LinkedHashSet<>(currentCounts.keySet());
		allLocObjs.addAll(newCounts.keySet());
		for (L locObj : allLocObjs) {
			for (int bin = 0; bin < currentCounts.getBinCnt(); bin++) {
				result.put(locObj, bin,
						meanLambda * (newCounts.getBinValue(locObj, bin) - currentCounts.getBinValue(locObj, bin)));
			}
		}
		return result;
	}

	// TODO extract generic functionality into DynamicData
	public static <L> DynamicData<L> newInertiaResidual(final DynamicData<L> currentCounts, final double meanLambda) {
		final DynamicData<L> result = new DynamicData<L>(currentCounts.getStartTime_s(), currentCounts.getBinSize_s(),
				currentCounts.getBinCnt());
		for (L locObj : currentCounts.keySet()) {
			for (int bin = 0; bin < currentCounts.getBinCnt(); bin++) {
				result.put(locObj, bin, (1.0 - meanLambda) * currentCounts.getBinValue(locObj, bin));
			}
		}
		return result;
	}

	public void updateDynamicDataResiduals(final double newLambda) {

		if (this.residualsUpdated) {
			throw new RuntimeException("Residuals have already been updated.");
		}
		this.residualsUpdated = true;

		final double deltaLambda = newLambda - this.meanLambda;
		for (int bin = 0; bin < this.currentIndicators.getTimeBinCnt(); bin++) {
			for (L currentSpaceObj : this.upcomingIndicators.getVisitedSpaceObjects(bin)) {
				this.interactionResidual.add(currentSpaceObj, bin, +deltaLambda);
			}
			for (L currentSpaceObj : this.currentIndicators.getVisitedSpaceObjects(bin)) {
				this.interactionResidual.add(currentSpaceObj, bin, -deltaLambda);
				this.inertiaResidual.add(currentSpaceObj, bin, -deltaLambda);
			}
			for (L currentSpaceObj : this.upcomingIndicators.getVisitedSpaceObjects(bin)) {
				this.regularizationResidual += deltaLambda * this.currentCounts.getBinValue(currentSpaceObj, bin);
			}
		}
	}

	// -------------------- GETTERS --------------------

	public double getRegularizationResidual() {
		return this.regularizationResidual;
	}

	public double getScoreChangeIfOne() {
		return this.scoreChangeIfOne;
	}

	public double getScoreChangeIfZero() {
		return this.scoreChangeIfZero;
	}
}
