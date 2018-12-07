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

import java.util.Map;

import javax.inject.Provider;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.functions.SubpopulationScoringParameters;
import org.matsim.facilities.Facility;

import besttimeresponse.TimeAllocator;
import besttimeresponse.TripTravelTimes;
import besttimeresponseintegration.BestTimeResponseStrategyFunctionality;
import besttimeresponseintegration.BestTimeResponseTravelTimes;
import floetteroed.utilities.TimeDiscretization;
import floetteroed.utilities.Units;
import matsimintegration.TimeDiscretizationFactory;

/**
 * Computes optimal time structures for given travel plans.
 * 
 * @author Gunnar Flötteröd
 *
 */
class TimeStructureOptimizer {

	// -------------------- CONSTANTS --------------------

	private final Scenario scenario;

	private final TimeDiscretization timeDiscretization;

	private final SubpopulationScoringParameters scoringParams;

	final Provider<TripRouter> tripRouterProvider;

	private final int maxTrials;

	private final int maxFailures;

	private final Map<String, TravelTime> mode2travelTime;

	// -------------------- CONSTRUCTION --------------------

	TimeStructureOptimizer(final Scenario scenario, final Provider<TripRouter> tripRouterProvider, final int maxTrials,
			final int maxFailures, final Map<String, TravelTime> mode2travelTime) {
		this.scenario = scenario;
		this.timeDiscretization = TimeDiscretizationFactory.newInstance(scenario.getConfig());
		this.scoringParams = new SubpopulationScoringParameters(scenario);
		this.tripRouterProvider = tripRouterProvider;
		this.maxTrials = maxTrials;
		this.maxFailures = maxFailures;
		this.mode2travelTime = mode2travelTime;
	}

	// -------------------- IMPLEMENTATION --------------------

	double computeScoreAndSetDepartureTimes(final Plan plan) {

		final TripTravelTimes<Facility, String> travelTimes;
		if ((this.tripRouterProvider != null) && (this.mode2travelTime != null)) {
			// use simulated travel times
			travelTimes = new BestTimeResponseTravelTimes(this.scenario.getNetwork(), this.timeDiscretization,
					this.tripRouterProvider.get(), plan.getPerson(), this.mode2travelTime);
		} else if ((this.tripRouterProvider == null) && (this.mode2travelTime == null)) {
			// use fictitious teleportation travel times
			travelTimes = new TripTravelTimes<Facility, String>() {
				@Override
				public double getTravelTime_s(Facility origin, Facility destination, double dptTime_s, String mode) {
//					final Id<Link> fromLinkId = ((ActivityWrapperFacility) origin).getLinkId();
//					final Id<Link> toLinkId = ((ActivityWrapperFacility) destination).getLinkId();
//					final Link fromLink = scenario.getNetwork().getLinks().get(fromLinkId);
//					final Link toLink = scenario.getNetwork().getLinks().get(toLinkId);
//					final Coord fromCoord_km = CoordUtils.getCenter(fromLink.getFromNode().getCoord(),
//							fromLink.getToNode().getCoord());
//					final Coord toCoord_km = CoordUtils.getCenter(toLink.getFromNode().getCoord(),
//							toLink.getToNode().getCoord());
//					final double dist_km = CoordUtils.calcEuclideanDistance(fromCoord_km, toCoord_km);
//					final double time_h = dist_km / 10.0;
//					return Units.S_PER_H * time_h;
					return Units.S_PER_H * 0.25;
				}
			};
		} else {
			throw new RuntimeException();
		}

		final BestTimeResponseStrategyFunctionality initialPlanData = new BestTimeResponseStrategyFunctionality(plan,
				this.scenario.getNetwork(), this.scoringParams, this.timeDiscretization, travelTimes);
		final TimeAllocator<Facility, String> timeAlloc = initialPlanData.getTimeAllocator();
		timeAlloc.optimizeDepartureTimes(initialPlanData.plannedActivities, this.maxTrials, this.maxFailures);

		// set departure times in the plan that was passed to this function
		for (int i = 0; i < timeAlloc.getResultPoint().length; i++) {
			final Activity act = (Activity) plan.getPlanElements().get(2 * i);
			act.setEndTime(timeAlloc.getResultPoint()[i]);
		}

		// return the corresponding utility value
		return timeAlloc.getResultValue();
	}
}
