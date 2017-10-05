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

package playground.ikaddoura.analysis.moneyGIS;


import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.noise.personLinkMoneyEvents.PersonLinkMoneyEvent;
import org.matsim.contrib.noise.personLinkMoneyEvents.PersonLinkMoneyEventHandler;

/**
 * @author ikaddoura
 *
 */
public class MoneyExtCostHandler implements  PersonLinkMoneyEventHandler {
	private static final Logger log = Logger.getLogger(MoneyExtCostHandler.class);

	private final Map<Id<Person>, Double> personId2toll = new HashMap<>();
	private final Map<Id<Person>, Double> personId2congestionToll = new HashMap<>();
	private final Map<Id<Person>, Double> personId2noiseToll = new HashMap<>();
	private final Map<Id<Person>, Double> personId2airPollutionToll = new HashMap<>();
	
	@Override
	public void reset(int iteration) {
		this.personId2toll.clear();
	}

	@Override
	public void handleEvent(PersonLinkMoneyEvent event) {
		
		if (personId2toll.get(event.getPersonId()) == null) {
			this.personId2toll.put(event.getPersonId(), -1. * event.getAmount());
		} else {
			double tollSoFar = this.personId2toll.get(event.getPersonId());
			this.personId2toll.put( event.getPersonId(), tollSoFar + (-1. * event.getAmount()) );
		}
		
		if (event.getDescription().equalsIgnoreCase("congestion")) {
			if (personId2congestionToll.get(event.getPersonId()) == null) {
				this.personId2congestionToll.put(event.getPersonId(), -1. * event.getAmount());
			} else {
				double tollSoFar = this.personId2congestionToll.get(event.getPersonId());
				this.personId2congestionToll.put( event.getPersonId(), tollSoFar + (-1. * event.getAmount()) );
			}
		} else if (event.getDescription().equalsIgnoreCase("noise")) {
			if (personId2noiseToll.get(event.getPersonId()) == null) {
				this.personId2noiseToll.put(event.getPersonId(), -1. * event.getAmount());
			} else {
				double tollSoFar = this.personId2noiseToll.get(event.getPersonId());
				this.personId2noiseToll.put( event.getPersonId(), tollSoFar + (-1. * event.getAmount()) );
			}
		} else if (event.getDescription().equalsIgnoreCase("airPollution")) {
			if (personId2airPollutionToll.get(event.getPersonId()) == null) {
				this.personId2airPollutionToll.put(event.getPersonId(), -1. * event.getAmount());
			} else {
				double tollSoFar = this.personId2airPollutionToll.get(event.getPersonId());
				this.personId2airPollutionToll.put( event.getPersonId(), tollSoFar + (-1. * event.getAmount()) );
			}
		} else {
			throw new RuntimeException("Unknown money event description. Aborting...");
		}
	}

	public Map<Id<Person>, Double> getPersonId2toll() {
		if (personId2toll.isEmpty()) log.warn("Map is empty!");
		return personId2toll;
	}

	public Map<Id<Person>, Double> getPersonId2congestionToll() {
		return personId2congestionToll;
	}

	public Map<Id<Person>, Double> getPersonId2noiseToll() {
		return personId2noiseToll;
	}

	public Map<Id<Person>, Double> getPersonId2airPollutionToll() {
		return personId2airPollutionToll;
	}
	
}
