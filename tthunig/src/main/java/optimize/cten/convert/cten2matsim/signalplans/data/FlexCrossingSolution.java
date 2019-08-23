/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package optimize.cten.convert.cten2matsim.signalplans.data;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;

import optimize.cten.data.DgCrossing;
import optimize.cten.data.DgGreen;

/**
 * @author tthunig
 */
public class FlexCrossingSolution implements CtenCrossingSolution {

	private Id<DgCrossing> id;
	private Map<Id<DgGreen>, FlexibleLight> lightsOfThisCrossing = new HashMap<>();
	
	public FlexCrossingSolution(Id<DgCrossing> id) {
		this.id = id;
	}
	
	@Override
	public Id<DgCrossing> getId() {
		return this.id;
	}
	
	public void addLight(FlexibleLight light) {
		lightsOfThisCrossing.put(light.getId(), light);
	}
	
	public Map<Id<DgGreen>, FlexibleLight> getLights() {
		return this.lightsOfThisCrossing;
	}

}
