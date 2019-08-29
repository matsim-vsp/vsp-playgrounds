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
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.av.robotaxi.fares.taxi.TaxiFareModule;
import org.matsim.contrib.av.robotaxi.fares.taxi.TaxiFaresConfigGroup;
import org.matsim.contrib.dvrp.passenger.PassengerRequestValidator;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.contrib.taxi.run.MultiModeTaxiConfigGroup;
import org.matsim.contrib.taxi.run.MultiModeTaxiModule;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl.Builder;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.SubtourModeChoice;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.run.RunBerlinScenario;

import com.google.inject.Inject;
import com.google.inject.Provider;

import playground.ikaddoura.savPricing.runSetupA.prepare.BerlinNetworkModification;
import playground.ikaddoura.savPricing.runSetupA.prepare.PersonAttributesModification;

/**
 * This class starts a simulation run with taxis.
 *
 *  - The input taxi vehicles file specifies the number of vehicles and the vehicle capacity.
 * 	- The taxi service area is set to the the inner-city Berlin area (see input shape file).
 * 	- The private car mode is still allowed in the Berlin city area.
 * 	- Initial plans are not modified.
 *
 * @author ikaddoura
 */

public final class RunBerlinTaxiScenarioA {

	private static final Logger log = Logger.getLogger(RunBerlinTaxiScenarioA.class);

	private final StageActivityTypes stageActivities = new StageActivityTypesImpl("pt interaction", "car interaction", "ride interaction");
	public static final String taxiServiceAreaAttribute = "taxiServiceArea";
	public static final String modeToReplaceCarTripsInBrandenburg = TransportMode.car;
	private final String taxiNetworkMode = TransportMode.car;

	private final String serviceAreaShapeFile;

	private Config config;
	private Scenario scenario;
	private Controler controler;
	private RunBerlinScenario berlin;

	private boolean hasPreparedConfig = false ;
	private boolean hasPreparedScenario = false ;
	private boolean hasPreparedControler = false ;

	private double dailyRewardTaxiInsteadOfPrivateCar;

	public static void main(String[] args) {

		String configFileName ;
		String overridingConfigFileName;
		String serviceAreaShapeFile;
		double dailyRewardTaxiInsteadOfPrivateCar;

		if (args.length > 0) {
			throw new RuntimeException();

		} else {
			configFileName = "scenarios/berlin-v5.2-1pct/input/berlin-taxiA-v5.2-1pct.config.xml";
			overridingConfigFileName = null;
			serviceAreaShapeFile = "http://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/avoev/berlin-sav-v5.2-10pct/input/shp-inner-city-area/inner-city-area.shp";
			dailyRewardTaxiInsteadOfPrivateCar = 0.;
		}

		new RunBerlinTaxiScenarioA( configFileName, overridingConfigFileName, serviceAreaShapeFile, dailyRewardTaxiInsteadOfPrivateCar).run() ;
	}

	public RunBerlinTaxiScenarioA( String configFileName, String overridingConfigFileName, String serviceAreaShapeFile, double dailyRewardTaxiInsteadOfPrivateCar) {

		this.serviceAreaShapeFile = serviceAreaShapeFile;
		this.dailyRewardTaxiInsteadOfPrivateCar = dailyRewardTaxiInsteadOfPrivateCar;
		this.berlin = new RunBerlinScenario( configFileName, overridingConfigFileName );
	}

	public Controler prepareControler() {
		if ( !hasPreparedScenario ) {
			prepareScenario() ;
		}

		controler = berlin.prepareControler();

		// taxi + dvrp module
		controler.addOverridingModule(new MultiModeTaxiModule());
		controler.addOverridingModule(new DvrpModule());
		controler.configureQSimComponents(
				DvrpQSimComponents.activateModes(TaxiConfigGroup.get(controler.getConfig()).getMode()));

		// reject taxi requests outside the service area
		controler.addOverridingQSimModule(new AbstractDvrpModeQSimModule(TaxiConfigGroup.get(config).getMode()) {
			@Override
			protected void configureQSim() {
				bindModal(PassengerRequestValidator.class)
						.toInstance(new ServiceAreaRequestValidator(taxiServiceAreaAttribute));
			}
		});

		// taxi fares
        controler.addOverridingModule(new TaxiFareModule());

		if (dailyRewardTaxiInsteadOfPrivateCar != 0.) {
			// rewards for no longer owning a car
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					this.addEventHandlerBinding()
							.toInstance(new DailyRewardHandlerSAVInsteadOfCar(dailyRewardTaxiInsteadOfPrivateCar,
									modeToReplaceCarTripsInBrandenburg));

					SAVPassengerTrackerImpl tracker = new SAVPassengerTrackerImpl(TransportMode.taxi);
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

				final String[] availableModes = availableModesArrayList.toArray(new String[availableModesArrayList.size()]);

				addPlanStrategyBinding("SubtourModeChoice_no-potential-sav-user").toProvider(new Provider<PlanStrategy>() {

					@Inject
					Scenario sc;

					@Override
					public PlanStrategy get() {

						log.info("SubtourModeChoice_no-potential-sav-user" + " - available modes: " + availableModes.toString());
						final String[] chainBasedModes = {modeToReplaceCarTripsInBrandenburg, "bicycle"};

						final Builder builder = new Builder(new RandomPlanSelector<>());
						builder.addStrategyModule(new SubtourModeChoice(sc.getConfig()
								.global().getNumberOfThreads(), availableModes, chainBasedModes, false,
								0.5, tripRouterProvider));
						builder.addStrategyModule(new ReRoute(sc, tripRouterProvider));
						return builder.build();
					}
				});
			}
		});

		hasPreparedControler = true ;
		return controler;
	}

	public Scenario prepareScenario() {
		if ( !hasPreparedConfig ) {
			prepareConfig( ) ;
		}

		scenario = berlin.prepareScenario();

		BerlinShpUtils shpUtils = new BerlinShpUtils(serviceAreaShapeFile);
		new BerlinNetworkModification(shpUtils).addSAVmode(scenario, taxiNetworkMode, taxiServiceAreaAttribute);
		new BerlinPlansModificationTagFormerCarUsers().run(scenario);
		new PersonAttributesModification(shpUtils, stageActivities).run(scenario);

		hasPreparedScenario = true ;
		return scenario;
	}

	public Config prepareConfig(ConfigGroup... modulesToAdd) {

		List<ConfigGroup> drtModules = new ArrayList<>();
		drtModules.add(new DvrpConfigGroup());
		drtModules.add(new MultiModeTaxiConfigGroup());
		drtModules.add(new TaxiFaresConfigGroup());

		List<ConfigGroup> modules = new ArrayList<>();
		for (ConfigGroup module : drtModules) {
			modules.add(module);
		}
		for (ConfigGroup module : modulesToAdd) {
			modules.add(module);
		}

		ConfigGroup[] modulesArray = new ConfigGroup[modules.size()];
		config = berlin.prepareConfig(modules.toArray(modulesArray));

		//no special adjustments (in contrast to Drt)
//		config.addConfigConsistencyChecker(new TaxiConfigConsistencyChecker());
		config.checkConsistency();

		hasPreparedConfig = true ;
		return config ;
	}

	public void run() {
		if ( !hasPreparedControler ) {
			prepareControler() ;
		}
		controler.run();
		log.info("Done.");
	}

}

