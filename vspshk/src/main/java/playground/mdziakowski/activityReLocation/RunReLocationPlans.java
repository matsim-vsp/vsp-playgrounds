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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.facilities.*;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class RunReLocationPlans {

    public static void main(String[] args) throws MalformedURLException {

        String planFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.3-1pct/input/berlin-v5.3-1pct.plans.xml.gz";
        String shapeFile = "D:/Arbeit/Berlin/ReLocation/BerlinShape/Bezirke_GK4.shp";
        String facilitiesFile = "D:/Arbeit/Berlin/ReLocation/MyOwnFacilities.xml";

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

        MatsimFacilitiesReader matsimFacilitiesReader = new MatsimFacilitiesReader(scenario);
        matsimFacilitiesReader.readFile(facilitiesFile);

        PopulationReader populationReader = new PopulationReader(scenario);
        populationReader.readURL(new URL(planFile));

        Map<String, Geometry> allDistricts = readShapeFile(shapeFile);
        Map<String, List<Coord>> newFacilities = new HashMap<>();

        ActivityFacilities activityFacilities = scenario.getActivityFacilities();

        for (ActivityFacility activityFacility : activityFacilities.getFacilities().values()) {
            Coord coord = activityFacility.getCoord();
            for (ActivityOption activityOption : activityFacility.getActivityOptions().values()) {
                if (activityOption.getType().contains("leisure")) {
                    String name = inDistrict(allDistricts, coord);
                    if (newFacilities.containsKey(name)) {
                        newFacilities.get(name).add(coord);
                    } else {
                        List<Coord> coords = new ArrayList<>();
                        coords.add(coord);
                        newFacilities.put(name, coords);
                    }
                }
            }
        }

        Population population = scenario.getPopulation();
        Population outPopulation1 = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation();

        for (Person person : population.getPersons().values()) {
            for (Plan plan : person.getPlans()) {
                for (PlanElement planElement : plan.getPlanElements()) {
                    if (planElement instanceof Activity) {
                        Activity activity = (Activity) planElement;
                        if (activity.getType().contains("leisure")) {
                            if (!(inDistrict(allDistricts,activity.getCoord()).equals("nichtBerlin"))) {
                                List<Coord> coords = newFacilities.get(inDistrict(allDistricts, activity.getCoord()));
                                activity.setCoord(coords.get(new Random().nextInt(coords.size())));
                            }
                        }
                    }
                }
            }
            outPopulation1.addPerson(person);
        }

        new PopulationWriter(outPopulation1).write("Run1.xml");

        System.out.println("Done");
    }

    public static Map<String, Geometry> readShapeFile(String shapeFile){
        Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(shapeFile);
        Map<String, Geometry> districts = new HashMap<>();

        for (SimpleFeature feature : features) {
            String id = (String) feature.getAttribute("Name");
            Geometry geometry = (Geometry) feature.getDefaultGeometry();
            districts.put(id, geometry);
        }

        return districts;
    }

    public static String inDistrict( Map<String, Geometry> allDistricts, Coord coord) {
        Point point = MGC.coord2Point(coord);
        for (String nameDistrict : allDistricts.keySet()) {
            Geometry geo = allDistricts.get(nameDistrict);
            if (geo.contains(point)) {
                return nameDistrict;
            }
        }
        return "nichtBerlin";
    }

}