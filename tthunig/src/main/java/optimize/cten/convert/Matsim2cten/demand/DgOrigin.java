/* *********************************************************************** *
 * project: org.matsim.*
 * DgOrigin
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package optimize.cten.convert.Matsim2cten.demand;

import java.util.Map;

import org.locationtech.jts.geom.Coordinate;
import org.matsim.api.core.v01.network.Link;


/**
 * 
 * @author dgrether
 *
 */
public interface DgOrigin {

	public void incrementDestinationZoneTrips(DgZone destinationZone);

	public void incrementDestinationLinkTrips(Link endLink);
	
	public Coordinate getCoordinate();
	
	public Map<DgZone, Double> getDestinationZoneTrips();
	
	public Map<Link, Double> getDestinationLinkTrips();
	
}
