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
package playground.jbischoff.csberlin.evaluation;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class WalkLegEvaluator {
	public static void main(String[] args) {
		

		String dir = "D:/runs-svn/bmw_carsharing/avparking/basecase/";
		
		EventsManager events = EventsUtils.createEventsManager();
		WalkLegHandler w = new WalkLegHandler();
		events.addHandler(w);
		new ParkingSearchEventsReader(events).readFile(dir+"output_events.xml.gz");
		System.out.print("trips\t" + w.trips+"\twalkTime\t"+w.walkTime+"\tavWalk\t"+w.walkTime/w.trips);
	}
	
}
class WalkLegHandler implements PersonDepartureEventHandler, PersonArrivalEventHandler{
	
	private Map<Id<Person>,Double> departureTime = new HashMap<>();
	int trips = 0;
	double walkTime = 0;
	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler#handleEvent(org.matsim.api.core.v01.events.PersonArrivalEvent)
	 */
	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (departureTime.containsKey(event.getPersonId())){
			double tt = event.getTime() - departureTime.remove(event.getPersonId());
			if (tt>1){
				trips++;
				walkTime=walkTime+tt;
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler#handleEvent(org.matsim.api.core.v01.events.PersonDepartureEvent)
	 */
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().equals(TransportMode.access_walk)||event.getLegMode().equals(TransportMode.egress_walk)){
			departureTime.put(event.getPersonId(), event.getTime());
		}
	}}
