/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.santiago.analysis.modalShareFromEvents;

import java.util.List;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

/**
 * @author amit
 */

public interface PersonFilter {
	
	String getUserGroupAsStringFromPersonId(final Id<Person> personId);

	List<String> getUserGroupsAsStrings();

	default boolean includePerson(Id<Person> personId){
		throw new RuntimeException("This method is not implemented yet, user other methods instead. " +
				"The idea is to use this method if a user group is not provided for filtering i.e. setting up a default user group in the filter itself.");
	}

}
