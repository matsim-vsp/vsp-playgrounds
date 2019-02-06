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
package playground.tschlenther.pave.initialSetup;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.router.DvrpRoutingNetworkProvider;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.contrib.taxi.run.TaxiConfigConsistencyChecker;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.run.TaxiModule;
import org.matsim.contrib.taxi.scheduler.TaxiScheduler;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.config.groups.QSimConfigGroup.StarttimeInterpretation;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

import playground.jbischoff.avparking.optimizer.PrivateAVScheduler;
import playground.tschlenther.pave.av.TSPrivateAVFleetGenerator;

/**
 * @author tschlenther
 *
 */
public class RunMaasScenario {

	public static final String CONFIG_FILE_RULEBASED = "C:/TU Berlin/MasterArbeit/input/mielec_taxi_config_rulebased.xml";
	public static final String CONFIG_FILE_ASSIGNMENT = "C:/TU Berlin/MasterArbeit/input/mielec_taxi_config_assigment.xml";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		run();
	}

	static void run() {

		TaxiConfigGroup taxiCfg = new TaxiConfigGroup();
		Config config = ConfigUtils.loadConfig(CONFIG_FILE_RULEBASED, new DvrpConfigGroup(), taxiCfg);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		config.controler().setLastIteration(1);
		config.controler().setOutputDirectory("output/test/");
		config.qsim().setStartTime(0);
		config.qsim().setSimStarttimeInterpretation(StarttimeInterpretation.onlyUseStarttime);
		config.qsim().setSnapshotStyle(SnapshotStyle.withHoles);
		config.global().setNumberOfThreads(8);
		config.addConfigConsistencyChecker(new TaxiConfigConsistencyChecker());
		config.checkConsistency();
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);

		TSPrivateAVFleetGenerator fleet = new TSPrivateAVFleetGenerator(scenario);

		controler.addOverridingModule(new DvrpModule());
		controler.configureQSimComponents(DvrpQSimComponents.activateModes(taxiCfg.getMode()));

		controler.addOverridingModule(new TaxiModule());

		controler.addOverridingModule(new AbstractModule() {
			@Inject
			private TaxiConfigGroup taxiCfg;

			@Override
			public void install() {
				bind(Fleet.class).annotatedWith(DvrpModes.mode(taxiCfg.getMode())).toInstance(fleet);
				addControlerListenerBinding().toInstance(fleet);

				installQSimModule(new AbstractDvrpModeQSimModule(taxiCfg.getMode()) {
					@Override
					protected void configureQSim() {
						bindModal(TaxiScheduler.class).toProvider(new Provider<TaxiScheduler>() {
							@Inject
							@Named(DvrpRoutingNetworkProvider.DVRP_ROUTING)
							private Network network;

							@Inject
							private MobsimTimer timer;

							@Inject
							@Named(DvrpTravelTimeModule.DVRP_ESTIMATED)
							private TravelTime travelTime;

							@Inject
							private TravelDisutilityFactory travelDisutilityFactory;

							@Override
							public TaxiScheduler get() {
								return new PrivateAVScheduler(taxiCfg, fleet, network, timer, travelTime,
										travelDisutilityFactory.createTravelDisutility(travelTime));
							}
						}).asEagerSingleton();
					}
				});
			}
		});

		controler.run();
	}

}
