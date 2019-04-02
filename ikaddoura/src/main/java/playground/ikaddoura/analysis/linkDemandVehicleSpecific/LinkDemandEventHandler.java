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
import org.matsim.core.utils.collections.Tuple;

/**
 * @author Ihab
 *
 */
public class LinkDemandEventHandler implements  LinkLeaveEventHandler {
	private static final Logger log = Logger.getLogger(LinkDemandEventHandler.class);
	private Network network;
	private String vehTypePrefix;
	private Tuple<Double, Double> timeBin;
	
	private Map<Id<Link>,Integer> linkId2demand = new HashMap<Id<Link>, Integer>();
	private Map<Id<Link>,Integer> linkId2otherVehicles = new HashMap<Id<Link>, Integer>();
	private Map<Id<Link>,Integer> linkId2specificVehicles = new HashMap<Id<Link>, Integer>();

	public LinkDemandEventHandler(Network network, String taxiPrefix) {
		this.network = network;
		this.vehTypePrefix = taxiPrefix;
		this.timeBin = null;
	}
	
	public LinkDemandEventHandler(Network network, String taxiPrefix, Tuple<Double, Double> timeBin) {
		this.network = network;
		this.vehTypePrefix = taxiPrefix;
		this.timeBin = timeBin;
	}

	@Override
	public void reset(int iteration) {
		this.linkId2demand.clear();
	}
	
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		
		boolean accountForEvent = false;
					
		if (timeBin == null) {
			accountForEvent = true;
		} else {
			if (event.getTime() >= timeBin.getFirst() && event.getTime() < timeBin.getSecond()) {
				accountForEvent = true;
			} else {
				accountForEvent = false;
			}
		}
		
		if (accountForEvent) {
			if (this.linkId2demand.containsKey(event.getLinkId())) {
				int agents = this.linkId2demand.get(event.getLinkId());
				this.linkId2demand.put(event.getLinkId(), agents + 1);
				
			} else {
				this.linkId2demand.put(event.getLinkId(), 1);
			}
			
			if (event.getVehicleId().toString().startsWith(vehTypePrefix)) {

				if (this.linkId2specificVehicles.containsKey(event.getLinkId())) {
					int agents = this.linkId2specificVehicles.get(event.getLinkId());
					this.linkId2specificVehicles.put(event.getLinkId(), agents + 1);
					
				} else {
					this.linkId2specificVehicles.put(event.getLinkId(), 1);
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
	}

	public void printResults(String fileName) {
		File file = new File(fileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("link;agents;" + vehTypePrefix + "_vehicles;otherVehicles");
			bw.newLine();
			
			for (Id<Link> linkId : this.network.getLinks().keySet()){
				
				int agents = 0;
				int taxiVehicles = 0;
				int otherVehicles = 0;
				
				if (this.linkId2demand.get(linkId) != null) {
					agents = this.linkId2demand.get(linkId);
				}
				
				if (this.linkId2specificVehicles.get(linkId) != null) {
					taxiVehicles = this.linkId2specificVehicles.get(linkId);
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
