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
package cba.trianglenet;

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

	final Plan plan;

	private final double sampersOnlyScore;

	private final double sampersTimeScore;

	private final double matsimTimeScore;

	private final double sampersChoiceProba;

	private final EpsilonDistribution epsDistr;

	// for testing
	private Double matsimChoiceProba = null;

	// -------------------- CONSTRUCTION --------------------

	PlanForResampling(final Plan plan, final double sampersOnlyScore, final double sampersTimeScore,
			final double matsimTimeScore, final double sampersChoiceProba, final EpsilonDistribution epsDistr) {
		this.plan = plan;
		this.sampersOnlyScore = sampersOnlyScore;
		this.sampersTimeScore = sampersTimeScore;
		this.matsimTimeScore = matsimTimeScore;
		this.sampersChoiceProba = sampersChoiceProba;
		this.epsDistr = epsDistr;
	}

	// -------------------- FOR TESTING --------------------

	void setMATSimChoiceProba(final Double matsimChoiceProba) {
		this.matsimChoiceProba = matsimChoiceProba;
	}

	Double getMATSimChoiceProba() {
		return this.matsimChoiceProba;
	}

	// -------------------- IMPLEMENTATION OF Attribute --------------------

	@Override
	public double getSampersOnlyScore() {
		return this.sampersOnlyScore;
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
		return this.sampersTimeScore;
	}

	@Override
	public double getMATSimTimeScore() {
		return this.matsimTimeScore;
	}

	@Override
	public double getSampersEpsilonRealization() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setSampersEpsilonRealization(double eps) {
		// TODO Auto-generated method stub
		
	}

//	@Override
//	public void setMATSimTimeScore(double score) {
//		// TODO Auto-generated method stub
//		
//	}

	@Override
	public Plan getMATSimPlan() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateMATSimTimeScore(double score, double innovationWeight) {
		// TODO Auto-generated method stub
		
	}
}
