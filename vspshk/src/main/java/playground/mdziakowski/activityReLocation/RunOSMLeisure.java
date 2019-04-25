/* *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.mdziakowski.activityReLocation;

import org.matsim.contrib.accessibility.osm.AmenityReader;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import java.io.FileNotFoundException;

public class RunOSMLeisure {

    public static void main(String[] args) {

        String root = "D:/Arbeit/Berlin/ReLocation/";

//        String inputOSMFile = root + "brandenburg-berlin-latest.osm_01.osm";
        String inputOSMFile = root + "berlin-latest.osm_01.osm";
//        String outputFacilityFile = root + "facilities.xml.gz";
        String outputFacilityFile = root + "MyOwnFacilities.xml";

        String newCoord = "DHDN_GK4";

        CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation("WGS84", newCoord);
        AmenityReader amenityReader = new AmenityReader(inputOSMFile, coordinateTransformation, OSMKeyForLeisure.buildOsmAllLeisureToMatsimTypeMap(), true);

        try {
            amenityReader.parseAmenity(inputOSMFile);
            amenityReader.writeFacilities(outputFacilityFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        System.out.println("Done");

    }

}
