/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.ikaddoura.analysis.modeAnalysis;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.testcases.MatsimTestUtils;

import playground.ikaddoura.analysis.modalSplitUserType.AgentAnalysisFilter;
import playground.ikaddoura.analysis.modalSplitUserType.ModeAnalysis;


/**
 * @author ikaddoura
 */

public class ModeAnalysisTest {
		
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();

	@Test
	public final void test1() {
		
		final String runDirectory = testUtils.getPackageInputDirectory();
		System.out.println(testUtils.getPackageInputDirectory());
		final String outputDirectory = testUtils.getOutputDirectory();
		final String runId = "test";
		
		// optional: Provide a personAttributes file which is used instead of the normal output person attributes file
		final String personAttributesFile = "test.output_personAttributes.xml.gz";
		
		Scenario scenario = loadScenario(runDirectory, runId, personAttributesFile);
		
		AgentAnalysisFilter filter = new AgentAnalysisFilter(scenario);
		
		filter.setSubpopulation("person");
		
		filter.setPersonAttribute("berlin");
		filter.setPersonAttributeName("home-activity-zone");
		
		filter.setZoneFile(null);
		filter.setRelevantActivityType(null);
		
		filter.preProcess(scenario);
				
		ModeAnalysis analysis = new ModeAnalysis(scenario, filter);
		analysis.run();
		
		File directory = new File(outputDirectory);
		directory.mkdirs();
		
		analysis.writeModeShares(outputDirectory);
		analysis.writeTripRouteDistances(outputDirectory);
		analysis.writeTripEuclideanDistances(outputDirectory);
		
		final List<Tuple<Double, Double>> distanceGroups = new ArrayList<>();
		distanceGroups.add(new Tuple<>(0., 1000.));
		distanceGroups.add(new Tuple<>(1000., 3000.));
		distanceGroups.add(new Tuple<>(3000., 5000.));
		distanceGroups.add(new Tuple<>(5000., 10000.));
		distanceGroups.add(new Tuple<>(10000., 20000.));
		distanceGroups.add(new Tuple<>(20000., 100000.));
		analysis.writeTripRouteDistances(outputDirectory, distanceGroups);
		analysis.writeTripEuclideanDistances(outputDirectory, distanceGroups);
				
		Assert.assertEquals("wrong number of trips", 1, analysis.getMode2TripCounterFiltered().get(TransportMode.car), MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong number of trips", 2, analysis.getMode2TripCounterFiltered().get("access_walk"), MatsimTestUtils.EPSILON);
		Assert.assertEquals("wrong number of trips", 1, analysis.getMode2TripCounterFiltered().get(TransportMode.transit_walk), MatsimTestUtils.EPSILON);
	}
	
	private static Scenario loadScenario(String runDirectory, String runId, String personAttributesFile) {
		Scenario scenario;
		Config config = ConfigUtils.loadConfig(runDirectory + runId + ".output_config.xml");
		config.network().setInputFile(null);
		config.plans().setInsistingOnUsingDeprecatedPersonAttributeFile(true);
		config.plans().setInputFile(runId + ".output_plans.xml.gz");
		if (personAttributesFile == null) {
			config.plans().setInputPersonAttributeFile(runId + ".output_personAttributes.xml.gz");
		} else {
			config.plans().setInputPersonAttributeFile(personAttributesFile);
		}
		config.vehicles().setVehiclesFile(null);
		config.transit().setTransitScheduleFile(null);
		config.transit().setVehiclesFile(null);
		scenario = ScenarioUtils.loadScenario(config);
		return scenario;
	}
}
