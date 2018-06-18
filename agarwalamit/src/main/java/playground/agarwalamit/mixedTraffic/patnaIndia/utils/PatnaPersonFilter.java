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
package playground.agarwalamit.mixedTraffic.patnaIndia.utils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.Gbl;
import playground.agarwalamit.utils.PersonFilter;

/**
 * @author amit
 */

public class PatnaPersonFilter implements PersonFilter{

	private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(PatnaPersonFilter.class);

	public enum PatnaUserGroup {
		urban, commuter, through
    }

    private int defaultSubPopCounter = 0;

	public static boolean isPersonBelongsToUrban(Id<Person> personId){
		return personId.toString().startsWith("slum") || personId.toString().startsWith("nonSlum");
	}
	
	public static boolean isPersonBelongsToSlum(Id<Person> personId){
		return personId.toString().startsWith("slum");
	}
	
	public static boolean isPersonBelongsToNonSlum(Id<Person> personId){
		return personId.toString().startsWith("nonsSum");
	}

	public static boolean isPersonBelongsToCommuter(Id<Person> personId){
		return Arrays.asList(personId.toString().split("_")).contains("E2I");
	}
	
	public static boolean isPersonBelongsToThroughTraffic(Id<Person> personId){
		return Arrays.asList(personId.toString().split("_")).contains("E2E");
	}
	
	public static PatnaUserGroup getUserGroup(Id<Person> personId){
		if(isPersonBelongsToUrban(personId)) return PatnaUserGroup.urban;
		else if(isPersonBelongsToCommuter(personId)) return PatnaUserGroup.commuter;
		else if (isPersonBelongsToThroughTraffic(personId)) return PatnaUserGroup.through;
		else throw new RuntimeException("Person id "+personId+" do not belong to any of the predefined user group. Aborting ...");
	}

	@Override
	public String getUserGroupAsStringFromPersonId(Id<Person> personId) {
		return PatnaPersonFilter.getUserGroup(personId).toString();
	}

	@Override
	public List<String> getUserGroupsAsStrings() {
		return Arrays.stream(PatnaUserGroup.values()).map(Enum::toString).collect(Collectors.toList());
	}

	@Override
	public boolean includePerson(Id<Person> personId){
		if (defaultSubPopCounter==0){
			LOG.warn("By default, persons belongs to "+PatnaUserGroup.urban+" will be included for the analysis/simulation.");
			LOG.warn(Gbl.ONLYONCE);
			defaultSubPopCounter++;
		}
		return isPersonBelongsToUrban(personId);
	}
}
