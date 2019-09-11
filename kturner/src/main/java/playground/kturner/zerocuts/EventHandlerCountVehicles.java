/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package playground.kturner.zerocuts;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.vehicles.Vehicle;

class EventHandlerCountVehicles implements BasicEventHandler{
	static Logger log = Logger.getLogger(EventHandlerCountVehicles.class);

	private LinkedList<Id<Vehicle>> vehicleIds_other = new LinkedList();
	private LinkedList<Id<Vehicle>> vehicleIds_freight = new LinkedList();
	private LinkedList<Id<Vehicle>> vehicleIds_tr = new LinkedList();
	private LinkedList<Id<Vehicle>> vehicleIds_unequalPersonIdAll = new LinkedList();
	private LinkedList<Id<Vehicle>> vehicleIds_unequalPersonIdOther = new LinkedList();
	private LinkedHashMap<Id<Person>, Integer> numCarTrip = new LinkedHashMap<>();

	public void handleEvent(Event event) {
		if (event instanceof VehicleEntersTrafficEvent) { 
			Id<Vehicle> vehicleId = ((VehicleEntersTrafficEvent) event).getVehicleId();
			Id<Person> personId = ((VehicleEntersTrafficEvent) event).getPersonId();
			if(vehicleId.toString() != personId.toString()) {
				vehicleIds_unequalPersonIdAll.add(vehicleId);
			}
			
			if(vehicleId.toString().startsWith("freight_")) {
				if (! vehicleIds_freight.contains(vehicleId )){
					vehicleIds_freight.add(vehicleId);
				} else {
//					log.debug("Vehicle already in List: " + vehicleId);
				}
			} 
			else if(vehicleId.toString().startsWith("tr_")) {
				if (! vehicleIds_tr.contains(vehicleId )){
					vehicleIds_tr.add(vehicleId);
				} else {
//					log.debug("Vehicle already in List: " + vehicleId);
				}
			}
			else {
				//Count number of trips per Person using car
				if (((VehicleEntersTrafficEvent) event).getNetworkMode() == "car"){
					if(numCarTrip.containsKey(personId)) {
						numCarTrip.put(personId, numCarTrip.get(personId)+1);
					} else {
						numCarTrip.put(personId, 1);
					}
				}

				if(vehicleId.toString() != personId.toString()) {
					vehicleIds_unequalPersonIdOther.add(vehicleId);
				}
				if (! vehicleIds_other.contains(vehicleId )){
					vehicleIds_other.add(vehicleId);
				}
				else {
//					log.debug("Vehicle already in List: " + vehicleId);
				}
			}
		}
	}

	void reset() {
		vehicleIds_other.clear();
		vehicleIds_freight.clear();
		vehicleIds_tr.clear();
		vehicleIds_unequalPersonIdAll.clear();
		vehicleIds_unequalPersonIdOther.clear();
		numCarTrip.clear();
		log.info("cleared VehicleIds lists");
	}

	int getNumberOfCars() {
		return vehicleIds_other.size();
	}

	int getNumberOfFreightVehicles() {
		return vehicleIds_freight.size();
	}

	int getNumberOfTransitVehicles() {
		return vehicleIds_tr.size();
	}
	
	int getNumberOfVehicleUnequalPersonAll() {
		return vehicleIds_unequalPersonIdAll.size();
	}
	
	int getNumberOfVehicleUnequalPersonOther() {
		return vehicleIds_unequalPersonIdOther.size();
	}
	
	TreeMap<Integer, Integer> getNumberOfCarTripsPerAgent() {
		TreeMap<Integer, Integer> numberOfCarTripsPerAgent = new TreeMap<>();
		for (int i: numCarTrip.values()) {
			if (numberOfCarTripsPerAgent.containsKey(i)) {
				numberOfCarTripsPerAgent.put(i, numberOfCarTripsPerAgent.get(i)+1);
			} else {
				numberOfCarTripsPerAgent.put(i, 1);
			}
		}
		return numberOfCarTripsPerAgent;
	}
}