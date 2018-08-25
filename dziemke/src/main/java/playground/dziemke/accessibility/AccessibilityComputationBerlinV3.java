package playground.dziemke.accessibility;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup.AreaOfAccesssibilityComputation;
import org.matsim.contrib.accessibility.AccessibilityModule;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.accessibility.utils.AccessibilityUtils;
import org.matsim.contrib.accessibility.utils.VisualizationUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;

import com.vividsolutions.jts.geom.Envelope;

/**
 * Compute accessibility based on Berlin scenario version 5.0
 * 
 * @author dziemke
 */
public class AccessibilityComputationBerlinV3 {
	
	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig("../../matsim-berlin/scenarios/berlin-v5.0-1pct-2018-06-18/input/berlin-5.0_config.xml");

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory("../../shared-svn/projects/accessibility_berlin/output/v3/500_car_edu_16/");

		config.facilities().setInputFile(new File("../../shared-svn/projects/accessibility_berlin/osm/berlin/amenities/2018-05-30/facilities.xml").getAbsolutePath());
		
		config.subtourModeChoice().setProbaForRandomSingleTripMode(0.5);

		config.transitRouter().setCacheTree(true);
		
		config.plansCalcRoute().setInsertingAccessEgressWalk(true);
		config.qsim().setUsingTravelTimeCheckInTeleportation(true);
		config.qsim().setTrafficDynamics(TrafficDynamics.kinematicWaves);

		ConfigUtils.setVspDefaults(config);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);

		// Accessibility configurations
		Double cellSize = 500.;
		boolean push2Geoserver = false; // Set true for run on server
		boolean createQGisOutput = true; // Set false for run on server
		
		final Envelope envelope = new Envelope(4574000, 4620000, 5802000, 5839000); // Berlin; notation: minX, maxX, minY, maxY
		String scenarioCRS = "EPSG:31468"; // EPSG:31468 = DHDN GK4
		
		AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class);
		acg.setTimeOfDay(16*60.*60.);
		acg.setAreaOfAccessibilityComputation(AreaOfAccesssibilityComputation.fromShapeFile);
		acg.setShapeFileCellBasedAccessibility("../../shared-svn/studies/countries/de/open_berlin_scenario/input/shapefiles/2013/Berlin_DHDN_GK4.shp");
		acg.setEnvelope(envelope);
		acg.setCellSizeCellBasedAccessibility(cellSize.intValue());
//		acg.setComputingAccessibilityForMode(Modes4Accessibility.walk, true);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.freespeed, false);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.car, true);
//		acg.setComputingAccessibilityForMode(Modes4Accessibility.pt, true);
		acg.setOutputCrs(scenarioCRS);
		
//		final List<String> activityTypes = Arrays.asList(new String[]{FacilityTypes.EDUCATION, "s"});
//		final List<String> activityTypes = Arrays.asList(new String[]{FacilityTypes.EDUCATION});
		final List<String> activityTypes = Arrays.asList(new String[]{"s"});
		
		final ActivityFacilities densityFacilities = AccessibilityUtils.createFacilityForEachLink(scenario.getNetwork()); // will be aggregated in downstream code!
		
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
			controler.addOverridingModule(module);
		}
		controler.run();
		
		// Accessibility visualizations in QGis
		if (createQGisOutput) {
			final boolean includeDensityLayer = true;
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