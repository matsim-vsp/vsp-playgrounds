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
import org.matsim.contrib.greedo.datastructures.CountIndicatorUtils;
import org.matsim.contrib.greedo.datastructures.SpaceTimeIndicators;
import org.matsim.contrib.greedo.recipes.AccelerationRecipe;
import org.matsim.contrib.greedo.recipes.AcceptAllRecipe;
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
	private final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> personId2hypothetialSlotUsage;
	private final DynamicData<Id<?>> currentWeightedCounts;
	private final DynamicData<Id<?>> upcomingWeightedCounts;

	private final Double sumOfUnweightedCountDifferences2;
	private final double sumOfWeightedCountDifferences2;

	private final Map<Id<Person>, Double> personId2UtilityChange;
	private final double totalUtilityChange;

	private final double lambdaBar;
	private final double beta;

	private Double sumOfUnweightedReplannerCountDifferences2 = null;
	private Double sumOfWeightedReplannerCountDifferences2 = null;
	private Double replannerUtilityChangeSum = null;

	private Double sumOfUnweightedNonReplannerCountDifferences2 = null;
	private Double sumOfWeightedNonReplannerCountDifferences2 = null;
	private Double nonReplannerUtilityChangeSum = null;

	private Double replannerSizeSum = null;
	private Double nonReplannerSizeSum = null;

	// -------------------- CONSTRUCTION --------------------

	ReplannerIdentifier(final GreedoConfigGroup greedoConfig, final int iteration,
			final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> personId2physicalSlotUsage,
			final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> personId2hypotheticalSlotUsage,
			final Population population, final Map<Id<Person>, Double> personId2UtilityChange) {

		this.greedoConfig = greedoConfig;
		this.personId2physicalSlotUsage = personId2physicalSlotUsage;
		this.personId2hypothetialSlotUsage = personId2hypotheticalSlotUsage;
		this.population = population;

		this.personId2UtilityChange = personId2UtilityChange;
		this.totalUtilityChange = personId2UtilityChange.values().stream().mapToDouble(utlChange -> utlChange).sum();

		final DynamicData<Id<?>> currentUnweightedCounts;
		{
			final Tuple<DynamicData<Id<?>>, DynamicData<Id<?>>> currentWeightedAndUnweightedCounts = CountIndicatorUtils
					.newWeightedAndUnweightedCounts(this.greedoConfig.newTimeDiscretization(),
							this.personId2physicalSlotUsage.values());
			this.currentWeightedCounts = currentWeightedAndUnweightedCounts.getFirst();
			currentUnweightedCounts = currentWeightedAndUnweightedCounts.getSecond();
		}

		final DynamicData<Id<?>> upcomingUnweightedCounts;
		{
			final Tuple<DynamicData<Id<?>>, DynamicData<Id<?>>> upcomingWeightedAndUnweightedCounts = CountIndicatorUtils
					.newWeightedAndUnweightedCounts(this.greedoConfig.newTimeDiscretization(),
							this.personId2hypothetialSlotUsage.values());
			this.upcomingWeightedCounts = upcomingWeightedAndUnweightedCounts.getFirst();
			upcomingUnweightedCounts = upcomingWeightedAndUnweightedCounts.getSecond();
		}
		if (this.greedoConfig.getDetailedLogging()) {
			this.sumOfUnweightedCountDifferences2 = CountIndicatorUtils.sumOfDifferences2(currentUnweightedCounts,
					upcomingUnweightedCounts);
		} else {
			this.sumOfUnweightedCountDifferences2 = null;
		}
		this.sumOfWeightedCountDifferences2 = CountIndicatorUtils.sumOfDifferences2(this.currentWeightedCounts,
				this.upcomingWeightedCounts);

		this.lambdaBar = this.greedoConfig.getReplanningRate(iteration);
		this.beta = 2.0 * this.lambdaBar * this.sumOfWeightedCountDifferences2
				/ Math.max(this.totalUtilityChange, 1e-8);
	}

	// -------------------- GETTERS (FOR LOGGING) --------------------

	public Double getSumOfUnweightedCountDifferences2() {
		return this.sumOfUnweightedCountDifferences2;
	}

	public Double getSumOfWeightedCountDifferences2() {
		return this.sumOfWeightedCountDifferences2;
	}

	public Double getLambdaBar() {
		return this.lambdaBar;
	}

	public Double getBeta() {
		return this.beta;
	}

	public Double getSumOfUnweightedReplannerCountDifferences2() {
		return this.sumOfUnweightedReplannerCountDifferences2;
	}

	public Double getSumOfWeightedReplannerCountDifferences2() {
		return this.sumOfWeightedReplannerCountDifferences2;
	}

	public Double getSumOfUnweightedNonReplannerCountDifferences2() {
		return this.sumOfUnweightedNonReplannerCountDifferences2;
	}

	public Double getSumOfWeightedNonReplannerCountDifferences2() {
		return this.sumOfWeightedNonReplannerCountDifferences2;
	}

	public Double getReplannerExpectedUtilityChangeSum() {
		return this.replannerUtilityChangeSum;
	}

	public Double getNonReplannerExpectedUtilityChangeSum() {
		return this.nonReplannerUtilityChangeSum;
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
		double inertiaResidual = (1.0 - this.lambdaBar) * this.totalUtilityChange;
		double sumOfInteractionResiduals2 = interactionResiduals.sumOfEntries2();

		// Instantiate the re-planning recipe.

		final ReplannerIdentifierRecipe recipe;
		if (GreedoConfigGroup.ModeType.sample == this.greedoConfig.getModeTypeField()) {
			recipe = new UniformReplanningRecipe(this.lambdaBar);
		} else if (GreedoConfigGroup.ModeType.off == this.greedoConfig.getModeTypeField()) {
			recipe = new AcceptAllRecipe();
		} else if (GreedoConfigGroup.ModeType.accelerate == this.greedoConfig.getModeTypeField()) {
			recipe = new AccelerationRecipe();
		} else if (GreedoConfigGroup.ModeType.mah2007 == this.greedoConfig.getModeTypeField()) {
			recipe = new Mah2007Recipe(this.personId2UtilityChange, this.lambdaBar);
		} else if (GreedoConfigGroup.ModeType.mah2009 == this.greedoConfig.getModeTypeField()) {
			recipe = new Mah2009Recipe(this.personId2UtilityChange, this.lambdaBar);
		} else {
			throw new RuntimeException("Unknown mode: " + this.greedoConfig.getModeTypeField());
		}

		// Go through all vehicles and decide which driver gets to re-plan.

		final List<Id<Person>> allPersonIdsShuffled = new ArrayList<>(this.population.getPersons().keySet());
		Collections.shuffle(allPersonIdsShuffled);

		final DynamicData<Id<?>> weightedReplannerCountDifferences;
		final DynamicData<Id<?>> unweightedReplannerCountDifferences;
		final DynamicData<Id<?>> weightedNonReplannerCountDifferences;
		final DynamicData<Id<?>> unweightedNonReplannerCountDifferences;
		if (this.greedoConfig.getDetailedLogging()) {
			weightedReplannerCountDifferences = new DynamicData<>(greedoConfig.newTimeDiscretization());
			unweightedReplannerCountDifferences = new DynamicData<>(greedoConfig.newTimeDiscretization());
			weightedNonReplannerCountDifferences = new DynamicData<>(greedoConfig.newTimeDiscretization());
			unweightedNonReplannerCountDifferences = new DynamicData<>(greedoConfig.newTimeDiscretization());
		} else {
			weightedReplannerCountDifferences = null;
			unweightedReplannerCountDifferences = null;
			weightedNonReplannerCountDifferences = null;
			unweightedNonReplannerCountDifferences = null;
		}

		this.replannerUtilityChangeSum = 0.0;
		this.nonReplannerUtilityChangeSum = 0.0;

		this.replannerSizeSum = 0.0;
		this.nonReplannerSizeSum = 0.0;

		final Set<Id<Person>> replanners = new LinkedHashSet<>();

		for (Id<Person> personId : allPersonIdsShuffled) {

			final ScoreUpdater<Id<?>> scoreUpdater = new ScoreUpdater<>(this.personId2physicalSlotUsage.get(personId),
					this.personId2hypothetialSlotUsage.get(personId), this.lambdaBar,
					this.beta, interactionResiduals, inertiaResidual, this.personId2UtilityChange.get(personId), sumOfInteractionResiduals2);

			final boolean replanner = recipe.isReplanner(personId, scoreUpdater.getScoreChangeIfOne(),
					scoreUpdater.getScoreChangeIfZero());

			if (replanner) {
				replanners.add(personId);
				if (this.greedoConfig.getDetailedLogging()) {
					CountIndicatorUtils.addIndicatorsToTotalsTreatingNullAsZero(weightedReplannerCountDifferences,
							unweightedReplannerCountDifferences, this.personId2hypothetialSlotUsage.get(personId),
							+1.0);
					CountIndicatorUtils.addIndicatorsToTotalsTreatingNullAsZero(weightedReplannerCountDifferences,
							unweightedReplannerCountDifferences, this.personId2physicalSlotUsage.get(personId), -1.0);
				}
				this.replannerUtilityChangeSum += this.personId2UtilityChange.get(personId);
				if (this.personId2physicalSlotUsage.containsKey(personId)) {
					this.replannerSizeSum += this.personId2physicalSlotUsage.get(personId).size();
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
				this.nonReplannerUtilityChangeSum += this.personId2UtilityChange.get(personId);
				if (this.personId2physicalSlotUsage.containsKey(personId)) {
					this.nonReplannerSizeSum += this.personId2physicalSlotUsage.get(personId).size();
				}
			}

			// Interaction residuals are updated by reference.
			scoreUpdater.updateResiduals(replanner ? 1.0 : 0.0);
			inertiaResidual = scoreUpdater.getUpdatedInertiaResidual();
			sumOfInteractionResiduals2 = scoreUpdater.getUpdatedSumOfInteractionResiduals2();
		}

		// >>> collect statistics, only for logging >>>

		if (this.greedoConfig.getDetailedLogging()) {
			this.sumOfUnweightedReplannerCountDifferences2 = CountIndicatorUtils
					.sumOfEntries2(unweightedReplannerCountDifferences);
			this.sumOfWeightedReplannerCountDifferences2 = CountIndicatorUtils
					.sumOfEntries2(weightedReplannerCountDifferences);
			this.sumOfUnweightedNonReplannerCountDifferences2 = CountIndicatorUtils
					.sumOfEntries2(unweightedNonReplannerCountDifferences);
			this.sumOfWeightedNonReplannerCountDifferences2 = CountIndicatorUtils
					.sumOfEntries2(weightedNonReplannerCountDifferences);
		}

		// <<< collect statistics, only for logging <<<

		return replanners;
	}
}
