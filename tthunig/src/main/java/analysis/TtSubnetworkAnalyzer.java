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
package analysis;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.vehicles.Vehicle;
import org.opengis.feature.simple.SimpleFeature;

import playground.dgrether.analysis.eventsfilter.FeatureNetworkLinkCenterCoordFilter;

/**
 * @author tthunig
 */
public class TtSubnetworkAnalyzer implements LinkEnterEventHandler, LinkLeaveEventHandler, PersonDepartureEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {

	private Network subNetwork;
	private Set<Id<Vehicle>> vehiclesInSubNetwork = new HashSet<>();
	private int numberOfTripsInSubnetwork = 0;
	private int numberOfTripsInTotal = 0;
	
	private double totalDistanceSubnetwork = 0.0;
	private double totalTtSubnetwork = 0.0;
	private double totalDelaySubnetwork = 0.0;

	private Map<Id<Vehicle>, Double> veh2earliestLinkExitTime = new HashMap<>();
	private Map<Id<Person>, Double> pers2lastDepatureTime = new HashMap<>();

	
	public TtSubnetworkAnalyzer(String filterFeatureFilename, Network fullNetwork) {	
		ShapeFileReader shapeReader = new ShapeFileReader();
		Collection<SimpleFeature> features = shapeReader.readFileAndInitialize(filterFeatureFilename);
		
		NetworkFilterManager netFilter = new NetworkFilterManager(fullNetwork);
		FeatureNetworkLinkCenterCoordFilter filter = new FeatureNetworkLinkCenterCoordFilter(
				MGC.getCRS(TransformationFactory.WGS84_UTM33N), features.iterator().next(), shapeReader.getCoordinateSystem());
		netFilter.addLinkFilter(filter);
		subNetwork = netFilter.applyFilters();
	}
	
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (subNetwork.getLinks().containsKey(event.getLinkId())) {
			pers2lastDepatureTime.put(event.getPersonId(), event.getTime());
		}
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		if (subNetwork.getLinks().containsKey(event.getLinkId())) {
			// consider time between departure and enterTraffic as delay
			double delay = event.getTime() - pers2lastDepatureTime.remove(event.getPersonId());
			totalTtSubnetwork += delay;
			totalDelaySubnetwork += delay;
			
			// consider time on link separately
			// for the first link every vehicle needs one second without delay
			veh2earliestLinkExitTime.put(event.getVehicleId(), event.getTime() + 1);
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (subNetwork.getLinks().containsKey(event.getLinkId())) {
			if (!vehiclesInSubNetwork.contains(event.getVehicleId())) {
				// new trip through the subnetwork
				vehiclesInSubNetwork.add(event.getVehicleId());
				numberOfTripsInSubnetwork++;
			}
			
			Link link = subNetwork.getLinks().get(event.getLinkId());
			totalDistanceSubnetwork += link.getLength();
			
			// calculate earliest link exit time
			double freespeedTt = link.getLength() / link.getFreespeed();
			// this is the earliest time where matsim sets the agent to the next link
			double matsimFreespeedTT = Math.floor(freespeedTt + 1);	
			veh2earliestLinkExitTime.put(event.getVehicleId(), event.getTime() + matsimFreespeedTT);
			totalTtSubnetwork += matsimFreespeedTT;
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (subNetwork.getLinks().containsKey(event.getLinkId())) {
			double currentDelay = event.getTime() - veh2earliestLinkExitTime.remove(event.getVehicleId());
			totalDelaySubnetwork += currentDelay;
			totalTtSubnetwork += currentDelay;
		}
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {		
		numberOfTripsInTotal++;
		// remove vehicle from map to count every new trip
		if (vehiclesInSubNetwork.contains(event.getVehicleId())) {
			vehiclesInSubNetwork.remove(event.getVehicleId());
			
			// consider delay on last link if link belongs to the subnetwork
			if (subNetwork.getLinks().containsKey(event.getLinkId())) {
				double currentDelay = event.getTime() - veh2earliestLinkExitTime.remove(event.getVehicleId());
				totalDelaySubnetwork += currentDelay;
				totalTtSubnetwork += currentDelay;
			}
		}
	}

	@Override
	public void reset(int iteration) {
		this.numberOfTripsInSubnetwork = 0;
		this.numberOfTripsInTotal = 0;
		this.totalDistanceSubnetwork = 0.;
		this.totalTtSubnetwork = 0.;
		this.totalDelaySubnetwork = 0.;
		this.vehiclesInSubNetwork.clear();
		this.pers2lastDepatureTime.clear();
		this.veh2earliestLinkExitTime.clear();
	}
	
	public int getNumberOfTripsInSubnetwork() {
		return numberOfTripsInSubnetwork;
	}
	
	public double getRelativeNumberOfTripsInSubnetwork() {
		return numberOfTripsInSubnetwork * 1. / numberOfTripsInTotal;
	}
	
	public double getTotalDelaySubnetwork() {
		return totalDelaySubnetwork;
	}
	
	public double getTotalDistanceSubnetwork() {
		return totalDistanceSubnetwork;
	}
	
	public double getTotalTtSubnetwork() {
		return totalTtSubnetwork;
	}
}
