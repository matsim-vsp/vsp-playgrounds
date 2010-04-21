/* *********************************************************************** *
 * project: org.matsim.*
 * BKickLegScoring
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.benjamin.income2;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.RouteWRefs;
import org.matsim.core.scoring.CharyparNagelScoringParameters;
import org.matsim.core.scoring.charyparNagel.LegScoringFunction;
import org.matsim.households.Income;
import org.matsim.households.PersonHouseholdMapping;
import org.matsim.households.Income.IncomePeriod;

/**
 * @author dgrether
 * @author bkick
 * @author michaz
 * 
 *
 */
public class BKickIncome2LegScoring extends LegScoringFunction {

	private static final Logger log = Logger.getLogger(BKickIncome2LegScoring.class);

	private static double betaIncomeCar = 4.58;

	private static double betaIncomePt = 4.58;

	private double incomePerDay;

	private final Network network;

	public BKickIncome2LegScoring(final Plan plan, final CharyparNagelScoringParameters params, PersonHouseholdMapping hhdb, Network network) {
		super(plan, params);
		Income income = hhdb.getHousehold(plan.getPerson().getId()).getIncome();
		this.incomePerDay = this.calculateIncomePerDay(income);
		this.network = network;
		log.trace("Using BKickLegScoring...");
	}

	@Override
	public void finish() {
		this.score += (betaIncomeCar * Math.log(this.incomePerDay));
	}

	@Override
	protected double calcLegScore(final double departureTime, final double arrivalTime, final LegImpl leg) {
		double dist = calculateLegDistance(leg);
		double travelTime = arrivalTime - departureTime; // traveltime in seconds
		if (TransportMode.car.equals(leg.getMode())) {
			double betaIncome = betaIncomeCar;
			double distanceCostCar = this.params.marginalUtilityOfDistanceCar * dist;
			double distanceCost = distanceCostCar;
			double betaTravelTime = this.params.marginalUtilityOfTraveling;
			return calculateScore(betaIncome, distanceCost, betaTravelTime, travelTime);
		} else if (TransportMode.pt.equals(leg.getMode())) {
			double betaIncome = betaIncomePt;
			double distanceCostPt = this.params.marginalUtilityOfDistancePt * dist;
			double distanceCost = distanceCostPt;
			double betaTravelTime = this.params.marginalUtilityOfTravelingPT;
			return calculateScore(betaIncome, distanceCost, betaTravelTime, travelTime);
		} else {
			throw new IllegalStateException("Scoring funtion not defined for other modes than pt and car!");
		}
	}

	private double calculateLegDistance(final LegImpl leg) {
		RouteWRefs route = leg.getRoute();
		double dist = route.getDistance();
		if (TransportMode.car.equals(leg.getMode())) {
			dist += this.network.getLinks().get(route.getEndLinkId()).getLength();
		}
		
		if (Double.isNaN(dist)){
			throw new IllegalStateException("Route distance is NaN for person: " + this.plan.getPerson().getId());
		}
		return dist;
	}

	private double calculateScore(double betaIncome, double distanceCost, double betaTravelTime, double travelTime) {
		double betaCost = betaIncome / this.incomePerDay;
		double distanceScore = betaCost * distanceCost;
		double travelTimeScore = travelTime * betaTravelTime;
		double score = distanceScore + travelTimeScore;
		if (Double.isNaN(score)){
			throw new IllegalStateException("Leg score is NaN for person: " + this.plan.getPerson().getId());
		}
		return score;
	}

	private double calculateIncomePerDay(Income income) {
		if (income.getIncomePeriod().equals(IncomePeriod.year)) {
			double incomePerDay = income.getIncome() / 240;
			if (Double.isNaN(incomePerDay)){
				throw new IllegalStateException("cannot calculate income for person: " + this.plan.getPerson().getId());
			}
			return incomePerDay;
		} else {
			throw new UnsupportedOperationException("Can't calculate income per day");
		}
	}

}
