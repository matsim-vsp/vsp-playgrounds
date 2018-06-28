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

import com.vividsolutions.jts.geom.Envelope;

/**
 * @author dziemke
 * 
 * For configurations also see playground.agarwalamit.mixedTraffic.patnaIndia.policies.PatnaPolicyControler
 */
public class AccessibilityComputationPatna {
	public static final Logger LOG = Logger.getLogger(AccessibilityComputationPatna.class);
	
	public static void main(String[] args) throws IOException {
		Double cellSize = 50.;
		boolean push2Geoserver = false;
		boolean createQGisOutput = true;
		
		// TODO
//		String runOutputFolder = "../../runs-svn/patnaIndia/run108/jointDemand/policies/0.15pcu/bau/";
//		String runOutputFolder = "../../runs-svn/patnaIndia/run108/jointDemand/policies/0.15pcu/BT-b/";
		String runOutputFolder = "../../runs-svn/patnaIndia/run108/jointDemand/policies/0.15pcu/BT-mb/";
		String accessibilityOutputDirectory = runOutputFolder + "../accessibilities/BT-mb/50_1.0-1.7/";
		
//		final Config config = ConfigUtils.createConfig(new AccessibilityConfigGroup());
		Config config = ConfigUtils.loadConfig(runOutputFolder + "output_config.xml.gz", new AccessibilityConfigGroup());
		Envelope envelope = new Envelope(307000,324000,2829000,2837000); // Notation: minX, maxX, minY, maxY
		String scenarioCRS = "EPSG:24345"; // EPSG:24345 = Kalianpur 1975 / UTM zone 45N
		
//		config.network().setInputFile(runOutputFolder + "output_network.xml.gz");
		config.network().setInputFile("output_network.xml.gz");
		
		config.plans().setInputFile("output_plans.xml.gz");
		
		config.plans().setInputPersonAttributeFile("output_personAttributes.xml.gz");
		
		config.vspExperimental().setVspDefaultsCheckingLevel(VspDefaultsCheckingLevel.warn);
		
		config.vehicles().setVehiclesFile("output_vehicles.xml.gz");
		
		config.facilities().setInputFile(new File("../../runs-svn/patnaIndia/run108/jointDemand/policies/0.15pcu/accessibilities/facilities/2017-09-26_facilities.xml").getAbsolutePath());
		
//		config.global().setCoordinateSystem(scenarioCRS);

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(accessibilityOutputDirectory);
		config.controler().setFirstIteration(0); // new
		config.controler().setLastIteration(0);
		config.controler().setRunId("in_patna_" + cellSize.toString().split("\\.")[0]);
		
		AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class);
		acg.setCellSizeCellBasedAccessibility(cellSize.intValue());
		acg.setAreaOfAccessibilityComputation(AreaOfAccesssibilityComputation.fromBoundingBox);
		acg.setEnvelope(envelope);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.freespeed, true);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.car, true);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.bike, true);
		acg.setOutputCrs(scenarioCRS);
		
//		config.travelTimeCalculator().setAnalyzedModes("car,bike,motorbike");
//		config.travelTimeCalculator().setSeparateModes(true);
		
//		config.plansCalcRoute().setNetworkModes(Arrays.asList("car", "bike", "motorbike"));
		
		MutableScenario scenario = (MutableScenario) ScenarioUtils.loadScenario(config);

		// Activity types
		final List<String> activityTypes = Arrays.asList(new String[]{FacilityTypes.EDUCATION}); // TODO
		
		// Create densities from network
		final ActivityFacilities densityFacilities = AccessibilityUtils.createFacilityForEachLink(scenario.getNetwork());
		
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
			final Double lowerBound = 1.0; // (upperBound - lowerBound) ideally nicely divisible by (range - 2) // TODO
			final Double upperBound = 1.7;
			final int populationThreshold = (int) (0 / (1000/cellSize * 1000/cellSize)); // TODO
			
			String osName = System.getProperty("os.name");
			String workingDirectory = config.controler().getOutputDirectory();
			for (String actType : activityTypes) {
				String actSpecificWorkingDirectory = workingDirectory + actType + "/";
				for (Modes4Accessibility mode : acg.getIsComputingMode()) {
					VisualizationUtils.createQGisOutputGraduatedStandardColorRange(actType, mode.toString(), envelope, workingDirectory,
							scenarioCRS, includeDensityLayer, lowerBound, upperBound, range, cellSize.intValue(),
							populationThreshold);
					VisualizationUtils.createSnapshot(actSpecificWorkingDirectory, mode.toString(), osName);
				}
			}
		}
	}
}