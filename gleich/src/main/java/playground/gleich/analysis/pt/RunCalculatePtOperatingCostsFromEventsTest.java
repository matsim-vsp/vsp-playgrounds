package playground.gleich.analysis.pt;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;

public class RunCalculatePtOperatingCostsFromEventsTest {



    // Link (44) --> (45) --> 56 --> 66
    //         <-- 54 <-- 65 <--/
    // 50 * (2400 + 100 + 2400 + 1200) / 1000 = 305 km
    // 305 km / 2 trains = 185 km/veh/day

    @Test
    public void testKmDrivenEmptyPopulation() {
        Config config = ConfigUtils.loadConfig("src/main/java/playground/gleich/analysis/pt/input/config.xml");
        config.controler().setOutputDirectory("src/main/java/playground/gleich/analysis/pt/output");
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

        Scenario scenario = ScenarioUtils.loadScenario(config) ;

        Controler controler = new Controler(scenario);
        controler.run();

        String networkFile = "src/main/java/playground/gleich/analysis/pt/output/output_network.xml.gz";
        String inScheduleFile = "src/main/java/playground/gleich/analysis/pt/output/output_transitSchedule.xml.gz";
        String inTransitVehicleFile = "src/main/java/playground/gleich/analysis/pt/output/output_transitVehicles.xml.gz";
        String eventsFile = "src/main/java/playground/gleich/analysis/pt/output/output_events.xml.gz";
        String shapeFile = "src/main/java/playground/gleich/analysis/pt/input/boxShapeFile/box.shp";


        String coordRefSystem = "epsg:3857";
        String minibusIdentifier = "";

        double costPerHour = 1;
        double costPerKm = 1;
        double costPerDayFixVeh = 1;

        CalculatePtOperatingCostsFromEvents costCalculator = new CalculatePtOperatingCostsFromEvents(networkFile, inScheduleFile, inTransitVehicleFile, coordRefSystem, minibusIdentifier);
        costCalculator.run(eventsFile, shapeFile, costPerHour, costPerKm, costPerDayFixVeh);

        Assert.assertEquals(305.0, costCalculator.getKmDriven(),0.);
        Assert.assertEquals(2.0, costCalculator.getNumVehUsed(),0.);
    }

    @Test
    public void testKmDrivenWithPopulation() {
        Config config = ConfigUtils.loadConfig("src/main/java/playground/gleich/analysis/pt/input/config.xml");
        config.controler().setOutputDirectory("src/main/java/playground/gleich/analysis/pt/output");
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

        Scenario scenario = ScenarioUtils.loadScenario(config) ;

        Person person = makePerson(8 * 3600.0,
                new Coord(1050.0, 1050.0),
                new Coord(4000.0, 3000.0),
                "0",
                scenario.getPopulation().getFactory());

        scenario.getPopulation().addPerson(person);

        Controler controler = new Controler(scenario);
        controler.run();

        String networkFile = "src/main/java/playground/gleich/analysis/pt/output/output_network.xml.gz";
        String inScheduleFile = "src/main/java/playground/gleich/analysis/pt/output/output_transitSchedule.xml.gz";
        String inTransitVehicleFile = "src/main/java/playground/gleich/analysis/pt/output/output_transitVehicles.xml.gz";
        String eventsFile = "src/main/java/playground/gleich/analysis/pt/output/output_events.xml.gz";
        String shapeFile = "src/main/java/playground/gleich/analysis/pt/input/boxShapeFile/box.shp";


        String coordRefSystem = "epsg:3857";
        String minibusIdentifier = "";

        double costPerHour = 1;
        double costPerKm = 1;
        double costPerDayFixVeh = 1;

        CalculatePtOperatingCostsFromEvents costCalculator = new CalculatePtOperatingCostsFromEvents(networkFile, inScheduleFile, inTransitVehicleFile, coordRefSystem, minibusIdentifier);
        costCalculator.run(eventsFile, shapeFile, costPerHour, costPerKm, costPerDayFixVeh);

        Assert.assertEquals(305.0, costCalculator.getKmDriven(),0.);
        Assert.assertEquals(2.0, costCalculator.getNumVehUsed(),0.);

    }



    @Test
    public void testOutsideZone() {
        Config config = ConfigUtils.loadConfig("src/main/java/playground/gleich/analysis/pt/input/config.xml");
        config.controler().setOutputDirectory("src/main/java/playground/gleich/analysis/pt/output");
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

        Scenario scenario = ScenarioUtils.loadScenario(config) ;

        Person person = makePerson(8 * 3600.0,
                new Coord(1050.0, 1050.0),
                new Coord(4000.0, 1000.0),
                "0",
                scenario.getPopulation().getFactory());

        scenario.getPopulation().addPerson(person);

        Controler controler = new Controler(scenario);
        controler.run();

        String networkFile = "src/main/java/playground/gleich/analysis/pt/output/output_network.xml.gz";
        String inScheduleFile = "src/main/java/playground/gleich/analysis/pt/output/output_transitSchedule.xml.gz";
        String inTransitVehicleFile = "src/main/java/playground/gleich/analysis/pt/output/output_transitVehicles.xml.gz";
        String eventsFile = "src/main/java/playground/gleich/analysis/pt/output/output_events.xml.gz";
        String shapeFile = "src/main/java/playground/gleich/analysis/pt/input/boxShapeFile/box.shp";


        String coordRefSystem = "epsg:3857";
        String minibusIdentifier = "";

        double costPerHour = 1;
        double costPerKm = 1;
        double costPerDayFixVeh = 1;

        CalculatePtOperatingCostsFromEvents costCalculator = new CalculatePtOperatingCostsFromEvents(networkFile, inScheduleFile, inTransitVehicleFile, coordRefSystem, minibusIdentifier);
        costCalculator.run(eventsFile, shapeFile, costPerHour, costPerKm, costPerDayFixVeh);

        Assert.assertEquals(0., costCalculator.getPkm(),0.); // 0.0 pkm
    }


    @Test
    public void testEnterNodeOutsideZone() {
        Config config = ConfigUtils.loadConfig("src/main/java/playground/gleich/analysis/pt/input/config.xml");
        config.controler().setOutputDirectory("src/main/java/playground/gleich/analysis/pt/output");
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

        Scenario scenario = ScenarioUtils.loadScenario(config) ;

        Person person = makePerson(8 * 3600.0,
                new Coord(1000.0, 3000.0),
                new Coord(4000.0, 3000.0),
                "0",
                scenario.getPopulation().getFactory());

        scenario.getPopulation().addPerson(person);

        Controler controler = new Controler(scenario);
        controler.run();

        String networkFile = "src/main/java/playground/gleich/analysis/pt/output/output_network.xml.gz";
        String inScheduleFile = "src/main/java/playground/gleich/analysis/pt/output/output_transitSchedule.xml.gz";
        String inTransitVehicleFile = "src/main/java/playground/gleich/analysis/pt/output/output_transitVehicles.xml.gz";
        String eventsFile = "src/main/java/playground/gleich/analysis/pt/output/output_events.xml.gz";
        String shapeFile = "src/main/java/playground/gleich/analysis/pt/input/boxShapeFile/box.shp";


        String coordRefSystem = "epsg:3857";
        String minibusIdentifier = "";

        double costPerHour = 1;
        double costPerKm = 1;
        double costPerDayFixVeh = 1;

        CalculatePtOperatingCostsFromEvents costCalculator = new CalculatePtOperatingCostsFromEvents(networkFile, inScheduleFile, inTransitVehicleFile, coordRefSystem, minibusIdentifier);
        costCalculator.run(eventsFile, shapeFile, costPerHour, costPerKm, costPerDayFixVeh);


        // h--> (44) --> (45) --> 56 --> 66 --> w
        //                       2400 +  100 +     = 2.5 pkm

        Assert.assertEquals(2.5, costCalculator.getPkm(),0.);
    }

    @Test
    public void testEnterNodeWithinZone() {
        Config config = ConfigUtils.loadConfig("src/main/java/playground/gleich/analysis/pt/input/config.xml");
        config.controler().setOutputDirectory("src/main/java/playground/gleich/analysis/pt/output");
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

        Scenario scenario = ScenarioUtils.loadScenario(config) ;

        Person person = makePerson(8 * 3600.0,
                new Coord(4000.0, 3000.0),
                new Coord(1000.0, 3000.0),
                "0",
                scenario.getPopulation().getFactory());

        scenario.getPopulation().addPerson(person);

        Controler controler = new Controler(scenario);
        controler.run();

        String networkFile = "src/main/java/playground/gleich/analysis/pt/output/output_network.xml.gz";
        String inScheduleFile = "src/main/java/playground/gleich/analysis/pt/output/output_transitSchedule.xml.gz";
        String inTransitVehicleFile = "src/main/java/playground/gleich/analysis/pt/output/output_transitVehicles.xml.gz";
        String eventsFile = "src/main/java/playground/gleich/analysis/pt/output/output_events.xml.gz";
        String shapeFile = "src/main/java/playground/gleich/analysis/pt/input/boxShapeFile/box.shp";


        String coordRefSystem = "epsg:3857";
        String minibusIdentifier = "";

        double costPerHour = 1;
        double costPerKm = 1;
        double costPerDayFixVeh = 1;

        CalculatePtOperatingCostsFromEvents costCalculator = new CalculatePtOperatingCostsFromEvents(networkFile, inScheduleFile, inTransitVehicleFile, coordRefSystem, minibusIdentifier);
        costCalculator.run(eventsFile, shapeFile, costPerHour, costPerKm, costPerDayFixVeh);

        Assert.assertEquals(305.0, costCalculator.getKmDriven(),0.);
        Assert.assertEquals(2.0, costCalculator.getNumVehUsed(),0.);

        //  h--> (66) --> 65 --> 54 --> (44) --> w
        //               2400 + 1200                = 3.6 pkm

        Assert.assertEquals(3.6, costCalculator.getPkm(),0.);
    }


    @Test
    public void testWithTransfer() {
        Config config = ConfigUtils.loadConfig("src/main/java/playground/gleich/analysis/pt/input/config.xml");
        config.controler().setOutputDirectory("src/main/java/playground/gleich/analysis/pt/output");
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

        Scenario scenario = ScenarioUtils.loadScenario(config) ;

        Person person = makePerson(8 * 3600.0,
                new Coord(1050.0, 1050.0),
                new Coord(4000.0, 3000.0),
                "0",
                scenario.getPopulation().getFactory());

        scenario.getPopulation().addPerson(person);

        Controler controler = new Controler(scenario);
        controler.run();

        String networkFile = "src/main/java/playground/gleich/analysis/pt/output/output_network.xml.gz";
        String inScheduleFile = "src/main/java/playground/gleich/analysis/pt/output/output_transitSchedule.xml.gz";
        String inTransitVehicleFile = "src/main/java/playground/gleich/analysis/pt/output/output_transitVehicles.xml.gz";
        String eventsFile = "src/main/java/playground/gleich/analysis/pt/output/output_events.xml.gz";
        String shapeFile = "src/main/java/playground/gleich/analysis/pt/input/boxShapeFile/box.shp";


        String coordRefSystem = "epsg:3857";
        String minibusIdentifier = "";

        double costPerHour = 1;
        double costPerKm = 1;
        double costPerDayFixVeh = 1;

        CalculatePtOperatingCostsFromEvents costCalculator = new CalculatePtOperatingCostsFromEvents(networkFile, inScheduleFile, inTransitVehicleFile, coordRefSystem, minibusIdentifier);
        costCalculator.run(eventsFile, shapeFile, costPerHour, costPerKm, costPerDayFixVeh);

        // h--> (45) --> 56 --> 66 --> w
        //              2400 +  100 +        = 2.5 pkm

        Assert.assertEquals(2.5, costCalculator.getPkm(),0.);




    }

    @Test
    public void testTwoPlans() {

        Config config = ConfigUtils.loadConfig("src/main/java/playground/gleich/analysis/pt/input/config.xml");
        config.controler().setOutputDirectory("src/main/java/playground/gleich/analysis/pt/output");
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

        Scenario scenario = ScenarioUtils.loadScenario(config) ;

        Person person0 = makePerson(8 * 3600.0,
                new Coord(1000.0, 1000.0),
                new Coord(4000.0, 3000.0),
                "0",
                scenario.getPopulation().getFactory());

        Person person1 = makePerson(8 * 3600.0,
                new Coord(1000.0, 1000.0),
                new Coord(4000.0, 3000.0),
                "1",
                scenario.getPopulation().getFactory());

        scenario.getPopulation().addPerson(person0);
        scenario.getPopulation().addPerson(person1);

        Controler controler = new Controler(scenario);
        controler.run();

        String networkFile = "src/main/java/playground/gleich/analysis/pt/output/output_network.xml.gz";
        String inScheduleFile = "src/main/java/playground/gleich/analysis/pt/output/output_transitSchedule.xml.gz";
        String inTransitVehicleFile = "src/main/java/playground/gleich/analysis/pt/output/output_transitVehicles.xml.gz";
        String eventsFile = "src/main/java/playground/gleich/analysis/pt/output/output_events.xml.gz";
        String shapeFile = "src/main/java/playground/gleich/analysis/pt/input/boxShapeFile/box.shp";

        String coordRefSystem = "epsg:3857";
        String minibusIdentifier = "";

        double costPerHour = 1;
        double costPerKm = 1;
        double costPerDayFixVeh = 1;

        CalculatePtOperatingCostsFromEvents costCalculator = new CalculatePtOperatingCostsFromEvents(networkFile, inScheduleFile, inTransitVehicleFile, coordRefSystem, minibusIdentifier);
        costCalculator.run(eventsFile, shapeFile, costPerHour, costPerKm, costPerDayFixVeh);

        Assert.assertEquals(305.0, costCalculator.getKmDriven(), 0.);
        Assert.assertEquals(2.0, costCalculator.getNumVehUsed(), 0.);
        Assert.assertEquals(5.0, costCalculator.getPkm(), 0.);

    }

    private Person makePerson(double depTime, Coord homeCoord, Coord workCoord, String pId, PopulationFactory pf) {
        Id<Person> personId = Id.createPersonId(pId);
        Person person = pf.createPerson(personId);
        Plan plan = pf.createPlan();
        Activity activity1 = pf.createActivityFromCoord("h", homeCoord);
        activity1.setEndTime(depTime);
        plan.addActivity(activity1);
        plan.addLeg(pf.createLeg("pt"));
        Activity activity2 = pf.createActivityFromCoord("w", workCoord);
        activity2.setEndTime(16*3600.0);
        plan.addActivity(activity2);
//        plan.addLeg(pf.createLeg("pt"));
//        Activity activity3 = pf.createActivityFromCoord("h", homeCoord);
//        activity3.setEndTimeUndefined();
//        plan.addActivity(activity3);

        person.addPlan(plan);
        return person;
    }


}
