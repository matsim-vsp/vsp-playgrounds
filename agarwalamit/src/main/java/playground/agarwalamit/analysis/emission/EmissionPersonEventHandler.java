/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.agarwalamit.analysis.emission;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEventHandler;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;
import org.matsim.contrib.emissions.types.ColdPollutant;
import org.matsim.contrib.emissions.types.WarmPollutant;
import org.matsim.contrib.emissions.utils.EmissionUtils;
import org.matsim.vehicles.Vehicle;

import playground.agarwalamit.utils.MapUtils;

/**
 * EmissionEvents only have vehicle id, which looks fine, however, to collect emissions for every person, one need
 * a connector, which is only available via normal events file. Thus, both emission and normal event files are required.
 * For this, use CombinedMatsimEventsReader.
 *
 * Created by amit on 23/12/2016.
 */

public class EmissionPersonEventHandler implements WarmEmissionEventHandler, ColdEmissionEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler{

    private final Map<Id<Vehicle>, Id<Person>> vehicle2DriverIdCollector = new HashMap<>();

    private final Map<Id<Person>, Map<String, Double>> personId2ColdEmissions = new HashMap<>();
    private final Map<Id<Person>, Map<String, Double>> personId2WarmEmissions = new HashMap<>();
    private final Map<Id<Vehicle>, Map<String, Double>> vehicleId2ColdEmissions = new HashMap<>();
    private final Map<Id<Vehicle>, Map<String, Double>> vehicleId2WarmEmissions = new HashMap<>();

    @Override
    public void handleEvent(WarmEmissionEvent event) {
        Id<Person> driverId = getDriverOfVehicle(event.getVehicleId());
        {
//            Map<WarmPollutant, Double> warmEmissions = this.personId2WarmEmissions.get(driverId);
//
//            if (warmEmissions == null ) {
//                this.personId2WarmEmissions.put(driverId, event.getWarmEmissions());
//            } else {
//                this.personId2WarmEmissions.put(driverId,
//                        Arrays.stream(WarmPollutant.values()).collect(
//                                Collectors.toMap(wp -> wp,
//                                        wp -> warmEmissions.get( wp ) + event.getWarmEmissions().get( wp ) ))
//                );
//            }
            this.personId2WarmEmissions.merge(driverId,
                    event.getWarmEmissions(),
                    (a, b) -> MapUtils.mergeMaps(a, b) );
        }

        {

            this.vehicleId2WarmEmissions.merge(event.getVehicleId(),
                    event.getWarmEmissions(),
                    (a, b) -> MapUtils.mergeMaps(a, b) );
        }
    }

    @Override
    public void handleEvent(ColdEmissionEvent event) {
        Id<Person> driverId = getDriverOfVehicle(event.getVehicleId());
        {

            this.personId2ColdEmissions.merge(driverId,
                    event.getColdEmissions(),
                    (a, b) -> MapUtils.mergeMaps(a, b));
        }

        {

            this.vehicleId2ColdEmissions.merge(event.getVehicleId(),
                    event.getColdEmissions(),
                    (a, b) -> MapUtils.mergeMaps(a, b));
        }

    }

    @Override
    public void reset(int iteration) {
        this.personId2WarmEmissions.clear();
        this.personId2ColdEmissions.clear();
        this.vehicleId2ColdEmissions.clear();
        this.vehicleId2WarmEmissions.clear();
        this.vehicle2DriverIdCollector.clear();
    }

    @Override
    public void handleEvent(VehicleEntersTrafficEvent event) {
        this.vehicle2DriverIdCollector.put(event.getVehicleId(), event.getPersonId());
    }

    @Override
    public void handleEvent(VehicleLeavesTrafficEvent event) {
        this.vehicle2DriverIdCollector.remove(event.getVehicleId());
    }

    private Id<Person> getDriverOfVehicle(Id<Vehicle> vehicleId) {
        return this.vehicle2DriverIdCollector.get(vehicleId);
    }

    public Map<Id<Person>, Map<String, Double>> getPersonId2ColdEmissions() {
        return personId2ColdEmissions;
    }

    public Map<Id<Person>, Map<String, Double>> getPersonId2WarmEmissions() {
        return personId2WarmEmissions;
    }

    public Map<Id<Vehicle>, Map<String, Double>> getVehicleId2ColdEmissions() {
        return vehicleId2ColdEmissions;
    }

    public Map<Id<Vehicle>, Map<String, Double>> getVehicleId2WarmEmissions() {
        return vehicleId2WarmEmissions;
    }

    public Map<Id<Vehicle>, Map<String, Double>> getVehicleId2TotalEmissions(){
        return this.vehicleId2WarmEmissions.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry ->
                EmissionUtils.sumUpEmissions(entry.getValue(), this.vehicleId2ColdEmissions.get(entry.getKey()))));
    }

    public Map<Id<Person>, Map<String, Double>> getPersonId2TotalEmissions(){
        return this.personId2WarmEmissions.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry ->
                EmissionUtils.sumUpEmissions(entry.getValue(), this.personId2ColdEmissions.get(entry.getKey()))));
    }
}
