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
package playground.ikaddoura.analysis.travelTimes;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;

/**
 * @author Ihab
 *
 */
public class DynamicLinkVolumesTravelTimesEventHandler implements  LinkEnterEventHandler, LinkLeaveEventHandler {
	private static final Logger log = Logger.getLogger(DynamicLinkVolumesTravelTimesEventHandler.class);
	
	private double timeBinSize = 3600.;
	private Network network;
	
	private SortedMap<Double, Map<Id<Link>, Double>> timeBinEndTime2linkId2travelTimeSum = new TreeMap<Double, Map<Id<Link>, Double>>();
	private SortedMap<Double, Map<Id<Link>, Integer>> timeBinEndTime2linkId2Demand = new TreeMap<Double, Map<Id<Link>, Integer>>();
	private Map<Id<Vehicle>, Double> vehicleId2enterTime = new HashMap<>();
	
	public DynamicLinkVolumesTravelTimesEventHandler(Network network) {
		this.network = network;
	}

	@Override
	public void reset(int iteration) {
		this.timeBinEndTime2linkId2Demand.clear();
		this.timeBinEndTime2linkId2travelTimeSum.clear();
		this.vehicleId2enterTime.clear();
	}
	
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		
		if (vehicleId2enterTime.get(event.getVehicleId()) == null) {
			// The trip has started on that link.
			
		} else {
			double currentTimeBin = (Math.floor(event.getTime() / this.timeBinSize) + 1) * this.timeBinSize;
			
			double tt = event.getTime() - vehicleId2enterTime.get(event.getVehicleId());

			if (this.timeBinEndTime2linkId2Demand.containsKey(currentTimeBin)) {
				if (this.timeBinEndTime2linkId2Demand.get(currentTimeBin).containsKey(event.getLinkId())) {
					int agents = this.timeBinEndTime2linkId2Demand.get(currentTimeBin).get(event.getLinkId());
					this.timeBinEndTime2linkId2Demand.get(currentTimeBin).put(event.getLinkId(), agents + 1);
					
					double travelTimeSum = this.timeBinEndTime2linkId2travelTimeSum.get(currentTimeBin).get(event.getLinkId());
					this.timeBinEndTime2linkId2travelTimeSum.get(currentTimeBin).put(event.getLinkId(), travelTimeSum + tt);
					
				} else {
					this.timeBinEndTime2linkId2Demand.get(currentTimeBin).put(event.getLinkId(), 1);
					this.timeBinEndTime2linkId2travelTimeSum.get(currentTimeBin).put(event.getLinkId(), tt);
				}
				
			} else {
				Map<Id<Link>, Integer> linkId2demand = new HashMap<Id<Link>, Integer>();
				linkId2demand.put(event.getLinkId(), 1);
				this.timeBinEndTime2linkId2Demand.put(currentTimeBin, linkId2demand);
				
				Map<Id<Link>, Double> linkId2ttSum = new HashMap<Id<Link>, Double>();
				linkId2ttSum.put(event.getLinkId(), tt);
				this.timeBinEndTime2linkId2travelTimeSum.put(currentTimeBin, linkId2ttSum);
			}
			
			vehicleId2enterTime.remove(event.getVehicleId());
		}
	}

	public void printResults(String path) {
		
		String fileName = path + "link-analysis_trafficVolume_averageTravelTime.csv";
		File file1 = new File(fileName);
		File file2 = new File(fileName + "t");
		
		try {
			BufferedWriter bw1 = new BufferedWriter(new FileWriter(file1));
			BufferedWriter bw2 = new BufferedWriter(new FileWriter(file2));

			bw1.write("link ID; free speed travel time [sec]");
			bw2.write("\"String\"");
			
			for (Double timeBinEndTime : this.timeBinEndTime2linkId2Demand.keySet()) {
				bw1.write("; volume [sample size] " + Time.writeTime(timeBinEndTime, Time.TIMEFORMAT_HHMMSS) + "; avg travel Time [sec] " + Time.writeTime(timeBinEndTime, Time.TIMEFORMAT_HHMMSS));
				bw2.write(",\"Real\",\"Real\"");
			}
			bw1.newLine();
			
			for (Id<Link> linkId : this.network.getLinks().keySet()){
				
				bw1.write(linkId.toString() + ";");
				double freeTT = this.network.getLinks().get(linkId).getLength() / this.network.getLinks().get(linkId).getFreespeed();
				bw1.write(String.valueOf(freeTT));
				
				for (Double timeBinEndTime : this.timeBinEndTime2linkId2Demand.keySet()) {
					int agents = 0;
					double avgTravelTime = 0.;
					if (this.timeBinEndTime2linkId2Demand.get(timeBinEndTime).containsKey(linkId)) {
						agents = this.timeBinEndTime2linkId2Demand.get(timeBinEndTime).get(linkId);
						avgTravelTime = this.timeBinEndTime2linkId2travelTimeSum.get(timeBinEndTime).get(linkId) / (double) this.timeBinEndTime2linkId2Demand.get(timeBinEndTime).get(linkId);
					}
					bw1.write(";" + agents + ";" + avgTravelTime);
				}
				bw1.newLine();
			}
			
			bw1.close();
			bw2.close();
			log.info("Output written to " + fileName);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		vehicleId2enterTime.put(event.getVehicleId(), event.getTime());
	}

}
