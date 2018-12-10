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
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.VspDefaultsCheckingLevel;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;

import com.vividsolutions.jts.geom.Envelope;


/**
 * @author dziemke
 */
public class AccessibilityComputationBerlin_V2 {
	public static final Logger LOG = Logger.getLogger(AccessibilityComputationBerlin_V2.class);
	
	public static void main(String[] args) {
		Double cellSize = 500.;
		boolean push2Geoserver = false; // Set true for run on server
		boolean createQGisOutput = true; // Set false for run on server
		
//		String runOutputFolder = "../../runs-svn/open_berlin_scenario/b5_22/";
////		String accessibilityOutputDirectory = runOutputFolder + "../accessibilities/";
//		
//		Config config = ConfigUtils.loadConfig(runOutputFolder + "b5_22.output_config_simple_small.xml", new AccessibilityConfigGroup());
//
//		final Envelope envelope = new Envelope(4574000, 4620000, 5802000, 5839000); // Berlin; notation: minX, maxX, minY, maxY
//		String scenarioCRS = "EPSG:31468"; // EPSG:31468 = DHDN GK4
//		
//		config.network().setInputFile("b5_22.output_network.xml.gz");
////		config.network().setInputFile(new File ("../../shared-svn/studies/countries/de/open_berlin_scenario/be_5/network/berlin-car_be_5_withVspAdjustments2018-04-30_network.xml.gz").getAbsolutePath());
//
//		
////		config.plans().setInputFile("b5_22.output_plans.xml.gz");
//		config.plans().setInputFile("b5_22.output_plans_no_links.xml.gz");
//		
//		config.plans().setInputPersonAttributeFile("b5_22.output_personAttributes.xml.gz");
//		
//		config.vspExperimental().setVspDefaultsCheckingLevel(VspDefaultsCheckingLevel.warn);
//		
//		config.vehicles().setVehiclesFile("b5_22.output_vehicles.xml.gz");
//		
//		config.counts().setInputFile("b5_22.output_counts.xml.gz");
//		
//		config.transit().setUseTransit(false);
////		config.transit().setTransitScheduleFile("");
////		config.transit().setVehiclesFile("");
//		
//		config.facilities().setInputFile(new File("../../runs-svn/patnaIndia/run108/jointDemand/policies/0.15pcu/accessibilities/facilities/2017-09-26_facilities.xml").getAbsolutePath());
//		
//		config.vehicles().setVehiclesFile("b5_22.output_vehicles.xml.gz");

		
		

		// Berlin old
		final Config config = ConfigUtils.createConfig(new AccessibilityConfigGroup());

		final Envelope envelope = new Envelope(4574000, 4620000, 5802000, 5839000); // Berlin; notation: minX, maxX, minY, maxY
		String scenarioCRS = "EPSG:31468"; // EPSG:31468 = DHDN GK4
		
//		config.network().setInputFile("../../shared-svn/studies/countries/de/open_berlin_scenario/be_5/network/be_5_network_with-pt-ride-freight.xml.gz");
		config.network().setInputFile("../../shared-svn/studies/countries/de/open_berlin_scenario/be_5/network/berlin-car_be_5_withVspAdjustments2018-04-30_network.xml.gz");
		config.facilities().setInputFile(new File("../../shared-svn/projects/accessibility_berlin/osm/berlin/amenities/2018-05-30/facilities.xml").getAbsolutePath());
		
//		CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation(scenarioCRS, "EPSG:4326");
//		Coord southwest = transformation.transform(new Coord(envelope.getMinX(), envelope.getMinY()));
//		Coord northeast = transformation.transform(new Coord(envelope.getMaxX(), envelope.getMaxY()));
//		Network network = null;
//		ActivityFacilities facilities = null;
//		try {
//			URL osm = new URL("http://overpass-api.de/api/xapi_meta?*[bbox=" + southwest.getX() + "," + southwest.getY() + "," + northeast.getX() + "," + northeast.getY() +"]");
//		    AccessibilityOsmNetworkReader networkReader = new AccessibilityOsmNetworkReader(((HttpURLConnection) osm.openConnection()).getInputStream(), scenarioCRS);
//			networkReader.setKeepPaths(true);
//			networkReader.setIincludeLowHierarchyWays(true);
//			networkReader.createNetwork();
//			network = networkReader.getNetwork();
//			facilities = AccessibilityFacilityUtils.createFacilites(((HttpURLConnection) osm.openConnection()).getInputStream(), scenarioCRS, 0.);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		
		config.controler().setOutputDirectory("../../shared-svn/projects/accessibility_berlin/output/car_500_10min/");
		config.controler().setRunId("de_berlin_" + cellSize.toString().split("\\.")[0]);
		
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(0);
		config.global().setCoordinateSystem(scenarioCRS);
	
		AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class);
//		acg.setAreaOfAccessibilityComputation(AreaOfAccesssibilityComputation.fromBoundingBox);
		acg.setAreaOfAccessibilityComputation(AreaOfAccesssibilityComputation.fromShapeFile);
		acg.setShapeFileCellBasedAccessibility("../../shared-svn/studies/countries/de/open_berlin_scenario/input/shapefiles/2013/Berlin_DHDN_GK4.shp");
		acg.setEnvelope(envelope);
		acg.setTileSize_m(cellSize.intValue());
//		acg.setComputingAccessibilityForMode(Modes4Accessibility.walk, true);
//		acg.setComputingAccessibilityForMode(Modes4Accessibility.freespeed, false);
//		acg.setComputingAccessibilityForMode(Modes4Accessibility.car, true);
//		acg.setComputingAccessibilityForMode(Modes4Accessibility.pt, true);
		acg.setOutputCrs(scenarioCRS);
		
		ConfigUtils.setVspDefaults(config);
		
//		config.plansCalcRoute().setInsertingAccessEgressWalk(true);
		
		// ---------- Schedule-based pt
//		config.transit().setUseTransit(true);
//		config.transit().setTransitScheduleFile("../../runs-svn/open_berlin_scenario/b5_22e1a/b5_22e1a.output_transitSchedule.xml.gz");
//		config.transit().setVehiclesFile("../../runs-svn/open_berlin_scenario/b5_22e1a/b5_22e1a.output_transitVehicles.xml.gz");
//		config.qsim().setEndTime(100*3600.);
//		
//		config.transitRouter().setCacheTree(true);
//		
//		ModeParams ptParams = new ModeParams(TransportMode.transit_walk);
//		config.planCalcScore().addModeParams(ptParams);
		
//		Scenario scenario2 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
//		new MatsimNetworkReader(TransformationFactory.getCoordinateTransformation("EPSG:4326", scenarioCRS), 
//		scenario2.getNetwork()).readFile("../../../shared-svn/projects/maxess/data/nairobi/digital_matatus/matsim_2015-06-16_2/network.xml");
//		MergeNetworks.merge(network, null, scenario2.getNetwork());

		// ... trying to alter settings to drive down the number of "35527609 transfer links to be added".
//		config.transitRouter().setAdditionalTransferTime(additionalTransferTime); // default: 0.0
//		config.transitRouter().setDirectWalkFactor(directWalkFactor); // default: 1.0
//		config.transitRouter().setMaxBeelineWalkConnectionDistance(0.); // default: 100.0
//		config.transitRouter().setExtensionRadius(0.); // default: 200.0
//		config.transitRouter().setSearchRadius(0.); // default: 1000.0
		// ----------
		
		Scenario scenario = ScenarioUtils.loadScenario(config);

//		final List<String> activityTypes = Arrays.asList(new String[]{FacilityTypes.EDUCATION, "s"});
		final List<String> activityTypes = Arrays.asList(new String[]{FacilityTypes.EDUCATION});
//		final List<String> activityTypes = Arrays.asList(new String[]{"s"});
		
		final ActivityFacilities densityFacilities = AccessibilityUtils.createFacilityForEachLink(Labels.DENSITIY, scenario.getNetwork()); // will be aggregated in downstream code!
		
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