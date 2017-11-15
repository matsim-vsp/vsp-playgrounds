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
import java.util.Map;
import javax.inject.Inject;
import org.matsim.api.core.v01.Id;
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
import org.matsim.vehicles.Vehicle;

/**
 * Created by amit on 08.11.17.
 * <p>
 * The idea is to collect the emissions by all persons on a link between receptor's entry-exit on this link.
 * <p>
 * Probably, a problem is that (I think), *EmissionEvents are thrown after LinkLeaveEvent.
 */

public class OnRoadExposureEventHandler implements WarmEmissionEventHandler, ColdEmissionEventHandler,
        VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler,
        LinkLeaveEventHandler, LinkEnterEventHandler {

    private final Vehicle2DriverEventHandler vehicle2DriverDelegate = new Vehicle2DriverEventHandler();

    private final OnRoadExposureConfigGroup config;

    private OnRoadExposureTable onRoadExposureTable = new OnRoadExposureTable(); // this will keep all info in it
    private Map<Id<Link>, Map<Id<Vehicle>, VehicleLinkEmissionCollector>> agentsOnLink = new HashMap<>();

    /**
     * temporarily store all leaving agents to process them for warm emission events
     */
    private Map<Double, Map<Id<Vehicle>, VehicleLinkEmissionCollector>> sameTimeLeftAgent = new HashMap<>(); //

    private final Map<Id<Vehicle>, String> vehicleId2Mode = new HashMap<>();

    @Inject
    public OnRoadExposureEventHandler(OnRoadExposureConfigGroup config) {
        this.config = config;
    }

    @Override
    public void reset(int iteration) {
        this.vehicle2DriverDelegate.reset(iteration);
        this.agentsOnLink.clear();
        this.vehicleId2Mode.clear();
        this.onRoadExposureTable.clear();
    }

    @Override
    public void handleEvent(ColdEmissionEvent event) {
        this.agentsOnLink.get(event.getLinkId())
                         .values()
                         .stream()
                         .forEach(e -> e.addColdEmissions(event.getColdEmissions()));
    }

    @Override
    public void handleEvent(WarmEmissionEvent event) {
        // all persons who are currently on the link are exposed
        this.agentsOnLink.get(event.getLinkId())
                         .values()
                         .stream()
                         .forEach(e -> e.addWarmEmissions(event.getWarmEmissions()));

        //  all persons who left in this time step are also exposed.
        if (this.sameTimeLeftAgent.get(new Double(event.getTime())) !=null ) {
            this.sameTimeLeftAgent.get(new Double(event.getTime()))
                                  .values()
                                  .stream()
                                  .forEach(e -> e.addWarmEmissions(event.getWarmEmissions()));
        }
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        Id<Vehicle> vehicleId = event.getVehicleId();
        Id<Link> linkId = event.getLinkId();
        double time = event.getTime();

        registerReceptor(vehicleId, linkId, time);
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
        Id<Vehicle> vehicleId = event.getVehicleId();
        Id<Link> linkId = event.getLinkId();
        double now = event.getTime();

        VehicleLinkEmissionCollector vehicleLinkEmissionCollector = deRegisterReceptor(vehicleId, linkId, now);
        {
            // clean previous time steps
            this.sameTimeLeftAgent.keySet()
                                  .stream()
                                  .filter(past -> past < now)
                                  .forEach(past -> this.sameTimeLeftAgent.remove(past));


            Map<Id<Vehicle>, VehicleLinkEmissionCollector> tempContainer = this.sameTimeLeftAgent.get(new Double (now));
            if (tempContainer == null) {
                tempContainer = new HashMap<>();
            }
            tempContainer.put(vehicleId, vehicleLinkEmissionCollector);
            this.sameTimeLeftAgent.put(new Double(now),tempContainer);
        }
    }

    @Override
    public void handleEvent(VehicleEntersTrafficEvent event) {
        vehicle2DriverDelegate.handleEvent(event);

        Id<Vehicle> vehicleId = event.getVehicleId();
        this.vehicleId2Mode.put(vehicleId, event.getNetworkMode());

        registerReceptor(vehicleId, event.getLinkId(), event.getTime());
    }

    @Override
    public void handleEvent(VehicleLeavesTrafficEvent event) {
        this.vehicle2DriverDelegate.handleEvent(event);
        this.vehicleId2Mode.remove(event.getVehicleId());

        Id<Vehicle> vehicleId = event.getVehicleId();
        Id<Link> linkId = event.getLinkId();
        double time = event.getTime();

        deRegisterReceptor(vehicleId, linkId, time);
    }

    // >>>> register - deregister - reregister >>>>

    private VehicleLinkEmissionCollector registerReceptor(Id<Vehicle> vehicleId, Id<Link> linkId, double time){
        VehicleLinkEmissionCollector vehicleLinkEmissionCollector = new VehicleLinkEmissionCollector(vehicleId,
                linkId, this.vehicleId2Mode.get(vehicleId) );
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

        Id<Person> personId = getDriverOfVehicle(vehicleId);
        this.onRoadExposureTable.addInfoToTable(personId, linkId, this.vehicleId2Mode.get(vehicleId), time, inhaledMass);
        return vehicleLinkEmissionCollector;
    }

    // <<<< register - deregister - reregister <<<<

    public Id<Person> getDriverOfVehicle(Id<Vehicle> vehicleId) {
        return vehicle2DriverDelegate.getDriverOfVehicle(vehicleId);
    }

    public OnRoadExposureTable getOnRoadExposureTable() {
        return onRoadExposureTable;
    }
}
