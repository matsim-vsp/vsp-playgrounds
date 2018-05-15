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

package playground.ikaddoura.subtourModeChoice;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.testcases.MatsimTestUtils;

public class RunSubtourModeChoiceTestIT {
	
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();
	
	@Ignore
	@Test
	public final void test1() {
		
		OutputDirectoryLogging.catchLogEntries();
		
		Config config = ConfigUtils.loadConfig(testUtils.getPackageInputDirectory() + "config-test.xml");

		config.subtourModeChoice().setProbaForRandomSingleTripMode(0.5);
		
		config.controler().setOutputDirectory(testUtils.getOutputDirectory());
		final Controler controler = new Controler(config);
				
		controler.run();
		
		boolean differentNonChainBasedModes = false;
		
		for (Person person : controler.getScenario().getPopulation().getPersons().values()) {
			
			for (Plan plan : person.getPlans()) {
				String mode = null;
				for (PlanElement pE : plan.getPlanElements()) {
					if (pE instanceof Leg) {
						Leg leg = (Leg) pE;
						if (mode == null || mode.equals(leg.getMode())) {
							// same mode or first leg
						} else {
							// subtour with different modes
							for (String chainBasedMode : config.subtourModeChoice().getChainBasedModes()) {
								if (chainBasedMode.equals(mode) || chainBasedMode.equals(leg.getMode())) {
									System.out.println("chain-based-mode: " + chainBasedMode);
									System.out.println(mode + " / " + leg.getMode());
									throw new RuntimeException("One of the two different modes is a chain based mode. Aborting...");
								} else {
									differentNonChainBasedModes = true;
								}
							}
						}
						mode = leg.getMode();						
					}
				}
			}
		}
		
		Assert.assertEquals("There is not a single plan with different non-chain-based modes.", true, differentNonChainBasedModes);

	}

}
