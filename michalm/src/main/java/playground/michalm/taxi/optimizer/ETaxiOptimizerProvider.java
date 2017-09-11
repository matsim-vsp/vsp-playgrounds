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

package playground.michalm.taxi.optimizer;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.MapConfiguration;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.contrib.taxi.optimizer.DefaultTaxiOptimizerProvider;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizer;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

import playground.michalm.ev.data.EvData;
import playground.michalm.taxi.optimizer.assignment.AssignmentETaxiOptimizer;
import playground.michalm.taxi.optimizer.assignment.AssignmentETaxiOptimizerParams;
import playground.michalm.taxi.optimizer.rules.RuleBasedETaxiOptimizer;
import playground.michalm.taxi.optimizer.rules.RuleBasedETaxiOptimizerParams;
import playground.michalm.taxi.scheduler.ETaxiScheduler;

public class ETaxiOptimizerProvider implements Provider<TaxiOptimizer> {
	public static final String TYPE = "type";

	public enum EOptimizerType {
		E_RULE_BASED, E_ASSIGNMENT;
	}

	private final TaxiConfigGroup taxiCfg;
	private final Network network;
	private final Fleet fleet;
	private final MobsimTimer timer;
	private final TravelTime travelTime;
	private final TravelDisutility travelDisutility;
	private final ETaxiScheduler eScheduler;
	private final EvData evData;

	@Inject
	public ETaxiOptimizerProvider(TaxiConfigGroup taxiCfg, Fleet fleet, @Named(DvrpModule.DVRP_ROUTING) Network network,
			MobsimTimer timer, @Named(DvrpTravelTimeModule.DVRP_ESTIMATED) TravelTime travelTime,
			@Named(DefaultTaxiOptimizerProvider.TAXI_OPTIMIZER) TravelDisutility travelDisutility,
			ETaxiScheduler eScheduler, EvData evData) {
		this.taxiCfg = taxiCfg;
		this.fleet = fleet;
		this.network = network;
		this.timer = timer;
		this.travelTime = travelTime;
		this.travelDisutility = travelDisutility;
		this.eScheduler = eScheduler;
		this.evData = evData;
	}

	@Override
	public TaxiOptimizer get() {
		Configuration optimizerConfig = new MapConfiguration(taxiCfg.getOptimizerConfigGroup().getParams());
		EOptimizerType type = EOptimizerType.valueOf(optimizerConfig.getString(TYPE));

		switch (type) {
			case E_RULE_BASED:
				return new RuleBasedETaxiOptimizer(taxiCfg, fleet, network, timer, travelTime, travelDisutility,
						eScheduler, evData, new RuleBasedETaxiOptimizerParams(optimizerConfig));

			case E_ASSIGNMENT:
				return new AssignmentETaxiOptimizer(taxiCfg, fleet, network, timer, travelTime, travelDisutility,
						eScheduler, evData, new AssignmentETaxiOptimizerParams(optimizerConfig));

			default:
				throw new RuntimeException();
		}
	}
}
