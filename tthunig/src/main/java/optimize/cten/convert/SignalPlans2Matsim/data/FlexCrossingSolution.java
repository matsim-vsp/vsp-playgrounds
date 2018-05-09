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
package optimize.cten.convert.SignalPlans2Matsim.data;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;

/**
 * @author tthunig
 */
public class FlexCrossingSolution implements CtenCrossingSolution {

	private Id<CtenCrossingSolution> id;
	private Map<Id<FlexibleLight>, FlexibleLight> lightsOfThisCrossing = new HashMap<>();
	
	public FlexCrossingSolution(Id<CtenCrossingSolution> id) {
		this.id = id;
	}
	
	@Override
	public Id<CtenCrossingSolution> getId() {
		return this.id;
	}
	
	public void addLight(FlexibleLight light) {
		lightsOfThisCrossing.put(light.getId(), light);
	}

}
