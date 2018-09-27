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

package playground.ikaddoura.savPricing;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.vehicles.Vehicle;

/**
 * 
 * @author ikaddoura
 */

public class SAVPassengerTracker implements PersonDepartureEventHandler, PersonArrivalEventHandler, ActivityEndEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler {
	private static final Logger log = Logger.getLogger(SAVPassengerTracker.class);

	private final Set<Id<Person>> taxiDrivers = new HashSet<>();
	private final Set<Id<Person>> currentTaxiPassengers = new HashSet<>();

	private final Set<Id<Vehicle>> taxiVehicles = new HashSet<>();
	private final Map<Id<Vehicle>, Id<Person>> vehicle2passenger = new HashMap<>();
	private final Map<Id<Vehicle>, Id<Person>> vehicle2lastPassenger = new HashMap<>();

	@Override
	public void reset(int iteration) {
		this.vehicle2passenger.clear();
		this.vehicle2lastPassenger.clear();
		this.taxiDrivers.clear();
		this.taxiVehicles.clear();
		this.currentTaxiPassengers.clear();
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
	
		if (event.getActType().equals(VrpAgentLogic.BEFORE_SCHEDULE_ACTIVITY_TYPE)) {
			this.taxiDrivers.add(event.getPersonId());
		}			
	}
	
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().equals(TransportMode.taxi)) {
			currentTaxiPassengers.add(event.getPersonId());
		}
	}
	
	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (event.getLegMode().equals(TransportMode.taxi)) {
			currentTaxiPassengers.remove(event.getPersonId());
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		
		if (currentTaxiPassengers.contains(event.getPersonId())) {

			// passenger entering a taxi

			taxiVehicles.add(event.getVehicleId());
			
			vehicle2lastPassenger.put(event.getVehicleId(), event.getPersonId());

			if (vehicle2passenger.get(event.getVehicleId()) != null) {
				throw new RuntimeException("More than one passenger in one SAV. Not (yet) considered. Aborting...");
				
			} else {
				vehicle2passenger.put(event.getVehicleId(), event.getPersonId());
			}
			
		}
	}
	
	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
	
		if (currentTaxiPassengers.contains(event.getPersonId())) {
			
			// passenger leaving a taxi

			if (vehicle2passenger.get(event.getVehicleId()) != null) {
				vehicle2passenger.remove(event.getVehicleId());
			} else {
				log.warn("The passenger " + event.getPersonId() + " should have entered the taxi vehicle " + event.getVehicleId() + " before time = " + event.getTime() + ".");
				for (Id<Vehicle> vehicleId : vehicle2passenger.keySet()) {
					log.warn("taxi vehicle: " + vehicleId + " ## passenger: " + vehicle2passenger.get(vehicleId));
				}
			}
		}
	}

	public Set<Id<Person>> getTaxiDrivers() {
		return taxiDrivers;
	}

	/**
	 * May not have collected all taxi vehicle IDs at the time of passenger boarding.
	 * Should only be called after the passenger has entered the vehicle. 
	 */	
	public Set<Id<Vehicle>> getTaxiVehicles() {
		return taxiVehicles;
	}

	public Map<Id<Vehicle>, Id<Person>> getVehicle2passenger() {
		return vehicle2passenger;
	}

	public Map<Id<Vehicle>, Id<Person>> getVehicle2lastPassenger() {
		return vehicle2lastPassenger;
	}

	public Set<Id<Person>> getCurrentTaxiPassengers() {
		return currentTaxiPassengers;
	}
	
	public boolean isTaxiPassenger(Id<Person> personId) {
		if (this.currentTaxiPassengers.contains(personId)) {
			return true;
		} else {
			return false;
		}
	}
	
}

