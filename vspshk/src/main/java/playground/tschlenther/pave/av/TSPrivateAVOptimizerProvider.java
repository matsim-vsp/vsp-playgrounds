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

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.router.DvrpRoutingNetworkProvider;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizer;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.scheduler.TaxiScheduler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Provider;
import com.google.inject.name.Named;

/**
 * @author tschlenther
 *
 */
public class TSPrivateAVOptimizerProvider implements Provider<TaxiOptimizer> {

	private final EventsManager eventsManager;
	private final TaxiConfigGroup taxiCfg;
	private final Fleet fleet;
	private final TaxiScheduler scheduler;
	private final MobsimTimer timer;
	private final TravelTime travelTime;
	private final TravelDisutility travelDisutility;
	private final Network network;

	public TSPrivateAVOptimizerProvider(EventsManager eventsManager, TaxiConfigGroup taxiCfg, Fleet fleet,
			TaxiScheduler scheduler, MobsimTimer timer, @Named(DvrpRoutingNetworkProvider.DVRP_ROUTING) Network network,
			@Named(DvrpTravelTimeModule.DVRP_ESTIMATED) TravelTime travelTime, TravelDisutility travelDisutility) {
		this.eventsManager = eventsManager;
		this.taxiCfg = taxiCfg;
		this.fleet = fleet;
		this.scheduler = scheduler;
		this.timer = timer;
		this.network = network;
		this.travelTime = travelTime;
		this.travelDisutility = travelDisutility;
	}

	@Override
	public TaxiOptimizer get() {
		LeastCostPathCalculator router = new DijkstraFactory().createPathCalculator(network, travelDisutility,
				travelTime);
		TSPrivateAVRequestInserter requestInserter = new TSPrivateAVRequestInserter(fleet, scheduler, timer, travelTime,
				router);
		return new TSPrivateAVTaxiDispatcher(eventsManager, taxiCfg, fleet, scheduler, requestInserter);
	}

}
