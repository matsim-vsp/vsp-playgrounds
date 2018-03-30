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
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.data.FleetImpl;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.taxi.benchmark.DvrpBenchmarkControlerModule;
import org.matsim.contrib.taxi.benchmark.DvrpBenchmarkTravelTimeModule;
import org.matsim.contrib.taxi.benchmark.RunTaxiBenchmark;
import org.matsim.contrib.taxi.benchmark.TaxiBenchmarkConfigConsistencyChecker;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.run.TaxiModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.vsp.ev.EvConfigGroup;
import org.matsim.vsp.ev.EvModule;
import org.matsim.vsp.ev.data.ChargingInfrastructureImpl;
import org.matsim.vsp.ev.data.EvDataImpl;
import org.matsim.vsp.ev.data.file.ChargerReader;
import org.matsim.vsp.ev.stats.ChargerOccupancyTimeProfileCollectorProvider;
import org.matsim.vsp.ev.stats.ChargerOccupancyXYDataProvider;

import playground.michalm.taxi.data.file.EvrpVehicleReader;
import playground.michalm.taxi.ev.ETaxiUtils;

/**
 * For a fair and consistent benchmarking of taxi dispatching algorithms we assume that link travel times are
 * deterministic. To simulate this property, we remove (1) all other traffic, and (2) link capacity constraints (e.g. by
 * increasing the capacities by 100+ times), as a result all vehicles move with the free-flow speed (which is the
 * effective speed).
 * <p>
 * </p>
 * To model the impact of traffic, we can use a time-variant network, where we specify different free-flow speeds for
 * each link over time. The default approach is to specify free-flow speeds in each time interval (usually 15 minutes).
 */
public class RunETaxiBenchmark {
	public static void run(String configFile, int runs) {
		Config config = ConfigUtils.loadConfig(configFile, new TaxiConfigGroup(), new DvrpConfigGroup(),
				new EvConfigGroup());
		createControler(config, runs).run();
	}

	public static Controler createControler(Config config, int runs) {
		DvrpConfigGroup.get(config).setNetworkMode(null);// to switch off network filtering
		TaxiConfigGroup taxiCfg = TaxiConfigGroup.get(config);
		EvConfigGroup evCfg = EvConfigGroup.get(config);
		config.controler().setLastIteration(runs - 1);
		config.addConfigConsistencyChecker(new TaxiBenchmarkConfigConsistencyChecker());
		config.checkConsistency();

		Scenario scenario = RunTaxiBenchmark.loadBenchmarkScenario(config, 15 * 60, 30 * 3600);

		// TODO bind Fleet and EvData
		final FleetImpl fleet = new FleetImpl();
		new EvrpVehicleReader(scenario.getNetwork(), fleet).parse(taxiCfg.getTaxisFileUrl(config.getContext()));
		final ChargingInfrastructureImpl chargingInfrastructure = new ChargingInfrastructureImpl();
		new ChargerReader(scenario.getNetwork(), chargingInfrastructure)
				.parse(evCfg.getChargersFileUrl(config.getContext()));
		EvDataImpl evData = new EvDataImpl();
		ETaxiUtils.initEvData(fleet, evData, chargingInfrastructure);

		Controler controler = new Controler(scenario);
		controler.setModules(new DvrpBenchmarkControlerModule());
		controler.addOverridingModule(new TaxiModule());
		controler.addOverridingModule(new EvModule(evData, chargingInfrastructure));

		controler.addOverridingModule(ETaxiOptimizerModules.createBenchmarkModule());

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addMobsimListenerBinding().toProvider(ChargerOccupancyTimeProfileCollectorProvider.class);
				addMobsimListenerBinding().toProvider(ChargerOccupancyXYDataProvider.class);
				addControlerListenerBinding().to(ETaxiBenchmarkStats.class).asEagerSingleton();
				bind(Fleet.class).toInstance(fleet);// overrride the binding specified in TaxiModule
				install(new DvrpBenchmarkTravelTimeModule());
			}
		});

		return controler;
	}

	public static void main(String[] args) {
		String cfg = "../../shared-svn/projects/maciejewski/Mielec/2014_02_base_scenario/" + //
				"mielec_etaxi_benchmark_config.xml";
		run(cfg, 1);
	}
}
