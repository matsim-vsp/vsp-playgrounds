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
 * 
 * 1: network incident (reduced freespeed travel time before trip has started); with within-day replanning
 * 2: network incident (reduced freespeed travel time directly after trip has started); with within-day replanning
 * 3: network incident (reduced freespeed travel time long after trip has started); with within-day replanning
 * (a=dvrp approach; b=dobler approach)
 * 
 * 5: two agents; no network incident; with within-day replanning
 * (a=dvrp approach; b=dobler approach)
 * 
* @author ikaddoura
* 
*/
public class IncidentWithinDayReplanningIT {
	
	private final String networkFile = "network-2-routes.xml";	
	private final int withinDayReplanInterval = 1;

	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();

	// see if the network change events are considered in the travel time computation
	// dvrp approach
	@Test
	public final void test1a() {
		
		LinkDemandEventHandler handler0;
		
		LinkDemandEventHandler handler1a;
		LinkDemandEventHandler handler2a;
		LinkDemandEventHandler handler3a;
		
		{
			String outputDirectory = testUtils.getOutputDirectory() + "output_0/";
			
			final Config config = ConfigUtils.loadConfig(testUtils.getPackageInputDirectory() + "config.xml", new DvrpConfigGroup());

			config.network().setInputFile(networkFile);
			config.plans().setInputFile("plans.xml");
			config.controler().setOutputDirectory(outputDirectory);
			config.controler().setFirstIteration(0);
			config.controler().setLastIteration(1);
			
			final Scenario scenario = ScenarioUtils.loadScenario(config);
			final Controler controler = new Controler(scenario);
			controler.getConfig().controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
			
			handler0 = new LinkDemandEventHandler(controler.getScenario().getNetwork());
			controler.getEvents().addHandler(handler0);
			
			controler.run();
		}
		
		{
			String networkChangeEventsFile = "networkChangeEvents-network-2-routes_7.00.xml";
			String outputDirectory = testUtils.getOutputDirectory() + "output_1a/";
			
			final Config config = ConfigUtils.loadConfig(testUtils.getPackageInputDirectory() + "config.xml", new DvrpConfigGroup());
			config.controler().setFirstIteration(0);
			config.controler().setLastIteration(1);

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

			// within-day replanning
			controler.addOverridingModule( new AbstractModule() {
				@Override public void install() {
										
					this.addMobsimListenerBinding().toInstance(incidentMobsimListener);
					this.addControlerListenerBinding().toInstance(incidentMobsimListener);
				
					this.bind(TravelTime.class).to(DvrpTravelTimeEstimator.class);
					bind(Network.class).annotatedWith(Names.named(DvrpModule.DVRP_ROUTING)).to(Network.class).asEagerSingleton();
				}
			}) ;
			controler.addOverridingModule(new DvrpTravelTimeModule());
			
			handler1a = new LinkDemandEventHandler(controler.getScenario().getNetwork());
			controler.getEvents().addHandler(handler1a);
			
			controler.run();
		}
		
		{
			String networkChangeEventsFile = "networkChangeEvents-network-2-routes_8.05.xml";
			String outputDirectory = testUtils.getOutputDirectory() + "output_2a/";
			
			final Config config = ConfigUtils.loadConfig(testUtils.getPackageInputDirectory() + "config.xml", new DvrpConfigGroup());

			config.network().setInputFile(networkFile);
			config.network().setChangeEventsInputFile(networkChangeEventsFile);
			config.network().setTimeVariantNetwork(true);
			config.plans().setInputFile("plans.xml");
			config.controler().setOutputDirectory(outputDirectory);
			config.controler().setFirstIteration(0);
			config.controler().setLastIteration(1);
			
			final Scenario scenario = ScenarioUtils.loadScenario(config);
			final Controler controler = new Controler(scenario);
			controler.getConfig().controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
			
			IncidentBestRouteMobsimListener incidentMobsimListener = new IncidentBestRouteMobsimListener();
			incidentMobsimListener.setOnlyReplanDirectlyAffectedAgents(false);
			incidentMobsimListener.setWithinDayReplanInterval(withinDayReplanInterval);

			// within-day replanning
			controler.addOverridingModule( new AbstractModule() {
				@Override public void install() {
										
					this.addMobsimListenerBinding().toInstance(incidentMobsimListener);
					this.addControlerListenerBinding().toInstance(incidentMobsimListener);
					
					this.bind(TravelTime.class).to(DvrpTravelTimeEstimator.class);
					bind(Network.class).annotatedWith(Names.named(DvrpModule.DVRP_ROUTING)).to(Network.class).asEagerSingleton();
				}
			}) ;
			controler.addOverridingModule(new DvrpTravelTimeModule());
			
			handler2a = new LinkDemandEventHandler(controler.getScenario().getNetwork());
			controler.getEvents().addHandler(handler2a);
			
			controler.run();
		}
		
		{
			String networkChangeEventsFile = "networkChangeEvents-network-2-routes_10.00.xml";
			String outputDirectory = testUtils.getOutputDirectory() + "output_3a/";
			
			final Config config = ConfigUtils.loadConfig(testUtils.getPackageInputDirectory() + "config.xml", new DvrpConfigGroup());

			config.network().setInputFile(networkFile);
			config.network().setChangeEventsInputFile(networkChangeEventsFile);
			config.network().setTimeVariantNetwork(true);
			config.plans().setInputFile("plans.xml");
			config.controler().setOutputDirectory(outputDirectory);
			config.controler().setFirstIteration(0);
			config.controler().setLastIteration(1);
			
			final Scenario scenario = ScenarioUtils.loadScenario(config);
			final Controler controler = new Controler(scenario);
			controler.getConfig().controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
			
			IncidentBestRouteMobsimListener incidentMobsimListener = new IncidentBestRouteMobsimListener();
			incidentMobsimListener.setOnlyReplanDirectlyAffectedAgents(false);
			incidentMobsimListener.setWithinDayReplanInterval(withinDayReplanInterval);

			// within-day replanning
			controler.addOverridingModule( new AbstractModule() {
				@Override public void install() {
										
					this.addMobsimListenerBinding().toInstance(incidentMobsimListener);
					this.addControlerListenerBinding().toInstance(incidentMobsimListener);
					
					this.bind(TravelTime.class).to(DvrpTravelTimeEstimator.class);
					bind(Network.class).annotatedWith(Names.named(DvrpModule.DVRP_ROUTING)).to(Network.class).asEagerSingleton();
				}
			}) ;
			controler.addOverridingModule(new DvrpTravelTimeModule());
			
			handler3a = new LinkDemandEventHandler(controler.getScenario().getNetwork());
			controler.getEvents().addHandler(handler3a);
			
			controler.run();
		}
		
		System.out.println("+++++++ 0: ");
		printResults(handler0);
		
		System.out.println("+++++++ 1a: ");
		printResults(handler1a);
		
		System.out.println("+++++++ 2a: ");
		printResults(handler2a);
		
		System.out.println("+++++++ 3a: ");
		printResults(handler3a);
		
		System.out.println("-------------");
	
		Assert.assertEquals(true, getLongRouteDemand(handler0) == 0 && getShortRouteDemand(handler0) == 1);
		
		Assert.assertEquals(true, getLongRouteDemand(handler1a) == 1 && getShortRouteDemand(handler1a) == 0);
		Assert.assertEquals(true, getLongRouteDemand(handler2a) == 1 && getShortRouteDemand(handler1a) == 0);
		Assert.assertEquals(true, getLongRouteDemand(handler3a) == 0 && getShortRouteDemand(handler3a) == 1);
		
	}
	
	// see if the network change events are considered in the travel time computation
	// dobler approach
	@Test
	public final void test1bNetworkChangeEventBeforeTrip() {
		
		LinkDemandEventHandler handler1b;
		
		{
			String networkChangeEventsFile = "networkChangeEvents-network-2-routes_7.00.xml";
			String outputDirectory = testUtils.getOutputDirectory() + "output_1b/";
			
			final Config config = ConfigUtils.loadConfig(testUtils.getPackageInputDirectory() + "config.xml");

			config.network().setInputFile(networkFile);
			config.network().setChangeEventsInputFile(networkChangeEventsFile);
			config.network().setTimeVariantNetwork(true);
			config.plans().setInputFile("plans.xml");
			config.controler().setOutputDirectory(outputDirectory);
			config.controler().setFirstIteration(0);
			config.controler().setLastIteration(0);
			
			final Scenario scenario = ScenarioUtils.loadScenario(config);
			final Controler controler = new Controler(scenario);
			controler.getConfig().controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
			
			IncidentBestRouteMobsimListener incidentMobsimListener = new IncidentBestRouteMobsimListener();
			incidentMobsimListener.setOnlyReplanDirectlyAffectedAgents(false);
			incidentMobsimListener.setWithinDayReplanInterval(withinDayReplanInterval);

			Set<String> analyzedModes = new HashSet<>();
			analyzedModes.add(TransportMode.car);
			final TravelTimeCollector travelTime = new TravelTimeCollector(controler.getScenario(), analyzedModes);
			
			// within-day replanning
			controler.addOverridingModule( new AbstractModule() {
				@Override public void install() {
										
					this.addMobsimListenerBinding().toInstance(incidentMobsimListener);
					this.addControlerListenerBinding().toInstance(incidentMobsimListener);
					
					this.bind(TravelTime.class).toInstance(travelTime);
					this.addEventHandlerBinding().toInstance(travelTime);
					this.addMobsimListenerBinding().toInstance(travelTime);
					
				}
			}) ;
			
			handler1b = new LinkDemandEventHandler(controler.getScenario().getNetwork());
			controler.getEvents().addHandler(handler1b);
			
			controler.run();
		}
		
		System.out.println("+++++++ 1b: ");
		printResults(handler1b);

		System.out.println("-------------");
			
		Assert.assertEquals(true, getLongRouteDemand(handler1b) == 1 && getShortRouteDemand(handler1b) == 0);
	}
	
	// see if the network change events are considered in the travel time computation
	// dobler approach
	@Test
	public final void test1bNetworkChangeEventDuringTrip() {
		
		LinkDemandEventHandler handler2b;
		{
			String networkChangeEventsFile = "networkChangeEvents-network-2-routes_8.05.xml";
			String outputDirectory = testUtils.getOutputDirectory() + "output_2b/";
			
			final Config config = ConfigUtils.loadConfig(testUtils.getPackageInputDirectory() + "config.xml");

			config.network().setInputFile(networkFile);
			config.network().setChangeEventsInputFile(networkChangeEventsFile);
			config.network().setTimeVariantNetwork(true);
			config.plans().setInputFile("plans.xml");
			config.controler().setOutputDirectory(outputDirectory);
			config.controler().setFirstIteration(0);
			config.controler().setLastIteration(0);
			
			final Scenario scenario = ScenarioUtils.loadScenario(config);
			final Controler controler = new Controler(scenario);
			controler.getConfig().controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
			
			IncidentBestRouteMobsimListener incidentMobsimListener = new IncidentBestRouteMobsimListener();
			incidentMobsimListener.setOnlyReplanDirectlyAffectedAgents(false);
			incidentMobsimListener.setWithinDayReplanInterval(withinDayReplanInterval);

			Set<String> analyzedModes = new HashSet<>();
			analyzedModes.add(TransportMode.car);
			final TravelTimeCollector travelTime = new TravelTimeCollector(controler.getScenario(), analyzedModes);
			
			// within-day replanning
			controler.addOverridingModule( new AbstractModule() {
				@Override public void install() {
										
					this.addMobsimListenerBinding().toInstance(incidentMobsimListener);
					this.addControlerListenerBinding().toInstance(incidentMobsimListener);
					
					this.bind(TravelTime.class).toInstance(travelTime);
					this.addEventHandlerBinding().toInstance(travelTime);
					this.addMobsimListenerBinding().toInstance(travelTime);
					
				}
			}) ;
			
			handler2b = new LinkDemandEventHandler(controler.getScenario().getNetwork());
			controler.getEvents().addHandler(handler2b);
			
			controler.run();
		}

		System.out.println("+++++++ 2b: ");
		printResults(handler2b);
	
		System.out.println("-------------");
			
		Assert.assertEquals(true, getLongRouteDemand(handler2b) == 1 && getShortRouteDemand(handler2b) == 0);
	}
	
	// see if the network change events are considered in the travel time computation
	// with withing-day replanning
	// dobler approach
	@Test
	public final void test1bChangeEventAfterTripWithReplanning() {
		
		LinkDemandEventHandler handler3b;
		{
			String networkChangeEventsFile = "networkChangeEvents-network-2-routes_10.00.xml";
			String outputDirectory = testUtils.getOutputDirectory() + "output_3b_withReplanning/";
			
			final Config config = ConfigUtils.loadConfig(testUtils.getPackageInputDirectory() + "config.xml");

			config.network().setInputFile(networkFile);
			config.network().setChangeEventsInputFile(networkChangeEventsFile);
			config.network().setTimeVariantNetwork(true);
			config.plans().setInputFile("plans.xml");
			config.controler().setOutputDirectory(outputDirectory);
			config.controler().setFirstIteration(0);
			config.controler().setLastIteration(0);
			
			final Scenario scenario = ScenarioUtils.loadScenario(config);
			final Controler controler = new Controler(scenario);
			controler.getConfig().controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
			
			IncidentBestRouteMobsimListener incidentMobsimListener = new IncidentBestRouteMobsimListener();
			incidentMobsimListener.setOnlyReplanDirectlyAffectedAgents(false);
			incidentMobsimListener.setWithinDayReplanInterval(withinDayReplanInterval);

			Set<String> analyzedModes = new HashSet<>();
			analyzedModes.add(TransportMode.car);
			final TravelTimeCollector travelTime = new TravelTimeCollector(controler.getScenario(), analyzedModes);
			
			// within-day replanning
			controler.addOverridingModule( new AbstractModule() {
				@Override public void install() {
										
					this.addMobsimListenerBinding().toInstance(incidentMobsimListener);
					this.addControlerListenerBinding().toInstance(incidentMobsimListener);
					
					this.bind(TravelTime.class).toInstance(travelTime);
					this.addEventHandlerBinding().toInstance(travelTime);
					this.addMobsimListenerBinding().toInstance(travelTime);
				}
			}) ;
			
			handler3b = new LinkDemandEventHandler(controler.getScenario().getNetwork());
			controler.getEvents().addHandler(handler3b);
			
			controler.run();
		}
		
		// there is no reason for taking the longer route. The network change event (speed reduction on the short route) occurs when the trip is over.
		Assert.assertEquals(true, getLongRouteDemand(handler3b) == 0 && getShortRouteDemand(handler3b) == 1);
	}
	
	// see if the congestion effects are considered in the travel time computation (10 agents)
	// dvrp approach
	@Test
	public final void test3a() {
		
		LinkDemandEventHandler handler6;
		LinkDemandEventHandler handler7a;
		
		{
			String outputDirectory = testUtils.getOutputDirectory() + "output_6/";
			
			final Config config = ConfigUtils.loadConfig(testUtils.getPackageInputDirectory() + "config.xml", new DvrpConfigGroup());

			config.network().setInputFile(networkFile);
			config.plans().setInputFile("plans_moreAgents.xml");
			config.controler().setOutputDirectory(outputDirectory);
			config.controler().setFirstIteration(0);
			config.controler().setLastIteration(1);
			
			final Scenario scenario = ScenarioUtils.loadScenario(config);
			final Controler controler = new Controler(scenario);
			controler.getConfig().controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
			
			handler6 = new LinkDemandEventHandler(controler.getScenario().getNetwork());
			controler.getEvents().addHandler(handler6);
			
			controler.run();
		}
		
		{
			String outputDirectory = testUtils.getOutputDirectory() + "output_7a/";
			
			DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
			dvrpConfigGroup.setTravelTimeEstimationAlpha(1.0);
			final Config config = ConfigUtils.loadConfig(testUtils.getPackageInputDirectory() + "config.xml", dvrpConfigGroup);
			
			config.network().setInputFile(networkFile);
			config.plans().setInputFile("plans_moreAgents.xml");
			config.controler().setOutputDirectory(outputDirectory);
			config.controler().setFirstIteration(0);
			config.controler().setLastIteration(1);
			
			final Scenario scenario = ScenarioUtils.loadScenario(config);
			final Controler controler = new Controler(scenario);
			controler.getConfig().controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
			
			IncidentBestRouteMobsimListener incidentMobsimListener = new IncidentBestRouteMobsimListener();
			incidentMobsimListener.setOnlyReplanDirectlyAffectedAgents(false);
			incidentMobsimListener.setWithinDayReplanInterval(withinDayReplanInterval);
				
			// within-day replanning
			controler.addOverridingModule( new AbstractModule() {
				@Override public void install() {
										
					this.addMobsimListenerBinding().toInstance(incidentMobsimListener);
					this.addControlerListenerBinding().toInstance(incidentMobsimListener);
					
					this.bind(TravelTime.class).to(DvrpTravelTimeEstimator.class);
					bind(Network.class).annotatedWith(Names.named(DvrpModule.DVRP_ROUTING)).to(Network.class).asEagerSingleton();
				}
			}) ;
			controler.addOverridingModule(new DvrpTravelTimeModule());
			
			handler7a = new LinkDemandEventHandler(controler.getScenario().getNetwork());
			controler.getEvents().addHandler(handler7a);
			
			controler.run();
		}
		
		System.out.println("+++++++ 6: ");
		printResults(handler6);
		
		System.out.println("+++++++ 7a: ");
		printResults(handler7a);
		
		System.out.println("-------------");
		
		Assert.assertEquals(true, getLongRouteDemand(handler6) == 0 && getShortRouteDemand(handler6) == 10);
		Assert.assertEquals(true, getLongRouteDemand(handler7a) == 10 && getShortRouteDemand(handler7a) == 0);
	}
	
	// see if the congestion effects are considered in the travel time computation (10 agents)
	// dobler approach
	@Test
	public final void test3b() {
		
		LinkDemandEventHandler handler7b;

		{
			String outputDirectory = testUtils.getOutputDirectory() + "output_7b/";
			final Config config = ConfigUtils.loadConfig(testUtils.getPackageInputDirectory() + "config.xml");
			
			config.network().setInputFile(networkFile);
			config.plans().setInputFile("plans_moreAgents.xml");
			config.controler().setOutputDirectory(outputDirectory);
			config.controler().setFirstIteration(0);
			config.controler().setLastIteration(0);
			
			final Scenario scenario = ScenarioUtils.loadScenario(config);
			final Controler controler = new Controler(scenario);
			controler.getConfig().controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
			
			IncidentBestRouteMobsimListener incidentMobsimListener = new IncidentBestRouteMobsimListener();
			incidentMobsimListener.setOnlyReplanDirectlyAffectedAgents(false);
			incidentMobsimListener.setWithinDayReplanInterval(withinDayReplanInterval);
			
			Set<String> analyzedModes = new HashSet<>();
			analyzedModes.add(TransportMode.car);
			final TravelTimeCollector travelTime = new TravelTimeCollector(controler.getScenario(), analyzedModes);
			
			// within-day replanning
			controler.addOverridingModule( new AbstractModule() {
				@Override public void install() {
										
					this.addMobsimListenerBinding().toInstance(incidentMobsimListener);
					this.addControlerListenerBinding().toInstance(incidentMobsimListener);
					
					this.bind(TravelTime.class).toInstance(travelTime);
					this.addEventHandlerBinding().toInstance(travelTime);
					this.addMobsimListenerBinding().toInstance(travelTime);
					
				}
			}) ;
			
			handler7b = new LinkDemandEventHandler(controler.getScenario().getNetwork());
			controler.getEvents().addHandler(handler7b);
			
			controler.run();
		}
	
		System.out.println("+++++++ 7b: ");
		printResults(handler7b);
		System.out.println("-------------");		
		
		Assert.assertEquals(true, getLongRouteDemand(handler7b) == 1 && getShortRouteDemand(handler7b) == 9);
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

