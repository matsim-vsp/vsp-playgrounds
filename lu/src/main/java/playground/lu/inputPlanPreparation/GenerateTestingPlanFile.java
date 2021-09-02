package playground.lu.inputPlanPreparation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import playground.lu.readShapeFile.ShapeFileReadingUtils;

public class GenerateTestingPlanFile {
    private static final Logger log = Logger.getLogger(GenerateTestingPlanFile.class);

    private final static String INPUT_PLAN_FILE = "/Users/luchengqi/Desktop/Homework2-package/Task3-Scenario/cb-drtplans.xml.gz";
    private final static String OUTPUT_PATH = "/Users/luchengqi/Desktop/Homework2-package/Task3-Scenario/plans.xml.gz";
    private final static String NETWORK = "/Users/luchengqi/Desktop/Homework2-package/Task3-Scenario/network.xml.gz";
//    private final static String CRS = "EPSG:25833";
    private final static String INNER_CITY = "/Users/luchengqi/Documents/MATSimScenarios/Cottbus/shapeFiles/inner-city.shp";
    private final static String OUTSKIRT = "/Users/luchengqi/Documents/MATSimScenarios/Cottbus/shapeFiles/outskirt.shp";

    private final static int TO_GENERATE = 2400;
    private final static double START_TIME_MORNING = 18000;
    private final static int TIME_WINDOW = 27000;
    private final static int WORKING_HOURS_BASE = 14400;

    public static void main(String[] args) {
        int counter = 0;
        Random rnd = new Random(5678);

        Config config = ConfigUtils.createConfig();
        config.plans().setInputFile(INPUT_PLAN_FILE);
        config.network().setInputFile(NETWORK);
//        config.global().setCoordinateSystem(CRS);
        Scenario scenario = ScenarioUtils.loadScenario(config);

        Population population = scenario.getPopulation();
        PopulationFactory populationFactory = population.getFactory();

        Network network = scenario.getNetwork();
        Geometry city = ShapeFileReadingUtils.getGeometryFromShapeFile(INNER_CITY);
        Geometry outskirt = ShapeFileReadingUtils.getGeometryFromShapeFile(OUTSKIRT);
        List<Link> linksInCity = new ArrayList<>();
        List<Link> linksInOutskirt = new ArrayList<>();


        for (Link link : network.getLinks().values()) {
            if (ShapeFileReadingUtils.isLinkWithinGeometry(link, city)) {
                linksInCity.add(link);
            }
            if (ShapeFileReadingUtils.isLinkWithinGeometry(link, outskirt)) {
                linksInOutskirt.add(link);
            }
        }

        int numLinksInCity = linksInCity.size();
        int numLinksInOutskirt = linksInOutskirt.size();

        while (counter < TO_GENERATE) {
            int home = rnd.nextInt(numLinksInOutskirt);
            int work = rnd.nextInt(numLinksInCity);
            Activity act0 = populationFactory.createActivityFromLinkId("home", linksInOutskirt.get(home).getId());
            act0.setCoord(linksInOutskirt.get(home).getCoord());
            double act0EndTime = START_TIME_MORNING + rnd.nextInt(TIME_WINDOW);
            act0.setEndTime(act0EndTime);
            Leg leg0 = populationFactory.createLeg("drt");
            Activity act1 = populationFactory.createActivityFromLinkId("work", linksInCity.get(work).getId());
            act1.setCoord(linksInCity.get(work).getCoord());
            double act1EndTime = act0EndTime + WORKING_HOURS_BASE + rnd.nextInt(21600);
            act1.setEndTime(act1EndTime);
            Leg leg1 = populationFactory.createLeg("drt");
            Activity act2 = populationFactory.createActivityFromLinkId("home", linksInOutskirt.get(home).getId());
            act2.setCoord(linksInOutskirt.get(home).getCoord());

            Plan plan = populationFactory.createPlan();
            Person person = populationFactory.createPerson(Id.createPersonId("additional_" + counter));
            plan.addActivity(act0);
            plan.addLeg(leg0);
            plan.addActivity(act1);
            plan.addLeg(leg1);
            plan.addActivity(act2);
            person.addPlan(plan);
            population.addPerson(person);

            counter += 1;
        }

        log.info("Writing population...");
        new PopulationWriter(population).write(OUTPUT_PATH);
        log.info("Writing population... Done.");
    }
}
