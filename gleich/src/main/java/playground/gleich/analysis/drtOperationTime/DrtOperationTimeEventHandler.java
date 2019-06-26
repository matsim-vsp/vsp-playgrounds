/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
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

package playground.gleich.analysis.drtOperationTime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEventHandler;
import org.matsim.core.api.experimental.events.AgentWaitingForPtEvent;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.api.experimental.events.handler.AgentWaitingForPtEventHandler;
import org.matsim.core.api.experimental.events.handler.TeleportationArrivalEventHandler;
import org.matsim.core.utils.misc.Counter;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

/**
 * @author gleich
 *
 */
public class DrtOperationTimeEventHandler implements ActivityStartEventHandler, ActivityEndEventHandler {
	
	private final static Logger log = Logger.getLogger(DrtOperationTimeEventHandler.class);
	private Map<Id<Person>, Double> drtVehDriver2OperationTime = new HashMap<>();
	private Map<Id<Person>, Double> drtVehDriver2LastStayTaskEndTime = new HashMap<>();
	private Counter counter = new Counter("[" + this.getClass().getSimpleName() + "] handled ExperiencedTrip # ", "", 4);
	
	/**
	 * 
	 * @param network
	 * @param monitoredModes : All trips to be monitored have to consist only of legs of these modes
	 * @param monitoredStartAndEndLinks : only trips which start or end on one these links will be monitored. 
	 * Set to null if you want to have all trips from all origins and to all destinations
	 */
	public DrtOperationTimeEventHandler() {
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if( event.getActType().equals("DrtStay") ){
			drtVehDriver2LastStayTaskEndTime.put(event.getPersonId(), event.getTime());
		}
	}
	
	@Override
	public void handleEvent(ActivityStartEvent event) {
		if( event.getActType().equals("DrtStay") ) {
			if (drtVehDriver2OperationTime.containsKey(event.getPersonId())) {
				// not the first activity start
				double newTotal = drtVehDriver2OperationTime.get(event.getPersonId()) + 
						event.getTime() - drtVehDriver2LastStayTaskEndTime.get(event.getPersonId());
				drtVehDriver2OperationTime.put(event.getPersonId(), newTotal);
			} else {
				// first activity start after veh is put into service. Ignore this.
				drtVehDriver2OperationTime.put(event.getPersonId(), 0.0);
			}
		}
	}

	// Getter
	public Map<Id<Person>, Double> getDrtVehDriver2OperationTime() {
		return drtVehDriver2OperationTime;
	}

}
