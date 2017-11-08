/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.ikaddoura.incidents;

import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeEstimator;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.withinday.trafficmonitoring.TravelTimeCollector;

import com.google.inject.name.Names;

import playground.ikaddoura.analysis.linkDemand.LinkDemandEventHandler;
import playground.ikaddoura.incidents.incidentWithinDayReplanning.IncidentBestRouteMobsimListener;

/**
 * 0: no network incident; no within-day replanning
 * 0a: network incident (reduced freespeed travel time before trip has started); no within-day replanning
 * 0b: network incident (reduced freespeed travel time directly after trip has started); no within-day replanning
 * 0c: network incident (reduced freespeed travel time after trip has started); no within-day replanning
 * 1: network incident (reduced freespeed travel time before trip has started); with within-day replanning
 * 2: network incident (reduced freespeed travel time directly after trip has started); with within-day replanning
 * 
 * 3a: two agents; no network incident; no within-day replanning
 * 3b: two agents; no network incident; with within-day replanning
 * 
* @author ikaddoura
* 
*/
public class IncidentWithinDayReplanningIT {

	LinkDemandEventHandler handler0;
	LinkDemandEventHandler handler0a;
	LinkDemandEventHandler handler0b;
	LinkDemandEventHandler handler0c;
	LinkDemandEventHandler handler1;
	LinkDemandEventHandler handler2;
	LinkDemandEventHandler handler3;

	LinkDemandEventHandler handler4a;
	LinkDemandEventHandler handler4b;

	final String networkFile = "network-2-routes.xml";	

	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();
	private int withinDayReplanInterval = 1;

	// see if the network change events are considered in the travel time computation
	@Test
	public final void test1() {
		
		{
			String outputDirectory = testUtils.getOutputDirectory() + "output-berlin-analysis_0/";
			
			final Config config = ConfigUtils.loadConfig(testUtils.getPackageInputDirectory() + "config.xml", new DvrpConfigGroup());

			config.network().setInputFile(networkFile);
			config.plans().setInputFile("plans.xml");
			config.controler().setOutputDirectory(outputDirectory);
			
			final Scenario scenario = ScenarioUtils.loadScenario(config);
			final Controler controler = new Controler(scenario);
			controler.getConfig().controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
			
			handler0 = new LinkDemandEventHandler(controler.getScenario().getNetwork());
			controler.getEvents().addHandler(handler0);
			
			controler.run();
		}
		
		{
			String networkChangeEventsFile = "networkChangeEvents-network-2-routes_7.00.xml";
			String outputDirectory = testUtils.getOutputDirectory() + "output-berlin-analysis_0a/";
			
			final Config config = ConfigUtils.loadConfig(testUtils.getPackageInputDirectory() + "config.xml", new DvrpConfigGroup());

			config.network().setInputFile(networkFile);
			config.network().setChangeEventsInputFile(networkChangeEventsFile);
			config.network().setTimeVariantNetwork(true);
			config.plans().setInputFile("plans.xml");
			config.controler().setOutputDirectory(outputDirectory);
			
			final Scenario scenario = ScenarioUtils.loadScenario(config);
			final Controler controler = new Controler(scenario);
			controler.getConfig().controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
			
			handler0a = new LinkDemandEventHandler(controler.getScenario().getNetwork());
			controler.getEvents().addHandler(handler0a);
			
			controler.run();
		}
		
		{
			String networkChangeEventsFile = "networkChangeEvents-network-2-routes_8.05.xml";
			String outputDirectory = testUtils.getOutputDirectory() + "output-berlin-analysis_0b/";
			
			final Config config = ConfigUtils.loadConfig(testUtils.getPackageInputDirectory() + "config.xml", new DvrpConfigGroup());

			config.network().setInputFile(networkFile);
			config.network().setChangeEventsInputFile(networkChangeEventsFile);
			config.network().setTimeVariantNetwork(true);
			config.plans().setInputFile("plans.xml");
			config.controler().setOutputDirectory(outputDirectory);
			
			final Scenario scenario = ScenarioUtils.loadScenario(config);
			final Controler controler = new Controler(scenario);
			controler.getConfig().controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
			
			handler0b = new LinkDemandEventHandler(controler.getScenario().getNetwork());
			controler.getEvents().addHandler(handler0b);
			
			controler.run();
		}
		
		{
			String networkChangeEventsFile = "networkChangeEvents-network-2-routes_10.00.xml";
			String outputDirectory = testUtils.getOutputDirectory() + "output-berlin-analysis_0c/";
			
			final Config config = ConfigUtils.loadConfig(testUtils.getPackageInputDirectory() + "config.xml");

			config.network().setInputFile(networkFile);
			config.network().setChangeEventsInputFile(networkChangeEventsFile);
			config.network().setTimeVariantNetwork(true);
			config.plans().setInputFile("plans.xml");
			config.controler().setOutputDirectory(outputDirectory);
			
			final Scenario scenario = ScenarioUtils.loadScenario(config);
			final Controler controler = new Controler(scenario);
			controler.getConfig().controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
			
			handler0c = new LinkDemandEventHandler(controler.getScenario().getNetwork());
			controler.getEvents().addHandler(handler0c);
			
			controler.run();
		}
		
		{
			String networkChangeEventsFile = "networkChangeEvents-network-2-routes_7.00.xml";
			String outputDirectory = testUtils.getOutputDirectory() + "output-berlin-analysis_1/";
			
			final Config config = ConfigUtils.loadConfig(testUtils.getPackageInputDirectory() + "config.xml", new DvrpConfigGroup());

			config.network().setInputFile(networkFile);
			config.network().setChangeEventsInputFile(networkChangeEventsFile);
			config.network().setTimeVariantNetwork(true);
			config.plans().setInputFile("plans.xml");
			config.controler().setOutputDirectory(outputDirectory);
			
			final Scenario scenario = ScenarioUtils.loadScenario(config);
			final Controler controler = new Controler(scenario);
			controler.getConfig().controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
			
			IncidentBestRouteMobsimListener incidentMobsimListener = new IncidentBestRouteMobsimListener();
			incidentMobsimListener.setOnlyReplanDirectlyAffectedAgents(false);
			incidentMobsimListener.setWithinDayReplanInterval(withinDayReplanInterval);

//			Set<String> analyzedModes = new HashSet<>();
//			analyzedModes.add(TransportMode.car);
//			final TravelTimeCollector travelTime = new TravelTimeCollector(controler.getScenario(), analyzedModes);
			
			// within-day replanning
			controler.addOverridingModule( new AbstractModule() {
				@Override public void install() {
										
					this.addMobsimListenerBinding().toInstance(incidentMobsimListener);
					this.addControlerListenerBinding().toInstance(incidentMobsimListener);
					
//					this.bind(TravelTime.class).toInstance(travelTime);
//					this.addEventHandlerBinding().toInstance(travelTime);
//					this.addMobsimListenerBinding().toInstance(travelTime);
					
					this.bind(TravelTime.class).to(DvrpTravelTimeEstimator.class);
					bind(Network.class).annotatedWith(Names.named(DvrpModule.DVRP_ROUTING)).to(Network.class).asEagerSingleton();
				}
			}) ;
			controler.addOverridingModule(new DvrpTravelTimeModule());
			
			handler1 = new LinkDemandEventHandler(controler.getScenario().getNetwork());
			controler.getEvents().addHandler(handler1);
			
			controler.run();
		}
		
		{
			String networkChangeEventsFile = "networkChangeEvents-network-2-routes_8.05.xml";
			String outputDirectory = testUtils.getOutputDirectory() + "output-berlin-analysis_2/";
			
			final Config config = ConfigUtils.loadConfig(testUtils.getPackageInputDirectory() + "config.xml", new DvrpConfigGroup());

			config.network().setInputFile(networkFile);
			config.network().setChangeEventsInputFile(networkChangeEventsFile);
			config.network().setTimeVariantNetwork(true);
			config.plans().setInputFile("plans.xml");
			config.controler().setOutputDirectory(outputDirectory);
			
			final Scenario scenario = ScenarioUtils.loadScenario(config);
			final Controler controler = new Controler(scenario);
			controler.getConfig().controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
			
			IncidentBestRouteMobsimListener incidentMobsimListener = new IncidentBestRouteMobsimListener();
			incidentMobsimListener.setOnlyReplanDirectlyAffectedAgents(false);
			incidentMobsimListener.setWithinDayReplanInterval(withinDayReplanInterval);

//			Set<String> analyzedModes = new HashSet<>();
//			analyzedModes.add(TransportMode.car);
//			final TravelTimeCollector travelTime = new TravelTimeCollector(controler.getScenario(), analyzedModes);
			
			// within-day replanning
			controler.addOverridingModule( new AbstractModule() {
				@Override public void install() {
										
					this.addMobsimListenerBinding().toInstance(incidentMobsimListener);
					this.addControlerListenerBinding().toInstance(incidentMobsimListener);
					
//					this.bind(TravelTime.class).toInstance(travelTime);
//					this.addEventHandlerBinding().toInstance(travelTime);
//					this.addMobsimListenerBinding().toInstance(travelTime);
					
					this.bind(TravelTime.class).to(DvrpTravelTimeEstimator.class);
					bind(Network.class).annotatedWith(Names.named(DvrpModule.DVRP_ROUTING)).to(Network.class).asEagerSingleton();
//					
				}
			}) ;
			controler.addOverridingModule(new DvrpTravelTimeModule());
			
			handler2 = new LinkDemandEventHandler(controler.getScenario().getNetwork());
			controler.getEvents().addHandler(handler2);
			
			controler.run();
		}
		
		{
			String networkChangeEventsFile = "networkChangeEvents-network-2-routes_10.00.xml";
			String outputDirectory = testUtils.getOutputDirectory() + "output-berlin-analysis_3/";
			
			final Config config = ConfigUtils.loadConfig(testUtils.getPackageInputDirectory() + "config.xml", new DvrpConfigGroup());

			config.network().setInputFile(networkFile);
			config.network().setChangeEventsInputFile(networkChangeEventsFile);
			config.network().setTimeVariantNetwork(true);
			config.plans().setInputFile("plans.xml");
			config.controler().setOutputDirectory(outputDirectory);
			
			final Scenario scenario = ScenarioUtils.loadScenario(config);
			final Controler controler = new Controler(scenario);
			controler.getConfig().controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
			
			IncidentBestRouteMobsimListener incidentMobsimListener = new IncidentBestRouteMobsimListener();
			incidentMobsimListener.setOnlyReplanDirectlyAffectedAgents(false);
			incidentMobsimListener.setWithinDayReplanInterval(withinDayReplanInterval);

//			Set<String> analyzedModes = new HashSet<>();
//			analyzedModes.add(TransportMode.car);
//			final TravelTimeCollector travelTime = new TravelTimeCollector(controler.getScenario(), analyzedModes);
			
			// within-day replanning
			controler.addOverridingModule( new AbstractModule() {
				@Override public void install() {
										
					this.addMobsimListenerBinding().toInstance(incidentMobsimListener);
					this.addControlerListenerBinding().toInstance(incidentMobsimListener);
					
//					this.bind(TravelTime.class).toInstance(travelTime);
//					this.addEventHandlerBinding().toInstance(travelTime);
//					this.addMobsimListenerBinding().toInstance(travelTime);
					
					this.bind(TravelTime.class).to(DvrpTravelTimeEstimator.class);
					bind(Network.class).annotatedWith(Names.named(DvrpModule.DVRP_ROUTING)).to(Network.class).asEagerSingleton();
//					
				}
			}) ;
			controler.addOverridingModule(new DvrpTravelTimeModule());
			
			handler3 = new LinkDemandEventHandler(controler.getScenario().getNetwork());
			controler.getEvents().addHandler(handler3);
			
			controler.run();
		}
		
		System.out.println("+++++++ 0: ");
		printResults(handler0);
		
		System.out.println("+++++++ 0a: ");
		printResults(handler0a);
		
		System.out.println("+++++++ 0b: ");
		printResults(handler0b);
		
		System.out.println("+++++++ 0c: ");
		printResults(handler0c);
		
		System.out.println("+++++++ 1: ");
		printResults(handler1);
		
		System.out.println("+++++++ 2: ");
		printResults(handler2);
		
		System.out.println("+++++++ 3: ");
		printResults(handler3);
		
		System.out.println("-------------");
	
		Assert.assertEquals(true, getLongRouteDemand(handler0) == 0 && getShortRouteDemand(handler0) == 1);
		
		Assert.assertEquals(true, getLongRouteDemand(handler0a) == 0 && getShortRouteDemand(handler0a) == 1);
		Assert.assertEquals(true, getLongRouteDemand(handler0b) == 0 && getShortRouteDemand(handler0b) == 1);
		Assert.assertEquals(true, getLongRouteDemand(handler0c) == 0 && getShortRouteDemand(handler0c) == 1);
		
		Assert.assertEquals(true, getLongRouteDemand(handler1) == 1 && getShortRouteDemand(handler1) == 0);
		Assert.assertEquals(true, getLongRouteDemand(handler2) == 1 && getShortRouteDemand(handler1) == 0);
		Assert.assertEquals(true, getLongRouteDemand(handler3) == 0 && getShortRouteDemand(handler3) == 1);
	}
	
	// see if the congestion effects are considered in the travel time computation
	@Test
	public final void test2() {
		{
			String outputDirectory = testUtils.getOutputDirectory() + "output-berlin-analysis_4a/";
			
			final Config config = ConfigUtils.loadConfig(testUtils.getPackageInputDirectory() + "config.xml", new DvrpConfigGroup());

			config.network().setInputFile(networkFile);
			config.plans().setInputFile("plans_2agents.xml");
			config.controler().setOutputDirectory(outputDirectory);
			
			final Scenario scenario = ScenarioUtils.loadScenario(config);
			final Controler controler = new Controler(scenario);
			controler.getConfig().controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
			
			handler4a = new LinkDemandEventHandler(controler.getScenario().getNetwork());
			controler.getEvents().addHandler(handler4a);
			
			controler.run();
		}
		
		{
			String outputDirectory = testUtils.getOutputDirectory() + "output-berlin-analysis_4b/";
			
			DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
			dvrpConfigGroup.setTravelTimeEstimationAlpha(1.0);
//			dvrpConfigGroup.setNetworkMode(TransportMode.car);
//			dvrpConfigGroup.setMode(TransportMode.car);
			final Config config = ConfigUtils.loadConfig(testUtils.getPackageInputDirectory() + "config.xml", dvrpConfigGroup);
			
			config.network().setInputFile(networkFile);
			config.plans().setInputFile("plans_2agents.xml");
			config.controler().setOutputDirectory(outputDirectory);
			
			final Scenario scenario = ScenarioUtils.loadScenario(config);
			final Controler controler = new Controler(scenario);
			controler.getConfig().controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
			
			IncidentBestRouteMobsimListener incidentMobsimListener = new IncidentBestRouteMobsimListener();
			incidentMobsimListener.setOnlyReplanDirectlyAffectedAgents(false);
			incidentMobsimListener.setWithinDayReplanInterval(withinDayReplanInterval);
			
//			Set<String> analyzedModes = new HashSet<>();
//			analyzedModes.add(TransportMode.car);
//			final TravelTimeCollector travelTime = new TravelTimeCollector(controler.getScenario(), analyzedModes);
			
			// within-day replanning
			controler.addOverridingModule( new AbstractModule() {
				@Override public void install() {
										
					this.addMobsimListenerBinding().toInstance(incidentMobsimListener);
					this.addControlerListenerBinding().toInstance(incidentMobsimListener);
					
//					this.bind(TravelTime.class).toInstance(travelTime);
//					this.addEventHandlerBinding().toInstance(travelTime);
//					this.addMobsimListenerBinding().toInstance(travelTime);
					
					this.bind(TravelTime.class).to(DvrpTravelTimeEstimator.class);
					bind(Network.class).annotatedWith(Names.named(DvrpModule.DVRP_ROUTING)).to(Network.class).asEagerSingleton();
				}
			}) ;
			controler.addOverridingModule(new DvrpTravelTimeModule());
			
			handler4b = new LinkDemandEventHandler(controler.getScenario().getNetwork());
			controler.getEvents().addHandler(handler4b);
			
			controler.run();
		}
		
		System.out.println("+++++++ 4a: ");
		printResults(handler4a);
		
		System.out.println("+++++++ 4b: ");
		printResults(handler4b);
		
		System.out.println("-------------");
		
		Assert.assertEquals(true, getLongRouteDemand(handler4a) == 0 && getShortRouteDemand(handler4a) == 2);
		Assert.assertEquals(true, getLongRouteDemand(handler4b) == 2 && getShortRouteDemand(handler4b) == 0);
	}
	
	private void printResults(LinkDemandEventHandler handler) {
		System.out.println("long route: " + getLongRouteDemand(handler));
		System.out.println("short route: " + getShortRouteDemand(handler));
	}
	
	private int getShortRouteDemand(LinkDemandEventHandler handler) {
		int demand = 0;
		if (handler.getLinkId2demand().containsKey(Id.createLinkId("link_7_8"))) {
			demand = handler.getLinkId2demand().get(Id.createLinkId("link_7_8"));
		}
		return demand;
	}
	
	private int getLongRouteDemand(LinkDemandEventHandler handler) {
		int demand = 0;
		if (handler.getLinkId2demand().containsKey(Id.createLinkId("link_3_6"))) {
			demand = handler.getLinkId2demand().get(Id.createLinkId("link_3_6"));
		}
		return demand;
	}
}

