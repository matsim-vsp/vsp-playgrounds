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

package playground.jbischoff.avparking;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.passenger.PassengerEnginePlugin;
import org.matsim.contrib.dvrp.passenger.PassengerRequestCreator;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic.DynActionCreator;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentQueryHelper;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentSourcePlugin;
import org.matsim.contrib.dynagent.run.DynActivityEnginePlugin;
import org.matsim.contrib.dynagent.run.DynRoutingModule;
import org.matsim.contrib.parking.parkingsearch.ParkingUtils;
import org.matsim.contrib.parking.parkingsearch.evaluation.ParkingListener;
import org.matsim.contrib.parking.parkingsearch.manager.FacilityBasedParkingManager;
import org.matsim.contrib.parking.parkingsearch.manager.ParkingSearchManager;
import org.matsim.contrib.parking.parkingsearch.manager.WalkLegFactory;
import org.matsim.contrib.parking.parkingsearch.manager.vehicleteleportationlogic.VehicleTeleportationLogic;
import org.matsim.contrib.parking.parkingsearch.manager.vehicleteleportationlogic.VehicleTeleportationToNearbyParking;
import org.matsim.contrib.parking.parkingsearch.routing.ParkingRouter;
import org.matsim.contrib.parking.parkingsearch.routing.WithinDayParkingRouter;
import org.matsim.contrib.parking.parkingsearch.sim.ParkingSearchPopulationPlugin;
import org.matsim.contrib.parking.parkingsearch.sim.ParkingSearchPrepareForSimImpl;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.PrepareForSim;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.TeleportationPlugin;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsPlugin;
import org.matsim.core.mobsim.qsim.messagequeueengine.MessageQueuePlugin;
import org.matsim.core.mobsim.qsim.pt.TransitEnginePlugin;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEnginePlugin;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.vis.otfvis.OnTheFlyServer.NonPlanAgentQueryHelper;

import com.google.inject.Inject;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

public final class DvrpParkingModule extends AbstractModule {
	public static final String DVRP_ROUTING = "dvrp_routing";// TODO ==> dvrp_optimizer???

	@Inject
	private DvrpConfigGroup dvrpCfg;

	private final Module module;
	private final List<Class<? extends MobsimListener>> listeners;

	@SuppressWarnings("unchecked")
	public DvrpParkingModule(final Class<? extends VrpOptimizer> vrpOptimizerClass,
			final Class<? extends PassengerRequestCreator> passengerRequestCreatorClass,
			final Class<? extends DynActionCreator> dynActionCreatorClass) {

		module = new com.google.inject.AbstractModule() {
			@Override
			protected void configure() {
				bind(VrpOptimizer.class).to(vrpOptimizerClass).asEagerSingleton();
				bind(PassengerRequestCreator.class).to(passengerRequestCreatorClass).asEagerSingleton();
				bind(DynActionCreator.class).to(dynActionCreatorClass).asEagerSingleton();
			}
		};

		listeners = new ArrayList<>();
		if (MobsimListener.class.isAssignableFrom(vrpOptimizerClass)) {
			listeners.add((Class<? extends MobsimListener>)vrpOptimizerClass);
		}
	}

	@SafeVarargs
	public DvrpParkingModule(Module module, Class<? extends MobsimListener>... listeners) {
		this.module = module;
		this.listeners = Arrays.asList(listeners);
	}

	@Override
	public void install() {
		
		final DynRoutingModule routingModuleCar = new DynRoutingModule(TransportMode.car);
		StageActivityTypes stageActivityTypesCar = new StageActivityTypes() {
			@Override
			public boolean isStageActivity(String activityType) {

				return (activityType.equals(ParkingUtils.PARKACTIVITYTYPE));
			}
		};
		routingModuleCar.setStageActivityTypes(stageActivityTypesCar);
		addRoutingModuleBinding(TransportMode.car).toInstance(routingModuleCar);

		String mode = dvrpCfg.getMode();
		addRoutingModuleBinding(mode).toInstance(new DynRoutingModule(mode));
		bind(ParkingSearchManager.class).to(FacilityBasedParkingManager.class).asEagerSingleton();
		bind(WalkLegFactory.class).asEagerSingleton();
		bind(PrepareForSim.class).to(ParkingSearchPrepareForSimImpl.class);
		addControlerListenerBinding().to(ParkingListener.class);
		bind(ParkingRouter.class).to(WithinDayParkingRouter.class);
		bind(VehicleTeleportationLogic.class).to(VehicleTeleportationToNearbyParking.class);


		// Visualisation of schedules for DVRP DynAgents
		bind(NonPlanAgentQueryHelper.class).to(VrpAgentQueryHelper.class);

		// VrpTravelTimeEstimator
		install(new DvrpTravelTimeModule());
	}

	@Provides
	@Singleton
	@Named(DvrpParkingModule.DVRP_ROUTING)
	private Network provideDvrpRoutingNetwork(Network network, DvrpConfigGroup dvrpCfg) {
		if (dvrpCfg.getNetworkMode() == null) { // no mode filtering
			return network;
		}
		
		Network dvrpNetwork = NetworkUtils.createNetwork();
		new TransportModeNetworkFilter(network).filter(dvrpNetwork, Collections.singleton(dvrpCfg.getNetworkMode()));
		return dvrpNetwork;
	}

	@Provides
	private Collection<AbstractQSimPlugin> provideQSimPlugins(Config config) {
		final Collection<AbstractQSimPlugin> plugins = new ArrayList<>();
		plugins.add(new MessageQueuePlugin(config));
		plugins.add(new DynActivityEnginePlugin(config));
		plugins.add(new QNetsimEnginePlugin(config));
		if (config.network().isTimeVariantNetwork()) {
			plugins.add(new NetworkChangeEventsPlugin(config));
		}
		if (config.transit().isUseTransit()) {
			plugins.add(new TransitEnginePlugin(config));
		}
		plugins.add(new TeleportationPlugin(config));

		plugins.add(new ParkingSearchPopulationPlugin(config));
		plugins.add(new PassengerEnginePlugin(config, dvrpCfg.getMode()));
		plugins.add(new VrpAgentSourcePlugin(config));
		plugins.add(new QSimPlugin(config));
		return plugins;
	}

	private class QSimPlugin extends AbstractQSimPlugin {
		public QSimPlugin(Config config) {
			super(config);
		}

		@Override
		public Collection<? extends Module> modules() {
			Collection<Module> result = new ArrayList<>();
			result.add(module);
			return result;
		}

		@Override
		public Collection<Class<? extends MobsimListener>> listeners() {
			Collection<Class<? extends MobsimListener>> result = new ArrayList<>();
			for (Class<? extends MobsimListener> l : listeners) {
				result.add(l);
			}
			return result;
		}
	}
}
