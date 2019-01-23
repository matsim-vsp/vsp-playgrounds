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
package org.matsim.contrib.greedo;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.pseudosimulation.MobSimSwitcher;
import org.matsim.contrib.pseudosimulation.PSimConfigGroup;
import org.matsim.contrib.pseudosimulation.PSimTravelTimeCalculator;
import org.matsim.contrib.pseudosimulation.SwitchingMobsimProvider;
import org.matsim.contrib.pseudosimulation.transit.FifoTransitEmulator;
import org.matsim.contrib.pseudosimulation.transit.FifoTransitPerformance;
import org.matsim.contrib.pseudosimulation.transit.NoTransitEmulator;
import org.matsim.contrib.pseudosimulation.transit.TransitEmulator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.qsim.QSimProvider;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;

import com.google.inject.Singleton;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class Greedo extends AbstractModule {

	// -------------------- CONSTANTS --------------------

	private static final Logger log = Logger.getLogger(Greedo.class);

	// -------------------- MEMBERS --------------------

	private Config config = null;

	private Scenario scenario = null;

	private GreedoProgressListener greedoProgressListener = new GreedoProgressListener() {
	};

	// -------------------- CONSTRUCTION --------------------

	public Greedo() {
	}

	// -------------------- WIRE GREEDO INTO MATSim --------------------

	public void meet(final Config config) {

		if (this.config != null) {
			throw new RuntimeException("Have already met a config.");
		}
		this.config = config;

		/*
		 * Ensure a valid acceleration configuration; fall back to default values if not
		 * available.
		 */
		if (!config.getModules().containsKey(GreedoConfigGroup.GROUP_NAME)) {
			log.warn("Config module " + GreedoConfigGroup.GROUP_NAME + " is missing, falling back to default values.");
		}
		final GreedoConfigGroup accelerationConfig = ConfigUtils.addOrGetModule(config, GreedoConfigGroup.class);

		/*
		 * Ensure that the simulation starts at iteration 0. One could relax at the cost
		 * of somewhat messier code.
		 */
		if (config.controler().getFirstIteration() != 0) {
			config.controler()
					.setLastIteration(config.controler().getLastIteration() - config.controler().getFirstIteration());
			config.controler().setFirstIteration(0);
			log.warn("Setting firstIteration = " + config.controler().getFirstIteration());
			log.warn("Setting lastIteration = " + config.controler().getLastIteration());
		}

		/*
		 * Use the event manager that does not check for event order. Essential for
		 * PSim, which generates events person-after-person.
		 * 
		 * TODO The original pSim code also called
		 * config.parallelEventHandling().setNumberOfThreads(1), which seems to be a
		 * performance bottleneck. Why would it be necessary? Gunnar 2019-01-23.
		 */
		config.parallelEventHandling().setSynchronizeOnSimSteps(false);

		/*
		 * Preliminary analysis of innovation strategies.
		 */
		int expensiveStrategyCnt = 0;
		int cheapStrategyCnt = 0;
		double cheapStrategyWeightSum = 0.0;
		for (StrategySettings strategySettings : config.strategy().getStrategySettings()) {
			final String strategyName = strategySettings.getStrategyName();
			if (strategySettings.getWeight() > 0) {
				if (accelerationConfig.getExpensiveStrategyList().contains(strategyName)) {
					expensiveStrategyCnt++;
				} else if (accelerationConfig.getCheapStrategyList().contains(strategyName)) {
					cheapStrategyCnt++;
					cheapStrategyWeightSum += strategySettings.getWeight();
				} else {
					log.warn("Dismissing unknown strategy: " + strategyName);
				}
			}
		}

		/*
		 * Ensure a valid PSim configuration; fall back to default values if not
		 * available.
		 */
		if (!config.getModules().containsKey(PSimConfigGroup.GROUP_NAME)) {
			log.warn("Config module " + PSimConfigGroup.GROUP_NAME + " is missing, falling back to default values.");
		}
		final PSimConfigGroup pSimConf = ConfigUtils.addOrGetModule(config, PSimConfigGroup.class);

		/*
		 * Adjust number of PSim iterations per cycle to number and type of innovation
		 * strategies.
		 */
		{
			final int originalIterationsPerCycle = pSimConf.getIterationsPerCycle();
			if (cheapStrategyCnt > 0) {
				// Make sure that every strategy can be used used on average at least once.
				pSimConf.setIterationsPerCycle(
						Math.max(expensiveStrategyCnt + cheapStrategyCnt, originalIterationsPerCycle));
			} else {
				if (expensiveStrategyCnt > 0) {
					// Only best-response strategies: every best-response strategy is used on
					// average exactly once.
					pSimConf.setIterationsPerCycle(expensiveStrategyCnt);
				} else {
					// No innovation strategies at all!
					log.warn("No relevant strategies recognized.");
				}
			}
			if (pSimConf.getIterationsPerCycle() != originalIterationsPerCycle) {
				log.warn("Adjusted number of pSim iterations per cycle from " + originalIterationsPerCycle + " to "
						+ pSimConf.getIterationsPerCycle() + ".");
			}
		}

		/*
		 * Adjust iteration numbers to pSim iteration overhead.
		 */
		config.controler().setLastIteration(config.controler().getLastIteration() * pSimConf.getIterationsPerCycle());
		config.controler()
				.setWriteEventsInterval(config.controler().getWriteEventsInterval() * pSimConf.getIterationsPerCycle());
		config.controler()
				.setWritePlansInterval(config.controler().getWritePlansInterval() * pSimConf.getIterationsPerCycle());
		config.controler().setWriteSnapshotsInterval(
				config.controler().getWriteSnapshotsInterval() * pSimConf.getIterationsPerCycle());
		log.warn("Adjusting iteration numbers in config.controler() "
				+ "under the assumption that pSim iteration were so far not accounted for:");
		log.warn("  lastIteration = " + config.controler().getLastIteration());
		log.warn("  writeEventsInterval = " + config.controler().getWriteEventsInterval());
		log.warn("  writeSnapshotsInterval = " + config.controler().getWriteSnapshotsInterval());
		log.warn("  writePlansInterval = " + config.controler().getWritePlansInterval());

		/*
		 * Use minimal choice set and always remove the worse plan. This probably as
		 * close as it can get to best-response in the presence of random innovation
		 * strategies.
		 */
		config.strategy().setMaxAgentPlanMemorySize(1);
		config.strategy().setPlanSelectorForRemoval("WorstPlanSelector");
		log.warn("Approximating a best-response simulation through the following settings:");
		log.warn("  maxAgentPlanMemorySize = 1");
		log.warn("  planSelectorForRemoval = worstPlanSelector");

		/*
		 * Keep only plan innovation strategies. Re-weight for maximum pSim efficiency.
		 * 
		 */
		if (accelerationConfig.getAdjustStrategyWeights()) {
			final double singleExpensiveStrategyProba = 1.0 / pSimConf.getIterationsPerCycle();
			final double cheapStrategyProbaSum = 1.0 - singleExpensiveStrategyProba * expensiveStrategyCnt;
			final double cheapStrategyWeightFactor = cheapStrategyProbaSum / cheapStrategyWeightSum;
			double probaSum = 0;
			for (StrategySettings strategySettings : config.strategy().getStrategySettings()) {
				final String strategyName = strategySettings.getStrategyName();
				if (accelerationConfig.getExpensiveStrategyList().contains(strategyName)) {
					strategySettings.setWeight(singleExpensiveStrategyProba);
				} else if (accelerationConfig.getCheapStrategyList().contains(strategyName)) {
					strategySettings.setWeight(cheapStrategyWeightFactor * strategySettings.getWeight());
				} else {
					strategySettings.setWeight(0.0); // i.e., dismiss
				}
				log.warn("Setting weight of strategy " + strategyName + " to " + strategySettings.getWeight() + ".");
				probaSum += strategySettings.getWeight();
			}
			if (Math.abs(1.0 - probaSum) >= 1e-8) {
				throw new RuntimeException("The sum of all strategy probabilities is " + probaSum + ".");
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

		ConfigUtils.addOrGetModule(this.config, GreedoConfigGroup.class).configure(this.scenario,
				this.scenario.getNetwork().getLinks().keySet(),
				this.scenario.getTransitVehicles().getVehicles().keySet());
	}

	// -------------------- Overriding of AbstractModule --------------------

	@Override
	public void install() {

		this.bind(MobSimSwitcher.class);
		this.addControlerListenerBinding().to(MobSimSwitcher.class);
		this.bindMobsim().toProvider(SwitchingMobsimProvider.class);
		this.bind(TravelTimeCalculator.class).to(PSimTravelTimeCalculator.class);
		this.bind(TravelTime.class).toProvider(PSimTravelTimeCalculator.class);

		// TODO This is probably needed because the QSimProvider otherwise somehow gets
		// shadowed by the SwitchingMobsimProvider. Gunnar 2019-01-23.
		this.bind(QSimProvider.class);

		if (this.config.transit().isUseTransit()) {
			// TODO See warning below.
			log.warn("Transit is included -- this is only tested with deterministic SBB transit.");
			this.bind(FifoTransitPerformance.class);
			this.addEventHandlerBinding().to(FifoTransitPerformance.class);
			this.bind(TransitEmulator.class).to(FifoTransitEmulator.class);
		} else {
			this.bind(TransitEmulator.class).to(NoTransitEmulator.class);
		}

		this.bind(GreedoProgressListener.class).toInstance(this.greedoProgressListener);
		this.bind(WireGreedoIntoMATSimListener.class).in(Singleton.class);
		this.addControlerListenerBinding().to(WireGreedoIntoMATSimListener.class);
		this.addEventHandlerBinding().to(WireGreedoIntoMATSimListener.class);
		this.addPlanStrategyBinding(AcceptIntendedReplanningStrategy.STRATEGY_NAME)
				.toProvider(AcceptIntendedReplanningStragetyProvider.class);
	}

	// -------------------- FURTHER CONFIGURATION --------------------

	// Just for debugging.
	public void setGreedoProgressListener(final GreedoProgressListener greedoProgressListener) {
		this.greedoProgressListener = greedoProgressListener;
	}

}
