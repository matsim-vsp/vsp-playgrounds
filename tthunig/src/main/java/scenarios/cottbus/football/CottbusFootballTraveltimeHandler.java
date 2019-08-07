/* *********************************************************************** *
 * project: org.matsim.*
 * CottbusFootballTraveltimeHandler
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package scenarios.cottbus.football;

import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.collections.Tuple;

import scenarios.cottbus.football.demand.CottbusFootballStrings;

/**
 * @author dgrether
 * 
 */
public class CottbusFootballTraveltimeHandler implements PersonDepartureEventHandler, PersonArrivalEventHandler, PersonStuckEventHandler {

	private Map<Id<Person>, Double> arrivaltimesSPN2FB;
	private Map<Id<Person>, Double> arrivaltimesCB2FB;
	private Map<Id<Person>, Double> arrivaltimesFB2SPN;
	private Map<Id<Person>, Double> arrivaltimesFB2CB;
	
	private Map<Id<Person>, Double> travelTimesPerPerson;
	
	private int numberOfStuckedPersons;

	public CottbusFootballTraveltimeHandler(){
		this.reset(0);
	}
	
	@Override
	public void reset(int iteration) {
		this.travelTimesPerPerson = new TreeMap<>();
		this.arrivaltimesFB2CB = new TreeMap<>();
		this.arrivaltimesFB2SPN = new TreeMap<>();
		this.arrivaltimesCB2FB = new TreeMap<>();
		this.arrivaltimesSPN2FB = new TreeMap<>();
		this.numberOfStuckedPersons = 0;
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		handleArrival(event.getPersonId(), event.getTime());
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		numberOfStuckedPersons++;
//		if (travelTimesPerPerson.containsKey(event.getPersonId())){
			// handle as arrival if already departured
			handleArrival(event.getPersonId(), event.getTime());
//		}
	}

	private void handleArrival(Id<Person> personId, double time) {
		double previousTT = travelTimesPerPerson.get(personId);
		travelTimesPerPerson.put(personId, previousTT + time);
		
		if (personId.toString().endsWith(CottbusFootballStrings.SPN2FB)) {
			Double tr = this.arrivaltimesSPN2FB.get(personId);
			if (tr == null) {
				this.arrivaltimesSPN2FB.put(personId, time);
			}
			else {
				this.arrivaltimesFB2SPN.put(personId, time);
			}
		}
		if (personId.toString().endsWith(CottbusFootballStrings.CB2FB)) {
			Double tr = this.arrivaltimesCB2FB.get(personId);
			if (tr == null) {
				this.arrivaltimesCB2FB.put(personId, time);
			}
			else {
				this.arrivaltimesFB2CB.put(personId, time);
			}
		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (!travelTimesPerPerson.containsKey(event.getPersonId()))
			travelTimesPerPerson.put(event.getPersonId(), 0.0);
		double previousTT = travelTimesPerPerson.get(event.getPersonId());
		travelTimesPerPerson.put(event.getPersonId(), previousTT - event.getTime());
	}

	public Tuple<Double,Double> getTotalAndAverageTravelTime() {
		Double totalTT = 0.0;
		for (Double travelTime : travelTimesPerPerson.values()) {
			totalTT += travelTime;
		}
		Double att = totalTT / travelTimesPerPerson.size();
		return new Tuple<Double, Double>(totalTT, att);
	}

	public Map<Id<Person>, Double> getArrivalTimesCB2FB() {
		return this.arrivaltimesCB2FB;
	}

	public Map<Id<Person>, Double> getArrivalTimesFB2CB() {
		return this.arrivaltimesFB2CB;
	}

	public Map<Id<Person>, Double> getArrivalTimesSPN2FB() {
		return this.arrivaltimesSPN2FB;
	}

	public Map<Id<Person>, Double> getArrivalTimesFB2SPN() {
		return this.arrivaltimesFB2SPN;
	}
	
	public int getNumberOfStuckedPersons() {
		return this.numberOfStuckedPersons;
	}
}
