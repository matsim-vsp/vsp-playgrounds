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

package playground.ikaddoura.analysis.od;

import java.util.List;

/**
* @author ikaddoura
*/

public class TripFilter {
	
	private final double timeStart;
	private final double timeEnd;
	private final String personIdPrefix;
	private final List<String> modes;
	
	public TripFilter(double consideredTimeStart, double consideredTimeEnd, String consideredPersonIdPrefix, List<String> consideredModes) {
		this.timeStart = consideredTimeStart;
		this.timeEnd = consideredTimeEnd;
		this.personIdPrefix = consideredPersonIdPrefix;
		this.modes = consideredModes;
	}
	
	public boolean considerTrip(ODTrip odTrip) {
		
		// check modes
		if (!modes.isEmpty()) {
			boolean consideredMode = false;
			for (String mode : modes) {
				if (mode.equals(odTrip.getMode())) {
					consideredMode = true;
				}
			}
			if (!consideredMode) return false;
		}
		
		// check time
		if (odTrip.getDepartureTime() < timeStart || odTrip.getDepartureTime() >= timeEnd) {
			return false;
		}
		
		// check personIdPrefix
		if (personIdPrefix != "") {
			if (!odTrip.getPersonId().toString().startsWith(personIdPrefix)) {
				return false;
			}
		}
		
		return true;
	}

	@Override
	public String toString() {
		return "_timeStart-" + timeStart + "_timeEnd-" + timeEnd + "_personIdPrefix-" + personIdPrefix
				+ "_modePrefix-" + modes.toString();
	}

}

