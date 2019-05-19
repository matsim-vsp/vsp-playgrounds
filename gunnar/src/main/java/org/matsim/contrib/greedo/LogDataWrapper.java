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

import java.util.List;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class LogDataWrapper {

	// private final WireGreedoIntoMATSimControlerListener accelerator;

	private final Ages ages;

	Utilities.SummaryStatistics summaryStats;

	private final ReplannerIdentifier.LastExpectations lastExpectations;

	public LogDataWrapper(// final WireGreedoIntoMATSimControlerListener accelerator,
			final Ages ages, Utilities.SummaryStatistics summaryStats,
			final ReplannerIdentifier.LastExpectations lastExpectations) {
		// this.accelerator = accelerator;
		this.ages = ages;
		this.summaryStats = summaryStats;
		this.lastExpectations = lastExpectations;
	}

	public Double getLambdaBar() {
		return this.lastExpectations.lambdaBar;
	}

	public Double getBeta() {
		return this.lastExpectations.beta;
	}

	public Double getUnconstrainedBeta() {
		return this.lastExpectations.unconstrainedBeta;
	}

	public Double getDelta() {
		return this.lastExpectations.delta;
	}

	public Double getPredictedUtilityChange() {
		if ((this.lastExpectations.sumOfReplannerUtilityChanges != null)
				&& (this.lastExpectations.sumOfWeightedReplannerCountDifferences2 != null)
				&& (this.getBeta() != null)) {
			return (this.lastExpectations.sumOfReplannerUtilityChanges
					- this.lastExpectations.sumOfWeightedReplannerCountDifferences2 / this.getBeta());
		} else {
			return null;
		}
	}

	public Double getSumOfWeightedCountDifferences2() {
		return this.lastExpectations.getSumOfWeightedCountDifferences2();
	}

	public Double getSumOfUnweightedCountDifferences2() {
		return this.lastExpectations.getSumOfUnweightedCountDifferences2();
	}

	public Double getLastRealizedUtilitySum() {
		// return this.accelerator.getRealizedUtilitySum();
		return this.summaryStats.realizedUtilitySum;
	}

	public Double getLastExpectedUtilityChangeSumAccelerated() {
		return this.lastExpectations.sumOfReplannerUtilityChanges;
	}

	public Double getLastExpectedUtilityChangeSumTotal() {
		return this.lastExpectations.getSumOfUtilityChanges();
	}

	public Double getLastExpectedUtilityChangeSumUniform() {
		return this.lastExpectations.getSumOfUtilityChangesGivenUniformReplanning();
	}

	public Double getLastRealizedUtilityChangeSum() {
		// return this.accelerator.getRealizedUtilityChangeSum();
		return this.summaryStats.realizedUtilityChangeSum;
	}

	public List<Integer> getSortedAges() {
		// return this.accelerator.getSortedAgesView();
		return this.ages.getSortedAges();
	}

	public Double getAverageAge() {
		// return this.accelerator.getAveragAge();
		return this.ages.getAverageAge();
	}

	public Double getAverageWeight() {
		// return this.accelerator.getAverageWeight();
		return this.ages.getAverageWeight();
	}

	public Double getSumOfUnweightedReplannerCountDifferences2() {
		return this.lastExpectations.sumOfUnweightedReplannerCountDifferences2;
	}

	public Double getSumOfWeightedReplannerCountDifferences2() {
		return this.lastExpectations.sumOfWeightedReplannerCountDifferences2;
	}

	// public Double getReplannerUtilityChangeSum() {
	// return this.identifier.replannerExpectedUtilityChangeSum;
	// }

	public Double getSumOfUnweightedNonReplannerCountDifferences2() {
		return this.lastExpectations.sumOfUnweightedNonReplannerCountDifferences2;
	}

	public Double getSumOfWeightedNonReplannerCountDifferences2() {
		return this.lastExpectations.sumOfWeightedNonReplannerCountDifferences2;
	}

	public Double getNonReplannerUtilityChangeSum() {
		return this.lastExpectations.sumOfReplannerUtilityChanges;
	}

	public Integer getNumberOfReplanners() {
		return this.lastExpectations.numberOfReplanners;
	}

	public Integer getNumberOfNonReplanners() {
		return this.lastExpectations.numberOfNonReplanners;
	}

	public Integer getPopulationSize() {
		return this.lastExpectations.getNumberOfPersons();
	}

	public Double getReplannerSizeSum() {
		return this.lastExpectations.replannerSizeSum;
	}

	public Double getNonReplannerSizeSum() {
		return this.lastExpectations.nonReplannerSizeSum;
	}

	// public Double getRelativeUtilityEfficiency() {
	// if ((this.accelerator.getRealizedUtilityChangeSum() != null)
	// && (this.lastExpectations.sumOfReplannerUtilityChanges != null)
	// && (this.lastExpectations.getSumOfUtilityChanges() != null)) {
	// return Math.abs(
	// this.accelerator.getRealizedUtilityChangeSum() -
	// this.lastExpectations.sumOfReplannerUtilityChanges)
	// / this.lastExpectations.getSumOfUtilityChanges();
	// } else {
	// return null;
	// }
	// }

	// public Double getRelativeSlotVariability() {
	// if ((this.lastExpectations.sumOfWeightedReplannerCountDifferences2 != null)
	// && (this.lastExpectations.getSumOfWeightedCountDifferences2() != null)) {
	// return this.lastExpectations.sumOfWeightedReplannerCountDifferences2
	// / this.lastExpectations.getSumOfWeightedCountDifferences2();
	// } else {
	// return null;
	// }
	// }

	// public Double getEstimatedBetaNumerator() {
	// return this.lastExpectations.sumOfWeightedReplannerCountDifferences2;
	// }

	// public Double getEstimatedBetaDenominator() {
	// if ((this.accelerator.getRealizedUtilityChangeSum() != null)
	// && (this.lastExpectations.sumOfReplannerUtilityChanges != null)) {
	// return Math.abs(this.accelerator.getRealizedUtilityChangeSum()
	// - this.lastExpectations.sumOfReplannerUtilityChanges);
	// } else {
	// return null;
	// }
	//
	// }

	// public Double getEstimatedBeta() {
	// if ((this.accelerator.getRealizedUtilityChangeSum() != null)
	// && (this.lastExpectations.sumOfReplannerUtilityChanges != null)
	// && (this.lastExpectations.sumOfWeightedReplannerCountDifferences2 != null)) {
	// return this.lastExpectations.sumOfWeightedReplannerCountDifferences2
	// / Math.abs(this.accelerator.getRealizedUtilityChangeSum()
	// - this.lastExpectations.sumOfReplannerUtilityChanges);
	// } else {
	// return null;
	// }
	// }

}
