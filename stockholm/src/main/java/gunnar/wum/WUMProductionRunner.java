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
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.greedo.Greedo;
import org.matsim.contrib.greedo.GreedoConfigGroup;
import org.matsim.contrib.pseudosimulation.PSimConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfig;
import org.matsim.core.mobsim.qsim.components.StandardQSimComponentConfigurator;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.utils.CreatePseudoNetwork;
import org.matsim.roadpricing.RoadPricingConfigGroup;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleType;

import com.google.inject.Provides;

import ch.sbb.matsim.config.SBBTransitConfigGroup;
import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.mobsim.qsim.SBBTransitModule;
import ch.sbb.matsim.mobsim.qsim.pt.SBBTransitEngineQSimModule;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import org.matsim.vehicles.VehicleUtils;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class WUMProductionRunner {

	static final String temporaryPath = "/Users/GunnarF/NoBackup/data-workspace/wum/";
	// static final String archivePath = "/Users/GunnarF/OneDrive - VTI/My
	// Data/wum/";

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

	public static void removeModeInformation(final Scenario scenario) {
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

	static void runProductionScenario(final boolean runLocally, final boolean cleanInitialPlans) {

		final String configFileName;
		if (runLocally) {
			configFileName = FileUtils.getFile(temporaryPath, "production-scenario/config.xml").toString();
		} else {
			configFileName = "./config.xml";
		}

		final Config config = ConfigUtils.loadConfig(configFileName, new SwissRailRaptorConfigGroup(),
				new SBBTransitConfigGroup(), new RoadPricingConfigGroup(), new PSimConfigGroup());

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		if (runLocally) {
			config.controler().setLastIteration(100);
			config.plans()
					.setInputFile("/Users/GunnarF/NoBackup/data-workspace/wum/production-scenario/1PctAllModes.xml");
			config.controler()
					.setOutputDirectory("/Users/GunnarF/NoBackup/data-workspace/wum/production-scenario/output");
			config.transit().setTransitScheduleFile(
					"/Users/GunnarF/OneDrive - VTI/My Data/wum/data/output/transitSchedule_reduced.xml.gz");
			config.transit().setVehiclesFile(
					"/Users/GunnarF/OneDrive - VTI/My Data/wum/data/output/transitVehiclesDifferentiated.xml.gz");
		}

		final Greedo greedo;
		if (config.getModules().containsKey(GreedoConfigGroup.GROUP_NAME)) {
			Logger.getLogger(WUMProductionRunner.class).info("Using greedo.");
			config.addModule(new GreedoConfigGroup());
			greedo = new Greedo();
			greedo.meet(config);
		} else {
			Logger.getLogger(WUMProductionRunner.class).info("NOT using greedo.");
			greedo = null;
		}

		final Scenario scenario = ScenarioUtils.loadScenario(config);
		if (cleanInitialPlans) {
			removeModeInformation(scenario);
		}
		scaleTransitCapacities(scenario, config.qsim().getStorageCapFactor());

		if (greedo != null) {
			// Assumes all at this point existing network links to be capacitated.
			greedo.meet(scenario);
		}

		// Now add non-capacitated transit links.
		new CreatePseudoNetwork(scenario.getTransitSchedule(), scenario.getNetwork(), "tr_").createNetwork();

		final Controler controler = new Controler(scenario);

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.install(new SBBTransitModule());
				this.install(new SwissRailRaptorModule());
			}

			@Provides
			QSimComponentsConfig provideQSimComponentsConfig() {
				QSimComponentsConfig components = new QSimComponentsConfig();
				new StandardQSimComponentConfigurator(config).configure(components);
				SBBTransitEngineQSimModule.configure(components);
				return components;
			}
		});

		if (greedo != null) {
			controler.addOverridingModule(greedo);
		}

		controler.run();
	}

	public static void main(String[] args) {
		System.out.println("STARTED ...");
		final boolean runLocally = false;
		final boolean cleanInitialPlans = false;
		runProductionScenario(runLocally, cleanInitialPlans);
		System.out.println("... DONE");
	}

}
