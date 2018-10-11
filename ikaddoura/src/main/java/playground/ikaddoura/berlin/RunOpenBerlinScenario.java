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
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import playground.ikaddoura.analysis.IKAnalysisRun;
import playground.ikaddoura.analysis.modalSplitUserType.AgentAnalysisFilter;
import playground.ikaddoura.analysis.modalSplitUserType.ModalSplitUserTypeControlerListener;
import playground.ikaddoura.durationBasedTimeAllocationMutator.DurationBasedTimeAllocationPlanStrategyProvider;
import playground.vsp.analysis.modules.modalAnalyses.modalShare.ModalShareControlerListener;
import playground.vsp.analysis.modules.modalAnalyses.modalShare.ModalShareEventHandler;

/**
* @author ikaddoura
*/

public class RunOpenBerlinScenario {

	private static final Logger log = Logger.getLogger(RunOpenBerlinScenario.class);

	private static String configFile;
	private static String outputDirectory;
	private static String runId;
	private static String visualizationScriptInputDirectory;
	
	private static boolean useCarTravelTimeForRide;
	private static boolean useSBBptRouter;
	private static boolean useDurationBasedTimeAllocationMutator;
	private static double probaForRandomSingleTripMode;
	private static boolean useVSPdefaults;
	
	private static double ascCar;
	private static double ascPt;
	private static double ascTransitWalk;
	private static double ascWalk;
	private static double ascBicycle;
	private static double ascRide;
	
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
			
			useCarTravelTimeForRide = Boolean.parseBoolean(args[4]);
			log.info("useCarTravelTimeForRide: "+ useCarTravelTimeForRide);
			
			useSBBptRouter = Boolean.parseBoolean(args[5]);
			log.info("useSBBptRouter: "+ useSBBptRouter);
			
			useDurationBasedTimeAllocationMutator = Boolean.parseBoolean(args[6]);
			log.info("useDurationBasedTimeAllocationMutator: "+ useDurationBasedTimeAllocationMutator);
			
			probaForRandomSingleTripMode = Double.parseDouble(args[7]);
			log.info("probaForRandomSingleTripMode: "+ probaForRandomSingleTripMode);
			
			useVSPdefaults = Boolean.parseBoolean(args[8]);
			log.info("useVSPdefaults: "+ useVSPdefaults);
			
			ascCar = Double.parseDouble(args[9]);
			log.info("ascCar: "+ ascCar);

			ascPt = Double.parseDouble(args[10]);
			log.info("ascPt: "+ ascPt);
			
			ascTransitWalk = Double.parseDouble(args[11]);
			log.info("ascTransitWalk: "+ ascTransitWalk);

			ascWalk = Double.parseDouble(args[12]);
			log.info("ascWalk: "+ ascWalk);

			ascBicycle = Double.parseDouble(args[13]);
			log.info("ascBicycle: "+ ascBicycle);
			
			ascRide = Double.parseDouble(args[14]);
			log.info("ascRide: "+ ascRide);

		} else {
			
			configFile = "/Users/ihab/Documents/workspace/matsim-project/examples/scenarios/equil/config.xml";
			outputDirectory = "/Users/ihab/Desktop/test-run-equil/";
			runId = "test-run";
		}
		
		RunOpenBerlinScenario runner = new RunOpenBerlinScenario();
		runner.run();
	}

	public void run() {
		
		Config config = ConfigUtils.loadConfig(configFile);
		
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.failIfDirectoryExists);
		config.controler().setOutputDirectory(outputDirectory);
		config.controler().setRunId(runId);
		
		final String STRATEGY_NAME = "durationBasedTimeMutator";

		if (useDurationBasedTimeAllocationMutator) {
			// add own time allocation mutator strategy to config
			StrategySettings stratSets = new StrategySettings();
			stratSets.setStrategyName(STRATEGY_NAME);
			stratSets.setWeight(0.1);
			stratSets.setSubpopulation("person");
			config.strategy().addStrategySettings(stratSets);
		}
		
		config.subtourModeChoice().setProbaForRandomSingleTripMode(probaForRandomSingleTripMode);
		
		if (useVSPdefaults) {
			config.plansCalcRoute().setInsertingAccessEgressWalk(true);
			config.qsim().setUsingTravelTimeCheckInTeleportation(true);
			config.qsim().setTrafficDynamics(TrafficDynamics.kinematicWaves);
		}
		
		config.planCalcScore().getModes().get(TransportMode.car).setConstant(ascCar);
		config.planCalcScore().getModes().get(TransportMode.pt).setConstant(ascPt);
		config.planCalcScore().getModes().get(TransportMode.transit_walk).setConstant(ascTransitWalk);
		config.planCalcScore().getModes().get(TransportMode.walk).setConstant(ascWalk);
		config.planCalcScore().getModes().get("bicycle").setConstant(ascBicycle);
		config.planCalcScore().getModes().get(TransportMode.ride).setConstant(ascRide);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		
		if (useDurationBasedTimeAllocationMutator) {
			// add own time allocation mutator strategy to controler
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					addPlanStrategyBinding(STRATEGY_NAME).toProvider(DurationBasedTimeAllocationPlanStrategyProvider.class);
				}
			});
		}
		
		if (useSBBptRouter) {
			// use the sbb pt raptor router
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					install(new SwissRailRaptorModule());
				}
			});
		}
		
		if (useCarTravelTimeForRide) {
			// use the (congested) car travel time for the teleported ride mode
			controler.addOverridingModule(new AbstractModule(){
				@Override
				public void install() {
					addTravelTimeBinding(TransportMode.ride).to(networkTravelTime());
					addTravelDisutilityFactoryBinding(TransportMode.ride).to(carTravelDisutilityFactoryKey());        }
		    });
		}
		
		// some online analysis
		
		controler.addOverridingModule(new AbstractModule() {
			
			@Override
			public void install() {
				this.bind(ModalShareEventHandler.class);
				this.addControlerListenerBinding().to(ModalShareControlerListener.class);
				
				this.addControlerListenerBinding().to(ModalSplitUserTypeControlerListener.class);
			}
		});
		
				
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

