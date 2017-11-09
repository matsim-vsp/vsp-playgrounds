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

import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import playground.agarwalamit.mixedTraffic.patnaIndia.simTime.TravelDistanceForSimTimeExp;

/**
 * Created by amit on 08.11.17.
 */

public class OnRoadExposureCalculator {

    private final OnRoadExposureConfigGroup config;

    OnRoadExposureCalculator(OnRoadExposureConfigGroup onRoadExposureConfigGroup) {
        this.config = onRoadExposureConfigGroup;
    }

    /**
     * @param config
     * @param mode
     * @param emissionRate
     * @param travelTime
     * @return
     */
    public Map<String, Double> calculate(String mode, Map<String, Double> emissionRate, double travelTime) {
        return emissionRate.entrySet()
                           .stream()
                           .collect(Collectors.toMap(e -> e.getKey(),
                                   e -> calculateForSinglePollutant(e.getKey(), e.getValue(), mode, travelTime)));
    }

    /**
     * total inhalation in gm = (b * o * r * p * t + e  * o * r * p / d)
     * b --> background concentration
     * e --> emissions in g/m for time bin T
     * d --> dispersion rate
     * o --> occupancy rate
     * r --> breathing rate
     * p --> penetration rate
     * t --> travelTime
     */
    private double calculateForSinglePollutant(String pollutant, double pollutantValue, String mode, double travelTime) {

        return (config.getPollutantToBackgroundConcentration().get(pollutant)
                * config.getModeToOccupancy().get(mode)
                * config.getModeToBreathingRate().get(mode)
                * config.getPollutantToPenetrationRate(mode).get(pollutant)
                * travelTime)
                + (pollutantValue / config.getDispersionRate()
                * config.getModeToOccupancy().get(mode)
                * config.getModeToBreathingRate().get(mode)
                * config.getPollutantToPenetrationRate(mode).get(pollutant));
    }

}
