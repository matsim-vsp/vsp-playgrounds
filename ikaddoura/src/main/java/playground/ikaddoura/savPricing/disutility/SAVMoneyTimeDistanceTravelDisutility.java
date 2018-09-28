/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.ikaddoura.savPricing.disutility;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.noise.personLinkMoneyEvents.PersonLinkMoneyEvent;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

import playground.ikaddoura.moneyTravelDisutility.MoneyEventAnalysis;
import playground.ikaddoura.moneyTravelDisutility.data.AgentFilter;
import playground.ikaddoura.moneyTravelDisutility.data.LinkInfo;
import playground.ikaddoura.moneyTravelDisutility.data.TimeBin;

/**
* 
* A travel disutility which adds a term for monetary payments to the disutility which is passed into the constructor.
* The monetary payments are estimated based on time, link- and agent-specific {@link PersonLinkMoneyEvent}s in the previous iteration(s)
* that are caught and analyzed by {@link MoneyEventAnalysis}.
* 
* @author ikaddoura
*/

public class SAVMoneyTimeDistanceTravelDisutility implements TravelDisutility {
	private static final Logger log = Logger.getLogger(SAVMoneyTimeDistanceTravelDisutility.class);

	private MoneyEventAnalysis moneyEventAnalysis;
	private AgentFilter vehicleFilter;
	private final TravelTime travelTime;
	private final double marginalUtilityOfMoney;
	private final double marginalUtilityOfTime_sec;
	private final double marginalUtilityOfDistance_m;
	private final double timeBinSize;
	
	public SAVMoneyTimeDistanceTravelDisutility(
			TravelTime travelTime,
			Scenario scenario,
			MoneyEventAnalysis moneyAnalysis,
			AgentFilter vehicleFilter,
			String savOptimizerMode) {
		
		this.travelTime = travelTime;
		this.timeBinSize = scenario.getConfig().travelTimeCalculator().getTraveltimeBinSize();
		this.marginalUtilityOfMoney = scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney();

		this.marginalUtilityOfTime_sec = (scenario.getConfig().planCalcScore().getModes().get(savOptimizerMode).getMarginalUtilityOfTraveling() +
				(-1. * scenario.getConfig().planCalcScore().getPerforming_utils_hr() )  ) / 3600.;
		
		this.marginalUtilityOfDistance_m = scenario.getConfig().planCalcScore().getModes().get(savOptimizerMode).getMarginalUtilityOfDistance() + 
				scenario.getConfig().planCalcScore().getModes().get(savOptimizerMode).getMonetaryDistanceRate() * marginalUtilityOfMoney;

		log.info("marginalUtilityOfDistance_m: " + this.marginalUtilityOfDistance_m);
		log.info("marginalUtilityOfTime_sec: " + this.marginalUtilityOfTime_sec);
		
		if (marginalUtilityOfTime_sec > 0.) log.warn("Check marginalUtilityOfTime_sec: " + marginalUtilityOfTime_sec);
		if (marginalUtilityOfDistance_m > 0.) log.warn("Check marginalUtilityOfDistance_m: " + marginalUtilityOfDistance_m);
		if (marginalUtilityOfMoney < 0.) log.warn("Check marginalUtilityOfMoney: " + marginalUtilityOfMoney);

		if (marginalUtilityOfTime_sec >= 0. && marginalUtilityOfTime_sec >= 0.) {
			throw new RuntimeException("Check scoring parameters for " + savOptimizerMode + ".");
		}
		
		this.vehicleFilter = vehicleFilter;
		if (this.vehicleFilter == null) {
			log.info("vehicle filter is null. Not differentiating between different vehicle types...");
		}
		
		this.moneyEventAnalysis = moneyAnalysis;	
	}

	@Override
	public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
		
		double travelTimeDisutility = 0.;
		
		if (travelTime != null) {
			travelTimeDisutility = -1. * marginalUtilityOfTime_sec * travelTime.getLinkTravelTime(link, time, person, vehicle);
		} else {
			travelTimeDisutility = -1. * marginalUtilityOfTime_sec * (link.getLength() / link.getFreespeed());
		}
		
		double linkExpectedTollDisutility = calculateExpectedTollDisutility(link, time, person, vehicle);
		double distanceDisutility = -1. * marginalUtilityOfDistance_m * link.getLength();
				
		return travelTimeDisutility + linkExpectedTollDisutility + distanceDisutility;
	}

	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		return -1. *  marginalUtilityOfTime_sec * (link.getLength() / link.getFreespeed()) - 1. * marginalUtilityOfDistance_m * link.getLength();
	}

	private double calculateExpectedTollDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
				
		/* The following is an estimate of the tolls that an agent would have to pay if choosing that link in the next
		iteration i based on the tolls in iteration i-1 */
				
		int intervalNr = getIntervalNr(time);
		
		double estimatedAmount = 0.;

		LinkInfo linkInfo = moneyEventAnalysis.getLinkId2info().get(link.getId());
		if (linkInfo != null) {
						
			TimeBin timeBin = linkInfo.getTimeBinNr2timeBin().get(intervalNr);
			if (timeBin != null) {

				if(this.vehicleFilter != null) {
					Id<Person> personId = null;
					if (person != null) {
						personId = person.getId();
					} else {
						// person Id is null
					}
					String agentType = vehicleFilter.getAgentTypeFromId(personId);
					Double avgMoneyAmountVehicleType = timeBin.getAgentTypeId2avgAmount().get(agentType);
					
					if (avgMoneyAmountVehicleType != null && avgMoneyAmountVehicleType != 0.) {
						estimatedAmount = avgMoneyAmountVehicleType;
					} else {
						estimatedAmount = timeBin.getAverageAmount();
					}
				} else {
					estimatedAmount = timeBin.getAverageAmount();
				}
			}
		}
				
		double linkExpectedTollDisutility = -1 * estimatedAmount * this.marginalUtilityOfMoney;
		return linkExpectedTollDisutility;
	}
	
	private int getIntervalNr(double time) {
		int timeBinNr = (int) (time / timeBinSize);
		return timeBinNr;
	}
	
}

