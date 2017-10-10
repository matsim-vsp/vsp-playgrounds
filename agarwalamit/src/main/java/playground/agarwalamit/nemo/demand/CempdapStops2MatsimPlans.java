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

package playground.agarwalamit.nemo.demand;

import java.io.IOException;
import playground.vsp.demandde.cemdap.output.CemdapStops2MatsimPlansConverter;

/**
 * Created by amit on 22.09.17.
 */

public class CempdapStops2MatsimPlans {

    public static void main(String[] args) throws IOException {
        // Local use
        String cemdapDataRoot = "/Users/amit/Documents/gitlab/nemo/data/cemdap_output/";
        int numberOfFirstCemdapOutputFile = 100;
        int numberOfPlans = 1;
        int numberOfPlansFile = 100;
        String outputDirectory = "/Users/amit/Documents/gitlab/nemo/data/matsim_initial/" + numberOfPlansFile + "/";
        String zonalShapeFile = "/Users/amit/Documents/gitlab/nemo/data/cemdap_input/shapeFiles/sourceShape_NRW/dvg2gem_nw.shp";
        String zoneIdTag = "KN";
        boolean allowVariousWorkAndEducationLocations = true;
        boolean addStayHomePlan = true;
        boolean useLandCoverData = true;
        String landCoverFile = "/Users/amit/Documents/gitlab/nemo/data/cemdap_input/shapeFiles/CORINE_landcover_nrw/corine_nrw_src_clc12.shp";
        String stopFile = "Stops_sample.out";
        String activityFile = "Activity.out";
        boolean simplifyGeometries = false;

        // Server use
        if (args.length != 0) {
            numberOfFirstCemdapOutputFile = Integer.parseInt(args[0]);
            numberOfPlans = Integer.parseInt(args[1]);
            allowVariousWorkAndEducationLocations = Boolean.parseBoolean(args[2]);
            addStayHomePlan = Boolean.parseBoolean(args[3]);
            outputDirectory = args[4];
            zonalShapeFile = args[5];
            cemdapDataRoot = args[6];
            useLandCoverData = Boolean.parseBoolean(args[7]);
            landCoverFile = args[8];
            simplifyGeometries = Boolean.valueOf(args[9]);
        }

        CemdapStops2MatsimPlansConverter.convert(cemdapDataRoot, numberOfFirstCemdapOutputFile, numberOfPlans, outputDirectory,
                zonalShapeFile, zoneIdTag, allowVariousWorkAndEducationLocations, addStayHomePlan,
                useLandCoverData, landCoverFile, stopFile, activityFile,simplifyGeometries);
    }


}
