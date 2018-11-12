/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DefaultControlerModules.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package scenarios.illustrative.smith;

import org.matsim.api.core.v01.events.LinkEnterEvent;

import scenarios.illustrative.analysis.TtAbstractAnalysisTool;

/**
 * This class extends the abstract analysis tool for the specific scenario that
 * we called Smith' scenario.
 * 
 * @see scenarios.illustrative.analysis.TtAbstractAnalysisTool
 * 
 * @author tthunig
 * 
 */
final class AnalyzeSmith extends TtAbstractAnalysisTool {
	
	@Override
	protected int determineRoute(LinkEnterEvent linkEnterEvent) {
		int route = -1;
		switch (linkEnterEvent.getLinkId().toString()) {
		case "2_3":
			// upper route
			route = 0;
			break;
		case "2_4":
			// lower route
			route = 1;
			break;
		default:
			break;
		}
		return route;
	}

	@Override
	protected void defineNumberOfRoutes() {
		setNumberOfRoutes(2);
	}

}
