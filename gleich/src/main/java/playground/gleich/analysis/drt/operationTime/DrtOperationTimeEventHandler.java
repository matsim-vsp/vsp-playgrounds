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

package playground.gleich.analysis.drt.operationTime;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.misc.Counter;

/**
 * @author gleich
 *
 */
public class DrtOperationTimeEventHandler implements ActivityStartEventHandler, ActivityEndEventHandler {
	
	private final static Logger log = Logger.getLogger(DrtOperationTimeEventHandler.class);
	private Map<Id<Person>, Double> drtVehDriver2OperationTime = new HashMap<>();
	private Map<Id<Person>, Double> drtVehDriver2LastStayTaskEndTime = new HashMap<>();
	private Counter counter = new Counter("[" + this.getClass().getSimpleName() + "] handled ExperiencedTrip # ", "", 4);
	
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
