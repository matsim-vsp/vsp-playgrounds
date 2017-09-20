/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.ikaddoura.optAV;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.av.robotaxi.scoring.TaxiFareConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.run.TaxiModule;
import org.matsim.contrib.taxi.run.TaxiOutputModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import playground.ikaddoura.analysis.detailedPersonTripAnalysis.PersonTripAnalysisModule;
import playground.ikaddoura.analysis.linkDemand.LinkDemandEventHandler;
import playground.ikaddoura.decongestion.DecongestionConfigGroup;
import playground.ikaddoura.optAV.OptAVConfigGroup.TollingApproach;

/**
 * @author ikaddoura
 *
 */
public class OptAVCarUserSAVDriverPricingTestIT {
	
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();

	/**
	 * car + taxi trips
	 */
	@Test
	public final void test1(){

		String configFile = testUtils.getPackageInputDirectory() + "config4.xml";
		final boolean otfvis = false;
		 
		// ##################################################################
		// baseCase
		// ##################################################################
		
		LinkDemandEventHandler handler1;
		{
			Config config = ConfigUtils.loadConfig(configFile,
					new OptAVConfigGroup(),
					new TaxiConfigGroup(),
					new DvrpConfigGroup(),
					new TaxiFareConfigGroup(),
					new OTFVisConfigGroup(),
					new NoiseConfigGroup());
			
			config.controler().setOutputDirectory(testUtils.getOutputDirectory() + "bc");
			config.travelTimeCalculator().setTraveltimeBinSize(60);
			
			final OptAVConfigGroup optAVParams = ConfigUtils.addOrGetModule(config, OptAVConfigGroup.class);
			optAVParams.setAccountForNoise(false);
			optAVParams.setAccountForCongestion(false);
			optAVParams.setOptAVApproach(TollingApproach.NoPricing);
			
			Scenario scenario1 = ScenarioUtils.loadScenario(config);
			Controler controler = new Controler(scenario1);
			
			// taxi
			
			controler.addOverridingModule(new TaxiOutputModule());
			controler.addOverridingModule(new TaxiModule());
			controler.addOverridingModule(new OptAVModule(scenario1));
			controler.addOverridingModule(new PersonTripAnalysisModule());
			if (otfvis) controler.addOverridingModule(new OTFVisLiveModule());	
			
			handler1 = new LinkDemandEventHandler(controler.getScenario().getNetwork());
			controler.getEvents().addHandler(handler1);
			
			controler.getConfig().controler().setCreateGraphs(false);
	        controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
			controler.run();
		}
		
		// ##################################################################
		// congestion pricing for car users only
		// ##################################################################

		LinkDemandEventHandler handler2;
		{
			Config config = ConfigUtils.loadConfig(configFile,
					new OptAVConfigGroup(),
					new TaxiConfigGroup(),
					new DvrpConfigGroup(),
					new TaxiFareConfigGroup(),
					new OTFVisConfigGroup());
			
			config.controler().setOutputDirectory(testUtils.getOutputDirectory() + "c-car-only");
			config.travelTimeCalculator().setTraveltimeBinSize(60);
			
			final OptAVConfigGroup optAVParams = ConfigUtils.addOrGetModule(config, OptAVConfigGroup.class);
			optAVParams.setAccountForNoise(false);
			optAVParams.setAccountForCongestion(true);
			optAVParams.setOptAVApproach(TollingApproach.PrivateAndExternalCost);
			optAVParams.setChargeSAVTollsFromPassengers(false);
			optAVParams.setChargeTollsFromCarUsers(true);
			optAVParams.setChargeTollsFromSAVDriver(false);
			
			final DecongestionConfigGroup decongestionSettings = ConfigUtils.addOrGetModule(config, DecongestionConfigGroup.class);
			decongestionSettings.setMsa(true);
			decongestionSettings.setTOLL_BLEND_FACTOR(0.);
			decongestionSettings.setKd(0.);
			decongestionSettings.setKi(0.);
			decongestionSettings.setKp(999.);
			decongestionSettings.setFRACTION_OF_ITERATIONS_TO_END_PRICE_ADJUSTMENT(1.0);
			decongestionSettings.setFRACTION_OF_ITERATIONS_TO_START_PRICE_ADJUSTMENT(0.0);
			decongestionSettings.setUPDATE_PRICE_INTERVAL(1);
			decongestionSettings.setWRITE_LINK_INFO_CHARTS(false);
			decongestionSettings.setRUN_FINAL_ANALYSIS(false);
			
			Scenario scenario = ScenarioUtils.loadScenario(config);
			Controler controler = new Controler(scenario);
				
			controler.addOverridingModule(new TaxiOutputModule());
			controler.addOverridingModule(new TaxiModule());
			controler.addOverridingModule(new OptAVModule(scenario));
			
			if (otfvis) controler.addOverridingModule(new OTFVisLiveModule());

			handler2 = new LinkDemandEventHandler(controler.getScenario().getNetwork());
			controler.getEvents().addHandler(handler2);
			
			controler.getConfig().controler().setCreateGraphs(false);
	        controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
			controler.run();
		}
		
		// ##################################################################
		// congestion pricing for taxi drivers only
		// ##################################################################

		LinkDemandEventHandler handler3;
		{
			Config config = ConfigUtils.loadConfig(configFile,
					new OptAVConfigGroup(),
					new TaxiConfigGroup(),
					new DvrpConfigGroup(),
					new TaxiFareConfigGroup(),
					new OTFVisConfigGroup());
			
			config.controler().setOutputDirectory(testUtils.getOutputDirectory() + "c-SAV");
			config.travelTimeCalculator().setTraveltimeBinSize(60);
			
			final OptAVConfigGroup optAVParams = ConfigUtils.addOrGetModule(config, OptAVConfigGroup.class);
			optAVParams.setAccountForNoise(false);
			optAVParams.setAccountForCongestion(true);
			optAVParams.setOptAVApproach(TollingApproach.PrivateAndExternalCost);
			optAVParams.setChargeSAVTollsFromPassengers(false);
			optAVParams.setChargeTollsFromCarUsers(false);
			optAVParams.setChargeTollsFromSAVDriver(true);
			
			final DecongestionConfigGroup decongestionSettings = ConfigUtils.addOrGetModule(config, DecongestionConfigGroup.class);
			decongestionSettings.setMsa(true);
			decongestionSettings.setTOLL_BLEND_FACTOR(0.);
			decongestionSettings.setKd(0.);
			decongestionSettings.setKi(0.);
			decongestionSettings.setKp(999.);
			decongestionSettings.setFRACTION_OF_ITERATIONS_TO_END_PRICE_ADJUSTMENT(1.0);
			decongestionSettings.setFRACTION_OF_ITERATIONS_TO_START_PRICE_ADJUSTMENT(0.0);
			decongestionSettings.setUPDATE_PRICE_INTERVAL(1);
			decongestionSettings.setWRITE_LINK_INFO_CHARTS(false);
			decongestionSettings.setRUN_FINAL_ANALYSIS(false);
			
			Scenario scenario = ScenarioUtils.loadScenario(config);
			Controler controler = new Controler(scenario);
				
			controler.addOverridingModule(new TaxiOutputModule());
			controler.addOverridingModule(new TaxiModule());
			controler.addOverridingModule(new OptAVModule(scenario));
			
			if (otfvis) controler.addOverridingModule(new OTFVisLiveModule());

			handler3 = new LinkDemandEventHandler(controler.getScenario().getNetwork());
			controler.getEvents().addHandler(handler3);
			
			controler.getConfig().controler().setCreateGraphs(false);
	        controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
			controler.run();
		}
		
		// ##################################################################
		// congestion pricing for car users and taxi drivers
		// ##################################################################

		LinkDemandEventHandler handler4;
		{
			Config config = ConfigUtils.loadConfig(configFile,
					new OptAVConfigGroup(),
					new TaxiConfigGroup(),
					new DvrpConfigGroup(),
					new TaxiFareConfigGroup(),
					new OTFVisConfigGroup());
			
			config.controler().setOutputDirectory(testUtils.getOutputDirectory() + "c-car-SAV");
			config.travelTimeCalculator().setTraveltimeBinSize(60);
			
			final OptAVConfigGroup optAVParams = ConfigUtils.addOrGetModule(config, OptAVConfigGroup.class);
			optAVParams.setAccountForNoise(false);
			optAVParams.setAccountForCongestion(true);
			optAVParams.setOptAVApproach(TollingApproach.PrivateAndExternalCost);
			optAVParams.setChargeSAVTollsFromPassengers(false);
			optAVParams.setChargeTollsFromCarUsers(true);
			optAVParams.setChargeTollsFromSAVDriver(true);
			
			final DecongestionConfigGroup decongestionSettings = ConfigUtils.addOrGetModule(config, DecongestionConfigGroup.class);
			decongestionSettings.setMsa(true);
			decongestionSettings.setTOLL_BLEND_FACTOR(0.);
			decongestionSettings.setKd(0.);
			decongestionSettings.setKi(0.);
			decongestionSettings.setKp(999.);
			decongestionSettings.setFRACTION_OF_ITERATIONS_TO_END_PRICE_ADJUSTMENT(1.0);
			decongestionSettings.setFRACTION_OF_ITERATIONS_TO_START_PRICE_ADJUSTMENT(0.0);
			decongestionSettings.setUPDATE_PRICE_INTERVAL(1);
			decongestionSettings.setWRITE_LINK_INFO_CHARTS(false);
			decongestionSettings.setRUN_FINAL_ANALYSIS(false);
			
			Scenario scenario = ScenarioUtils.loadScenario(config);
			Controler controler = new Controler(scenario);
				
			controler.addOverridingModule(new TaxiOutputModule());
			controler.addOverridingModule(new TaxiModule());
			controler.addOverridingModule(new OptAVModule(scenario));
			
			if (otfvis) controler.addOverridingModule(new OTFVisLiveModule());

			handler4 = new LinkDemandEventHandler(controler.getScenario().getNetwork());
			controler.getEvents().addHandler(handler4);
			
			controler.getConfig().controler().setCreateGraphs(false);
	        controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
			controler.run();
		}

		// print outs
					
		System.out.println("----------------------------------");
		System.out.println("Base case:");
		printResults1(handler1);
	
		System.out.println("----------------------------------");
		System.out.println("Congestion pricing car users only:");
		printResults1(handler2);
		
		System.out.println("----------------------------------");
		System.out.println("Congestion pricing SAV drivers only:");
		printResults1(handler3);
		
		System.out.println("----------------------------------");
		System.out.println("Congestion pricing car users and SAV drivers:");
		printResults1(handler4);
		
		Assert.assertEquals(true, getNoiseSensitiveRouteDemand(handler2) < getNoiseSensitiveRouteDemand(handler1));
		Assert.assertEquals(true, getNoiseSensitiveRouteDemand(handler3) < getNoiseSensitiveRouteDemand(handler1));
		Assert.assertEquals(true, getNoiseSensitiveRouteDemand(handler4) < getNoiseSensitiveRouteDemand(handler2));
		Assert.assertEquals(true, getNoiseSensitiveRouteDemand(handler4) < getNoiseSensitiveRouteDemand(handler3));
	}
	
	private void printResults1(LinkDemandEventHandler handler) {
		System.out.println("long but low external costs: " + getLongUncongestedDemand(handler));
		System.out.println("short but high external costs: " + (getNoiseSensitiveRouteDemand(handler)));
	}
	
	private int getNoiseSensitiveRouteDemand(LinkDemandEventHandler handler) {
		int noiseSensitiveRouteDemand = 0;
		if (handler.getLinkId2demand().containsKey(Id.createLinkId("link_7_8"))) {
			noiseSensitiveRouteDemand = handler.getLinkId2demand().get(Id.createLinkId("link_7_8"));
		}
		return noiseSensitiveRouteDemand;
	}
	
	private int getLongUncongestedDemand(LinkDemandEventHandler handler) {
		int longUncongestedRouteDemand = 0;
		if (handler.getLinkId2demand().containsKey(Id.createLinkId("link_1_2"))) {
			longUncongestedRouteDemand = handler.getLinkId2demand().get(Id.createLinkId("link_1_2"));
		}
		return longUncongestedRouteDemand;
	}
		
}
