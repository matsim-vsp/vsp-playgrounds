/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DefaultControlerModules.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package scenarios.illustrative.smith;

import java.io.File;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.analysis.SignalAnalysisTool;
import org.matsim.contrib.signals.builder.SignalsModule;
import org.matsim.contrib.signals.controller.laemmerFix.LaemmerConfigGroup;
import org.matsim.contrib.signals.controller.sylvia.SylviaConfigGroup;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalControlWriter20;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupsWriter20;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalSystemsWriter20;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup.TravelTimeCalculatorType;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultSelector;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.lanes.LanesWriter;

import analysis.signals.SignalAnalysisListener;
import analysis.signals.SignalAnalysisWriter;
import scenarios.illustrative.analysis.TtAbstractAnalysisTool;
import scenarios.illustrative.analysis.TtAnalyzedResultsWriter;
import scenarios.illustrative.smith.CreateSmithPopulation.InitRoutesType;
import scenarios.illustrative.smith.CreateSmithSignals.SignalControlType;
import utils.OutputUtils;

/**
 * Class to run a simulation of Smith' scenario with or without signals. 
 * It analyzes the simulation with help of AnalyzeSmith.java
 * 
 * @author tthunig
 * 
 */
public final class RunSmithSimulation {

	private static final Logger log = Logger.getLogger(RunSmithSimulation.class);

	/* population parameter */
	private static final int NUMBER_OF_PERSONS = 3180;
	private static final InitRoutesType INIT_ROUTES_TYPE = InitRoutesType.OSZILLATING;
	// initial score for all initial plans. choose null for no score
	private static final Double INIT_PLAN_SCORE = null;
	
	private static final boolean USE_SIGNALS = true;
	private static final SignalControlType SIGNAL_CONTROL = SignalControlType.LAEMMER_WITH_GROUPS;
	// TODO intersection modeling: conflicts...
	
//	// defines which kind of pricing should be used
//	private static final PricingType PRICING_TYPE = PricingType.V3;
//	public enum PricingType{
//		NONE, V3, V4, V8, V9, FLOWBASED
//	}
		
	private static final boolean WRITE_INITIAL_FILES = false;

	
	private static String OUTPUT_BASE_DIR = "../../runs-svn/smith/interG3_minG1/";
	
	public static void main(String[] args) {
		Config config = defineConfig();
		Scenario scenario = prepareScenario(config);
		Controler controler = prepareController(scenario);
	
		controler.run();
	}

	private static Scenario prepareScenario(Config config) {
		Scenario scenario = ScenarioUtils.loadScenario(config);
		createNetwork(scenario);
		createPopulation(scenario);
		createRunNameAndOutputDir(scenario);
	
		// add missing scenario elements
		SignalSystemsConfigGroup signalsConfigGroup = ConfigUtils.addOrGetModule(config,
				SignalSystemsConfigGroup.GROUP_NAME, SignalSystemsConfigGroup.class);
		if (signalsConfigGroup.isUseSignalSystems()) {
			scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());
			createSignals(scenario);
		}
		
		if (WRITE_INITIAL_FILES) 
			writeInitFiles(scenario);
		
		return scenario;
	}

	private static Controler prepareController(Scenario scenario) {
		Config config = scenario.getConfig();
		Controler controler = new Controler(scenario);

		// add the signals module if signal systems are used
		SignalSystemsConfigGroup signalsConfigGroup = ConfigUtils.addOrGetModule(config,
				SignalSystemsConfigGroup.GROUP_NAME, SignalSystemsConfigGroup.class);
		if (signalsConfigGroup.isUseSignalSystems()) {
			controler.addOverridingModule(new SignalsModule());
		}
		
//		if (!PRICING_TYPE.equals(PricingType.NONE) && !PRICING_TYPE.equals(PricingType.FLOWBASED)){
//			// add tolling
//			TollHandler tollHandler = new TollHandler(scenario);
//			
//			// add correct TravelDisutilityFactory for tolls if ReRoute is used
//			StrategySettings[] strategies = config.strategy().getStrategySettings()
//					.toArray(new StrategySettings[0]);
//			for (int i = 0; i < strategies.length; i++) {
//				if (strategies[i].getStrategyName().equals(DefaultStrategy.ReRoute.toString())){
//					if (strategies[i].getWeight() > 0.0){ // ReRoute is used
//						final CongestionTollTimeDistanceTravelDisutilityFactory factory =
//								new CongestionTollTimeDistanceTravelDisutilityFactory(
//										new Builder( TransportMode.car ),
//								tollHandler
//							) ;
//						factory.setSigma(SIGMA);
//						controler.addOverridingModule(new AbstractModule(){
//							@Override
//							public void install() {
//								this.bindCarTravelDisutilityFactory().toInstance( factory );
//							}
//						});
//					}
//				}
//			}		
//			
//			// choose the correct congestion handler and add it
//			EventHandler congestionHandler = null;
//			switch (PRICING_TYPE){
//			case V3:
//				congestionHandler = new CongestionHandlerImplV3(controler.getEvents(), 
//						controler.getScenario());
//				break;
//			case V4:
//				congestionHandler = new CongestionHandlerImplV4(controler.getEvents(), 
//						controler.getScenario());
//				break;
//			case V8:
//				congestionHandler = new CongestionHandlerImplV8(controler.getEvents(), 
//						controler.getScenario());
//				break;
//			case V9:
//				congestionHandler = new CongestionHandlerImplV9(controler.getEvents(), 
//						controler.getScenario());
//				break;
//			default:
//				break;
//			}
//			controler.addControlerListener(
//					new MarginalCongestionPricingContolerListener(controler.getScenario(), 
//							tollHandler, congestionHandler));
//		
//		} else if (PRICING_TYPE.equals(PricingType.FLOWBASED)) {
//			
//			Initializer initializer = new Initializer();
//			controler.addControlerListener(initializer);		
//		} 
		
		controler.addOverridingModule(new AbstractModule() {			
			@Override
			public void install() {
				SignalSystemsConfigGroup signalsConfigGroup = ConfigUtils.addOrGetModule(config,
						SignalSystemsConfigGroup.GROUP_NAME, SignalSystemsConfigGroup.class);
				if (signalsConfigGroup.isUseSignalSystems()) {
					// bind tool to analyze signals
					this.bind(SignalAnalysisTool.class);
					this.bind(SignalAnalysisWriter.class);
					this.addControlerListenerBinding().to(SignalAnalysisListener.class);
				}

				this.bind(TtAbstractAnalysisTool.class).to(AnalyzeSmith.class).asEagerSingleton();
				this.addEventHandlerBinding().to(TtAbstractAnalysisTool.class);
				this.bind(TtAnalyzedResultsWriter.class);
				this.addControlerListenerBinding().to(AnalysisScriptBinderWithoutTolls.class);
			}
		});
		
		return controler;
	}

	private static Config defineConfig() {
		Config config = ConfigUtils.createConfig();

		// set number of iterations
		config.controler().setLastIteration( 100 );

		config.qsim().setUseLanes( true );
		SignalSystemsConfigGroup signalConfigGroup = ConfigUtils
				.addOrGetModule(config, SignalSystemsConfigGroup.GROUP_NAME,
						SignalSystemsConfigGroup.class);
		signalConfigGroup.setUseSignalSystems( USE_SIGNALS? true : false );
		config.qsim().setUsingFastCapacityUpdate(false);
		
		LaemmerConfigGroup laemmerConfigGroup = ConfigUtils.addOrGetModule(config, LaemmerConfigGroup.class);
		laemmerConfigGroup.setMaxCycleTime(90);
		laemmerConfigGroup.setDesiredCycleTime(60);
		laemmerConfigGroup.setIntergreenTime(3);
		laemmerConfigGroup.setMinGreenTime(1);
		
		SylviaConfigGroup sylviaConfig = ConfigUtils.addOrGetModule(config, SylviaConfigGroup.class);
		sylviaConfig.setSignalGroupMaxGreenScale(3);
		sylviaConfig.setUseFixedTimeCycleAsMaximalExtension(false);
//		sylviaConfig.setSensorDistanceMeter(10);

		// set brain exp beta
		config.planCalcScore().setBrainExpBeta( 20 );

		// choose between link to link and node to node routing
		boolean link2linkRouting = true;
		config.controler().setLinkToLinkRoutingEnabled(link2linkRouting);
		
		config.travelTimeCalculator().setCalculateLinkToLinkTravelTimes(link2linkRouting);
		config.travelTimeCalculator().setCalculateLinkTravelTimes(true);
		
		// set travelTimeBinSize
		config.travelTimeCalculator().setTraveltimeBinSize( 10 );
		
		config.travelTimeCalculator().setTravelTimeCalculatorType(
				TravelTimeCalculatorType.TravelTimeCalculatorHashMap.toString());
		// hash map and array produce same results. only difference: memory and time.
		// for small time bins and sparse values hash map is better. theresa, may'15
		
		// define strategies:
		{
			StrategySettings strat = new StrategySettings() ;
			strat.setStrategyName( DefaultStrategy.ReRoute.toString() );
			strat.setWeight( 0.1 ) ;
			strat.setDisableAfter( config.controler().getLastIteration() - 30 );
			config.strategy().addStrategySettings(strat);
		}
		{
			StrategySettings strat = new StrategySettings() ;
			strat.setStrategyName( DefaultSelector.SelectRandom.toString() );
			strat.setWeight( 0.0 ) ;
			strat.setDisableAfter( config.controler().getLastIteration() - 30 );
			config.strategy().addStrategySettings(strat);
		}
		{
			StrategySettings strat = new StrategySettings() ;
			strat.setStrategyName( DefaultSelector.ChangeExpBeta.toString() );
			strat.setWeight( 0.9 ) ;
			strat.setDisableAfter( config.controler().getLastIteration() );
			config.strategy().addStrategySettings(strat);
		}
		{
			StrategySettings strat = new StrategySettings() ;
			strat.setStrategyName( DefaultSelector.BestScore.toString() );
			strat.setWeight( 0.0 ) ;
			strat.setDisableAfter( config.controler().getLastIteration() - 30 );
			config.strategy().addStrategySettings(strat);
		}
		{
			StrategySettings strat = new StrategySettings() ;
			strat.setStrategyName( DefaultSelector.KeepLastSelected.toString() );
			strat.setWeight( 0.0 ) ;
			strat.setDisableAfter( config.controler().getLastIteration() );
			config.strategy().addStrategySettings(strat);
		}

		// choose maximal number of plans per agent. 0 means unlimited
		config.strategy().setMaxAgentPlanMemorySize( 0 );
		
		config.qsim().setStuckTime(3600 * 10.);
		
		// set end time to 12 am (4 hours after simulation start) to
		// shorten simulation run time
		config.qsim().setEndTime(3600 * 12);
		
		// adapt monetary distance cost rate
		// (should be negative. use -12.0 to balance time [h] and distance [m].
		// use -0.000015 to approximately balance the utility of travel time and
		// distance in a scenario with 3 vs 11min travel time and 40 vs 50 km.
		// use -0.0 to use only time.)
		config.planCalcScore().getModes().get(TransportMode.car).setMonetaryDistanceRate( -0.0 );
		
		config.planCalcScore().setMarginalUtilityOfMoney( 1.0 ); // default is 1.0

		config.controler().setOverwriteFileSetting( OverwriteFileSetting.overwriteExistingFiles );		
		// note: the output directory is defined in createRunNameAndOutputDir(...) after all adaptations are done
		
		config.vspExperimental().setWritingOutputEvents(true);
		config.planCalcScore().setWriteExperiencedPlans(true);

		config.controler().setWriteEventsInterval( config.controler().getLastIteration() );
		config.controler().setWritePlansInterval( config.controler().getLastIteration() );
		
		ActivityParams dummyAct = new ActivityParams("dummy");
		dummyAct.setTypicalDuration(12 * 3600);
		config.planCalcScore().addActivityParams(dummyAct);
		
		config.controler().setCreateGraphs( false );
		
		return config;
	}

	private static void createNetwork(Scenario scenario) {	
		
		CreateSmithNetworkAndLanes netCreator = new CreateSmithNetworkAndLanes(scenario);
		// TODO
//		netCreator.setCapacity(NUMBER_OF_PERSONS);
		netCreator.setCapacity(3600);
		netCreator.createNetworkWithLanes();
	}

	private static void createPopulation(Scenario scenario) {
		
		CreateSmithPopulation popCreator = new CreateSmithPopulation(scenario.getPopulation(), scenario.getNetwork());
		popCreator.createPersons(NUMBER_OF_PERSONS, INIT_ROUTES_TYPE, INIT_PLAN_SCORE);
	}

	private static void createSignals(Scenario scenario) {
		// this method is only called when signal systems are used
		
		CreateSmithSignals signalsCreator = new CreateSmithSignals(scenario);
		signalsCreator.createSignals(SIGNAL_CONTROL);
	}

	private static void createRunNameAndOutputDir(Scenario scenario) {

		Config config = scenario.getConfig();
		
		String runName = OutputUtils.getCurrentDateIncludingTime();

		runName += "_" + NUMBER_OF_PERSONS;
		if (!INIT_ROUTES_TYPE.equals(InitRoutesType.NONE)){
			runName += "_init" + INIT_ROUTES_TYPE;
			if (INIT_PLAN_SCORE != null)
				runName += "-score" + INIT_PLAN_SCORE;
		}
		
		if (USE_SIGNALS) {
			runName += "_signals" + SIGNAL_CONTROL;
		}

		runName += "_" + config.controler().getLastIteration() + "it";
		
		// create info about used strategies
		StrategySettings[] strategies = config.strategy().getStrategySettings()
				.toArray(new StrategySettings[0]);
		for (int i = 0; i < strategies.length; i++) {
			double weight = strategies[i].getWeight();
			if (weight != 0.0){
				String name = strategies[i].getStrategyName();
				runName += "_" + name + weight;
				if (name.equals(DefaultStrategy.ReRoute.toString())){
					runName += "_tbs"
							+ config.travelTimeCalculator().getTraveltimeBinSize();
				}
				if (name.equals(DefaultSelector.ChangeExpBeta.toString())){
					runName += "_beta" + (int)config.planCalcScore().getBrainExpBeta();
				}
			}
		}
		
//		if (!PRICING_TYPE.equals(PricingType.NONE)){
//			runName += "_" + PRICING_TYPE.toString();
//		}
		
		String outputDir = OUTPUT_BASE_DIR + runName + "/"; 
		// create directory
		new File(outputDir).mkdirs();

		config.controler().setOutputDirectory(outputDir);
		log.info("The output will be written to " + outputDir);
	}

	private static void writeInitFiles(Scenario scenario) {
		String outputDir = scenario.getConfig().controler().getOutputDirectory() + "initialFiles/";
		// create directory
		new File(outputDir).mkdirs();
		
		// write network and lanes
		new NetworkWriter(scenario.getNetwork()).write(outputDir + "network.xml");
		new LanesWriter(scenario.getLanes()).write(outputDir + "lanes.xml");
		
		// write population
		new PopulationWriter(scenario.getPopulation()).write(outputDir + "plans.xml");
		
		// write signal files
		if (USE_SIGNALS) {
			SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
			new SignalSystemsWriter20(signalsData.getSignalSystemsData()).write(outputDir + "signalSystems.xml");
			new SignalControlWriter20(signalsData.getSignalControlData()).write(outputDir + "signalControl.xml");
			new SignalGroupsWriter20(signalsData.getSignalGroupsData()).write(outputDir + "signalGroups.xml");
		}
		
		// write config
		new ConfigWriter(scenario.getConfig()).write(outputDir + "config.xml");
	}
}
