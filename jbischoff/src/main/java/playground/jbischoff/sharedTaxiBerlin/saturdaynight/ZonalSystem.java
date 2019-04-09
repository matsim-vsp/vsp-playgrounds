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

import java.util.Map;

import org.locationtech.jts.geom.Geometry;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class ZonalSystem {

	public enum OptimizationCriterion {Fare, Occupancy, Performance}
	
	private final Map<String,Geometry> zones;
	private final OptimizationCriterion optimizationCriterion;
	/**
	 * 
	 */
	public ZonalSystem(Map<String,Geometry> zones, OptimizationCriterion c) {
		this.zones = zones;
		this.optimizationCriterion = c;
		
	}
	
	/**
	 * @return the zones
	 */
	public Map<String, Geometry> getZones() {
		return zones;
	}

	/**
	 * @return the optimizationCriterion
	 */
	public OptimizationCriterion getOptimizationCriterion() {
		return optimizationCriterion;
	}
	
}
