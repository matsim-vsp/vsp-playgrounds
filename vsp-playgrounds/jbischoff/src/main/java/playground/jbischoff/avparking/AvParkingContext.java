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
package playground.jbischoff.avparking;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import playground.jbischoff.avparking.optimizer.PrivateAVTaxiDispatcher.AVParkBehavior;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class AvParkingContext {

	private List<Id<Link>> avParkings;
	private AVParkBehavior behavior;

	/**
	 * 
	 */
	public AvParkingContext(List<Id<Link>> avParkings, AVParkBehavior behavior ) {
		this.avParkings = avParkings;
		this.behavior = behavior;
		
	}
	
	/**
	 * @return the avParkings
	 */
	public List<Id<Link>> getAvParkings() {
		return avParkings;
	}
	/**
	 * @return the behavior
	 */
	public AVParkBehavior getBehavior() {
		return behavior;
	}
}
