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

package playground.agarwalamit.fundamentalDiagrams.dynamicPCU.areaSpeedRatioMethod.projectedArea;

/**
 * Created by amit on 29.06.17.
 *
 * see ChandraKumar2003EffctLaneWidthMxdTrfc for details about the projected area.
 */

public enum VehicleProjectedAreaRatio {

    // vehicleType (ratio with respect to car), // area
    car (1.0), // 5.39
    bicycle (0.16), // 0.85
    bike (0.16), // 0.85
    motorbike (0.22), // 1.2
    truck (3.27), // 17.62
    bus(4.59), // 24.74
    tractor (3.02), // 16.28
    cycleRickshaw (0.48) ;// 2.56

    private final double projectedAreaRatio;

    public double getProjectedAreaRatio() {
        return this.projectedAreaRatio;
    }

    public static double getProjectedAreaRatio (final String vehicleType) {
        double ratio =0.;
        for (VehicleProjectedAreaRatio vpar : VehicleProjectedAreaRatio.values()) {
            if (vehicleType.equals(vpar.toString())) return vpar.getProjectedAreaRatio();
        }
        return ratio;
    }

    private VehicleProjectedAreaRatio (double projectedAreaRatio) {
        this.projectedAreaRatio = projectedAreaRatio;
    }

}
