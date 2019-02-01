/* *********************************************************************** *
 * project: org.matsim.*
 * MyControlerListener.java
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

package playground.ikaddoura.networkDesign;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import com.google.inject.Inject;

import playground.ikaddoura.analysis.shapes.Network2Shape;

/**
 * 
 * Network design approach: Change link parameters based on revenues and costs.
 * 
 * @author ikaddoura
 *
 */

public class NetworkDesign implements IterationStartsListener, IterationEndsListener, LinkEnterEventHandler {
	
	private static final Logger log = Logger.getLogger(NetworkDesign.class);

	private final String crs = TransformationFactory.DHDN_GK4;
	private final CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(crs, crs);
	
	private final double sampleSize = 100;
	private final int networkAdjustmentInterval = 1;
	private final int networkAdjustmentStartIteration = 0;
	private final int writeNetworkInterval = 10;
		
	private final double minimumCapacity = 250.;
	private final double minimumNumberOfLanes = 1.;

	private final boolean accountForProfit = true;
	private final double revenuesPerCarUserAndMeter = 0.001;
	private final double infrastructureCostPerLaneAndMeter = 2.5;
	
	private final double absoluteCapacityAdjustment = 250;
	private final double absoluteLaneAdjustment = 0.25;
	
	private final Map<Id<Link>, Integer> linkId2Volume = new HashMap<>();
	
	@Inject
	private Scenario scenario;

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		
		if (event.getIteration() >= networkAdjustmentStartIteration && event.getIteration() % networkAdjustmentInterval == 0.) {
			log.info("Adjusting network...");
			
			for (Id<Link> linkId : scenario.getNetwork().getLinks().keySet()) {
				Link link = scenario.getNetwork().getLinks().get(linkId);

				double volume = 0;
				if (linkId2Volume.get(linkId) != null) volume = linkId2Volume.get(linkId) * sampleSize;
								
				if (volume > link.getCapacity() * 24) {
					
					if (accountForProfit) {
						final double revenues = volume  * link.getLength() * revenuesPerCarUserAndMeter;
						final double infrastructureCosts = link.getNumberOfLanes() * link.getLength() * infrastructureCostPerLaneAndMeter;	
						
						final double subsidy = 0.; // TODO: account for user benefits, accessibility, ...
						final double toll = 0.; // TODO: noise, ...
						
						final double profit = revenues - infrastructureCosts + subsidy - toll;
						
						if (profit > 0.) {
							increaseCapacity(link);
						} else {
							reduceCapacity(link);
						}
					} else {
						increaseCapacity(link);
					}
				} else {
					reduceCapacity(link);
				}
			}
		}
		
	}

	private void increaseCapacity(Link link) {
		link.setCapacity(link.getCapacity() + absoluteCapacityAdjustment);
		link.setNumberOfLanes(link.getNumberOfLanes() + absoluteLaneAdjustment);
		link.setFreespeed(getFreeSpeed(link.getCapacity(), link.getNumberOfLanes()));
	}

	private double getFreeSpeed(double capacity, double numberOfLanes) {
		double freeSpeed = 0.;
		if (capacity < 500.) {
			freeSpeed = 15/3.6;
		} else if (capacity >= 500. && capacity < 1000.) {
			freeSpeed = 30/3.6;
		} else if (capacity >= 1000. && capacity < 2000.) {
			freeSpeed = 50/3.6;
		} else if (capacity >= 2000.) {
			freeSpeed = 80/3.6;
		}
		return freeSpeed;
	}

	private void reduceCapacity(Link link) {
		link.setCapacity(link.getCapacity() - absoluteCapacityAdjustment);
		link.setNumberOfLanes(link.getNumberOfLanes() - absoluteLaneAdjustment);
		link.setFreespeed(getFreeSpeed(link.getCapacity(), link.getNumberOfLanes()));
	
		if (Double.isInfinite(link.getCapacity()) || link.getCapacity() < minimumCapacity ||
				Double.isInfinite(link.getNumberOfLanes()) || link.getNumberOfLanes() < minimumNumberOfLanes) {
			link.setCapacity(minimumCapacity);
			link.setNumberOfLanes(minimumNumberOfLanes);
		}
	}

	@Override
	public void reset(int iteration) {
		linkId2Volume.clear();
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		Id<Link> linkId = event.getLinkId();
		if (linkId2Volume.get(linkId) == null) {
			linkId2Volume.put(linkId, 1);
		} else {
			linkId2Volume.put(linkId, linkId2Volume.get(linkId) + 1);
		}
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
				
		if (event.getIteration() % writeNetworkInterval == 0) {
			log.info("Writing adjusted network...");
			new NetworkWriter(scenario.getNetwork()).write(scenario.getConfig().controler().getOutputDirectory() + "/" + event.getIteration() + ".adjusted-network.xml.gz");
			String shpDir = scenario.getConfig().controler().getOutputDirectory() + "/" + event.getIteration() + ".adjusted-network/";
			File file = new File(shpDir);
			file.mkdirs();
			Network2Shape.exportNetwork2Shp(scenario, shpDir, crs, ct);
			log.info("Writing adjusted network... Done.");
		}
		
	}
	
}
