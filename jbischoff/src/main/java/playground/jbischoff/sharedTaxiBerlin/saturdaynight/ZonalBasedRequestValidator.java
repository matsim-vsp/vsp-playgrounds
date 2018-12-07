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

/**
 *
 */
package playground.jbischoff.sharedTaxiBerlin.saturdaynight;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.passenger.DefaultPassengerRequestValidator;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;
import org.matsim.contrib.dvrp.passenger.PassengerRequestValidator;
import org.matsim.core.utils.geometry.geotools.MGC;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * @author jbischoff
 */
public class ZonalBasedRequestValidator implements PassengerRequestValidator {

	private Map<String, Geometry> zones;
	private Map<Id<Link>, String> link2zone = new HashMap<>();
	private Network network;
	private final DefaultPassengerRequestValidator delegate = new DefaultPassengerRequestValidator();

	@Inject
	public ZonalBasedRequestValidator(Network network, ZonalSystem initialZones) {
		this.zones = initialZones.getZones();
		this.network = network;

	}

	@Override
	public Set<String> validateRequest(PassengerRequest request) {
		//fail-fast ==> return the first encountered cause
		Set<String> causes = delegate.validateRequest(request);
		if (!causes.isEmpty()) {
			return causes;
		}
		if (!linkIsCovered(request.getFromLink())) {
			return Collections.singleton("fromLink not covered");
		}
		if (!linkIsCovered(request.getToLink())) {
			return Collections.singleton("toLink not covered");
		}
		return causes;//==empty set
	}

	public void updateZones(Map<String, Geometry> newZones) {
		this.zones = newZones;
		this.link2zone.clear();
	}

	private boolean linkIsCovered(Link l) {
		if (getZoneForLinkId(l.getId()) != null)
			return true;
		else
			return false;
	}

	private String getZoneForLinkId(Id<Link> linkId) {
		if (this.link2zone.containsKey(linkId)) {
			return link2zone.get(linkId);
		}

		Point linkCoord = MGC.coord2Point(network.getLinks().get(linkId).getCoord());

		for (Entry<String, Geometry> e : zones.entrySet()) {
			if (e.getValue().contains(linkCoord)) {
				link2zone.put(linkId, e.getKey());
				return e.getKey();
			}
		}
		link2zone.put(linkId, null);
		return null;

	}

	/**
	 * @return the zones
	 */
	public Map<String, Geometry> getZones() {
		return zones;
	}
}
