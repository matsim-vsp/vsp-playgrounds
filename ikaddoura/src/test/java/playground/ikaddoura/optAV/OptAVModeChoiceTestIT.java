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

import java.util.Collections;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.av.robotaxi.scoring.TaxiFareConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.contrib.taxi.optimizer.DefaultTaxiOptimizerProvider;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizer;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.run.TaxiModule;
import org.matsim.contrib.taxi.run.TaxiQSimModule;
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
public class OptAVModeChoiceTestIT {
	
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();

	/**
	 * former car users and other users have same fix costs
	 * 
	 */
	@Test
	public final void test1(){

		String configFile = testUtils.getPackageInputDirectory() + "config3.xml";
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

		config1.controler().setOutputDirectory(testUtils.getOutputDirectory() + "1");
		OptAVConfigGroup optAVParams1 = ConfigUtils.addOrGetModule(config1, OptAVConfigGroup.class);
		optAVParams1.setAccountForNoise(false);
		optAVParams1.setAccountForCongestion(false);
		optAVParams1.setDailyFixCostAllSAVusers(1000.);
		optAVParams1.setFixCostsSAVinsteadOfCar(0);


		Scenario scenario1 = ScenarioUtils.loadScenario(config1);
		Controler controler1 = new Controler(scenario1);

		String mode1 = TaxiConfigGroup.get(config1).getMode();
		controler1.addQSimModule(new TaxiQSimModule());
		controler1.addOverridingModule(DvrpModule.createModule(mode1,
				Collections.singleton(TaxiOptimizer.class)));
		controler1.addOverridingModule(new TaxiModule());
		controler1.addOverridingModule(new OptAVModule(scenario1));		
		
		if (otfvis) controler1.addOverridingModule(new OTFVisLiveModule());	
		
		LinkDemandEventHandler handler1 = new LinkDemandEventHandler(controler1.getScenario().getNetwork());
		controler1.getEvents().addHandler(handler1);
		
		controler1.getConfig().controler().setCreateGraphs(true);

		// run
		
        controler1.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        controler1.run();
		
		// ##################################################################
		// low SAV fix cost
		// ##################################################################

		Config config2 = ConfigUtils.loadConfig(configFile,
				new OptAVConfigGroup(),
				new TaxiConfigGroup(),
				new DvrpConfigGroup(),
				new TaxiFareConfigGroup(),
				new OTFVisConfigGroup(),
				new NoiseConfigGroup());
		config2.planCalcScore().getModes().get(DefaultTaxiOptimizerProvider.TAXI_OPTIMIZER).setMonetaryDistanceRate(-0.01);

		config2.controler().setOutputDirectory(testUtils.getOutputDirectory() + "2");
		OptAVConfigGroup optAVParams2 = ConfigUtils.addOrGetModule(config2, OptAVConfigGroup.class);
		optAVParams2.setAccountForNoise(false);
		optAVParams2.setAccountForCongestion(false);
		optAVParams2.setDailyFixCostAllSAVusers(-10000.);
		optAVParams2.setFixCostsSAVinsteadOfCar(0);
		
		Scenario scenario2 = ScenarioUtils.loadScenario(config2);
		Controler controler2 = new Controler(scenario2);

		String mode2 = TaxiConfigGroup.get(config2).getMode();
		controler2.addQSimModule(new TaxiQSimModule());
		controler2.addOverridingModule(DvrpModule.createModule(mode2,
				Collections.singleton(TaxiOptimizer.class)));
		controler2.addOverridingModule(new TaxiModule());
		controler2.addOverridingModule(new OptAVModule(scenario2));		        
		
		if (otfvis) controler2.addOverridingModule(new OTFVisLiveModule());

		LinkDemandEventHandler handler2 = new LinkDemandEventHandler(controler2.getScenario().getNetwork());
		controler2.getEvents().addHandler(handler2);
		
		controler2.getConfig().controler().setCreateGraphs(true);
		
		// run
        controler2.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		controler2.run();
		
		// print outs
					
		System.out.println("----------------------------------");
		System.out.println("High SAV fix costs for all users:");
		printResults1(handler1);
	
		System.out.println("----------------------------------");
		System.out.println("Very low SAV fix costs for all users:");
		printResults1(handler2);
		
		// the number of taxi trips should increase
		Assert.assertEquals(true, getRoadTrafficVolume(handler1) == 0);
		Assert.assertEquals(true, getRoadTrafficVolume(handler2) == 10);
	}
	
	/**
	 * former car users and other users have different fix costs
	 * 
	 */
	@Test
	public final void test2(){

		String configFile = testUtils.getPackageInputDirectory() + "config3.xml";
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

		config1.controler().setOutputDirectory(testUtils.getOutputDirectory() + "1");
		OptAVConfigGroup optAVParams1 = ConfigUtils.addOrGetModule(config1, OptAVConfigGroup.class);
		optAVParams1.setAccountForNoise(false);
		optAVParams1.setAccountForCongestion(false);
		optAVParams1.setDailyFixCostAllSAVusers(1000.);
		optAVParams1.setFixCostsSAVinsteadOfCar(0);

		Scenario scenario1 = ScenarioUtils.loadScenario(config1);
		Controler controler1 = new Controler(scenario1);

		String mode1 = TaxiConfigGroup.get(config1).getMode();
		controler1.addQSimModule(new TaxiQSimModule());
		controler1.addOverridingModule(DvrpModule.createModule(mode1,
				Collections.singleton(TaxiOptimizer.class)));
		controler1.addOverridingModule(new TaxiModule());
		controler1.addOverridingModule(new OptAVModule(scenario1));		
		controler1.addOverridingModule(new PersonTripAnalysisModule());
		
		if (otfvis) controler1.addOverridingModule(new OTFVisLiveModule());	
		
		LinkDemandEventHandler handler1 = new LinkDemandEventHandler(controler1.getScenario().getNetwork());
		controler1.getEvents().addHandler(handler1);
		
		controler1.getConfig().controler().setCreateGraphs(true);

		// run
		
        controler1.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        controler1.run();
		
		// ##################################################################
		// low SAV fix cost
		// ##################################################################

		Config config2 = ConfigUtils.loadConfig(configFile,
				new OptAVConfigGroup(),
				new TaxiConfigGroup(),
				new DvrpConfigGroup(),
				new TaxiFareConfigGroup(),
				new OTFVisConfigGroup(),
				new NoiseConfigGroup());
		config2.planCalcScore().getModes().get(DefaultTaxiOptimizerProvider.TAXI_OPTIMIZER).setMonetaryDistanceRate(-0.01);

		config2.controler().setOutputDirectory(testUtils.getOutputDirectory() + "2");
		OptAVConfigGroup optAVParams2 = ConfigUtils.addOrGetModule(config2, OptAVConfigGroup.class);
		optAVParams2.setAccountForNoise(false);
		optAVParams2.setAccountForCongestion(false);
		optAVParams2.setFixCostsSAVinsteadOfCar(-10000);
		optAVParams2.setDailyFixCostAllSAVusers(1000.);
		
		Scenario scenario2 = ScenarioUtils.loadScenario(config2);
		Controler controler2 = new Controler(scenario2);

		String mode2 = TaxiConfigGroup.get(config2).getMode();
		controler2.addQSimModule(new TaxiQSimModule());
		controler2.addOverridingModule(DvrpModule.createModule(mode2,
				Collections.singleton(TaxiOptimizer.class)));
		controler2.addOverridingModule(new TaxiModule());
		controler2.addOverridingModule(new OptAVModule(scenario2));		        
		controler2.addOverridingModule(new PersonTripAnalysisModule());
		
		if (otfvis) controler2.addOverridingModule(new OTFVisLiveModule());

		LinkDemandEventHandler handler2 = new LinkDemandEventHandler(controler2.getScenario().getNetwork());
		controler2.getEvents().addHandler(handler2);
		
		controler2.getConfig().controler().setCreateGraphs(true);
		
		// run
        controler2.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		controler2.run();
		
		// print outs
					
		System.out.println("----------------------------------");
		System.out.println("High SAV fix costs for all users:");
		printResults1(handler1);
	
		System.out.println("----------------------------------");
		System.out.println("Very low SAV fix costs for former car users:");
		printResults1(handler2);
		
		// the number of taxi trips should increase
		Assert.assertEquals(true, getRoadTrafficVolume(handler1) == 0);
		Assert.assertEquals(true, getRoadTrafficVolume(handler2) == 2);
	}
	
	private void printResults1(LinkDemandEventHandler handler) {
		System.out.println("taxi demand " + (getRoadTrafficVolume(handler)));
	}
	
	private int getRoadTrafficVolume(LinkDemandEventHandler handler) {
		int noiseSensitiveRouteDemand = 0;
		if (handler.getLinkId2demand().containsKey(Id.createLinkId("link_1_2"))) {
			noiseSensitiveRouteDemand = handler.getLinkId2demand().get(Id.createLinkId("link_1_2"));
		}
		return noiseSensitiveRouteDemand;
	}
		
}
