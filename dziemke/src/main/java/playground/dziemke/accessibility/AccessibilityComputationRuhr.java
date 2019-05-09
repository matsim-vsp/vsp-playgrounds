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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup.AreaOfAccesssibilityComputation;
import org.matsim.contrib.accessibility.AccessibilityModule;
import org.matsim.contrib.accessibility.FacilityTypes;
import org.matsim.contrib.accessibility.Labels;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.accessibility.utils.AccessibilityUtils;
import org.matsim.contrib.accessibility.utils.VisualizationUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.VspDefaultsCheckingLevel;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;

import org.locationtech.jts.geom.Envelope;

/**
 * @author dziemke
 */
public class AccessibilityComputationRuhr {
	public static final Logger LOG = Logger.getLogger(AccessibilityComputationRuhr.class);
	
	public static void main(String[] args) throws IOException {
		int tileSize_m = 500;
		boolean push2Geoserver = false;
		boolean createQGisOutput = true;
		
		// TODO
		String runOutputFolder = "../../public-svn/matsim/scenarios/countries/de/ruhrgebiet/ruhrgebiet-v1.0-1pct/output-ruhrgebiet-v1.0-1pct/";
		String accessibilityOutputDirectory = runOutputFolder + "accessibilities/500_-3.5-3.5/";
		
		final Config config = ConfigUtils.createConfig(new AccessibilityConfigGroup());
//		Config config = ConfigUtils.loadConfig(runOutputFolder + "ruhrgebiet-v1.0-1pct.output_config.xml", new AccessibilityConfigGroup());
		Envelope envelope = new Envelope(353400, 370700, 5690500, 5710700); // Notation: minX, maxX, minY, maxY
		String scenarioCRS = "EPSG:25832";
		config.global().setCoordinateSystem(scenarioCRS);
		
//		config.network().setInputFile("ruhrgebiet-v1.0-1pct.output_network.xml.gz");
		config.network().setInputFile("../../public-svn/matsim/scenarios/countries/de/ruhrgebiet/ruhrgebiet-v1.0-1pct/output-ruhrgebiet-v1.0-1pct/ruhrgebiet-v1.0-1pct.output_network.xml.gz");
		
//		config.plans().setInputFile("ruhrgebiet-v1.0-1pct.output_plans.xml.gz");
		
//		config.plans().setInputPersonAttributeFile("output_personAttributes.xml.gz");
		
		config.vspExperimental().setVspDefaultsCheckingLevel(VspDefaultsCheckingLevel.warn);
		
//		config.vehicles().setVehiclesFile("output_vehicles.xml.gz");
		
		config.facilities().setInputFile(new File("/Users/dominik/Bicycle/NEMO/facilities/2019-05-08_essen_vicinity.xml.gz").getAbsolutePath());
		
//		config.global().setCoordinateSystem(scenarioCRS);

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(accessibilityOutputDirectory);
		config.controler().setFirstIteration(0); // new
		config.controler().setLastIteration(0);
		config.controler().setRunId("de_essen_" + tileSize_m);
		
		AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class);
		acg.setTileSize_m(tileSize_m);
//		acg.setAreaOfAccessibilityComputation(AreaOfAccesssibilityComputation.fromShapeFile);
		acg.setAreaOfAccessibilityComputation(AreaOfAccesssibilityComputation.fromBoundingBox);
		acg.setEnvelope(envelope);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.freespeed, true);
//		acg.setComputingAccessibilityForMode(Modes4Accessibility.car, true);
//		acg.setComputingAccessibilityForMode(Modes4Accessibility.bike, true);
		acg.setOutputCrs(scenarioCRS);
		
//		config.travelTimeCalculator().setAnalyzedModes("car,bike,motorbike");
//		config.travelTimeCalculator().setSeparateModes(true);
		
//		config.plansCalcRoute().setNetworkModes(Arrays.asList("car", "bike", "motorbike"));
		
		MutableScenario scenario = (MutableScenario) ScenarioUtils.loadScenario(config);

		// Activity types
		final List<String> activityTypes = Arrays.asList(new String[]{"supermarket"}); // TODO
		
		// Create densities from network
		final ActivityFacilities densityFacilities = AccessibilityUtils.createFacilityForEachLink(Labels.DENSITIY, scenario.getNetwork());
		
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
		if (createQGisOutput) {
			final boolean includeDensityLayer = false;
			final Integer range = 9; // In the current implementation, this must always be 9
			final Double lowerBound = -3.5; // (upperBound - lowerBound) ideally nicely divisible by (range - 2) // TODO
			final Double upperBound = 3.5;
			final int populationThreshold = (int) (0 / (1000/tileSize_m * 1000/tileSize_m)); // TODO
			
			String osName = System.getProperty("os.name");
			String workingDirectory = config.controler().getOutputDirectory();
			for (String actType : activityTypes) {
				String actSpecificWorkingDirectory = workingDirectory + actType + "/";
				for (Modes4Accessibility mode : acg.getIsComputingMode()) {
					VisualizationUtils.createQGisOutputGraduatedStandardColorRange(actType, mode.toString(), envelope, workingDirectory,
							scenarioCRS, includeDensityLayer, lowerBound, upperBound, range, tileSize_m, populationThreshold);
					VisualizationUtils.createSnapshot(actSpecificWorkingDirectory, mode.toString(), osName);
				}
			}
		}
	}
}
