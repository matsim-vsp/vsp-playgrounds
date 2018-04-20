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

package playground.agarwalamit.analysis.speed.tripSpeed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.api.experimental.events.handler.TeleportationArrivalEventHandler;
import org.matsim.vehicles.Vehicle;

/**
 * Created by amit on 19.04.18.
 */

public class PersonTripHandler implements PersonDepartureEventHandler, PersonArrivalEventHandler, LinkLeaveEventHandler,
        TeleportationArrivalEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {

    private final Map<Id<Person>, List<Trip>> personToTrip = new HashMap<>();
    private final Map<Id<Vehicle>, Id<Person>> vehicleIdToPersonId = new HashMap<>();
    private final Network network;

    public PersonTripHandler(Network network) {
        this.network = network;
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        List<Trip> trips = this.personToTrip.get(event.getPersonId());

        if (trips==null){
            trips = new ArrayList<>();
        }

        Trip trip = new Trip(event.getPersonId(), event.getLegMode(), event.getTime(), event.getLinkId(), trips.size());
        trips.add(trip);
        this.personToTrip.put(event.getPersonId(), trips);
    }

    @Override
    public void handleEvent(VehicleEntersTrafficEvent event) {
        List<Trip> trips = this.personToTrip.get(event.getPersonId());
        trips.get(trips.size()-1).setVehicleId(event.getVehicleId());
        this.vehicleIdToPersonId.put(event.getVehicleId(), event.getPersonId());
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
        List<Trip> trips = this.personToTrip.get(this.vehicleIdToPersonId.get(event.getVehicleId()));
        Trip lastTrip = trips.get(trips.size()-1);
        if (! lastTrip.getDepartureLink().equals(event.getLinkId())) {
            lastTrip.travelledOn(event.getLinkId(), this.network.getLinks().get(event.getLinkId()).getLength());
        }
    }

    @Override
    public void handleEvent(VehicleLeavesTrafficEvent event) {
        this.vehicleIdToPersonId.remove(event.getVehicleId());
    }

    @Override
    public void handleEvent(PersonArrivalEvent event) {
        List<Trip> trips = this.personToTrip.get(event.getPersonId());
        Trip lastTrip = trips.get(trips.size()-1);
        lastTrip.travelledOn(event.getLinkId(), this.network.getLinks().get(event.getLinkId()).getLength());
        lastTrip.arrival(event.getLinkId(), event.getTime());
    }

    @Override
    public void handleEvent(TeleportationArrivalEvent event) {
        List<Trip> trips = this.personToTrip.get(event.getPersonId());
        Trip lastTrip = trips.get(trips.size()-1);
        lastTrip.teleportation(event.getTime(), event.getDistance());
    }

    @Override
    public void reset(int iteration) {
        this.personToTrip.clear();
    }

    public Map<Id<Person>, List<Trip>> getPersonToTrip() {
        return personToTrip;
    }
}
