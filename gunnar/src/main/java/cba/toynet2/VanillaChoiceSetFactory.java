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
package cba.toynet2;

import java.util.*;
import cba.resampling.ChoiceSetFactory;
import cba.resampling.MyGumbelDistribution;
import floetteroed.utilities.math.MultinomialLogit;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class VanillaChoiceSetFactory implements ChoiceSetFactory<PlanForResampling> {

	// -------------------- MEMBERS --------------------

	private final double sampersLogitScale;

	private final Random rnd;

	private final Scenario scenario;

	private final UtilityFunction sampersUtilityFunction;

	private final int numberOfDraws;

	// -------------------- CONSTRUCTION --------------------

	VanillaChoiceSetFactory(final double sampersLogitScale, final double sampersDefaultDestModeUtil,
			final double sampersDefaultTimeUtil, final Random rnd, final Scenario scenario, final int numberOfDraws) {
		this.sampersLogitScale = sampersLogitScale;
		this.rnd = rnd;
		this.scenario = scenario;
		this.sampersUtilityFunction = new UtilityFunction(sampersDefaultDestModeUtil, sampersDefaultTimeUtil);
		this.numberOfDraws = numberOfDraws;
	}

	// --------------- IMPLEMENTATION OF ChoiceSetProvider ---------------

	@Override
	public Set<PlanForResampling> newChoiceSet(final Person person) {

		final MultinomialLogit sampersMNL = new MultinomialLogit(TourSequence.Type.values().length, 1);
		sampersMNL.setUtilityScale(this.sampersLogitScale);
		sampersMNL.setCoefficient(0, 1.0);

		// define universal choice set

		final List<Double> activityModeUtilities = new ArrayList<>(TourSequence.Type.values().length);
		final List<Double> sampersTravelTimeUtilities = new ArrayList<>(TourSequence.Type.values().length);
		for (int i = 0; i < TourSequence.Type.values().length; i++) {
			final TourSequence tourSeq = new TourSequence(TourSequence.Type.values()[i]);
			final double activityModeUtility = this.sampersUtilityFunction.getActivityModeUtility(tourSeq.type);
			final double sampersTimeUtility = this.sampersUtilityFunction.getSampersTimeUtility(tourSeq.type);
			activityModeUtilities.add(activityModeUtility);
			sampersTravelTimeUtilities.add(sampersTimeUtility);
			sampersMNL.setAttribute(i, 0, activityModeUtility + sampersTimeUtility);
		}
		sampersMNL.enforcedUpdate();

		// sample choice set

		final Map<Integer, PlanForResampling> plansForResampling = new LinkedHashMap<>();
		for (int i = 0; i < this.numberOfDraws; i++) {
			final int planIndex = sampersMNL.draw(this.rnd);
			if (!plansForResampling.containsKey(planIndex)) {
				final TourSequence tourSequence = new TourSequence(TourSequence.Type.values()[planIndex]);
				final PlanForResampling planForResampling = new PlanForResampling(tourSequence,
						tourSequence.asPlan(this.scenario, person), activityModeUtilities.get(planIndex),
						sampersTravelTimeUtilities.get(planIndex), sampersMNL.getProbs().get(planIndex),
						new MyGumbelDistribution(this.sampersLogitScale));
				plansForResampling.put(planIndex, planForResampling);
			}
		}

		return new LinkedHashSet<>(plansForResampling.values());
	}
}
