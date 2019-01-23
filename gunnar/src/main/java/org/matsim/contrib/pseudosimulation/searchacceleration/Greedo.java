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

	private final int defaultIterationsPerCycle = 10;

	// -------------------- MEMBERS --------------------

	private Config config = null;

	private Scenario scenario = null;

	private GreedoProgressListener greedoProgressListener = new GreedoProgressListener() {
	};

	// -------------------- CONSTRUCTION --------------------

	public Greedo() {
	}

	// -------------------- CONFIGURATION --------------------

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
		 * Ensure a valid acceleration configuration; fall back to default values if not
		 * available.
		 */
		final AccelerationConfigGroup accelerationConfig = ConfigUtils.addOrGetModule(config,
				AccelerationConfigGroup.class);

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
		// config.parallelEventHandling().setNumberOfThreads(1); // FIXME Why was this
		// ever set to one?

		/*
		 * Preliminary analysis of innovation strategies.
		 */
		int bestResponseStrategyCnt = 0;
		int randomInnovationStrategyCnt = 0;
		double randomInnovationStrategyWeightSum = 0.0;
		for (StrategySettings strategySettings : config.strategy().getStrategySettings()) {
			final String strategyName = strategySettings.getStrategyName();
			if (strategySettings.getWeight() > 0) {
				if (accelerationConfig.getExpensiveStrategyList().contains(strategyName)) {
					bestResponseStrategyCnt++;
				} else if (accelerationConfig.getCheapStrategyList().contains(strategyName)) {
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
		if (accelerationConfig.getAdjustStrategyWeights()) {
			final double individualBestResponseStrategyProba = 1.0 / pSimConf.getIterationsPerCycle();
			final double bestResponseStrategyProbaSum = individualBestResponseStrategyProba * bestResponseStrategyCnt;
			final double randomInnovationStrategyProbaSum = 1.0 - bestResponseStrategyProbaSum;
			final double randomInnovationStrategyWeightFactor = randomInnovationStrategyProbaSum
					/ randomInnovationStrategyWeightSum;
			double sum = 0;
			for (StrategySettings strategySettings : config.strategy().getStrategySettings()) {
				final String strategyName = strategySettings.getStrategyName();
				if (accelerationConfig.getExpensiveStrategyList().contains(strategyName)) {
					strategySettings.setWeight(individualBestResponseStrategyProba);
				} else if (accelerationConfig.getCheapStrategyList().contains(strategyName)) {
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

		// Needs this probably because the QSimProvider otherwise gets shadowed by the
		// SwitchingMobsimProvider.
		this.bind(QSimProvider.class);

		if (this.config.transit().isUseTransit()) {
			this.bind(FifoTransitPerformance.class);
			this.addEventHandlerBinding().to(FifoTransitPerformance.class);
			this.bind(TransitEmulator.class).to(FifoTransitEmulator.class);
		} else {
			this.bind(TransitEmulator.class).to(NoTransitEmulator.class);
		}

		this.bind(GreedoProgressListener.class).toInstance(this.greedoProgressListener);
		this.bind(SearchAccelerator.class).in(Singleton.class);
		this.addControlerListenerBinding().to(SearchAccelerator.class);
		this.addEventHandlerBinding().to(SearchAccelerator.class);
		this.addPlanStrategyBinding(AcceptIntendedReplanningStrategy.STRATEGY_NAME)
				.toProvider(AcceptIntendedReplanningStragetyProvider.class);
	}

	// @Provides
	// QSimComponentsConfig provideQSimComponentsConfig() {
	// QSimComponentsConfig components = new QSimComponentsConfig();
	// new StandardQSimComponentConfigurator(this.config).configure(components);
	// if (this.config.transit().isUseTransit()) {
	// SBBTransitEngineQSimModule.configure(components);
	// }
	// return components;
	// }
}
