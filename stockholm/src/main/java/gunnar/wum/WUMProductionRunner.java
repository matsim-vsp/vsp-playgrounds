/*
 * Copyright 2018 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.flotterod@gmail.com
 *
 */
package gunnar.wum;

import org.apache.commons.io.FileUtils;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.pseudosimulation.MobSimSwitcher;
import org.matsim.contrib.pseudosimulation.PSimConfigGroup;
import org.matsim.contrib.pseudosimulation.mobsim.PSimProvider;
import org.matsim.contrib.pseudosimulation.mobsim.SwitchingMobsimProvider;
import org.matsim.contrib.pseudosimulation.mobsim.transitperformance.TransitEmulator;
import org.matsim.contrib.pseudosimulation.replanning.PlanCatcher;
import org.matsim.contrib.pseudosimulation.searchacceleration.AccelerationConfigGroup;
import org.matsim.contrib.pseudosimulation.searchacceleration.AcceptIntendedReplanningStragetyProvider;
import org.matsim.contrib.pseudosimulation.searchacceleration.AcceptIntendedReplanningStrategy;
import org.matsim.contrib.pseudosimulation.searchacceleration.SearchAccelerator;
import org.matsim.contrib.pseudosimulation.searchacceleration.listeners.FifoTransitEmulator;
import org.matsim.contrib.pseudosimulation.searchacceleration.listeners.FifoTransitPerformance;
import org.matsim.contrib.pseudosimulation.trafficinfo.PSimTravelTimeCalculator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.mobsim.qsim.QSimProvider;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfig;
import org.matsim.core.mobsim.qsim.components.StandardQSimComponentConfigurator;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.utils.CreatePseudoNetwork;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleType;

import com.google.inject.Provides;
import com.google.inject.Singleton;

import ch.sbb.matsim.config.SBBTransitConfigGroup;
import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.mobsim.qsim.SBBTransitModule;
import ch.sbb.matsim.mobsim.qsim.pt.SBBTransitEngineQSimModule;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class WUMProductionRunner {

	static final String temporaryPath = "/Users/GunnarF/NoBackup/data-workspace/wum/";
	static final String archivePath = "/Users/GunnarF/OneDrive - VTI/My Data/wum/";

	static void scaleTransitCapacities(final Scenario scenario, final double factor) {
		for (VehicleType vehicleType : scenario.getTransitVehicles().getVehicleTypes().values()) {
			// vehicle capacities (in person units) get scaled DOWN
			final VehicleCapacity capacity = vehicleType.getCapacity();
			capacity.setSeats((int) Math.ceil(capacity.getSeats() * factor));
			capacity.setStandingRoom((int) Math.ceil(capacity.getStandingRoom() * factor));
			// access and egress times per person get scaled UP
			vehicleType.setAccessTime(vehicleType.getAccessTime() / factor);
			vehicleType.setEgressTime(vehicleType.getEgressTime() / factor);
			// PCU equivalents -- attempting to cause a failure if used
			vehicleType.setPcuEquivalents(Double.NaN);
		}
	}

	static void removeModeInformation(final Scenario scenario) {
		for (Person person : scenario.getPopulation().getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				for (PlanElement planElement : plan.getPlanElements()) {
					if (planElement instanceof Leg) {
						final Leg leg = (Leg) planElement;
						leg.setMode(TransportMode.car);
						leg.setRoute(null);
					}
				}
			}
		}
	}

	static void runProductionScenario() {

		final String configFileName = FileUtils.getFile(temporaryPath, "production-scenario/config.xml").toString();
		final String transitPrefix = "tr_";

		final Config config = ConfigUtils.loadConfig(configFileName, new SwissRailRaptorConfigGroup(),
				new SBBTransitConfigGroup(), new PSimConfigGroup(), new AccelerationConfigGroup());
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		// >>> for acceleration >>>
		final StrategySettings acceptIntendedReplanningStrategySettings = new StrategySettings();
		acceptIntendedReplanningStrategySettings.setStrategyName(AcceptIntendedReplanningStrategy.STRATEGY_NAME);
		acceptIntendedReplanningStrategySettings.setWeight(0.0); // changed dynamically
		config.strategy().addStrategySettings(acceptIntendedReplanningStrategySettings);
		// <<< for acceleration <<<

		// TODO EXPERIMENTAL
		ConfigUtils.addOrGetModule(config, PSimConfigGroup.class).setIterationsPerCycle(2);
		config.controler().setWriteEventsInterval(1);
		// TODO EXPERIMENTAL
		
		final Scenario scenario = ScenarioUtils.loadScenario(config);

		final Network network = scenario.getNetwork();
		TransitSchedule schedule = scenario.getTransitSchedule();
		new CreatePseudoNetwork(schedule, network, transitPrefix).createNetwork();

		removeModeInformation(scenario);

		scaleTransitCapacities(scenario, config.qsim().getStorageCapFactor());

		// >>> for acceleration >>>
		ConfigUtils.addOrGetModule(config, AccelerationConfigGroup.class).configure(scenario,
				ConfigUtils.addOrGetModule(config, PSimConfigGroup.class).getIterationsPerCycle());
		// <<< for acceleration <<<

		final Controler controler = new Controler(scenario);
		controler.addOverridingModule(new SwissRailRaptorModule());
		controler.addOverridingModule(new SBBTransitModule());

		// >>> for acceleration >>>
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.bind(QSimProvider.class);
			}

			@Provides
			QSimComponentsConfig provideQSimComponentsConfig(Config config) {
				QSimComponentsConfig components = new QSimComponentsConfig();
				new StandardQSimComponentConfigurator(config).configure(components);
				SBBTransitEngineQSimModule.configure(components);
				return components;
			}
		});
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				// General-purpose + car-specific PSim.
				// final PSimConfigGroup pSimConf = ConfigUtils.addOrGetModule(config, PSimConfigGroup.class);
				// final MobSimSwitcher mobSimSwitcher = new MobSimSwitcher(pSimConf, scenario);
				final MobSimSwitcher mobSimSwitcher = new MobSimSwitcher(scenario);
				this.addControlerListenerBinding().toInstance(mobSimSwitcher);
				this.bind(MobSimSwitcher.class).toInstance(mobSimSwitcher);
				this.bindMobsim().toProvider(SwitchingMobsimProvider.class);
				this.bind(TravelTimeCalculator.class).to(PSimTravelTimeCalculator.class);
				this.bind(TravelTime.class).toProvider(PSimTravelTimeCalculator.class);
				this.bind(PlanCatcher.class).toInstance(new PlanCatcher());
								
				// this.bind(PSimProvider.class).toInstance(new PSimProvider(scenario, controler.getEvents()));
				this.bind(PSimProvider.class);
				
				// Transit-specific PSim.
				final FifoTransitPerformance transitPerformance = new FifoTransitPerformance(mobSimSwitcher,
						scenario.getPopulation(), scenario.getTransitVehicles(), scenario.getTransitSchedule());
				this.bind(FifoTransitPerformance.class).toInstance(transitPerformance);
				this.addEventHandlerBinding().toInstance(transitPerformance);
				this.bind(TransitEmulator.class).to(FifoTransitEmulator.class);
				// Acceleration logic.
				this.bind(SearchAccelerator.class).in(Singleton.class);
				this.addControlerListenerBinding().to(SearchAccelerator.class);
				this.addEventHandlerBinding().to(SearchAccelerator.class);
				this.addPlanStrategyBinding(AcceptIntendedReplanningStrategy.STRATEGY_NAME)
						.toProvider(AcceptIntendedReplanningStragetyProvider.class);
			}
		});
		// <<< for acceleration <<<

		controler.run();
	}

	public static void main(String[] args) {
		System.out.println("STARTED ...");
		runProductionScenario();
		System.out.println("... DONE");
	}

}
