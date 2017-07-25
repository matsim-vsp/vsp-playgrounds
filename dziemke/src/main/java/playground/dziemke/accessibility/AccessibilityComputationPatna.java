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
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup.AreaOfAccesssibilityComputation;
import org.matsim.contrib.accessibility.AccessibilityModule;
import org.matsim.contrib.accessibility.FacilityTypes;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.accessibility.utils.AccessibilityFacilityUtils;
import org.matsim.contrib.accessibility.utils.AccessibilityUtils;
import org.matsim.contrib.accessibility.utils.VisualizationUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.matsim.facilities.ActivityFacilities;

import com.vividsolutions.jts.geom.Envelope;

/**
 * @author dziemke
 */
public class AccessibilityComputationPatna {
	public static final Logger LOG = Logger.getLogger(AccessibilityComputationPatna.class);
	
	public static void main(String[] args) throws IOException {
		Double cellSize = 1000.;
		boolean push2Geoserver = false; // set true for run on server
		boolean createQGisOutput = true; // set false for run on server

		final Config config = ConfigUtils.createConfig(new AccessibilityConfigGroup());
		String scenarioCRS = "EPSG:24345"; // says Amit
		Envelope envelope = new Envelope(306000,326000,2829000,2837000); // Notation: minX, maxX, minY, maxY
		
		// Network file
		String networkFile = "../../runs-svn/patnaIndia/run108/jointDemand/policies/0.15pcu/bau/output_network.xml.gz";
		
		// ---------- Input (directly from OSM)
		CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(scenarioCRS, "EPSG:4326");
		Coord southwest = transformation.transform(new Coord(envelope.getMinX(), envelope.getMinY()));
		Coord northeast = transformation.transform(new Coord(envelope.getMaxX(), envelope.getMaxY()));
		URL osm = new URL("http://overpass.osm.rambler.ru/cgi/xapi_meta?*[bbox=" + southwest.getX() + "," + southwest.getY() + "," + northeast.getX() + "," + northeast.getY() +"]");
//		HttpURLConnection connection = (HttpURLConnection) osm.openConnection();
		HttpURLConnection connection2 = (HttpURLConnection) osm.openConnection(); // TODO There might be more elegant option without creating this twice
//	    Network network = createNetwork(connection.getInputStream(), scenarioCRS);
		double buildingTypeFromVicinityRange = 0.;
		ActivityFacilities facilities = AccessibilityFacilityUtils.createFacilites(connection2.getInputStream(), scenarioCRS, buildingTypeFromVicinityRange);
		
		config.controler().setOutputDirectory("../../runs-svn/patnaIndia/run108/jointDemand/policies/0.15pcu/bau/accessibiliities/");
		
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(0);
		config.controler().setRunId("in_patna_" + cellSize.toString().split("\\.")[0]);
		
		config.global().setCoordinateSystem(scenarioCRS);
	
		AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class);
		acg.setCellSizeCellBasedAccessibility(cellSize.intValue());
		acg.setComputingAccessibilityForMode(Modes4Accessibility.freespeed, true);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.bike, true);
		acg.setOutputCrs(scenarioCRS);
		
		acg.setAreaOfAccessibilityComputation(AreaOfAccesssibilityComputation.fromNetwork);
		
		ConfigUtils.setVspDefaults(config);
		
		MutableScenario scenario = (MutableScenario) ScenarioUtils.loadScenario(config);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario.getNetwork());
		networkReader.readFile(networkFile);
		
		scenario.setActivityFacilities(facilities);
//		scenario.setNetwork(network);

		// Activity types
//		final List<String> activityTypes = Arrays.asList(new String[]{FacilityTypes.SHOPPING, FacilityTypes.LEISURE, FacilityTypes.OTHER, FacilityTypes.EDUCATION});
		final List<String> activityTypes = Arrays.asList(new String[]{FacilityTypes.EDUCATION});
		
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
			final Double lowerBound = -13.0; // (upperBound - lowerBound) ideally nicely divisible by (range - 2)
			final Double upperBound = 1.0;
			final int populationThreshold = (int) (0 / (1000/cellSize * 1000/cellSize));
			
			String osName = System.getProperty("os.name");
			String workingDirectory = config.controler().getOutputDirectory();
			for (String actType : activityTypes) {
				String actSpecificWorkingDirectory = workingDirectory + actType + "/";
				for (Modes4Accessibility mode : acg.getIsComputingMode()) {
					VisualizationUtils.createQGisOutput(actType, mode.toString(), envelope, workingDirectory,
							scenarioCRS, includeDensityLayer, lowerBound, upperBound, range, cellSize.intValue(),
							populationThreshold);
					VisualizationUtils.createSnapshot(actSpecificWorkingDirectory, mode.toString(), osName);
				}
			}  
		}
	}
}