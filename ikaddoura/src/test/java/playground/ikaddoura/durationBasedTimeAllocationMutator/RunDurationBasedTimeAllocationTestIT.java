/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.ikaddoura.durationBasedTimeAllocationMutator;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.utils.misc.Time;
import org.matsim.testcases.MatsimTestUtils;

public class RunDurationBasedTimeAllocationTestIT {
	
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();
	
	private static final String STRATEGY_NAME = "durationBasedTimeMutator";

	@Test
	public final void test1() {
		
		OutputDirectoryLogging.catchLogEntries();
		
		Config config = ConfigUtils.loadConfig(testUtils.getPackageInputDirectory() + "config-test.xml");

		//add a strategy to the config
		StrategySettings stratSets = new StrategySettings();
		stratSets.setStrategyName(STRATEGY_NAME);
		stratSets.setWeight(100.0);
		config.strategy().addStrategySettings(stratSets);

		config.controler().setOutputDirectory(testUtils.getOutputDirectory());
		final Controler controler = new Controler(config);
		
		//add the binding strategy 
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addPlanStrategyBinding(STRATEGY_NAME).toProvider(DurationBasedTimeAllocationPlanStrategyProvider.class);
			}
		});
				
		controler.run();
		
		for (Person person : controler.getScenario().getPopulation().getPersons().values()) {
			for (PlanElement pE : person.getSelectedPlan().getPlanElements()) {
				if (pE instanceof Activity) {
					Activity act = (Activity) pE;
					
					if (act.getMaximumDuration().isDefined()) {
						System.out.println(act.getMaximumDuration());
						Assert.assertEquals("Wrong activity end time.", true, act.getMaximumDuration().seconds() <= 10 + 10);
					}
				}
			}
		}

	}

}
