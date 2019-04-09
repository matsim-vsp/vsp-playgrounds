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

package playground.ikaddoura.savPricing.runSetupA.prepare;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;

import playground.ikaddoura.savPricing.BerlinShpUtils;

/**
* @author ikaddoura
*/

public final class BerlinNetworkModification {
	private static final Logger log = Logger.getLogger(BerlinNetworkModification.class);
	private final BerlinShpUtils shpUtils;

	public BerlinNetworkModification(BerlinShpUtils shpUtils) {
		this.shpUtils = shpUtils;
	}
	
	public void addSAVmode(Scenario scenario, String taxiNetworkMode, String serviceAreaAttribute) {
		
		log.info("Adjusting network...");

		int counter = 0;
		for (Link link : scenario.getNetwork().getLinks().values()) {
			if (counter % 10000 == 0)
				log.info("link #" + counter);
			counter++;
			if (link.getAllowedModes().contains(TransportMode.car)
					&& link.getAllowedModes().contains(TransportMode.ride)
					&& link.getAllowedModes().contains("freight")) {
				Set<String> allowedModes = new HashSet<>();
				allowedModes.add(TransportMode.car);
				allowedModes.add("freight");
				allowedModes.add(TransportMode.ride);
				allowedModes.add(taxiNetworkMode);

				link.setAllowedModes(allowedModes);

				if (shpUtils.isCoordInDrtServiceArea(link.getFromNode().getCoord())
						|| shpUtils.isCoordInDrtServiceArea(link.getToNode().getCoord())) {
					link.getAttributes().putAttribute(serviceAreaAttribute, true);
				} else {
					link.getAttributes().putAttribute(serviceAreaAttribute, false);
				}

			} else if (link.getAllowedModes().contains(TransportMode.pt)) {
				// skip pt links
			} else {
				throw new RuntimeException("Aborting...");
			}
		}
	}

}

