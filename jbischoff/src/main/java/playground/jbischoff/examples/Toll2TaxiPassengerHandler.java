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

/**
 * 
 */
package playground.jbischoff.examples;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.mutable.MutableDouble;
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
 * @author  jbischoff
 * This event handler catches any money event thrown at taxi drivers and passes them to the taxi passenger. 
 * Should a taxi be empty, the toll will be charged to the next passenger (does not make sense if there's a re-balancing of vehicles)
 */
/**
 *
 */
public class Toll2TaxiPassengerHandler implements PersonMoneyEventHandler, ActivityEndEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler {

	Map<Id<Vehicle>,Id<Person>> vehicle2passenger = new HashMap<>();
	Map<Id<Vehicle>,MutableDouble> vehicle2SavedTolls = new HashMap<>();
	Set<Id<Person>> taxidrivers = new HashSet<>();
	private EventsManager events;
	
	
	@Inject
	public Toll2TaxiPassengerHandler(EventsManager events) {
		this.events = events;
		events.addHandler(this);
		
	}
	
	/* (non-Javadoc)
	 * @see org.matsim.core.events.handler.EventHandler#reset(int)
	 */
	@Override
	public void reset(int iteration) {
		taxidrivers.clear();
		vehicle2passenger.clear();
		vehicle2SavedTolls.clear();
	}
	
	
	
	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler#handleEvent(org.matsim.api.core.v01.events.PersonLeavesVehicleEvent)
	 */
	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if (vehicle2passenger.containsKey(event.getVehicleId())){
			vehicle2passenger.remove(event.getVehicleId());
		}
	}

	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler#handleEvent(org.matsim.api.core.v01.events.PersonEntersVehicleEvent)
	 */
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (vehicle2SavedTolls.containsKey(event.getVehicleId())&&(!taxidrivers.contains(event.getPersonId()))){
			vehicle2passenger.put(event.getVehicleId(), event.getPersonId());
			double tollForPickup = vehicle2SavedTolls.get(event.getVehicleId()).doubleValue();
			vehicle2SavedTolls.get(event.getVehicleId()).setValue(0);
			events.processEvent(new PersonMoneyEvent(event.getTime(), event.getPersonId(), tollForPickup));
			
		}
	}

	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.events.handler.ActivityEndEventHandler#handleEvent(org.matsim.api.core.v01.events.ActivityEndEvent)
	 */
	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (event.getActType().equals(VrpAgentLogic.BEFORE_SCHEDULE_ACTIVITY_TYPE)){
			Id<Vehicle> vid = Id.createVehicleId(event.getPersonId());
			this.vehicle2SavedTolls.put(vid, new MutableDouble());
			this.taxidrivers.add(event.getPersonId());
		}
	}

	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler#handleEvent(org.matsim.api.core.v01.events.PersonMoneyEvent)
	 */
	@Override
	public void handleEvent(PersonMoneyEvent event) {
		if (this.taxidrivers.contains(event.getPersonId())){
			Id<Vehicle> vid = Id.createVehicleId(event.getPersonId());
			if (vehicle2passenger.containsKey(vid)){
				Id<Person> passenger = vehicle2passenger.get(vid);
				events.processEvent(new PersonMoneyEvent(event.getTime(), passenger, event.getAmount()));
			} else {
				vehicle2SavedTolls.get(vid).add(event.getAmount());
			
			}
		}
	}

	
}
