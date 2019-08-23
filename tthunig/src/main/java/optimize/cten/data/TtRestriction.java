/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DefaultControlerModules.java
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
package optimize.cten.data;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;

/**
 * @author tthunig
 *
 */
public class TtRestriction {
	
	private Id<DgStreet> lightId;
	private boolean allowed;
	/** 
	 * if allowed==true: lights that are allowed to show green at the same time than lightId;
	 * if allowed==false: lights that have to show red when lightId shows green
	 * 
	 * the tuple contains the 'before' and 'after' intergreen time that is needed for the cten format
	 */
	private Map<Id<DgStreet>, Tuple<Integer, Integer>> rlightsAllowed = new HashMap<>();
	/** lights that have to be switched on at the same time step than lightId */
	private List<Id<DgStreet>> rlightsOn = new LinkedList<>();
	/** lights that have to be switched off at the same time step than lightId */
	private List<Id<DgStreet>> rlightsOff = new LinkedList<>();
	
	public TtRestriction(Id<DgStreet> lightId, boolean allowed) {
		this.lightId = lightId;
		this.allowed = allowed;
	}
	
	public void addAllowedLight(Id<DgStreet> lightId, int beforeIntergreen, int afterIntergreen){
		if (!rlightsAllowed.containsKey(lightId)){
			rlightsAllowed.put(lightId, new Tuple<Integer, Integer>(beforeIntergreen, afterIntergreen));
		}
	}
	
	public void addOnLight(Id<DgStreet> lightId){
		if (!rlightsOn.contains(lightId)){
			rlightsOn.add(lightId);
		}
	}
	
	public void addOffLight(Id<DgStreet> lightId) {
		if (!rlightsOff.contains(lightId)) {
			rlightsOff.add(lightId);
		}
	}

	public Id<DgStreet> getLightId() {
		return lightId;
	}

	public boolean isAllowed() {
		return allowed;
	}

	public Map<Id<DgStreet>, Tuple<Integer, Integer>> getRlightsAllowed() {
		return rlightsAllowed;
	}

	public List<Id<DgStreet>> getRlightsOn() {
		return rlightsOn;
	}

	public List<Id<DgStreet>> getRlightsOff() {
		return rlightsOff;
	}
	
}
