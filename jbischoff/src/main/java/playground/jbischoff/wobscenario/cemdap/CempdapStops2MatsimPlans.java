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

package playground.jbischoff.wobscenario.cemdap;

import java.io.IOException;

/**
 * Created by amit on 22.09.17.
 */

public class CempdapStops2MatsimPlans {


    /**
     * The plan is now:
     * - generate the matsim_plans without any coordinates for each file of cempdap_output (i.e. same process 5 times)
     * - the zone information is added to the activity types e.g. home_510
     * - sample first matsim_plans
     * - take sampled plans, add other plans of the sampled persons from other matsim_plans file and combine them in a file
     * - add the acitivity locations based on CORINE land cover data and zone information
     */

    public static void main(String[] args) throws IOException {
        // Local use
        String cemdapDataRoot = "D:/cemdap-vw/Output/";
        for (int i = 1; i<=5;i++){
        	if (i == 4) continue;
        int numberOfFirstCemdapOutputFile = i;
        int numberOfPlans = 1;
        int numberOfPlansFile = i;
        String outputDirectory = "D:/cemdap-vw/Output/" + numberOfPlansFile + "/";
        String zonalShapeFile1 = "../../../shared-svn/projects/vw_rufbus/projekt2/data/new_cemdap_scenario/zensus/nssa.shp";
        String zoneIdTag1 = "AGS";
        String zonalShapeFile2 = "../../../shared-svn/projects/vw_rufbus/projekt2/data/new_cemdap_scenario/zensus/wvi-zones.shp";
        String zoneIdTag2 = "NO";
        boolean allowVariousWorkAndEducationLocations = true;
        boolean addStayHomePlan = true;
        boolean useLandCoverData = true;
        String landCoverFile = "../../../shared-svn/projects/vw_rufbus/projekt2/data/new_cemdap_scenario/zensus/corine-nssa.shp";
        String stopFile = "Stops.out";
        String activityFile = "Activity.out";
        boolean simplifyGeometries = true;
        boolean assignCoordinatesToActivities = true;
        boolean combiningGeoms = false;

      

        CemdapStops2MatsimPlansConverter.convert(cemdapDataRoot, numberOfFirstCemdapOutputFile, numberOfPlans, outputDirectory,
                zonalShapeFile1, zoneIdTag1, zonalShapeFile2, zoneIdTag2, allowVariousWorkAndEducationLocations, addStayHomePlan,
                useLandCoverData, landCoverFile, stopFile, activityFile,simplifyGeometries, combiningGeoms, assignCoordinatesToActivities);
        
    }
    }
}
