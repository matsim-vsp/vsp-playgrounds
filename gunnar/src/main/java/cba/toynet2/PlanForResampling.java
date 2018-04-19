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

import cba.resampling.Alternative;
import cba.resampling.EpsilonDistribution;
import org.matsim.api.core.v01.population.Plan;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class PlanForResampling implements Alternative {

	// -------------------- MEMBERS --------------------d

	private final TourSequence tourSequence;

	private final Plan plan;

	private final double activityModeUtility;

	private final double sampersTravelTimeUtility;

	private final double sampersChoiceProba;

	private final EpsilonDistribution epsDistr;

	// TODO NEW
	private final double matsimTimeScoreOffset;

	private double matsimTimeScore;

	private Double epsilonRealization = null;

	// -------------------- CONSTRUCTION --------------------

	PlanForResampling(final TourSequence tourSequence, final Plan plan, final double activityModeUtility,
			final double sampersTravelTimeUtility, final double sampersChoiceProba,
			final EpsilonDistribution epsDistr) {
		this.tourSequence = tourSequence;
		this.plan = plan;
		this.activityModeUtility = activityModeUtility;
		this.sampersTravelTimeUtility = sampersTravelTimeUtility;
		this.matsimTimeScore = sampersTravelTimeUtility;
		this.sampersChoiceProba = sampersChoiceProba;
		this.epsDistr = epsDistr;

		// TODO NEW
		if (TourSequence.Type.work_car.equals(tourSequence.type)
				|| (TourSequence.Type.work_pt.equals(tourSequence.type))) {
			this.matsimTimeScoreOffset = 102.0; // TODO HACK!!!
		} else {
			this.matsimTimeScoreOffset = 0.0;
		}
	}

	// -------------------- FOR TESTING --------------------

	TourSequence getTourSequence() {
		return this.tourSequence;
	}

	// -------------------- IMPLEMENTATION OF Attribute --------------------

	@Override
	public double getSampersOnlyScore() {
		return this.activityModeUtility;
	}

	@Override
	public double getSampersChoiceProbability() {
		return this.sampersChoiceProba;
	}

	@Override
	public EpsilonDistribution getEpsilonDistribution() {
		return this.epsDistr;
	}

	@Override
	public double getSampersTimeScore() {
		return this.sampersTravelTimeUtility;
	}

	@Override
	public double getMATSimTimeScore() {
		// TODO NEW
		return (this.matsimTimeScore + this.matsimTimeScoreOffset);
	}

	@Override
	public double getSampersEpsilonRealization() {
		return this.epsilonRealization;
	}

	@Override
	public void setSampersEpsilonRealization(double eps) {
		this.epsilonRealization = eps;
	}

	// TODO NEW
	// @Override
	// public void setMATSimTimeScore(double score) {
	// this.matsimTimeScore = score;
	// }

	// TODO NEW
	@Override
	public void updateMATSimTimeScore(double score, double innovationWeight) {
		this.matsimTimeScore = (1.0 - innovationWeight) * this.matsimTimeScore + innovationWeight * score;
	}

	@Override
	public Plan getMATSimPlan() {
		return this.plan;
	}

	// -------------------- OVERRIDING OF Object --------------------

	@Override
	public String toString() {
		final StringBuffer result = new StringBuffer();
		result.append(this.getClass().getSimpleName() + " for person " + this.plan.getPerson() + " and type "
				+ this.tourSequence.type + "\n");
		result.append("V(type,dest,mode) = " + this.activityModeUtility + "\n");
		result.append("V_Sampers(time)   = " + this.sampersTravelTimeUtility + "\n");
		result.append("V_MATSim(time)    = " + this.matsimTimeScore + " + " + this.matsimTimeScoreOffset + "\n");
		result.append("P_sampers(this)   = " + this.sampersChoiceProba);
		return result.toString();
	}

}
