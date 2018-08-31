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

package playground.ikaddoura.berlin;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.decongestion.DecongestionConfigGroup;
import org.matsim.contrib.decongestion.DecongestionModule;
import org.matsim.contrib.decongestion.routing.TollTimeDistanceTravelDisutilityFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import playground.ikaddoura.analysis.IKAnalysisRun;
import playground.ikaddoura.analysis.modalSplitUserType.AgentAnalysisFilter;
import playground.ikaddoura.analysis.modalSplitUserType.ModalSplitUserTypeControlerListener;
import playground.vsp.analysis.modules.modalAnalyses.modalShare.ModalShareControlerListener;
import playground.vsp.analysis.modules.modalAnalyses.modalShare.ModalShareEventHandler;
import playground.vsp.congestion.controler.AdvancedMarginalCongestionPricingContolerListener;
import playground.vsp.congestion.handlers.CongestionHandlerImplV10;
import playground.vsp.congestion.handlers.CongestionHandlerImplV3;
import playground.vsp.congestion.handlers.CongestionHandlerImplV9;
import playground.vsp.congestion.handlers.TollHandler;
import playground.vsp.congestion.routing.CongestionTollTimeDistanceTravelDisutilityFactory;

/**
* @author ikaddoura
*/

public class RunBerlin {

	private static final Logger log = Logger.getLogger(RunBerlin.class);

	private static String configFile;
	private static String outputDirectory;
	private static String runId;
	private static String visualizationScriptInputDirectory;
	private static boolean activateDecongestionPricing;
	private static boolean activateQueueBasedCongestionPricing;
	private static String queueBasedCongestionPricingApproach;
	private static double blendFactorQCP;
	
	public static void main(String[] args) {
		if (args.length > 0) {
			
			configFile = args[0];		
			log.info("configFile: "+ configFile);
			
			outputDirectory = args[1];
			log.info("outputDirectory: "+ outputDirectory);
			
			runId = args[2];
			log.info("runId: "+ runId);
			
			visualizationScriptInputDirectory = args[3];
			log.info("visualizationScriptInputDirectory: "+ visualizationScriptInputDirectory);
			
			activateDecongestionPricing = Boolean.parseBoolean(args[4]);
			log.info("activateDecongestionPricing: "+ activateDecongestionPricing);
			
			activateQueueBasedCongestionPricing = Boolean.parseBoolean(args[5]);
			log.info("activateQueueBasedCongestionPricing: "+ activateQueueBasedCongestionPricing);
			
			queueBasedCongestionPricingApproach = args[6];
			log.info("queueBasedCongestionPricingApproach: "+ queueBasedCongestionPricingApproach);

			blendFactorQCP = Double.parseDouble(args[7]);
			log.info("blendFactorQCP: "+ blendFactorQCP);
			
		} else {
			
			configFile = "/Users/ihab/Documents/workspace/runs-svn/b5_decongestion/input/berlin-5.0_config_0c_1pct.xml";
			outputDirectory = "/Users/ihab/Documents/workspace/runs-svn/b5_decongestion/output/b5_1pct_1a-test3/";
			runId = "test1";
			visualizationScriptInputDirectory = "./visualization-scripts/";
			activateDecongestionPricing = true;
			activateQueueBasedCongestionPricing = true;
			queueBasedCongestionPricingApproach = "V10";
			blendFactorQCP = 1.0;
		}
		
		RunBerlin runner = new RunBerlin();
		runner.run();
	}

	public void run() {
		
		Config config = null;
		
		if (activateDecongestionPricing) {
			config = ConfigUtils.loadConfig(configFile, new DecongestionConfigGroup());
		} else {
			config = ConfigUtils.loadConfig(configFile);
		}

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.failIfDirectoryExists);
		config.controler().setOutputDirectory(outputDirectory);
		config.controler().setRunId(runId);
		
		config.subtourModeChoice().setProbaForRandomSingleTripMode(0.5);
		
		config.plansCalcRoute().setInsertingAccessEgressWalk(true);
		config.qsim().setUsingTravelTimeCheckInTeleportation(true);
		config.qsim().setTrafficDynamics(TrafficDynamics.kinematicWaves);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		
		// use the sbb pt raptor router
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				install(new SwissRailRaptorModule());
			}
		});
		
		// use the (congested) car travel time for the teleported ride mode
		controler.addOverridingModule(new AbstractModule(){
			@Override
			public void install() {
				addTravelTimeBinding(TransportMode.ride).to(networkTravelTime());
				addTravelDisutilityFactoryBinding(TransportMode.ride).to(carTravelDisutilityFactoryKey());        }
	    });
		
		// some online analysis
		controler.addOverridingModule(new AbstractModule() {
			
			@Override
			public void install() {
				this.addControlerListenerBinding().to(ModalSplitUserTypeControlerListener.class);
			}
		});
		
		if (activateDecongestionPricing) {
			// congestion toll computation
			
			controler.addOverridingModule(new DecongestionModule(scenario));
			
			// toll-adjusted routing
			
			final TollTimeDistanceTravelDisutilityFactory travelDisutilityFactory = new TollTimeDistanceTravelDisutilityFactory();
			travelDisutilityFactory.setSigma(0.);
			
			controler.addOverridingModule(new AbstractModule(){
				@Override
				public void install() {
					this.bindCarTravelDisutilityFactory().toInstance( travelDisutilityFactory );
				}
			});	
		}
		
		if (activateQueueBasedCongestionPricing) {
			
			final TollHandler congestionTollHandlerQBP = new TollHandler(controler.getScenario());
			final CongestionTollTimeDistanceTravelDisutilityFactory factory = new CongestionTollTimeDistanceTravelDisutilityFactory(
					new RandomizingTimeDistanceTravelDisutilityFactory(TransportMode.car, controler.getConfig().planCalcScore()),
					congestionTollHandlerQBP, controler.getConfig().planCalcScore()
				);
			factory.setSigma(0.);
			factory.setBlendFactor(blendFactorQCP);
			
			controler.addOverridingModule(new AbstractModule(){
				@Override
				public void install() {
					this.bindCarTravelDisutilityFactory().toInstance( factory );
				}
			}); 
			
			if (queueBasedCongestionPricingApproach.equals("V3")) {
				controler.addControlerListener(new AdvancedMarginalCongestionPricingContolerListener(controler.getScenario(), congestionTollHandlerQBP, new CongestionHandlerImplV3(controler.getEvents(), controler.getScenario())));
			} else if (queueBasedCongestionPricingApproach.equals("V9")) {
				controler.addControlerListener(new AdvancedMarginalCongestionPricingContolerListener(controler.getScenario(), congestionTollHandlerQBP, new CongestionHandlerImplV9(controler.getEvents(), controler.getScenario())));
			} else if (queueBasedCongestionPricingApproach.equals("V10")) {
				controler.addControlerListener(new AdvancedMarginalCongestionPricingContolerListener(controler.getScenario(), congestionTollHandlerQBP, new CongestionHandlerImplV10(controler.getEvents(), controler.getScenario())));
			} else {
				throw new RuntimeException("Unknown queue based congestion pricing approach. Aborting...");
			}
		}
				
		controler.run();
		
		log.info("Running offline analysis...");
				
		final String scenarioCRS = TransformationFactory.DHDN_GK4;	
		final String shapeFileZones = null;
		final String zonesCRS = null;
		final String homeActivity = "home";
		final int scalingFactor = 10;
		
		List<AgentAnalysisFilter> filters = new ArrayList<>();

		AgentAnalysisFilter filter1 = new AgentAnalysisFilter(scenario);
		filter1.setSubpopulation("person");
		filter1.setPersonAttribute("berlin");
		filter1.setPersonAttributeName("home-activity-zone");
		filter1.preProcess(scenario);
		filters.add(filter1);
		
		AgentAnalysisFilter filter2 = new AgentAnalysisFilter(scenario);
		filter2.preProcess(scenario);
		filters.add(filter2);
		
		AgentAnalysisFilter filter3 = new AgentAnalysisFilter(scenario);
		filter3.setSubpopulation("person");
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
	
		log.info("Done.");
		
	}

}

