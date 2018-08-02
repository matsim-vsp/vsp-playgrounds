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
import org.matsim.contrib.decongestion.DecongestionConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.contrib.taxi.optimizer.DefaultTaxiOptimizerProvider;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.run.TaxiModule;
import org.matsim.contrib.taxi.run.examples.TaxiDvrpModules;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import playground.ikaddoura.analysis.detailedPersonTripAnalysis.PersonTripAnalysisModule;
import playground.ikaddoura.analysis.linkDemand.LinkDemandEventHandler;

/**
 * @author ikaddoura
 *
 */
public class OptAVTestIT {
	
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();

	/**
	 * only taxi trips
	 * 
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
				new TaxiFareConfigGroup(),
				new OTFVisConfigGroup(),
				new NoiseConfigGroup());
		config1.planCalcScore().getModes().get(DefaultTaxiOptimizerProvider.TAXI_OPTIMIZER).setMonetaryDistanceRate(-0.01);

		config1.controler().setOutputDirectory(testUtils.getOutputDirectory() + "bc1");
		OptAVConfigGroup optAVParams1 = ConfigUtils.addOrGetModule(config1, OptAVConfigGroup.class);
		optAVParams1.setAccountForNoise(false);
		optAVParams1.setAccountForCongestion(false);

		Scenario scenario1 = ScenarioUtils.loadScenario(config1);
		Controler controler1 = new Controler(scenario1);
		
		controler1.addOverridingModule(TaxiDvrpModules.create());
		controler1.addOverridingModule(new TaxiModule());
		controler1.addOverridingModule(new OptAVModule(scenario1));		
		
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
				new TaxiFareConfigGroup(),
				new OTFVisConfigGroup(),
				new NoiseConfigGroup());
		config2.planCalcScore().getModes().get(DefaultTaxiOptimizerProvider.TAXI_OPTIMIZER).setMonetaryDistanceRate(-0.01);

		config2.controler().setOutputDirectory(testUtils.getOutputDirectory() + "n");
		OptAVConfigGroup optAVParams2 = ConfigUtils.addOrGetModule(config2, OptAVConfigGroup.class);
		optAVParams2.setAccountForNoise(true);
		optAVParams2.setAccountForCongestion(false);
		optAVParams2.setChargeSAVTollsFromPassengers(true);
		optAVParams2.setChargeTollsFromCarUsers(false);
		optAVParams2.setChargeTollsFromSAVDriver(true);

		Scenario scenario2 = ScenarioUtils.loadScenario(config2);
		Controler controler2 = new Controler(scenario2);
		
		controler2.addOverridingModule(TaxiDvrpModules.create());
		controler2.addOverridingModule(new TaxiModule());
		controler2.addOverridingModule(new OptAVModule(scenario2));		        
		controler2.addOverridingModule(new PersonTripAnalysisModule());
		
		if (otfvis) controler2.addOverridingModule(new OTFVisLiveModule());

		LinkDemandEventHandler handler2 = new LinkDemandEventHandler(controler2.getScenario().getNetwork());
		controler2.getEvents().addHandler(handler2);
		
		controler2.getConfig().controler().setCreateGraphs(false);
		
		// run
        controler2.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		controler2.run();
		
		// ##################################################################
		// noise pricing + high km-based cost
		// ##################################################################

		Config config3 = ConfigUtils.loadConfig(configFile,
				new OptAVConfigGroup(),
				new TaxiConfigGroup(),
				new DvrpConfigGroup(),
				new TaxiFareConfigGroup(),
				new OTFVisConfigGroup(),
				new NoiseConfigGroup());
		
		config3.controler().setOutputDirectory(testUtils.getOutputDirectory() + "n-with-operating-costs");
		config3.planCalcScore().getModes().get(DefaultTaxiOptimizerProvider.TAXI_OPTIMIZER).setMonetaryDistanceRate(-9999999.0);
		ConfigUtils.addOrGetModule(config3, TaxiFareConfigGroup.class).setDistanceFare_m(0.02);
		
		OptAVConfigGroup optAVParams3 = ConfigUtils.addOrGetModule(config3, OptAVConfigGroup.class);
		optAVParams3.setAccountForNoise(true);
		optAVParams3.setAccountForCongestion(false);
		optAVParams3.setChargeSAVTollsFromPassengers(true);
		optAVParams3.setChargeTollsFromSAVDriver(true);
		optAVParams3.setChargeTollsFromCarUsers(false);
		
		Scenario scenario3 = ScenarioUtils.loadScenario(config3);
		Controler controler3 = new Controler(scenario3);
		
		controler3.addOverridingModule(TaxiDvrpModules.create());
		controler3.addOverridingModule(new TaxiModule());
		controler3.addOverridingModule(new OptAVModule(scenario3));		        
		controler3.addOverridingModule(new PersonTripAnalysisModule());
		
		if (otfvis) controler2.addOverridingModule(new OTFVisLiveModule());

		LinkDemandEventHandler handler3 = new LinkDemandEventHandler(controler3.getScenario().getNetwork());
		controler3.getEvents().addHandler(handler3);
		
		controler3.getConfig().controler().setCreateGraphs(false);
		
		// run
        controler3.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		controler3.run();
		
		// print outs
					
		System.out.println("----------------------------------");
		System.out.println("Base case:");
		printResults1(handler1);
	
		System.out.println("----------------------------------");
		System.out.println("Noise pricing:");
		printResults1(handler2);
		
		System.out.println("----------------------------------");
		System.out.println("Noise pricing - with high operating costs:");
		printResults1(handler3);
		
		// the demand on the noise sensitive route should go down in case of noise pricing (n)
		Assert.assertEquals(true, getNoiseSensitiveRouteDemand(handler2) < getNoiseSensitiveRouteDemand(handler1));
		
		// the demand on the long and low-noise-cost route should go down in case of noise + operating cost pricing (n-with-operating-costs)
		Assert.assertEquals(true, getNoiseSensitiveRouteDemand(handler3) > getNoiseSensitiveRouteDemand(handler2));
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
				new TaxiFareConfigGroup(),
				new OTFVisConfigGroup(),
				new NoiseConfigGroup());
		
		config1.controler().setOutputDirectory(testUtils.getOutputDirectory() + "bc2");
		config1.travelTimeCalculator().setTraveltimeBinSize(60);
		
		final OptAVConfigGroup optAVParams1 = ConfigUtils.addOrGetModule(config1, OptAVConfigGroup.class);
		optAVParams1.setAccountForNoise(false);
		optAVParams1.setAccountForCongestion(false);
		
		Scenario scenario1 = ScenarioUtils.loadScenario(config1);
		Controler controler1 = new Controler(scenario1);
		
		// taxi
		
		controler1.addOverridingModule(TaxiDvrpModules.create());
		controler1.addOverridingModule(new TaxiModule());
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
				new TaxiFareConfigGroup(),
				new OTFVisConfigGroup());
		
		config2.controler().setOutputDirectory(testUtils.getOutputDirectory() + "c");
		config2.travelTimeCalculator().setTraveltimeBinSize(60);
		
		final OptAVConfigGroup optAVParams2 = ConfigUtils.addOrGetModule(config2, OptAVConfigGroup.class);
		optAVParams2.setAccountForNoise(false);
		optAVParams2.setAccountForCongestion(true);
		
		final DecongestionConfigGroup decongestionSettings = ConfigUtils.addOrGetModule(config2, DecongestionConfigGroup.class);
		decongestionSettings.setMsa(true);
		decongestionSettings.setTollBlendFactor(0.);
		decongestionSettings.setKd(0.);
		decongestionSettings.setKi(0.);
		decongestionSettings.setKp(999.);
		decongestionSettings.setFractionOfIterationsToEndPriceAdjustment(1.0);
		decongestionSettings.setFractionOfIterationsToStartPriceAdjustment(0.0);
		decongestionSettings.setUpdatePriceInterval(1);
		decongestionSettings.setWriteLinkInfoCharts(false);
		decongestionSettings.setRunFinalAnalysis(false);
		
		Scenario scenario2 = ScenarioUtils.loadScenario(config2);
		Controler controler2 = new Controler(scenario2);
			
		controler2.addOverridingModule(TaxiDvrpModules.create());
		controler2.addOverridingModule(new TaxiModule());
		controler2.addOverridingModule(new OptAVModule(scenario2));
		
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
