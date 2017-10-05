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
package analysis;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;

/**
 * @author tthunig
 */
public class TtStaticLinkFlowValuesPerHour implements LinkEnterEventHandler, VehicleEntersTrafficEventHandler{

	private static final Logger LOG = Logger.getLogger(TtStaticLinkFlowValuesPerHour.class);
		
	private Map<Id<Link>, int[]> staticLinkFlowsPerHour = new HashMap<>();
	
	@Override
	public void reset(int iteration) {
		staticLinkFlowsPerHour.clear();
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		increment(event.getLinkId(), getBin(event.getTime()));
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		increment(event.getLinkId(), getBin(event.getTime()));
	}
	
	private void increment(Id<Link> linkId, int hour) {
		if (!staticLinkFlowsPerHour.containsKey(linkId))
			staticLinkFlowsPerHour.put(linkId, new int[24]);
		staticLinkFlowsPerHour.get(linkId)[hour%24]++;
	}

	/**
	 * converts time from seconds to hours, e.g. 1800 = 0, 3600 = 1 ...
	 * @param time in seconds
	 * @return hours
	 */
	private int getBin(double time) {
		return (int) time / 3600;
	}

	public Map<Id<Link>, int[]> getStaticLinkFlows() {
		return staticLinkFlowsPerHour;
	}
	
	public int[] getStaticLinkFlows(Id<Link> linkId) {
		if (!staticLinkFlowsPerHour.containsKey(linkId))
			return new int[24];
		return staticLinkFlowsPerHour.get(linkId);
	}

}
