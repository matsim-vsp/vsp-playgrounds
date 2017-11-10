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

/**
 * Created by amit on 08.11.17.
 */

public class OnRoadExposureCalculator {

    /**
     *
     * @param config
     * @param mode
     * @param emissionRate
     * @param travelTime
     * @return
     */
    public static double calculate(OnRoadExposureConfigGroup config, String mode,
                                   Map<String, Double> emissionRate, double travelTime){
        /**
         * total inhalation in gm = (b + e / d ) * o * r * p . t
         * b --> background concentration
         * e --> emissions in g/m for time bin T
         * d --> dispersion rate
         * o --> occupancy rate
         * r --> breathing rate
         * p --> penetration rate
         * t --> travelTime
         */
        double value = 0.;
        for ( String pollutant : config.getPollutantToBackgroundConcentration().keySet() ){
            value += ( config.getPollutantToBackgroundConcentration().get(pollutant)
                    + emissionRate.get(pollutant) / config.getDispersionRate() )
                    * config.getPollutantToPenetrationRate(mode).get(pollutant)
                    * config.getModeToBreathingRate().get(mode)
                    * config.getModeToOccupancy().get(mode);
        }

        return  value * travelTime;
    }
}
