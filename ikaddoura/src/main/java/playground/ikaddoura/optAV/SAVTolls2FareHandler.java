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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;

/**
 * (1) identify taxi drivers.
 * (2) identify the passengers, i.e. the persons waiting for / sitting inside the SAV.
 * (3) throw money events for the passengers
 * 
* @author ikaddoura
*/

public class SAVTolls2FareHandler implements ActivityEndEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, PersonMoneyEventHandler {
	private static final Logger log = Logger.getLogger(SAVTolls2FareHandler.class);
	
	private Set<Id<Vehicle>> taxiVehicles = new HashSet<>();
	private Set<Id<Person>> taxiDrivers = new HashSet<>();
	private Map<Id<Person>, Id<Vehicle>> taxiDriver2Vehicle = new HashMap<>();
	private Map<Id<Vehicle>, Set<Id<Person>>> vehicle2passengers = new HashMap<>();
	
	@Inject
	private EventsManager events;
	
	@Override
	public void reset(int iteration) {
		this.taxiDrivers.clear();
		this.taxiDriver2Vehicle.clear();
		this.vehicle2passengers.clear();
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
				
		if (taxiDrivers.contains(event.getPersonId())) {
			
			// taxi driver
			
			taxiDriver2Vehicle.put(event.getPersonId(), event.getVehicleId());
			taxiVehicles.add(event.getVehicleId());
			
		} else {
			
			if (taxiVehicles.contains(event.getVehicleId())) {
				// passenger getting into a taxi

				if (vehicle2passengers.get(event.getVehicleId()) != null) {
					vehicle2passengers.get(event.getVehicleId()).add(event.getPersonId());
					
				} else {
					Set<Id<Person>> passengers = new HashSet<>();
					passengers.add(event.getPersonId());
					vehicle2passengers.put(event.getVehicleId(), passengers);
				}
			}
		}
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if (taxiDrivers.contains(event.getPersonId())) {
			
			// taxi driver
			vehicle2passengers.remove(event.getVehicleId());
			taxiDriver2Vehicle.remove(event.getPersonId());

		} else {
			
			// passenger
			
		}
	}
	
	@Override
	public void handleEvent(PersonMoneyEvent event) {
		if (taxiDrivers.contains(event.getPersonId())) {
			// taxi driver road charge	
			
			Id<Vehicle> taxiVehicleId = this.taxiDriver2Vehicle.get(event.getPersonId());
			log.info("taxi vehicle: " + taxiVehicleId);
			
			double numberOfPassengers = this.vehicle2passengers.get(taxiVehicleId).size();
			log.info("number of passengers: " + numberOfPassengers);
			
			double amount = event.getAmount() / numberOfPassengers;
			log.info("amount per passenger: " + amount);

			for (Id<Person> personId : this.vehicle2passengers.get(taxiVehicleId)) {
				PersonMoneyEvent moneyEvent = new PersonMoneyEvent(event.getTime(), personId, amount);
				events.processEvent(moneyEvent);
			}
								
		} else {
			// passenger
		}
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (event.getActType().equals(VrpAgentLogic.BEFORE_SCHEDULE_ACTIVITY_TYPE)) {
			this.taxiDrivers.add(event.getPersonId());
		}		
	}

}

