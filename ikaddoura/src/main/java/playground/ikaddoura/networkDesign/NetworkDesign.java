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

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

import com.google.inject.Inject;

/**
 * 
 * Network design approach: Change link parameters based on revenues and costs.
 * 
 * @author ikaddoura
 *
 */

public class NetworkDesign implements IterationEndsListener, LinkEnterEventHandler {
	
	private static final Logger log = Logger.getLogger(NetworkDesign.class);

	private final int networkAdjustmentInterval = 5;
	private final int networkAdjustmentStartIteration = 0;
		
	private final double minimumCapacity = 250.;
	private final double minimumNumberOfLanes = 1.;

	private final boolean accountForProfit = false;
	private final double revenuesPerCarUserAndMeter = 0.006; // Energiesteuer 0.60 EUR / l ; 10 l pro 100 km
	private final double infrastructureCostPerLaneAndMeter = 0.05; // 20. / 365.
	
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
				if (linkId2Volume.get(linkId) != null) volume = linkId2Volume.get(linkId) / scenario.getConfig().qsim().getFlowCapFactor();
								
				if (volume > link.getCapacity()) {
					
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
	}

	private void reduceCapacity(Link link) {
		link.setCapacity(link.getCapacity() - absoluteCapacityAdjustment);
		link.setNumberOfLanes(link.getNumberOfLanes() - absoluteLaneAdjustment);
	
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
	
}
