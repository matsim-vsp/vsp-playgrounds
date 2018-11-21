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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.data.file.FleetProvider;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.passenger.PassengerEngine;
import org.matsim.contrib.dvrp.passenger.PassengerRequestCreator;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.MobsimTimerProvider;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelDisutilityProvider;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic.DynActionCreator;
import org.matsim.contrib.dynagent.run.DynRoutingModule;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.contrib.taxi.data.validator.DefaultTaxiRequestValidator;
import org.matsim.contrib.taxi.data.validator.TaxiRequestValidator;
import org.matsim.contrib.taxi.optimizer.DefaultTaxiOptimizerProvider;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizer;
import org.matsim.contrib.taxi.passenger.SubmittedTaxiRequestsCollector;
import org.matsim.contrib.taxi.passenger.TaxiRequestCreator;
import org.matsim.contrib.taxi.run.Taxi;
import org.matsim.contrib.taxi.run.TaxiConfigConsistencyChecker;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.run.TaxiModule;
import org.matsim.contrib.taxi.run.TaxiQSimModule;
import org.matsim.contrib.taxi.scheduler.TaxiScheduler;
import org.matsim.contrib.taxi.util.TaxiSimulationConsistencyChecker;
import org.matsim.contrib.taxi.util.stats.TaxiStatsDumper;
import org.matsim.contrib.taxi.util.stats.TaxiStatusTimeProfileCollectorProvider;
import org.matsim.contrib.taxi.vrpagent.TaxiActionCreator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.config.groups.QSimConfigGroup.StarttimeInterpretation;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import com.google.inject.Inject;
import com.google.inject.Key;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

import playground.jbischoff.avparking.AvParkingContext;
import playground.jbischoff.avparking.ParkingTaxiModule;
import playground.jbischoff.avparking.PrivateAVFleetGenerator;
import playground.jbischoff.avparking.optimizer.PrivateAVOptimizerProvider;
import playground.jbischoff.avparking.optimizer.PrivateAVScheduler;
import playground.tschlenther.pave.av.TSPrivateAVFleetGenerator;
import playground.tschlenther.pave.av.TSPrivateAVOptimizerProvider;

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

		TSPrivateAVFleetGenerator  fleet = new TSPrivateAVFleetGenerator(scenario);  
		
	
		controler.addOverridingModule(DvrpModule.createModule(taxiCfg.getMode(),
				Collections.singleton(TaxiOptimizer.class)));
		
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(Fleet.class).annotatedWith(Taxi.class).toInstance(fleet);
				bind(Fleet.class).toInstance(fleet);
				
				addControlerListenerBinding().toInstance(fleet);	
				
				DvrpTravelDisutilityProvider.bindTravelDisutilityForOptimizer(binder(), Taxi.class);
				bind(PrivateAVScheduler.class).asEagerSingleton();
			

				bind(SubmittedTaxiRequestsCollector.class).toInstance(new SubmittedTaxiRequestsCollector());
				addControlerListenerBinding().to(SubmittedTaxiRequestsCollector.class);

				addControlerListenerBinding().to(TaxiSimulationConsistencyChecker.class);
				addControlerListenerBinding().to(TaxiStatsDumper.class);

				addRoutingModuleBinding(taxiCfg.getMode()).toInstance(
						new DynRoutingModule(taxiCfg.getMode()));

				if (taxiCfg.getTimeProfiles()) {
					addMobsimListenerBinding().toProvider(TaxiStatusTimeProfileCollectorProvider.class);
					// add more time profiles if necessary
				}
				
				bind(TaxiRequestValidator.class).to(DefaultTaxiRequestValidator.class);
			}
		});

		controler.run();
		
	}

}
