package searchacceleration.datastructures;

import java.util.Map;

import floetteroed.utilities.DynamicData;
import floetteroed.utilities.Tuple;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class IndicatorNumerics<L> {

	// -------------------- MEMBERS --------------------

	private final SpaceTimeIndicatorVectorListbased<L> current;

	private final SpaceTimeIndicatorVectorListbased<L> upcoming;

	private final double meanLambda;

	private final DynamicData<L> currentCounts;

	private final DynamicData<L> interactionResidual;

	private final DynamicData<L> inertiaResidual;

	private double regularizationResidual;

	private final double scoreChangeIfZero;

	private final double scoreChangeIfOne;

	private boolean residualsUpdated = false;

	// -------------------- CONSTRUCTION --------------------

	public IndicatorNumerics(final SpaceTimeIndicatorVectorListbased<L> current,
			final SpaceTimeIndicatorVectorListbased<L> upcoming, final double meanLambda,
			final DynamicData<L> currentCounts, final double currentCountsSumOfSquares, final double w,
			final double delta, final DynamicData<L> interactionResidual, final DynamicData<L> inertiaResidual,
			final double regularizationResidual) {

		this.current = current;
		this.upcoming = upcoming;

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
			final SpaceTimeVectorMapbased<L> currentTmp = new SpaceTimeVectorMapbased<>(this.current);
			final SpaceTimeVectorMapbased<L> upcomingTmp = new SpaceTimeVectorMapbased<>(this.upcoming);
			final SpaceTimeVectorMapbased<L> deltaTmp = upcomingTmp.newDeepCopy();
			deltaTmp.subtract(currentTmp);

			for (Map.Entry<Tuple<L, Integer>, Double> changeEntry : deltaTmp.data.entrySet()) {
				final double changeValue = changeEntry.getValue();
				changeSumOfSquares += changeValue * changeValue;
				changeTimesInteractionResiduals += changeValue
						* interactionResidual.getBinValue(changeEntry.getKey().getA(), changeEntry.getKey().getB());
			}

			for (Map.Entry<Tuple<L, Integer>, Double> currentEntry : currentTmp.data.entrySet()) {
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

	public void updateResiduals(final double newLambda) {

		if (this.residualsUpdated) {
			throw new RuntimeException("Residuals have already been updated.");
		}
		this.residualsUpdated = true;

		final double deltaLambda = newLambda - this.meanLambda;
		for (int bin = 0; bin < this.current.data.size(); bin++) {
			if (this.upcoming.data.get(bin) != null) {
				for (L currentSpaceObj : this.upcoming.data.get(bin)) {
					this.interactionResidual.add(currentSpaceObj, bin, +deltaLambda);
				}
			}
			if (this.current.data.get(bin) != null) {
				for (L currentSpaceObj : this.current.data.get(bin)) {
					this.interactionResidual.add(currentSpaceObj, bin, -deltaLambda);
					this.inertiaResidual.add(currentSpaceObj, bin, -deltaLambda);
				}
			}
			for (L currentSpaceObj : this.upcoming.data.get(bin)) {
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
