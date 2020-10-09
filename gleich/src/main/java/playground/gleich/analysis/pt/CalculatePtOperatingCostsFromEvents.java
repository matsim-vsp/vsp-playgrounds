/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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
package playground.gleich.analysis.pt;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;

import java.io.File;
import java.net.MalformedURLException;
import java.util.*;
import java.util.stream.Collectors;

/** 
 * 
// * @param netFile
// * @param inScheduleFile: possible to use normal schedule file with all lines, not only minibus
// * @param inTransitVehicleFile: possible to use normal vehicle file with all lines, not only minibus
// * @param coordRefSystem
 */
public class CalculatePtOperatingCostsFromEvents {

	private final Network network;
	private final TransitSchedule inSchedule;
	private final Vehicles inTransitVehicles;
	private final String coordRefSystem;
	private final String minibusIdentifier;
	private final String attributeNameIsInShapeFile = "isInShapeFile";
	private final String attributeValueIsInShapeFile = "TRUE";


    private double hoursDriven = 0.0;

    private double kmDriven = 0.0;
    private int numVehUsed = 0;
    private double pkm = 0.0;
    private double totalCost = 0.0;
	private final static Logger log = Logger.getLogger(CalculatePtOperatingCostsFromEvents.class);



	public CalculatePtOperatingCostsFromEvents(String netFile, String inScheduleFile, String inTransitVehicleFile, String coordRefSystem, String minibusIdentifier) {
		this.coordRefSystem = coordRefSystem;
		this.minibusIdentifier = minibusIdentifier;

		// read files
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(netFile);
		config.transit().setTransitScheduleFile(inScheduleFile);
		config.transit().setVehiclesFile(inTransitVehicleFile);
		config.global().setCoordinateSystem(coordRefSystem); // coordinate reference system should be irrelevant, no need to make it configurable
		Scenario scenario = ScenarioUtils.loadScenario(config);
		this.network = scenario.getNetwork();
		this.inSchedule = scenario.getTransitSchedule();
		this.inTransitVehicles = scenario.getTransitVehicles();
	}

    public double getHoursDriven() {
        return hoursDriven;
    }

    public double getKmDriven() {
        return kmDriven;
    }

    public int getNumVehUsed() {
        return numVehUsed;
    }

    public double getPkm() {
        return pkm;
    }

    public double getTotalCost() {
        return totalCost;
    }

	/**
	 * Example for usage + convenience
	 * @param args
	 */
	public static void main(String[] args) {
		String networkFile = "/home/gregor/git/shared-svn/projects/avoev/matsim-input-files/vulkaneifel/v0/optimizedNetwork.xml.gz";

//		String inScheduleFile = "/home/gregor/git/shared-svn/projects/avoev/matsim-input-files/vulkaneifel/v0/optimizedSchedule.xml.gz";
//		String inTransitVehicleFile = "/home/gregor/git/shared-svn/projects/avoev/matsim-input-files/vulkaneifel/v0/optimizedVehicles.xml.gz";
// 		String eventsFile = "../runs-svn/avoev/snz-vulkaneifel/output-Vu-BC/Vu-BC.output_events.xml.gz";
//		String eventsFile = "../runs-svn/avoev/snz-vulkaneifel/output-Vu-DRT-1/Vu-DRT-1.output_events.xml.gz";
//		String eventsFile = "../runs-svn/avoev/snz-vulkaneifel/output-Vu-DRT-2/Vu-DRT-2.output_events.xml.gz";

		String inScheduleFile = "/home/gregor/git/shared-svn/projects/avoev/matsim-input-files/vulkaneifel/v1/optimizedSchedule_all-buses-split.xml.gz";
		String inTransitVehicleFile = "/home/gregor/git/shared-svn/projects/avoev/matsim-input-files/vulkaneifel/v1/optimizedVehicles_all-buses-split.xml.gz";
		String eventsFile = "../runs-svn/avoev/snz-vulkaneifel/output-Vu-DRT-3/Vu-DRT-3.output_events.xml.gz";

		String shapeFile = "../shared-svn/projects/avoev/matsim-input-files/vulkaneifel/v0/vulkaneifel.shp";

//		String networkFile = "/home/gregor/git/shared-svn/projects/avoev/matsim-input-files/gladbeck_umland/v0/optimizedNetwork.xml.gz";
//		String inScheduleFile = "/home/gregor/git/shared-svn/projects/avoev/matsim-input-files/gladbeck_umland/v1/optimizedSchedule_nonSB-bus-split-at-hubs.xml.gz";
//		String inTransitVehicleFile = "/home/gregor/git/shared-svn/projects/avoev/matsim-input-files/gladbeck_umland/v1/optimizedVehicles_nonSB-bus-split-at-hubs.xml.gz";
//		String inScheduleFile = "/home/gregor/git/shared-svn/projects/avoev/matsim-input-files/gladbeck_umland/v0/optimizedSchedule.xml.gz";
//		String inTransitVehicleFile = "/home/gregor/git/shared-svn/projects/avoev/matsim-input-files/gladbeck_umland/v0/optimizedVehicles.xml.gz";

		String coordRefSystem = "epsg:25832";
		String minibusIdentifier = "";

		double costPerHour = 1;
		double costPerKm = 1;
		double costPerDayFixVeh = 1;

//		String networkFile = "/home/gregor/git/runs-svn/capetown-minibuses/output-minibus-wo-transit/100pct/2018-12-03_100pct_1500it/output_network.xml.gz";
//		String inScheduleFile = "/home/gregor/git/runs-svn/capetown-minibuses/output-minibus-wo-transit/100pct/2018-12-03_100pct_1500it/ITERS/it.1500/1500.transitScheduleScored.xml.gz";
//		String inTransitVehicleFile = "/home/gregor/git/runs-svn/capetown-minibuses/output-minibus-wo-transit/100pct/2018-12-03_100pct_1500it/ITERS/it.1500/1500.transitVehicles.xml.gz";
		// input files with formal transit
//		String inScheduleFile = "/home/gregor/git/capetown/output-minibus-w-transit/2018-11-09/ITERS/it.100/100.transitSchedule.xml.gz";
//		String inTransitVehicleFile = "/home/gregor/git/capetown/output-minibus-w-transit/2018-11-09/ITERS/it.100/100.transitVehicles.xml.gz";
//		String networkFile = "/home/gregor/git/matsim/contribs/av/src/test/resources/intermodal_scenario/network.xml";
//		String inScheduleFile = "/home/gregor/git/matsim/contribs/av/src/test/resources/intermodal_scenario/transitschedule.xml";
//		String inTransitVehicleFile = "/home/gregor/git/matsim/contribs/av/src/test/resources/intermodal_scenario/transitVehicles.xml";

//		String coordRefSystem = "SA_Lo19";
//		String minibusIdentifier = "para_";

//		double costPerHour = 15;
//		double costPerKm = 1.75;
//		double costPerDayFixVeh = 700;

		// add vehicle types
		CalculatePtOperatingCostsFromEvents costCalculator = new CalculatePtOperatingCostsFromEvents(networkFile, inScheduleFile, inTransitVehicleFile, coordRefSystem, minibusIdentifier);
		costCalculator.run(eventsFile, shapeFile, costPerHour, costPerKm, costPerDayFixVeh);
	}

	public void run(String eventsFile, String shapeFile, double costPerHour, double costPerKm, double costPerDayFixVeh) {

		attributeNetwork(shapeFile);
		//DEBUG
		new NetworkWriter(network).write("attributedNetwork.xml.gz");

		EventsManager events = EventsUtils.createEventsManager();

		VehKmInShapeEventHandler eventHandler = new VehKmInShapeEventHandler(network,
				inSchedule, inTransitVehicles);
		events.addHandler(eventHandler);
		events.initProcessing();
		MatsimEventsReader eventsReader = new MatsimEventsReader(events);
		eventsReader.readFile(eventsFile);
		events.finishProcessing();

		// a vehicle could be used on multiple lines, so calculate according to that
		Map<Id<Vehicle>, Double> veh2timeInArea = eventHandler.getVeh2timeInArea();
		Map<Id<Vehicle>, Double> veh2distanceInArea = eventHandler.getVeh2distanceInArea();
		Map<Id<Vehicle>, Double> veh2paxDistanceInArea = eventHandler.getVeh2paxDistanceInArea();

		hoursDriven = veh2timeInArea.values().stream().collect(Collectors.summingDouble(Double::doubleValue)) / 3600; // s -> h
		kmDriven = veh2distanceInArea.values().stream().collect(Collectors.summingDouble(Double::doubleValue)) / 1000; // m -> km
		numVehUsed = veh2distanceInArea.size();

		pkm = veh2paxDistanceInArea.values().stream().collect(Collectors.summingDouble(Double::doubleValue)) / 1000; // m -> km

		totalCost = hoursDriven * costPerHour + kmDriven * costPerKm + numVehUsed * costPerDayFixVeh;
		System.out.println("hoursDriven: " + hoursDriven + " -> cost " + hoursDriven * costPerHour);
		System.out.println("kmDriven: " + kmDriven + " -> cost " + kmDriven * costPerKm + " ; km per veh per day: " + kmDriven / numVehUsed);
		System.out.println("numVehUsed: " + numVehUsed + " -> cost " + numVehUsed * costPerDayFixVeh);
		System.out.println("totalCost: " + totalCost);
		System.out.println("pkm: " + pkm);
	}
    private class VehKmInShapeEventHandler implements LinkEnterEventHandler, VehicleEntersTrafficEventHandler,
			VehicleLeavesTrafficEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler,
			TransitDriverStartsEventHandler {

        private final Network network;
        private final TransitSchedule inSchedule;
		private final Vehicles inTransiVehicles;

		private Map<Id<Vehicle>, Double> veh2enterServiceInAreaEventTime = new HashMap<>();
		private Map<Id<Vehicle>, Double> veh2timeInArea = new HashMap<>();
		private Map<Id<Vehicle>, Double> veh2distanceInArea = new HashMap<>();
		private Map<Id<Vehicle>, Double> veh2paxDistanceInArea = new HashMap<>();
		private Map<Id<Vehicle>, Integer> veh2currentPax = new HashMap<>();
		private Set<Id<Person>> transitDriverIds = new HashSet<>();

		public VehKmInShapeEventHandler(Network network, TransitSchedule inSchedule, Vehicles inTransitVehicles) {
			this.network = network;
			this.inSchedule = inSchedule;
			this.inTransiVehicles = inTransitVehicles;
		}

		// vehicle starts service at VehicleEntersTrafficEvent
		@Override
		public void handleEvent(VehicleEntersTrafficEvent vehicleEntersTrafficEvent) {
			if (inTransiVehicles.getVehicles().containsKey(vehicleEntersTrafficEvent.getVehicleId())) {
				// it's a transit vehicle, do something
				Object attributeIsInShape = network.getLinks().get(vehicleEntersTrafficEvent.getLinkId()).getAttributes().getAttribute(attributeNameIsInShapeFile);
				String attrValue = attributeIsInShape == null ? null : attributeIsInShape.toString();
				if (attributeValueIsInShapeFile.equals(attrValue)) {
					// vehicle starts service in area
					veh2enterServiceInAreaEventTime.put(vehicleEntersTrafficEvent.getVehicleId(), vehicleEntersTrafficEvent.getTime());
				}
			}
		}

		@Override
		public void handleEvent (LinkEnterEvent linkEnterEvent) {
			if (inTransiVehicles.getVehicles().containsKey(linkEnterEvent.getVehicleId())) {
				// it's a transit vehicle, do something
				Object attributeIsInShape = network.getLinks().get(linkEnterEvent.getLinkId()).getAttributes().getAttribute(attributeNameIsInShapeFile);
				String attrValue = attributeIsInShape == null ? null : attributeIsInShape.toString();
				if (attributeValueIsInShapeFile.equals(attrValue)) {
					// in the study area: add distance travelled on link
					veh2distanceInArea.put(linkEnterEvent.getVehicleId(),
							veh2distanceInArea.getOrDefault(linkEnterEvent.getVehicleId(), 0.0) +
									network.getLinks().get(linkEnterEvent.getLinkId()).getLength());
					veh2paxDistanceInArea.put(linkEnterEvent.getVehicleId(),
							veh2paxDistanceInArea.getOrDefault(linkEnterEvent.getVehicleId(), 0.0) +
									network.getLinks().get(linkEnterEvent.getLinkId()).getLength() * veh2currentPax.get(linkEnterEvent.getVehicleId()));
					if (! veh2enterServiceInAreaEventTime.containsKey(linkEnterEvent.getVehicleId())) {
						// vehicle was not monitored yet and entered area now
						veh2enterServiceInAreaEventTime.put(linkEnterEvent.getVehicleId(), linkEnterEvent.getTime());
					}
				} else {
					// either vehicle was already outside the area before (do nothing) or this is the first link outside the area
					if (veh2enterServiceInAreaEventTime.containsKey(linkEnterEvent.getVehicleId())) {
						// vehicle was in the area before: save time travelled in area and remove from monitored vehicles
						veh2timeInArea.put(linkEnterEvent.getVehicleId(),
								veh2timeInArea.getOrDefault(linkEnterEvent.getVehicleId(), 0.0) +
										linkEnterEvent.getTime() - veh2enterServiceInAreaEventTime.get(linkEnterEvent.getVehicleId()));
						veh2enterServiceInAreaEventTime.remove(linkEnterEvent.getVehicleId());
					}
				}
			}
		}

		// vehicle ends service at VehicleLeavesTrafficEvent
		@Override
		public void handleEvent(VehicleLeavesTrafficEvent vehicleLeavesTrafficEvent) {
			// either vehicle was already outside the area before (do nothing) or this is the first link outside the area
			if (veh2enterServiceInAreaEventTime.containsKey(vehicleLeavesTrafficEvent.getVehicleId())) {
				// vehicle was in the area before: save time travelled in area and remove from monitored vehicles
				veh2timeInArea.put(vehicleLeavesTrafficEvent.getVehicleId(),
						veh2timeInArea.getOrDefault(vehicleLeavesTrafficEvent.getVehicleId(), 0.0) +
								vehicleLeavesTrafficEvent.getTime() - veh2enterServiceInAreaEventTime.get(vehicleLeavesTrafficEvent.getVehicleId()));
				veh2enterServiceInAreaEventTime.remove(vehicleLeavesTrafficEvent.getVehicleId());
			}
		}

		// collect transit driver ids
		@Override
		public void handleEvent(TransitDriverStartsEvent transitDriverStartsEvent) {
			transitDriverIds.add(transitDriverStartsEvent.getDriverId());
			veh2currentPax.put(transitDriverStartsEvent.getVehicleId(), 0);
		}

		// count all
		@Override
		public void handleEvent(PersonEntersVehicleEvent personEntersVehicleEvent) {
			if (inTransiVehicles.getVehicles().containsKey(personEntersVehicleEvent.getVehicleId()) &&
					! transitDriverIds.contains(personEntersVehicleEvent.getPersonId())) {
				veh2currentPax.put(personEntersVehicleEvent.getVehicleId(),
						veh2currentPax.get(personEntersVehicleEvent.getVehicleId()) + 1);
			}
		}

		@Override
		public void handleEvent(PersonLeavesVehicleEvent personLeavesVehicleEvent) {
			if (veh2currentPax.containsKey(personLeavesVehicleEvent.getVehicleId()) &&
					! transitDriverIds.contains(personLeavesVehicleEvent.getPersonId())) {
				veh2currentPax.put(personLeavesVehicleEvent.getVehicleId(),
						veh2currentPax.get(personLeavesVehicleEvent.getVehicleId()) - 1);
			}
		}

		private Map<Id<Vehicle>, Double> getVeh2timeInArea() {
			return veh2timeInArea;
		}

		private Map<Id<Vehicle>, Double> getVeh2distanceInArea() {
			return veh2distanceInArea;
		}

		private Map<Id<Vehicle>, Double> getVeh2paxDistanceInArea() {
			return veh2paxDistanceInArea;
		}

	}

	private void attributeNetwork(String shapeFile) {
		List<PreparedGeometry> preparedGeometries;
		try {
			preparedGeometries = ShpGeometryUtils.loadPreparedGeometries(new File(shapeFile).toURI().toURL());
		} catch (MalformedURLException e) {
			throw new RuntimeException(e.getMessage());
		}

		network.getLinks().values().stream().forEach(link -> link.getAttributes().putAttribute(attributeNameIsInShapeFile,
				ShpGeometryUtils.isCoordInPreparedGeometries(link.getFromNode().getCoord(), preparedGeometries) ?
						attributeValueIsInShapeFile : "FALSE"));
//		network.getLinks().values().stream().forEach(link -> link.getAttributes().putAttribute(attributeNameIsInShapeFile,
//				attributeValueIsInShapeFile));

	}

	// tested with matsim/contribs/av/src/test/resources/intermodal_scenario
//	hoursDriven: 26.666666666666664 -> cost 399.99999999999994
//	kmDriven: 1000.0 -> cost 2000.0 ; km per veh per day: 250.0
//	numVehUsed: 4 -> cost 2400.0
//	totalCost: 4800.0

}
