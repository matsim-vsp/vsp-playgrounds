/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.ikaddoura.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.ControlerDefaultsModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.controler.NewControlerModule;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.corelisteners.ControlerDefaultCoreListenersModule;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;

import com.google.inject.Module;

/**
* @author ikaddoura
*/

public class MapWalkAndBicycleDemandToNetwork {

	public static void main(String[] args) throws IOException {
		String outputFile = "/Users/ihab/Desktop/walk-and-bicycle-demand-10pct.txt";
		int scaleFactor = 10;
		
		Map<Integer, Map<Id<Link>, Integer>> timeBin2linkId2demand = new HashMap<>();

		Config config = ConfigUtils.createConfig();
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.global().setCoordinateSystem("GK4");
		config.network().setInputFile("/Users/ihab/Documents/workspace/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-10pct/output-berlin-v5.4-10pct/berlin-v5.4-10pct.output_network.xml.gz");
		config.plans().setInputFile("/Users/ihab/Documents/workspace/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-10pct/output-berlin-v5.4-10pct/berlin-v5.4-10pct.output_plans.xml.gz");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		for (Link link : scenario.getNetwork().getLinks().values()) {
			if (link.getFreespeed() > 13.8) {
				// exclude motorway road segments...
				link.setFreespeed(0.0000001);
			}
			if (link.getAllowedModes().contains("pt")) {
				// exclude pt links...
				link.setFreespeed(0.0000001);
			}
		}
		Module module = new org.matsim.core.controler.AbstractModule() {
			
			@Override
			public void install() {
				install ( new NewControlerModule() );
				install ( new ControlerDefaultCoreListenersModule() );
				install ( new ControlerDefaultsModule() );
				install ( new ScenarioByInstanceModule(scenario) );
			}
		};
		com.google.inject.Injector injector = Injector.createInjector(config, module);
		TripRouter tripRouter = injector.getInstance(TripRouter.class);
		
		// use the car network
		String mainMode = TransportMode.car;
		
		for (Person person : scenario.getPopulation().getPersons().values()) {
			for (Trip trip : TripStructureUtils.getTrips(person.getSelectedPlan())) {
				if (trip.getLegsOnly().size() == 1) {
					// only consider trips with a single leg
					if (trip.getLegsOnly().get(0).getMode().equals("walk") || trip.getLegsOnly().get(0).getMode().equals("bicycle")) {
						// only consider walk or bicycle trips
						if (trip.getOriginActivity().getEndTime().seconds() >= 0. && trip.getOriginActivity().getEndTime().seconds() < 24 * 3600.) {
							
							int timeBin = (int) (trip.getOriginActivity().getEndTime().seconds() / 3600.);
							
							Facility fromFacility = FacilitiesUtils.wrapLink(scenario.getNetwork().getLinks().get(trip.getOriginActivity().getLinkId()));
							Facility toFacility = FacilitiesUtils.wrapLink(scenario.getNetwork().getLinks().get(trip.getDestinationActivity().getLinkId()));
							
							double departureTime = trip.getOriginActivity().getEndTime().seconds();
							
							List<? extends PlanElement> result = tripRouter.calcRoute(mainMode, fromFacility, toFacility, departureTime, person, null);
							
							for (PlanElement pE : result) {
								if (pE instanceof Leg) {
									Leg leg = (Leg) pE;
									NetworkRoute route = (NetworkRoute) leg.getRoute();
									for (Id<Link> linkId : route.getLinkIds()) {
										timeBin2linkId2demand.putIfAbsent(timeBin, new HashMap<>());
										timeBin2linkId2demand.get(timeBin).merge(linkId, 1, (v1, v2) -> v1 + v2);
									}
								}
							}
						}
					}
				}
			}
		}
		
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFile);
		writer.write("link Id");
		for (int i = 3; i < 23; i++) {
			writer.write("; demand_" + i);
		}
		writer.newLine();
		
		for (Id<Link> linkId : scenario.getNetwork().getLinks().keySet()) {
			writer.write(linkId.toString());
			for (int i = 3; i < 23; i++) {
				int demand = 0;
				if (timeBin2linkId2demand.get(i) != null && timeBin2linkId2demand.get(i).get(linkId) != null) {
					demand = timeBin2linkId2demand.get(i).get(linkId);
				}
				writer.write(";" + scaleFactor * demand);
			}
			writer.newLine();
		}
		writer.close();
	}

}

