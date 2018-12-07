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
package org.matsim.contrib.pseudosimulation.searchacceleration;

import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.pseudosimulation.MobSimSwitcher;
import org.matsim.contrib.pseudosimulation.PSimConfigGroup;
import org.matsim.contrib.pseudosimulation.PSimTravelTimeCalculator;
import org.matsim.contrib.pseudosimulation.SwitchingMobsimProvider;
import org.matsim.contrib.pseudosimulation.searchacceleration.utils.PTCapacityAdjusmentPerSample;
import org.matsim.contrib.pseudosimulation.transit.NoTransitEmulator;
import org.matsim.contrib.pseudosimulation.transit.TransitEmulator;
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

import com.google.inject.Provides;
import com.google.inject.Singleton;

import ch.sbb.matsim.mobsim.qsim.SBBTransitModule;
import ch.sbb.matsim.mobsim.qsim.pt.SBBTransitEngineQSimModule;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class Greedo extends AbstractModule {

	// -------------------- CONSTANTS --------------------

	private static final Logger log = Logger.getLogger(Greedo.class);

	private final int defaultIterationsPerCycle = 10;

	// -------------------- MEMBERS --------------------

	private final Set<String> bestResponseInnovationStrategyNames = new LinkedHashSet<>();

	private final Set<String> randomInnovationStrategyNames = new LinkedHashSet<>();

	private boolean adjustStrategyWeights = true;

	private Config config = null;

	private Scenario scenario = null;

	// private Controler controler = null;

	private GreedoProgressListener greedoProgressListener = new GreedoProgressListener() {
	};

	// -------------------- CONSTRUCTION --------------------

	public Greedo() {

		// Strategies that imply a costly best-response (e.g. re-route) calculation.
		this.setBestResponseStrategyName("ReRoute");
		this.setBestResponseStrategyName("TimeAllocationMutator_ReRoute");
		this.setBestResponseStrategyName("ChangeLegMode");
		this.setBestResponseStrategyName("ChangeTripMode");
		this.setBestResponseStrategyName("ChangeSingleLegMode");
		this.setBestResponseStrategyName("SubtoutModeChoice");

		// Strategies that are cheap to calculate.
		this.setRandomInnovationStrategyName("TimeAllocationMutator");
	}

	// -------------------- CONFIGURATION --------------------

	public void setBestResponseStrategyName(String strategyName) {
		this.bestResponseInnovationStrategyNames.add(strategyName);
	}

	public void setRandomInnovationStrategyName(String strategyName) {
		this.randomInnovationStrategyNames.add(strategyName);
	}

	public void setAdjustStrategyWeights(boolean adjustStrategyWeights) {
		this.adjustStrategyWeights = adjustStrategyWeights;
	}

	public void setGreedoProgressListener(GreedoProgressListener greedoProgressListener) {
		this.greedoProgressListener = greedoProgressListener;
	}

	// -------------------- WIRE GREEDO INTO MATSim --------------------

	public void meet(final Config config) {

		if (this.config != null) {
			throw new RuntimeException("Have already met a config.");
		}
		this.config = config;

		/*
		 * Ensure that the simulation starts at iteration 0. This should not be
		 * necessary but is currently assumed when handling iteration-dependent
		 * re-planning rates.
		 */
		config.controler()
				.setLastIteration(config.controler().getLastIteration() - config.controler().getFirstIteration());
		config.controler().setFirstIteration(0);

		/*
		 * Use the event manager that does not check for event order. Essential for
		 * PSim, which generates events person-after-person.
		 */
		config.parallelEventHandling().setSynchronizeOnSimSteps(false);
		// config.parallelEventHandling().setNumberOfThreads(1); // FIXME Why was this ever set to one?

		/*
		 * Preliminary analysis of innovation strategies.
		 */
		int bestResponseStrategyCnt = 0;
		int randomInnovationStrategyCnt = 0;
		double randomInnovationStrategyWeightSum = 0.0;
		for (StrategySettings strategySettings : config.strategy().getStrategySettings()) {
			final String strategyName = strategySettings.getStrategyName();
			if (strategySettings.getWeight() > 0) {
				if (this.bestResponseInnovationStrategyNames.contains(strategyName)) {
					bestResponseStrategyCnt++;
				} else if (this.randomInnovationStrategyNames.contains(strategyName)) {
					randomInnovationStrategyCnt++;
					randomInnovationStrategyWeightSum += strategySettings.getWeight();
				}
			}
		}

		/*
		 * Ensure a valid PSim configuration; fall back to default values if not
		 * available.
		 */
		final boolean pSimConfigExists = config.getModules().containsKey(PSimConfigGroup.GROUP_NAME);
		final PSimConfigGroup pSimConf = ConfigUtils.addOrGetModule(config, PSimConfigGroup.class);
		if (!pSimConfigExists) {
			// Adjust number of PSim iterations to number and type of innovation strategies.
			if (randomInnovationStrategyCnt > 0) {
				// This case is difficult to configure automatically. Make sure that every
				// strategy is used on average at least once.
				pSimConf.setIterationsPerCycle(Math.max(bestResponseStrategyCnt + randomInnovationStrategyCnt,
						this.defaultIterationsPerCycle));
			} else {
				if (bestResponseStrategyCnt > 0) {
					// Only best-response strategies: every best-response strategy is used on
					// average exactly once.
					pSimConf.setIterationsPerCycle(bestResponseStrategyCnt);
				} else {
					// No innovation strategies at all; this can only be for testing. Since PSim has
					// no effect, use minimum number of pSim iterations (1 pSim + 1 real per cycle).
					pSimConf.setIterationsPerCycle(2);
				}
			}
		}

		// Adjust iteration numbers to pSim iteration overhead.
		config.controler().setLastIteration(config.controler().getLastIteration() * pSimConf.getIterationsPerCycle());
		config.controler()
				.setWriteEventsInterval(config.controler().getWriteEventsInterval() * pSimConf.getIterationsPerCycle());
		config.controler()
				.setWritePlansInterval(config.controler().getWritePlansInterval() * pSimConf.getIterationsPerCycle());
		config.controler().setWriteSnapshotsInterval(
				config.controler().getWriteSnapshotsInterval() * pSimConf.getIterationsPerCycle());
		// TODO: Deprecate this, allow for SBB transit only:
		pSimConf.setFullTransitPerformanceTransmission(false);

		/*
		 * Ensure a valid acceleration configuration; fall back to default values if not
		 * available.
		 */
		ConfigUtils.addOrGetModule(config, AccelerationConfigGroup.class);

		/*
		 * Use minimal choice set and always remove the worse plan. This probably as
		 * close as it can get to best-response in the presence of random innovation
		 * strategies.
		 */
		config.strategy().setMaxAgentPlanMemorySize(1);
		config.strategy().setPlanSelectorForRemoval("WorstPlanSelector");

		/*
		 * Keep only plan innovation strategies. Re-weight for maximum pSim efficiency.
		 * 
		 */
		if (this.adjustStrategyWeights) {
			final double individualBestResponseStrategyProba = 1.0 / pSimConf.getIterationsPerCycle();
			final double bestResponseStrategyProbaSum = individualBestResponseStrategyProba * bestResponseStrategyCnt;
			final double randomInnovationStrategyProbaSum = 1.0 - bestResponseStrategyProbaSum;
			final double randomInnovationStrategyWeightFactor = randomInnovationStrategyProbaSum
					/ randomInnovationStrategyWeightSum;
			double sum = 0;
			for (StrategySettings strategySettings : config.strategy().getStrategySettings()) {
				final String strategyName = strategySettings.getStrategyName();
				if (this.bestResponseInnovationStrategyNames.contains(strategyName)) {
					strategySettings.setWeight(individualBestResponseStrategyProba);
				} else if (this.randomInnovationStrategyNames.contains(strategyName)) {
					strategySettings.setWeight(randomInnovationStrategyWeightFactor * strategySettings.getWeight());
				} else {
					strategySettings.setWeight(0.0); // i.e., dismiss
				}
				sum += strategySettings.getWeight();
			}
			if (Math.abs(1.0 - sum) >= 1e-8) {
				throw new RuntimeException("The sum of all strategy probabilities is " + sum + ".");
			}
		}

		/*
		 * Add a strategy that decides which of the better-response re-planning
		 * decisions coming out of the pSim is allowed to be implemented.
		 */
		final StrategySettings acceptIntendedReplanningStrategySettings = new StrategySettings();
		acceptIntendedReplanningStrategySettings.setStrategyName(AcceptIntendedReplanningStrategy.STRATEGY_NAME);
		acceptIntendedReplanningStrategySettings.setWeight(0.0); // changed dynamically
		config.strategy().addStrategySettings(acceptIntendedReplanningStrategySettings);
	}

	public void meet(final Scenario scenario) {

		if (this.config == null) {
			throw new RuntimeException("First meet the config.");
		} else if (this.scenario != null) {
			throw new RuntimeException("Have already met the scenario.");
		}
		this.scenario = scenario;

		ConfigUtils.addOrGetModule(this.config, AccelerationConfigGroup.class).configure(this.scenario,
				ConfigUtils.addOrGetModule(this.config, PSimConfigGroup.class).getIterationsPerCycle());
	}

	// @Override
	// public void meet(final Controler controler) {
	//
	// if (this.scenario == null) {
	// throw new RuntimeException("First meet the scenario.");
	// } else if (this.controler != null) {
	// throw new RuntimeException("Have already met the controler.");
	// }
	// this.controler = controler;
	//
	// controler.addOverridingModule(this);
	// }

	// -------------------- Overriding of AbstractModule --------------------

	@Override
	public void install() {

		// if (this.controler == null) {
		// throw new RuntimeException("First meet the controler.");
		// }

		// General-purpose + car-specific PSim.
		// final PSimConfigGroup pSimConf = ConfigUtils.addOrGetModule(this.config,
		// PSimConfigGroup.class);
		// final MobSimSwitcher mobSimSwitcher = new MobSimSwitcher(pSimConf,
		// this.scenario);

		// final MobSimSwitcher mobSimSwitcher = new MobSimSwitcher();
		// this.addControlerListenerBinding().toInstance(mobSimSwitcher);
		// this.bind(MobSimSwitcher.class).toInstance(mobSimSwitcher);
		// this.bindMobsim().toProvider(SwitchingMobsimProvider.class);

		this.bind(MobSimSwitcher.class);
		this.addControlerListenerBinding().to(MobSimSwitcher.class);
		this.bindMobsim().toProvider(SwitchingMobsimProvider.class);
		this.bind(TravelTimeCalculator.class).to(PSimTravelTimeCalculator.class);
		this.bind(TravelTime.class).toProvider(PSimTravelTimeCalculator.class);

		// this.bind(PlanCatcher.class).toInstance(new PlanCatcher());

		// this.bind(PSimProvider.class).toInstance(new PSimProvider(this.scenario,
		// this.controler.getEvents()));

		// Transit-specific PSim. TODO Allow only for SBB transit.
		// if (this.config.transit().isUseTransit()) {
		// final FifoTransitPerformance transitPerformance = new
		// FifoTransitPerformance(mobSimSwitcher,
		// this.scenario.getPopulation(), this.scenario.getTransitVehicles(),
		// this.scenario.getTransitSchedule());
		// this.bind(FifoTransitPerformance.class).toInstance(transitPerformance);
		// this.addEventHandlerBinding().toInstance(transitPerformance);
		// this.bind(TransitEmulator.class).to(FifoTransitEmulator.class);
		// } else {
		this.bind(TransitEmulator.class).to(NoTransitEmulator.class);
		// }

		this.bind(QSimProvider.class);

		// Acceleration logic.
		this.bind(GreedoProgressListener.class).toInstance(this.greedoProgressListener);
		this.bind(SearchAccelerator.class).in(Singleton.class);
		this.addControlerListenerBinding().to(SearchAccelerator.class);
		this.addEventHandlerBinding().to(SearchAccelerator.class);
		this.addPlanStrategyBinding(AcceptIntendedReplanningStrategy.STRATEGY_NAME)
				.toProvider(AcceptIntendedReplanningStragetyProvider.class);
	}

	@Provides
	QSimComponentsConfig provideQSimComponentsConfig() {
		QSimComponentsConfig components = new QSimComponentsConfig();
		new StandardQSimComponentConfigurator(this.config).configure(components);
		if (this.config.transit().isUseTransit()) {
			SBBTransitEngineQSimModule.configure(components);
		}
		return components;
	}

	// =========================================================================
	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------
	// =========================================================================

	public static void main(String[] args) {

		Greedo greedo = new Greedo();
		final boolean excludeAllButPT = false;

		// STEP 1: The Config.

		Config config = ConfigUtils
				.loadConfig("/Users/GunnarF/NoBackup/data-workspace/wum/production-scenario/config.xml");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		// FIXME QSim misinterprets default time values.
		if (greedo != null) {
			greedo.meet(config);
		}

		// STEP 2: The Scenario.

		Scenario scenario = ScenarioUtils.loadScenario(config);

		Network network = scenario.getNetwork();
		TransitSchedule schedule = scenario.getTransitSchedule();
		new CreatePseudoNetwork(schedule, network, "tr_").createNetwork();

		if (excludeAllButPT) {
			final Set<Id<Person>> remove = new LinkedHashSet<>();
			for (Person person : scenario.getPopulation().getPersons().values()) {
				for (PlanElement planEl : person.getSelectedPlan().getPlanElements()) {
					if (planEl instanceof Leg && !"pt".equals(((Leg) planEl).getMode())) {
						remove.add(person.getId());
					}
				}
			}
			log.info("before pt filter: " + scenario.getPopulation().getPersons().size());
			for (Id<Person> removeId : remove) {
				scenario.getPopulation().getPersons().remove(removeId);
			}
			log.info("after pt filter: " + scenario.getPopulation().getPersons().size());
		}

		if (greedo != null) {
			greedo.meet(scenario);
		}

		// STEP 3: The Controler.

		Controler controler = new Controler(scenario);

		controler.addOverridingModule(new SwissRailRaptorModule());
		controler.addOverridingModule(new SBBTransitModule());
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.bind(QSimProvider.class);
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
			// greedo.meet(controler);
		}

		double samplesize = config.qsim().getStorageCapFactor();
		PTCapacityAdjusmentPerSample capadjuster = new PTCapacityAdjusmentPerSample();
		capadjuster.adjustStoarageAndFlowCapacity(scenario, samplesize);

		controler.run();

		System.out.println("... DONE");
	}
}
