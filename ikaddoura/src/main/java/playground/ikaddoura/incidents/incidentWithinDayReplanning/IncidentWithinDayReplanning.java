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

package playground.ikaddoura.incidents.incidentWithinDayReplanning;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.withinday.trafficmonitoring.TravelTimeCollector;

import playground.ikaddoura.incidents.NetworkChangeEventsUtils;

/**
* @author ikaddoura
* 
*/
public class IncidentWithinDayReplanning {

// ############################################################################################################################################

//	private static String day = "2016-02-11";
	private static String day = "2016-03-15";
	
	private static String populationFile = "/Users/ihab/Documents/workspace/runs-svn/incidents/berlin/input/be_251.output_plans_selected.xml.gz";
	private static String networkFile = "/Users/ihab/Documents/workspace/runs-svn/incidents/berlin/input/be_251.output_network.xml.gz";
	private static String configFile = "/Users/ihab/Documents/workspace/runs-svn/incidents/berlin/input/config.xml";
	private static String networkChangeEventsFile = "/Users/ihab/Documents/workspace/runs-svn/incidents/berlin/input/incidentData_berlin_" + day + "/networkChangeEvents_" + day + ".xml.gz";
	private static String runOutputBaseDirectory = "/Users/ihab/Documents/workspace/runs-svn/incidents/berlin/output/output_";
		
	private static final boolean reducePopulationToAffectedAgents = false;
	private static final String reducedPopulationFile = "path-to-reduced-population.xml.gz";
	
	private static final boolean applyNetworkChangeEvents = false;
	private static final boolean applyWithinDayReplanning = false;
		
// ############################################################################################################################################
	
	private static final Logger log = Logger.getLogger(IncidentWithinDayReplanning.class);
	
	public static void main(String[] args) {
		IncidentWithinDayReplanning incidentWithinDayReplanning = new IncidentWithinDayReplanning();
		incidentWithinDayReplanning.run();
	}

	private void run() {
		
//		OutputDirectoryLogging.catchLogEntries();
//		try {
//			OutputDirectoryLogging.initLoggingWithOutputDirectory(runOutputBaseDirectory);
//		} catch (IOException e1) {
//			e1.printStackTrace();
//		}
		
		final Config config = ConfigUtils.loadConfig(configFile);
		config.network().setInputFile(networkFile);
		config.plans().setRemovingUnneccessaryPlanAttributes(true);
		config.plans().setInputFile(populationFile);

		config.controler().setOutputDirectory(runOutputBaseDirectory + day + "_networkChangeEvents-" + applyNetworkChangeEvents + "_withinDayReplanning-" + applyWithinDayReplanning + "/");
		
		if (applyNetworkChangeEvents) {
			config.network().setChangeEventsInputFile(networkChangeEventsFile);
			config.network().setTimeVariantNetwork(true);			
		} else {
			log.info("Not considering any network change events.");
		}
		
		final Scenario scenario = ScenarioUtils.loadScenario(config);

		final Controler controler = new Controler(scenario);
		controler.getConfig().controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		
		if (reducePopulationToAffectedAgents) {
			log.warn("Reduced population should only be used for testing purposes.");
			
			log.info("Reducing the population size from " + scenario.getPopulation().getPersons().size() + "...");

			Set<Id<Link>> incidentLinkIds = NetworkChangeEventsUtils.getIncidentLinksFromNetworkChangeEventsFile(scenario);
			Set<Id<Person>> personIdsToKeepInPopulation = NetworkChangeEventsUtils.getPersonIDsOfAgentsDrivingAlongSpecificLinks(scenario, incidentLinkIds);
			NetworkChangeEventsUtils.filterPopulation(scenario, personIdsToKeepInPopulation);

			log.info("... to " + scenario.getPopulation().getPersons().size() + " agents (= those agents driving along incident links).");
			PopulationWriter writer = new PopulationWriter(scenario.getPopulation());
			writer.write(reducedPopulationFile);
		} else {
			log.info("Using the normal population.");
		}
		
		if (applyWithinDayReplanning) {
			
			Set<String> analyzedModes = new HashSet<>();
			analyzedModes.add(TransportMode.car);
			final TravelTimeCollector travelTime = new TravelTimeCollector(controler.getScenario(), analyzedModes);
							
			controler.addOverridingModule( new AbstractModule() {
				@Override public void install() {
										
					this.bind(IncidentBestRouteMobsimListener.class).asEagerSingleton();
					this.addMobsimListenerBinding().to(IncidentBestRouteMobsimListener.class);
					this.addControlerListenerBinding().to(IncidentBestRouteMobsimListener.class);

					this.bind(TravelTime.class).toInstance(travelTime);
					this.addEventHandlerBinding().toInstance(travelTime);
					this.addMobsimListenerBinding().toInstance(travelTime);
				}
			}) ;
		} else {
			if (applyNetworkChangeEvents) {
				log.warn("Applying network change events without within-day replanning.");
			}
		}
				
		controler.run();		
	}
	
}

