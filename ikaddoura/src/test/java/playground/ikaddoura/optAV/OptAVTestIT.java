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
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.contrib.noise.NoiseModule;
import org.matsim.contrib.noise.data.NoiseContext;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.contrib.taxi.optimizer.DefaultTaxiOptimizerProvider;
import org.matsim.contrib.taxi.run.TaxiConfigConsistencyChecker;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.run.TaxiModule;
import org.matsim.contrib.taxi.run.TaxiOutputModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import playground.ikaddoura.analysis.detailedPersonTripAnalysis.PersonTripAnalysisModule;
import playground.ikaddoura.analysis.linkDemand.LinkDemandEventHandler;
import playground.ikaddoura.decongestion.DecongestionConfigGroup;
import playground.ikaddoura.decongestion.DecongestionModule;
import playground.ikaddoura.moneyTravelDisutility.MoneyEventAnalysis;
import playground.ikaddoura.moneyTravelDisutility.MoneyTimeDistanceTravelDisutilityFactory;
import playground.ikaddoura.moneyTravelDisutility.data.AgentFilter;
import playground.ikaddoura.optAV.OptAVConfigGroup.OptAVApproach;

/**
 * @author ikaddoura
 *
 */
public class OptAVTestIT {
	
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();

	/**
	 * only taxi trips
	 */
	@Test
	public final void test1(){

		String configFile = testUtils.getPackageInputDirectory() + "config.xml";
		final boolean otfvis = false;
		 
		// ##################################################################
		// baseCase
		// ##################################################################
		
		Config config1 = ConfigUtils.loadConfig(configFile,
				new OptAVConfigGroup(),
				new TaxiConfigGroup(),
				new DvrpConfigGroup(),
				new OTFVisConfigGroup(),
				new NoiseConfigGroup());
		
		config1.controler().setOutputDirectory(testUtils.getOutputDirectory() + "bc1");
		OptAVConfigGroup optAVParams1 = ConfigUtils.addOrGetModule(config1, OptAVConfigGroup.class);
		optAVParams1.setAccountForNoise(false);
		optAVParams1.setAccountForCongestion(false);
		optAVParams1.setOptAVApproach(OptAVApproach.NoPricing);

		Scenario scenario1 = ScenarioUtils.loadScenario(config1);
		Controler controler1 = new Controler(scenario1);
		
		controler1.addOverridingModule(new OptAVModule(scenario1));		
		controler1.addOverridingModule(new PersonTripAnalysisModule());
		
		if (otfvis) controler1.addOverridingModule(new OTFVisLiveModule());	
		
		LinkDemandEventHandler handler1 = new LinkDemandEventHandler(controler1.getScenario().getNetwork());
		controler1.getEvents().addHandler(handler1);
		
		controler1.getConfig().controler().setCreateGraphs(false);

		// run
		
        controler1.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        controler1.run();
		
		// ##################################################################
		// noise pricing
		// ##################################################################

		Config config2 = ConfigUtils.loadConfig(configFile,
				new OptAVConfigGroup(),
				new TaxiConfigGroup(),
				new DvrpConfigGroup(),
				new OTFVisConfigGroup(),
				new NoiseConfigGroup());
		
		config2.controler().setOutputDirectory(testUtils.getOutputDirectory() + "n");
		OptAVConfigGroup optAVParams2 = ConfigUtils.addOrGetModule(config2, OptAVConfigGroup.class);
		optAVParams2.setAccountForNoise(true);
		optAVParams2.setAccountForCongestion(false);
		optAVParams2.setOptAVApproach(OptAVApproach.PrivateAndExternalCost);
		
		Scenario scenario2 = ScenarioUtils.loadScenario(config2);
		Controler controler2 = new Controler(scenario2);
		
		controler2.addOverridingModule(new OptAVModule(scenario2));		        
		controler2.addOverridingModule(new PersonTripAnalysisModule());
		
		if (otfvis) controler2.addOverridingModule(new OTFVisLiveModule());

		LinkDemandEventHandler handler2 = new LinkDemandEventHandler(controler2.getScenario().getNetwork());
		controler2.getEvents().addHandler(handler2);
		
		controler2.getConfig().controler().setCreateGraphs(false);
		
		// run
        controler2.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		controler2.run();
		
		// print outs
					
		System.out.println("----------------------------------");
		System.out.println("Base case:");
		printResults1(handler1);
	
		System.out.println("----------------------------------");
		System.out.println("Noise pricing:");
		printResults1(handler2);
		
		// the demand on the noise sensitive route should go down in case of noise pricing (n)
		Assert.assertEquals(true, getNoiseSensitiveRouteDemand(handler2) < getNoiseSensitiveRouteDemand(handler1));
	}
	
	/**
	 * car + taxi trips
	 */
	@Test
	public final void test2(){

		String configFile = testUtils.getPackageInputDirectory() + "config2.xml";
		final boolean otfvis = false;
		 
		// ##################################################################
		// baseCase
		// ##################################################################
		
		Config config1 = ConfigUtils.loadConfig(configFile,
				new OptAVConfigGroup(),
				new TaxiConfigGroup(),
				new DvrpConfigGroup(),
				new OTFVisConfigGroup(),
				new NoiseConfigGroup());
		
		config1.controler().setOutputDirectory(testUtils.getOutputDirectory() + "bc2");
		final OptAVConfigGroup optAVParams1 = ConfigUtils.addOrGetModule(config1, OptAVConfigGroup.class);
		optAVParams1.setAccountForNoise(false);
		optAVParams1.setAccountForCongestion(false);
		optAVParams1.setOptAVApproach(OptAVApproach.NoPricing);
		
		Scenario scenario1 = ScenarioUtils.loadScenario(config1);
		Controler controler1 = new Controler(scenario1);
		
		// taxi
		
		controler1.addOverridingModule(new OptAVModule(scenario1));
		controler1.addOverridingModule(new PersonTripAnalysisModule());
		if (otfvis) controler1.addOverridingModule(new OTFVisLiveModule());	
		
		LinkDemandEventHandler handler1 = new LinkDemandEventHandler(controler1.getScenario().getNetwork());
		controler1.getEvents().addHandler(handler1);
		
		controler1.getConfig().controler().setCreateGraphs(false);
        controler1.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		controler1.run();
		
		// ##################################################################
		// congestion pricing
		// ##################################################################

		Config config2 = ConfigUtils.loadConfig(configFile,
				new OptAVConfigGroup(),
				new TaxiConfigGroup(),
				new DvrpConfigGroup(),
				new OTFVisConfigGroup());
		
		config2.controler().setOutputDirectory(testUtils.getOutputDirectory() + "c");
		final OptAVConfigGroup optAVParams2 = ConfigUtils.addOrGetModule(config2, OptAVConfigGroup.class);
		optAVParams2.setAccountForNoise(false);
		optAVParams2.setAccountForCongestion(true);
		optAVParams2.setOptAVApproach(OptAVApproach.PrivateAndExternalCost);
		
		final DecongestionConfigGroup decongestionSettings = ConfigUtils.addOrGetModule(config2, DecongestionConfigGroup.class);
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
		
		Scenario scenario2 = ScenarioUtils.loadScenario(config2);
		Controler controler2 = new Controler(scenario2);
			
		controler2.addOverridingModule(new OptAVModule(scenario2));
		controler2.addOverridingModule(new PersonTripAnalysisModule());
		if (otfvis) controler2.addOverridingModule(new OTFVisLiveModule());

		LinkDemandEventHandler handler2 = new LinkDemandEventHandler(controler2.getScenario().getNetwork());
		controler2.getEvents().addHandler(handler2);
		
		controler2.getConfig().controler().setCreateGraphs(false);
        controler2.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		controler2.run();

		// print outs
					
		System.out.println("----------------------------------");
		System.out.println("Base case:");
		printResults1(handler1);
	
		System.out.println("----------------------------------");
		System.out.println("Congestion pricing:");
		printResults1(handler2);
		
		// the demand on the congested route should go down in case of congestion pricing
		Assert.assertEquals(true, getNoiseSensitiveRouteDemand(handler2) < getNoiseSensitiveRouteDemand(handler1));
	}
	
	private void printResults1(LinkDemandEventHandler handler) {
		System.out.println("long but uncongested: " + getLongUncongestedDemand(handler));
		System.out.println("high external cost: " + (getNoiseSensitiveRouteDemand(handler)));
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
