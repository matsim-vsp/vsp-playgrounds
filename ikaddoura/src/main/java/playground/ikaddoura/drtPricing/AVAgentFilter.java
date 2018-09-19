/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.ikaddoura.drtPricing;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import playground.ikaddoura.moneyTravelDisutility.data.AgentFilter;

/**
* @author ikaddoura
*/

public class AVAgentFilter implements AgentFilter {
	private static final Logger log = Logger.getLogger(AVAgentFilter.class);
	private int wrnCnt = 0;

	@Override
	public String getAgentTypeFromId(Id<Person> id) {
			
		if (id == null) {
			if (wrnCnt < 5) {
				log.warn("Person id is null. Assuming this person to be a taxi driver.");
				if (wrnCnt == 4) log.warn("Further warnings of this type are not printed out.");
				wrnCnt++;
			}
			return "taxi";
		}
		
		if (id.toString().startsWith("taxi")
				|| id.toString().startsWith("av")
				|| id.toString().startsWith("sav")
				|| id.toString().startsWith("rt")) {
			return "taxi";
		
		} else {
			return "other";
		}
	}

}

