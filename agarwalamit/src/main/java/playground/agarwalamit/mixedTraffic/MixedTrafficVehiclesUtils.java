/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.mixedTraffic;

import org.matsim.api.core.v01.TransportMode;

/**
 * @author amit
 */
public class MixedTrafficVehiclesUtils {

	/**
	 * @param travelMode
	 * for which PCU value is required
	 */
	public static double getPCU(final String travelMode){
		double pcu;
		switch (travelMode) {
		case TransportMode.car: pcu = 1.0; break;
		case "bicycle":
		case TransportMode.bike: pcu = 0.25; break;
		case "motorbike": pcu = 0.25;break;
		case TransportMode.walk: pcu = 0.10;break;
//		case PT :
		case "truck": pcu = 3.0; break;
		default: throw new RuntimeException("No PCU is set for travel mode "+travelMode+ ".");
		}
		return pcu;
	}
	
	/**
	 * @param travelMode
	 * for which speed is required
	 */
	public static double getSpeed(final String travelMode){
		double speed;
		switch (travelMode) {
		case TransportMode.car: speed = 16.67; break;
		case "bicycle":
		case TransportMode.bike: speed = 4.17; break;
		case "motorbike": speed = 16.67;break;
		case TransportMode.walk: speed = 1.2;break;
//		case PT :
		case "truck": speed = 8.33; break;
		default: throw new RuntimeException("No speed is set for travel mode "+travelMode+ ".");
		}
		return speed;
	}
	
	/**
	 * @param travelMode
	 * for which effective cell size is required
	 * @return 
	 * physical road space occupied based on PCU unit 
	 * default is cell size for car (7.5 m)
	 */
	public static double getCellSize(final String travelMode){
		double matsimCellSize = 7.5;
		return matsimCellSize*getPCU(travelMode);
	}

	public static double getLength(final String travelMode){
		switch (travelMode){
			case TransportMode.car: return 3.72;
			case "bicycle":
			case TransportMode.bike:
			case "motorbike": return 1.9;
			case "truck": return 7.5;
		}
		throw new RuntimeException("Length fot "+travelMode+" is not found.");
	}
}
