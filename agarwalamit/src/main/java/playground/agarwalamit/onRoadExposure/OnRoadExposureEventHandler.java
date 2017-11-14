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

package playground.agarwalamit.onRoadExposure;

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

    private Map<Id<Person>, Map<String, Double>> person2InhaledMass = new HashMap<>(); //TODO throw events instead
    private Map<Id<Link>, Map<Id<Vehicle>, VehicleLinkEmissionCollector>> agentsOnLink = new HashMap<>();
    private final Map<Id<Vehicle>, String> vehicleId2Mode = new HashMap<>();

    @Inject
    public OnRoadExposureEventHandler(OnRoadExposureConfigGroup config) {
        this.config = config;
    }

    @Override
    public void reset(int iteration) {
        this.vehicle2DriverDelegate.reset(iteration);
        this.person2InhaledMass.clear();
        this.agentsOnLink.clear();
        this.vehicleId2Mode.clear();
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
        this.agentsOnLink.get(event.getLinkId())
                         .values()
                         .stream()
                         .forEach(e -> e.addWarmEmissions(event.getWarmEmissions()));
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        Id<Vehicle> vehicleId = event.getVehicleId();
        Id<Link> linkId = event.getLinkId();
        double time = event.getTime();

        VehicleLinkEmissionCollector vehicleLinkEmissionCollector = new VehicleLinkEmissionCollector(event.getVehicleId(),
                event.getLinkId(),
                this.vehicleId2Mode.get(event.getVehicleId()));
        vehicleLinkEmissionCollector.setLinkEnterTime(event.getTime());


        Map<Id<Vehicle>, VehicleLinkEmissionCollector> vehicleId2EmissionCollector = this.agentsOnLink.get(event.getLinkId());
        if (vehicleId2EmissionCollector == null) {
            vehicleId2EmissionCollector = new HashMap<>();
        }
        vehicleId2EmissionCollector.put(event.getVehicleId(), vehicleLinkEmissionCollector);
        agentsOnLink.put(event.getLinkId(), vehicleId2EmissionCollector);
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
        Id<Vehicle> vehicleId = event.getVehicleId();
        Id<Link> linkId = event.getLinkId();
        double time = event.getTime();

        VehicleLinkEmissionCollector vehicleLinkEmissionCollector = this.agentsOnLink.get(linkId).remove(vehicleId);
        vehicleLinkEmissionCollector.setLinkLeaveTime(time);
        Map<String, Double> inhaledMass = vehicleLinkEmissionCollector.getInhaledMass(config);

        Id<Person> personId = getDriverOfVehicle(vehicleId);
        Map<String, Double> temp = this.person2InhaledMass.get(personId);

        if (temp == null) temp = inhaledMass;
        else {
            for (String str : inhaledMass.keySet()) {
                temp.put(str, temp.get(str) + inhaledMass.get(str));
            }
        }

        this.person2InhaledMass.put(personId, temp);
    }

    @Override
    public void handleEvent(VehicleEntersTrafficEvent event) {
        vehicle2DriverDelegate.handleEvent(event);

        this.vehicleId2Mode.put(event.getVehicleId(), event.getNetworkMode());

        VehicleLinkEmissionCollector vehicleLinkEmissionCollector = new VehicleLinkEmissionCollector(event.getVehicleId(),
                event.getLinkId(),
                event.getNetworkMode());
        vehicleLinkEmissionCollector.setLinkEnterTime(event.getTime());

        Map<Id<Vehicle>, VehicleLinkEmissionCollector> vehicleId2EmissionCollector = this.agentsOnLink.get(event.getLinkId());
        if (vehicleId2EmissionCollector == null) {
            vehicleId2EmissionCollector = new HashMap<>();
        }
        vehicleId2EmissionCollector.put(event.getVehicleId(), vehicleLinkEmissionCollector);
        agentsOnLink.put(event.getLinkId(), vehicleId2EmissionCollector);
    }

    @Override
    public void handleEvent(VehicleLeavesTrafficEvent event) {
        vehicle2DriverDelegate.handleEvent(event);

        Id<Vehicle> vehicleId = event.getVehicleId();
        Id<Link> linkId = event.getLinkId();
        double time = event.getTime();

        VehicleLinkEmissionCollector vehicleLinkEmissionCollector = this.agentsOnLink.get(linkId).remove(vehicleId);
        vehicleLinkEmissionCollector.setLinkLeaveTime(time);
        Map<String, Double> inhaledMass = vehicleLinkEmissionCollector.getInhaledMass(config);

        Map<String, Double> temp = this.person2InhaledMass.get(event.getPersonId());

        if (temp == null) temp = inhaledMass;
        else {
            for (String str : inhaledMass.keySet()) {
                temp.put(str, temp.get(str) + inhaledMass.get(str));
            }
        }

        this.person2InhaledMass.put(event.getPersonId(), temp);

    }

    public Id<Person> getDriverOfVehicle(Id<Vehicle> vehicleId) {
        return vehicle2DriverDelegate.getDriverOfVehicle(vehicleId);
    }

    public Map<Id<Person>, Map<String, Double>> getPerson2InhaledMass() {
        return person2InhaledMass;
    }

    public Map<String, Double> getTotalInhaledMass() {
        Map<String, Double> totalMass = new HashMap<>();
        for (Id<Person> personId : this.person2InhaledMass.keySet()) {
            this.person2InhaledMass.get(personId)
                                   .entrySet()
                                   .stream()
                                   .forEach(e -> {
                                       if (totalMass.containsKey(e.getKey())) {
                                           totalMass.put(e.getKey(), e.getValue() + totalMass.get(e.getKey()));
                                       } else {
                                           totalMass.put(e.getKey(), e.getValue());
                                       }
                                   });
        }
        return totalMass;
    }
}
