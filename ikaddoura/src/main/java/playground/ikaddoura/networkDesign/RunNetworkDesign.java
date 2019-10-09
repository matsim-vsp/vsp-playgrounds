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

package playground.ikaddoura.networkDesign;

import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.run.RunBerlinScenario;

/**
* @author ikaddoura
* 
*/
public class RunNetworkDesign {

	private static final String configFile = "/Users/ihab/Documents/workspace/runs-svn/networkDesign/input/berlin-v5.2-1pct-networkDesign.config.xml";
	private static final Logger log = Logger.getLogger(RunNetworkDesign.class);
	private static final boolean useGridNetwork = true;
	
	public static void main(String[] args) {
		
		RunBerlinScenario berlin = new RunBerlinScenario(configFile, null);
		
		final Config config = berlin.prepareConfig();
		if (useGridNetwork) config.network().setInputFile("initial.grid-network.xml.gz");

		config.controler().setOutputDirectory("/Users/ihab/Documents/workspace/runs-svn/networkDesign/output/run5/");
		config.controler().setLastIteration(50);
		
		final Scenario scenario = berlin.prepareScenario();
		
		// adjust population
		for (Person person : scenario.getPopulation().getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			StageActivityTypes stageActivities = new StageActivityTypesImpl("pt interaction", "car interaction", "freight interaction", "ride interaction");
			List<Trip> trips = TripStructureUtils.getTrips(person.getSelectedPlan(), stageActivities);
			
			plan.getPlanElements().clear();
			
			MainModeIdentifierImpl mainModeIdentifierImpl = new MainModeIdentifierImpl();
			
			if (trips.size() > 0) {
				plan.addActivity(trips.get(0).getOriginActivity());
				for (Trip trip : trips) {
					
					String mainMode = mainModeIdentifierImpl.identifyMainMode(trip.getTripElements());
					plan.addLeg(scenario.getPopulation().getFactory().createLeg(mainMode));
					
					plan.addActivity(trip.getDestinationActivity());
				}
			} else {
				// plan without any trips
			}
			
			for (PlanElement pE : plan.getPlanElements()) {
				if (pE instanceof Activity) {
					Activity act = (Activity) pE;
					if (act.getCoord() == null) act.setCoord(scenario.getNetwork().getLinks().get(act.getLinkId()).getCoord());
					act.setLinkId(null);
				}
			}
		}
		
		new PopulationWriter(scenario.getPopulation()).write("/Users/ihab/Desktop/population-adjusted.xml.gz");
		
		if (!useGridNetwork) {
			// adjust network
			for (Link link : scenario.getNetwork().getLinks().values()) {
				link.setCapacity(1000.);
				link.setNumberOfLanes(1);
				link.setFreespeed(50/3.6);
			}
		}
				
		final Controler controler = berlin.prepareControler();
		controler.addOverridingModule( new AbstractModule() {
			
			@Override public void install() {
				this.bind(NetworkDesign.class).asEagerSingleton();
				this.addControlerListenerBinding().to(NetworkDesign.class);
				this.addEventHandlerBinding().to(NetworkDesign.class);
			}

		}) ;
				
		berlin.run();
		
		log.info("Run completed.");
		
	}	
}

