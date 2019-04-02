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

package playground.ikaddoura.analysis.linkDemandFiltered;

import java.util.Set;

/**
* @author ikaddoura
*/

public class ModeFilter implements Filter {

	private final Set<String> modesToInclude;
	
	public ModeFilter(Set<String> modesToInclude) {
		this.modesToInclude = modesToInclude;
	}
	
	@Override
	public boolean include(String mode) {
		return modesToInclude.contains(mode);
	}

	@Override
	public String toFileName() {
		return "FILTER-modes=" + modesToInclude.toString();
	}

}

