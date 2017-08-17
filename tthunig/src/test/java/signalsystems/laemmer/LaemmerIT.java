/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DefaultControlerModules.java
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
package signalsystems.laemmer;

import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.testcases.MatsimTestUtils;

import analysis.signals.TtSignalAnalysisTool;
import scenarios.illustrative.singleCrossing.SingleCrossingScenario;
import signals.laemmer.model.LaemmerConfig.Regime;
import signalsystems.sylvia.SylviaIT;

/**
 * @author tthunig
 *
 */
public class LaemmerIT {

	private static final Logger log = Logger.getLogger(SylviaIT.class);

	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();
	
	/**
	 * single intersection with demand (equals flow capacity) only in NS-direction. signals should show green only for the NS-direction.
	 */
	@Test
	public void testSingleCrossingScenarioDemandNS() {
		TtSignalAnalysisTool signalAnalyzer = runSingleCrossingScenario(1800, 0, true);

		Map<Id<SignalGroup>, Double> totalSignalGreenTimes = signalAnalyzer.getTotalSignalGreenTime(); // NS should show a lot more green than WE
		Map<Id<SignalGroup>, Double> avgSignalGreenTimePerCycle = signalAnalyzer.calculateAvgSignalGreenTimePerFlexibleCycle(); // WE should be almost 0
		Map<Id<SignalSystem>, Double> avgCycleTimePerSystem = signalAnalyzer.calculateAvgFlexibleCycleTimePerSignalSystem(); // should be around 60, at most 90

		log.info("total signal green times: " + totalSignalGreenTimes.get(SingleCrossingScenario.signalGroupId1) + ", "
				+ totalSignalGreenTimes.get(SingleCrossingScenario.signalGroupId2));
		log.info("avg signal green times per cycle: " + avgSignalGreenTimePerCycle.get(SingleCrossingScenario.signalGroupId1) + ", "
				+ avgSignalGreenTimePerCycle.get(SingleCrossingScenario.signalGroupId2));
		log.info("avg cycle time per system: " + avgCycleTimePerSystem.get(SingleCrossingScenario.signalSystemId));

		Assert.assertNull("signal group 1 should show no green", avgSignalGreenTimePerCycle.get(SingleCrossingScenario.signalGroupId1));
		Assert.assertNotNull("signal group 2 should show green", avgSignalGreenTimePerCycle.get(SingleCrossingScenario.signalGroupId2));
		Assert.assertEquals("avg cycle time of the system and total green time of NS-group should be equal", totalSignalGreenTimes.get(SingleCrossingScenario.signalGroupId2), avgCycleTimePerSystem.get(SingleCrossingScenario.signalSystemId), MatsimTestUtils.EPSILON);
	}
	
	/**
	 * Test minimum green time:
	 * single intersection with high demand in WE-direction, very low demand in NS-direction but minimum green time. I.e. the NS-signal should show green for exactly this 5 seconds per cycle.
	 */
	@Test
	public void testSingleCrossingScenarioLowVsHighDemandWithMinG(){
		TtSignalAnalysisTool signalAnalyzer = runSingleCrossingScenario(90, 1800, true);
		
		Map<Id<SignalGroup>, Double> totalSignalGreenTimes = signalAnalyzer.getTotalSignalGreenTime(); 
		Map<Id<SignalGroup>, Double> avgSignalGreenTimePerCycle = signalAnalyzer.calculateAvgSignalGreenTimePerFlexibleCycle(); 
		Map<Id<SignalSystem>, Double> avgCycleTimePerSystem = signalAnalyzer.calculateAvgFlexibleCycleTimePerSignalSystem(); 
		
		log.info("total signal green times: " + totalSignalGreenTimes.get(SingleCrossingScenario.signalGroupId1) + ", " + totalSignalGreenTimes.get(SingleCrossingScenario.signalGroupId2));
		log.info("avg signal green times per cycle: " + avgSignalGreenTimePerCycle.get(SingleCrossingScenario.signalGroupId1) + ", "
				+ avgSignalGreenTimePerCycle.get(SingleCrossingScenario.signalGroupId2));
		log.info("avg cycle time per system: " + avgCycleTimePerSystem.get(SingleCrossingScenario.signalSystemId));
		
		Assert.assertTrue("total signal green time of WE-direction should be higher than NS-direction", totalSignalGreenTimes.get(SingleCrossingScenario.signalGroupId1)-totalSignalGreenTimes.get(SingleCrossingScenario.signalGroupId2) > 0);
		Assert.assertTrue("avg signal green time per cycle of WE-direction should be higher than NS-direction", avgSignalGreenTimePerCycle.get(SingleCrossingScenario.signalGroupId1)-avgSignalGreenTimePerCycle.get(SingleCrossingScenario.signalGroupId2) > 0);
		Assert.assertEquals("avg signal green time per cycle of NS-direction should be the minimum green time of 5 seconds", 5.0, avgSignalGreenTimePerCycle.get(SingleCrossingScenario.signalGroupId2), MatsimTestUtils.EPSILON);
		Assert.assertTrue("cycle time should stay below 90 seconds", avgCycleTimePerSystem.get(SingleCrossingScenario.signalSystemId) <= 90);
	}
	
	/**
	 * counterpart to minimum green time test above.
	 * single intersection with high demand in WE-direction, very low demand in NS-direction. No minimum green time! I.e. the NS-signal should show green for less than 5 seconds per cycle.
	 */
	@Test
	public void testSingleCrossingScenarioLowVsHighDemandWoMinG(){
		TtSignalAnalysisTool signalAnalyzer = runSingleCrossingScenario(90, 1800, false);
		
		Map<Id<SignalGroup>, Double> totalSignalGreenTimes = signalAnalyzer.getTotalSignalGreenTime(); 
		Map<Id<SignalGroup>, Double> avgSignalGreenTimePerCycle = signalAnalyzer.calculateAvgSignalGreenTimePerFlexibleCycle(); 
		Map<Id<SignalSystem>, Double> avgCycleTimePerSystem = signalAnalyzer.calculateAvgFlexibleCycleTimePerSignalSystem(); 
		
		log.info("total signal green times: " + totalSignalGreenTimes.get(SingleCrossingScenario.signalGroupId1) + ", " + totalSignalGreenTimes.get(SingleCrossingScenario.signalGroupId2));
		log.info("avg signal green times per cycle: " + avgSignalGreenTimePerCycle.get(SingleCrossingScenario.signalGroupId1) + ", "
				+ avgSignalGreenTimePerCycle.get(SingleCrossingScenario.signalGroupId2));
		log.info("avg cycle time per system: " + avgCycleTimePerSystem.get(SingleCrossingScenario.signalSystemId));
		
		Assert.assertTrue("total signal green time of WE-direction should be higher than NS-direction", totalSignalGreenTimes.get(SingleCrossingScenario.signalGroupId1)-totalSignalGreenTimes.get(SingleCrossingScenario.signalGroupId2) > 0);
		Assert.assertTrue("avg signal green time per cycle of WE-direction should be higher than NS-direction", avgSignalGreenTimePerCycle.get(SingleCrossingScenario.signalGroupId1)-avgSignalGreenTimePerCycle.get(SingleCrossingScenario.signalGroupId2) > 0);
		Assert.assertTrue("avg signal green time per cycle of NS-direction should be less than 5 seconds", avgSignalGreenTimePerCycle.get(SingleCrossingScenario.signalGroupId2) < 5.0);
		Assert.assertTrue("cycle time should stay below 90 seconds", avgCycleTimePerSystem.get(SingleCrossingScenario.signalSystemId) <= 90);
	}
	
	private TtSignalAnalysisTool runSingleCrossingScenario(double flowNS, double flowWE, boolean minG) {
		SingleCrossingScenario singleCrossingScenario = new SingleCrossingScenario(flowNS, flowWE, true, Regime.COMBINED, false, false, false, false, true, true, 0, false);
		if (minG){
			singleCrossingScenario.setMinG(5);
		}
		
		Controler controler = singleCrossingScenario.defineControler();
		controler.getConfig().controler().setOutputDirectory(testUtils.getOutputDirectory());
		// add signal analysis tool
		TtSignalAnalysisTool signalAnalyzer = new TtSignalAnalysisTool();
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.addEventHandlerBinding().toInstance(signalAnalyzer);
				this.addControlerListenerBinding().toInstance(signalAnalyzer);
			}
		});
		
		controler.run();
		
		return signalAnalyzer;
	}

	@Test
	public void testSingleCrossingScenarioPrivilegeWE(){
		// more demand on WE than on NS
	}
	
	// TODO test green wave (higher capacity wins)
	
	// TODO test stabilizing regime
	// TODO test optimizing regime
	
	// TODO somehow test stochasticity (besser als fixed?!; reagiert auf nachfrage - anders als constant)
	// TODO somehow test temporarily overcrowded situations (no exeption, stau löst sich wieder auf)
	// TODO somehow test liveArrivalRate vs. exact data (letzteres erzeugt bessere ergebnisse; liveArrivalRates werden korrekt bestimmt)
	// TODO somehow test minimal green time - wird es eingehalten? wird trotzdem 'bester' richtung grün gegeben? (zur not ergebnis des ganzen runs mit ergebnissen aus nicos MA abgleichen)
	// TODO somehow test grouping
	// TODO test lanes
	// ...

}
