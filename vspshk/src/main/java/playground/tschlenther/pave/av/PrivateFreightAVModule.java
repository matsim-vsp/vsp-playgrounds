/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.tschlenther.pave.av;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.data.file.FleetProvider;
import org.matsim.contrib.dvrp.passenger.DefaultPassengerRequestValidator;
import org.matsim.contrib.dvrp.passenger.PassengerRequestValidator;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.dynagent.run.DynRoutingModule;
import org.matsim.contrib.taxi.data.validator.DefaultTaxiRequestValidator;
import org.matsim.contrib.taxi.data.validator.TaxiRequestValidator;
import org.matsim.contrib.taxi.passenger.SubmittedTaxiRequestsCollector;
import org.matsim.contrib.taxi.run.Taxi;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.util.TaxiSimulationConsistencyChecker;
import org.matsim.contrib.taxi.util.stats.TaxiStatsDumper;
import org.matsim.contrib.taxi.util.stats.TaxiStatusTimeProfileCollectorProvider;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;

import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.name.Names;

/**
 * @author tschlenther
 *
 */
public final class PrivateFreightAVModule extends AbstractDvrpModeModule{

	@Inject
	private TaxiConfigGroup taxiCfg;
	
	private Scenario scenario;

	public PrivateFreightAVModule(Scenario scenario) {
		this.scenario = scenario;
	}
	
	@Override
	public void install() {
		String mode = taxiCfg.getMode();
//		install(FleetProvider.createModule(mode, taxiCfg.getTaxisFileUrl(getConfig().getContext())));
		
		TSPrivateAVFleetGenerator  fleet = new TSPrivateAVFleetGenerator(scenario);  
		
		
		//for some reason (annotations!?) fleet must be bound three times
		bind(Fleet.class).annotatedWith(Names.named(taxiCfg.getMode())).toInstance(fleet);
		bind(Fleet.class).annotatedWith(Taxi.class).toInstance(fleet);
		bind(Fleet.class).toInstance(fleet);
		
		addControlerListenerBinding().toInstance(fleet);

		bind(TravelDisutilityFactory.class).annotatedWith(Taxi.class)
				.toInstance(travelTime -> new TimeAsTravelDisutility(travelTime));

		bind(SubmittedTaxiRequestsCollector.class).asEagerSingleton();
		addControlerListenerBinding().to(SubmittedTaxiRequestsCollector.class);

		addControlerListenerBinding().to(TaxiSimulationConsistencyChecker.class);
		addControlerListenerBinding().to(TaxiStatsDumper.class);

		addRoutingModuleBinding(mode).toInstance(new DynRoutingModule(mode));

		if (taxiCfg.getTimeProfiles()) {
			addMobsimListenerBinding().toProvider(TaxiStatusTimeProfileCollectorProvider.class);
			// add more time profiles if necessary
		}
		bindModal(PassengerRequestValidator.class).to(DefaultPassengerRequestValidator.class).asEagerSingleton();
//		bind(TaxiRequestValidator.class).to(DefaultTaxiRequestValidator.class);		
	}

}
