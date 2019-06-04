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

import org.apache.log4j.Logger;
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

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class RunReLocationPlans {

    private static final Logger log = Logger.getLogger(RunReLocationPlans.class);

    public static void main(String[] args) throws MalformedURLException {

        String planFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.3-1pct/input/berlin-v5.3-1pct.plans.xml.gz";
        String shapeFile = "D:/Arbeit/Berlin/ReLocation/BB_BE_Shape/grid_5000_intersect_Id.shp";
        String facilitiesFile = "D:/Arbeit/Berlin/ReLocation/MyOwnFacilities.xml";

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

        MatsimFacilitiesReader matsimFacilitiesReader = new MatsimFacilitiesReader(scenario);
        matsimFacilitiesReader.readFile(facilitiesFile);

        PopulationReader populationReader = new PopulationReader(scenario);
        populationReader.readURL(new URL(planFile));

        Map<String, Geometry> allDistricts = readShapeFile(shapeFile);

        Map<String, List<Coord>> oldLeisureFacilities = new HashMap<>();
        Map<String, List<Coord>> oldShoppingFacilities = new HashMap<>();

        Map<String, List<Coord>> newLeisureFacilities = new HashMap<>();
        Map<String, List<Coord>> newShoppingFacilities = new HashMap<>();

        ActivityFacilities activityFacilities = scenario.getActivityFacilities();

        for (ActivityFacility activityFacility : activityFacilities.getFacilities().values()) {
            Coord coord = activityFacility.getCoord();
            for (ActivityOption activityOption : activityFacility.getActivityOptions().values()) {
                if (activityOption.getType().contains("leisure")) {
                    String name = inDistrict(allDistricts, coord);
                    if (newLeisureFacilities.containsKey(name)) {
                        newLeisureFacilities.get(name).add(coord);
                    } else {
                        List<Coord> coords = new ArrayList<>();
                        coords.add(coord);
                        newLeisureFacilities.put(name, coords);
                    }
                }
                if (activityOption.getType().contains("shopping")) {
                    String name = inDistrict(allDistricts, coord);
                    if (newShoppingFacilities.containsKey(name)) {
                        newShoppingFacilities.get(name).add(coord);
                    } else {
                        List<Coord> coords = new ArrayList<>();
                        coords.add(coord);
                        newShoppingFacilities.put(name, coords);
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
                        Coord coord = activity.getCoord();
                        if (activity.getType().contains("leisure")) {
                            String name = inDistrict(allDistricts, coord);
                            if (oldLeisureFacilities.containsKey(name)) {
                                oldLeisureFacilities.get(name).add(coord);
                            } else {
                                List<Coord> coords = new ArrayList<>();
                                coords.add(coord);
                                oldLeisureFacilities.put(name, coords);
                            }
                        }
                        if (activity.getType().contains("shopping")) {
                            String name = inDistrict(allDistricts, coord);
                            if (oldShoppingFacilities.containsKey(name)) {
                                oldShoppingFacilities.get(name).add(coord);
                            } else {
                                List<Coord> coords = new ArrayList<>();
                                coords.add(coord);
                                oldShoppingFacilities.put(name, coords);
                            }
                        }
                    }
                }
            }
        }

        if (allDistricts.size() != newLeisureFacilities.size()) {
            log.warn((allDistricts.size() - newLeisureFacilities.size()) + " Zones have 0 leisure Activities");
        }
        checkingAmountOfActivitiesPerZone(newLeisureFacilities);

        if (allDistricts.size() != newShoppingFacilities.size()) {
            log.warn((allDistricts.size() - newShoppingFacilities.size()) + " Zones have 0 shopping Activities");
        }
        checkingAmountOfActivitiesPerZone(newShoppingFacilities);

        for (Person person : population.getPersons().values()) {
            for (Plan plan : person.getPlans()) {
                for (PlanElement planElement : plan.getPlanElements()) {
                    if (planElement instanceof Activity) {
                        Activity activity = (Activity) planElement;
                        if (activity.getType().contains("leisure")) {
                            if (!(inDistrict(allDistricts,activity.getCoord()).equals("noZone"))) {
                                List<Coord> coords = newLeisureFacilities.get(inDistrict(allDistricts, activity.getCoord()));
                                if (coords != null) {
                                    activity.setCoord(coords.get(new Random().nextInt(coords.size())));
                                }
                            }
                        }
                        if (activity.getType().contains("shopping")) {
                            if (!(inDistrict(allDistricts,activity.getCoord()).equals("noZone"))) {
                                List<Coord> coords = newLeisureFacilities.get(inDistrict(allDistricts, activity.getCoord()));
                                if (coords != null) {
                                    activity.setCoord(coords.get(new Random().nextInt(coords.size())));
                                }
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

    private static void checkingAmountOfActivitiesPerZone(Map<String, List<Coord>> newFacilities) {
        for (Map.Entry<String, List<Coord>> amountList : newFacilities.entrySet()) {
            if (amountList.getValue().size() == 0) {
                log.warn(amountList.getKey() + " has 0 activities, maybe use lager zones");
            }
        }
    }

    private static Map<String, Geometry> readShapeFile(String shapeFile){
        Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(shapeFile);
        Map<String, Geometry> districts = new HashMap<>();

        for (SimpleFeature feature : features) {
            String id = feature.getID();
            Geometry geometry = (Geometry) feature.getDefaultGeometry();
            districts.put(id, geometry);
        }

        return districts;
    }

    private static String inDistrict( Map<String, Geometry> allDistricts, Coord coord) {
        Point point = MGC.coord2Point(coord);
        for (String nameDistrict : allDistricts.keySet()) {
            Geometry geo = allDistricts.get(nameDistrict);
            if (geo.contains(point)) {
                return nameDistrict;
            }
        }
        return "noZone";
    }

}