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
import java.util.HashMap;
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
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vehicles.Vehicle;

/**
 * Created by amit on 08.11.17.
 * <p>
 * The idea is to collect the emissions by all persons on a link between receptor's entry-exit on this link.
 * <p>
 * Exposure due to its own emissions are considered now (because events are processed in next time step).
 * <p>
 * The only thing which is not included is cold emission events which are thrown at a later time step but on a former link. This should not be significant anyways.
 */

public class OnRoadExposureHandler implements WarmEmissionEventHandler, ColdEmissionEventHandler,
        VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler,
        LinkLeaveEventHandler, LinkEnterEventHandler {

    private boolean processedAllEvents = false; // just to make sure that all events are processed before getting any analysis.

    private final TreeMap<Double, TreeMap<Id<Person>, List<Event>>> time2ListOfEvents = new TreeMap<>();
    private final Map<Id<Vehicle>, Tuple<Id<Person>,String>> driverAgents = new HashMap<>();
    private OnRoadExposureTable onRoadExposureTable = new OnRoadExposureTable(); // this will keep all info in it
    private Map<Id<Link>, Map<Id<Vehicle>, VehicleLinkEmissionCollector>> agentsOnLink = new HashMap<>();
    private Vehicle2DriverEventHandler delegate = new Vehicle2DriverEventHandler();
    
    private final OnRoadExposureConfigGroup config;

    public OnRoadExposureHandler(OnRoadExposureConfigGroup config) {
        this.config = config;
    }

    @Override
    public void reset(int iteration) {
        this.agentsOnLink.clear();
        this.driverAgents.clear();
        this.onRoadExposureTable.clear();
        this.time2ListOfEvents.clear();
        this.delegate.reset(iteration);
    }

    @Override
    public void handleEvent(ColdEmissionEvent event) { //source
        TreeMap<Id<Person>, List<Event>> person2Events = this.time2ListOfEvents.get(event.getTime());
        person2Events.get( this.delegate.getDriverOfVehicle(event.getVehicleId()) ).add(event);
        processEvents(event.getTime());
    }

    @Override
    public void handleEvent(WarmEmissionEvent event) { //source
        TreeMap<Id<Person>, List<Event>> person2Events = this.time2ListOfEvents.get(event.getTime());
        person2Events.get( this.delegate.getDriverOfVehicle(event.getVehicleId()) ).add(event);
        processEvents(event.getTime());
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        TreeMap<Id<Person>, List<Event>> person2Events = this.time2ListOfEvents.get(event.getTime());
        person2Events.get(  this.delegate.getDriverOfVehicle(event.getVehicleId()) ).add(event);
        processEvents(event.getTime());
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
        Id<Person> personId = this.delegate.getDriverOfVehicle(event.getVehicleId());
        TreeMap<Id<Person>, List<Event>> person2Events = this.time2ListOfEvents.get(event.getTime());

        if (person2Events==null) {
            person2Events = new TreeMap<>();
            person2Events.put(personId, new ArrayList<>());
        } else if (! person2Events.containsKey(personId)) {
            person2Events.put(personId, new ArrayList<>());
        }

        person2Events.get(personId).add(event);
        this.time2ListOfEvents.put(event.getTime(), person2Events);

        processEvents(event.getTime());
    }

    @Override
    public void handleEvent(VehicleEntersTrafficEvent event) {
    		this.delegate.handleEvent(event);
        Id<Person> personId = event.getPersonId();
        TreeMap<Id<Person>, List<Event>> person2Events = this.time2ListOfEvents.get(event.getTime());

        if (person2Events==null) {
            person2Events = new TreeMap<>();
            person2Events.put(personId, new ArrayList<>());
        } else if (! person2Events.containsKey(personId)) {
            person2Events.put(personId, new ArrayList<>());
        }

        person2Events.get(personId).add(event);
        this.time2ListOfEvents.put(event.getTime(), person2Events);

        processEvents(event.getTime());
    }

    @Override
    public void handleEvent(VehicleLeavesTrafficEvent event) {
//    		this.delegate.handleEvent(event); // keep the info such that info can be used in the events later than this time step.
        Id<Person> personId = event.getPersonId();
        TreeMap<Id<Person>, List<Event>> person2Events = this.time2ListOfEvents.get(event.getTime());

        if (person2Events==null) {
            person2Events = new TreeMap<>();
            person2Events.put(personId, new ArrayList<>());
        } else if (! person2Events.containsKey(personId)) {
            person2Events.put(personId, new ArrayList<>());
        }

        person2Events.get(personId).add(event);
        this.time2ListOfEvents.put(event.getTime(), person2Events);

        processEvents(event.getTime());
    }

    private void processEvents(double time){ // basically delay all concerned events until next time step.
        if ( this.time2ListOfEvents.firstKey() >= time) { //proceed if currentTimeStep is more than the least time in map
            return;
        }

        Map.Entry<Double, TreeMap<Id<Person>, List<Event>>> prevEntry = this.time2ListOfEvents.pollFirstEntry();

        List<Event> events = prevEntry.getValue()
                                      .entrySet()
                                      .stream()
                                      .flatMap(e -> EmissionEventsComparator.sort( e.getValue()).stream())
                                      .collect(Collectors.toList());

        for (Event event : events) {
            if(event instanceof VehicleEntersTrafficEvent) {

                VehicleEntersTrafficEvent vehicleEntersTrafficEvent = (VehicleEntersTrafficEvent) event;
                createOnRoadExporureTrip(vehicleEntersTrafficEvent);

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
//                this.driverAgents.remove(vehicleLeavesTrafficEvent.getVehicleId());

            } else {
                throw new RuntimeException("Event "+event+" is not implemented yet.");
            }
        }
    }

    // >>>> register - deregister >>>>

    private void createOnRoadExporureTrip(VehicleEntersTrafficEvent event){
        this.onRoadExposureTable.createTripAndAddInfo(event.getPersonId(), event.getLinkId(), event.getNetworkMode(), event.getTime());
    }

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
            throw new RuntimeException("Container already has a vehicle with id "+vehicleId+" on link "+ linkId+ " at time "+time+", this is undesirable during registration of source. " +
                    "It can happen due to wrong sorting of the events.");
        }

        agentsOnLink.put(linkId, vehicleId2EmissionCollector);
    }

    private void deRegisterReceptor(Id<Vehicle> vehicleId, Id<Link> linkId, double time){
        VehicleLinkEmissionCollector vehicleLinkEmissionCollector ;
        
        vehicleLinkEmissionCollector = this.agentsOnLink.get(linkId).remove(vehicleId);

        vehicleLinkEmissionCollector.setLinkLeaveTime(time);
        Map<String, Double> inhaledMass = vehicleLinkEmissionCollector.getInhaledMass(config);

        this.onRoadExposureTable.addInfoToTable(driverAgents.get(vehicleId).getFirst(), linkId, driverAgents.get(vehicleId).getSecond(), time, inhaledMass);
    }

    // <<<< register - deregister <<<<


    // >>>> get output >>>>
    public OnRoadExposureTable getOnRoadExposureTable() {
        if (! processedAllEvents) {
            processEvents(Double.POSITIVE_INFINITY);
            processedAllEvents=true;
        }
        return onRoadExposureTable;
    }
}
