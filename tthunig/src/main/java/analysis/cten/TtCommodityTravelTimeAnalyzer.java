/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package analysis.cten;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author tthunig
 */
@Singleton
public class TtCommodityTravelTimeAnalyzer implements PersonDepartureEventHandler, PersonArrivalEventHandler, PersonStuckEventHandler {

	private Map<String, Double> avgTravelTimePerCommodity = new HashMap<>();
	private Map<String, List<Double>> allTravelTimesPerCommodity = new HashMap<>();
	
	private Map<Id<Person>, Double> pers2lastDepatureTime = new HashMap<>();
	
	@Inject
	public TtCommodityTravelTimeAnalyzer(EventsManager em) {	
		em.addHandler(this);
	}
	
	@Override
	public void reset(int iteration) {
		avgTravelTimePerCommodity.clear();
		allTravelTimesPerCommodity.clear();
		pers2lastDepatureTime.clear();
	}
	
	@Override
	public void handleEvent(PersonStuckEvent event) {
		pers2lastDepatureTime.remove(event.getPersonId());
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (!pers2lastDepatureTime.containsKey(event.getPersonId())) {
			throw new RuntimeException("Person " + event.getPersonId() + " is arriving but has not departed (or has stucked inbetween).");
		}
		double tripDuration = event.getTime() - pers2lastDepatureTime.get(event.getPersonId());
		String comId = event.getPersonId().toString().substring(0, 5);
		if (!allTravelTimesPerCommodity.containsKey(comId)) {
			allTravelTimesPerCommodity.put(comId, new LinkedList<>());
		}
		allTravelTimesPerCommodity.get(comId).add(tripDuration);
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		pers2lastDepatureTime.put(event.getPersonId(), event.getTime());
		
	}

	public Map<String, Double> getAvgTravelTimePerCommodity() {
		if (avgTravelTimePerCommodity.isEmpty()) {
			computeAvgTT();
		}
		return avgTravelTimePerCommodity;
	}

	private void computeAvgTT() {
		for (String comId : allTravelTimesPerCommodity.keySet()) {
			double totalTTcom = 0;
			for (Double tt : allTravelTimesPerCommodity.get(comId)) {
				totalTTcom += tt;
			}
			avgTravelTimePerCommodity.put(comId, totalTTcom / allTravelTimesPerCommodity.get(comId).size());
		}
	}

	public Map<String, List<Double>> getAllTravelTimesPerCommodity() {
		return allTravelTimesPerCommodity;
	}

}
