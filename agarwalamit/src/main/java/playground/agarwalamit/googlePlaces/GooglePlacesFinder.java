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

package playground.agarwalamit.googlePlaces;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.matsim.core.utils.io.IOUtils;
import playground.agarwalamit.utils.FileUtils;
import se.walkercrou.places.GooglePlaces;
import se.walkercrou.places.Place;

/**
 * Here are the list of supported types: https://developers.google.com/places/supported_types
 *
 * Created by amit on 03.08.17.
 */

public class GooglePlacesFinder {

  // get the key from https://developers.google.com/maps/documentation/javascript/get-api-key
    private static final String apiKey = "xxx";

    public static void main(String[] args) {

        List<Place> allPlaces = new ArrayList<>();
        GooglePlaces client = new GooglePlaces(apiKey);

        // RADAR search is deprecated and provide minimum results (https://developers.google.com/places/web-service/search#RadarSearchRequests)
        //        List<Place> places = client.getNearbyPlaces(25.5941, 85.1376, GooglePlacesInterface.MAXIMUM_RADIUS, GooglePlacesInterface.MAXIMUM_RESULTS);
        {
            List<Place> places = client.getPlacesByQuery("school in Patna, Bihar", GooglePlaces.DEFAULT_RESULTS);
            allPlaces.addAll(places);
        }

        {
            List<Place> places = client.getPlacesByQuery("college in Patna, Bihar", GooglePlaces.DEFAULT_RESULTS);
            allPlaces.addAll(places);
        }

        {
            List<Place> places = client.getPlacesByQuery("university in Patna, Bihar", GooglePlaces.DEFAULT_RESULTS);
            allPlaces.addAll(places);
        }

        //cross check with following supported places types
        List<String> educationalTypes = Arrays.asList("school", "university");

        String outFile = FileUtils.RUNS_SVN+ "/patnaIndia/run110/patnaEducationalPlaces.txt";
        BufferedWriter writer = IOUtils.getBufferedWriter(outFile);
        try {
            writer.write("name\tlatitude\tlongitude\ttype\tid\n");
            for(Place place : allPlaces) {
                for(String type : educationalTypes) {
                    if (place.getTypes().contains(type)) {
                        writer.write(
                                place.getName() +"\t"+
                                        place.getLatitude()+"\t"+
                                        place.getLongitude() +"\t"+
                                        type+"\t"+
                                        place.getPlaceId()+"\n");
                        writer.flush();
                    } else {
                        System.out.println(place.getTypes().toString());
                    }
                }
            }
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException("Data is not written/read. Reason : " + e);
        }


    }

}
