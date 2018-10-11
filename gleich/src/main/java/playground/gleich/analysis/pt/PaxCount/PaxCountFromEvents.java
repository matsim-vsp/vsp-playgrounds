/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package playground.gleich.analysis.pt.PaxCount;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.analysis.TransitLoad;
import org.matsim.pt.analysis.TransitLoad.StopInformation;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * 
 * @author gleich
 *
 */
public class PaxCountFromEvents {
	
	private final static Logger log = Logger.getLogger(PaxCountFromEvents.class);
	
	private final String eventsFile;
	private final Scenario scenario;
	private Map<Id<TransitLine>, Map<Id<TransitRoute>, Map<Id<Departure>, Map<Integer, StopInformationEnhanced>>>> line2route2dep2stop2info;
	
	PaxCountFromEvents (String networkFile, String scheduleFile, String eventsFile) {
		this.eventsFile = eventsFile;
        scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);
        new TransitScheduleReader(scenario).readFile(scheduleFile);
	}
	
	public static void main (String[] args) {
		if (args.length != 4) {
			throw new RuntimeException("Wrong number of args to main method. " + ""
					+ "Should be path to network file, transitSchedule file, events file, "+
					"and path to analysis output file.");
		}
		PaxCountFromEvents paxCounter = new PaxCountFromEvents(args[0], args[1], args[2]);
		paxCounter.run();
		paxCounter.writeLine2route2dep2stop2info(args[3], ";");
	}

	private void run() {
		TransitLoad transitLoad = readEvents();
		line2route2dep2stop2info = countInVehPaxAndSaveStopInfo(transitLoad);
	}
	
	private TransitLoad readEvents() {
		log.info("reading events");
		EventsManager events = EventsUtils.createEventsManager();
		TransitLoad transitLoad = new TransitLoad();
		events.addHandler(transitLoad);

		new EventsReaderXMLv1(events).readFile(eventsFile);
		return transitLoad;
	}
	
	private Map<Id<TransitLine>, Map<Id<TransitRoute>, Map<Id<Departure>, Map<Integer, StopInformationEnhanced>>>> countInVehPaxAndSaveStopInfo(
			TransitLoad transitLoad) {
		log.info("counting passengers in vehicle");

		/* use stopNr in route.getStops() instead of TransitStopFacility, because the
		 * same TransitStopFacility can be served multiple times
		 */
		Map<Id<TransitLine>, Map<Id<TransitRoute>, Map<Id<Departure>, Map<Integer, StopInformationEnhanced>>>> line2route2dep2stop2info = new HashMap<>();
		for (TransitLine line : this.scenario.getTransitSchedule().getTransitLines().values()) {
			Map<Id<TransitRoute>, Map<Id<Departure>, Map<Integer, StopInformationEnhanced>>> route2dep2stop2info = new HashMap<>();
			line2route2dep2stop2info.put(line.getId(), route2dep2stop2info);

			for (TransitRoute route : line.getRoutes().values()) {
				Map<Id<Departure>, Map<Integer, StopInformationEnhanced>> dep2stop2info = new HashMap<>();
				route2dep2stop2info.put(route.getId(), dep2stop2info);

				for (Departure dep : route.getDepartures().values()) {
					Map<Integer, StopInformationEnhanced> stop2info = new HashMap<>();
					dep2stop2info.put(dep.getId(), stop2info);
					int nOfPassengers = 0;

					/*
					 * count how often a stop was visited while following the route in
					 * route.getStops() to differentiate multiple servings of the same
					 * TransitStopFacility. Count from 0, so count equals index in list
					 */
					Map<Id<TransitStopFacility>, Integer> stop2nrVisits = new HashMap<>();
					for (int i = 0; i < route.getStops().size(); i++) {
						TransitRouteStop stop = route.getStops().get(i);
						Integer nrVisits = stop2nrVisits.get(stop.getStopFacility().getId());
						if (nrVisits == null) {
							nrVisits = 0;
						} else {
							nrVisits++;
						}
						stop2nrVisits.put(stop.getStopFacility().getId(), nrVisits);

						List<StopInformation> siList = transitLoad.getDepartureStopInformation(line, route,
								stop.getStopFacility(), dep);
						if (siList != null) {
							StopInformation si = siList.get(nrVisits);
							if (si != null) {
								nOfPassengers -= si.nOfLeaving;
								nOfPassengers += si.nOfEntering;
							} else {
								log.warn("no StopInformation for line: " + line.getId().toString() + ", route: "
										+ route.getId().toString() + ", at stop: "
										+ stop.getStopFacility().getId().toString() + ", departure: "
										+ dep.getId().toString()
										+ ". This can happen if a transit vehicle has not completed its transit route, because it stuck or similar.");
							}
							stop2info.put(i, new StopInformationEnhanced(si.nOfEntering, si.nOfLeaving, nOfPassengers,
									si.arrivalTime, si.departureTime));
						}
					}
				}
			}
		}
		return line2route2dep2stop2info;
	}
	
	private void writeLine2route2dep2stop2info(String filename, String sep) {
		log.info("start writing output file");

		BufferedWriter bw = IOUtils.getBufferedWriter(filename);

		try {
			// write header
			bw.write("lineId" + sep + "routeId" + sep + "depId" + sep + "stopNr" + sep + "stopFacilityId" + sep
					+ "arrivalTimePlanned" + sep + "departureTimePlanned" + sep + "arrivalTimeReal" + sep
					+ "departureTimeReal" + sep + "nPaxEntering" + sep + "nPaxLeaving" + sep + "nPaxInVehicle" + sep
					+ "transportMode");
			bw.newLine();

			for (TransitLine line : this.scenario.getTransitSchedule().getTransitLines().values()) {

				for (TransitRoute route : line.getRoutes().values()) {

					for (Departure dep : route.getDepartures().values()) {

						for (int i = 0; i < route.getStops().size(); i++) {
							TransitRouteStop stop = route.getStops().get(i);
							StopInformationEnhanced stopInfo = line2route2dep2stop2info.get(line.getId())
									.get(route.getId()).get(dep.getId()).get(i);
							// write data
							bw.write(line.getId().toString() + sep + route.getId().toString() + sep
									+ dep.getId().toString() + sep + i + sep + stop.getStopFacility().getId().toString()
									+ sep + Time.writeTime(dep.getDepartureTime() + stop.getArrivalOffset()) + sep
									+ Time.writeTime(dep.getDepartureTime() + stop.getDepartureOffset()) + sep
									+ Time.writeTime(stopInfo.arrivalTime) + sep
									+ Time.writeTime(stopInfo.departureTime) + sep + stopInfo.nPaxEntering + sep
									+ stopInfo.nPaxLeaving + sep + stopInfo.nPaxInVehicle + sep
									+ route.getTransportMode());
							bw.newLine();
						}
					}
				}
			}

			bw.close();
			log.info("finished writing output file");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static class StopInformationEnhanced {
		public final int nPaxEntering;
		public final int nPaxLeaving;
		public final int nPaxInVehicle;
		public final double arrivalTime;
		public final double departureTime;
		
		StopInformationEnhanced(int nPaxEntering, int nPaxLeaving, int nPaxInVehicle, double arrivalTime, double departureTime) {
			this.nPaxEntering = nPaxEntering;
			this.nPaxLeaving = nPaxLeaving;
			this.nPaxInVehicle = nPaxInVehicle;
			this.arrivalTime = arrivalTime;
			this.departureTime = departureTime;
		}
	}
	
	public Map<Id<TransitLine>, Map<Id<TransitRoute>, Map<Id<Departure>, Map<Integer, StopInformationEnhanced>>>> getLine2route2dep2stop2info() {
		return line2route2dep2stop2info;
	}
	
}
