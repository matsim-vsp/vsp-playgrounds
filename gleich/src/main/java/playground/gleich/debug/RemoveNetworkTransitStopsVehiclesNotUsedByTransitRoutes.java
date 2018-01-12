/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * RemoveNetworkAndTransitStopsNotUsedByTransitRoutes.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2018 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package playground.gleich.debug;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;

public class RemoveNetworkTransitStopsVehiclesNotUsedByTransitRoutes {

	Scenario inputScenario;
	Network outputNetwork;
	TransitSchedule outputSchedule;
	Vehicles outputVehicles;
	Set<TransitStopFacility> usedTransitStops = new HashSet<>();

	public RemoveNetworkTransitStopsVehiclesNotUsedByTransitRoutes(Scenario inputScenario) {
		this.inputScenario = inputScenario;
		System.out.println(inputScenario.getConfig().getContext().getFile().substring(0,
				inputScenario.getConfig().getContext().getFile().lastIndexOf("/"))
				+ "/output_reduced_transitSchedule.xml");
		Scenario creatorScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		outputNetwork = creatorScenario.getNetwork();
		outputSchedule = creatorScenario.getTransitSchedule();
		outputVehicles = creatorScenario.getTransitVehicles();
	}

	public static void main(String[] args) {
		RemoveNetworkTransitStopsVehiclesNotUsedByTransitRoutes obj;
		if (args.length == 1) {
			// Use given String as path to Config file
			Config config = ConfigUtils.loadConfig(args[0]);
			Scenario scenario = ScenarioUtils.loadScenario(config);
			obj = new RemoveNetworkTransitStopsVehiclesNotUsedByTransitRoutes(scenario);
			obj.run();
		} else {
			System.err.println("Arg 1: config.xml is missing.");
			System.exit(1);
		}

	}

	private void run() {
		findAndCopyUsedTransitStops();
		copyTransitLines();
		
		findAndCopyUsedTransitVehicles();

		findLinksWithUsedTransitStopsAndBuildMinimalNetwork();
		buildAndSetNetworkRoutesOnMinimalNetwork();

		TransitScheduleWriter scheduleWriter = new TransitScheduleWriter(outputSchedule);
		scheduleWriter.writeFile(inputScenario.getConfig().getContext().getFile().substring(0,
				inputScenario.getConfig().getContext().getFile().lastIndexOf("/"))
				+ "/output_reduced_transitSchedule.xml.gz");
		NetworkWriter networkWriter = new NetworkWriter(outputNetwork);
		networkWriter.write(inputScenario.getConfig().getContext().getFile().substring(0,
				inputScenario.getConfig().getContext().getFile().lastIndexOf("/")) + "/output_reduced_network.xml.gz");
		VehicleWriterV1 vehicleWriter = new VehicleWriterV1(outputVehicles);
		vehicleWriter.writeFile(inputScenario.getConfig().getContext().getFile().substring(0,
				inputScenario.getConfig().getContext().getFile().lastIndexOf("/")) + "/output_reduced_transitVehicles.xml.gz");
	}

	private void findAndCopyUsedTransitStops() {
		usedTransitStops = new HashSet<>();
		// find all used TransitStops
		for (TransitLine line : inputScenario.getTransitSchedule().getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (TransitRouteStop stop : route.getStops()) {
					usedTransitStops.add(stop.getStopFacility());
				}
			}
		}

		// copy into outputSchedule
		for (TransitStopFacility stop : usedTransitStops) {
			outputSchedule.addStopFacility(stop);
		}
	}

	private void copyTransitLines() {
		for (TransitLine line : inputScenario.getTransitSchedule().getTransitLines().values()) {
			outputSchedule.addTransitLine(line);
		}
	}
	
	private void findAndCopyUsedTransitVehicles() {
		for (TransitLine line : outputSchedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (Departure dep : route.getDepartures().values()) {
					if (!outputVehicles.getVehicles().containsKey(dep.getVehicleId())) {
						Vehicle veh = inputScenario.getTransitVehicles().getVehicles().get(dep.getVehicleId());
						VehicleType vehType = inputScenario.getTransitVehicles().getVehicleTypes().get(veh.getType().getId());
						
						if (!outputVehicles.getVehicleTypes().containsKey(vehType.getId())) {
							outputVehicles.addVehicleType(vehType);
						}
						outputVehicles.addVehicle(veh);
					}
				}
			}
		}
	}

	private void findLinksWithUsedTransitStopsAndBuildMinimalNetwork() {
		String centralDummyNodeName = "dummy_node";
		Node centralDummyNode = NetworkUtils.createAndAddNode(outputNetwork, Id.createNodeId(centralDummyNodeName),
				CoordUtils.createCoord(0, 0));

		for (TransitStopFacility stop : usedTransitStops) {
			Link stopLink = inputScenario.getNetwork().getLinks().get(stop.getLinkId());

			if (!outputNetwork.getLinks().containsKey(stopLink.getId())) {

				Node fromNodeOfStopLink = stopLink.getFromNode();
				if (!outputNetwork.getNodes().containsKey(fromNodeOfStopLink.getId())) {
					NetworkUtils.createAndAddNode(outputNetwork, fromNodeOfStopLink.getId(),
							fromNodeOfStopLink.getCoord());
				}

				Node toNodeOfStopLink = stopLink.getToNode();
				if (!outputNetwork.getNodes().containsKey(toNodeOfStopLink.getId())) {
					NetworkUtils.createAndAddNode(outputNetwork, toNodeOfStopLink.getId(),
							toNodeOfStopLink.getCoord());
				}

				outputNetwork.addLink(stopLink);

				/*
				 * add dummy to and from links to/from dummy_node in order to have a valid
				 * network
				 */
				NetworkUtils.createAndAddLink(outputNetwork,
						Id.createLinkId("dummy_link_from_" + stop.getLinkId().toString()),
						inputScenario.getNetwork().getLinks().get(stop.getLinkId()).getToNode(), centralDummyNode, 1.,
						1., 2000., 1.);
				NetworkUtils.createAndAddLink(outputNetwork,
						Id.createLinkId("dummy_link_to_" + stop.getLinkId().toString()), centralDummyNode,
						inputScenario.getNetwork().getLinks().get(stop.getLinkId()).getFromNode(), 1., 1., 2000., 1.);
			}
		}
	}

	private void buildAndSetNetworkRoutesOnMinimalNetwork() {
		LinkNetworkRouteFactory factory = new LinkNetworkRouteFactory();
		for (TransitLine line : outputSchedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				List<Id<Link>> networkLinks = new ArrayList<>();
				for (TransitRouteStop stop : route.getStops()) {
					networkLinks.add(Id.createLinkId("dummy_link_to_" + stop.getStopFacility().getLinkId()));
					networkLinks.add(stop.getStopFacility().getLinkId());
					networkLinks.add(Id.createLinkId("dummy_link_from_" + stop.getStopFacility().getLinkId()));
				}
				NetworkRoute newNetworkRoute = (NetworkRoute) factory.createRoute(networkLinks.get(0),
						networkLinks.get(networkLinks.size() - 1));
				newNetworkRoute.setLinkIds(networkLinks.get(0), networkLinks.subList(1, networkLinks.size() - 2),
						networkLinks.get(networkLinks.size() - 1));

				route.setRoute(newNetworkRoute);
			}
		}
	}
}
