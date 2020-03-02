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

public class RunBerlinModeAnalysis {

	public static void main(String[] args) {
		
		final String runId = "berlin-drt-v5.5-10pct_drt-12";
		final String runDirectory = "/Users/ihab/Documents/workspace/runs-svn/drt-open-berlin/berlin-drt-v5.5-10pct_drt-12/";
		
		final String outputDirectory = runDirectory + "/modal-split-analysis/";
		
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(null);
		config.plans().setInputFile(runDirectory + "ITERS/it.450/berlin-drt-v5.5-10pct_drt-12.450.plans.xml.gz");
		config.controler().setRunId(runId);
		config.global().setCoordinateSystem("EPSG:31468");
		config.vehicles().setVehiclesFile(null);
		config.transit().setTransitScheduleFile(null);
		config.transit().setVehiclesFile(null);
		config.facilities().setInputFile(null);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		AgentAnalysisFilter filter = new AgentAnalysisFilter("A");
		
		filter.setSubpopulation("person");
		
		filter.setPersonAttribute("berlin");
		filter.setPersonAttributeName("home-activity-zone");
		
//		filter.setZoneFile("/Users/ihab/Documents/workspace/shared-svn/projects/avoev/matsim-input-files/berlin/berlin.shp");
//		filter.setRelevantActivityType("home");
		
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
		distanceGroups.add(new Tuple<>(100000., 999999999999.));
		analysis.writeTripRouteDistances(outputDirectory, distanceGroups);
		analysis.writeTripEuclideanDistances(outputDirectory, distanceGroups);
	}
}

