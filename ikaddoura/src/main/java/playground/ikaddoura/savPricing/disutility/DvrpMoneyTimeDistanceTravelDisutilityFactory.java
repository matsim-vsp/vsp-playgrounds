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
package playground.ikaddoura.savPricing.disutility;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;

import playground.ikaddoura.moneyTravelDisutility.MoneyEventAnalysis;
import playground.ikaddoura.moneyTravelDisutility.data.AgentFilter;


/**
 * 
 * A travel disutility which accounts for link-, time- and vehicle-specific monetary payments.
 * 
 * @author ikaddoura
 *
 */
public final class DvrpMoneyTimeDistanceTravelDisutilityFactory implements TravelDisutilityFactory {
	
	@Inject
	private MoneyEventAnalysis moneyAnalysis;
	
	@Inject(optional = true)
	private AgentFilter vehicleFilter;
	
	@Inject
	private Scenario scenario;
	
	@Override
	public final TravelDisutility createTravelDisutility(TravelTime travelTime) {
				
		return new DvrpMoneyTimeDistanceTravelDisutility(
				travelTime,
				scenario,
				this.moneyAnalysis,
				this.vehicleFilter
			);
	}
	
}
