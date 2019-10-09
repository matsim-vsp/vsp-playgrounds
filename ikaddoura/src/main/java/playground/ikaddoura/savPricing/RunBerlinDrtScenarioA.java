/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.ikaddoura.savPricing;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.analysis.ScoreStats;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.av.robotaxi.fares.drt.DrtFareModule;
import org.matsim.contrib.av.robotaxi.fares.drt.DrtFaresConfigGroup;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtModule;
import org.matsim.contrib.dvrp.passenger.PassengerRequestValidator;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl.Builder;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.SubtourModeChoice;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.TripRouter;
import org.matsim.run.RunBerlinScenario;

import com.google.inject.Inject;
import com.google.inject.Provider;

import playground.ikaddoura.savPricing.runSetupA.prepare.BerlinNetworkModification;
import playground.ikaddoura.savPricing.runSetupA.prepare.PersonAttributesModification;

/**
 * This class starts a simulation run with DRT.
 * <p>
 * - The input DRT vehicles file specifies the number of vehicles and the vehicle capacity (a vehicle capacity of 1 means there is no ride-sharing).
 * - The DRT service area is set to the the inner-city Berlin area (see input shape file).
 * - The private car mode is still allowed in the Berlin city area.
 * - Initial plans are not modified.
 *
 * @author ikaddoura
 */

public final class RunBerlinDrtScenarioA {

	private static final Logger log = Logger.getLogger(RunBerlinDrtScenarioA.class);

	public static final String drtServiceAreaAttribute = "drtServiceArea";
	public static final String modeToReplaceCarTripsInBrandenburg = TransportMode.car;
	private final String taxiNetworkMode = TransportMode.car;

	private final String drtServiceAreaShapeFile;

	private Config config;
	private Scenario scenario;
	private Controler controler;
	private RunBerlinScenario berlin;

	private boolean hasPreparedConfig = false;
	private boolean hasPreparedScenario = false;
	private boolean hasPreparedControler = false;

	private double dailyRewardDrtInsteadOfPrivateCar;

	public static void main(String[] args) {

		String configFileName;
		String overridingConfigFileName;
		String drtServiceAreaShapeFile;
		double dailyRewardDrtInsteadOfPrivateCar;

		if (args.length == 4) {
			configFileName = args[0];
			if (args[1].equals("") || args[1] == null || args[1].equals("null")) {
				overridingConfigFileName = null;
			} else {
				overridingConfigFileName = args[1];
			}
			drtServiceAreaShapeFile = args[2];
			dailyRewardDrtInsteadOfPrivateCar = Double.parseDouble(args[3]);

		} else {
			configFileName = "scenarios/berlin-v5.2-1pct/input/berlin-drtA-v5.2-1pct-Berlkoenig.config.xml";
			overridingConfigFileName = null;
			drtServiceAreaShapeFile = "http://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/avoev/berlin-sav-v5.2-10pct/input/shp-berlkoenig-area/berlkoenig-area.shp";
			dailyRewardDrtInsteadOfPrivateCar = 0.;
		}

		new RunBerlinDrtScenarioA(configFileName, overridingConfigFileName, drtServiceAreaShapeFile,
				dailyRewardDrtInsteadOfPrivateCar).run();
	}

	public RunBerlinDrtScenarioA(String configFileName, String overridingConfigFileName, String drtServiceAreaShapeFile,
			double dailyRewardDrtInsteadOfPrivateCar) {
		this.drtServiceAreaShapeFile = drtServiceAreaShapeFile;
		this.dailyRewardDrtInsteadOfPrivateCar = dailyRewardDrtInsteadOfPrivateCar;
		this.berlin = new RunBerlinScenario(configFileName, overridingConfigFileName);
	}

	public Controler prepareControler() {
		if (!hasPreparedScenario) {
			prepareScenario();
		}

		controler = berlin.prepareControler();

		// drt + dvrp module
		controler.addOverridingModule(new MultiModeDrtModule());
		controler.addOverridingModule(new DvrpModule());
		controler.configureQSimComponents(DvrpQSimComponents.activateModes(
				DrtConfigGroup.getSingleModeDrtConfig(controler.getConfig()).getMode()));

		// reject drt requests outside the service area
		controler.addOverridingQSimModule(
				new AbstractDvrpModeQSimModule(DrtConfigGroup.getSingleModeDrtConfig(config).getMode()) {
			@Override
			protected void configureQSim() {
				bindModal(PassengerRequestValidator.class).toInstance(
						new ServiceAreaRequestValidator(drtServiceAreaAttribute));
			}
		});

		// Add drt-specific fare module
		controler.addOverridingModule(new DrtFareModule());

		if (dailyRewardDrtInsteadOfPrivateCar != 0.) {
			// rewards for no longer owning a car
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {

					this.addEventHandlerBinding()
							.toInstance(new DailyRewardHandlerSAVInsteadOfCar(dailyRewardDrtInsteadOfPrivateCar,
									modeToReplaceCarTripsInBrandenburg));

					SAVPassengerTrackerImpl tracker = new SAVPassengerTrackerImpl(TransportMode.drt);
					this.bind(SAVPassengerTracker.class).toInstance(tracker);
					this.addEventHandlerBinding().toInstance(tracker);
				}
			});
		}

		// different modes for different subpopulations
		controler.addOverridingModule(new AbstractModule() {

			@Override
			public void install() {

				final Provider<TripRouter> tripRouterProvider = binder().getProvider(TripRouter.class);

				List<String> availableModesArrayList = new ArrayList<>();
				availableModesArrayList.add("bicycle");
				availableModesArrayList.add("pt");
				availableModesArrayList.add("walk");
				availableModesArrayList.add(modeToReplaceCarTripsInBrandenburg);

				final String[] availableModes = availableModesArrayList.toArray(
						new String[availableModesArrayList.size()]);

				addPlanStrategyBinding("SubtourModeChoice_no-potential-sav-user").toProvider(
						new Provider<PlanStrategy>() {

							@Inject
							Scenario sc;

							@Override
							public PlanStrategy get() {

								log.info("SubtourModeChoice_no-potential-sav-user"
										+ " - available modes: "
										+ availableModes.toString());
								final String[] chainBasedModes = { modeToReplaceCarTripsInBrandenburg, "bicycle" };

								final Builder builder = new Builder(new RandomPlanSelector<>());
								builder.addStrategyModule(
										new SubtourModeChoice(sc.getConfig().global().getNumberOfThreads(),
												availableModes, chainBasedModes, false, 0.5, tripRouterProvider));
								builder.addStrategyModule(new ReRoute(sc, tripRouterProvider));
								return builder.build();
							}
						});
			}
		});

		hasPreparedControler = true;
		return controler;
	}

	public Scenario prepareScenario() {
		if (!hasPreparedConfig) {
			prepareConfig();
		}

		scenario = berlin.prepareScenario();

		RouteFactories routeFactories = scenario.getPopulation().getFactory().getRouteFactories();
		routeFactories.setRouteFactory(DrtRoute.class, new DrtRouteFactory());

		BerlinShpUtils shpUtils = new BerlinShpUtils(drtServiceAreaShapeFile);
		new BerlinNetworkModification(shpUtils).addSAVmode(scenario, taxiNetworkMode, drtServiceAreaAttribute);
		new BerlinPlansModificationTagFormerCarUsers().run(scenario);
		new PersonAttributesModification(shpUtils).run(scenario);

		hasPreparedScenario = true;
		return scenario;
	}

	public Config prepareConfig(ConfigGroup... modulesToAdd) {

		// dvrp, drt config groups
		List<ConfigGroup> drtModules = new ArrayList<>();
		drtModules.add(new DvrpConfigGroup());
		drtModules.add(new MultiModeDrtConfigGroup());
		drtModules.add(new DrtFaresConfigGroup());

		List<ConfigGroup> modules = new ArrayList<>();
		for (ConfigGroup module : drtModules) {
			modules.add(module);
		}
		for (ConfigGroup module : modulesToAdd) {
			modules.add(module);
		}

		ConfigGroup[] modulesArray = new ConfigGroup[modules.size()];
		config = berlin.prepareConfig(modules.toArray(modulesArray));

		DrtConfigs.adjustDrtConfig(DrtConfigGroup.getSingleModeDrtConfig(config), config.planCalcScore());

		hasPreparedConfig = true;
		return config;
	}

	public void run() {
		if (!hasPreparedControler) {
			prepareControler();
		}
		controler.run();
		log.info("Done.");
	}

	// add a getScore method
	public ScoreStats getScoreStats() {
		return controler.getScoreStats();
	}

}

