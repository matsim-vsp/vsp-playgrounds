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

package playground.dziemke.cemdapMatsimCadyts.controller;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import playground.vsp.cadyts.marginals.AgentFilter;

/**
* @author ikaddoura
*/

public class BerlinAgentFilter implements AgentFilter{

	private String[] personIdPrefixesToBeExcluded = {"freight"};

	@Override
	public boolean includeAgent(Id<Person> id) {
		
		for (String prefix : personIdPrefixesToBeExcluded) {
			if (id.toString().startsWith(prefix)) {
				return false;
			}
		}
		return true;
	}
}

