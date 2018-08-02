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

package playground.ikaddoura.optAV.disutility;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.noise.personLinkMoneyEvents.PersonLinkMoneyEvent;
import org.matsim.contrib.taxi.optimizer.DefaultTaxiOptimizerProvider;
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

public class DvrpMoneyTimeDistanceTravelDisutility implements TravelDisutility {
	private static final Logger log = Logger.getLogger(DvrpMoneyTimeDistanceTravelDisutility.class);

	private MoneyEventAnalysis moneyEventAnalysis;
	private AgentFilter vehicleFilter;
	private final TravelTime travelTime;
	private final double marginalUtilityOfMoney;
	private final double utilsPerSec;
	private final double utilsPerM;
	private final double costPerM;
	private final double timeBinSize;
	
	public DvrpMoneyTimeDistanceTravelDisutility(
			TravelTime travelTime,
			Scenario scenario,
			MoneyEventAnalysis moneyAnalysis,
			AgentFilter vehicleFilter) {
		
		this.travelTime = travelTime;
		this.timeBinSize = scenario.getConfig().travelTimeCalculator().getTraveltimeBinSize();
		this.utilsPerSec = scenario.getConfig().planCalcScore().getModes().get(DefaultTaxiOptimizerProvider.TAXI_OPTIMIZER).getMarginalUtilityOfTraveling();
		this.utilsPerM = scenario.getConfig().planCalcScore().getModes().get(DefaultTaxiOptimizerProvider.TAXI_OPTIMIZER).getMarginalUtilityOfDistance();
		this.costPerM = scenario.getConfig().planCalcScore().getModes().get(DefaultTaxiOptimizerProvider.TAXI_OPTIMIZER).getMonetaryDistanceRate();
		this.marginalUtilityOfMoney = scenario.getConfig().planCalcScore().getMarginalUtilityOfMoney();

		if (utilsPerSec > 0. || utilsPerM > 0. || costPerM > 0. || marginalUtilityOfMoney < 0.) {
			log.warn("utilsPerSec: " + utilsPerSec);
			log.warn("utilsPerM: " + utilsPerM);
			log.warn("costPerM: " + costPerM);
			log.warn("marginalUtilityOfMoney: " + marginalUtilityOfMoney);

			throw new RuntimeException("Check scoring parameters for " + DefaultTaxiOptimizerProvider.TAXI_OPTIMIZER + ". Travel disutility may become positive. Aborting...");
		}
		
		if ((utilsPerSec + utilsPerM + costPerM) == 0.) {
			log.warn("Check scoring parameters for " + DefaultTaxiOptimizerProvider.TAXI_OPTIMIZER + " (utilsPerSec + utilsPerM + costPerM = 0.).");
		}
		
		this.vehicleFilter = vehicleFilter;
		this.moneyEventAnalysis = moneyAnalysis;	
	}

	@Override
	public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
		
		double travelTimeDisutility = 0.;
		
		if (travelTime != null) {
			travelTimeDisutility = -1. * utilsPerSec * travelTime.getLinkTravelTime(link, time, person, vehicle);
		} else {
			travelTimeDisutility = -1. * utilsPerSec * (link.getLength() / link.getFreespeed());
		}
		
		double linkExpectedTollDisutility = calculateExpectedTollDisutility(link, time, person, vehicle);
		
		double distanceDisutility = -1. * utilsPerM * link.getLength() + -1. * costPerM * link.getLength() * marginalUtilityOfMoney;
				
		return travelTimeDisutility + linkExpectedTollDisutility + distanceDisutility;
	}

	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		return -1. * utilsPerSec * (link.getLength() / link.getFreespeed()) - 1. * utilsPerM * link.getLength() + -1. * costPerM * link.getLength() * marginalUtilityOfMoney;
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

