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
import org.matsim.contrib.greedo.datastructures.Ages;
import org.matsim.contrib.greedo.datastructures.CountIndicatorUtils;
import org.matsim.contrib.greedo.datastructures.ScoreUpdater;
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

	private final double sumOfUnweightedCountDifferences2;
	private final double sumOfWeightedCountDifferences2;

	private final Map<Id<Person>, Double> personId2weightedUtilityChange;
	private final double totalWeightedUtilityChange;

	private final Ages ages;

	private final double lambda;
	private final double beta;
	private final double delta;

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

		this.personId2weightedUtilityChange = new LinkedHashMap<>(personId2UtilityChange.size());
		personId2UtilityChange.entrySet().stream().forEach(entry -> {
			final Id<Person> personId = entry.getKey();
			final double weightedUtilityChange = ages.getPersonWeights().get(personId) * entry.getValue();
			this.personId2weightedUtilityChange.put(personId, weightedUtilityChange);
		});
		this.totalWeightedUtilityChange = this.personId2weightedUtilityChange.values().stream()
				.mapToDouble(utlChange -> utlChange).sum();

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

		this.lambda = this.greedoConfig.getReplanningRate(greedoIteration);
		this.beta = 2.0 * this.lambda * this.sumOfWeightedCountDifferences2 / this.totalWeightedUtilityChange;
		this.delta = Math
				.pow(this.greedoConfig.getRegularizationThreshold()
						/ (1.0 - this.greedoConfig.getRegularizationThreshold()), 2.0)
				* this.sumOfUnweightedCountDifferences2;
	}

	// -------------------- GETTERS (FOR LOGGING) --------------------

	public Double getSumOfUnweightedCountDifferences2() {
		return this.sumOfUnweightedCountDifferences2;
	}

	public Double getSumOfWeightedCountDifferences2() {
		return this.sumOfWeightedCountDifferences2;
	}

	public Double getMeanReplanningRate() {
		return this.lambda;
	}

	// -------------------- IMPLEMENTATION --------------------

	Set<Id<Person>> drawReplanners() {

		// Initialize score residuals.

		final DynamicData<Id<?>> interactionResiduals = CountIndicatorUtils
				.newWeightedDifference(this.upcomingWeightedCounts, this.currentWeightedCounts, this.lambda);
		double inertiaResidual = (1.0 - this.lambda) * this.totalWeightedUtilityChange;
		double regularizationResidual = 0;
		double sumOfInteractionResiduals2 = interactionResiduals.sumOfEntries2();

		// Instantiate the re-planning recipe.

		final ReplannerIdentifierRecipe recipe;
		if (GreedoConfigGroup.ModeType.off == this.greedoConfig.getModeTypeField()) {
			recipe = new UniformReplanningRecipe(this.lambda);
		} else if (GreedoConfigGroup.ModeType.accelerate == this.greedoConfig.getModeTypeField()) {
			recipe = new AccelerationRecipe();
		} else if (GreedoConfigGroup.ModeType.mah2007 == this.greedoConfig.getModeTypeField()) {
			recipe = new Mah2007Recipe(this.personId2weightedUtilityChange, this.lambda);
		} else if (GreedoConfigGroup.ModeType.mah2009 == this.greedoConfig.getModeTypeField()) {
			recipe = new Mah2009Recipe(this.personId2weightedUtilityChange, this.lambda);
		} else {
			throw new RuntimeException("Unknown mode: " + this.greedoConfig.getModeTypeField());
		}

		// Go through all vehicles and decide which driver gets to re-plan.

		final Set<Id<Person>> replanners = new LinkedHashSet<>();

		final List<Id<Person>> allPersonIdsShuffled = new ArrayList<>(this.population.getPersons().keySet());
		Collections.shuffle(allPersonIdsShuffled);

		for (Id<Person> personId : allPersonIdsShuffled) {

			final ScoreUpdater<Id<?>> scoreUpdater = new ScoreUpdater<>(this.personId2physicalSlotUsage.get(personId),
					this.personId2pseudoSimSlotUsage.get(personId), this.lambda,
					this.ages.getPersonWeights().get(personId), this.beta, this.delta, interactionResiduals,
					inertiaResidual, regularizationResidual, this.greedoConfig,
					this.personId2weightedUtilityChange.get(personId), this.totalWeightedUtilityChange,
					sumOfInteractionResiduals2);

			final boolean replanner = recipe.isReplanner(personId, scoreUpdater.getScoreChangeIfOne(),
					scoreUpdater.getScoreChangeIfZero());
			if (replanner) {
				replanners.add(personId);
			}

			scoreUpdater.updateResiduals(replanner ? 1.0 : 0.0); // interaction residual by reference
			inertiaResidual = scoreUpdater.getUpdatedInertiaResidual();
			regularizationResidual = scoreUpdater.getUpdatedRegularizationResidual();
			sumOfInteractionResiduals2 = scoreUpdater.getUpdatedSumOfInteractionResiduals2();
		}

		return replanners;
	}
}
