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
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.types.ColdPollutant;
import org.matsim.contrib.emissions.types.WarmPollutant;
import org.matsim.vehicles.Vehicle;

/**
 * Created by amit on 09.11.17.
 */

public class VehicleLinkEmissionCollector {

    private final Id<Vehicle> vehicleId;
    private final Id<Link> linkId;
    private final String mode;

    private final Map<String, Double> emissions = new HashMap<>();
    private double travelTime = 0;

    VehicleLinkEmissionCollector(Id<Vehicle> vehicleId, Id<Link> linkId, String mode) {
        this.vehicleId = vehicleId;
        this.linkId = linkId;
        this.mode = mode;
        initialize();
    }

    private void initialize(){
        this.emissions.clear();
        for (WarmPollutant warmPollutant : WarmPollutant.values()) {
            if (warmPollutant.equals(WarmPollutant.CO2_TOTAL)) continue;
            emissions.put(warmPollutant.toString(), 0.);
        }
    }

    void setLinkEnterTime(double time) {
        travelTime -= time;
    }

    void setLinkLeaveTime(double time) {
        travelTime += time;
    }

    void addColdEmissions(Map<ColdPollutant, Double> coldEmissions) {
        this.emissions.entrySet()
                      .stream()
                      .filter(e -> ColdPollutant.getValue(e.getKey())!=null)
                      .forEach(e -> this.emissions.put(e.getKey(),
                              e.getValue() + coldEmissions.get(ColdPollutant.valueOf(e.getKey()))));
    }

    void addWarmEmissions(Map<WarmPollutant, Double> warmEmissions) {
        this.emissions.entrySet()
                     .stream()
                     .forEach(e -> this.emissions.put(e.getKey(),
                             e.getValue() + warmEmissions.get(WarmPollutant.valueOf(e.getKey()))));
    }

    Map<String, Double> getInhaledMass(OnRoadExposureConfigGroup config) {
        Map<String, Double> emiss= new OnRoadExposureCalculator(config).calculate(this.mode, emissions, travelTime);
        this.initialize(); // clear the emissions once inhaled mass is calculated to avoid possibility of multiple counts
        return emiss;
    }

    public Id<Vehicle> getVehicleId() {
        return vehicleId;
    }

    public Id<Link> getLinkId() {
        return linkId;
    }
}
