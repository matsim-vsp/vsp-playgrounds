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
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup.AreaOfAccesssibilityComputation;
import org.matsim.contrib.accessibility.AccessibilityModule;
import org.matsim.contrib.accessibility.FacilityTypes;
import org.matsim.contrib.accessibility.Labels;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.accessibility.utils.AccessibilityUtils;
import org.matsim.contrib.accessibility.utils.VisualizationUtils;
import org.matsim.contrib.matrixbasedptrouter.MatrixBasedPtRouterConfigGroup;
import org.matsim.contrib.matrixbasedptrouter.PtMatrix;
import org.matsim.contrib.matrixbasedptrouter.utils.BoundingBox;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;

import com.vividsolutions.jts.geom.Envelope;

/**
 * @author dziemke
 */
public class AccessibilityComputationNairobiMatrixBased {
	public static final Logger LOG = Logger.getLogger(AccessibilityComputationNairobiMatrixBased.class);

	public static void main(String[] args) {
		int tileSize_m = 500;
		boolean push2Geoserver = false; // Set true for run on server
		boolean createQGisOutput = true; // Set false for run on server

		final Config config = ConfigUtils.createConfig(new AccessibilityConfigGroup());
		final Envelope envelope = new  Envelope(240000, 280000, 9844000, 9874000); // whole Nairobi
		String scenarioCRS = "EPSG:21037"; // EPSG:21037 = Arc 1960 / UTM zone 37S, for Nairobi, Kenya
	
		config.network().setInputFile("../../shared-svn/projects/maxess/data/nairobi/network/2015-10-15_network.xml");
		config.facilities().setInputFile("../../shared-svn/projects/maxess/data/nairobi/facilities/02/facilities.xml");

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory("../../shared-svn/projects/maxess/data/nairobi/output/matrix-based/");
		config.controler().setLastIteration(0);
		config.controler().setRunId("ke_nairobi_" + tileSize_m + "work_matrix-based");
		
		config.global().setCoordinateSystem(scenarioCRS);

		AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class);
		acg.setAreaOfAccessibilityComputation(AreaOfAccesssibilityComputation.fromBoundingBox);
		acg.setEnvelope(envelope);
		acg.setTileSize_m(tileSize_m);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.freespeed, true);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.walk, true);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.matrixBasedPt, true);
		acg.setOutputCrs(scenarioCRS); // = Arc 1960 / UTM zone 37S, for Nairobi, Kenya
		
		MatrixBasedPtRouterConfigGroup mbConfig = new MatrixBasedPtRouterConfigGroup();
		mbConfig.setPtStopsInputFile("../../shared-svn/projects/maxess/data/nairobi/digital_matatus/otp_2015-06-16/fromIDs.csv");
		mbConfig.setPtTravelDistancesInputFile("../../shared-svn/projects/maxess/data/nairobi/digital_matatus/otp_2015-06-16/td.csv");
		mbConfig.setPtTravelTimesInputFile("../../shared-svn/projects/maxess/data/nairobi/digital_matatus/otp_2015-06-16/tt.csv");
		mbConfig.setUsingPtStops(true);
		mbConfig.setUsingTravelTimesAndDistances(true);
		config.addModule(mbConfig);
		
		ConfigUtils.setVspDefaults(config);

		ModeParams ptParams = new ModeParams(TransportMode.transit_walk);
		config.planCalcScore().addModeParams(ptParams);

		
		Scenario scenario = ScenarioUtils.loadScenario(config);

		final PtMatrix ptMatrix = PtMatrix.createPtMatrix(config.plansCalcRoute(), new BoundingBox(envelope), mbConfig);
		scenario.addScenarioElement(PtMatrix.NAME, ptMatrix);
		
		final List<String> activityTypes = Arrays.asList(new String[]{FacilityTypes.WORK});

		final ActivityFacilities densityFacilities = AccessibilityUtils.createFacilityForEachLink(Labels.DENSITIY, scenario.getNetwork());

		final Controler controler = new Controler(scenario);

		for (String activityType : activityTypes) {
			AccessibilityModule module = new AccessibilityModule();
			module.setConsideredActivityType(activityType);
			module.addAdditionalFacilityData(densityFacilities);
			module.setPushing2Geoserver(push2Geoserver);
			controler.addOverridingModule(module);
		}

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(PtMatrix.class).toInstance(ptMatrix);
			}
		});

		controler.run();

		if (createQGisOutput) {
			final boolean includeDensityLayer = true;
			final Integer range = 9; // In the current implementation, this must always be 9
			final Double lowerBound = -7.; // (upperBound - lowerBound) ideally	nicely divisible by (range - 2)
			final Double upperBound = 0.;
			final int populationThreshold = (int) (50 / (1000 / tileSize_m * 1000 / tileSize_m));

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