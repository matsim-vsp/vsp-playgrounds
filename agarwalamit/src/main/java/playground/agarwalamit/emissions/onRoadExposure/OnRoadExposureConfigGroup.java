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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.emissions.types.WarmPollutant;
import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * Created by amit on 08.11.17.
 */

public class OnRoadExposureConfigGroup extends ReflectiveConfigGroup {

    public OnRoadExposureConfigGroup() {
        super(GROUP_NAME);
        this.initializeMaps();
    }

    public static final String GROUP_NAME = "onRoadExposure";
    private double dispersionRate = 9.0; //m^2/s; --> BigazziEtc2015OnRoadExposureMotorists

    private final Map<String, Double> pollutantToBackgroundConcentration = new HashMap<>();
    private final Map<String, Map<String, Double>> modeToPollutantToPenetrationRate = new HashMap<>();
    private final Map<String, Double> modeToBreathingRate = new HashMap<>();
    private final Map<String, Double> modeToOccupancy = new HashMap<>();

    private final Map<String, Double> pollutantToPenetrationRate = new HashMap<>();

    private boolean usingMicroGramUnits = true;

    public void initializeMaps(){
        Arrays.stream(WarmPollutant.values()).forEach(pollutant -> {
            pollutantToBackgroundConcentration.put(pollutant.toString(), 0.);
            pollutantToPenetrationRate.put(pollutant.toString(), 1.);
        });
        //fill data at least for mode car
        this.modeToBreathingRate.put(TransportMode.car, 0.66/3600.0 );// cubic-meter / s
        this.modeToOccupancy.put(TransportMode.car, 1.2);
        this.modeToPollutantToPenetrationRate.put(TransportMode.car, pollutantToPenetrationRate);
    }

    public double getDispersionRate() {
        return dispersionRate;
    }

    public void setDispersionRate(double dispersionRate) {
        this.dispersionRate = dispersionRate;
    }

    public Map<String, Double> getPollutantToBackgroundConcentration() {
        return pollutantToBackgroundConcentration;
    }

    public Map<String, Double> getPollutantToPenetrationRate(String mode) {
        if (! modeToPollutantToPenetrationRate.containsKey(mode)) modeToPollutantToPenetrationRate.put(mode, pollutantToPenetrationRate);
        return modeToPollutantToPenetrationRate.get(mode);
    }

    public Map<String, Double> getModeToBreathingRate() {
        return modeToBreathingRate;
    }

    public Map<String, Double> getModeToOccupancy() {
        return modeToOccupancy;
    }

    public boolean isUsingMicroGramUnits() {
        return usingMicroGramUnits;
    }

    public void setUsingMicroGramUnits(boolean usingMicroGramUnits) {
        this.usingMicroGramUnits = usingMicroGramUnits;
    }
}
