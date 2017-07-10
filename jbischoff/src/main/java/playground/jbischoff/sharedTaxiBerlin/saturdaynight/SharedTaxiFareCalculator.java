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
package playground.jbischoff.sharedTaxiBerlin.saturdaynight;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.mutable.MutableDouble;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.passenger.events.DrtRequestRejectedEvent;
import org.matsim.contrib.drt.passenger.events.DrtRequestRejectedEventHandler;
import org.matsim.contrib.drt.passenger.events.DrtRequestScheduledEvent;
import org.matsim.contrib.drt.passenger.events.DrtRequestScheduledEventHandler;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEventHandler;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.core.api.experimental.events.EventsManager;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class SharedTaxiFareCalculator
		implements DrtRequestRejectedEventHandler, DrtRequestScheduledEventHandler, DrtRequestSubmittedEventHandler {
	
	
	Map<Id<Link>,MutableDouble> faresPerLink = new HashMap<>();
	Map<Id<Request>, DrtRequestSubmittedEvent> requests = new HashMap<>();
	double fareFactor = 0.5;
	

	@Inject
	/**
	 * 
	 */
	public SharedTaxiFareCalculator(EventsManager events) {
		events.addHandler(this);
	}
	
	@Override
	public void reset(int iteration) {
		faresPerLink.clear();
	}
	
	/* (non-Javadoc)
	 * @see org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEventHandler#handleEvent(org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent)
	 */
	@Override
	public void handleEvent(DrtRequestSubmittedEvent event) {
		requests.put(event.getRequestId(), event);
	}

	/* (non-Javadoc)
	 * @see org.matsim.contrib.drt.passenger.events.DrtRequestScheduledEventHandler#handleEvent(org.matsim.contrib.drt.passenger.events.DrtRequestScheduledEvent)
	 */
	@Override
	public void handleEvent(DrtRequestScheduledEvent event) {
		DrtRequestSubmittedEvent submission = requests.remove(event.getRequestId());
		double fare = calcFare(submission.getUnsharedRideDistance());
		if (faresPerLink.containsKey(submission.getFromLinkId())){
			faresPerLink.get(submission.getFromLinkId()).add(fare*0.5);
		} else {
			faresPerLink.put(submission.getFromLinkId(), new MutableDouble(fare*0.5));
		}
		
		if (faresPerLink.containsKey(submission.getToLinkId())){
			faresPerLink.get(submission.getToLinkId()).add(fare*0.5);
		} else {
			faresPerLink.put(submission.getToLinkId(), new MutableDouble(fare*0.5));
		}
		
	}

	/**
	 * @param unsharedRideDistance
	 * @return
	 */
	private double calcFare(double unsharedRideDistance) {
		double lowFareDistance = unsharedRideDistance>7000?unsharedRideDistance-7000:0;
		double highFareDistance = unsharedRideDistance>7000?7000:unsharedRideDistance;
		double taxifare = 3.9 + 2*highFareDistance/1000. +1.5* lowFareDistance/1000.;
		return taxifare*fareFactor;
	}

	/* (non-Javadoc)
	 * @see org.matsim.contrib.drt.passenger.events.DrtRequestRejectedEventHandler#handleEvent(org.matsim.contrib.drt.passenger.events.DrtRequestRejectedEvent)
	 */
	@Override
	public void handleEvent(DrtRequestRejectedEvent event) {
		requests.remove(event.getRequestId());
	}
	
	/**
	 * @return the faresPerLink
	 */
	public Map<Id<Link>, MutableDouble> getFaresPerLink() {
		return faresPerLink;
	}

	

}
