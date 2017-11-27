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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaPersonFilter;
import playground.agarwalamit.utils.FileUtils;
import playground.agarwalamit.utils.PersonFilter;

/**
 * Created by amit on 22.11.17.
 */

public class PatnaDataFiltering {

    private static final String wardsFile = FileUtils.SHARED_SVN+"/projects/patnaIndia/inputs/raw/others/wardFile/Wards.shp";
    private static final Collection<SimpleFeature> simpleFeatureCollection = ShapeFileReader.getAllFeatures(wardsFile);

    public static void main(String[] args) {

        String inputFile = FileUtils.RUNS_SVN+"/patnaIndia/run111/onRoadExposure/bauLastItr/analysis/personToOnRoadExposure.txt";
        String outputFile = FileUtils.RUNS_SVN+"/patnaIndia/run111/onRoadExposure/bauLastItr/analysis/personToOnRoadExposure_urbanPersonOnly.txt";

        BufferedReader reader = IOUtils.getBufferedReader(inputFile);
        BufferedWriter writer = IOUtils.getBufferedWriter(outputFile);
        Map<String, String> zoneId2Data = new HashMap<>();

        try {
            String line = reader.readLine();
            while (line!=null){
                if (line.startsWith("personId")) writer.write(line+"\n");
                else {
                    String parts [] = line.split("\t");
                    String personId = parts[0];
                    if (isConcernedPerson(personId)) {
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

    private static boolean isConcernedPerson(String personId) {
        Id<Person> person = Id.createPersonId(personId);
        PersonFilter patnaPersonFilter = new PatnaPersonFilter();
        return patnaPersonFilter.getUserGroupAsStringFromPersonId(person).equals(PatnaPersonFilter.PatnaUserGroup.urban.toString());
    }
}
