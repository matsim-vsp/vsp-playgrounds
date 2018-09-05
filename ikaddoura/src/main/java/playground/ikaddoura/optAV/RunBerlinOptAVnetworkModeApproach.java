/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.ikaddoura.optAV;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.contrib.av.robotaxi.scoring.TaxiFareConfigGroup;
import org.matsim.contrib.decongestion.DecongestionConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.contrib.noise.utils.MergeNoiseCSVFile;
import org.matsim.contrib.noise.utils.ProcessNoiseImmissions;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.run.TaxiModule;
import org.matsim.contrib.taxi.run.examples.TaxiDvrpModules;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.run.RunBerlinScenario;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

import playground.ikaddoura.analysis.IKAnalysisRun;
import playground.ikaddoura.analysis.modalSplitUserType.AgentAnalysisFilter;
import playground.ikaddoura.analysis.modalSplitUserType.ModalSplitUserTypeControlerListener;

/**
* @author ikaddoura
*/

public class RunBerlinOptAVnetworkModeApproach {

	private static final Logger log = Logger.getLogger(RunBerlinOptAVnetworkModeApproach.class);

	private static String baseDirectory;
	private static String configFile;
	private static String outputDirectory;
	private static String runId;
	private static String visualizationScriptInputDirectory;
	private static String berlinShapeFile;

	private static final boolean otfvis = false;
	
	private final Map<Integer, Geometry> zoneId2geometry = new HashMap<Integer, Geometry>();
	
	public static void main(String[] args) {
		if (args.length > 0) {
			throw new RuntimeException();
			
		} else {
			
			baseDirectory = "/Users/ihab/Documents/workspace/runs-svn/b5_optAV_networkModeApproach/";
			runId = "test1";

			configFile = baseDirectory + "scenarios/berlin-v5.2-1pct/input/berlin-v5.2-1pct.config_test.xml";
			outputDirectory = baseDirectory + "scenarios/berlin-v5.2-1pct/local-run_" + runId + "/";
			visualizationScriptInputDirectory = baseDirectory + "visualization-scripts";
			berlinShapeFile = baseDirectory + "berlin-shp/berlin.shp";
		}
		
		RunBerlinOptAVnetworkModeApproach runner = new RunBerlinOptAVnetworkModeApproach();
		runner.run();
	}

	public void run() {
	
		RunBerlinScenario berlin = new RunBerlinScenario( configFile, null );
		ConfigGroup[] customModules = {
				new OptAVConfigGroup(),
				new TaxiConfigGroup(),
				new DvrpConfigGroup(),
				new TaxiFareConfigGroup(),
				new OTFVisConfigGroup(),
				new NoiseConfigGroup(),
				new DecongestionConfigGroup()};
		Config config = berlin.prepareConfig(customModules);
		
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.failIfDirectoryExists);
		config.controler().setOutputDirectory(outputDirectory);
		config.controler().setRunId(runId);
		
		Scenario scenario = berlin.prepareScenario();
		
		loadShapeFile();
		int counterLinksInBerlin = 0;
		int counterLinksInBrandenburg = 0;
		// network mode adjustments
		for (Link link : scenario.getNetwork().getLinks().values()) {
			if (link.getAllowedModes().contains(TransportMode.car) && link.getAllowedModes().contains(TransportMode.ride) && link.getAllowedModes().contains("freight")) {
				Set<String> allowedModes = new HashSet<>();
				allowedModes.add("freight");
				allowedModes.add(TransportMode.ride);
				
				// "car" = mode used by taxis; should be allowed everywhere
				allowedModes.add(TransportMode.car);
				
				// "car_br" = mode used by cars; should not be allowed in berlin (except for P+R access roads TODO)
				if ( isCoordInArea(link.getFromNode().getCoord()) || isCoordInArea(link.getToNode().getCoord()) ) {
					// from or to node in berlin
					counterLinksInBerlin++;
				} else {
					allowedModes.add(TransportMode.car + "_br");
					counterLinksInBrandenburg++;
				}
				
				link.setAllowedModes(allowedModes);
			
			} else if (link.getAllowedModes().contains(TransportMode.pt)) {	
				// skip pt links
			} else {
				throw new RuntimeException("Aborting...");
			}
		}
		
		log.info("links in Berlin: " + counterLinksInBerlin);
		log.info("links in Brandenburg: " + counterLinksInBrandenburg);

		new NetworkWriter(scenario.getNetwork()).write(baseDirectory + "adjusted-network.xml.gz");
				
		// rename car to car_br (remaining car trips in plans file are only trips within brandenburg)
		for (Person person : scenario.getPopulation().getPersons().values()) {
			for (Leg leg : TripStructureUtils.getLegs(person.getSelectedPlan().getPlanElements())) {
				if (leg.getMode().equals("car")) {
					leg.setMode("car_br");
				}
			}
		}
		
		// remove activity link Ids
		for (Person person : scenario.getPopulation().getPersons().values()) {
			for (PlanElement pE : person.getSelectedPlan().getPlanElements()) {
				if (pE instanceof Activity) {
					Activity act = (Activity) pE;
					act.setLinkId(null);
				}
			}
		}
		
		new PopulationWriter(scenario.getPopulation()).write(baseDirectory + "adjusted-population.xml.gz");
		
		Controler controler = berlin.prepareControler();
		
		// some online analysis
		controler.addOverridingModule(new AbstractModule() {
			
			@Override
			public void install() {				
				this.addControlerListenerBinding().to(ModalSplitUserTypeControlerListener.class);
			}
		});
		
		// taxi-related modules
		controler.addOverridingModule(TaxiDvrpModules.create());
		controler.addOverridingModule(new TaxiModule());
		
		// optAV module
		controler.addOverridingModule(new OptAVModule(scenario));
		
		// otfvis
		if (otfvis) controler.addOverridingModule(new OTFVisLiveModule());	
		
		berlin.run();
		
		// some offline analysis
		
		log.info("Running offline analysis...");
				
		final String scenarioCRS = TransformationFactory.DHDN_GK4;	
		final String shapeFileZones = null;
		final String zonesCRS = null;
		final String homeActivity = "home";
		final int scalingFactor = 10;
		
		List<AgentAnalysisFilter> filters = new ArrayList<>();

		AgentAnalysisFilter filter1 = new AgentAnalysisFilter(scenario);
		filter1.setPersonAttribute("berlin");
		filter1.setPersonAttributeName("home-activity-zone");
		filter1.preProcess(scenario);
		filters.add(filter1);
		
		AgentAnalysisFilter filter2 = new AgentAnalysisFilter(scenario);
		filter2.preProcess(scenario);
		filters.add(filter2);
		
		AgentAnalysisFilter filter3 = new AgentAnalysisFilter(scenario);
		filter3.setPersonAttribute("brandenburg");
		filter3.setPersonAttributeName("home-activity-zone");
		filter3.preProcess(scenario);
		filters.add(filter3);

		IKAnalysisRun analysis = new IKAnalysisRun(
				scenario,
				null,
				visualizationScriptInputDirectory,
				scenarioCRS,
				shapeFileZones,
				zonesCRS,
				homeActivity,
				scalingFactor,
				filters,
				null);
		analysis.run();
		
		// noise post-analysis
		
		OptAVConfigGroup optAVParams = ConfigUtils.addOrGetModule(controler.getConfig(), OptAVConfigGroup.class);
		if (optAVParams.isAccountForNoise()) {
			String immissionsDir = controler.getConfig().controler().getOutputDirectory() + "/ITERS/it." + controler.getConfig().controler().getLastIteration() + "/immissions/";
			String receiverPointsFile = controler.getConfig().controler().getOutputDirectory() + "/receiverPoints/receiverPoints.csv";
				
			NoiseConfigGroup noiseParams = ConfigUtils.addOrGetModule(controler.getConfig(), NoiseConfigGroup.class);

			ProcessNoiseImmissions processNoiseImmissions = new ProcessNoiseImmissions(immissionsDir, receiverPointsFile, noiseParams.getReceiverPointGap());
			processNoiseImmissions.run();
				
			final String[] labels = { "immission", "consideredAgentUnits" , "damages_receiverPoint" };
			final String[] workingDirectories = { controler.getConfig().controler().getOutputDirectory() + "/ITERS/it." + controler.getConfig().controler().getLastIteration() + "/immissions/" , controler.getConfig().controler().getOutputDirectory() + "/ITERS/it." + controler.getConfig().controler().getLastIteration() + "/consideredAgentUnits/" , controler.getConfig().controler().getOutputDirectory() + "/ITERS/it." + controler.getConfig().controler().getLastIteration()  + "/damages_receiverPoint/" };
		
			MergeNoiseCSVFile merger = new MergeNoiseCSVFile() ;
			merger.setReceiverPointsFile(receiverPointsFile);
			merger.setOutputDirectory(controler.getConfig().controler().getOutputDirectory() + "/ITERS/it." + controler.getConfig().controler().getLastIteration() + "/");
			merger.setTimeBinSize(noiseParams.getTimeBinSizeNoiseComputation());
			merger.setWorkingDirectory(workingDirectories);
			merger.setLabel(labels);
			merger.run();
		}
	
		log.info("Done.");
		
	}
	
	private void loadShapeFile() {		
		Collection<SimpleFeature> features;
		features = ShapeFileReader.getAllFeatures(berlinShapeFile);
		int featureCounter = 0;
		for (SimpleFeature feature : features) {
			zoneId2geometry.put(featureCounter, (Geometry) feature.getDefaultGeometry());
			featureCounter++;
		}
	}
	
	private boolean isCoordInArea(Coord coord) {
		boolean coordInArea = false;
		for (Geometry geometry : zoneId2geometry.values()) {
			Point p = MGC.coord2Point(coord); 
			
			if (p.within(geometry)) {
				coordInArea = true;
			}
		}
		return coordInArea;
	}

}

