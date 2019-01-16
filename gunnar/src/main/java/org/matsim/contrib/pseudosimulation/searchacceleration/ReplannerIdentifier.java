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
package org.matsim.contrib.pseudosimulation.searchacceleration;

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
import org.matsim.contrib.pseudosimulation.searchacceleration.datastructures.Ages;
import org.matsim.contrib.pseudosimulation.searchacceleration.datastructures.CountIndicatorUtils;
import org.matsim.contrib.pseudosimulation.searchacceleration.datastructures.ScoreUpdater;
import org.matsim.contrib.pseudosimulation.searchacceleration.datastructures.SpaceTimeIndicators;
import org.matsim.contrib.pseudosimulation.searchacceleration.recipes.AccelerationRecipe;
import org.matsim.contrib.pseudosimulation.searchacceleration.recipes.Mah2007Recipe;
import org.matsim.contrib.pseudosimulation.searchacceleration.recipes.Mah2009Recipe;
import org.matsim.contrib.pseudosimulation.searchacceleration.recipes.ReplannerIdentifierRecipe;
import org.matsim.contrib.pseudosimulation.searchacceleration.recipes.TreatConvergedAgentsSeparately;
import org.matsim.contrib.pseudosimulation.searchacceleration.recipes.UniformReplanningRecipe;

import floetteroed.utilities.DynamicData;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class ReplannerIdentifier {

	// -------------------- MEMBERS --------------------

	private final AccelerationConfigGroup replanningParameters;
	private final double lambda;
	private final double delta;

	private final Population population;
	private final Set<Id<Person>> convergedAgentIds;

	private final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> personId2physicalSlotUsage;
	private final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> personId2pseudoSimSlotUsage;
	// private final Map<Id<?>, Double> slotWeights;
	private final DynamicData<Id<?>> currentWeightedCounts;
	private final DynamicData<Id<?>> upcomingWeightedCounts;
	private final double sumOfWeightedCountDifferences2;

	private final double beta;

	private final Map<Id<Person>, Double> personId2ageWeightedUtilityChange;
	private final double totalAgeWeightedUtilityChange;

	private final Ages ages;

	// private Double shareOfScoreImprovingReplanners = null;
	// private Double score = null;

	// private SortedSet<IndividualReplanningResult> individualReplanningResults =
	// new TreeSet<>();

	// private Double uniformGreedyScoreChange = null;
	// private Double realizedGreedyScoreChange = null;

	// private Double uniformReplannerShare = null;

	// -------------------- GETTERS (FOR LOGGING) --------------------

	// public double getUniformReplanningObjectiveFunctionValue() {
	// return (2.0 - this.lambda) * this.lambda *
	// (this.sumOfWeightedCountDifferences2 + this.delta);
	// }

	// public Double getShareOfScoreImprovingReplanners() {
	// return this.shareOfScoreImprovingReplanners;
	// }

	// public Double getFinalObjectiveFunctionValue() {
	// return this.score;
	// }

	public Double getSumOfWeightedCountDifferences2() {
		return this.sumOfWeightedCountDifferences2;
	}

	public Double getMeanReplanningRate() {
		return this.lambda;
	}

	// public Double getUniformGreedyScoreChange() {
	// return this.uniformGreedyScoreChange;
	// }

	// public Double getRealizedGreedyScoreChange() {
	// return this.realizedGreedyScoreChange;
	// }

	// public Double getUniformReplannerShare() {
	// return this.uniformReplannerShare;
	// }

	// public List<IndividualReplanningResult>
	// getIndividualReplanningResultListView() {
	// return Collections.unmodifiableList(new
	// ArrayList<>(this.individualReplanningResults));
	// }

	// -------------------- CONSTRUCTION --------------------

	ReplannerIdentifier(final AccelerationConfigGroup replanningParameters, final int iteration,
			final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> personId2physicalSlotUsage,
			final Map<Id<Person>, SpaceTimeIndicators<Id<?>>> personId2pseudoSimSlotUsage,
			// final Map<Id<?>, Double> slotWeights,
			final Population population, final Map<Id<Person>, Double> personId2UtilityChange,
			// final double totalUtilityChange,
			// final double delta,
			final Set<Id<Person>> convergedAgentIds, final Set<Id<Person>> nonConvergedAgentIds, final Ages ages) {

		this.ages = ages;

		this.replanningParameters = replanningParameters;
		this.population = population;

		this.personId2ageWeightedUtilityChange = new LinkedHashMap<>(personId2UtilityChange.size());
		personId2UtilityChange.entrySet().stream().forEach(entry -> {
			final Id<Person> personId = entry.getKey();
			final double weightedUtilityChange = ages.getWeight(personId) * entry.getValue();
			this.personId2ageWeightedUtilityChange.put(personId, weightedUtilityChange);
		});
		// this.personId2utilityChange = personId2UtilityChange;
		// this.totalUtilityChange = totalUtilityChange;
		this.totalAgeWeightedUtilityChange = this.personId2ageWeightedUtilityChange.values().stream()
				.mapToDouble(utlChange -> utlChange).sum();

		this.personId2physicalSlotUsage = personId2physicalSlotUsage;
		this.personId2pseudoSimSlotUsage = personId2pseudoSimSlotUsage;
		// this.slotWeights = slotWeights;
		// this.currentWeightedCounts =
		// CountIndicatorUtils.newWeightedCounts(this.personId2physicalSlotUsage.values(),
		// slotWeights, this.replanningParameters.getTimeDiscretization());
		this.currentWeightedCounts = CountIndicatorUtils.newCounts(this.replanningParameters.getTimeDiscretization(),
				this.personId2physicalSlotUsage.values());
		// this.upcomingWeightedCounts =
		// CountIndicatorUtils.newWeightedCounts(this.personId2pseudoSimSlotUsage.values(),
		// slotWeights, this.replanningParameters.getTimeDiscretization());
		this.upcomingWeightedCounts = CountIndicatorUtils.newCounts(this.replanningParameters.getTimeDiscretization(),
				this.personId2pseudoSimSlotUsage.values());
		this.sumOfWeightedCountDifferences2 = CountIndicatorUtils.sumOfDifferences2(this.currentWeightedCounts,
				this.upcomingWeightedCounts);

		this.convergedAgentIds = convergedAgentIds;

		this.lambda = this.replanningParameters.getMeanReplanningRate(iteration);
		// this.beta = 2.0 * this.lambda * this.sumOfWeightedCountDifferences2 /
		// this.totalUtilityChange;
		this.beta = 2.0 * this.lambda * this.sumOfWeightedCountDifferences2 / this.totalAgeWeightedUtilityChange;
		this.delta = this.replanningParameters.getInitialRegularizationWeight() * 2.0
				* this.sumOfWeightedCountDifferences2;
	}

	// -------------------- IMPLEMENTATION --------------------

	Set<Id<Person>> drawReplanners() {

		// Initialize score residuals.

		final DynamicData<Id<?>> interactionResiduals = CountIndicatorUtils
				.newWeightedDifference(this.upcomingWeightedCounts, this.currentWeightedCounts, this.lambda);
		// double inertiaResidual = (1.0 - this.lambda) * this.totalUtilityChange;
		double inertiaResidual = (1.0 - this.lambda) * this.totalAgeWeightedUtilityChange;
		double regularizationResidual = 0;
		double sumOfInteractionResiduals2 = interactionResiduals.sumOfEntries2();

		// final DynamicData<Id<?>> uniformInteractionResiduals = CountIndicatorUtils
		// .newWeightedDifference(this.upcomingWeightedCounts,
		// this.currentWeightedCounts, this.lambda);
		// double uniformInertiaResidual = inertiaResidual;
		// double uniformRegularizationResidual = regularizationResidual;
		// double sumOfUniformInteractionResiduals2 = sumOfInteractionResiduals2;

		// Select the replanning recipe.

		final ReplannerIdentifierRecipe recipe;
		{
			final ReplannerIdentifierRecipe recipeForNonConvergedAgents;
			if (AccelerationConfigGroup.ModeType.off == this.replanningParameters.getModeTypeField()) {
				recipeForNonConvergedAgents = new UniformReplanningRecipe(this.lambda);
			} else if (AccelerationConfigGroup.ModeType.accelerate == this.replanningParameters.getModeTypeField()) {
				recipeForNonConvergedAgents = new AccelerationRecipe();
			} else if (AccelerationConfigGroup.ModeType.mah2007 == this.replanningParameters.getModeTypeField()) {
				// recipeForNonConvergedAgents = new Mah2007Recipe(this.personId2utilityChange,
				// this.lambda);
				recipeForNonConvergedAgents = new Mah2007Recipe(this.personId2ageWeightedUtilityChange, this.lambda);
			} else if (AccelerationConfigGroup.ModeType.mah2009 == this.replanningParameters.getModeTypeField()) {
				// recipeForNonConvergedAgents = new Mah2009Recipe(this.personId2utilityChange,
				// this.lambda);
				recipeForNonConvergedAgents = new Mah2009Recipe(this.personId2ageWeightedUtilityChange, this.lambda);
			} else {
				throw new RuntimeException("Unknown mode: " + this.replanningParameters.getModeTypeField());
			}
			recipe = new TreatConvergedAgentsSeparately(this.convergedAgentIds,
					new UniformReplanningRecipe(this.lambda), recipeForNonConvergedAgents);
		}

		// Go through all vehicles and decide which driver gets to re-plan.

		final Set<Id<Person>> replanners = new LinkedHashSet<>();
		final List<Id<Person>> allPersonIdsShuffled = new ArrayList<>(this.population.getPersons().keySet());

		Collections.shuffle(allPersonIdsShuffled);

		// this.score = 0.0; // this.getUniformReplanningObjectiveFunctionValue();

		// int scoreImprovingReplanners = 0;

		// this.realizedGreedyScoreChange = 0.0;
		// this.uniformGreedyScoreChange = 0.0;

		// int uniformReplanners = 0;

		for (Id<Person> driverId : allPersonIdsShuffled) {

			final ScoreUpdater<Id<?>> scoreUpdater = new ScoreUpdater<>(this.personId2physicalSlotUsage.get(driverId),
					this.personId2pseudoSimSlotUsage.get(driverId),
					// this.slotWeights,
					this.lambda, this.ages.getWeight(driverId), this.beta, this.delta, interactionResiduals,
					inertiaResidual, regularizationResidual, this.replanningParameters,
					// this.personId2utilityChange.get(driverId), this.totalUtilityChange,
					this.personId2ageWeightedUtilityChange.get(driverId), this.totalAgeWeightedUtilityChange,
					sumOfInteractionResiduals2);

			// final ScoreUpdater<Id<?>> uniformScoreUpdater = new ScoreUpdater<>(
			// this.personId2physicalSlotUsage.get(driverId),
			// this.personId2pseudoSimSlotUsage.get(driverId),
			// this.slotWeights, this.lambda, this.beta,
			// // this.delta,
			// uniformInteractionResiduals, uniformInertiaResidual,
			// uniformRegularizationResidual,
			// this.replanningParameters, this.personId2utilityChange.get(driverId),
			// this.totalUtilityChange,
			// sumOfUniformInteractionResiduals2);

			final boolean replanner = recipe.isReplanner(driverId, scoreUpdater.getScoreChangeIfOne(),
					scoreUpdater.getScoreChangeIfZero());
			if (replanner) {
				replanners.add(driverId);
				// this.score += scoreUpdater.getScoreChangeIfOne();
				// realizedGreedyScoreChange += scoreUpdater.getGreedyScoreChangeIfOne();
			}
			// else {
			// this.score += scoreUpdater.getScoreChangeIfZero();
			// realizedGreedyScoreChange += scoreUpdater.getGreedyScoreChangeIfZero();
			// }

			// this.uniformGreedyScoreChange += this.lambda *
			// scoreUpdater.getGreedyScoreChangeIfOne()
			// + (1.0 - this.lambda) * scoreUpdater.getGreedyScoreChangeIfZero();

			// if (Math.min(scoreUpdater.getScoreChangeIfOne(),
			// scoreUpdater.getScoreChangeIfZero()) < 0) {
			// scoreImprovingReplanners++;
			// }

			// if (replanner == uniformScoreUpdater.wouldBeUniformReplanner) {
			// uniformReplanners++;
			// }

			scoreUpdater.updateResiduals(replanner ? 1.0 : 0.0); // interaction residual by reference
			inertiaResidual = scoreUpdater.getUpdatedInertiaResidual();
			regularizationResidual = scoreUpdater.getUpdatedRegularizationResidual();
			sumOfInteractionResiduals2 = scoreUpdater.getUpdatedSumOfInteractionResiduals2();

			// uniformScoreUpdater.updateResiduals(uniformScoreUpdater.wouldBeUniformReplanner
			// ? 1.0 : 0.0);
			// uniformInertiaResidual = uniformScoreUpdater.getUpdatedInertiaResidual();
			// uniformRegularizationResidual =
			// uniformScoreUpdater.getUpdatedRegularizationResidual();
			// sumOfUniformInteractionResiduals2 =
			// uniformScoreUpdater.getUpdatedSumOfInteractionResiduals2();

			// for debugging
			// {
			// final double exactSum2 = interactionResiduals.sumOfEntries2();
			// log.info("Relative interaction residual error = "
			// + (sumOfInteractionResiduals2 - exactSum2) / exactSum2);
			// }

			// this.individualReplanningResults.add(new IndividualReplanningResult(driverId,
			// uniformScoreUpdater.getCriticalDelta(),
			// this.personId2utilityChange.get(driverId), replanner,
			// scoreUpdater.wouldBeUniformReplanner, scoreUpdater.wouldBeGreedyReplanner));
		}

		// this.shareOfScoreImprovingReplanners = ((double) scoreImprovingReplanners) /
		// allPersonIdsShuffled.size();
		// this.uniformReplannerShare = ((double) uniformReplanners) /
		// allPersonIdsShuffled.size();

		return replanners;
	}
}
