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

package playground.mdziakowski.activityReLocation;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.opengis.feature.simple.SimpleFeature;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * overrides the coordinates from activities from an old plan file with new coordinates from a facility file
 * needs a shape file to make sure that the coordinates will replaced with a nearby facility 
 * 
 * @author mdziakowski
 */

public class RunReLocationPlansSauber {

    private static final Logger log = Logger.getLogger(RunReLocationPlans.class);

    public static void main(String[] args) {
    	
    	String planFile;
    	String shapeFile;
    	String facilitiesFile;
    	String outputPlans;
    	String logFile;
    	
    	if ( args.length==0 || args[0].equals("")) {

	        planFile = "http://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.3-10pct/input/berlin-v5.3-10pct.plans.xml.gz";	
	        shapeFile = "D:/Arbeit/Berlin/ReLocation/BB_BE_Shape/grid_2000_intersect_Id.shp";
	        facilitiesFile = "D:/Arbeit/Berlin/ReLocation/FirstBerlinBrandenburgFacilities/combinedFacilities.xml";
	        outputPlans = "D:/Arbeit/Berlin/ReLocation/richtigerRun/PlansWithNewLocations2000.xml";
	        logFile = "D:/Arbeit/Berlin/ReLocation/richtigerRun/log2000";
	        
    	} else {
    		
    		planFile = args[0];
    		shapeFile = args[1];
    		facilitiesFile = args[2];
    		outputPlans = args[3];
    		logFile = args[4];
    		
    	}
    	
        OutputDirectoryLogging.catchLogEntries();
        try {
        	OutputDirectoryLogging.initLoggingWithOutputDirectory(logFile);
        } catch (IOException e1) {
        	e1.printStackTrace();
        }
        
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

        MatsimFacilitiesReader matsimFacilitiesReader = new MatsimFacilitiesReader(scenario);
        matsimFacilitiesReader.readFile(facilitiesFile);

        PopulationReader populationReader = new PopulationReader(scenario);
        try {
            populationReader.readURL(new URL(planFile));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        Map<String, Geometry> allZones = readShapeFile(shapeFile);

        ActivityFacilities activityFacilities = scenario.getActivityFacilities();

        Map<String, List<Coord>> newLeisureFacilities = facilitiesToZone(activityFacilities, allZones, "leisure");
        Map<String, List<Coord>> newShoppingFacilities = facilitiesToZone(activityFacilities, allZones, "shopping");

        Population population = scenario.getPopulation();

        Map<String, List<Coord>> oldLeisureActivities = oldActivitiesToZone(population, allZones, "leisure");
        Map<String, List<Coord>> oldShoppingActivities = oldActivitiesToZone(population, allZones, "shopping");

        logWarnings(allZones, newLeisureFacilities, oldLeisureActivities, "leisure");
        logWarnings(allZones, newShoppingFacilities, oldShoppingActivities, "shopping");

        oldLeisureActivities.clear();
        oldShoppingActivities.clear();
        
        Population outPopulation = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation();

        createNewPopulation(outputPlans, allZones, newLeisureFacilities, newShoppingFacilities, population,
				outPopulation);
      
        System.out.println("Done");

    }

    /**
     * creates a new population file with new coordinates for specific activities
     * 
     * @param outputPlans - the location for the output plan file
     * @param allZones - a shape file with zones
     * @param newLeisureFacilities - a map with 
     * @param newShoppingFacilities
     * @param population
     * @param outPopulation1
     */
    
	private static void createNewPopulation(String outputPlans, Map<String, Geometry> allZones,
			Map<String, List<Coord>> newLeisureFacilities, Map<String, List<Coord>> newShoppingFacilities,
			Population population, Population outPopulation) {
		for (Person person : population.getPersons().values()) {
            for (Plan plan : person.getPlans()) {
                for (PlanElement planElement : plan.getPlanElements()) {
                    if (planElement instanceof Activity) {
                        Activity activity = (Activity) planElement;
                        String act = inDistrict(allZones, activity.getCoord());
                        if (activity.getType().contains("leisure")) {
                            if (!(act.equals("noZone"))) {
                                List<Coord> coords = newLeisureFacilities.get(act);
                                if (coords != null) {
                                    activity.setCoord(coords.get(new Random().nextInt(coords.size())));
                                }
                            }
                        }
                        if (activity.getType().contains("shopping")) {
                            if (!(act.equals("noZone"))) {
                                List<Coord> coords = newShoppingFacilities.get(act);
                                if (coords != null) {
                                    activity.setCoord(coords.get(new Random().nextInt(coords.size())));
                                }
                            }
                        }
                    }
                }
            }
            outPopulation.addPerson(person);
        }

        new PopulationWriter(outPopulation).write(outputPlans);
	}

    /**
     * reads a shape file with the zones for your relocation
     * 
     * @param shapeFile - the shape file with the zones
     * @return a map with the zoneId as keys and a geometry as value
     */
    
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

    /**
     * classified all facilities from the facility file with a specific matsim activity to the zones from the shape file
     * 
     * @param activityFacilities - all facilities that should be used for the relocation
     * @param allZones - a shape file with zones
     * @param matsimActivity - the activity type where your facilities should be 
     * @return a map with zoneIds as keys and a list with the coordinates from all facilities in that zone
     */
      
    private static Map<String, List<Coord>> facilitiesToZone(ActivityFacilities activityFacilities,  Map<String, Geometry> allZones, String matsimActivity) {
        Map<String, List<Coord>> newFacilities = new HashMap<>();
        for (ActivityFacility activityFacility : activityFacilities.getFacilities().values()) {
            Coord coord = activityFacility.getCoord();
            for (ActivityOption activityOption : activityFacility.getActivityOptions().values()) {
                if (activityOption.getType().contains(matsimActivity)) {
                    String name = inDistrict(allZones, coord);
                    if (name.equals("noZone")) {
                        log.warn("no Zone found for Activity (Coord " + activityFacility.getCoord() + ")");
                        continue;
                    }
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
        return newFacilities;
    }

    /**
     * checks if a coordinate is in a zone from the shape file 
     * 
     * @param allZones - all zones from the shape file
     * @param coord - coordinate that will be checked
     * @return the the zoneId as a string or "noZone", if the coordinate can't be associated
     */
    
    private static String inDistrict( Map<String, Geometry> allZones, Coord coord) {
        Point point = MGC.coord2Point(coord);
        for (String nameZone : allZones.keySet()) {
            Geometry geo = allZones.get(nameZone);
            if (geo.contains(point)) {
                return nameZone;
            }
        }
        return "noZone";
    }

    /**
     *  classified all activities from the old population file with a specific matsim activity to the zones from the shape file
     * 
     * @param population - old population with a plans file
     * @param allZones - a shape file with zones
     * @param matsimActivity - the activity type that should be relocated
     * @return a map with zoneIds as keys and a list with the coordinates from all activities in that zone
     */
    
    private static Map<String, List<Coord>> oldActivitiesToZone(Population population, Map<String, Geometry> allZones, String matsimActivity) {
        Map<String, List<Coord>> oldActivities = new HashMap<>();
        for (Person person : population.getPersons().values()) {
            for (Plan plan : person.getPlans()) {
                for (PlanElement planElement : plan.getPlanElements()) {
                    if (planElement instanceof Activity) {
                        Activity activity = (Activity) planElement;
                        Coord coord = activity.getCoord();
                        if (activity.getType().contains(matsimActivity)) {
                            String name = inDistrict(allZones, coord);
                            if (oldActivities.containsKey(name)) {
                                oldActivities.get(name).add(coord);
                            } else {
                                List<Coord> coords = new ArrayList<>();
                                coords.add(coord);
                                oldActivities.put(name, coords);
                            }
                        }
                    }
                }
            }
        }
        return oldActivities;
    }

    /**
     * writes in the log file warnings, because some characteristic of the input files can cause problems
     * 
     * @param allZones - all zones from the shape file 
     * @param newFacilities - the new facilities for the relocation and their zones
     * @param oldActivities - the old activities from the plans file and their zones
     * @param matsimtype - the activity type that should be checked for warnings
     */
    
    private static void logWarnings(Map<String, Geometry> allZones, Map<String, List<Coord>> newFacilities, Map<String, List<Coord>> oldActivities, String matsimtype) {

        List<String> warningType1 = new ArrayList<>();
        List<String> warningType2 = new ArrayList<>();
        List<String> warningType3 = new ArrayList<>();

        for (String zone : allZones.keySet()) {
            if (oldActivities.containsKey(zone) && newFacilities.containsKey(zone)) {
//                warningType1.add();
            } else if (oldActivities.containsKey(zone) && !(newFacilities.containsKey(zone))) {
                warningType2.add(oldActivities.get(zone).size() + " old " + matsimtype + "  activities and " + "0 new facilities found");
            } else if (!(oldActivities.containsKey(zone)) && newFacilities.containsKey(zone)) {
                warningType3.add("0 old " + matsimtype + " activities and " + newFacilities.get(zone).size() + " new facilities found");
            }
        }
        log.warn("Total mismatch: " + (warningType2.size() + warningType3.size()) + " " + matsimtype);
//        if (warningType1.size() != 0) {
//            log.warn("Mismatch between " + matsimtype + " activities from Agents an tagged facilities");
//        }
//        for (String massage : warningType1) {
//            log.warn(massage);
//        }
//        if (warningType2.size() != 0) {
//            log.warn("Found old " + matsimtype + " activities from the agents but there are no facilities tagged");
//        }
//        for (String massage : warningType2) {
//            log.warn(massage);
//        }
//        if (warningType3.size() != 0) {
//            log.warn("Found facilities but no old " + matsimtype + " activities from the agents");
//        }
//        for (String massage : warningType3) {
//            log.warn(massage);
//        }
    }

}
