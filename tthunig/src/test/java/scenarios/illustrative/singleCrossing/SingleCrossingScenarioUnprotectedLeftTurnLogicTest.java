/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package scenarios.illustrative.singleCrossing;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.contrib.signals.SignalSystemsConfigGroup.IntersectionLogic;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author tthunig
 */
public class SingleCrossingScenarioUnprotectedLeftTurnLogicTest {

	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();
	
	@Test
	@Ignore // until branch turnAcceptanceLogic has been merged into master
	public void testSingleIntersectionScenarioWithLeftTurns() {
		RunSingleIntersectionWithUnprotectedLeftTurnLogic simulationWithRestrictedLeftTurns = new RunSingleIntersectionWithUnprotectedLeftTurnLogic(
				IntersectionLogic.CONFLICTING_DIRECTIONS_AND_TURN_RESTRICTIONS);
		RunSingleIntersectionWithUnprotectedLeftTurnLogic simulationWoRestrictedLeftTurns = new RunSingleIntersectionWithUnprotectedLeftTurnLogic(
				IntersectionLogic.CONFLICTING_DIRECTIONS_NO_TURN_RESTRICTIONS);
		
		double leftTurnDelayWTurnRestriction = simulationWithRestrictedLeftTurns.runSingleIntersection();
		double leftTurnDelayWoTurnRestriction = simulationWoRestrictedLeftTurns.runSingleIntersection();
		Assert.assertTrue("Delay without restriction should be less than with restricted left turns.", leftTurnDelayWoTurnRestriction < leftTurnDelayWTurnRestriction);
		Assert.assertEquals("Delay value for the case without turn restrictions is not as expected!", 23051, leftTurnDelayWoTurnRestriction, MatsimTestUtils.EPSILON);
		Assert.assertEquals("Delay value for the case with turn restrictions is not as expected!", 84314, leftTurnDelayWTurnRestriction, MatsimTestUtils.EPSILON);
	}
	
}
