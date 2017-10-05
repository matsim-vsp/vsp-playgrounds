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
package congestionPricing2CapacityAdoption;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.scenario.ScenarioUtils;

import analysis.TtAnalyzedGeneralResultsWriter;
import analysis.TtGeneralAnalysis;
import analysis.TtListenerToBindGeneralAnalysis;
import analysis.TtStaticLinkFlowValuesPerHour;
import congestionPricing2CapacityAdoption.TtRunCapAdopForBraessIterative.PricingType;
import playground.dziemke.utils.LogToOutputSaver;
import playground.vsp.congestion.controler.MarginalCongestionPricingContolerListener;
import playground.vsp.congestion.handlers.CongestionHandlerImplV10;
import playground.vsp.congestion.handlers.CongestionHandlerImplV3;
import playground.vsp.congestion.handlers.CongestionHandlerImplV4;
import playground.vsp.congestion.handlers.CongestionHandlerImplV7;
import playground.vsp.congestion.handlers.CongestionHandlerImplV8;
import playground.vsp.congestion.handlers.CongestionHandlerImplV9;
import playground.vsp.congestion.handlers.TollHandler;
import playground.vsp.congestion.routing.CongestionTollTimeDistanceTravelDisutilityFactory;

/**
 * @author tthunig
 *
 */
public class TtRunCapAdopOnce {

	private static final Logger LOG = Logger.getLogger(TtRunCapAdopOnce.class);
	
	private static final String RUN_ID = "be_218";
	private static final String INPUT_BASE_DIR = "../../runs-svn/berlin_scenario_2016/" + RUN_ID + "/";	
	private static final String OUTPUT_BASE_DIR = "../../runs-svn/berlin_capacityReduction/";

	private static final int FIRST_IT = 0;
	private static final int LAST_IT = 500;
	// note: innovative strategies are switched of at iteration 200
	
	private static final PricingType PRICING_TYPE = PricingType.V9;
	
	// choose a sigma for the randomized router
	// (higher sigma cause more randomness. use 0.0 for no randomness.)
	private static final double SIGMA = 0.0;
	
	private static final double MIN_CAP = 1.;

	private enum CapRedType{
		BASIC, PRICING, CAP_RED
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
//		createInputFilesBasicPricing();
		
		String eventsFileBasic = OUTPUT_BASE_DIR + "2017-08-6_be_218_500it_BASIC_m/be_218.output_events.xml.gz";
		String eventsFilePricing = OUTPUT_BASE_DIR + "2017-08-7_be_218_500it_PRICING_LP_toleranz10_m/be_218.output_events.xml.gz";
		compareBasicPricingCreateCapRedNet(eventsFileBasic, eventsFilePricing);
		
//		runOnThisMachine();
	}

	private static void createInputFilesBasicPricing() {
		Config config = createConfig(CapRedType.PRICING);
		String outputDir = config.controler().getOutputDirectory() + "initialFiles/";
		// create directory
		new File(outputDir).mkdirs();
		// write config
		new ConfigWriter(config).write(outputDir + "config.xml");
		
		config = createConfig(CapRedType.BASIC);
		outputDir = config.controler().getOutputDirectory() + "initialFiles/";
		// create directory
		new File(outputDir).mkdirs();
		// write config
		new ConfigWriter(config).write(outputDir + "config.xml");
	}

	private static void compareBasicPricingCreateCapRedNet(String eventsFileBasic, String eventsFilePricing) {
		// TODO try whether log from TtRunCapAdopOnce is now included in run-log-files
		Config config = createConfig(CapRedType.CAP_RED);
		// idea 1:
		LogToOutputSaver.setOutputDirectory(config.controler().getOutputDirectory());
		// idea 2:
//		try {
//			System.setOut(new PrintStream(config.controler().getOutputDirectory() + "sysLog.txt"));
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		}
		// idea 3: the log4j.xml in my classes directory - where is tthunig.log written to?
		
		LOG.info("read in events from basic run");
		EventsManager managerBasic = new EventsManagerImpl();
		TtStaticLinkFlowValuesPerHour flowValuesBasic = new TtStaticLinkFlowValuesPerHour();
		managerBasic.addHandler(flowValuesBasic);
		MatsimEventsReader readerBasic = new MatsimEventsReader(managerBasic);
		readerBasic.readFile(eventsFileBasic);
		
		LOG.info("read in events from pricing run");
		EventsManager managerPricing = new EventsManagerImpl();
		TtStaticLinkFlowValuesPerHour flowValuesPricing = new TtStaticLinkFlowValuesPerHour();
		managerPricing.addHandler(flowValuesPricing);
		MatsimEventsReader readerPricing = new MatsimEventsReader(managerPricing);
		readerPricing.readFile(eventsFilePricing);
		
		LOG.warn("Prepare capacity reduction controler");
//		Config config = createConfig(CapRedType.CAP_RED);
		Controler controlerCapRed = createControler(config, false);
//		TtStaticLinkFlowValuesPerHour flowValuesCapRed = new TtStaticLinkFlowValuesPerHour();
//		TtGeneralAnalysis generalAnalysisCapRed= new TtGeneralAnalysis(controlerCapRed.getScenario().getNetwork());
//		controlerCapRed.addOverridingModule(new AbstractModule() {
//			@Override
//			public void install() {
//				this.addEventHandlerBinding().toInstance(flowValuesCapRed);
//				this.bind(TtGeneralAnalysis.class).toInstance(generalAnalysisCapRed);
//				this.addEventHandlerBinding().toInstance(generalAnalysisCapRed);
//			}
//		});
		
		LOG.warn("Compare static flow values of the pricing and non-pricing run. potentially reduce flow capacity.");
		List<Id<Link>> reducedLinks = new ArrayList<>();
		for (Link link : controlerCapRed.getScenario().getNetwork().getLinks().values()){
			// look for maximal flow values per hour of both runs
			int basicFlowValue = maxValue(flowValuesBasic.getStaticLinkFlows(link.getId()));
			int pricingFlowValue = maxValue(flowValuesPricing.getStaticLinkFlows(link.getId()));
			// check if pricingFlowValue is at most 1/3 of basicFlowValue and less than link flow capacity
			pricingFlowValue = Math.max(1, pricingFlowValue); //otherwise a change to 0 would always be enough
			if (pricingFlowValue * 3 < basicFlowValue &&
					pricingFlowValue < link.getCapacity()*config.qsim().getFlowCapFactor()){
				LOG.warn("Reduce capacity of link " + link.getId() + " from " + link.getCapacity()*config.qsim().getFlowCapFactor() + " to " + pricingFlowValue);
				link.setCapacity( Math.max( pricingFlowValue/config.qsim().getFlowCapFactor(), MIN_CAP ));
				reducedLinks.add(link.getId());
			}
		}
		if (!reducedLinks.isEmpty()){
			writeInitFiles(controlerCapRed.getScenario());
//			LOG.warn("Run simulation with " + reducedLinks.size() + " reduced link capacities.");
//			controlerCapRed.run();
		} else {
			LOG.warn("No link capacities have been reduced.");
		}
		LOG.info("Done!");
		for (Id<Link> reducedLinkId : reducedLinks){
			LOG.info("Link " + reducedLinkId + " max flow values. Basic: " + maxValue(flowValuesBasic.getStaticLinkFlows(reducedLinkId))
					+ ", Pricing: " + maxValue(flowValuesPricing.getStaticLinkFlows(reducedLinkId)) 
//					+ ", CapRed: " + maxValue(flowValuesCapRed.getStaticLinkFlows(reducedLinkId))
					);
		}
		LOG.info("Number of reduced link capacities = " + reducedLinks.size() + ".");
//		LOG.info("Travel time basic = " + generalAnalysisBasic.getTotalTt() + "; pricing = " + generalAnalysisPricing.getTotalTt() + "; capRed = " + generalAnalysisCapRed.getTotalTt());
	}

	private static void runBasicPricingOnThisMachine() {
		LOG.warn("Prepare simulation with pricing " + PRICING_TYPE);
		Controler controlerPricing = createControler(createConfig(CapRedType.PRICING), true);
		TtStaticLinkFlowValuesPerHour flowValuesPricing = new TtStaticLinkFlowValuesPerHour();
		TtGeneralAnalysis generalAnalysisPricing = new TtGeneralAnalysis(controlerPricing.getScenario().getNetwork());
		controlerPricing.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.addEventHandlerBinding().toInstance(flowValuesPricing);
				this.bind(TtGeneralAnalysis.class).toInstance(generalAnalysisPricing);
				this.addEventHandlerBinding().toInstance(generalAnalysisPricing);
			}
		});
		writeInitFiles(controlerPricing.getScenario());
		LOG.warn("Start simulation with pricing " + PRICING_TYPE);
		controlerPricing.run();
		
		LOG.warn("Prepare simulation without pricing");
		Controler controlerBasic = createControler(createConfig(CapRedType.BASIC), false);
		TtStaticLinkFlowValuesPerHour flowValuesBasic = new TtStaticLinkFlowValuesPerHour();
		TtGeneralAnalysis generalAnalysisBasic = new TtGeneralAnalysis(controlerBasic.getScenario().getNetwork());
		controlerBasic.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.addEventHandlerBinding().toInstance(flowValuesBasic);
				this.bind(TtGeneralAnalysis.class).toInstance(generalAnalysisBasic);
				this.addEventHandlerBinding().toInstance(generalAnalysisBasic);
			}
		});
		writeInitFiles(controlerBasic.getScenario());
		LOG.warn("Start simulation without pricing");
		controlerBasic.run();
	}

	private static int maxValue(int[] staticLinkFlows) {
		int max = staticLinkFlows[0];
		for (int i = 0; i < staticLinkFlows.length; i++) {
			if (staticLinkFlows[i] > max) {
				max = staticLinkFlows[i];
			}
		}
		return max;
	}

	private static Controler createControler(Config config, boolean pricing) {
		Scenario scenario = ScenarioUtils.loadScenario( config ) ;
		Controler controler = new Controler(scenario);

		if (pricing) {
			// add tolling
			TollHandler tollHandler = new TollHandler(scenario);

			// add correct TravelDisutilityFactory for tolls if ReRoute is used
			StrategySettings[] strategies = config.strategy().getStrategySettings().toArray(new StrategySettings[0]);
			for (int i = 0; i < strategies.length; i++) {
				if (strategies[i].getStrategyName().equals(DefaultStrategy.ReRoute.toString())) {
					if (strategies[i].getWeight() > 0.0) { // ReRoute is used
						final CongestionTollTimeDistanceTravelDisutilityFactory factory = new CongestionTollTimeDistanceTravelDisutilityFactory(
								new RandomizingTimeDistanceTravelDisutilityFactory(TransportMode.car, config.planCalcScore()), tollHandler, config.planCalcScore());
						factory.setSigma(SIGMA);
						controler.addOverridingModule(new AbstractModule() {
							@Override
							public void install() {
								this.bindCarTravelDisutilityFactory().toInstance(factory);
							}
						});
					}
				}
			}
						
			// choose the correct congestion handler and add it
			EventHandler congestionHandler = null;
			switch (PRICING_TYPE){
			case V3:
				congestionHandler = new CongestionHandlerImplV3(controler.getEvents(), scenario);
				break;
			case V4:
				congestionHandler = new CongestionHandlerImplV4(controler.getEvents(), scenario);
				break;
			case V7:
				congestionHandler = new CongestionHandlerImplV7(controler.getEvents(), scenario);
				break;
			case V8:
				congestionHandler = new CongestionHandlerImplV8(controler.getEvents(), scenario);
				break;
			case V9:
				congestionHandler = new CongestionHandlerImplV9(controler.getEvents(), scenario);
				break;
			case V10:
				congestionHandler = new CongestionHandlerImplV10(controler.getEvents(), scenario);
				break;
			default:
				break;
			}
			controler.addControlerListener(new MarginalCongestionPricingContolerListener(scenario, tollHandler, congestionHandler));
		}
		
		controler.addOverridingModule(new AbstractModule() {			
			@Override
			public void install() {
//				this.bind(TtGeneralAnalysis.class).asEagerSingleton();
//				this.addEventHandlerBinding().to(TtGeneralAnalysis.class);
				this.bind(TtAnalyzedGeneralResultsWriter.class);
				this.addControlerListenerBinding().to(TtListenerToBindGeneralAnalysis.class);
			}
		});
		return controler;
	}

	private static Config createConfig(CapRedType type) {
		Config config = ConfigUtils.loadConfig(INPUT_BASE_DIR + RUN_ID + ".output_config.xml.gz");
		config.controler().setOutputDirectory(OUTPUT_BASE_DIR + createOutputName(type) + "/");
		config.network().setInputFile(RUN_ID + ".output_network.xml.gz");
		config.plans().setInputFile(RUN_ID + ".output_plans.xml.gz");
		config.counts().setInputFile(RUN_ID + ".output_counts.xml.gz");
		config.controler().setFirstIteration(FIRST_IT);
		config.controler().setLastIteration(LAST_IT);
		config.qsim().setEndTime(32*3600);
		config.qsim().setStuckTime(3600);
		config.qsim().setRemoveStuckVehicles(false);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		config.controler().setWriteEventsInterval(100);
		config.controler().setWritePlansInterval(100);
		config.vspExperimental().setWritingOutputEvents(true);
		return config;
	}

	private static String createOutputName(CapRedType type) {
		// get the current date in format "yyyy-mm-dd"
		Calendar cal = Calendar.getInstance();
		// this class counts months from 0, but days from 1
		int month = cal.get(Calendar.MONTH) + 1;
		String monthStr = month + "";
		if (month < 10)
			monthStr = "0" + month;
		String date = cal.get(Calendar.YEAR) + "-" + monthStr + "-" + cal.get(Calendar.DAY_OF_MONTH);

		String runName = date + "_" + RUN_ID + "_" + (LAST_IT - FIRST_IT) + "it_" + type;
		if (type.equals(CapRedType.PRICING)){
			runName += "_" + PRICING_TYPE;
		}
		
		String outputDir = OUTPUT_BASE_DIR + runName + "/"; 
		// create directory
		new File(outputDir).mkdirs();
		
		return runName;
	}
	
	private static void writeInitFiles(Scenario scenario) {		
		String outputDir = scenario.getConfig().controler().getOutputDirectory() + "initialFiles/";
		// create directory
		new File(outputDir).mkdirs();
		
		// write network and lanes
		new NetworkWriter(scenario.getNetwork()).write(outputDir + "network.xml");
		
		// write population
		new PopulationWriter(scenario.getPopulation()).write(outputDir + "plans.xml");
		
		// write config
		new ConfigWriter(scenario.getConfig()).write(outputDir + "config.xml");
	}

}
