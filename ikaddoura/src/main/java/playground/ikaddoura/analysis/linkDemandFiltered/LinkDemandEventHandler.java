/* *********************************************************************** *
 * project: org.matsim.*
 * LinksEventHandler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.ikaddoura.analysis.linkDemandFiltered;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.vehicles.Vehicle;

/**
 * @author Ihab
 *
 */
public class LinkDemandEventHandler implements  LinkLeaveEventHandler, VehicleEntersTrafficEventHandler {
	private static final Logger log = Logger.getLogger(LinkDemandEventHandler.class);
	
	private final Network network;
	private final ModeFilter filter;
	private final Map<Id<Vehicle>, String> vehicleId2currentMode = new HashMap<>();
	
	private Map<Id<Link>,Integer> linkId2filteredAgents = new HashMap<Id<Link>, Integer>();
	private Map<Id<Link>,Integer> linkId2unfilteredAgents = new HashMap<Id<Link>, Integer>();

	public LinkDemandEventHandler(Network network, ModeFilter modeFilter) {
		this.network = network;
		this.filter = modeFilter;
	}

	@Override
	public void reset(int iteration) {
		linkId2filteredAgents.clear();
		linkId2unfilteredAgents.clear();
	}
	
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		
		if (this.linkId2unfilteredAgents.containsKey(event.getLinkId())) {
			int agents = this.linkId2unfilteredAgents.get(event.getLinkId());
			this.linkId2unfilteredAgents.put(event.getLinkId(), agents + 1);
			
		} else {
			this.linkId2unfilteredAgents.put(event.getLinkId(), 1);
		}
		
		if (filter.include(this.vehicleId2currentMode.get(event.getVehicleId()))) {
			if (this.linkId2filteredAgents.containsKey(event.getLinkId())) {
				int agents = this.linkId2filteredAgents.get(event.getLinkId());
				this.linkId2filteredAgents.put(event.getLinkId(), agents + 1);
				
			} else {
				this.linkId2filteredAgents.put(event.getLinkId(), 1);
			}
		}
	}

	public void printResults(String fileName) {
		File file = new File(fileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("link;filtered-agents (sample size);unfiltered-agents (sample size)");
			bw.newLine();
			
			for (Id<Link> linkId : this.network.getLinks().keySet()){
				
				int filteredAgents = 0;
				int unfilteredAgents = 0;
				
				if (this.linkId2filteredAgents.get(linkId) != null) {
					filteredAgents = this.linkId2filteredAgents.get(linkId);
				}
				
				if (this.linkId2unfilteredAgents.get(linkId) != null) {
					unfilteredAgents = this.linkId2unfilteredAgents.get(linkId);
				}
						
				bw.write(linkId + ";" + filteredAgents + ";" + unfilteredAgents);
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Map<Id<Link>, Integer> getLinkId2demand() {
		return linkId2filteredAgents;
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		this.vehicleId2currentMode.put(event.getVehicleId(), event.getNetworkMode());
	}

}
