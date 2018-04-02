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

package playground.michalm.taxi.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.router.DvrpRoutingNetworkProvider;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.contrib.taxi.run.TaxiConfigConsistencyChecker;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.run.TaxiModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.matsim.vsp.ev.EvConfigGroup;
import org.matsim.vsp.ev.EvModule;
import org.matsim.vsp.ev.charging.ChargingLogic;
import org.matsim.vsp.ev.data.ChargingInfrastructure;
import org.matsim.vsp.ev.data.EvFleet;
import org.matsim.vsp.ev.stats.ChargerOccupancyTimeProfileCollectorProvider;
import org.matsim.vsp.ev.stats.ChargerOccupancyXYDataProvider;

import com.google.inject.Key;
import com.google.inject.name.Names;

import playground.michalm.taxi.data.file.EvrpFleetProvider;
import playground.michalm.taxi.ev.ETaxiChargingLogicFactory;
import playground.michalm.taxi.ev.EvFleetProvider;

public class RunETaxiScenario {
	private static final String CONFIG_FILE = "mielec_2014_02/mielec_etaxi_config.xml";

	public static void run(String configFile, boolean otfvis) {
		Config config = ConfigUtils.loadConfig(configFile, new TaxiConfigGroup(), new DvrpConfigGroup(),
				new OTFVisConfigGroup(), new EvConfigGroup());
		createControler(config, otfvis).run();
	}

	public static Controler createControler(Config config, boolean otfvis) {
		DvrpConfigGroup.get(config).setNetworkMode(null);// to switch off network filtering
		TaxiConfigGroup taxiCfg = TaxiConfigGroup.get(config);
		config.addConfigConsistencyChecker(new TaxiConfigConsistencyChecker());
		config.checkConsistency();

		Scenario scenario = ScenarioUtils.loadScenario(config);

		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new TaxiModule());
		controler.addOverridingModule(new EvModule());
		controler.addOverridingModule(ETaxiOptimizerModules.createDefaultModule());

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(Network.class).annotatedWith(Names.named(ChargingInfrastructure.CHARGERS))//
						.to(Key.get(Network.class, Names.named(DvrpRoutingNetworkProvider.DVRP_ROUTING)))
						.asEagerSingleton();
				bind(ChargingLogic.Factory.class).to(ETaxiChargingLogicFactory.class).asEagerSingleton();

				bind(Fleet.class).toProvider(new EvrpFleetProvider(taxiCfg.getTaxisFileUrl(getConfig().getContext())))
						.asEagerSingleton();

				bind(EvFleet.class).toProvider(new EvFleetProvider(() -> 20, // aux power about 1 kW at 20oC
						EvFleetProvider::isTurnedOn)).asEagerSingleton();

				addMobsimListenerBinding().toProvider(ChargerOccupancyTimeProfileCollectorProvider.class);
				addMobsimListenerBinding().toProvider(ChargerOccupancyXYDataProvider.class);
			}
		});

		if (otfvis) {
			controler.addOverridingModule(new OTFVisLiveModule());
		}

		return controler;
	}

	public static void main(String[] args) {
		// String configFile = "./src/main/resources/one_etaxi/one_etaxi_config.xml";
		// String configFile =
		// "../../shared-svn/projects/maciejewski/Mielec/2014_02_base_scenario/mielec_etaxi_config.xml";
		RunETaxiScenario.run(CONFIG_FILE, false);
	}
}
