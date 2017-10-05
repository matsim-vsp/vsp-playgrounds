/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.dziemke.accessibility;

import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.AccessibilityModule;
import org.matsim.contrib.accessibility.FacilityTypes;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.accessibility.utils.AccessibilityUtils;
import org.matsim.contrib.accessibility.utils.VisualizationUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;

import com.vividsolutions.jts.geom.Envelope;

/**
 * @author dziemke
 */
public class AccessibilityComputationCottbus {
	public static final Logger LOG = Logger.getLogger(AccessibilityComputationCottbus.class);
	
	public static void main(String[] args) {
		Double cellSize = 2000.;
		boolean push2Geoserver = false;
		boolean createQGisOutput = true;
		
		String runOutputFolder = "../../public-svn/matsim/scenarios/countries/de/cottbus/commuter-population-only-car-traffic-only-100pct-2016-03-18/";
		String accessibilityOutputDirectory = runOutputFolder + "accessibilities/";
		
		Config config = ConfigUtils.loadConfig(runOutputFolder + "config.xml", new AccessibilityConfigGroup());
		Envelope envelope = new Envelope(447000,461000,5729000,5740000);
		String scenarioCRS = "EPSG:32633"; // EPSG:32633 = WGS 84 / UTM zone 33N, for Eastern half of Germany
		
		config.network().setInputFile("network_wgs84_utm33n.xml.gz");
		config.plans().setInputFile("commuter_population_wgs84_utm33n_car_only.xml.gz");
		
//		config.global().setCoordinateSystem(scenarioCRS);
		
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(accessibilityOutputDirectory);
		config.controler().setLastIteration(0);
		config.controler().setRunId("de_cottbus_" + cellSize.toString().split("\\.")[0]);
		
//		config.transit().setTransitScheduleFile(runOutputFolder + "output_transitSchedule.xml.gz");
//		config.transit().setVehiclesFile(runOutputFolder + "output_transitVehicles.xml.gz");
		
		AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class);
		acg.setCellSizeCellBasedAccessibility(cellSize.intValue());
		acg.setEnvelope(envelope);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.walk, true);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.freespeed, true);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.car, true);
		acg.setOutputCrs(scenarioCRS);
		
		MutableScenario scenario = (MutableScenario) ScenarioUtils.loadScenario(config);
		
		// Create facilities from plans
		ActivityFacilities activityFacilities = AccessibilityUtils.createFacilitiesFromPlans(scenario.getPopulation());
		scenario.setActivityFacilities(activityFacilities);

		// Activity types
		final List<String> activityTypes = Arrays.asList(new String[]{FacilityTypes.WORK});

		// Collect homes for density layer
		final ActivityFacilities densityFacilities = AccessibilityUtils.collectActivityFacilitiesWithOptionOfType(scenario, FacilityTypes.HOME);

		final Controler controler = new Controler(scenario);

		for (String activityType : activityTypes) {
			AccessibilityModule module = new AccessibilityModule();
			module.setConsideredActivityType(activityType);
			module.addAdditionalFacilityData(densityFacilities);
			module.setPushing2Geoserver(push2Geoserver);
			controler.addOverridingModule(module);
		}
		controler.run();
		
		// QGis
		if (createQGisOutput == true) {
			final boolean includeDensityLayer = false;
			final Integer range = 9; // In the current implementation, this must always be 9
			final Double lowerBound = 0.; // (upperBound - lowerBound) ideally nicely divisible by (range - 2)
			final Double upperBound = 3.5;
			final int populationThreshold = (int) (50 / (1000/cellSize * 1000/cellSize));

			String osName = System.getProperty("os.name");
			String workingDirectory = config.controler().getOutputDirectory();
			for (String actType : activityTypes) {
				String actSpecificWorkingDirectory = workingDirectory + actType + "/";
				for (Modes4Accessibility mode : acg.getIsComputingMode()) {
					VisualizationUtils.createQGisOutputGraduatedStandardColorRange(actType, mode.toString(), envelope, workingDirectory,
							scenarioCRS, includeDensityLayer, lowerBound, upperBound, range, cellSize.intValue(), populationThreshold);
					VisualizationUtils.createSnapshot(actSpecificWorkingDirectory, mode.toString(), osName);
				}
			}
		}
	}
}