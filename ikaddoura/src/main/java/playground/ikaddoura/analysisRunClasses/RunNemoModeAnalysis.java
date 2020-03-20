/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.ikaddoura.analysisRunClasses;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.matsim.analysis.AgentAnalysisFilter;
import org.matsim.analysis.DefaultAnalysisMainModeIdentifier;
import org.matsim.analysis.modalSplitUserType.ModeAnalysis;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;

/**
* @author ikaddoura
*/

public class RunNemoModeAnalysis {

	public static void main(String[] args) {
		
//		final String runDirectory = "/Users/ihab/Desktop/test-run-directory_transit-walk/";
//		final String outputDirectory = "/Users/ihab/Desktop/modal-split-analysis-transit-walk/";
//		final String runId = "test";

		final String runId = "nemo_baseCase_030";
		final String runDirectory = "/Users/ihab/Desktop/ils3a/laudan/nemo-mercartor/nemo_baseCase/nemo_baseCase_030/output/";
		final String outputBaseDirectory = "/Users/ihab/Desktop/";
		
		// if iteration < 0 --> analysis of the final iteration
		int iteration = -1;
		
		final String outputDirectory;
		if (iteration >= 0) {
			outputDirectory = outputBaseDirectory + "/" + runId + "_modal-split-analysis_" + "it." + iteration + "/";
		} else {
			outputDirectory = outputBaseDirectory + "/" + runId + "_modal-split-analysis/";
		}
		
		// optional: Provide a personAttributes file which is used instead of the normal output person attributes file; null --> using the output person attributes file
//		final String personAttributesFile = "/Users/ihab/Desktop/ils4a/ziemke/open_berlin_scenario/input/be_5_ik/population/personAttributes_500_10pct.xml.gz";
		final String personAttributesFile = null;

		Scenario scenario = loadScenario(runDirectory, runId, personAttributesFile, iteration);
		
		AgentAnalysisFilter filter = new AgentAnalysisFilter();
		
//		filter.setSubpopulation("person");
		
//		filter.setPersonAttribute("berlin");
//		filter.setPersonAttributeName("home-activity-zone");
		
		filter.setZoneFile("/Users/ihab/Documents/workspace/shared-svn/projects/nemo_mercator/data/original_files/shapeFiles/shapeFile_Ruhrgebiet/ruhrgebiet_boundary.shp");
		filter.setRelevantActivityType("home");
		
		filter.preProcess(scenario);
				
		ModeAnalysis analysis = new ModeAnalysis(scenario, filter, new DefaultAnalysisMainModeIdentifier());
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
		distanceGroups.add(new Tuple<>(100000., 1000000.));
		distanceGroups.add(new Tuple<>(100000., 999999999999.));
		analysis.writeTripRouteDistances(outputDirectory, distanceGroups);
		analysis.writeTripEuclideanDistances(outputDirectory, distanceGroups);
	}
	
	private static Scenario loadScenario(String runDirectory, String runId, String personAttributesFile, int iteration) {
		
		Scenario scenario;
		
		if (iteration < 0) {
			if (runId == null) {
				Config config = ConfigUtils.loadConfig(runDirectory + "output_config.xml");
				config.network().setInputFile(null);
				config.plans().setInputFile(runDirectory + "output_plans.xml.gz");
				if (personAttributesFile == null) {
					config.plans().setInputPersonAttributeFile(runDirectory + "output_personAttributes.xml.gz");
				} else {
					config.plans().setInputPersonAttributeFile(personAttributesFile);
				}
				config.vehicles().setVehiclesFile(null);
				config.transit().setTransitScheduleFile(null);
				config.transit().setVehiclesFile(null);
				scenario = ScenarioUtils.loadScenario(config);
				return scenario;
				
			} else {
				Config config = ConfigUtils.loadConfig(runDirectory + runId + ".output_config.xml");
				config.network().setInputFile(null);
				config.plans().setInputFile(runDirectory + runId + ".output_plans.xml.gz");
				if (personAttributesFile == null) {
					config.plans().setInputPersonAttributeFile(runDirectory + runId + ".output_personAttributes.xml.gz");
				} else {
					config.plans().setInputPersonAttributeFile(personAttributesFile);
				}
				config.vehicles().setVehiclesFile(null);
				config.transit().setTransitScheduleFile(null);
				config.transit().setVehiclesFile(null);
				scenario = ScenarioUtils.loadScenario(config);
				return scenario;
			}
			
		} else {
			Config config = ConfigUtils.createConfig();
	
			if (runId == null) {
				config.plans().setInputFile(runDirectory + "ITERS/it." + iteration + "/" + iteration + "." + "plans.xml.gz");
				if (personAttributesFile == null) {
					throw new RuntimeException("Person attributes file required. Aborting...");
				} else {
					config.plans().setInputPersonAttributeFile(personAttributesFile);
				}
				scenario = ScenarioUtils.loadScenario(config);
				return scenario;
				
			} else {
				config.plans().setInputFile(runDirectory + "ITERS/it." + iteration + "/" + runId + "." + iteration + "." + "plans.xml.gz");
				if (personAttributesFile == null) {
					throw new RuntimeException("Person attributes file required. Aborting...");
				} else {
					config.plans().setInputPersonAttributeFile(personAttributesFile);
				}
				scenario = ScenarioUtils.loadScenario(config);
				return scenario;
			}
		}
		
	}
}

