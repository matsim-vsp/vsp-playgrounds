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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup.AreaOfAccesssibilityComputation;
import org.matsim.contrib.accessibility.AccessibilityModule;
import org.matsim.contrib.accessibility.FacilityTypes;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.accessibility.utils.AccessibilityFacilityUtils;
import org.matsim.contrib.accessibility.utils.AccessibilityOsmNetworkReader;
import org.matsim.contrib.accessibility.utils.AccessibilityUtils;
import org.matsim.contrib.accessibility.utils.VisualizationUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.facilities.ActivityFacilities;

import com.vividsolutions.jts.geom.Envelope;

/**
 * @author dziemke
 */
public class AccessibilityComputationBerlin_V2 {
	public static final Logger LOG = Logger.getLogger(AccessibilityComputationBerlin_V2.class);
	
	public static void main(String[] args) {
		Double cellSize = 100.;
		boolean push2Geoserver = false; // set true for run on server
		boolean createQGisOutput = true; // set false for run on server
		String scenarioCRS = "EPSG:31468"; // = DHDN GK4
//		final Envelope envelope = new Envelope(4590000, 4593000, 5823500, 5825500); // Berlin-GG; notation: minX, maxX, minY, maxY
//		final Envelope envelope = new Envelope(4589000, 4591500, 5820500, 5822500); // Berlin-SG; notation: minX, maxX, minY, maxY
		final Envelope envelope = new Envelope(4589000, 4593000, 5820500, 5825500); // Berlin-SG/GG; notation: minX, maxX, minY, maxY
		
		final Config config = ConfigUtils.createConfig(new AccessibilityConfigGroup());
		
		CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(scenarioCRS, "EPSG:4326");
		Coord southwest = transformation.transform(new Coord(envelope.getMinX(), envelope.getMinY()));
		Coord northeast = transformation.transform(new Coord(envelope.getMaxX(), envelope.getMaxY()));
		Network network = null;
		ActivityFacilities facilities = null;
		try {
			URL osm = new URL("http://overpass-api.de/api/xapi_meta?*[bbox=" + southwest.getX() + "," + southwest.getY() + "," + northeast.getX() + "," + northeast.getY() +"]");
		    AccessibilityOsmNetworkReader networkReader = new AccessibilityOsmNetworkReader(((HttpURLConnection) osm.openConnection()).getInputStream(), scenarioCRS);
			networkReader.setKeepPaths(true);
			networkReader.setIincludeLowHierarchyWays(true);
			networkReader.createNetwork();
			network = networkReader.getNetwork();
			facilities = AccessibilityFacilityUtils.createFacilites(((HttpURLConnection) osm.openConnection()).getInputStream(), scenarioCRS, 0.);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		config.controler().setOutputDirectory("../../shared-svn/projects/accessibility_berlin/output/neu2/");
		config.controler().setRunId("de_berlin_" + cellSize.toString().split("\\.")[0]);
		
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(0);
		config.global().setCoordinateSystem(scenarioCRS);
	
		AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class);
		acg.setAreaOfAccessibilityComputation(AreaOfAccesssibilityComputation.fromBoundingBox);
		acg.setEnvelope(envelope);
		acg.setCellSizeCellBasedAccessibility(cellSize.intValue());
		acg.setComputingAccessibilityForMode(Modes4Accessibility.walk, true);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.freespeed, false);
		acg.setOutputCrs(scenarioCRS);
		
		ConfigUtils.setVspDefaults(config);
		
		MutableScenario scenario = (MutableScenario) ScenarioUtils.loadScenario(config);
		scenario.setNetwork(network);
		scenario.setActivityFacilities(facilities);

		final List<String> activityTypes = Arrays.asList(new String[]{FacilityTypes.SHOPPING});
		final ActivityFacilities densityFacilities = AccessibilityUtils.createFacilityForEachLink(scenario.getNetwork()); // will be aggregated in downstream code!
		
		final Controler controler = new Controler(scenario);
		
		for (String activityType : activityTypes) {
			AccessibilityModule module = new AccessibilityModule();
			module.setConsideredActivityType(activityType);
			module.addAdditionalFacilityData(densityFacilities);
			module.setPushing2Geoserver(push2Geoserver);
			controler.addOverridingModule(module);
		}
		
		controler.run();
		
		if (createQGisOutput) {
			final boolean includeDensityLayer = false;
			final Integer range = 9; // In the current implementation, this must always be 9
			final Double lowerBound = -3.5; // (upperBound - lowerBound) ideally nicely divisible by (range - 2)
			final Double upperBound = 3.5;
			final int populationThreshold = (int) (0 / (1000/cellSize * 1000/cellSize));
			
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