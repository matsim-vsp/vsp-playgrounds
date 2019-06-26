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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.contrib.accessibility.osm.CombinedOsmReader;
import org.matsim.contrib.accessibility.utils.AccessibilityFacilityUtils;
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

public class RunReLocation {

	private static String root;
	private static String osmFile;
	private static String planFile;
	private static String shapeFile;
	private static String output;
	private static final String outPutCRS = "EPSG:31468";
	private static final Logger log = Logger.getLogger(RunReLocation.class);
	private static final Random random = new Random(55332654);

	public static void main(String[] args) {

		if (args.length == 0 || args[0].equals("")) {
			// path for the input
			root = "./";

			// osm file for the new Locations. Must be in a .osm Format
			osmFile = "";

			// plan file where the locations should be changed
			planFile = "";

			// shape file with zones for the relocation
			shapeFile = "";

			// path for the output
			output = root + "output/";
		} else {
			root = args[0];
			osmFile = args[1];
			planFile = args[2];
			shapeFile = args[3];
			output = args[0] + "output/";
		}

		setUpLog();
		osmToFacilities();
		reLocation();
		
	}

	/**
	 * overrides the coordinates from activities from an old plan file with new coordinates from a facility file
	 * needs a shape file to make sure that the coordinates will replaced with a nearby facility 
	 */
	private static void reLocation() {
		
		log.info("starts relocation");
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

        MatsimFacilitiesReader matsimFacilitiesReader = new MatsimFacilitiesReader(scenario);
        matsimFacilitiesReader.readFile(output + "/facilities.xml");

        PopulationReader populationReader = new PopulationReader(scenario);
        populationReader.readFile(planFile);

        Map<String, Geometry> allZones = readShapeFile(shapeFile);

        ActivityFacilities activityFacilities = scenario.getActivityFacilities();

        Map<String, List<Coord>> newLeisureFacilities = facilitiesToZone(activityFacilities, allZones, "leisure");
        Map<String, List<Coord>> newShoppingFacilities = facilitiesToZone(activityFacilities, allZones, "shop");

        Population population = scenario.getPopulation();

        Map<String, List<Coord>> oldLeisureActivities = oldActivitiesToZone(population, allZones, "leisure");
        Map<String, List<Coord>> oldShoppingActivities = oldActivitiesToZone(population, allZones, "shopping");

        logWarnings(allZones, newLeisureFacilities, oldLeisureActivities, "leisure");
        logWarnings(allZones, newShoppingFacilities, oldShoppingActivities, "shopping");

        oldLeisureActivities.clear();
        oldShoppingActivities.clear();       

        createNewPopulation(new String(output + "output_" + planFile), allZones, newLeisureFacilities, newShoppingFacilities, population);	
	}

	
	/**
	 * sets up a log file
	 */
	private static void setUpLog() {
		OutputDirectoryLogging.catchLogEntries();
		try {
			OutputDirectoryLogging.initLoggingWithOutputDirectory(output + "/log");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * reads a osm file and writes the facilities out
	 */
	private static void osmToFacilities() {
		log.info("startign with facilities");
		CombinedOsmReader combinedOsmReader = new CombinedOsmReader(outPutCRS,
				AccessibilityFacilityUtils.buildOsmLandUseToMatsimTypeMap(),
				AccessibilityFacilityUtils.buildOsmBuildingToMatsimTypeMap(),
				AccessibilityFacilityUtils.buildOsmAmenityToMatsimTypeMapV2(),
				AccessibilityFacilityUtils.buildOsmLeisureToMatsimTypeMapV2(),
				AccessibilityFacilityUtils.buildOsmTourismToMatsimTypeMapV2(),
				AccessibilityFacilityUtils.buildUnmannedEntitiesList(), 0);
		try {
			combinedOsmReader.parseFile(osmFile);
			combinedOsmReader.writeFacilities(output + "facilities.xml");
		} catch (Exception e) {
			e.printStackTrace();
		}
		log.info("facilities done");
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
        if (warningType1.size() != 0) {
            log.warn("Mismatch between " + matsimtype + " activities from Agents an tagged facilities");
        }
        for (String massage : warningType1) {
            log.warn(massage);
        }
        if (warningType2.size() != 0) {
            log.warn("Found old " + matsimtype + " activities from the agents but there are no facilities tagged");
        }
        for (String massage : warningType2) {
            log.warn(massage);
        }
        if (warningType3.size() != 0) {
            log.warn("Found facilities but no old " + matsimtype + " activities from the agents");
        }
        for (String massage : warningType3) {
            log.warn(massage);
        }
    }

    /**
     * creates a new population file with new coordinates for specific activities
     * 
     * @param outputPlans - the location for the output plan file
     * @param allZones - a shape file with zones
     * @param newLeisureFacilities - a map with zoneId and a list of coordinates for leisure
     * @param newShoppingFacilities - a map with zoneId and a list of coordinates for shopping
     * @param population - the population from the plans file that you want to change
     */  
	private static void createNewPopulation(String outputPlans, Map<String, Geometry> allZones,
			Map<String, List<Coord>> newLeisureFacilities, Map<String, List<Coord>> newShoppingFacilities,
			Population population) {
		
		 Population outPopulation = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation();
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
                                    activity.setCoord(coords.get(random.nextInt(coords.size())));
                                }
                            }
                        }
                        if (activity.getType().contains("shopping")) {
                            if (!(act.equals("noZone"))) {
                                List<Coord> coords = newShoppingFacilities.get(act);
                                if (coords != null) {
                                    activity.setCoord(coords.get(random.nextInt(coords.size())));
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
}
