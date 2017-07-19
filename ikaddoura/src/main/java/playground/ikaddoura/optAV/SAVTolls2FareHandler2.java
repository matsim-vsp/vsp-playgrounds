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
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.contrib.noise.personLinkMoneyEvents.PersonLinkMoneyEvent;
import org.matsim.contrib.noise.personLinkMoneyEvents.PersonLinkMoneyEventHandler;
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

public class SAVTolls2FareHandler2 implements LinkEnterEventHandler, ActivityEndEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, PersonLinkMoneyEventHandler {
	private static final Logger log = Logger.getLogger(SAVTolls2FareHandler2.class);
	
	private Set<Id<Person>> taxiDrivers = new HashSet<>();
	private Set<Id<Vehicle>> taxiVehicles = new HashSet<>();
	private Map<Id<Person>, Id<Vehicle>> taxiDriver2Vehicle = new HashMap<>();
	private Map<Id<Vehicle>, Double> taxiVehicle2amount = new HashMap<>();
	private Map<Id<Vehicle>, Id<Person>> vehicle2person = new HashMap<>();
	
//	private Map<Id<Link>, Map<Integer, > > linkId2
	
	@Inject
	private EventsManager events;

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
				
		if (taxiVehicles.contains(event.getVehicleId())) {
			
			// taxi driver
			
			taxiDriver2Vehicle.put(event.getPersonId(), event.getVehicleId());
			taxiVehicles.add(event.getVehicleId());

		} else {
			
			// passenger
			
			vehicle2person.put(event.getVehicleId(), event.getPersonId());
			
			if (taxiVehicle2amount.containsKey(event.getVehicleId())) {
				events.processEvent(new PersonMoneyEvent(event.getTime(), event.getPersonId(), taxiVehicle2amount.get(event.getVehicleId())));
				taxiVehicle2amount.remove(event.getVehicleId());
			}
		}
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if (taxiVehicles.contains(event.getVehicleId())) {
			
			// taxi driver
		
			taxiDriver2Vehicle.remove(event.getPersonId());
			
		} else {
			
			// passenger
			
			if (taxiVehicle2amount.containsKey(event.getVehicleId())) {
				events.processEvent(new PersonMoneyEvent(event.getTime(), event.getPersonId(), taxiVehicle2amount.get(event.getVehicleId())));
				taxiVehicle2amount.remove(event.getVehicleId());
			}
			
			vehicle2person.remove(event.getVehicleId());
			
		}
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (event.getActType().equals(VrpAgentLogic.BEFORE_SCHEDULE_ACTIVITY_TYPE)) {
			this.taxiDrivers.add(event.getPersonId());
		}		
	}

	@Override
	public void handleEvent(PersonLinkMoneyEvent event) {
				
		if (taxiDrivers.contains(event.getPersonId())) {
			// taxi driver pays a road charge
		
			Id<Vehicle> taxiVehicleId = this.taxiDriver2Vehicle.get(event.getPersonId());
			double relevantTime = event.getRelevantTime();
			
			Id<Person> passengerAtThatTime = null;
					
			// identify passenger at relevant time
			
			if (passengerAtThatTime == null) {
				if (this.taxiVehicle2amount.containsKey(event.getPersonId())) {
					this.taxiVehicle2amount.put(taxiDriver2Vehicle.get(event.getPersonId()), this.taxiVehicle2amount.get(event.getPersonId()) + event.getAmount());
				} else {
					this.taxiVehicle2amount.put(taxiDriver2Vehicle.get(event.getPersonId()), event.getAmount());
				}
			}
					
		} else {
			// ignore payments by passengers / normal cars
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		
	}

}

