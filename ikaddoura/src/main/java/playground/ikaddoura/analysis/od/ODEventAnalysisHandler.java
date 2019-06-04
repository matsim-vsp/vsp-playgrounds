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

package playground.ikaddoura.analysis.od;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;

/**
* @author ikaddoura
*/

public class ODEventAnalysisHandler implements TransitDriverStartsEventHandler, ActivityEndEventHandler, PersonDepartureEventHandler, PersonArrivalEventHandler {
	
	private final static Logger log = Logger.getLogger(ODEventAnalysisHandler.class);
	
	private final String[] helpLegModes;
	private final String helpActivitySubString;
	
	// temporary information
	private final Map<Id<Person>,Integer> personId2currentTripNumber = new HashMap<>();

	private final Set<Id<Person>> ptDrivers = new HashSet<>();
	private final Set<Id<Person>> taxiDrivers = new HashSet<Id<Person>>();

	// analysis information to be stored
	private final Map<Id<Person>,Map<Integer,String>> personId2tripNumber2legMode = new HashMap<>();
	private final Map<Id<Person>,Map<Integer,Double>> personId2tripNumber2departureTime = new HashMap<>();
	private final Map<Id<Person>,Map<Integer,Double>> personId2tripNumber2arrivalTime = new HashMap<>();
	private final Map<Id<Person>,Map<Integer,Id<Link>>> personId2tripNumber2departureLink = new HashMap<>();
	private final Map<Id<Person>,Map<Integer,Id<Link>>> personId2tripNumber2arrivalLink = new HashMap<>();

	public ODEventAnalysisHandler(String[] helpLegModes, String helpActivitySubString) {
		this.helpLegModes = helpLegModes;
		this.helpActivitySubString = helpActivitySubString;
		
		log.info("help leg modes: " + helpLegModes);
		log.info("help activity substring: " + helpActivitySubString);
	}

	@Override
	public void reset(int iteration) {
		personId2currentTripNumber.clear();
		personId2tripNumber2departureTime.clear();
		personId2tripNumber2arrivalTime.clear();
		personId2tripNumber2legMode.clear();
		ptDrivers.clear();
		taxiDrivers.clear();
	}
	
	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		ptDrivers.add(event.getDriverId());
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		
		if (event.getActType().equals(VrpAgentLogic.BEFORE_SCHEDULE_ACTIVITY_TYPE)) {
			this.taxiDrivers.add(event.getPersonId());
		}
		
		if (ptDrivers.contains(event.getPersonId()) || taxiDrivers.contains(event.getPersonId())){
			// activities by pt or taxi drivers are not considered
			
		} else {
			if (event.getActType().toString().contains(helpActivitySubString)){
				// pseudo activities are excluded
				
			} else {
				// a "real" activity
				
				if (personId2currentTripNumber.containsKey(event.getPersonId())) {
					// the following trip is at least the person's second trip
					personId2currentTripNumber.put(event.getPersonId(), personId2currentTripNumber.get(event.getPersonId()) + 1);
					
					Map<Integer,Double> tripNumber2departureTime = personId2tripNumber2departureTime.get(event.getPersonId());
					tripNumber2departureTime.put(personId2currentTripNumber.get(event.getPersonId()), event.getTime());
					personId2tripNumber2departureTime.put(event.getPersonId(), tripNumber2departureTime);
					
					Map<Integer,Id<Link>> tripNumber2departureLink = personId2tripNumber2departureLink.get(event.getPersonId());
					tripNumber2departureLink.put(personId2currentTripNumber.get(event.getPersonId()), event.getLinkId());
					personId2tripNumber2departureLink.put(event.getPersonId(), tripNumber2departureLink);
			
				} else {
					// the following trip is the person's first trip
					personId2currentTripNumber.put(event.getPersonId(), 1);
					
					Map<Integer,Double> tripNumber2departureTime = new HashMap<Integer, Double>();
					tripNumber2departureTime.put(1, event.getTime());
					personId2tripNumber2departureTime.put(event.getPersonId(), tripNumber2departureTime);
					
					Map<Integer,Id<Link>> tripNumber2departureLink = new HashMap<>();
					tripNumber2departureLink.put(1, event.getLinkId());
					personId2tripNumber2departureLink.put(event.getPersonId(), tripNumber2departureLink);

				}
			}	
		}
	}
	
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (ptDrivers.contains(event.getPersonId()) || taxiDrivers.contains(event.getPersonId())){
			// no normal person
			
		} else {
			if (personId2tripNumber2legMode.containsKey(event.getPersonId())) {
				// at least the person's second trip
				int tripNumber = personId2currentTripNumber.get(event.getPersonId());
				Map<Integer,String> tripNumber2legMode = personId2tripNumber2legMode.get(event.getPersonId());
				
				if (tripNumber2legMode.get(tripNumber) == null) {
					// save the help leg mode (better than to have nothing; there may be transit_walk trips without any main mode leg)
					tripNumber2legMode.put(personId2currentTripNumber.get(event.getPersonId()), event.getLegMode());
					personId2tripNumber2legMode.put(event.getPersonId(), tripNumber2legMode);
				
				} else {
					// there is already a mode stored for the current trip, only overwrite help leg modes
					boolean isHelpLeg = false;
					for (String helpLegMode : helpLegModes) {
						if(event.getLegMode().toString().equals(helpLegMode)) {
							isHelpLeg = true;
						}
					}
					if (!isHelpLeg) {
						// no help leg -> save the leg mode
						tripNumber2legMode.put(personId2currentTripNumber.get(event.getPersonId()), event.getLegMode());
						personId2tripNumber2legMode.put(event.getPersonId(), tripNumber2legMode);	
					}
				}
				
			} else {
				// the person's first trip
				Map<Integer,String> tripNumber2legMode = new HashMap<Integer,String>();
				tripNumber2legMode.put(1, event.getLegMode());
				personId2tripNumber2legMode.put(event.getPersonId(), tripNumber2legMode);
			}
		}		
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (ptDrivers.contains(event.getPersonId()) || taxiDrivers.contains(event.getPersonId())) {
			// no normal person
			
		} else {
			int currentTripNumber = this.personId2currentTripNumber.get(event.getPersonId());
			
			 // arrival time + arrival link
			Map<Integer, Double> tripNumber2arrivalTime;
			Map<Integer,Id<Link>> tripNumber2arrivalLink;
			if (this.personId2tripNumber2arrivalTime.containsKey(event.getPersonId())) {
				tripNumber2arrivalTime = this.personId2tripNumber2arrivalTime.get(event.getPersonId());
				tripNumber2arrivalLink = this.personId2tripNumber2arrivalLink.get(event.getPersonId());
			} else {
				tripNumber2arrivalTime = new HashMap<>();
				tripNumber2arrivalLink = new HashMap<>();
			}
			tripNumber2arrivalTime.put(currentTripNumber, event.getTime());
			personId2tripNumber2arrivalTime.put(event.getPersonId(), tripNumber2arrivalTime);
			
			tripNumber2arrivalLink.put(currentTripNumber, event.getLinkId());
			personId2tripNumber2arrivalLink.put(event.getPersonId(), tripNumber2arrivalLink);
		}
	}
	
	public Map<Id<Person>, Map<Integer, String>> getPersonId2tripNumber2legMode() {
		return personId2tripNumber2legMode;
	}

	public Map<Id<Person>, Map<Integer, Double>> getPersonId2tripNumber2departureTime() {
		return personId2tripNumber2departureTime;
	}
	
	public Map<Id<Person>, Map<Integer, Double>> getPersonId2tripNumber2arrivalTime() {
		return personId2tripNumber2arrivalTime;
	}

	public Map<Id<Person>, Map<Integer, Id<Link>>> getPersonId2tripNumber2departureLink() {
		return personId2tripNumber2departureLink;
	}

	public Map<Id<Person>, Map<Integer, Id<Link>>> getPersonId2tripNumber2arrivalLink() {
		return personId2tripNumber2arrivalLink;
	}
	
}

