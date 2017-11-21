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

package playground.agarwalamit.emissions.onRoadExposure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEventHandler;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vehicles.Vehicle;

/**
 * Created by amit on 08.11.17.
 * <p>
 * The idea is to collect the emissions by all persons on a link between receptor's entry-exit on this link.
 * <p>
 * Exposure due to its own emissions are considered now (because events are processed in next time step).
 * <p>
 * The only thing which is not included is cold emission events which are thrown at a later time step but on a former link. This should be significant anyways.
 */

public class OnRoadExposureHandler implements WarmEmissionEventHandler, ColdEmissionEventHandler,
        VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler,
        LinkLeaveEventHandler, LinkEnterEventHandler {

    private final EventsComparatorForEmissions.EventsOrder eventsOrder;

    private boolean processedAllEvents = false; // just to make sure that all events are processed before getting any analysis.

    private TreeMap<Double, List<Event>> time2ListOfEvents = new TreeMap<>();

    private final Map<Id<Vehicle>, Tuple<Id<Person>,String>> driverAgents = new HashMap<>();

    private final OnRoadExposureConfigGroup config;

    private OnRoadExposureTable onRoadExposureTable = new OnRoadExposureTable(); // this will keep all info in it
    private Map<Id<Link>, Map<Id<Vehicle>, VehicleLinkEmissionCollector>> agentsOnLink = new HashMap<>();

    public OnRoadExposureHandler(OnRoadExposureConfigGroup config) {
        this(config, EventsComparatorForEmissions.EventsOrder.EMISSION_EVENTS_BEFORE_LINK_LEAVE_EVENT);
    }

    public OnRoadExposureHandler(OnRoadExposureConfigGroup config, EventsComparatorForEmissions.EventsOrder eventsOrder) {
        this.config = config;
        this.eventsOrder = eventsOrder;
    }

    @Override
    public void reset(int iteration) {
        this.agentsOnLink.clear();
        this.driverAgents.clear();
        this.onRoadExposureTable.clear();
        this.time2ListOfEvents.clear();
    }

    @Override
    public void handleEvent(ColdEmissionEvent event) { //source
        this.time2ListOfEvents.get(event.getTime()).add(event);
        processEvents(event.getTime());
    }

    @Override
    public void handleEvent(WarmEmissionEvent event) { //source
        this.time2ListOfEvents.get(event.getTime()).add(event);
        processEvents(event.getTime());
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        this.time2ListOfEvents.get(event.getTime()).add(event);
        processEvents(event.getTime());
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
        if ( ! this.time2ListOfEvents.containsKey(event.getTime()) ) {
            this.time2ListOfEvents.put(event.getTime(), new ArrayList<>());
        }
        this.time2ListOfEvents.get(event.getTime()).add(event);
        processEvents(event.getTime());
    }

    @Override
    public void handleEvent(VehicleEntersTrafficEvent event) {
        if (! this.time2ListOfEvents.containsKey(event.getTime())) {
            this.time2ListOfEvents.put(event.getTime(), new ArrayList<>());
        }

        this.time2ListOfEvents.get(event.getTime()).add(event);
        processEvents(event.getTime());
    }

    @Override
    public void handleEvent(VehicleLeavesTrafficEvent event) {
        if (! this.time2ListOfEvents.containsKey(event.getTime())) {
            this.time2ListOfEvents.put(event.getTime(), new ArrayList<>());
        }
        this.time2ListOfEvents.get(event.getTime()).add(event);
        processEvents(event.getTime());
    }

    private void processEvents(double time){ // basically delay all concerned events until next time step.
        // get list and remove previous time steps
        List<Event> events = this.time2ListOfEvents.entrySet()
                                                   .stream()
                                                   .filter(e -> e.getKey() < time)
                                                   .flatMap(e -> e.getValue().stream())
                                                   .collect(Collectors.toList());
        //remove old time steps
        new HashSet<>(this.time2ListOfEvents.keySet()).stream()
                                                      .filter(e -> e < time)
                                                      .forEach(e -> this.time2ListOfEvents.remove(e));

        Collections.sort(events, new EventsComparatorForEmissions(eventsOrder));

        for (Event event : events) {
            if(event instanceof VehicleEntersTrafficEvent) {

                VehicleEntersTrafficEvent vehicleEntersTrafficEvent = (VehicleEntersTrafficEvent) event;
                driverAgents.put(vehicleEntersTrafficEvent.getVehicleId(), new Tuple<>(vehicleEntersTrafficEvent.getPersonId(), vehicleEntersTrafficEvent.getNetworkMode()));
                registerReceptor(vehicleEntersTrafficEvent.getVehicleId(), vehicleEntersTrafficEvent.getLinkId(), vehicleEntersTrafficEvent.getTime());

            } else if(event instanceof LinkLeaveEvent) {

                LinkLeaveEvent linkLeaveEvent = (LinkLeaveEvent) event;
                deRegisterReceptor(linkLeaveEvent.getVehicleId(), linkLeaveEvent.getLinkId(), linkLeaveEvent.getTime());

            } else if(event instanceof LinkEnterEvent) {

                LinkEnterEvent linkEnterEvent = (LinkEnterEvent) event;
                registerReceptor(linkEnterEvent.getVehicleId(), linkEnterEvent.getLinkId(), linkEnterEvent.getTime());

            } else if(event instanceof ColdEmissionEvent) {

                final ColdEmissionEvent coldEmissionEvent = (ColdEmissionEvent) event;
                this.agentsOnLink.get(coldEmissionEvent.getLinkId())
                                 .values()
                                 .stream()
                                 .forEach(e -> e.addColdEmissions(coldEmissionEvent.getColdEmissions()));

            } else if(event instanceof WarmEmissionEvent) {

                final WarmEmissionEvent warmEmissionEvent = (WarmEmissionEvent) event;
                this.agentsOnLink.get(warmEmissionEvent.getLinkId())
                                 .values()
                                 .stream()
                                 .forEach(e -> e.addWarmEmissions(warmEmissionEvent.getWarmEmissions()));

            } else if(event instanceof VehicleLeavesTrafficEvent) {

                VehicleLeavesTrafficEvent vehicleLeavesTrafficEvent = (VehicleLeavesTrafficEvent) event;

                deRegisterReceptor(vehicleLeavesTrafficEvent.getVehicleId(), vehicleLeavesTrafficEvent.getLinkId(), vehicleLeavesTrafficEvent.getTime());
                this.driverAgents.remove(vehicleLeavesTrafficEvent.getVehicleId());

            } else {

                throw new RuntimeException("Event "+event+" is not implemented yet.");

            }
        }
    }

    // >>>> register - deregister >>>>

    private void registerReceptor(Id<Vehicle> vehicleId, Id<Link> linkId, double time){
        VehicleLinkEmissionCollector vehicleLinkEmissionCollector = new VehicleLinkEmissionCollector(vehicleId,
                linkId, this.driverAgents.get(vehicleId).getSecond() );
        vehicleLinkEmissionCollector.setLinkEnterTime(time);

        Map<Id<Vehicle>, VehicleLinkEmissionCollector> vehicleId2EmissionCollector = this.agentsOnLink.get(linkId);
        if (vehicleId2EmissionCollector == null) {
            vehicleId2EmissionCollector = new HashMap<>();
        }
        VehicleLinkEmissionCollector previousValueIfExists = vehicleId2EmissionCollector.put(vehicleId, vehicleLinkEmissionCollector);

        if ( previousValueIfExists!=null ) {
            throw new RuntimeException("Container already has a vehicle with id "+vehicleId+", this is undesirable during registration of source. " +
                    "It can happen due to wrong sorting of the events.");
        }

        agentsOnLink.put(linkId, vehicleId2EmissionCollector);
    }

    private void deRegisterReceptor(Id<Vehicle> vehicleId, Id<Link> linkId, double time){
        VehicleLinkEmissionCollector vehicleLinkEmissionCollector = this.agentsOnLink.get(linkId).remove(vehicleId);
        if (vehicleLinkEmissionCollector == null) {
            System.out.println("Vehicle id "+ vehicleId +" linkId "+ linkId + " time "+ time);
        }
        vehicleLinkEmissionCollector.setLinkLeaveTime(time);
        Map<String, Double> inhaledMass = vehicleLinkEmissionCollector.getInhaledMass(config);

        this.onRoadExposureTable.addInfoToTable(driverAgents.get(vehicleId).getFirst(), linkId, driverAgents.get(vehicleId).getSecond(), time, inhaledMass);
    }

    // <<<< register - deregister <<<<


    // >>>> get output >>>>
    public OnRoadExposureTable getOnRoadExposureTable() {
        if (! processedAllEvents) processEvents(Double.POSITIVE_INFINITY);
        return onRoadExposureTable;
    }
}
