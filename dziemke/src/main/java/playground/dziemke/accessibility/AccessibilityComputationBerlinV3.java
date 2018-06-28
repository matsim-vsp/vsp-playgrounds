package playground.dziemke.accessibility;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.accessibility.AccessibilityModule;
import org.matsim.contrib.accessibility.FacilityTypes;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup.AreaOfAccesssibilityComputation;
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

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;

public class AccessibilityComputationBerlinV3 {
	private static final Logger log = Logger.getLogger(AccessibilityComputationBerlinV3.class);
	
	public static void main(String[] args) {
		
		Config config;
		
		if ( args.length==0 || args[0]=="" ) {
//			String configFile = "scenarios/berlin-v5.0-0.1pct-2018-06-18/input/berlin-5.0_config_reduced.xml";
//			String configFile = "../../matsim-berlin/scenarios/berlin-v5.0-0.1pct-2018-06-18/input/berlin-5.0_config_reduced.xml";
			String configFile = "../../matsim-berlin/scenarios/berlin-v5.0-1pct-2018-06-18/input/berlin-5.0_config.xml";
			log.info("config file: " + configFile);
			config = ConfigUtils.loadConfig(configFile);
			
		} else {
			String configFile = args[0];
			log.info("config file: " + configFile);
			config = ConfigUtils.loadConfig(configFile);
		}
		
//		config.controler().setOverwriteFileSetting(OverwriteFileSetting.failIfDirectoryExists);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		
		// Acc
//		config.controler().setOutputDirectory("./data/output/berlin-5.0_1/002_acc_car/");
		config.controler().setOutputDirectory("../../shared-svn/projects/accessibility_berlin/output/v3/500_pt_shop/");
		// End acc
		
//		RunBerlinScenario runBerlinScenario = new RunBerlinScenario();
		AccessibilityComputationBerlinV3 runBerlinScenario = new AccessibilityComputationBerlinV3();
		runBerlinScenario.run(config);
	}

	void run(Config config) {
		
		config.subtourModeChoice().setProbaForRandomSingleTripMode(0.5);
		
		// Acc
		config.facilities().setInputFile(new File("../../shared-svn/projects/accessibility_berlin/osm/berlin/amenities/2018-05-30/facilities.xml").getAbsolutePath());
		// End acc
		
		// ---
		
		Scenario scenario = ScenarioUtils.loadScenario(config);

		// ---
		
		// Acc
		Double cellSize = 500.;
		boolean push2Geoserver = false; // Set true for run on server
		boolean createQGisOutput = true; // Set false for run on server
		
		final Envelope envelope = new Envelope(4574000, 4620000, 5802000, 5839000); // Berlin; notation: minX, maxX, minY, maxY
		String scenarioCRS = "EPSG:31468"; // EPSG:31468 = DHDN GK4
		
		AccessibilityConfigGroup acg = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.class);
//		acg.setAreaOfAccessibilityComputation(AreaOfAccesssibilityComputation.fromBoundingBox);
		acg.setAreaOfAccessibilityComputation(AreaOfAccesssibilityComputation.fromShapeFile);
		acg.setShapeFileCellBasedAccessibility("../../shared-svn/studies/countries/de/open_berlin_scenario/input/shapefiles/2013/Berlin_DHDN_GK4.shp");
		acg.setEnvelope(envelope);
		acg.setCellSizeCellBasedAccessibility(cellSize.intValue());
//		acg.setComputingAccessibilityForMode(Modes4Accessibility.walk, true);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.freespeed, false);
//		acg.setComputingAccessibilityForMode(Modes4Accessibility.car, true);
		acg.setComputingAccessibilityForMode(Modes4Accessibility.pt, true);
		acg.setOutputCrs(scenarioCRS);
		
		ConfigUtils.setVspDefaults(config);
		
//		config.plansCalcRoute().setInsertingAccessEgressWalk(true);
		
		// ---------- Schedule-based pt
//		config.transit().setUseTransit(true);
//		config.transit().setTransitScheduleFile("../../runs-svn/open_berlin_scenario/b5_22e1a/b5_22e1a.output_transitSchedule.xml.gz");
//		config.transit().setVehiclesFile("../../runs-svn/open_berlin_scenario/b5_22e1a/b5_22e1a.output_transitVehicles.xml.gz");
//		config.qsim().setEndTime(100*3600.);
//		
		config.transitRouter().setCacheTree(true);
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
		
//		final List<String> activityTypes = Arrays.asList(new String[]{FacilityTypes.EDUCATION, "s"});
//		final List<String> activityTypes = Arrays.asList(new String[]{FacilityTypes.EDUCATION});
		final List<String> activityTypes = Arrays.asList(new String[]{"s"});
		
		final ActivityFacilities densityFacilities = AccessibilityUtils.createFacilityForEachLink(scenario.getNetwork()); // will be aggregated in downstream code!
		// End acc
		
		Controler controler = new Controler(scenario);
		
		// use the sbb pt raptor router
//		controler.addOverridingModule(new AbstractModule() {
//			@Override
//			public void install() {
//				install(new SwissRailRaptorModule());
//			}
//		});
		
		// use the (congested) car travel time for the teleported ride mode
		controler.addOverridingModule(new AbstractModule(){
			@Override
			public void install() {
				addTravelTimeBinding(TransportMode.ride).to(networkTravelTime());
				addTravelDisutilityFactoryBinding(TransportMode.ride).to(carTravelDisutilityFactoryKey());        }
	    });
		
		// Acc
		for (String activityType : activityTypes) {
			AccessibilityModule module = new AccessibilityModule();
			module.setConsideredActivityType(activityType);
			module.addAdditionalFacilityData(densityFacilities);
			module.setPushing2Geoserver(push2Geoserver);
			controler.addOverridingModule(module);
		}
		// End acc
		
		// vsp defaults
		config.plansCalcRoute().setInsertingAccessEgressWalk(true);
		config.qsim().setUsingTravelTimeCheckInTeleportation(true);
		config.qsim().setTrafficDynamics(TrafficDynamics.kinematicWaves);
		
		controler.run();
		
		// Acc
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
		// End acc
	
		log.info("Done.");
	}
}