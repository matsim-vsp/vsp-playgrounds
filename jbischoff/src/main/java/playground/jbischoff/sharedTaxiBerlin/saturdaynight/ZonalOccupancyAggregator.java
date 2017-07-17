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

package playground.jbischoff.sharedTaxiBerlin.saturdaynight;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang.mutable.MutableInt;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.vehicles.Vehicle;

import com.google.inject.name.Named;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class ZonalOccupancyAggregator implements PersonEntersVehicleEventHandler,
		PersonLeavesVehicleEventHandler, ActivityEndEventHandler,  LinkEnterEventHandler {

	private Map<Id<Vehicle>, MutableInt> currentOccupancy = new HashMap<>();
	private Set<Id<Person>> taxiDrivers = new HashSet<>();
	private Map<Id<Link>, DescriptiveStatistics> linkOccupancy = new HashMap<>();
	private Map<String,Geometry> zones;
	private Map<Id<Link>,String> link2zone = new HashMap<>();
	private Map<String,Tuple<MutableDouble,MutableDouble>> zoneOcc = new HashMap<>(); 
	private Network network;
	private double overallMileage = 0.0;
	private double revenueMileage = 0.0;
	
	@Inject
	public ZonalOccupancyAggregator(Network network, EventsManager events, ZonalSystem drtzones) {
		this.zones = drtzones.getZones();
		this.network = network;
		events.addHandler(this);
	}

	/**
	 * 
	 */
	private void prepareZones() {
		for (String zoneId : zones.keySet()){
			zoneOcc.put(zoneId,new Tuple<MutableDouble,MutableDouble>(new MutableDouble(), new MutableDouble()));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.matsim.core.events.handler.<#reset(int)
	 */
	@Override
	public void reset(int iteration) {
		taxiDrivers.clear();
		currentOccupancy.clear();
		prepareZones();
		overallMileage = 0.0;
		revenueMileage = 0.0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.matsim.api.core.v01.events.handler.ActivityEndEventHandler#
	 * handleEvent(org.matsim.api.core.v01.events. ActivityEndEvent)
	 */
	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (event.getActType().equals(VrpAgentLogic.BEFORE_SCHEDULE_ACTIVITY_TYPE)) {
			Id<Vehicle> vid = Id.createVehicleId(event.getPersonId().toString());
			currentOccupancy.put(vid, new MutableInt(0));
			taxiDrivers.add(event.getPersonId());
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler#
	 * handleEvent(org.matsim.api.core.v01.events .PersonLeavesVehicleEvent)
	 */
	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if (!this.taxiDrivers.contains(event.getPersonId())){
		if (this.currentOccupancy.containsKey(event.getVehicleId())) {
			currentOccupancy.get(event.getVehicleId()).decrement();
		}}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler#
	 * handleEvent(org.matsim.api.core.v01.events .PersonEntersVehicleEvent)
	 */
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (this.currentOccupancy.containsKey(event.getVehicleId())) {
			if (!this.taxiDrivers.contains(event.getPersonId())) {
				currentOccupancy.get(event.getVehicleId()).increment();
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.matsim.api.core.v01.events.handler.LinkEnterEventHandler#handleEvent(
	 * org.matsim.api.core.v01.events.LinkEnterEvent)
	 */
	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (this.currentOccupancy.containsKey(event.getVehicleId())) {
			int co = currentOccupancy.get(event.getVehicleId()).intValue();
			String zone = getZoneForLinkId(event.getLinkId());
			double linkLength = network.getLinks().get(event.getLinkId()).getLength();
			if (zone!=null){
				zoneOcc.get(zone).getFirst().add(co*linkLength);
				zoneOcc.get(zone).getSecond().add(linkLength);
			}
			
			DescriptiveStatistics linkStats;
			if (linkOccupancy.containsKey(event.getLinkId())) {
				linkStats = linkOccupancy.get(event.getLinkId());
			} else {
				linkStats = new DescriptiveStatistics();
			}
			linkStats.addValue(co);
			linkOccupancy.put(event.getLinkId(), linkStats);
			overallMileage += linkLength;
			revenueMileage += linkLength*co;
			
		}
	}

	/**
	 * @return the linkOccupancy
	 */
	public Map<Id<Link>, DescriptiveStatistics> getLinkOccupancy() {
		return linkOccupancy;
	}
	
	public String getZoneForLinkId(Id<Link> linkId){
		if (this.link2zone.containsKey(linkId)){
			return link2zone.get(linkId);
		}
		
		Point linkCoord = MGC.coord2Point(network.getLinks().get(linkId).getCoord());
		
		for (Entry<String, Geometry> e : zones.entrySet()){
			if (e.getValue().contains(linkCoord)){
				link2zone.put(linkId, e.getKey());
				return e.getKey();
			}
		}
		link2zone.put(linkId, null);
		return null;
		
	}
	public Map<String,Double> calculateZoneOccupancy(){
		Map<String,Double> zoneOc = new HashMap<>();
		for (Entry<String,Tuple<MutableDouble,MutableDouble>> e : zoneOcc.entrySet() ){
			double occ = e.getValue().getFirst().doubleValue()/e.getValue().getSecond().doubleValue();
			zoneOc.put(e.getKey(),occ );
		}
	return zoneOc;
	}
	
	/**
	 * @return the overallMileage
	 */
	public double getOverallMileage() {
		return overallMileage;
	}
	
	/**
	 * @return the revenueMileage
	 */
	public double getRevenueMileage() {
		return revenueMileage;
	}
	
	public int getFleetSize(){
		return currentOccupancy.size();
	}

}
