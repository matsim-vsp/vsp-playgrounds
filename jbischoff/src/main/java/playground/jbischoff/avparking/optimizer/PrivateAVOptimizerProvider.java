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

import java.util.List;

import javax.inject.Inject;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.MapConfiguration;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.contrib.parking.parkingsearch.manager.ParkingSearchManager;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizer;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizerContext;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.scheduler.TaxiScheduler;
import org.matsim.contrib.taxi.scheduler.TaxiSchedulerParams;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Provider;
import com.google.inject.name.Named;

import playground.jbischoff.avparking.AvParkingContext;
import playground.michalm.taxi.optimizer.rules.RuleBasedETaxiOptimizerParams;

public class PrivateAVOptimizerProvider implements Provider<TaxiOptimizer> {
	public static final String TYPE = "type";

	private final TaxiConfigGroup taxiCfg;
	private final Network network;
	private final Fleet fleet;
	private final TravelTime travelTime;
	private final QSim qSim;

	private final ParkingSearchManager manager;

	private AvParkingContext context;

	@Inject
	public PrivateAVOptimizerProvider(TaxiConfigGroup taxiCfg, @Named(DvrpModule.DVRP_ROUTING) Network network,
			Fleet fleet, @Named(DvrpTravelTimeModule.DVRP_ESTIMATED) TravelTime travelTime, QSim qSim, ParkingSearchManager manager, AvParkingContext context) {
		this.taxiCfg = taxiCfg;
		this.network = network;
		this.fleet = fleet;
		this.travelTime = travelTime;
		this.qSim = qSim;
		this.manager = manager;
		this.context = context;
		
		
	}

	@Override
	public TaxiOptimizer get() {
		
		TaxiSchedulerParams schedulerParams = new TaxiSchedulerParams(taxiCfg);
		TravelDisutility travelDisutility = new TimeAsTravelDisutility(travelTime);
		PrivateAVScheduler scheduler = new PrivateAVScheduler(taxiCfg, network, fleet, qSim.getSimTimer(), schedulerParams,
				travelTime, travelDisutility);

		TaxiOptimizerContext optimContext = new TaxiOptimizerContext(fleet, network, qSim.getSimTimer(), travelTime,
				travelDisutility, scheduler);
		
		Configuration optimizerConfig = new MapConfiguration(taxiCfg.getOptimizerConfigGroup().getParams());
		return new PrivateAVTaxiDispatcher(optimContext, new RuleBasedETaxiOptimizerParams(optimizerConfig), manager, context);
	}
}
