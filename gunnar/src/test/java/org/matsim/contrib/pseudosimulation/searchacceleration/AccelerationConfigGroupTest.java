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

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.pseudosimulation.PSimConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class AccelerationConfigGroupTest {

	@Test
	public void testCheapAndExpensiveStrategies() {
		final String cheapStrategyString = "cheapStrategy1,cheapStrategy2 ,cheapStrategy3, cheapStrategy4 , cheapStrategy5";
		final String expensiveStrategyString = "expStrategy1,expStrategy2 ,expStrategy3, expStrategy4 , expStrategy5";
		final AccelerationConfigGroup conf = new AccelerationConfigGroup();
		conf.setCheapStrategies(cheapStrategyString);
		conf.setExpensiveStrategies(expensiveStrategyString);
		Assert.assertEquals("cheapStrategy1,cheapStrategy2,cheapStrategy3,cheapStrategy4,cheapStrategy5",
				conf.getCheapStrategies());
		Assert.assertEquals("expStrategy1,expStrategy2,expStrategy3,expStrategy4,expStrategy5",
				conf.getExpensiveStrategies());
	}

	@Test
	public void testReplanningStateProbability_ConstantReplanningRate() {

		for (int lastIteration : new int[] { 49, 50, 51 }) {

			final Config conf = ConfigUtils.createConfig();
			final Scenario scenario = ScenarioUtils.createScenario(conf);

			conf.controler().setFirstIteration(0);
			conf.controler().setLastIteration(lastIteration);

			final PSimConfigGroup pSimConf = ConfigUtils.addOrGetModule(conf, PSimConfigGroup.class);
			pSimConf.setIterationsPerCycle(10);

			final AccelerationConfigGroup accConf = ConfigUtils.addOrGetModule(conf, AccelerationConfigGroup.class);
			accConf.setInitialMeanReplanningRate(0.1);
			accConf.setReplanningRateIterationExponent(0.0); // constant
			accConf.configure(scenario, new LinkedHashSet<>(), new LinkedHashSet<>());

			for (int matsimIt = conf.controler().getFirstIteration(); matsimIt <= conf.controler()
					.getLastIteration(); matsimIt++) {
				final int greedoIt = accConf.getGreedoIteration(matsimIt);

				Assert.assertEquals(0.1, accConf.getMeanReplanningRate(greedoIt), 1e-8);

				final double[] replanningStateProbas = accConf.getAgeWeights(greedoIt);
				Assert.assertEquals(greedoIt + 1, replanningStateProbas.length);
				for (int age = 0; age <= greedoIt; age++) {
					Assert.assertEquals(Math.pow(1.0 - 0.1, age), replanningStateProbas[age], 1e-8);
				}
			}
		}
	}

	@Test
	public void testReplanningStateProbability_MSA() {

		for (int lastIteration : new int[] { 49, 50, 51 }) {

			final Config conf = ConfigUtils.createConfig();
			final Scenario scenario = ScenarioUtils.createScenario(conf);

			conf.controler().setFirstIteration(0);
			conf.controler().setLastIteration(lastIteration);

			final PSimConfigGroup pSimConf = ConfigUtils.addOrGetModule(conf, PSimConfigGroup.class);
			pSimConf.setIterationsPerCycle(10);

			final AccelerationConfigGroup accConf = ConfigUtils.addOrGetModule(conf, AccelerationConfigGroup.class);
			accConf.setInitialMeanReplanningRate(1.0);
			accConf.setReplanningRateIterationExponent(-1.0); // MSA
			accConf.configure(scenario, new LinkedHashSet<>(), new LinkedHashSet<>());

			for (int matsimIt = conf.controler().getFirstIteration(); matsimIt <= conf.controler()
					.getLastIteration(); matsimIt++) {
				final int greedoIt = accConf.getGreedoIteration(matsimIt);

				Assert.assertEquals(1.0 / (1.0 + greedoIt), accConf.getMeanReplanningRate(greedoIt), 1e-8);

				final double[] replanningStateProbas = accConf.getAgeWeights(greedoIt);
				double expected = 1.0;
				Assert.assertEquals(expected, replanningStateProbas[0], 1e-8);
				for (int age = 1; age <= greedoIt; age++) {
					expected *= (1.0 - accConf.getMeanReplanningRate(greedoIt - age));
					Assert.assertEquals(expected, replanningStateProbas[age], 1e-8);
				}
			}
		}
	}

}
