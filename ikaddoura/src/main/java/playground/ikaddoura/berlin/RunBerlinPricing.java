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
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.run.RunBerlinScenario;

import playground.ikaddoura.analysis.IKAnalysisRun;
import playground.ikaddoura.analysis.modalSplitUserType.AgentAnalysisFilter;
import playground.ikaddoura.analysis.modalSplitUserType.ModalSplitUserTypeControlerListener;
import playground.vsp.congestion.controler.AdvancedMarginalCongestionPricingContolerListener;
import playground.vsp.congestion.handlers.CongestionHandlerImplV10;
import playground.vsp.congestion.handlers.CongestionHandlerImplV3;
import playground.vsp.congestion.handlers.CongestionHandlerImplV9;
import playground.vsp.congestion.handlers.TollHandler;
import playground.vsp.congestion.routing.CongestionTollTimeDistanceTravelDisutilityFactory;

/**
* @author ikaddoura
*/

public class RunBerlinPricing {

	private static final Logger log = Logger.getLogger(RunBerlinPricing.class);
	private static String configFile;
	private static String outputDirectory;
	private static String runId;
	private static String visualizationScriptInputDirectory;
	private static boolean activateDecongestionPricing;
	private static boolean activateQueueBasedCongestionPricing;
	private static String queueBasedCongestionPricingApproach;
	private static double blendFactorQCP;
	private static int scaleFactor;
	
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
			
			scaleFactor = Integer.parseInt(args[4]);
			log.info("scaleFactor: "+ scaleFactor);
			
			activateDecongestionPricing = Boolean.parseBoolean(args[5]);
			log.info("activateDecongestionPricing: "+ activateDecongestionPricing);
			
			activateQueueBasedCongestionPricing = Boolean.parseBoolean(args[6]);
			log.info("activateQueueBasedCongestionPricing: "+ activateQueueBasedCongestionPricing);
			
			queueBasedCongestionPricingApproach = args[7];
			log.info("queueBasedCongestionPricingApproach: "+ queueBasedCongestionPricingApproach);

			blendFactorQCP = Double.parseDouble(args[8]);
			log.info("blendFactorQCP: "+ blendFactorQCP);
			
			scaleFactor = Integer.parseInt(args[9]);
			log.info("scaleFactor: "+ scaleFactor);
			
		} else {
			
			configFile = "/Users/ihab/Documents/workspace/matsim-berlin/scenarios/berlin-v5.2-1pct/input/berlin-pricing-v5.2-1pct.config.xml";
			runId = "decongestion-1";
			outputDirectory = "/Users/ihab/Documents/workspace/matsim-berlin/scenarios/berlin-v5.2-1pct/output-berlin-v5.2-1pct-" + runId;
			visualizationScriptInputDirectory = "./visualization-scripts/";
			scaleFactor = 100;

			activateDecongestionPricing = true;
			
			activateQueueBasedCongestionPricing = false;
			queueBasedCongestionPricingApproach = "V3";
			blendFactorQCP = 1.0;			
		}
		
		RunBerlinScenario berlin = new RunBerlinScenario( configFile, null );
			
		Config config = berlin.prepareConfig(new DecongestionConfigGroup());	
		
		config.controler().setRunId(runId);
		config.controler().setOutputDirectory(outputDirectory);
		
		Scenario scenario = berlin.prepareScenario();
		Controler controler = berlin.prepareControler();
		
		if (activateDecongestionPricing) {	
			// decongestion
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
		
		// some online analysis
		controler.addOverridingModule(new AbstractModule() {
			
			@Override
			public void install() {
				this.addControlerListenerBinding().to(ModalSplitUserTypeControlerListener.class);
			}
		});
		
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

		berlin.run();
		
		log.info("Running offline analysis...");
		
		final String scenarioCRS = TransformationFactory.DHDN_GK4;	
		final String shapeFileZones = null;
		final String zonesCRS = null;
		final String homeActivity = "home";
		final int scalingFactor = scaleFactor;
		
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
		
		List<String> modes = new ArrayList<>();
		modes.add(TransportMode.car);

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
				null,
				modes);
		analysis.run();
	
		log.info("Done.");
	}

}

