package playground.mdziakowski.activityReLocation;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.opengis.feature.simple.SimpleFeature;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class RunReLocationPlansSauber {

    private static final Logger log = Logger.getLogger(RunReLocationPlans.class);

    public static void main(String[] args) {

        String planFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.3-1pct/input/berlin-v5.3-1pct.plans.xml.gz";

        String shapeFile = "D:/Arbeit/Berlin/ReLocation/BB_BE_Shape/grid_5000_intersect_Id.shp";
        String facilitiesFile = "D:/Arbeit/Berlin/ReLocation/MyOwnFacilities.xml";

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

        Population outPopulation1 = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation();

        for (Person person : population.getPersons().values()) {
            for (Plan plan : person.getPlans()) {
                for (PlanElement planElement : plan.getPlanElements()) {
                    if (planElement instanceof Activity) {
                        Activity activity = (Activity) planElement;
                        if (activity.getType().contains("leisure")) {
                            if (!(inDistrict(allZones,activity.getCoord()).equals("noZone"))) {
                                List<Coord> coords = newLeisureFacilities.get(inDistrict(allZones, activity.getCoord()));
                                if (coords != null) {
                                    activity.setCoord(coords.get(new Random().nextInt(coords.size())));
                                }
                            }
                        }
                        if (activity.getType().contains("shopping")) {
                            if (!(inDistrict(allZones,activity.getCoord()).equals("noZone"))) {
                                List<Coord> coords = newShoppingFacilities.get(inDistrict(allZones, activity.getCoord()));
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

    private static Map<String, List<Coord>> facilitiesToZone(ActivityFacilities activityFacilities,  Map<String, Geometry> allZones, String matsimActivity) {
        Map<String, List<Coord>> newFacilities = new HashMap<>();
        for (ActivityFacility activityFacility : activityFacilities.getFacilities().values()) {
            Coord coord = activityFacility.getCoord();
            for (ActivityOption activityOption : activityFacility.getActivityOptions().values()) {
                if (activityOption.getType().contains(matsimActivity)) {
                    String name = inDistrict(allZones, coord);
                    if (name.equals("noZone")) {
                        log.warn("no Zone found for Activity " + activityFacility);
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

}
