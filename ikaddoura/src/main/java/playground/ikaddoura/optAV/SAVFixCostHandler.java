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

package playground.ikaddoura.optAV;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;

import com.google.inject.Inject;

/**
* @author ikaddoura
*/

public class SAVFixCostHandler implements PersonEntersVehicleEventHandler {

	private final Set<Id<Person>> passengersThatHaveAlreadyPaid = new HashSet<>();
	
	@Inject
	private SAVPassengerTracker savTracker;
	
	@Inject
	private EventsManager eventsManager;
	
	@Inject
	private Scenario scenario;
	
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (savTracker.getTaxiDrivers().contains(event.getPersonId())) {
			// taxi driver
			
		} else {
			// passenger
			
			if (savTracker.getTaxiVehicles().contains(event.getVehicleId())) {
				
				// passenger getting into a taxi
				
				if (!passengersThatHaveAlreadyPaid.contains(event.getPersonId())) {
					Person person = scenario.getPopulation().getPersons().get(event.getPersonId());
					boolean carOwnerInBaseCase = (boolean) person.getAttributes().getAttribute("CarOwnerInBaseCase");
					
					double costsPerDay = 0.;
					if (carOwnerInBaseCase && personWithoutCarTrips(person.getSelectedPlan())) {
						costsPerDay = (-1) * ConfigUtils.addOrGetModule(this.scenario.getConfig(), OptAVConfigGroup.class).getFixCostsSAVinsteadOfCar();					
					} else {
						costsPerDay = (-1) * ConfigUtils.addOrGetModule(this.scenario.getConfig(), OptAVConfigGroup.class).getFixCostSAV();
					}
					this.eventsManager.processEvent(new PersonMoneyEvent(event.getTime(), event.getPersonId(), costsPerDay ));
					
					this.passengersThatHaveAlreadyPaid.add(event.getPersonId());
				} else {
					// passenger has already paid...
				}
			}
		}
	}

	private boolean personWithoutCarTrips(Plan selectedPlan) {
		boolean personWithoutCarTrips = true;
		for (PlanElement pE : selectedPlan.getPlanElements()) {
			if (pE instanceof Leg) {
				Leg leg = (Leg) pE;
				if (leg.getMode().equals(TransportMode.car)) {
					personWithoutCarTrips = false;
				}	
			}	
		}
		return personWithoutCarTrips;
	}

	@Override
	public void reset(int iteration) {
		this.passengersThatHaveAlreadyPaid.clear();
	}

}

