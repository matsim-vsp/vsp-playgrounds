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

import java.lang.annotation.Annotation;

import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;

import com.google.inject.Inject;

import playground.ikaddoura.moneyTravelDisutility.MoneyEventAnalysis;
import playground.ikaddoura.moneyTravelDisutility.data.AgentFilter;

/**
* @author ikaddoura
*/

public class SAVMoneyTravelDisutilityModule extends AbstractModule {
	
	private final Class<? extends Annotation> savOptimizerModeAnnotation;
	private final SAVOptimizerMoneyTimeDistanceTravelDisutilityFactory factory;
	
	@Inject(optional = true)
	private AgentFilter agentFilter;
	
	private final AgentFilter agentFilterToBind;
	
	public SAVMoneyTravelDisutilityModule(Class<? extends Annotation> savOptimizerModeAnnotation, SAVOptimizerMoneyTimeDistanceTravelDisutilityFactory factory, AgentFilter agentFilter) {
		this.savOptimizerModeAnnotation = savOptimizerModeAnnotation;
		this.factory = factory;
		this.agentFilterToBind = agentFilter;
	}

	public SAVMoneyTravelDisutilityModule(Class<? extends Annotation> savOptimizerModeAnnotation, SAVOptimizerMoneyTimeDistanceTravelDisutilityFactory factory) {
		this.savOptimizerModeAnnotation = savOptimizerModeAnnotation;
		this.factory = factory;
		this.agentFilterToBind = null;
	}

	@Override
	public void install() {
		
		if (agentFilterToBind != null && agentFilter == null ) {
			this.bind(AgentFilter.class).toInstance(agentFilterToBind);
		}
		
		this.bind(TravelDisutilityFactory.class).annotatedWith(savOptimizerModeAnnotation).toInstance(factory);
		
		this.bind(MoneyEventAnalysis.class).asEagerSingleton();	
		this.addControlerListenerBinding().to(MoneyEventAnalysis.class);
		this.addEventHandlerBinding().to(MoneyEventAnalysis.class);
	}

}

