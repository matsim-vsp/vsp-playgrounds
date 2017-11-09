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
import java.util.stream.Collectors;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEventHandler;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.vehicles.Vehicle;

/**
 * Created by amit on 08.11.17.
 */

public class OnRoadExposureEventHandler implements WarmEmissionEventHandler, ColdEmissionEventHandler,
        VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler,
        LinkLeaveEventHandler, LinkEnterEventHandler {

    private final Map<Id<Vehicle>, Map<String, Double>> vehicle2Pollutant2Concentration = new HashMap<>();
    private final Vehicle2DriverEventHandler vehicle2DriverDelegate = new Vehicle2DriverEventHandler();

    @Override
    public void handleEvent(ColdEmissionEvent event) {
        Map<String, Double> pollutant2concentration = event.getColdEmissions()
                                                           .entrySet()
                                                           .stream()
                                                           .collect(Collectors.toMap(e -> e.getKey().getText(),
                                                                   e -> e.getValue()));
        if (vehicle2Pollutant2Concentration.containsKey(event.getVehicleId())) {
            Map<String, Double> valueSoFar = vehicle2Pollutant2Concentration.get(event.getVehicleId());
            Map<String, Double> newValue = pollutant2concentration.entrySet()
                                                                  .stream()
                                                                  .collect(Collectors.toMap(e -> e.getKey(),
                                                                          e -> e.getValue() + valueSoFar.get(e.getKey())));
            vehicle2Pollutant2Concentration.put(event.getVehicleId(), newValue);
        } else {
            vehicle2Pollutant2Concentration.put(event.getVehicleId(), pollutant2concentration);
        }

    }

    @Override
    public void handleEvent(WarmEmissionEvent event) {
        Map<String, Double> pollutant2concentration = event.getWarmEmissions()
                                                           .entrySet()
                                                           .stream()
                                                           .collect(Collectors.toMap(e -> e.getKey().getText(),
                                                                   e -> e.getValue()));
        if (vehicle2Pollutant2Concentration.containsKey(event.getVehicleId())) {
            Map<String, Double> valueSoFar = vehicle2Pollutant2Concentration.get(event.getVehicleId());
            Map<String, Double> newValue = pollutant2concentration.entrySet()
                                                                  .stream()
                                                                  .collect(Collectors.toMap(e -> e.getKey(),
                                                                          e -> e.getValue() + valueSoFar.get(e.getKey())));
            vehicle2Pollutant2Concentration.put(event.getVehicleId(), newValue);
        } else {
            vehicle2Pollutant2Concentration.put(event.getVehicleId(), pollutant2concentration);
        }
    }

    @Override
    public void reset(int iteration) {

    }

    @Override
    public void handleEvent(LinkEnterEvent event) {

    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {

    }

    @Override
    public void handleEvent(VehicleEntersTrafficEvent event) {
        vehicle2DriverDelegate.handleEvent(event);
    }

    @Override
    public void handleEvent(VehicleLeavesTrafficEvent event) {
        vehicle2DriverDelegate.handleEvent(event);
    }

    public Id<Person> getDriverOfVehicle(Id<Vehicle> vehicleId) {
        return vehicle2DriverDelegate.getDriverOfVehicle(vehicleId);
    }
}
