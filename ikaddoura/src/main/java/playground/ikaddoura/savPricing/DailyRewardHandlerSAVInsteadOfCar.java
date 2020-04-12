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

package playground.ikaddoura.savPricing;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;

import com.google.inject.Inject;

/**
* @author ikaddoura
*/

public final class DailyRewardHandlerSAVInsteadOfCar implements PersonEntersVehicleEventHandler {
	private static final Logger log = Logger.getLogger(DailyRewardHandlerSAVInsteadOfCar.class);

	private final Set<Id<Person>> passengersThatHaveAlreadyReceivedTheirReward = new HashSet<>();
	
	private int taxiUsersFormerNonCarUsers = 0;
	
	private int taxiUsersFormerCarUsers = 0;
	private double totalRewardsEarnedByTaxiUsersFormerCarUsers = 0.;
	
	@Inject
	private SAVPassengerTracker savTracker;
	
	@Inject
	private EventsManager eventsManager;
	
	@Inject
	private Scenario scenario;

	private final double dailyReward;
	private final String carMode;
	
	public DailyRewardHandlerSAVInsteadOfCar(double dailyReward, String carMode) {
		this.dailyReward = dailyReward;
		this.carMode = carMode;
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
					this.eventsManager.processEvent(new PersonMoneyEvent(event.getTime(), event.getPersonId(), dailyReward, null, null ));				
					
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

