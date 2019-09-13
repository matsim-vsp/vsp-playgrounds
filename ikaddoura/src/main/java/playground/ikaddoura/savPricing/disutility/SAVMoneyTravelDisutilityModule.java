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

package playground.ikaddoura.savPricing.disutility;

import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;

import playground.ikaddoura.moneyTravelDisutility.MoneyEventAnalysis;

/**
* @author ikaddoura
*/

public class SAVMoneyTravelDisutilityModule extends AbstractDvrpModeModule {
	
	private final SAVOptimizerMoneyTimeDistanceTravelDisutilityFactory factory;	

	public SAVMoneyTravelDisutilityModule(String savMode,
			SAVOptimizerMoneyTimeDistanceTravelDisutilityFactory factory) {
		super(savMode);
		this.factory = factory;
	}

	@Override
	public void install() {

		this.bindModal(TravelDisutilityFactory.class).toInstance(factory);
		
		this.bind(MoneyEventAnalysis.class).asEagerSingleton();	
		this.addControlerListenerBinding().to(MoneyEventAnalysis.class);
		this.addEventHandlerBinding().to(MoneyEventAnalysis.class);
	}

}

