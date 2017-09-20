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
package playground.ikaddoura.analysis.linkDemandVehicleSpecific;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

/**
 * @author Ihab
 *
 */
public class LinkDemandEventHandler implements  LinkLeaveEventHandler {
	private static final Logger log = Logger.getLogger(LinkDemandEventHandler.class);
	private Network network;
	private String taxiPrefix;
	
	private Map<Id<Link>,Integer> linkId2demand = new HashMap<Id<Link>, Integer>();
	private Map<Id<Link>,Integer> linkId2otherVehicles = new HashMap<Id<Link>, Integer>();
	private Map<Id<Link>,Integer> linkId2taxiVehicles = new HashMap<Id<Link>, Integer>();

	public LinkDemandEventHandler(Network network, String taxiPrefix) {
		this.network = network;
		this.taxiPrefix = taxiPrefix;
	}

	@Override
	public void reset(int iteration) {
		this.linkId2demand.clear();
	}
	
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		
		if (this.linkId2demand.containsKey(event.getLinkId())) {
			int agents = this.linkId2demand.get(event.getLinkId());
			this.linkId2demand.put(event.getLinkId(), agents + 1);
			
		} else {
			this.linkId2demand.put(event.getLinkId(), 1);
		}
		
		if (event.getVehicleId().toString().startsWith(taxiPrefix)) {
			
			if (this.linkId2taxiVehicles.containsKey(event.getLinkId())) {
				int agents = this.linkId2taxiVehicles.get(event.getLinkId());
				this.linkId2taxiVehicles.put(event.getLinkId(), agents + 1);
				
			} else {
				this.linkId2taxiVehicles.put(event.getLinkId(), 1);
			}
			
		} else {
			
			if (this.linkId2otherVehicles.containsKey(event.getLinkId())) {
				int agents = this.linkId2otherVehicles.get(event.getLinkId());
				this.linkId2otherVehicles.put(event.getLinkId(), agents + 1);
				
			} else {
				this.linkId2otherVehicles.put(event.getLinkId(), 1);
			}
			
		}
		
	}

	public void printResults(String fileName) {
		File file = new File(fileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("link;agents;taxiVehicles;otherVehicles");
			bw.newLine();
			
			for (Id<Link> linkId : this.network.getLinks().keySet()){
				
				int agents = 0;
				int taxiVehicles = 0;
				int otherVehicles = 0;
				
				if (this.linkId2demand.get(linkId) != null) {
					agents = this.linkId2demand.get(linkId);
				}
				
				if (this.linkId2taxiVehicles.get(linkId) != null) {
					taxiVehicles = this.linkId2taxiVehicles.get(linkId);
				}
				
				if (this.linkId2otherVehicles.get(linkId) != null) {
					otherVehicles = this.linkId2otherVehicles.get(linkId);
				}
				
				bw.write(linkId + ";" + agents + ";" + taxiVehicles + ";" + otherVehicles);
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Map<Id<Link>, Integer> getLinkId2demand() {
		return linkId2demand;
	}

}
