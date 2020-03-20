package playground.dziemke.accessibility;

import org.locationtech.jts.geom.Envelope;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.accessibility.*;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup.AreaOfAccesssibilityComputation;
import org.matsim.contrib.accessibility.utils.VisualizationUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * Compute accessibility based on Berlin scenario version 5.0
 * 
 * @author dziemke
 */
public class AccessibilityComputationBerlinV3 {
	
	public static void main(String[] args) {
//		Config config = ConfigUtils.loadConfig("../../matsim-berlin/scenarios/berlin-v5.0-1pct-2018-06-18/input/berlin-5.0_config.xml");
		Config config = ConfigUtils.loadConfig("../../shared-svn/projects/accessibility_berlin/scenarios/berlin-v5.0-1pct-2018-06-18/input/berlin-5.0_config.xml");
//		Config config = ConfigUtils.loadConfig("../../public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.2-1pct/input/berlin-v5.2-1pct.config_dz.xml");
		
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
//		config.controler().setOutputDirectory("../../shared-svn/projects/accessibility_berlin/output/v3/500_car_mall_2/");
//		config.controler().setOutputDirectory("../../shared-svn/projects/accessibility_berlin/output/v3/500_at_rebal_180_mall/");
//		config.controler().setOutputDirectory("../../shared-svn/projects/accessibility_berlin/output/v3_v521-1pct/500_car_edu/");
//		config.controler().setOutputDirectory("../../shared-svn/projects/accessibility_berlin/output/v3/500_at_edu_neu_4/");
//		config.controler().setOutputDirectory("../../shared-svn/projects/accessibility_berlin/output/v3/500_at_rebal_180_edu/");
		config.controler().setOutputDirectory("../../shared-svn/projects/accessibility_berlin/output/v3/500_pt_mall/");
		
		// More capacity
//		config.qsim().setFlowCapFactor(1.4*config.qsim().getFlowCapFactor());
//		config.qsim().setStorageCapFactor(1.4*config.qsim().getStorageCapFactor());
		//

//		File opportunitiesFile = new File("../../shared-svn/projects/accessibility_berlin/osm/berlin/amenities/2018-05-30/facilities.xml");
		File opportunitiesFile = new File("../../shared-svn/projects/accessibility_berlin/osm/berlin/amenities/2018-05-30/facilities_classified.xml");
		config.facilities().setInputFile(opportunitiesFile.getAbsolutePath());
		
		config.subtourModeChoice().setProbaForRandomSingleTripMode(0.5);

		config.transitRouter().setCacheTree(true);
		
		config.plansCalcRoute().setInsertingAccessEgressWalk(true);
		config.qsim().setUsingTravelTimeCheckInTeleportation(true);
		config.qsim().setTrafficDynamics(TrafficDynamics.kinematicWaves);

		ConfigUtils.setVspDefaults(config);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);

		// Accessibility configurations
		int tileSize_m = 500;
		boolean push2Geoserver = false; // Set true for run on server
		boolean createQGisOutput = true; // Set false for run on server
		
//		extend of measure points: 4574397.18, 4619397.18, 5802067.102, 5838567.102
		final Envelope envelope = new Envelope(4574000, 4620000, 5802000, 5839000); // Berlin; notation: minX, maxX, minY, maxY
//		final Envelope envelope = new Envelope(4570000, 4624000, 5796000, 5845000); // Berlin; notation: minX, maxX, minY, maxY
		String scenarioCRS = "EPSG:31468"; // EPSG:31468 = DHDN GK4
		
		AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class);
//		acg.setTimeOfDay(16*60.*60.);
		acg.setEnvelope(envelope);
//		acg.setAreaOfAccessibilityComputation(AreaOfAccesssibilityComputation.fromShapeFile);
//		acg.setShapeFileCellBasedAccessibility("../../shared-svn/studies/countries/de/open_berlin_scenario/input/shapefiles/2013/Berlin_DHDN_GK4.shp");
		acg.setAreaOfAccessibilityComputation(AreaOfAccesssibilityComputation.fromFacilitiesFile);
		File measurePointsFile = new File("../../shared-svn/projects/accessibility_berlin/av/waittimes_500_access_grid/facilities.xml");
//		File measurePointsFile = new File("../../shared-svn/projects/accessibility_berlin/av/waittimes_500_access_grid_rebalancing/facilities.xml");
//		File measurePointsFile = new File("../../shared-svn/projects/accessibility_berlin/av/waittimes_500_access_grid_rebal_180/facilities.xml");
		acg.setMeasuringPointsFile(measurePointsFile.getAbsolutePath());
		acg.setEnvelope(envelope);
		acg.setTileSize_m(tileSize_m);
//		acg.setComputingAccessibilityForMode(Modes4Accessibility.walk, true);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.freespeed, false);
//		acg.setComputingAccessibilityForMode(Modes4Accessibility.car, true);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.pt, true);
		acg.setOutputCrs(scenarioCRS);
		
//		final List<String> activityTypes = Arrays.asList(new String[]{FacilityTypes.EDUCATION, "s"});
//		final List<String> activityTypes = Arrays.asList(new String[]{FacilityTypes.EDUCATION});
		final List<String> activityTypes = Arrays.asList(new String[]{FacilityTypes.MALL});
//		final List<String> activityTypes = Arrays.asList(new String[]{"s"});
		
		final ActivityFacilities densityFacilities = AccessibilityUtils.createFacilityForEachLink(Labels.DENSITIY, scenario.getNetwork() ); // will be aggregated in downstream code!
		
		Controler controler = new Controler(scenario);
		
		// Use sbb pt raptor router
//		controler.addOverridingModule(new AbstractModule() {
//			@Override
//			public void install() {
//				install(new SwissRailRaptorModule());
//			}
//		});
		
		// Use the (congested) car travel time for the teleported ride mode
		controler.addOverridingModule(new AbstractModule(){
			@Override
			public void install() {
				addTravelTimeBinding(TransportMode.ride).to(networkTravelTime());
				addTravelDisutilityFactoryBinding(TransportMode.ride).to(carTravelDisutilityFactoryKey());        }
	    });
		
		// Accessibility module
		for (String activityType : activityTypes) {
			AccessibilityModule module = new AccessibilityModule();
			module.setConsideredActivityType(activityType);
			module.addAdditionalFacilityData(densityFacilities);
			module.setPushing2Geoserver(push2Geoserver);
			module.setCreateQGisOutput(createQGisOutput);
			controler.addOverridingModule(module);
		}
		controler.run();
		
		// Accessibility visualizations in QGis
		if (createQGisOutput) {
//			final boolean includeDensityLayer = true;
			final Integer range = 9; // In the current implementation, this must always be 9
			final Double lowerBound = -3.5; // (upperBound - lowerBound) ideally nicely divisible by (range - 2)
			final Double upperBound = 3.5;
			final int populationThreshold = (int) (0 / (1000/tileSize_m * 1000/tileSize_m));
			
			String osName = System.getProperty("os.name");
			String workingDirectory = config.controler().getOutputDirectory();
			for (String actType : activityTypes) {
				String actSpecificWorkingDirectory = workingDirectory + actType + "/";
				for (Modes4Accessibility mode : acg.getIsComputingMode()) {
					VisualizationUtils.createQGisOutputRuleBasedStandardColorRange(actType, mode.toString(), envelope, workingDirectory,
							scenarioCRS, lowerBound, upperBound, range, populationThreshold);
//					VisualizationUtils.createQGisOutputGraduatedStandardColorRange(actType, mode.toString(), envelope, workingDirectory,
//							scenarioCRS, includeDensityLayer, lowerBound, upperBound, range, tileSize_m, populationThreshold);
					VisualizationUtils.createSnapshot(actSpecificWorkingDirectory, mode.toString(), osName);
				}
			}
		}
	}
}
