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

package playground.agarwalamit.fundamentalDiagrams.dynamicPCU.surveyData;

import java.util.Arrays;
import java.util.List;

/**
 * Created by amit on 23.05.18.
 */

class SurveyDataUtils {

    static final List<String> modes = Arrays.asList("car", "big_car","bus","3W","2W");//car-->small_car
    static final List<String> modes_coded_in_data_file = Arrays.asList("CS", "CB","HV (Bus)","3W","2W");

    static double getPCU(String mode){
        switch(mode){
            case "car": return 1.0;
            case "big_car": return 1.5;
            case "bus": return 3.0;
            case "3W": return 1.5;
            case "2W": return 0.25;
        }
        throw new RuntimeException(mode + " not implemented yet.");
    }
    static double getSpeed(String mode){
        switch(mode){
            case "car": return 60.67/3.6;
            case "big_car": return 55.67/3.6;
            case "bus": return 37.50/3.6;
            case "3W": return 39.27/3.6;
            case "2W": return 67.70/3.6;
        }
        throw new RuntimeException(mode + " not implemented yet.");
    }

    /**
     * Source paper: MeenaEtcCapacitySixLaneAndSEF
     * @param mode
     * @return
     */
    static double getLength(String mode){
        switch(mode){
            case "car": return 3.72;
            case "big_car": return 4.58;
            case "bus": return 10.1;
            case "3W": return 3.2;
            case "2W": return 1.87;
        }
        throw new RuntimeException(mode + " not implemented yet.");
    }

    /**
     * Source paper: MeenaEtcCapacitySixLaneAndSEF
     * @param mode
     * @return
     */
    static double getProjectedArea(String mode){
        switch(mode){
            case "car": return 5.36;
            case "big_car": return 8.11;
            case "bus": return 24.54;
            case "3W": return 4.48;
            case "2W": return 1.20;
        }
        throw new RuntimeException(mode + " not implemented yet.");
    }
}
