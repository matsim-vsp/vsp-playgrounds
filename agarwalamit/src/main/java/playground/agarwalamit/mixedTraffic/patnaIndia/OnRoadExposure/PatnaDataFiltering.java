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

package playground.agarwalamit.mixedTraffic.patnaIndia.OnRoadExposure;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaPersonFilter;
import playground.agarwalamit.utils.FileUtils;
import playground.agarwalamit.utils.PersonFilter;

/**
 * Created by amit on 22.11.17.
 */

public class PatnaDataFiltering {

//    private static final String wardsFile = FileUtils.SHARED_SVN+"/projects/patnaIndia/inputs/raw/others/wardFile/Wards.shp";
//    private static final Collection<SimpleFeature> simpleFeatureCollection = ShapeFileReader.getAllFeatures(wardsFile);

    public static void main(String[] args) {

        PersonFilter personFilter = new PatnaPersonFilter();

        String inputFile = FileUtils.RUNS_SVN+"/patnaIndia/run111/onRoadExposure/bauLastItr/analysis/personToOnRoadExposure.txt";
        String outputFile = FileUtils.RUNS_SVN+"/patnaIndia/run111/onRoadExposure/bauLastItr/analysis/personToOnRoadExposure_urbanPersonOnly.txt";

//        Map<String, String> zoneId2Data = new HashMap<>();

        try (BufferedReader reader = IOUtils.getBufferedReader(inputFile);
             BufferedWriter writer = IOUtils.getBufferedWriter(outputFile);) {
            String line = reader.readLine();
            while (line!=null){
                if (line.startsWith("personId")) writer.write(line+"\n");
                else {
                    String parts [] = line.split("\t");
                    String personId = parts[0];
                    if ( personFilter.includePerson(Id.createPersonId(personId)) ) {
                        writer.write(line+"\n");
                    }
                }
                line = reader.readLine();
            }
            writer.close();
            reader.close();
        } catch (IOException e) {
            throw new RuntimeException("Data is not written/read. Reason : " + e);
        }
    }
}
