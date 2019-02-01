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
package playground.ikaddoura.savPricing;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.av.robotaxi.fares.taxi.TaxiFareConfigGroup;
import org.matsim.contrib.av.robotaxi.fares.taxi.TaxiFareModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.run.TaxiControlerCreator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.sav.DailyRewardHandlerSAVInsteadOfCar;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

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
				new SAVPricingConfigGroup(),
				new TaxiConfigGroup(),
				new DvrpConfigGroup(),
				new TaxiFareConfigGroup(),
				new OTFVisConfigGroup(),
				new NoiseConfigGroup());
		config1.planCalcScore().getModes().get(SAVPricingModule.TAXI_OPTIMIZER).setMonetaryDistanceRate(-0.01);
		config1.planCalcScore().getModes().get(TransportMode.taxi).setDailyMonetaryConstant(-1000.);
		config1.controler().setOutputDirectory(testUtils.getOutputDirectory() + "1");
		SAVPricingConfigGroup optAVParams1 = ConfigUtils.addOrGetModule(config1, SAVPricingConfigGroup.class);
		optAVParams1.setAccountForNoise(false);
		optAVParams1.setAccountForCongestion(false);
//		optAVParams1.setDailyFixCostAllSAVusers(1000.);
//		optAVParams1.setFixCostsSAVinsteadOfCar(0);

		Controler controler1 = TaxiControlerCreator.createControler(config1, otfvis);
		controler1.addOverridingModule(new SAVPricingModule(controler1.getScenario(), TransportMode.car));	
		
		// taxi fares
		controler1.addOverridingModule(new TaxiFareModule());
		
		// rewards for no longer owning a car
		controler1.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.addEventHandlerBinding().toInstance(new DailyRewardHandlerSAVInsteadOfCar(0., TransportMode.car));			
				org.matsim.sav.SAVPassengerTrackerImpl tracker = new org.matsim.sav.SAVPassengerTrackerImpl(TransportMode.taxi);
				this.bind(org.matsim.sav.SAVPassengerTracker.class).toInstance(tracker);
				this.addEventHandlerBinding().toInstance(tracker);
			}
		});
		
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
				new SAVPricingConfigGroup(),
				new TaxiConfigGroup(),
				new DvrpConfigGroup(),
				new TaxiFareConfigGroup(),
				new OTFVisConfigGroup(),
				new NoiseConfigGroup());
		config2.planCalcScore().getModes().get(SAVPricingModule.TAXI_OPTIMIZER).setMonetaryDistanceRate(-0.01);
		config2.planCalcScore().getModes().get(TransportMode.taxi).setDailyMonetaryConstant(10000.);
		config2.controler().setOutputDirectory(testUtils.getOutputDirectory() + "2");
		SAVPricingConfigGroup optAVParams2 = ConfigUtils.addOrGetModule(config2, SAVPricingConfigGroup.class);
		optAVParams2.setAccountForNoise(false);
		optAVParams2.setAccountForCongestion(false);
//		optAVParams2.setDailyFixCostAllSAVusers(-10000.);
//		optAVParams2.setFixCostsSAVinsteadOfCar(0);
		
		Controler controler2 = TaxiControlerCreator.createControler(config2, otfvis);
		controler2.addOverridingModule(new SAVPricingModule(controler2.getScenario(), TransportMode.car));	

		// taxi fares
		controler2.addOverridingModule(new TaxiFareModule());
		
		// rewards for no longer owning a car
		controler2.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.addEventHandlerBinding().toInstance(new DailyRewardHandlerSAVInsteadOfCar(0., TransportMode.car));			
				org.matsim.sav.SAVPassengerTrackerImpl tracker = new org.matsim.sav.SAVPassengerTrackerImpl(TransportMode.taxi);
				this.bind(org.matsim.sav.SAVPassengerTracker.class).toInstance(tracker);
				this.addEventHandlerBinding().toInstance(tracker);
			}
		});	        
		
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
				new SAVPricingConfigGroup(),
				new TaxiConfigGroup(),
				new DvrpConfigGroup(),
				new TaxiFareConfigGroup(),
				new OTFVisConfigGroup(),
				new NoiseConfigGroup());
		config1.planCalcScore().getModes().get(SAVPricingModule.TAXI_OPTIMIZER).setMonetaryDistanceRate(-0.01);
		config1.planCalcScore().getModes().get(TransportMode.taxi).setDailyMonetaryConstant(-1000.);

		config1.controler().setOutputDirectory(testUtils.getOutputDirectory() + "1");
		SAVPricingConfigGroup optAVParams1 = ConfigUtils.addOrGetModule(config1, SAVPricingConfigGroup.class);
		optAVParams1.setAccountForNoise(false);
		optAVParams1.setAccountForCongestion(false);
//		optAVParams1.setDailyFixCostAllSAVusers(1000.);
//		optAVParams1.setFixCostsSAVinsteadOfCar(0);

		Controler controler1 = TaxiControlerCreator.createControler(config1, otfvis);
		controler1.addOverridingModule(new SAVPricingModule(controler1.getScenario(), TransportMode.car));	

		// taxi fares
		controler1.addOverridingModule(new TaxiFareModule());
		
		// rewards for no longer owning a car
		controler1.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.addEventHandlerBinding().toInstance(new DailyRewardHandlerSAVInsteadOfCar(0., TransportMode.car));			
				org.matsim.sav.SAVPassengerTrackerImpl tracker = new org.matsim.sav.SAVPassengerTrackerImpl(TransportMode.taxi);
				this.bind(org.matsim.sav.SAVPassengerTracker.class).toInstance(tracker);
				this.addEventHandlerBinding().toInstance(tracker);
			}
		});	 
		
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
				new SAVPricingConfigGroup(),
				new TaxiConfigGroup(),
				new DvrpConfigGroup(),
				new TaxiFareConfigGroup(),
				new OTFVisConfigGroup(),
				new NoiseConfigGroup());
		config2.planCalcScore().getModes().get(SAVPricingModule.TAXI_OPTIMIZER).setMonetaryDistanceRate(-0.01);
		config2.planCalcScore().getModes().get(TransportMode.taxi).setDailyMonetaryConstant(-1000.);

		config2.controler().setOutputDirectory(testUtils.getOutputDirectory() + "2");
		SAVPricingConfigGroup optAVParams2 = ConfigUtils.addOrGetModule(config2, SAVPricingConfigGroup.class);
		optAVParams2.setAccountForNoise(false);
		optAVParams2.setAccountForCongestion(false);
//		optAVParams2.setFixCostsSAVinsteadOfCar(-10000);
//		optAVParams2.setDailyFixCostAllSAVusers(1000.);
		
		Controler controler2 = TaxiControlerCreator.createControler(config2, otfvis);
		controler2.addOverridingModule(new SAVPricingModule(controler2.getScenario(), TransportMode.car));	

		// taxi fares
		controler2.addOverridingModule(new TaxiFareModule());
		
		// rewards for no longer owning a car
		controler2.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.addEventHandlerBinding().toInstance(new DailyRewardHandlerSAVInsteadOfCar(10000., TransportMode.car));			
				org.matsim.sav.SAVPassengerTrackerImpl tracker = new org.matsim.sav.SAVPassengerTrackerImpl(TransportMode.taxi);
				this.bind(org.matsim.sav.SAVPassengerTracker.class).toInstance(tracker);
				this.addEventHandlerBinding().toInstance(tracker);
			}
		});	
		
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
