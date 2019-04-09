/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package playground.ikaddoura.savPricing;

import java.util.HashSet;
import java.util.Set;

import org.matsim.contrib.dvrp.passenger.DefaultPassengerRequestValidator;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;
import org.matsim.contrib.dvrp.passenger.PassengerRequestValidator;

/**
* @author ikaddoura
*/

public final class ServiceAreaRequestValidator implements PassengerRequestValidator {

	public static final String FROM_LINK_NOT_IN_SERVICE_AREA_CAUSE = "from_link_not_in_service_area";
	public static final String TO_LINK_NOT_IN_SERVICE_AREA_CAUSE = "to_link_not_in_service_area";

	private final DefaultPassengerRequestValidator delegate = new DefaultPassengerRequestValidator();

	private final String serviceAreaAttribute;

	public ServiceAreaRequestValidator(String serviceAreaAttribute) {
		this.serviceAreaAttribute = serviceAreaAttribute;
	}

	@Override
	public Set<String> validateRequest(PassengerRequest request) {

		Set<String> invalidRequestCauses = new HashSet<>();

		invalidRequestCauses.addAll(this.delegate.validateRequest(request));

		boolean fromLinkInServiceArea = (boolean)request.getFromLink()
				.getAttributes()
				.getAttribute(serviceAreaAttribute);
		boolean toLinkInServiceArea = (boolean)request.getToLink().getAttributes().getAttribute(serviceAreaAttribute);

		if (!fromLinkInServiceArea ) {
			invalidRequestCauses.add(FROM_LINK_NOT_IN_SERVICE_AREA_CAUSE);
		}
		if (!toLinkInServiceArea) {
			invalidRequestCauses.add(TO_LINK_NOT_IN_SERVICE_AREA_CAUSE);
		}
		
		return invalidRequestCauses;
	}

}

