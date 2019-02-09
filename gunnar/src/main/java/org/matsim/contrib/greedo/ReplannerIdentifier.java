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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.greedo.datastructures.Ages;
import org.matsim.contrib.greedo.datastructures.CountIndicatorUtils;
import org.matsim.contrib.greedo.datastructures.SpaceTimeIndicators;
import org.matsim.contrib.greedo.recipes.AccelerationRecipe;
import org.matsim.contrib.greedo.recipes.Mah2007Recipe;
import org.matsim.contrib.greedo.recipes.Mah2009Recipe;
import org.matsim.contrib.greedo.recipes.ReplannerIdentifierRecipe;
import org.matsim.contrib.greedo.recipes.UniformReplanningRecipe;
import org.matsim.core.utils.collections.Tuple;

import floetteroed.utilities.DynamicData;

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
	private final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> personId2pseudoSimSlotUsage;
	private final DynamicData<Id<?>> currentWeightedCounts;
	private final DynamicData<Id<?>> upcomingWeightedCounts;

	private final double sumOfUnweightedCountDifferences2; // TODO only for logging/debugging
	private final double sumOfWeightedCountDifferences2;

	private final Map<Id<Person>, Double> personId2weightedUtilityChange;
	private final Map<Id<Person>, Double> personId2unweightedUtilityChange;
	private final double totalWeightedUtilityChange;
	private final double totalUnweightedUtilityChange; // TODO only for logging/debugging

	private final Ages ages;
	private final List<Set<Id<Person>>> ageStrata;

	private final double lambdaBar;
	// private final double lambdaTarget;
	private final double beta;
	private final double delta;

	private Double sumOfUnweightedReplannerCountDifferences2 = null;
	private Double sumOfWeightedReplannerCountDifferences2 = null;
	private Double unweightedReplannerUtilityChangeSum = null;
	private Double weightedReplannerUtilityChangeSum = null;

	private Double sumOfUnweightedNonReplannerCountDifferences2 = null;
	private Double sumOfWeightedNonReplannerCountDifferences2 = null;
	private Double unweightedNonReplannerUtilityChangeSum = null;
	private Double weightedNonReplannerUtilityChangeSum = null;

	private Double replannerSizeSum = null;
	private Double nonReplannerSizeSum = null;

	// -------------------- CONSTRUCTION --------------------

	ReplannerIdentifier(final GreedoConfigGroup greedoConfig, final int greedoIteration,
			final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> personId2physicalSlotUsage,
			final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> personId2pseudoSimSlotUsage, final Population population,
			final Map<Id<Person>, Double> personId2UtilityChange, final Ages ages) {

		this.greedoConfig = greedoConfig;
		this.personId2physicalSlotUsage = personId2physicalSlotUsage;
		this.personId2pseudoSimSlotUsage = personId2pseudoSimSlotUsage;
		this.population = population;
		this.ages = ages;
		this.ageStrata = ages.stratifyByAgeWithMinumStratumSize(greedoConfig.getMinStratumSize());

		this.personId2unweightedUtilityChange = personId2UtilityChange;
		this.totalUnweightedUtilityChange = personId2UtilityChange.values().stream().mapToDouble(utlChange -> utlChange)
				.sum();

		// >>>>> REMOVING THE UTILITY WEIGHTING >>>>>

		this.personId2weightedUtilityChange = this.personId2unweightedUtilityChange;
		this.totalWeightedUtilityChange = this.totalUnweightedUtilityChange;

		// this.personId2weightedUtilityChange = new
		// LinkedHashMap<>(personId2UtilityChange.size());
		// personId2UtilityChange.entrySet().stream().forEach(entry -> {
		// final Id<Person> personId = entry.getKey();
		// final double weightedUtilityChange = ages.getPersonWeights().get(personId) *
		// entry.getValue();
		// this.personId2weightedUtilityChange.put(personId, weightedUtilityChange);
		// });
		// this.totalWeightedUtilityChange =
		// this.personId2weightedUtilityChange.values().stream()
		// .mapToDouble(utlChange -> utlChange).sum();

		// <<<<< REMOVING THE UTILITY WEIGHTING <<<<<

		final DynamicData<Id<?>> currentUnweightedCounts;
		{
			final Tuple<DynamicData<Id<?>>, DynamicData<Id<?>>> currentWeightedAndUnweightedCounts = CountIndicatorUtils
					.newWeightedAndUnweightedCounts(this.greedoConfig.getTimeDiscretization(),
							this.personId2physicalSlotUsage.values());
			this.currentWeightedCounts = currentWeightedAndUnweightedCounts.getFirst();
			currentUnweightedCounts = currentWeightedAndUnweightedCounts.getSecond();
		}

		final DynamicData<Id<?>> upcomingUnweightedCounts;
		{
			final Tuple<DynamicData<Id<?>>, DynamicData<Id<?>>> upcomingWeightedAndUnweightedCounts = CountIndicatorUtils
					.newWeightedAndUnweightedCounts(this.greedoConfig.getTimeDiscretization(),
							this.personId2pseudoSimSlotUsage.values());
			this.upcomingWeightedCounts = upcomingWeightedAndUnweightedCounts.getFirst();
			upcomingUnweightedCounts = upcomingWeightedAndUnweightedCounts.getSecond();
		}
		this.sumOfUnweightedCountDifferences2 = CountIndicatorUtils.sumOfDifferences2(currentUnweightedCounts,
				upcomingUnweightedCounts);
		this.sumOfWeightedCountDifferences2 = CountIndicatorUtils.sumOfDifferences2(this.currentWeightedCounts,
				this.upcomingWeightedCounts);

		this.lambdaBar = this.greedoConfig.getReplanningRate(greedoIteration);
		// this.lambdaTarget = this.greedoConfig.getTargetReplanningRate();

		this.beta = 2.0 * this.lambdaBar
				* (this.greedoConfig.getUseAgeWeightedBeta() ? this.sumOfWeightedCountDifferences2
						: this.sumOfUnweightedCountDifferences2)
				/ this.totalWeightedUtilityChange;

		this.delta = Math
				.pow(this.greedoConfig.getRegularizationThreshold()
						/ (1.0 - this.greedoConfig.getRegularizationThreshold()), 2.0)
				* CountIndicatorUtils.populationAverageUnweightedIndividualChangeSum2(personId2physicalSlotUsage,
						personId2pseudoSimSlotUsage); // TODO Check efficiency.

		// this.beta = 2.0 * this.lambdaBar * this.sumOfWeightedCountDifferences2 /
		// this.totalWeightedUtilityChange;
		// this.delta = Math
		// .pow(this.greedoConfig.getRegularizationThreshold()
		// / (1.0 - this.greedoConfig.getRegularizationThreshold()), 2.0)
		// * this.sumOfWeightedCountDifferences2 /
		// Math.pow(ages.getWeightAtAverageAge(), 2.0);
	}

	// -------------------- GETTERS (FOR LOGGING) --------------------

	public Double getSumOfUnweightedCountDifferences2() {
		return this.sumOfUnweightedCountDifferences2;
	}

	public Double getSumOfWeightedCountDifferences2() {
		return this.sumOfWeightedCountDifferences2;
	}

	public Double getSumOfUnweightedUtilityChanges() {
		return this.totalUnweightedUtilityChange;
	}

	public Double getSumOfWeightedUtilityChanges() {
		return this.totalWeightedUtilityChange;
	}

	public Double getLambdaBar() {
		return this.lambdaBar;
	}

	public Double getBeta() {
		return this.beta;
	}

	public Double getDelta() {
		return this.delta;
	}

	public Double getSumOfUnweightedReplannerCountDifferences2() {
		return this.sumOfUnweightedReplannerCountDifferences2;
	}

	public Double getSumOfWeightedReplannerCountDifferences2() {
		return this.sumOfWeightedReplannerCountDifferences2;
	}

	public Double getUnweightedReplannerUtilityChangeSum() {
		return this.unweightedReplannerUtilityChangeSum;
	}

	public Double getWeightedReplannerUtilityChangeSum() {
		return this.weightedReplannerUtilityChangeSum;
	}

	public Double getSumOfUnweightedNonReplannerCountDifferences2() {
		return this.sumOfUnweightedNonReplannerCountDifferences2;
	}

	public Double getSumOfWeightedNonReplannerCountDifferences2() {
		return this.sumOfWeightedNonReplannerCountDifferences2;
	}

	public Double getUnweightedNonReplannerUtilityChangeSum() {
		return this.unweightedNonReplannerUtilityChangeSum;
	}

	public Double getWeightedNonReplannerUtilityChangeSum() {
		return this.weightedNonReplannerUtilityChangeSum;
	}

	public Double getReplannerSizeSum() {
		return this.replannerSizeSum;
	}

	public Double getNonReplannerSizeSum() {
		return this.nonReplannerSizeSum;
	}

	// -------------------- IMPLEMENTATION --------------------

	Set<Id<Person>> drawReplanners() {

		// Initialize score residuals.

		final DynamicData<Id<?>> interactionResiduals = CountIndicatorUtils
				.newWeightedDifference(this.upcomingWeightedCounts, this.currentWeightedCounts, this.lambdaBar);
		double inertiaResidual = (1.0 - this.lambdaBar) * this.totalWeightedUtilityChange;
		// double regularizationResidual = 0;

		double[] regularizationResiduals = new double[this.ageStrata.size()];
		// double regularizationResidual = (this.lambdaBar - this.lambdaTarget) * (1.0 -
		// this.ages.getAverageWeight())
		// * this.population.getPersons().size();

		double sumOfInteractionResiduals2 = interactionResiduals.sumOfEntries2();

		// Instantiate the re-planning recipe.

		final ReplannerIdentifierRecipe recipe;
		if (GreedoConfigGroup.ModeType.off == this.greedoConfig.getModeTypeField()) {
			recipe = new UniformReplanningRecipe(this.lambdaBar);
		} else if (GreedoConfigGroup.ModeType.accelerate == this.greedoConfig.getModeTypeField()) {
			recipe = new AccelerationRecipe();
		} else if (GreedoConfigGroup.ModeType.mah2007 == this.greedoConfig.getModeTypeField()) {
			recipe = new Mah2007Recipe(this.personId2weightedUtilityChange, this.lambdaBar);
		} else if (GreedoConfigGroup.ModeType.mah2009 == this.greedoConfig.getModeTypeField()) {
			recipe = new Mah2009Recipe(this.personId2weightedUtilityChange, this.lambdaBar);
		} else {
			throw new RuntimeException("Unknown mode: " + this.greedoConfig.getModeTypeField());
		}

		// Go through all vehicles and decide which driver gets to re-plan.

		final Set<Id<Person>> replanners = new LinkedHashSet<>();

		final List<Id<Person>> allPersonIdsShuffled = new ArrayList<>(this.population.getPersons().keySet());
		Collections.shuffle(allPersonIdsShuffled);

		final DynamicData<Id<?>> weightedReplannerCountDifferences = new DynamicData<>(
				greedoConfig.getTimeDiscretization());
		final DynamicData<Id<?>> unweightedReplannerCountDifferences = new DynamicData<>(
				greedoConfig.getTimeDiscretization());
		final DynamicData<Id<?>> weightedNonReplannerCountDifferences = new DynamicData<>(
				greedoConfig.getTimeDiscretization());
		final DynamicData<Id<?>> unweightedNonReplannerCountDifferences = new DynamicData<>(
				greedoConfig.getTimeDiscretization());

		this.unweightedReplannerUtilityChangeSum = 0.0;
		this.unweightedNonReplannerUtilityChangeSum = 0.0;
		this.weightedReplannerUtilityChangeSum = 0.0;
		this.weightedNonReplannerUtilityChangeSum = 0.0;

		this.replannerSizeSum = 0.0;
		this.nonReplannerSizeSum = 0.0;

		for (Id<Person> personId : allPersonIdsShuffled) {

			// TODO Inefficient.
			Integer ageStratumIndex = null;
			for (int i = 0; ageStratumIndex == null; i++) {
				if (this.ageStrata.get(i).contains(personId)) {
					ageStratumIndex = i;
				}
			}

			final ScoreUpdater<Id<?>> scoreUpdater = new ScoreUpdater<>(this.personId2physicalSlotUsage.get(personId),
					this.personId2pseudoSimSlotUsage.get(personId), this.lambdaBar,
					this.ages.getPersonWeights().get(personId), this.beta, this.delta, interactionResiduals,
					inertiaResidual, regularizationResiduals[ageStratumIndex], this.greedoConfig,
					this.personId2weightedUtilityChange.get(personId), this.totalWeightedUtilityChange,
					sumOfInteractionResiduals2, this.ageStrata.get(ageStratumIndex).size());

			final boolean replanner = recipe.isReplanner(personId, scoreUpdater.getScoreChangeIfOne(),
					scoreUpdater.getScoreChangeIfZero());
			if (replanner) {
				replanners.add(personId);
				CountIndicatorUtils.addIndicatorsToTotalsTreatingNullAsZero(weightedReplannerCountDifferences,
						unweightedReplannerCountDifferences, this.personId2pseudoSimSlotUsage.get(personId), +1.0);
				CountIndicatorUtils.addIndicatorsToTotalsTreatingNullAsZero(weightedReplannerCountDifferences,
						unweightedReplannerCountDifferences, this.personId2physicalSlotUsage.get(personId), -1.0);
				this.unweightedReplannerUtilityChangeSum += this.personId2unweightedUtilityChange.get(personId);
				this.weightedReplannerUtilityChangeSum += this.personId2weightedUtilityChange.get(personId);
				if (this.personId2physicalSlotUsage.containsKey(personId)) {
					this.replannerSizeSum += this.personId2physicalSlotUsage.get(personId).size();
				}
			} else {
				CountIndicatorUtils.addIndicatorsToTotalsTreatingNullAsZero(weightedNonReplannerCountDifferences,
						unweightedNonReplannerCountDifferences, this.personId2pseudoSimSlotUsage.get(personId), +1.0);
				CountIndicatorUtils.addIndicatorsToTotalsTreatingNullAsZero(weightedNonReplannerCountDifferences,
						unweightedNonReplannerCountDifferences, this.personId2physicalSlotUsage.get(personId), -1.0);
				this.unweightedNonReplannerUtilityChangeSum += this.personId2unweightedUtilityChange.get(personId);
				this.weightedNonReplannerUtilityChangeSum += this.personId2weightedUtilityChange.get(personId);
				if (this.personId2physicalSlotUsage.containsKey(personId)) {
					this.nonReplannerSizeSum += this.personId2physicalSlotUsage.get(personId).size();
				}
			}

			// interaction residuals are updated by reference
			scoreUpdater.updateResiduals(replanner ? 1.0 : 0.0);
			inertiaResidual = scoreUpdater.getUpdatedInertiaResidual();

			// regularizationResidual = scoreUpdater.getUpdatedRegularizationResidual();
			regularizationResiduals[ageStratumIndex] = scoreUpdater.getUpdatedRegularizationResidual();

			sumOfInteractionResiduals2 = scoreUpdater.getUpdatedSumOfInteractionResiduals2();
		}

		// >>> collect statistics, only for logging >>>

		this.sumOfUnweightedReplannerCountDifferences2 = CountIndicatorUtils
				.sumOfEntries2(unweightedReplannerCountDifferences);
		this.sumOfWeightedReplannerCountDifferences2 = CountIndicatorUtils
				.sumOfEntries2(weightedReplannerCountDifferences);
		this.sumOfUnweightedNonReplannerCountDifferences2 = CountIndicatorUtils
				.sumOfEntries2(unweightedNonReplannerCountDifferences);
		this.sumOfWeightedNonReplannerCountDifferences2 = CountIndicatorUtils
				.sumOfEntries2(weightedNonReplannerCountDifferences);

		// <<< collect statistics, only for logging <<<

		return replanners;
	}
}
