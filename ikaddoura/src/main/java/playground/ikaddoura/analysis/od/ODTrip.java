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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

/**
* @author ikaddoura
*/

public class ODTrip {
	
	private String origin;
	private Coord originCoord;
	private String destination;
	private Coord destinationCoord;
	private String mode;
	private Id<Person> personId;
	private double departureTime;
	
	public String getOrigin() {
		return origin;
	}
	public void setOrigin(String origin) {
		this.origin = origin;
	}
	public String getDestination() {
		return destination;
	}
	public void setDestination(String destination) {
		this.destination = destination;
	}
	public String getMode() {
		return mode;
	}
	public void setMode(String mode) {
		this.mode = mode;
	}
	public Id<Person> getPersonId() {
		return personId;
	}
	public void setPersonId(Id<Person> personId) {
		this.personId = personId;
	}
	public double getDepartureTime() {
		return departureTime;
	}
	public void setDepartureTime(double departureTime) {
		this.departureTime = departureTime;
	}
	public Coord getOriginCoord() {
		return originCoord;
	}
	public void setOriginCoord(Coord originCoord) {
		this.originCoord = originCoord;
	}
	public Coord getDestinationCoord() {
		return destinationCoord;
	}
	public void setDestinationCoord(Coord destinationCoord) {
		this.destinationCoord = destinationCoord;
	}
	@Override
	public String toString() {
		return "ODTrip [origin=" + origin + ", destination=" + destination + ", mode=" + mode + ", personId=" + personId
				+ ", departureTime=" + departureTime + "]";
	}
		
}

