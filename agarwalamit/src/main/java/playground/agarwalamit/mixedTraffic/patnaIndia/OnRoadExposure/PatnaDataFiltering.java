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
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;
import playground.agarwalamit.utils.FileUtils;
import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * Created by amit on 22.11.17.
 */

public class PersonCoordChanger {

    private static final String wardsFile = FileUtils.SHARED_SVN+"/projects/patnaIndia/inputs/raw/others/wardFile/Wards.shp";
    private static final Collection<SimpleFeature> simpleFeatureCollection = ShapeFileReader.getAllFeatures(wardsFile);

    public static void main(String[] args) {

        String inputFile = FileUtils.RUNS_SVN+"/patnaIndia/run111/onRoadExposure/bauLastItr/analysis/personToOnRoadExposure.txt";
        String outputFile = FileUtils.RUNS_SVN+"/patnaIndia/run111/onRoadExposure/bauLastItr/analysis/personToOnRoadExposure_withZoneId.txt";
        String plansFile = FileUtils.RUNS_SVN+"/patnaIndia/run108/jointDemand/policies/0.15pcu/bau/output_plans.xml.gz";

        Population population = LoadMyScenarios.loadScenarioFromPlans(plansFile).getPopulation();

        BufferedReader reader = IOUtils.getBufferedReader(inputFile);
        BufferedWriter writer = IOUtils.getBufferedWriter(outputFile);
        Map<String, String> zoneId2Data = new HashMap<>();

        try {
            String line = reader.readLine();
            while (line!=null){
                if (line.startsWith("personId")) writer.write(line+"\tzoneId\n");
                else {
                    String parts [] = line.split("\t");
                    String X = parts[1];
                    String Y = parts[2];
                    if (X.equals("NA")) {
                        writer.write(line+"\tNA\n");
                    } else{
                        Person person = population.getPersons().get(Id.createPersonId(parts[0]));
                        Coord homeCoord = ((Activity)person.getSelectedPlan().getPlanElements().get(0)).getCoord();
                        String zone = getZoneId(homeCoord);
                        Coord cord = PatnaUtils.COORDINATE_TRANSFORMATION.transform(new Coord(Double.valueOf(X),Double.valueOf(Y)));
                        parts[1] = String.valueOf(homeCoord.getX());
                        parts[2] = String.valueOf(homeCoord.getY());
                        for(String str : parts){
                            writer.write(str+"\t");
                        }
                        writer.write(zone);
                        writer.newLine();
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

    private static String getZoneId (Coord cord){
        for(SimpleFeature simpleFeature : simpleFeatureCollection){
            CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(PatnaUtils.EPSG, TransformationFactory.WGS84);
            cord = ct.transform(cord);
            Point point = new GeometryFactory().createPoint(new Coordinate(cord.getX(), cord.getY()));
            if ( ((Geometry) simpleFeature.getDefaultGeometry()).contains(point ) ) {
                return String.valueOf(simpleFeature.getAttribute("ID1"));
            }
        }
        return "NA";
    }

}
