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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
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
 * Probably, a problem is that (I think), *EmissionEvents are thrown after LinkLeaveEvent.
 */

public class OnRoadExposureHandler implements WarmEmissionEventHandler, ColdEmissionEventHandler,
        VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler,
        LinkLeaveEventHandler, LinkEnterEventHandler, PersonEntersVehicleEventHandler {

    private TreeMap<Double, List<Event>> timeToEvents = new TreeMap<>(); // sorted by time: now sort list of events based on events occurrences

    private final Map<Id<Vehicle>, Tuple<Id<Person>,String>> driverAgents = new HashMap<>();

    private final OnRoadExposureConfigGroup config;

    private OnRoadExposureTable onRoadExposureTable = new OnRoadExposureTable(); // this will keep all info in it
    private Map<Id<Link>, Map<Id<Vehicle>, VehicleLinkEmissionCollector>> agentsOnLink = new HashMap<>();

    /**
     * temporarily store all leaving agents to process them for warm emission events
     */
//    private Map<Double, Map<Id<Vehicle>, VehicleLinkEmissionCollector>> sameTimeLeftAgent = new HashMap<>(); //

    @Inject
    public OnRoadExposureHandler(OnRoadExposureConfigGroup config) {
        this.config = config;
    }

    @Override
    public void reset(int iteration) {
        this.agentsOnLink.clear();
        this.driverAgents.clear();
        this.onRoadExposureTable.clear();
    }

    @Override
    public void handleEvent(ColdEmissionEvent event) { //source
//        if (tempColdEmissionEvents.containsKey(event.getVehicleId())) {
//            tempColdEmissionEvents.put(event.getVehicleId(), event);
//        } else {
            this.agentsOnLink.get(event.getLinkId())
                             .values()
                             .stream()
                             .forEach(e -> e.addColdEmissions(event.getColdEmissions()));
//        }
    }

    @Override
    public void handleEvent(WarmEmissionEvent event) { //source
        // all persons who are currently on the link are exposed
        this.agentsOnLink.get(event.getLinkId())
                         .values()
                         .stream()
                         .forEach(e -> e.addWarmEmissions(event.getWarmEmissions()));

        //  all persons who left in this time step are also exposed.
//        if (this.sameTimeLeftAgent.get(new Double(event.getTime())) !=null ) {
//            this.sameTimeLeftAgent.get(new Double(event.getTime()))
//                                  .values()
//                                  .stream()
//                                  .forEach(e -> e.addWarmEmissions(event.getWarmEmissions()));
//        }
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        registerReceptor(event.getVehicleId(), event.getLinkId(), event.getTime());
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
        Id<Vehicle> vehicleId = event.getVehicleId();
        Id<Link> linkId = event.getLinkId();
        double now = event.getTime();

        VehicleLinkEmissionCollector vehicleLinkEmissionCollector = deRegisterReceptor(vehicleId, linkId, now);
        {
            // clean previous time steps
//            this.sameTimeLeftAgent.keySet()
//                                  .stream()
//                                  .filter(past -> past < now)
//                                  .forEach(past -> this.sameTimeLeftAgent.remove(past));


//            Map<Id<Vehicle>, VehicleLinkEmissionCollector> tempContainer = this.sameTimeLeftAgent.get(new Double (now));
//            if (tempContainer == null) {
//                tempContainer = new HashMap<>();
//            }
//            tempContainer.put(vehicleId, vehicleLinkEmissionCollector);
//            this.sameTimeLeftAgent.put(new Double(now),tempContainer);
        }
    }

    @Override
    public void handleEvent(VehicleEntersTrafficEvent event) {
        driverAgents.put(event.getVehicleId(), new Tuple<>(event.getPersonId(), event.getNetworkMode()));
        registerReceptor(event.getVehicleId(), event.getLinkId(), event.getTime());
//        if (tempColdEmissionEvents.containsKey(event.getVehicleId())){
//            this.agentsOnLink.get(event.getLinkId())
//                             .values()
//                             .stream()
//                             .forEach(e -> e.addColdEmissions(tempColdEmissionEvents.get(event.getVehicleId()).getColdEmissions()));
////            tempColdEmissionEvents.remove(event.getVehicleId());
//        }
    }

    @Override
    public void handleEvent(VehicleLeavesTrafficEvent event) {
        Id<Vehicle> vehicleId = event.getVehicleId();

        deRegisterReceptor(vehicleId, event.getLinkId(), event.getTime());
        this.driverAgents.remove(event.getVehicleId());
    }

    // >>>> register - deregister - reregister >>>>

    private VehicleLinkEmissionCollector registerReceptor(Id<Vehicle> vehicleId, Id<Link> linkId, double time){
        VehicleLinkEmissionCollector vehicleLinkEmissionCollector = new VehicleLinkEmissionCollector(vehicleId,
                linkId, this.driverAgents.get(vehicleId).getSecond() );
        vehicleLinkEmissionCollector.setLinkEnterTime(time);

        Map<Id<Vehicle>, VehicleLinkEmissionCollector> vehicleId2EmissionCollector = this.agentsOnLink.get(linkId);
        if (vehicleId2EmissionCollector == null) {
            vehicleId2EmissionCollector = new HashMap<>();
        }
        vehicleId2EmissionCollector.put(vehicleId, vehicleLinkEmissionCollector);
        agentsOnLink.put(linkId, vehicleId2EmissionCollector);
        return vehicleLinkEmissionCollector;
    }

    private VehicleLinkEmissionCollector deRegisterReceptor(Id<Vehicle> vehicleId, Id<Link> linkId, double time){
        VehicleLinkEmissionCollector vehicleLinkEmissionCollector = this.agentsOnLink.get(linkId).remove(vehicleId);
        vehicleLinkEmissionCollector.setLinkLeaveTime(time);
        Map<String, Double> inhaledMass = vehicleLinkEmissionCollector.getInhaledMass(config);

        this.onRoadExposureTable.addInfoToTable(driverAgents.get(vehicleId).getFirst(), linkId, driverAgents.get(vehicleId).getSecond(), time, inhaledMass);
        return vehicleLinkEmissionCollector;
    }

    // <<<< register - deregister - reregister <<<<

    public OnRoadExposureTable getOnRoadExposureTable() {
        return onRoadExposureTable;
    }

//    // the problem is that...reading events file and regenerating emission events writes cold emission events before vehicle enters traffic event ..
//    private Map<Id<Vehicle>, ColdEmissionEvent> tempColdEmissionEvents = new HashMap<>();
    @Override
    public void handleEvent(PersonEntersVehicleEvent event) {
//        tempColdEmissionEvents.put(event.getVehicleId(), null);
    }

}
