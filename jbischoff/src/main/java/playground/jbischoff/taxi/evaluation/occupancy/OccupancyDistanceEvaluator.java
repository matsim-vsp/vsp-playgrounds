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
package playground.jbischoff.taxi.evaluation.occupancy;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.contrib.taxi.optimizer.assignment.TaxiToRequestAssignmentCostProvider;
import org.matsim.contrib.taxi.vrpagent.TaxiActionCreator;
import org.matsim.vehicles.Vehicle;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class OccupancyDistanceEvaluator
		implements LinkEnterEventHandler, ActivityEndEventHandler {
	
	Map<Id<Vehicle>,Boolean> vehicleBusy = new HashMap<>();
	
	private Network network;
	private double emptyKm = 0;
	private double occupiedKm = 0;
	
	

	public OccupancyDistanceEvaluator(Network network) {
		this.network = network;
	}



	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.events.handler.ActivityEndEventHandler#handleEvent(org.matsim.api.core.v01.events.ActivityEndEvent)
	 */
	@Override
	public void handleEvent(ActivityEndEvent event) {
		String type = event.getActType();
		Id<Vehicle> vid = Id.createVehicleId(event.getPersonId()); 
		
		if (type.equals(VrpAgentLogic.BEFORE_SCHEDULE_ACTIVITY_TYPE)){
			vehicleBusy.put(vid, false);
			
		} if (type.equals(TaxiActionCreator.PICKUP_ACTIVITY_TYPE)){
			vehicleBusy.put(vid, true);

		}
		if (type.equals(TaxiActionCreator.DROPOFF_ACTIVITY_TYPE)){
			vehicleBusy.put(vid, false);

		}
		
	}

	

	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.events.handler.LinkEnterEventHandler#handleEvent(org.matsim.api.core.v01.events.LinkEnterEvent)
	 */
	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (vehicleBusy.containsKey(event.getVehicleId())){
			double linkLength = network.getLinks().get(event.getLinkId()).getLength()/1000.;
			if (vehicleBusy.get(event.getVehicleId())){
				occupiedKm = occupiedKm+linkLength;
			} else {
				emptyKm = emptyKm+linkLength;
			}
		}
	}
	
	/**
	 * @return the emptyKm
	 */
	public double getEmptyKm() {
		return emptyKm;
	}
	
	/**
	 * @return the occupiedKm
	 */
	public double getOccupiedKm() {
		return occupiedKm;
	}

}
