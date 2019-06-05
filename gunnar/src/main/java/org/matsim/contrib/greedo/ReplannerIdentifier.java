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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.greedo.datastructures.CountIndicatorUtils;
import org.matsim.contrib.greedo.datastructures.SpaceTimeIndicators;
import org.matsim.contrib.greedo.recipes.AccelerationRecipe;
import org.matsim.core.utils.collections.Tuple;

import floetteroed.utilities.DynamicData;
import floetteroed.utilities.DynamicDataUtils;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class ReplannerIdentifier {

	// -------------------- MEMBERS --------------------

	private final GreedoConfigGroup greedoConfig;

	private final Population population;

	private final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> personId2physicalSlotUsage;
	private final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> personId2hypothetialSlotUsage;
	private final DynamicData<Id<?>> currentWeightedCounts;
	private final DynamicData<Id<?>> upcomingWeightedCounts;

	private final Map<Id<Person>, Double> personId2UtilityChange;
	private final double totalUtilityChange;

	private final double lambdaBar;
	private final double beta;
	private final Double unconstrainedBeta;
	private final Double delta;

	private LastExpectations lastExpectations = null;

	// -------------------- CONSTRUCTION --------------------

	ReplannerIdentifier(ReplanningEfficiencyEstimator replanningEfficiencyEstimator,
			final GreedoConfigGroup greedoConfig, final int iteration,
			final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> personId2physicalSlotUsage,
			final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> personId2hypotheticalSlotUsage,
			final Population population, final Map<Id<Person>, Double> personId2UtilityChange) {

		this.greedoConfig = greedoConfig;
		this.personId2physicalSlotUsage = personId2physicalSlotUsage;
		this.personId2hypothetialSlotUsage = personId2hypotheticalSlotUsage;
		this.population = population;

		this.personId2UtilityChange = personId2UtilityChange;
		this.totalUtilityChange = personId2UtilityChange.values().stream().mapToDouble(utlChange -> utlChange).sum();

		// final DynamicData<Id<?>> currentUnweightedCounts;
		{
			final Tuple<DynamicData<Id<?>>, DynamicData<Id<?>>> currentWeightedAndUnweightedCounts = CountIndicatorUtils
					.newWeightedAndUnweightedCounts(this.greedoConfig.newTimeDiscretization(),
							this.personId2physicalSlotUsage.values());
			this.currentWeightedCounts = currentWeightedAndUnweightedCounts.getFirst();
			// currentUnweightedCounts = currentWeightedAndUnweightedCounts.getSecond();
		}

		// final DynamicData<Id<?>> upcomingUnweightedCounts;
		{
			final Tuple<DynamicData<Id<?>>, DynamicData<Id<?>>> upcomingWeightedAndUnweightedCounts = CountIndicatorUtils
					.newWeightedAndUnweightedCounts(this.greedoConfig.newTimeDiscretization(),
							this.personId2hypothetialSlotUsage.values());
			this.upcomingWeightedCounts = upcomingWeightedAndUnweightedCounts.getFirst();
			// upcomingUnweightedCounts = upcomingWeightedAndUnweightedCounts.getSecond();
		}
		// if (this.greedoConfig.getDetailedLogging()) {
		// this.sumOfUnweightedCountDifferences2 =
		// DynamicDataUtils.sumOfDifferences2(currentUnweightedCounts,
		// upcomingUnweightedCounts);
		// } else {
		// this.sumOfUnweightedCountDifferences2 = null;
		// }
		final double sumOfWeightedCountDifferences2 = DynamicDataUtils.sumOfDifferences2(this.currentWeightedCounts,
				this.upcomingWeightedCounts);

		this.unconstrainedBeta = replanningEfficiencyEstimator.getBeta();
		this.delta = replanningEfficiencyEstimator.getDelta();

		if (this.greedoConfig.getReplannerIdentifierRecipe() instanceof AccelerationRecipe) {
			((AccelerationRecipe) this.greedoConfig.getReplannerIdentifierRecipe())
					.setUseBackupRecipe(this.unconstrainedBeta == null);
		}
		if (this.unconstrainedBeta != null) {
			this.beta = this.unconstrainedBeta;
			this.lambdaBar = 0.5 * this.unconstrainedBeta * this.totalUtilityChange
					/ Math.max(sumOfWeightedCountDifferences2, 1e-8);
		} else {
			this.lambdaBar = this.greedoConfig.getExogenousReplanningRate(iteration);
			this.beta = 2.0 * this.lambdaBar * sumOfWeightedCountDifferences2 / Math.max(this.totalUtilityChange, 1e-8);
		}

		// if ((this.greedoConfig.getReplannerIdentifierRecipe() instanceof
		// AccelerationRecipe)
		// && (this.unconstrainedBeta <= 0) ||
		// !replanningEfficiencyEstimator.hadEnoughData()) {
		// ((AccelerationRecipe)
		// this.greedoConfig.getReplannerIdentifierRecipe()).setUseBackupRecipe(true);
		// this.lambdaBar = this.greedoConfig.getExogenousReplanningRate(iteration);
		// this.beta = 2.0 * this.lambdaBar * sumOfWeightedCountDifferences2 /
		// Math.max(this.totalUtilityChange, 1e-8);
		// } else {
		// if (this.greedoConfig.getReplannerIdentifierRecipe() instanceof
		// AccelerationRecipe) {
		// ((AccelerationRecipe)
		// this.greedoConfig.getReplannerIdentifierRecipe()).setUseBackupRecipe(false);
		// }
		// this.beta = unconstrainedBeta;
		// this.lambdaBar = 0.5 * unconstrainedBeta * this.totalUtilityChange
		// / Math.max(sumOfWeightedCountDifferences2, 1e-8);
		// }
	}

	// -------------------- IMPLEMENTATION --------------------

	Set<Id<Person>> drawReplanners() {

		// Initialize score residuals.

		final DynamicData<Id<?>> interactionResiduals = DynamicDataUtils.newDifference(this.upcomingWeightedCounts,
				this.currentWeightedCounts, this.lambdaBar);
		double inertiaResidual = (1.0 - this.lambdaBar) * this.totalUtilityChange;
		double sumOfInteractionResiduals2 = interactionResiduals.sumOfEntries2();

		// Go through all vehicles and decide which driver gets to re-plan.

		final List<Id<Person>> allPersonIdsShuffled = new ArrayList<>(this.population.getPersons().keySet());
		Collections.shuffle(allPersonIdsShuffled);

		DynamicData<Id<?>> weightedReplannerCountDifferences = null;
		DynamicData<Id<?>> unweightedReplannerCountDifferences = null;
		DynamicData<Id<?>> weightedNonReplannerCountDifferences = null;
		DynamicData<Id<?>> unweightedNonReplannerCountDifferences = null;
		if (this.greedoConfig.getDetailedLogging()) {
			weightedReplannerCountDifferences = new DynamicData<>(this.greedoConfig.newTimeDiscretization());
			unweightedReplannerCountDifferences = new DynamicData<>(this.greedoConfig.newTimeDiscretization());
			weightedNonReplannerCountDifferences = new DynamicData<>(this.greedoConfig.newTimeDiscretization());
			unweightedNonReplannerCountDifferences = new DynamicData<>(this.greedoConfig.newTimeDiscretization());
		}

		double lastExpectedUtilityChangeSumAccelerated = 0.0;
		double nonReplannerUtilityChangeSum = 0.0;
		double replannerSizeSum = 0.0;
		double nonReplannerSizeSum = 0.0;

		final Set<Id<Person>> replanners = new LinkedHashSet<>();

		for (Id<Person> personId : allPersonIdsShuffled) {

			final ScoreUpdater<Id<?>> scoreUpdater = new ScoreUpdater<>(this.personId2physicalSlotUsage.get(personId),
					this.personId2hypothetialSlotUsage.get(personId), this.lambdaBar, this.beta, interactionResiduals,
					inertiaResidual, this.personId2UtilityChange.get(personId), sumOfInteractionResiduals2);

			final boolean replanner = this.greedoConfig.getReplannerIdentifierRecipe().isReplanner(personId,
					scoreUpdater.getScoreChangeIfOne(), scoreUpdater.getScoreChangeIfZero());

			if (replanner) {
				replanners.add(personId);
				if (this.greedoConfig.getDetailedLogging()) {
					CountIndicatorUtils.addIndicatorsToTotalsTreatingNullAsZero(weightedReplannerCountDifferences,
							unweightedReplannerCountDifferences, this.personId2hypothetialSlotUsage.get(personId),
							+1.0);
					CountIndicatorUtils.addIndicatorsToTotalsTreatingNullAsZero(weightedReplannerCountDifferences,
							unweightedReplannerCountDifferences, this.personId2physicalSlotUsage.get(personId), -1.0);
				}
				lastExpectedUtilityChangeSumAccelerated += this.personId2UtilityChange.get(personId);
				if (this.personId2physicalSlotUsage.containsKey(personId)) {
					replannerSizeSum += this.personId2physicalSlotUsage.get(personId).size();
				}
			} else {
				if (this.greedoConfig.getDetailedLogging()) {
					CountIndicatorUtils.addIndicatorsToTotalsTreatingNullAsZero(weightedNonReplannerCountDifferences,
							unweightedNonReplannerCountDifferences, this.personId2hypothetialSlotUsage.get(personId),
							+1.0);
					CountIndicatorUtils.addIndicatorsToTotalsTreatingNullAsZero(weightedNonReplannerCountDifferences,
							unweightedNonReplannerCountDifferences, this.personId2physicalSlotUsage.get(personId),
							-1.0);
				}
				nonReplannerUtilityChangeSum += this.personId2UtilityChange.get(personId);
				if (this.personId2physicalSlotUsage.containsKey(personId)) {
					nonReplannerSizeSum += this.personId2physicalSlotUsage.get(personId).size();
				}
			}

			// Interaction residuals are updated by reference.
			scoreUpdater.updateResiduals(replanner ? 1.0 : 0.0);
			inertiaResidual = scoreUpdater.getUpdatedInertiaResidual();
			sumOfInteractionResiduals2 = scoreUpdater.getUpdatedSumOfInteractionResiduals2();
		}

		// >>> collect statistics, only for logging >>>

		Double sumOfUnweightedReplannerCountDifferences2 = null;
		Double sumOfWeightedReplannerCountDifferences2 = null;
		Double sumOfUnweightedNonReplannerCountDifferences2 = null;
		Double sumOfWeightedNonReplannerCountDifferences2 = null;
		if (this.greedoConfig.getDetailedLogging()) {
			sumOfUnweightedReplannerCountDifferences2 = DynamicDataUtils
					.sumOfEntries2(unweightedReplannerCountDifferences);
			sumOfWeightedReplannerCountDifferences2 = DynamicDataUtils.sumOfEntries2(weightedReplannerCountDifferences);
			sumOfUnweightedNonReplannerCountDifferences2 = DynamicDataUtils
					.sumOfEntries2(unweightedNonReplannerCountDifferences);
			sumOfWeightedNonReplannerCountDifferences2 = DynamicDataUtils
					.sumOfEntries2(weightedNonReplannerCountDifferences);
		}

		// >>>>> NEW 2019-05-20 >>>>>

		final Map<Id<Person>, Double> personId2similarity = new LinkedHashMap<>();
		for (Id<Person> personId : this.population.getPersons().keySet()) {
			final SpaceTimeIndicators<Id<?>> hypotheticalSlotUsage = this.personId2hypothetialSlotUsage.get(personId);
			final SpaceTimeIndicators<Id<?>> physicalSlotUsage = this.personId2physicalSlotUsage.get(personId);
			double similarity = 0.0;
			for (int timeBin = 0; timeBin < this.greedoConfig.getBinCnt(); timeBin++) {
				if (hypotheticalSlotUsage != null) {
					for (SpaceTimeIndicators<Id<?>>.Visit hypotheticalVisit : hypotheticalSlotUsage
							.getVisits(timeBin)) {
						similarity += weightedReplannerCountDifferences.getBinValue(hypotheticalVisit.spaceObject,
								timeBin);
					}
				}
				if (physicalSlotUsage != null) {
					for (SpaceTimeIndicators<Id<?>>.Visit physicalVisit : physicalSlotUsage.getVisits(timeBin)) {
						similarity -= weightedReplannerCountDifferences.getBinValue(physicalVisit.spaceObject, timeBin);
					}
				}
			}
			similarity /= this.population.getPersons().size();
			personId2similarity.put(personId, similarity);
		}

		// <<<<< NEW 2019-05-20 <<<<<

		this.lastExpectations = new LastExpectations(this.lambdaBar, this.beta, this.unconstrainedBeta, this.delta,
				lastExpectedUtilityChangeSumAccelerated, sumOfUnweightedReplannerCountDifferences2,
				sumOfWeightedReplannerCountDifferences2, sumOfUnweightedNonReplannerCountDifferences2,
				sumOfWeightedNonReplannerCountDifferences2, nonReplannerUtilityChangeSum, replannerSizeSum,
				nonReplannerSizeSum, replanners.size(), this.population.getPersons().size() - replanners.size(),
				personId2similarity, this.greedoConfig.getReplannerIdentifierRecipe().getDeployedRecipeName());

		// <<< collect statistics, only for logging <<<

		return replanners;
	}

	// -------------------- INNER CLASS --------------------

	LastExpectations getLastExpectations() {
		return this.lastExpectations;
	}

	public static class LastExpectations {

		public final Double lambdaBar;
		public final Double beta;
		public final Double unconstrainedBeta;
		public final Double delta;
		public final Double sumOfReplannerUtilityChanges;
		public final Double sumOfNonReplannerUtilityChanges;
		public final Double sumOfUnweightedReplannerCountDifferences2;
		public final Double sumOfWeightedReplannerCountDifferences2;
		public final Double sumOfUnweightedNonReplannerCountDifferences2;
		public final Double sumOfWeightedNonReplannerCountDifferences2;
		public final Double replannerSizeSum;
		public final Double nonReplannerSizeSum;
		public final Integer numberOfReplanners;
		public final Integer numberOfNonReplanners;
		public final Map<Id<Person>, Double> personId2similarity;
		public final String replannerIdentifierRecipeName;

		LastExpectations() {
			this(null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					new LinkedHashMap<>(), null);
		}

		LastExpectations(final Double lambdaBar, final Double beta, final Double unconstrainedBeta, final Double delta,
				final Double lastExpectedUtilityChangeSumAccelerated,
				final Double sumOfUnweightedReplannerCountDifferences2,
				final Double sumOfWeightedReplannerCountDifferences2,
				final Double sumOfUnweightedNonReplannerCountDifferences2,
				final Double sumOfWeightedNonReplannerCountDifferences2, final Double nonReplannerUtilityChangeSum,
				final Double replannerSizeSum, final Double nonReplannerSizeSum, final Integer numberOfReplanners,
				final Integer numberOfNonReplanners, final Map<Id<Person>, Double> personId2similarity,
				final String replannerIdentifierRecipeName) {
			this.lambdaBar = lambdaBar;
			this.beta = beta;
			this.unconstrainedBeta = unconstrainedBeta;
			this.delta = delta;
			this.sumOfReplannerUtilityChanges = lastExpectedUtilityChangeSumAccelerated;
			this.sumOfNonReplannerUtilityChanges = nonReplannerUtilityChangeSum;
			this.sumOfUnweightedReplannerCountDifferences2 = sumOfUnweightedReplannerCountDifferences2;
			this.sumOfWeightedReplannerCountDifferences2 = sumOfWeightedReplannerCountDifferences2;
			this.sumOfUnweightedNonReplannerCountDifferences2 = sumOfUnweightedNonReplannerCountDifferences2;
			this.sumOfWeightedNonReplannerCountDifferences2 = sumOfWeightedNonReplannerCountDifferences2;
			this.replannerSizeSum = replannerSizeSum;
			this.nonReplannerSizeSum = nonReplannerSizeSum;
			this.numberOfReplanners = numberOfReplanners;
			this.numberOfNonReplanners = numberOfNonReplanners;
			this.personId2similarity = Collections.unmodifiableMap(personId2similarity);
			this.replannerIdentifierRecipeName = replannerIdentifierRecipeName;
		}

		public Double getSumOfUtilityChanges() {
			if ((this.sumOfReplannerUtilityChanges != null) && (this.sumOfNonReplannerUtilityChanges != null)) {
				return (this.sumOfReplannerUtilityChanges + this.sumOfNonReplannerUtilityChanges);
			} else {
				return null;
			}
		}

		public Double getSumOfUtilityChangesGivenUniformReplanning() {
			final Double sumOfUtilityChanges = this.getSumOfUtilityChanges();
			if ((sumOfUtilityChanges != null) && (this.lambdaBar != null)) {
				return this.lambdaBar * sumOfUtilityChanges;
			} else {
				return null;
			}
		}

		public Double getSumOfWeightedCountDifferences2() {
			if ((this.sumOfWeightedReplannerCountDifferences2 != null)
					&& (this.sumOfWeightedNonReplannerCountDifferences2 != null)) {
				return (this.sumOfWeightedReplannerCountDifferences2 + this.sumOfWeightedNonReplannerCountDifferences2);
			} else {
				return null;
			}
		}

		public Double getSumOfUnweightedCountDifferences2() {
			if ((this.sumOfUnweightedReplannerCountDifferences2 != null)
					&& (this.sumOfUnweightedNonReplannerCountDifferences2 != null)) {
				return (this.sumOfUnweightedReplannerCountDifferences2
						+ this.sumOfUnweightedNonReplannerCountDifferences2);
			} else {
				return null;
			}
		}

		public Integer getNumberOfPersons() {
			if ((this.numberOfReplanners != null) && (this.numberOfNonReplanners != null)) {
				return (this.numberOfReplanners + this.numberOfNonReplanners);
			} else {
				return null;
			}
		}
	}
}
