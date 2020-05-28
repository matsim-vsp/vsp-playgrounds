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

package playground.gleich.analysis.drt;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.fleet.FleetReader;
import org.matsim.contrib.dvrp.fleet.FleetSpecificationImpl;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;

/**
 * @author gleich
 */
public class DrtVehiclePositionWriter
		implements PassengerRequestScheduledEventHandler, LinkEnterEventHandler, PersonEntersVehicleEventHandler,
		PersonLeavesVehicleEventHandler {

	private static String NetworkFile = "/Users/gleich/Documents/EclipseWorkspace/av-bus_berlinNW/data/output/Berlin100pct/D2D/D2D_250_Cap4/output_network.xml.gz";
	private static String EventsFile = "/Users/gleich/Documents/EclipseWorkspace/av-bus_berlinNW/data/output/Berlin100pct/D2D/old_D2D_400_Cap4/output_events.xml.gz";
	private static String drtVehicleFile = "/Users/gleich/Documents/EclipseWorkspace/av-bus_berlinNW/data/input/Berlin100pct/drt/DrtVehicles.100pct.DRT_400_Cap4.xml";
	private static String outputDirectoryString = "/Users/gleich/Documents/EclipseWorkspace/av-bus_berlinNW/data/output/Berlin100pct/D2D/old_D2D_400_Cap4/drtVehiclePositions";

	private Network network;

	/**
	 * current implementation counts scheduled requests and substracts alighted passengers
	 * -> won't work if one scheduled request contains multiple passengers, which currently is not possible
	 * <p>
	 * scheduleCounter is broken, delivers negative counts
	 */
	private Map<Id<Vehicle>, Integer> drtVeh2scheduleCounter = new HashMap<>();
	private Map<Id<Vehicle>, Integer> drtVeh2CurrentNumPassengers = new HashMap<>();
	private Map<Id<Vehicle>, Id<Link>> drtVeh2CurrentLink = new HashMap<>();

	private String outputDirectory;
	private double nextOutputTime;
	private final double outputInterval;
	private String sep = ";";

	public static void main(String[] args) throws MalformedURLException {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(NetworkFile);
		EventsManager events = EventsUtils.createEventsManager();
		DrtVehiclePositionWriter eventHandler = new DrtVehiclePositionWriter(scenario.getNetwork(),
				new URL("file://" + drtVehicleFile), outputDirectoryString, 15 * 60);
		events.addHandler(eventHandler);
		new DrtEventsReader(events).readFile(EventsFile);
	}

	/**
	 * @param network
	 */
	public DrtVehiclePositionWriter(Network network, URL drtVehiclesFileUrl, String outputDirectory,
			double outputInterval) {
		this.network = network;
		this.outputDirectory = outputDirectory;
		this.outputInterval = outputInterval;
		this.nextOutputTime = outputInterval;
		initializeFromVehiclesFile(drtVehiclesFileUrl);
	}

	private void initializeFromVehiclesFile(URL drtVehiclesFileUrl) {
		FleetSpecificationImpl fleet = new FleetSpecificationImpl();
		new FleetReader(fleet).parse(drtVehiclesFileUrl);
		for (DvrpVehicleSpecification s : fleet.getVehicleSpecifications().values()) {
			drtVeh2CurrentLink.put(Id.createVehicleId(s.getId()), s.getStartLinkId());
			drtVeh2CurrentNumPassengers.put(Id.createVehicleId(s.getId()), 0);
			drtVeh2scheduleCounter.put(Id.createVehicleId(s.getId()), 0);
		}
	}

	private void calcNextOutputTime() {
		nextOutputTime += outputInterval;
	}

	private void outputWriterTrigger(double eventTime) {
		if (eventTime >= nextOutputTime) {
			writeOutput(nextOutputTime);
			calcNextOutputTime();
		}
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}

	// fetch vehicle movements
	@Override
	public void handleEvent(LinkEnterEvent event) {
		outputWriterTrigger(event.getTime());
		if (drtVeh2CurrentLink.containsKey(event.getVehicleId())) {
			drtVeh2CurrentLink.put(event.getVehicleId(), event.getLinkId());
		}
	}

	// detect boarding passengers
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		outputWriterTrigger(event.getTime());
		if (drtVeh2CurrentNumPassengers.containsKey(event.getVehicleId())) {
			drtVeh2CurrentNumPassengers.put(event.getVehicleId(),
					drtVeh2CurrentNumPassengers.get(event.getVehicleId()) + 1);
		}
	}

	// detect alighting passengers (and end of scheduled request)
	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		outputWriterTrigger(event.getTime());
		if (drtVeh2CurrentNumPassengers.containsKey(event.getVehicleId())) {
			drtVeh2CurrentNumPassengers.put(event.getVehicleId(),
					drtVeh2CurrentNumPassengers.get(event.getVehicleId()) - 1);
			drtVeh2scheduleCounter.put(event.getVehicleId(), drtVeh2scheduleCounter.get(event.getVehicleId()) - 1);
		}
	}

	// detect new scheduled requests
	@Override
	public void handleEvent(PassengerRequestScheduledEvent event) {
		outputWriterTrigger(event.getTime());
		drtVeh2scheduleCounter.put(Id.createVehicleId(event.getVehicleId()),
				drtVeh2scheduleCounter.get(event.getVehicleId()) + 1);
	}

	private void writeOutput(double outputTime) {
		BufferedWriter bw;
		try {
			bw = IOUtils.getBufferedWriter(outputDirectory + "/drtVehPositions_" + (int)outputTime + ".csv");
			// write header
			bw.write("vehId" + sep + "linkId" + sep + "x" + sep + "y" + sep + "passengers" + sep + "scheduledRequests");
			bw.newLine();

			for (Id<Vehicle> id : drtVeh2CurrentLink.keySet()) {
				Coord linkMidPoint = network.getLinks().get(drtVeh2CurrentLink.get(id)).getCoord();
				bw.write(id.toString()
						+ sep
						+ drtVeh2CurrentLink.get(id).toString()
						+ sep
						+ linkMidPoint.getX()
						+ sep
						+ linkMidPoint.getY()
						+ sep
						+ drtVeh2CurrentNumPassengers.get(id)
						+ sep
						+ drtVeh2scheduleCounter.get(id));
				bw.newLine();
			}

			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("could not write");
		}
	}
}
