/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.ikaddoura.analysis.carOwnerShip;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;

import playground.ikaddoura.savPricing.SAVPassengerTracker;

/**
* @author ikaddoura
*/

public class SAVInsteadOfCarAnalysisHandler implements PersonEntersVehicleEventHandler {
	private static final Logger log = Logger.getLogger(SAVInsteadOfCarAnalysisHandler.class);

	private final Set<Id<Person>> passengersThatHaveAlreadyReceivedTheirReward = new HashSet<>();
	
	private int taxiUsersFormerNonCarUsers = 0;
	
	private int taxiUsersFormerCarUsers = 0;
	private double totalRewardsEarnedByTaxiUsersFormerCarUsers = 0.;
	
	private final SAVPassengerTracker savTracker;	
	private final Scenario scenario;

	private final double dailyReward;
	private final String carMode;
	
	public SAVInsteadOfCarAnalysisHandler(Scenario scenario, SAVPassengerTracker savTracker, double dailyReward, String carMode) {
		this.dailyReward = dailyReward;
		this.carMode = carMode;
		this.scenario = scenario;
		this.savTracker = savTracker;
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		
		if (savTracker.isTaxiPassenger(event.getPersonId())) {
			
			// passenger getting into a taxi
			
			if (!passengersThatHaveAlreadyReceivedTheirReward.contains(event.getPersonId())) {
				Person person = scenario.getPopulation().getPersons().get(event.getPersonId());
				
				boolean carOwnerInBaseCase = false;
				if (person.getAttributes().getAttribute("CarOwnerInBaseCase") == null) {
					log.warn("no person attribute 'CarOwnerInBaseCase = true/false' found. Assuming this person not to be a car owner.");
				} else {
					carOwnerInBaseCase = (boolean) person.getAttributes().getAttribute("CarOwnerInBaseCase");
				}
								
				if (carOwnerInBaseCase && personWithoutCarTrips(person.getSelectedPlan())) {
					taxiUsersFormerCarUsers++;
					totalRewardsEarnedByTaxiUsersFormerCarUsers += dailyReward;					
				} else {
					taxiUsersFormerNonCarUsers++;
				}
				this.passengersThatHaveAlreadyReceivedTheirReward.add(event.getPersonId());
			}
		}
	}

	private boolean personWithoutCarTrips(Plan selectedPlan) {
		boolean personWithoutCarTrips = true;
		for (PlanElement pE : selectedPlan.getPlanElements()) {
			if (pE instanceof Leg) {
				Leg leg = (Leg) pE;
				if (leg.getMode().equals(carMode)) {
					personWithoutCarTrips = false;
				}	
			}	
		}
		return personWithoutCarTrips;
	}

	@Override
	public void reset(int iteration) {
		
		this.passengersThatHaveAlreadyReceivedTheirReward.clear();
		
		this.taxiUsersFormerCarUsers = 0;
		this.taxiUsersFormerNonCarUsers = 0;
		this.totalRewardsEarnedByTaxiUsersFormerCarUsers = 0.;
	}

	public int getSavUsersFormerNonCarUsers() {
		return taxiUsersFormerNonCarUsers;
	}

	public int getSavUsersFormerCarUsers() {
		return taxiUsersFormerCarUsers;
	}

	public double getTotalSAVFixCostPaidBySAVusersFormerCarUsers() {
		return totalRewardsEarnedByTaxiUsersFormerCarUsers;
	}

}

