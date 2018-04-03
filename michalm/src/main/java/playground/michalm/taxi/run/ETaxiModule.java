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

package playground.michalm.taxi.run;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.router.DvrpRoutingNetworkProvider;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.core.controler.AbstractModule;
import org.matsim.vsp.ev.charging.ChargingLogic;
import org.matsim.vsp.ev.charging.ChargingWithQueueingAndAssignmentLogic;
import org.matsim.vsp.ev.charging.FixedSpeedChargingStrategy;
import org.matsim.vsp.ev.data.ChargingInfrastructure;
import org.matsim.vsp.ev.discharging.AuxEnergyConsumption;

import com.google.inject.Key;
import com.google.inject.name.Names;

import playground.michalm.taxi.ev.ETaxiAuxConsumptionFactory;

/**
 * @author michalm
 */
public class ETaxiModule extends AbstractModule {
	private static final double CHARGING_SPEED_FACTOR = 1.; // full speed
	private static final double MAX_RELATIVE_SOC = 0.8;// up to 80% SOC
	private static final double TEMPERATURE = 20;// 20 oC

	private static ChargingLogic.Factory ETAXI_CHARGING_LOGIC_FACTORY = charger -> new ChargingWithQueueingAndAssignmentLogic(
			charger, new FixedSpeedChargingStrategy(charger.getPower() * CHARGING_SPEED_FACTOR, MAX_RELATIVE_SOC));

	@Override
	public void install() {
		bind(Network.class).annotatedWith(Names.named(ChargingInfrastructure.CHARGERS))//
				.to(Key.get(Network.class, Names.named(DvrpRoutingNetworkProvider.DVRP_ROUTING))).asEagerSingleton();
		bind(ChargingLogic.Factory.class).toInstance(ETAXI_CHARGING_LOGIC_FACTORY);
		bind(AuxEnergyConsumption.Factory.class).toInstance(new ETaxiAuxConsumptionFactory(() -> TEMPERATURE,
				vehicle -> vehicle.getSchedule().getStatus() == ScheduleStatus.STARTED));
	}
}
