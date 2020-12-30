/* *********************************************************************** *
 * project: org.matsim.*
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
package utils;

import static org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType.FastAStarLandmarks;

import java.io.File;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.analysis.SignalAnalysisTool;
import org.matsim.contrib.signals.builder.Signals;
import org.matsim.contrib.signals.controller.laemmerFix.LaemmerConfigGroup;
import org.matsim.contrib.signals.controller.laemmerFix.LaemmerSignalController;
import org.matsim.contrib.signals.controller.sylvia.SylviaConfigGroup;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemControllerData;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlansConfigGroup.HandlingOfPlansWithoutRoutingMode;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.lanes.LanesWriter;

import analysis.TtAnalyzedGeneralResultsWriter;
import analysis.TtGeneralAnalysis;
import analysis.TtListenerToBindGeneralAnalysis;
import analysis.signals.SignalAnalysisListener;
import analysis.signals.SignalAnalysisWriter;
import analysis.signals.TtQueueLengthAnalysisTool;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import signals.downstreamSensor.DownstreamPlanbasedSignalController;
import signals.gershenson.GershensonConfig;
import signals.gershenson.GershensonSignalController;
import signals.laemmerFlex.FullyAdaptiveLaemmerSignalController;

/**
 * @author tthunig
 *
 */
public class TtRunBerlinWithSignals {
	
	private static final Logger log = Logger.getLogger(TtRunBerlinWithSignals.class );
	
	private static final String RUN_ID = "55000";

	public static void main(String[] args) {
//		runWithRandomPop();
//		runWithOpenBerlinPop();
		runOpenBerlinV54FromInput(args[0]);
	}
	
	private static void runOpenBerlinV54FromInput(String configFileName) {
		Config config = ConfigUtils.loadConfig(configFileName) ;
		
		ConfigUtils.addOrGetModule(config, LaemmerConfigGroup.class);
		ConfigUtils.addOrGetModule(config, SylviaConfigGroup.class);
		ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUP_NAME, SignalSystemsConfigGroup.class);

		config.controler().setRoutingAlgorithmType( FastAStarLandmarks );
		
		config.subtourModeChoice().setProbaForRandomSingleTripMode( 0.5 );
		
		config.plansCalcRoute().setRoutingRandomness( 3. );
		config.plansCalcRoute().removeModeRoutingParams(TransportMode.ride);
		config.plansCalcRoute().removeModeRoutingParams(TransportMode.pt);
		config.plansCalcRoute().removeModeRoutingParams(TransportMode.bike);
		config.plansCalcRoute().removeModeRoutingParams("undefined");
	
		config.qsim().setInsertingWaitingVehiclesBeforeDrivingVehicles( true );
				
		// vsp defaults
		config.vspExperimental().setVspDefaultsCheckingLevel( VspExperimentalConfigGroup.VspDefaultsCheckingLevel.info );
		config.plansCalcRoute().setInsertingAccessEgressWalk( true );
		config.qsim().setUsingTravelTimeCheckInTeleportation( true );
		config.qsim().setTrafficDynamics( TrafficDynamics.kinematicWaves );
				
		// activities:
		for ( long ii = 600 ; ii <= 97200; ii+=600 ) {
			config.planCalcScore().addActivityParams( new ActivityParams( "home_" + ii + ".0" ).setTypicalDuration( ii ) );
			config.planCalcScore().addActivityParams( new ActivityParams( "work_" + ii + ".0" ).setTypicalDuration( ii ).setOpeningTime(6. * 3600. ).setClosingTime(20. * 3600. ) );
			config.planCalcScore().addActivityParams( new ActivityParams( "leisure_" + ii + ".0" ).setTypicalDuration( ii ).setOpeningTime(9. * 3600. ).setClosingTime(27. * 3600. ) );
			config.planCalcScore().addActivityParams( new ActivityParams( "shopping_" + ii + ".0" ).setTypicalDuration( ii ).setOpeningTime(8. * 3600. ).setClosingTime(20. * 3600. ) );
			config.planCalcScore().addActivityParams( new ActivityParams( "other_" + ii + ".0" ).setTypicalDuration( ii ) );
		}
		config.planCalcScore().addActivityParams( new ActivityParams( "freight" ).setTypicalDuration( 12.*3600. ) );
		
		// necessary since change in routing modes
		config.plans().setHandlingOfPlansWithoutRoutingMode(HandlingOfPlansWithoutRoutingMode.useMainModeIdentifier);
		
		
		Scenario scenario = ScenarioUtils.loadScenario( config ) ;
		
		SignalSystemsConfigGroup signalsConfigGroup = ConfigUtils.addOrGetModule(config,
				SignalSystemsConfigGroup.GROUP_NAME, SignalSystemsConfigGroup.class);
		
		if (signalsConfigGroup.isUseSignalSystems()) {
			scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());
		}
		
		Controler controler = new Controler( scenario );
		
		// configure signals
		Signals.Configurator configurator = new Signals.Configurator( controler ) ;
				
		// add additional bindings (analysis tools and classes that are necessary for
		// your own implementations, e.g. your own signal controllers, as e.g. the
		// config for Gershenson)
		controler.addOverridingModule(new AbstractModule() {			
			@Override
			public void install() {
				this.bind(TtGeneralAnalysis.class).asEagerSingleton();
				this.addEventHandlerBinding().to(TtGeneralAnalysis.class);
				this.bind(TtAnalyzedGeneralResultsWriter.class);
				this.addControlerListenerBinding().to(TtListenerToBindGeneralAnalysis.class);
				
				if (signalsConfigGroup.isUseSignalSystems()) {
					// bind tool to analyze signals
					this.bind(SignalAnalysisTool.class);
					this.bind(SignalAnalysisWriter.class);
					this.addControlerListenerBinding().to(SignalAnalysisListener.class);
					this.addControlerListenerBinding().to(TtQueueLengthAnalysisTool.class);
					this.addMobsimListenerBinding().to(TtQueueLengthAnalysisTool.class);
				}

			}
		});
		
		if (controler.getConfig().transit().isUsingTransitInMobsim()) {
			// use the sbb pt raptor router
			controler.addOverridingModule( new AbstractModule() {
				@Override
				public void install() {
					install( new SwissRailRaptorModule() );
				}
			} );
		} else {
			log.warn("Public transit will be teleported and not simulated in the mobsim! "
					+ "This will have a significant effect on pt-related parameters (travel times, modal split, and so on). "
					+ "Should only be used for testing or car-focused studies with a fixed modal split.  ");
		}
		
		// use the (congested) car travel time for the teleported ride mode
		controler.addOverridingModule( new AbstractModule() {
			@Override
			public void install() {
				addTravelTimeBinding( TransportMode.ride ).to( networkTravelTime() );
				addTravelDisutilityFactoryBinding( TransportMode.ride ).to( carTravelDisutilityFactoryKey() );
			}
		} );
		
		controler.run();
	}

	private static void runWithOpenBerlinPop() {
		String inputFileDir = "../../svn/shared-svn/studies/tthunig/osmData/";
		String inputFileDirOpenBerlin = inputFileDir + "openBerlinV5.5/";
		String dirNameOsmData = "2020_12_10_berlinBrandenburg";
		
		Config config = ConfigUtils.loadConfig(inputFileDirOpenBerlin + "berlin-v5.5-1pct.config.xml");
		
		config.controler().setLastIteration(0);
		config.controler().setOutputDirectory(inputFileDir + dirNameOsmData + "/output/2020_12_10_berlinPopV5.5_none");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		
		SignalSystemsConfigGroup signalsConfigGroup = 
				prepareConfigRegardingOsmAndSignals( config, dirNameOsmData );
		prepareConfigRegardingOpenBerlin(config);

		Scenario scenario = ScenarioUtils.loadScenario( config ) ;
		
		prepareScenarioRegardingOpenBerlin(scenario);
		
		if (signalsConfigGroup.isUseSignalSystems()) {
			scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());
		}
		
		// transform osm network from EPSG:32633 to GK4
		CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation("EPSG:32633",
//				TransformationFactory.GK4);
				"EPSG:31468");
		for (Node node : scenario.getNetwork().getNodes().values()) {
			node.setCoord(transformation.transform(node.getCoord()));
		}
		new NetworkWriter(scenario.getNetwork()).write(inputFileDir + dirNameOsmData + "/networkGK4.xml"); 

		combineOsmSignalNetworkWithOpenBerlinPtNetwork(scenario, inputFileDirOpenBerlin);
		removeAllNonPtNetworkInfoFromPlans(scenario);
		
//		useAdaptiveSignalControl(scenario);
		
		final Controler controler = new Controler( scenario );
        
		prepareControlerRegardingOsmAndSignals(signalsConfigGroup, controler);
		prepareControlerRegardingOpenBerlin(controler);
		
		writeInput(scenario);
		
//		controler.run();
	}

	private static void writeInput(Scenario scenario) {
		String outputDir = scenario.getConfig().controler().getOutputDirectory() + "/initialFiles/";
		// create directory
		new File(outputDir).mkdirs();
		
		// write network and lanes
		new NetworkWriter(scenario.getNetwork()).write(outputDir + "network.xml");
		// correct paths before writing config
		scenario.getConfig().network().setInputFile("network.xml");
		if (scenario.getConfig().qsim().isUseLanes()) {
			new LanesWriter(scenario.getLanes()).write(outputDir + "lanes.xml");
			scenario.getConfig().network().setLaneDefinitionsFile("lanes.xml");
		}
		String outputDirTmp = scenario.getConfig().controler().getOutputDirectory();
		// adapt output dir to be able to run it on the cluster
		scenario.getConfig().controler().setOutputDirectory("/net/ils3/thunig/runs-svn/berlin_signalsLanes/run" + RUN_ID + "/");
		
		// write population
		new PopulationWriter(scenario.getPopulation()).write(outputDir + "plans.xml");
		scenario.getConfig().plans().setInputFile("plans.xml");
		
		// write signal files
//		SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
//		new SignalSystemsWriter20(signalsData.getSignalSystemsData()).write(outputDir + "signalSystems.xml");
//		new SignalControlWriter20(signalsData.getSignalControlData()).write(outputDir + "signalControl.xml");
//		new SignalGroupsWriter20(signalsData.getSignalGroupsData()).write(outputDir + "signalGroups.xml");
//			
//		SignalSystemsConfigGroup signalsConfigGroup = ConfigUtils.addOrGetModule(scenario.getConfig(),
//				SignalSystemsConfigGroup.GROUP_NAME, SignalSystemsConfigGroup.class);
//		signalsConfigGroup.setSignalSystemFile("signalSystems.xml");
//		signalsConfigGroup.setSignalGroupsFile("signalGroups.xml");
//		signalsConfigGroup.setSignalControlFile("signalControl.xml");
		
		// write config
		new ConfigWriter(scenario.getConfig()).write(outputDir + "config.xml");
		
		// restore output dir
		scenario.getConfig().controler().setOutputDirectory(outputDirTmp);
	}

	private static void useAdaptiveSignalControl(Scenario scenario) {
		SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		for (SignalSystemControllerData control : signalsData.getSignalControlData().getSignalSystemControllerDataBySystemId().values()) {
			control.setControllerIdentifier(LaemmerSignalController.IDENTIFIER);
			// note: fixed time signal plan data is still there but will not be used when the controller identifier is 'laemmer'
		}
	}

	private static void combineOsmSignalNetworkWithOpenBerlinPtNetwork(Scenario scenario, String inputFileDirOpenBerlin) {
		Network openBerlinNetwork = NetworkUtils.createNetwork();
		new MatsimNetworkReader(openBerlinNetwork).readFile(inputFileDirOpenBerlin + "berlin-v5.5-network.xml.gz");
		for (Node node : openBerlinNetwork.getNodes().values()) {
			if (node.getId().toString().startsWith("pt_")) {
				// add node to the osm signals network
				scenario.getNetwork().addNode(node);
			}
		}
		for (Link link : openBerlinNetwork.getLinks().values()) {
			if (link.getId().toString().startsWith("pt_")) {
				// add link to the osm signals network
				scenario.getNetwork().addLink(link);
				if (!scenario.getNetwork().getNodes().containsKey(link.getFromNode().getId())
						|| !scenario.getNetwork().getNodes().containsKey(link.getToNode().getId())) {
					throw new RuntimeException("From or to node of pt link " + link.getId() + " is not a pt node.");
				}
			}
		}
	}

	private static void removeAllNonPtNetworkInfoFromPlans(Scenario scenario) {
		for (Person person : scenario.getPopulation().getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				for (PlanElement pe : plan.getPlanElements()) {
					if (pe instanceof Activity) {
						Activity act = (Activity) pe;
						// delete all link info from non pt network
						if (!act.getLinkId().toString().startsWith("pt_")) {
							act.setLinkId(null);
							if (act.getCoord() == null) {
								throw new RuntimeException("A activity of person " + person.getId() + " does not have a coordinate.");
							}
						}
					} else if (pe instanceof Leg) {
						Leg leg = (Leg) pe;
						// remove all routes that are no pt routes
						if (leg.getMode() != "pt") {
							leg.setRoute(null);
						}
					}
				}
			}
		}
	}

	private static void prepareControlerRegardingOsmAndSignals(SignalSystemsConfigGroup signalsConfigGroup,
			final Controler controler) {
		// configure signals
		Signals.Configurator configurator = new Signals.Configurator( controler ) ;
		// the signals module works for planbased, sylvia and laemmer signal controller
		// by default and is pluggable for your own signal controller like this:
//		configurator.addSignalControllerFactory(DownstreamPlanbasedSignalController.IDENTIFIER,
//				DownstreamPlanbasedSignalController.DownstreamFactory.class);
//		configurator.addSignalControllerFactory(FullyAdaptiveLaemmerSignalController.IDENTIFIER,
//				FullyAdaptiveLaemmerSignalController.LaemmerFlexFactory.class);
//		configurator.addSignalControllerFactory(GershensonSignalController.IDENTIFIER,
//				GershensonSignalController.GershensonFactory.class);

//		// bind gershenson config
//		controler.addOverridingModule(new AbstractModule() {
//			@Override
//			public void install() {
//				GershensonConfig gershensonConfig = new GershensonConfig();
//				bind(GershensonConfig.class).toInstance(gershensonConfig);
//			}
//		});
				
		// add additional bindings (analysis tools and classes that are necessary for
		// your own implementations, e.g. your own signal controllers, as e.g. the
		// config for Gershenson)
		controler.addOverridingModule(new AbstractModule() {			
			@Override
			public void install() {
//				GershensonConfig gershensonConfig = new GershensonConfig();
//				gershensonConfig.setMinimumGREENtime(5);
//				// ... set parameters as you like
//				bind(GershensonConfig.class).toInstance(gershensonConfig);
				
				this.bind(TtGeneralAnalysis.class).asEagerSingleton();
				this.addEventHandlerBinding().to(TtGeneralAnalysis.class);
				this.bind(TtAnalyzedGeneralResultsWriter.class);
				this.addControlerListenerBinding().to(TtListenerToBindGeneralAnalysis.class);
				
				if (signalsConfigGroup.isUseSignalSystems()) {
					// bind tool to analyze signals
					this.bind(SignalAnalysisTool.class);
					this.bind(SignalAnalysisWriter.class);
					this.addControlerListenerBinding().to(SignalAnalysisListener.class);
					this.addControlerListenerBinding().to(TtQueueLengthAnalysisTool.class);
					this.addMobsimListenerBinding().to(TtQueueLengthAnalysisTool.class);
				}

			}
		});
	}

	private static void prepareControlerRegardingOpenBerlin(final Controler controler) {
		if (controler.getConfig().transit().isUsingTransitInMobsim()) {
			// use the sbb pt raptor router
			controler.addOverridingModule( new AbstractModule() {
				@Override
				public void install() {
					install( new SwissRailRaptorModule() );
				}
			} );
		} else {
			log.warn("Public transit will be teleported and not simulated in the mobsim! "
					+ "This will have a significant effect on pt-related parameters (travel times, modal split, and so on). "
					+ "Should only be used for testing or car-focused studies with a fixed modal split.  ");
		}
		
		// TODO berlin v5.4
		// use the (congested) car travel time for the teleported ride mode
		controler.addOverridingModule( new AbstractModule() {
			@Override
			public void install() {
				addTravelTimeBinding( TransportMode.ride ).to( networkTravelTime() );
				addTravelDisutilityFactoryBinding( TransportMode.ride ).to( carTravelDisutilityFactoryKey() );
			}
		} );
		
		// TODO berlin v5.5
//		// use the (congested) car travel time for the teleported ride mode
//		controler.addOverridingModule(new AbstractModule() {
//			@Override
//			public void install() {
//				addTravelTimeBinding(TransportMode.ride).to(networkTravelTime());
//				addTravelDisutilityFactoryBinding(TransportMode.ride).to(carTravelDisutilityFactoryKey());
//				bind(AnalysisMainModeIdentifier.class).to(OpenBerlinIntermodalPtDrtRouterModeIdentifier.class);
//
//				addPlanStrategyBinding("RandomSingleTripReRoute").toProvider(RandomSingleTripReRoute.class);
//				addPlanStrategyBinding("ChangeSingleTripModeAndRoute").toProvider(ChangeSingleTripModeAndRoute.class);
//
//				bind(RaptorIntermodalAccessEgress.class).to(EnhancedRaptorIntermodalAccessEgress.class);
//
//				// use income-dependent marginal utility of money for scoring
//				bind(ScoringParametersForPerson.class).to(IncomeDependentUtilityOfMoneyPersonScoringParameters.class)
//						.in(Singleton.class);
//			}
//		});
	}

	private static SignalSystemsConfigGroup prepareConfigRegardingOsmAndSignals(Config config, String dirNameOsmData) {
		String osmDir = "../" + dirNameOsmData + "/";
		
		ConfigUtils.addOrGetModule(config, LaemmerConfigGroup.class);
		ConfigUtils.addOrGetModule(config, SylviaConfigGroup.class);
		
		config.network().setInputFile(osmDir + "network.xml");
		config.network().setLaneDefinitionsFile(osmDir + "lanes.xml");
		config.qsim().setUseLanes(true);
		config.controler().setLinkToLinkRoutingEnabled(true);
		config.travelTimeCalculator().setCalculateLinkToLinkTravelTimes(true);

		SignalSystemsConfigGroup signalsConfigGroup = ConfigUtils.addOrGetModule(config,
				SignalSystemsConfigGroup.GROUP_NAME, SignalSystemsConfigGroup.class);
		signalsConfigGroup.setSignalControlFile(osmDir + "signalControl.xml");
		signalsConfigGroup.setSignalGroupsFile(osmDir + "signalGroups.xml");
		signalsConfigGroup.setSignalSystemFile(osmDir + "signalSystems.xml");
//		signalsConfigGroup.setUseSignalSystems(true);
		signalsConfigGroup.setUseSignalSystems(false);
		config.qsim().setUsingFastCapacityUpdate(false);
		return signalsConfigGroup;
	}
	
	// TODO berlin v5.5
	public static void prepareScenarioRegardingOpenBerlin( Scenario scenario ) {
//		/*
//		 * We need to set the DrtRouteFactory before loading the scenario. Otherwise DrtRoutes in input plans are loaded
//		 * as GenericRouteImpls and will later cause exceptions in DrtRequestCreator. So we do this here, although this
//		 * class is also used for runs without drt.
//		 */
//		RouteFactories routeFactories = scenario.getPopulation().getFactory().getRouteFactories();
//		routeFactories.setRouteFactory(DrtRoute.class, new DrtRouteFactory());
//		
//		BerlinExperimentalConfigGroup berlinCfg = ConfigUtils.addOrGetModule(config, BerlinExperimentalConfigGroup.class);
//		if (berlinCfg.getPopulationDownsampleFactor() != 1.0) {
//			downsample(scenario.getPopulation().getPersons(), berlinCfg.getPopulationDownsampleFactor());
//		}
//
//		AssignIncome.assignIncomeToPersonSubpopulationAccordingToGermanyAverage(scenario.getPopulation());
	}

	private static void prepareConfigRegardingOpenBerlin(Config config) {
		config.transit().setTransitScheduleFile("berlin-v5.5-transit-schedule.xml.gz");
		config.transit().setVehiclesFile("berlin-v5.5-transit-vehicles.xml.gz");
		config.vehicles().setVehiclesFile("berlin-v5-mode-vehicle-types.xml");
		
		config.controler().setRoutingAlgorithmType( FastAStarLandmarks );
		
		config.subtourModeChoice().setProbaForRandomSingleTripMode( 0.5 );
		
		config.plansCalcRoute().setRoutingRandomness( 3. );
		config.plansCalcRoute().removeModeRoutingParams(TransportMode.ride);
		config.plansCalcRoute().removeModeRoutingParams(TransportMode.pt);
		config.plansCalcRoute().removeModeRoutingParams(TransportMode.bike);
		config.plansCalcRoute().removeModeRoutingParams("undefined");
	
		config.qsim().setInsertingWaitingVehiclesBeforeDrivingVehicles( true );
				
		// vsp defaults
		config.vspExperimental().setVspDefaultsCheckingLevel( VspExperimentalConfigGroup.VspDefaultsCheckingLevel.info );
		config.plansCalcRoute().setInsertingAccessEgressWalk( true );
		config.qsim().setUsingTravelTimeCheckInTeleportation( true );
		config.qsim().setTrafficDynamics( TrafficDynamics.kinematicWaves );
				
		// activities:
		for ( long ii = 600 ; ii <= 97200; ii+=600 ) {
			config.planCalcScore().addActivityParams( new ActivityParams( "home_" + ii + ".0" ).setTypicalDuration( ii ) );
			config.planCalcScore().addActivityParams( new ActivityParams( "work_" + ii + ".0" ).setTypicalDuration( ii ).setOpeningTime(6. * 3600. ).setClosingTime(20. * 3600. ) );
			config.planCalcScore().addActivityParams( new ActivityParams( "leisure_" + ii + ".0" ).setTypicalDuration( ii ).setOpeningTime(9. * 3600. ).setClosingTime(27. * 3600. ) );
			config.planCalcScore().addActivityParams( new ActivityParams( "shopping_" + ii + ".0" ).setTypicalDuration( ii ).setOpeningTime(8. * 3600. ).setClosingTime(20. * 3600. ) );
			config.planCalcScore().addActivityParams( new ActivityParams( "other_" + ii + ".0" ).setTypicalDuration( ii ) );
		}
		config.planCalcScore().addActivityParams( new ActivityParams( "freight" ).setTypicalDuration( 12.*3600. ) );
		
		// necessary since change in routing modes
		config.plans().setHandlingOfPlansWithoutRoutingMode(HandlingOfPlansWithoutRoutingMode.useMainModeIdentifier);

	
		// TODO berlin v5.5 .. RunDrtOpenBerlinScenario.AdditionalInformation.none
//		OutputDirectoryLogging.catchLogEntries();
//		
//		String[] typedArgs = Arrays.copyOfRange( args, 1, args.length );
//		
//		ConfigGroup[] customModulesToAdd = null ;
//		if (additionalInformation == RunDrtOpenBerlinScenario.AdditionalInformation.acceptUnknownParamsBerlinConfig) {
//			customModulesToAdd = new ConfigGroup[]{new BerlinExperimentalConfigGroup(true),
//					new PtExtensionsConfigGroup()};
//		} else {
//			customModulesToAdd = new ConfigGroup[]{new BerlinExperimentalConfigGroup(false),
//					new PtExtensionsConfigGroup()};
//		}
//		ConfigGroup[] customModulesAll = new ConfigGroup[customModules.length + customModulesToAdd.length];
//		
//		int counter = 0;
//		for (ConfigGroup customModule : customModules) {
//			customModulesAll[counter] = customModule;
//			counter++;
//		}
//		
//		for (ConfigGroup customModule : customModulesToAdd) {
//			customModulesAll[counter] = customModule;
//			counter++;
//		}
//		
//		final Config config = ConfigUtils.loadConfig( args[ 0 ], customModulesAll );
//		
//		config.controler().setRoutingAlgorithmType( FastAStarLandmarks );
//		
//		config.subtourModeChoice().setProbaForRandomSingleTripMode( 0.5 );
//		
//		config.plansCalcRoute().setRoutingRandomness( 3. );
//		config.plansCalcRoute().removeModeRoutingParams(TransportMode.ride);
//		config.plansCalcRoute().removeModeRoutingParams(TransportMode.pt);
//		config.plansCalcRoute().removeModeRoutingParams(TransportMode.bike);
//		config.plansCalcRoute().removeModeRoutingParams("undefined");
//		
//		config.qsim().setInsertingWaitingVehiclesBeforeDrivingVehicles( true );
//				
//		// vsp defaults
//		config.vspExperimental().setVspDefaultsCheckingLevel( VspExperimentalConfigGroup.VspDefaultsCheckingLevel.info );
//		config.plansCalcRoute().setAccessEgressType(PlansCalcRouteConfigGroup.AccessEgressType.accessEgressModeToLink);
//		config.qsim().setUsingTravelTimeCheckInTeleportation( true );
//		config.qsim().setTrafficDynamics( TrafficDynamics.kinematicWaves );
//				
//		// activities:
//		for ( long ii = 600 ; ii <= 97200; ii+=600 ) {
//			config.planCalcScore().addActivityParams( new ActivityParams( "home_" + ii + ".0" ).setTypicalDuration( ii ) );
//			config.planCalcScore().addActivityParams( new ActivityParams( "work_" + ii + ".0" ).setTypicalDuration( ii ).setOpeningTime(6. * 3600. ).setClosingTime(20. * 3600. ) );
//			config.planCalcScore().addActivityParams( new ActivityParams( "leisure_" + ii + ".0" ).setTypicalDuration( ii ).setOpeningTime(9. * 3600. ).setClosingTime(27. * 3600. ) );
//			config.planCalcScore().addActivityParams( new ActivityParams( "shopping_" + ii + ".0" ).setTypicalDuration( ii ).setOpeningTime(8. * 3600. ).setClosingTime(20. * 3600. ) );
//			config.planCalcScore().addActivityParams( new ActivityParams( "other_" + ii + ".0" ).setTypicalDuration( ii ) );
//		}
//		config.planCalcScore().addActivityParams( new ActivityParams( "freight" ).setTypicalDuration( 12.*3600. ) );
//
//		ConfigUtils.applyCommandline( config, typedArgs ) ;
		
	}
	
	private static void downsample( final Map<Id<Person>, ? extends Person> map, final double sample ) {
		// TODO berlin v5.5
//		final Random rnd = MatsimRandom.getLocalInstance();
//		log.warn( "Population downsampled from " + map.size() + " agents." ) ;
//		map.values().removeIf( person -> rnd.nextDouble() > sample ) ;
//		log.warn( "Population downsampled to " + map.size() + " agents." ) ;
	}
	
	
	@Deprecated
	private static void runWithRandomPop() {
		String inputFileDir = "../../svn/shared-svn/studies/tthunig/osmData/2020_12_10_berlinBrandenburg/";
		
		Config config = ConfigUtils.createConfig();
		
		config.controler().setLastIteration(0);
		config.controler().setOutputDirectory(inputFileDir + "output/randomPop/adaptive");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		
		ConfigUtils.addOrGetModule(config, LaemmerConfigGroup.class);
		ConfigUtils.addOrGetModule(config, SylviaConfigGroup.class);
		
		config.network().setInputFile(inputFileDir + "network.xml");
		config.network().setLaneDefinitionsFile(inputFileDir + "lanes.xml");
		config.qsim().setUseLanes(true);
		config.controler().setLinkToLinkRoutingEnabled(true);
		config.travelTimeCalculator().setCalculateLinkToLinkTravelTimes(true);

		SignalSystemsConfigGroup signalsConfigGroup = ConfigUtils.addOrGetModule(config,
				SignalSystemsConfigGroup.GROUP_NAME, SignalSystemsConfigGroup.class);
		signalsConfigGroup.setSignalControlFile(inputFileDir + "signalControl.xml");
		signalsConfigGroup.setSignalGroupsFile(inputFileDir + "signalGroups.xml");
		signalsConfigGroup.setSignalSystemFile(inputFileDir + "signalSystems.xml");
		signalsConfigGroup.setUseSignalSystems(true);
		config.qsim().setUsingFastCapacityUpdate(false);
		
		ActivityParams dummyAct = new ActivityParams("dummy");
		dummyAct.setTypicalDuration(12 * 3600);
		config.planCalcScore().addActivityParams(dummyAct);
		
		Scenario scenario = ScenarioUtils.loadScenario( config ) ;
		
		if (signalsConfigGroup.isUseSignalSystems()) {
			scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());
		}
		
		Random random = new Random(4711);
		for (int i=0; i<=10000; i++) {
			PopulationFactory popFac = scenario.getPopulation().getFactory();
			Person p = popFac.createPerson(Id.createPersonId(i));
			
			// create a start activity at the inLink
			Activity startAct = popFac.createActivityFromCoord("dummy", new Coord(random.nextDouble() * 18000 + 382000, 
					random.nextDouble() * 14000 + 5812000));
			// distribute agents uniformly between simulationStartTime and (simulationStartTime + simulationPeriod) am.
			startAct.setEndTime(8*3600);
		
			// create a drain activity at outLink
			Activity drainAct = popFac.createActivityFromCoord("dummy", new Coord(random.nextDouble() * 18000 + 382000, 
					random.nextDouble() * 14000 + 5812000));
			
			Leg leg = popFac.createLeg(TransportMode.car);
			
			Plan plan = popFac.createPlan();
			plan.addActivity(startAct);
			plan.addLeg(leg);
			plan.addActivity(drainAct);
			
			p.addPlan(plan);
			scenario.getPopulation().addPerson(p);
		}
		
		useAdaptiveSignalControl(scenario);
		
		Controler controler = new Controler( scenario );
        
		// configure signals
		Signals.Configurator configurator = new Signals.Configurator( controler ) ;
		// the signals module works for planbased, sylvia and laemmer signal controller
		// by default and is pluggable for your own signal controller like this:
		configurator.addSignalControllerFactory(DownstreamPlanbasedSignalController.IDENTIFIER,
				DownstreamPlanbasedSignalController.DownstreamFactory.class);
		configurator.addSignalControllerFactory(FullyAdaptiveLaemmerSignalController.IDENTIFIER,
				FullyAdaptiveLaemmerSignalController.LaemmerFlexFactory.class);
		configurator.addSignalControllerFactory(GershensonSignalController.IDENTIFIER,
				GershensonSignalController.GershensonFactory.class);

		// bind gershenson config
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				GershensonConfig gershensonConfig = new GershensonConfig();
				bind(GershensonConfig.class).toInstance(gershensonConfig);
			}
		});
				
		// add additional bindings (analysis tools and classes that are necessary for
		// your own implementations, e.g. your own signal controllers, as e.g. the
		// config for Gershenson)
		controler.addOverridingModule(new AbstractModule() {			
			@Override
			public void install() {
				GershensonConfig gershensonConfig = new GershensonConfig();
				gershensonConfig.setMinimumGREENtime(5);
				// ... set parameters as you like
				bind(GershensonConfig.class).toInstance(gershensonConfig);
				
				this.bind(TtGeneralAnalysis.class).asEagerSingleton();
				this.addEventHandlerBinding().to(TtGeneralAnalysis.class);
				this.bind(TtAnalyzedGeneralResultsWriter.class);
				this.addControlerListenerBinding().to(TtListenerToBindGeneralAnalysis.class);
				
				if (signalsConfigGroup.isUseSignalSystems()) {
					// bind tool to analyze signals
					this.bind(SignalAnalysisTool.class);
					this.bind(SignalAnalysisWriter.class);
					this.addControlerListenerBinding().to(SignalAnalysisListener.class);
					this.addControlerListenerBinding().to(TtQueueLengthAnalysisTool.class);
					this.addMobsimListenerBinding().to(TtQueueLengthAnalysisTool.class);
				}

			}
		});
		
		controler.run();
	}

}
