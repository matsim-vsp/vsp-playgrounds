package playground.gleich.analysis.pt;

import org.junit.Assert;
import org.junit.Before;
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

    private String networkFile;
    private String inScheduleFile;
    private String inTransitVehicleFile;
    private String eventsFile;
    private String shapeFile;
    private String coordRefSystem;
    private String minibusIdentifier;
    private double costPerHour;
    private double costPerKm;
    private double costPerDayFixVeh;

    private Scenario scenario;

    @Before
    public void prepareTestScenario() {
        networkFile = "src/main/java/playground/gleich/analysis/pt/output/output_network.xml.gz";
        inScheduleFile = "src/main/java/playground/gleich/analysis/pt/output/output_transitSchedule.xml.gz";
        inTransitVehicleFile = "src/main/java/playground/gleich/analysis/pt/output/output_transitVehicles.xml.gz";
        eventsFile = "src/main/java/playground/gleich/analysis/pt/output/output_events.xml.gz";
        shapeFile = "src/main/java/playground/gleich/analysis/pt/input/boxShapeFile/box.shp";
        coordRefSystem = "epsg:3857";
        minibusIdentifier = "";

        costPerHour = 1;
        costPerKm = 1;
        costPerDayFixVeh = 1;

        Config config = ConfigUtils.loadConfig("src/main/java/playground/gleich/analysis/pt/input/config.xml");
        config.controler().setOutputDirectory("src/main/java/playground/gleich/analysis/pt/output");
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

        scenario = ScenarioUtils.loadScenario(config);

    }

    /**
     * Tests the kmDriven and numVehUsed. This should depend solely on the transitSchedule and not on
     * the population. In this case, the population is empty. There are two trains on the line
     * and the following calculates the kmDriven
     * (44) --> (45) --> 56 --> 66
     *        <-- 54 <-- 65 <--/
     * 5 * (2400 + 100 + 2400 + 1200) / 1000 = 30.5 km
     * 30.5 km / 2 trains = 15.25 km/veh/day
     * Note: (xx) --> link is not counted
     */

    @Test
    public void testKmDrivenEmptyPopulation() {
        Controler controler = new Controler(scenario);
        controler.run();

        CalculatePtOperatingCostsFromEvents costCalculator = new CalculatePtOperatingCostsFromEvents(networkFile, inScheduleFile, inTransitVehicleFile, coordRefSystem, minibusIdentifier);
        costCalculator.run(eventsFile, shapeFile, costPerHour, costPerKm, costPerDayFixVeh);

        Assert.assertEquals(30.5, costCalculator.getKmDriven(),0.);
        Assert.assertEquals(2.0, costCalculator.getNumVehUsed(),0.);
    }

    /**
     * Also tests the kmDriven and numVehUsed. This time, there is an agent in the population who
     * rides through the zone. However, this should not impact the kmDriven. Therefore, the
     * expected values are the same as in the last test.
     */
    @Test
    public void testKmDrivenWithPopulation() {

        Person person = makePerson(6 * 3600.0,
                new Coord(1050.0, 1050.0),
                new Coord(4000.0, 3000.0),
                "0",
                scenario.getPopulation().getFactory());

        scenario.getPopulation().addPerson(person);

        Controler controler = new Controler(scenario);
        controler.run();

        CalculatePtOperatingCostsFromEvents costCalculator = new CalculatePtOperatingCostsFromEvents(networkFile, inScheduleFile, inTransitVehicleFile, coordRefSystem, minibusIdentifier);
        costCalculator.run(eventsFile, shapeFile, costPerHour, costPerKm, costPerDayFixVeh);

        Assert.assertEquals(30.5, costCalculator.getKmDriven(),0.);
        Assert.assertEquals(2.0, costCalculator.getNumVehUsed(),0.);

    }


    /**
     * Tests pkm when agent takes pt route that never enters the zone. Thefore, the expected
     * value for pkm is 0.0
     */

    @Test
    public void testOutsideZone() {

        Person person = makePerson(6 * 3600.0,
                new Coord(1050.0, 1050.0),
                new Coord(4000.0, 1000.0),
                "0",
                scenario.getPopulation().getFactory());

        scenario.getPopulation().addPerson(person);

        Controler controler = new Controler(scenario);
        controler.run();

        CalculatePtOperatingCostsFromEvents costCalculator = new CalculatePtOperatingCostsFromEvents(networkFile, inScheduleFile, inTransitVehicleFile, coordRefSystem, minibusIdentifier);
        costCalculator.run(eventsFile, shapeFile, costPerHour, costPerKm, costPerDayFixVeh);

        Assert.assertEquals(0., costCalculator.getPkm(),0.);
    }



    /**
     * Tests when agent goes Left-->Right on the horizontal line. That means that the pt
     * trip starts outside the zone and is later inside of the zone. The link that is
     * partially wihtin the zone should NOT be counted in this case.
     * person0: links h--> (44) --> (45) --> 56 --> 66 --> w
     *         = 2400 + 100 = 2.5 pkm
     * Note: (xx) --> link is not counted
     */
    @Test
    public void testEnterNodeOutsideZone() {

        Person person = makePerson(6 * 3600.0,
                new Coord(1000.0, 3000.0),
                new Coord(4000.0, 3000.0),
                "0",
                scenario.getPopulation().getFactory());

        scenario.getPopulation().addPerson(person);

        Controler controler = new Controler(scenario);
        controler.run();

        CalculatePtOperatingCostsFromEvents costCalculator = new CalculatePtOperatingCostsFromEvents(networkFile, inScheduleFile, inTransitVehicleFile, coordRefSystem, minibusIdentifier);
        costCalculator.run(eventsFile, shapeFile, costPerHour, costPerKm, costPerDayFixVeh);

        Assert.assertEquals(2.5, costCalculator.getPkm(),0.);
    }


    /**
     * Tests when agent goes Right-->Left on the horizontal line. That means that the pt
     * trip starts within the zone and is later outside of the zone. The link that is
     * partially wihtin the zone SHOULD be counted in this case.
     * person0: links h--> (66) --> 65 --> 54 --> (44) --> w
     *         = 2400 + 1200 = 3.6 pkm
     * Note: (xx) --> link is not counted
     */
    @Test
    public void testEnterNodeWithinZone() {

        Person person = makePerson(6 * 3600.0,
                new Coord(4000.0, 3000.0),
                new Coord(1000.0, 3000.0),
                "0",
                scenario.getPopulation().getFactory());

        scenario.getPopulation().addPerson(person);

        Controler controler = new Controler(scenario);
        controler.run();

        CalculatePtOperatingCostsFromEvents costCalculator = new CalculatePtOperatingCostsFromEvents(networkFile, inScheduleFile, inTransitVehicleFile, coordRefSystem, minibusIdentifier);
        costCalculator.run(eventsFile, shapeFile, costPerHour, costPerKm, costPerDayFixVeh);

        Assert.assertEquals(3.6, costCalculator.getPkm(),0.);
    }


    /**
     * Tests accuracy of pkm when agent first uses pt route outside of zone
     * and then transfers to a route within the zone
     * Following is the calculation of expected pkm:
     * person0: links h --> (12) --> xfer --> (45) --> 56 --> 66
     *         = 2400 + 100 = 2.5 pkm
     * Note: (xx) --> link is not counted
     */
    @Test
    public void testWithTransfer() {

        Person person = makePerson(6 * 3600.0,
                new Coord(1050.0, 1050.0),
                new Coord(4000.0, 3000.0),
                "0",
                scenario.getPopulation().getFactory());

        scenario.getPopulation().addPerson(person);

        Controler controler = new Controler(scenario);
        controler.run();

        CalculatePtOperatingCostsFromEvents costCalculator = new CalculatePtOperatingCostsFromEvents(networkFile, inScheduleFile, inTransitVehicleFile, coordRefSystem, minibusIdentifier);
        costCalculator.run(eventsFile, shapeFile, costPerHour, costPerKm, costPerDayFixVeh);

        Assert.assertEquals(2.5, costCalculator.getPkm(),0.);

    }

    /**
     * Tests the additive nature of pkm by letting 3 agents traverse network simultaneously
     * Following is the calculation of expected pkm:
     *
     * person0: links h --> (12) --> xfer --> (45) --> 56 --> 66
     *         = 2400 + 100 = 2.5 pkm
     * person1: links h--> (66) --> 65 --> 54 --> (44) --> w
     *         = 2400 + 1200 = 3.6 pkm
     * person2: links h--> (44) --> (45) --> 56 --> 66 --> w
     *         = 2400 + 100 = 2.5 pkm
     * total   = 8.6 pkm
     * Note: (xx) --> link is not counted
     */
    @Test
    public void testThreeAgents() {

        Person person0 = makePerson(6 * 3600.0,
                new Coord(1000.0, 1000.0),
                new Coord(4000.0, 3000.0),
                "0",
                scenario.getPopulation().getFactory());

        Person person1 = makePerson(6 * 3600.0,
                new Coord(4000.0, 3000.0),
                new Coord(1000.0, 3000.0),
                "1",
                scenario.getPopulation().getFactory());
        Person person2 = makePerson(6 * 3600.0,
                new Coord(1000.0, 3000.0),
                new Coord(4000.0, 3000.0),
                "2",
                scenario.getPopulation().getFactory());

        scenario.getPopulation().addPerson(person0);
        scenario.getPopulation().addPerson(person1);
        scenario.getPopulation().addPerson(person2);

        Controler controler = new Controler(scenario);
        controler.run();

        CalculatePtOperatingCostsFromEvents costCalculator = new CalculatePtOperatingCostsFromEvents(networkFile, inScheduleFile, inTransitVehicleFile, coordRefSystem, minibusIdentifier);
        costCalculator.run(eventsFile, shapeFile, costPerHour, costPerKm, costPerDayFixVeh);

        Assert.assertEquals(8.6, costCalculator.getPkm(), 0.);

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

        person.addPlan(plan);
        return person;
    }


}
