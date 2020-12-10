package playground.gleich.analysis.pt;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
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
import org.matsim.testcases.MatsimTestUtils;

public class CalculatePtOperatingCostsFromEventsTest {

    private String networkFile;
    private String transitScheduleFile;
    private String transitVehicleFile;
    private String eventsFile;
    private String shapeFile;
    private String coordRefSystem;
    private String minibusIdentifier;
    private double costPerHour;
    private double costPerKm;
    private double costPerDayFixVeh;

    private Scenario scenario;

    @Rule
    public MatsimTestUtils testUtils = new MatsimTestUtils();

    @Before
    public void prepareTestScenario() {
        Config config = ConfigUtils.loadConfig(testUtils.getClassInputDirectory() + "config.xml");
        config.controler().setOutputDirectory(testUtils.getOutputDirectory());
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);


        scenario = ScenarioUtils.loadScenario(config);

        shapeFile = testUtils.getClassInputDirectory() + "boxShapeFile/box.shp";

        networkFile = testUtils.getOutputDirectory() + "output_network.xml.gz";
        transitScheduleFile = testUtils.getOutputDirectory() + "output_transitSchedule.xml.gz";
        transitVehicleFile = testUtils.getOutputDirectory() + "output_transitVehicles.xml.gz";
        eventsFile = testUtils.getOutputDirectory() + "output_events.xml.gz";
        coordRefSystem = "epsg:3857";
        minibusIdentifier = "";

        costPerHour = 1.;
        costPerKm = 1.;
        costPerDayFixVeh = 1.;

    }

    /**
     * Tests calculations of kmDriven, numVehUsed, hoursDriven, and totalCost.
     * This should depend solely on the transitSchedule and not on the population.
     * In this case, the population is empty. There are two trains on the line
     * and the following calculates the kmDriven:
     * (44) --> (45) --> 56 --> 66
     *        <-- 54 <-- 65 <--/
     * 2 * (2400 + 100 + 2400 + 1200) / 1000 = 12.2 km
     * 12.2 km / 2 trains = 6.1 km/veh/day
     * Note: (xx) --> link is not counted
     *
     * HoursDriven Calculation:
     * 4-6 = 200s (link56) + 1s (node) + 1s (stop5a) = 202s
     * 6-4 = 200s (link65) + 1s (node) + 100s (link54) + 1s (node) + 1s (stop5b) = 303s
     * Total = 505s * 2 departures = 1010s (with delta of 5s, in case extra time is needed for passenger to enter or exit a vehicle)
     */

    @Test
    public void testPopulationIndependentVariablesEmptyPopulation() {
        Controler controler = new Controler(scenario);
        controler.run();

        CalculatePtOperatingCostsFromEvents costCalculator = new CalculatePtOperatingCostsFromEvents(networkFile, transitScheduleFile, transitVehicleFile, coordRefSystem, minibusIdentifier);
        costCalculator.run(eventsFile, shapeFile, costPerHour, costPerKm, costPerDayFixVeh);

        Assert.assertEquals(12.2, costCalculator.getKmDriven(),0.);
        Assert.assertEquals(2.0, costCalculator.getNumVehUsed(),0.);
        Assert.assertEquals(1010., costCalculator.getHoursDriven() * 3600, MatsimTestUtils.EPSILON);
        Assert.assertEquals(12.2 * 1. + 2 * 1. + 1010. / 3600, costCalculator.getTotalCost(), MatsimTestUtils.EPSILON);

    }

    /**
     * Tests calculations of kmDriven, numVehUsed, hoursDriven, and totalCost.
     * This time, there is an agent in the population who rides through the zone.
     * However, this should not impact the kmDriven the values; therefore, the
     * expected values are the same as in the last test.
     */
    @Test
    public void testPopulationIndependentVariablesWithPopulation() {

        Person person = makePerson(6 * 3600.0 - 5 * 60,
                new Coord(1050.0, 1050.0),
                new Coord(4000.0, 3000.0),
                "0",
                scenario.getPopulation().getFactory());

        scenario.getPopulation().addPerson(person);

        Controler controler = new Controler(scenario);
        controler.run();

        CalculatePtOperatingCostsFromEvents costCalculator = new CalculatePtOperatingCostsFromEvents(networkFile, transitScheduleFile, transitVehicleFile, coordRefSystem, minibusIdentifier);
        costCalculator.run(eventsFile, shapeFile, costPerHour, costPerKm, costPerDayFixVeh);

        Assert.assertEquals(12.2, costCalculator.getKmDriven(),0.);
        Assert.assertEquals(2.0, costCalculator.getNumVehUsed(),0.);
        Assert.assertEquals(1010., costCalculator.getHoursDriven() * 3600, 4.);
        Assert.assertEquals(12.2 * 1. + 2 * 1. + 1010. / 3600, costCalculator.getTotalCost(), 4 / 3600.);

    }


    /**
     * Tests pkm when agent takes pt route that never enters the zone. Thefore, the expected
     * value for pkm is 0.0
     */

    @Test
    public void testPkmOutsideZone() {

        Person person = makePerson(6 * 3600.0 - 5 * 60,
                new Coord(1050.0, 1050.0),
                new Coord(4000.0, 1000.0),
                "0",
                scenario.getPopulation().getFactory());

        scenario.getPopulation().addPerson(person);

        Controler controler = new Controler(scenario);
        controler.run();

        CalculatePtOperatingCostsFromEvents costCalculator = new CalculatePtOperatingCostsFromEvents(networkFile, transitScheduleFile, transitVehicleFile, coordRefSystem, minibusIdentifier);
        costCalculator.run(eventsFile, shapeFile, costPerHour, costPerKm, costPerDayFixVeh);

        Assert.assertEquals(0., costCalculator.getPkm(),0.);
    }



    /**
     * Tests when agent goes Left-->Right (Stop 4-->6) on the horizontal line. That means that the pt
     * trip starts outside the zone and is later inside of the zone. The link that is
     * partially within the zone should NOT be counted in this case.
     * person0: links h--> (44) --> (45) --> 56 --> 66 --> w
     *         = 2400 + 100 = 2.5 pkm
     * Note: (xx) --> link is not counted
     */
    @Test
    public void testPkmEnterNodeOutsideZone() {

        Person person = makePerson(6 * 3600.0 - 5 * 60,
                new Coord(1000.0, 3000.0),
                new Coord(4000.0, 3000.0),
                "0",
                scenario.getPopulation().getFactory());

        scenario.getPopulation().addPerson(person);

        Controler controler = new Controler(scenario);
        controler.run();

        CalculatePtOperatingCostsFromEvents costCalculator = new CalculatePtOperatingCostsFromEvents(networkFile, transitScheduleFile, transitVehicleFile, coordRefSystem, minibusIdentifier);
        costCalculator.run(eventsFile, shapeFile, costPerHour, costPerKm, costPerDayFixVeh);

        Assert.assertEquals(2.5, costCalculator.getPkm(),0.);
    }


    /**
     * Tests when agent goes Right-->Left (Stop 6-->4) on the horizontal line. That means that the pt
     * trip starts within the zone and is later outside of the zone. The link that is
     * partially within the zone SHOULD be counted in this case.
     * person0: links h--> (66) --> 65 --> 54 --> (44) --> w
     *         = 2400 + 1200 = 3.6 pkm
     * Note: (xx) --> link is not counted
     */
    @Test
    public void testPkmEnterNodeWithinZone() {

        Person person = makePerson(6 * 3600.0 - 5 * 60,
                new Coord(4000.0, 3000.0),
                new Coord(1000.0, 3000.0),
                "0",
                scenario.getPopulation().getFactory());

        scenario.getPopulation().addPerson(person);

        Controler controler = new Controler(scenario);
        controler.run();

        CalculatePtOperatingCostsFromEvents costCalculator = new CalculatePtOperatingCostsFromEvents(networkFile, transitScheduleFile, transitVehicleFile, coordRefSystem, minibusIdentifier);
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
    public void testPkmWithTransfer() {

        Person person = makePerson(6 * 3600.0 - 5 * 60,
                new Coord(1050.0, 1050.0),
                new Coord(4000.0, 3000.0),
                "0",
                scenario.getPopulation().getFactory());

        scenario.getPopulation().addPerson(person);

        Controler controler = new Controler(scenario);
        controler.run();

        CalculatePtOperatingCostsFromEvents costCalculator = new CalculatePtOperatingCostsFromEvents(networkFile, transitScheduleFile, transitVehicleFile, coordRefSystem, minibusIdentifier);
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
    public void testPkmThreeAgents() {

        Person person0 = makePerson(6 * 3600.0 - 5 * 60,
                new Coord(1000.0, 1000.0),
                new Coord(4000.0, 3000.0),
                "0",
                scenario.getPopulation().getFactory());

        Person person1 = makePerson(6 * 3600.0 - 5 * 60,
                new Coord(4000.0, 3000.0),
                new Coord(1000.0, 3000.0),
                "1",
                scenario.getPopulation().getFactory());
        Person person2 = makePerson(6 * 3600.0 - 5 * 60,
                new Coord(1000.0, 3000.0),
                new Coord(4000.0, 3000.0),
                "2",
                scenario.getPopulation().getFactory());

        scenario.getPopulation().addPerson(person0);
        scenario.getPopulation().addPerson(person1);
        scenario.getPopulation().addPerson(person2);

        Controler controler = new Controler(scenario);
        controler.run();

        CalculatePtOperatingCostsFromEvents costCalculator = new CalculatePtOperatingCostsFromEvents(networkFile, transitScheduleFile, transitVehicleFile, coordRefSystem, minibusIdentifier);
        costCalculator.run(eventsFile, shapeFile, costPerHour, costPerKm, costPerDayFixVeh);

        Assert.assertEquals(8.6, costCalculator.getPkm(), 0.);

    }

    private Person makePerson(double depTime, Coord act1Coord, Coord act2Coord, String pId, PopulationFactory pf) {
        Person person = pf.createPerson(Id.createPersonId(pId));
        Plan plan = pf.createPlan();
        Activity activity1 = pf.createActivityFromCoord("dummy", act1Coord);
        activity1.setEndTime(depTime);
        plan.addActivity(activity1);
        plan.addLeg(pf.createLeg("pt"));
        Activity activity2 = pf.createActivityFromCoord("dummy", act2Coord);
        activity2.setEndTimeUndefined();
        plan.addActivity(activity2);

        person.addPlan(plan);
        return person;
    }


}
