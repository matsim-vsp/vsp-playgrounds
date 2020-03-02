/* *********************************************************************** *
 * project: org.matsim.*
 * DefaultTravelCostCalculatorFactoryImpl
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.ikaddoura.router;

import org.matsim.analysis.vtts.VTTSHandler;
import org.matsim.core.config.Config;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;


/**
 * @author ikaddoura
 *
 */
public final class VTTSTimeDistanceTravelDisutilityFactory implements TravelDisutilityFactory {

	private VTTSHandler vttsHandler;
	private final Config config;
	
	public VTTSTimeDistanceTravelDisutilityFactory(VTTSHandler vttsHandler, Config config) {
		this.vttsHandler = vttsHandler ;
		this.config = config;
	}

	@Override
	public final TravelDisutility createTravelDisutility(TravelTime timeCalculator) {
		return new VTTSTimeDistanceTravelDisutility(timeCalculator, config, vttsHandler);
	}
	
}
