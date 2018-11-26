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

package playground.jbischoff.avparking.optimizer;

import javax.inject.Inject;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.MapConfiguration;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.router.DvrpRoutingNetworkProvider;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.contrib.parking.parkingsearch.manager.ParkingSearchManager;
import org.matsim.contrib.taxi.data.validator.TaxiRequestValidator;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizer;
import org.matsim.contrib.taxi.optimizer.rules.RuleBasedTaxiOptimizerParams;
import org.matsim.contrib.taxi.run.Taxi;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.scheduler.TaxiScheduler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Provider;
import com.google.inject.name.Named;

import playground.jbischoff.avparking.AvParkingContext;

public class PrivateAVOptimizerProvider implements Provider<TaxiOptimizer> {
	public static final String TYPE = "type";

	private final TaxiConfigGroup taxiCfg;
	private final Fleet fleet;
	private final Network network;
	private final MobsimTimer timer;
	private final TravelTime travelTime;
	private final TravelDisutility travelDisutility;
	private final TaxiScheduler scheduler;

	private final ParkingSearchManager manager;
	private final AvParkingContext context;
	
	private final TaxiRequestValidator requestValidator;
	private final EventsManager events;

	@Inject
	public PrivateAVOptimizerProvider(TaxiConfigGroup taxiCfg, @Taxi Fleet fleet,
			@Named(DvrpRoutingNetworkProvider.DVRP_ROUTING) Network network, MobsimTimer timer,
			@Named(DvrpTravelTimeModule.DVRP_ESTIMATED) TravelTime travelTime,
			@Taxi TravelDisutility travelDisutility,
			TaxiScheduler scheduler, ParkingSearchManager manager, AvParkingContext context,
			TaxiRequestValidator requestValidator, EventsManager events) {
		this.taxiCfg = taxiCfg;
		this.fleet = fleet;
		this.network = network;
		this.timer = timer;
		this.travelTime = travelTime;
		this.travelDisutility = travelDisutility;
		this.scheduler = scheduler;
		this.manager = manager;
		this.context = context;
		this.requestValidator = requestValidator;
		this.events = events;

	}

	@Override
	public TaxiOptimizer get() {
		Configuration optimizerConfig = new MapConfiguration(taxiCfg.getOptimizerConfigGroup().getParams());
		return PrivateAVTaxiDispatcher.create(taxiCfg, fleet, network, timer, travelTime, travelDisutility, scheduler,
				new RuleBasedTaxiOptimizerParams(optimizerConfig), manager, context, requestValidator, events);
	}
}
