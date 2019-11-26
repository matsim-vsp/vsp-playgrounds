/* *********************************************************************** *
 * project: org.matsim.*
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
  
package playground.kturner.gettingStartedAgain;

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.Vehicle;

class StringModification {

	public static void main(String[] args) {
		String prevString = "heavy26t_frozen_TW_16076";
		String newLinkId = "1234567";
		System.out.println("Prev: " + prevString);
		Id<Vehicle> newVehicleId = Id.createVehicleId(prevString.substring(0,prevString.lastIndexOf("_")+1)+newLinkId);
		System.out.println("New: " + newVehicleId.toString());
	}

}
