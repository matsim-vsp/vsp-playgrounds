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

/**
* @author ikaddoura
*/

public class TripFilter {
	
	private final double timeStart;
	private final double timeEnd;
	private final String personIdPrefix;
	private final String modePrefix;
	
	public TripFilter(double timeStart, double timeEnd, String personIdPrefix, String modePrefix) {
		this.timeStart = timeStart;
		this.timeEnd = timeEnd;
		this.personIdPrefix = personIdPrefix;
		this.modePrefix = modePrefix;
	}
	
	public boolean considerTrip(ODTrip odTrip) {
		
		if (odTrip.getDepartureTime() >= timeStart
				&& odTrip.getDepartureTime() <= timeEnd
				&& odTrip.getPersonId().toString().startsWith(personIdPrefix)
				&& odTrip.getMode().startsWith(modePrefix)) {
			return true;
		} else {
			return false;
		}		
	}

	@Override
	public String toString() {
		return "_timeStart-" + timeStart + "_timeEnd-" + timeEnd + "_personIdPrefix-" + personIdPrefix
				+ "_modePrefix-" + modePrefix;
	}

}

