package playground.dziemke.utils;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

public class Events2ExperiancedPlansConverterTest {

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();

    private static boolean setupRun = false;

    private static Population plans;
    private static Population experiencedPlans;

    @Before
    public void convertSample() {

        if (!setupRun) {

            String baseDir = utils.getClassInputDirectory();
            String eventsFile = baseDir + "output_events.xml.gz";
            String inputNetworkFile = baseDir + "output_network.xml.gz";
            String inputPlansFile = baseDir + "output_plans.xml.gz";
            String outputExperiancedPlansFile = baseDir + "experiencedPlans.xml.gz";

            Config config = ConfigUtils.createConfig();
            config.network().setInputFile(inputNetworkFile);
            config.plans().setInputFile(inputPlansFile);
            (new Events2ExperiencedPlansConverter(config, eventsFile, outputExperiancedPlansFile)).convert();

            plans = readPopulationFromFile(inputPlansFile);
            experiencedPlans = readPopulationFromFile(outputExperiancedPlansFile);

            setupRun = true;
        }
    }

    @Test
    public void testNotNull() {

        Assert.assertNotNull(plans);
        Assert.assertNotNull(experiencedPlans);
    }

    /**
     * StayHome persons like person7 should occur but without an event there should not be any activity made by them.
     */
    @Test
    public void testStayHome() {

        Person person7 = experiencedPlans.getPersons().get(Id.create("7", Person.class));
        Assert.assertNotNull(person7);
        Assert.assertTrue(person7.getSelectedPlan().getPlanElements().size() == 0);
    }

    /**
     * Due to the information given by the events, there should only be the selected plan left for everyone.
     */
    @Test
    public void testOnlySelectedPlanLeft() {

        for (Person current : experiencedPlans.getPersons().values()) {

            Assert.assertNotNull(current.getSelectedPlan());
            Assert.assertTrue(current.getPlans().size() == 1);
        }
    }

    /**
     * The travelTime of person1's first leg should be 25min (and not the 8:59 from the output-plans).
     */
    @Test
    public void testTravelTime() {

        Person person1 = experiencedPlans.getPersons().get(Id.create("1", Person.class));
        Assert.assertNotNull(person1);
        Leg firstLeg = (Leg)person1.getSelectedPlan().getPlanElements().get(1);
		Assert.assertTrue(firstLeg.getTravelTime().seconds() == 25*60);
    }

    private static Population readPopulationFromFile(String populationFile) {

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new PopulationReader(scenario).readFile(populationFile);
        return scenario.getPopulation();
    }
}
