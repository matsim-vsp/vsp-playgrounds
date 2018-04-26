/*
 * Copyright 2018 Mohammad Saleem
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
 * contact: salee@kth.se
 *
 */ 
package saleem.gaming.resultanalysis;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.ScoringParameters;

import saleem.gaming.scenariobuilding.GamingConstants;

/**
 * A  class to score monetary events, and map money to utility.
 * 
 * @author Mohammad Saleem
 */
public final class StockholmMoneyScoring implements SumScoringFunction.MoneyScoring {

	private double score;
	

	private final double marginalUtilityOfMoney;
	
	private Person person;

	public StockholmMoneyScoring(Person person, final ScoringParameters params) {
		this.marginalUtilityOfMoney = params.marginalUtilityOfMoney*GamingConstants.marginalutilityfactor;
		this.person=person;
	}

	public StockholmMoneyScoring(Person person, final double marginalUtilityOfMoney) {
		this.marginalUtilityOfMoney = marginalUtilityOfMoney*GamingConstants.marginalutilityfactor;
	}

	@Override
	public void addMoney(final double amount) {
		this.score += amount * this.marginalUtilityOfMoney ; // linear mapping of money to score
	}

	@Override
	public void finish() {
	}

	@Override
	public double getScore() {
		return this.score;
	}

}
