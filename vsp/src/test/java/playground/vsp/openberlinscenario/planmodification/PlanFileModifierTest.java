package playground.vsp.openberlinscenario.planmodification;

import org.apache.log4j.Logger;
import org.junit.*;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author gthunig on 08.05.2018
 */
public class PlanFileModifierTest {
    private final static Logger LOG = Logger.getLogger(PlanFileModifierTest.class);

//    private final static String ORIGINAL_INPUT_PLANS_FILE = "C:\\Users\\gthunig\\Desktop\\Vsp\\PlanFileModifierTest\\testPlans.xml";
    private final static String FORMATED_INPUT_PLANS_FILE = "C:\\Users\\gthunig\\Desktop\\Vsp\\PlanFileModifierTest\\testPlansFormated.xml";
    private final static String OUTPUT_PLANS_FILE = "C:\\Users\\gthunig\\Desktop\\Vsp\\PlanFileModifierTest\\testPlansModified.xml";
    private final static String OUTPUT_PLANS_FILE_2 = "C:\\Users\\gthunig\\Desktop\\Vsp\\PlanFileModifierTest\\testPlansModified2.xml";

    private static double SELECTION_PROBABILITY = 0.70;
    private static CoordinateTransformation ct = new IdentityTransformation();

    private static Population originalPopulationCase1;
    private static Population modifiedPopulationCase1;
    private static Population originalPopulationCase2;
    private static Population modifiedPopulationCase2;


//    public formatPlans() {
//        Population population = readPopulationFromFile("C:\\Users\\gthunig\\Desktop\\Vsp\\PlanFileModifierTest\\testPlansFormated.xml");
//
//        // Write population file
//        new PopulationWriter(population, null).write(FORMATED_INPUT_PLANS_FILE);
//        LOG.info("Modified plans file has been written to " + FORMATED_INPUT_PLANS_FILE);
//    }

    @BeforeClass
    public static void initializeTestPopulations() {
        PlanFileModifier planFileModifier = new PlanFileModifier(FORMATED_INPUT_PLANS_FILE, OUTPUT_PLANS_FILE,
                SELECTION_PROBABILITY, false, false,
                true, false,
                10000/* not tested*/, false, ct);
        planFileModifier.modifyPlans();

        originalPopulationCase1 = readPopulationFromFile(FORMATED_INPUT_PLANS_FILE);
        modifiedPopulationCase1 = readPopulationFromFile(OUTPUT_PLANS_FILE);

        PlanFileModifier planFileModifier2 = new PlanFileModifier(FORMATED_INPUT_PLANS_FILE, OUTPUT_PLANS_FILE_2,
                SELECTION_PROBABILITY, true, true,
                false, true, 10000,
                true, ct);
        planFileModifier2.modifyPlans();

        originalPopulationCase2 = readPopulationFromFile(FORMATED_INPUT_PLANS_FILE);
        modifiedPopulationCase2 = readPopulationFromFile(OUTPUT_PLANS_FILE_2);
    }

    @Test
    public void testSelectionProbability() {

        LOG.info("OriginalPopulationCase1 size: " + originalPopulationCase1.getPersons().size());
        LOG.info("ModifiedPopulationCase1 size: " + modifiedPopulationCase1.getPersons().size());
        LOG.info("selection probability: " + SELECTION_PROBABILITY);
        LOG.info("real selection probability: " +
                ((double)modifiedPopulationCase1.getPersons().size()/originalPopulationCase1.getPersons().size()));
        Assert.assertEquals("Selection probability was not correctly applied",
                11, modifiedPopulationCase1.getPersons().size(),
                MatsimTestUtils.EPSILON);
    }

    @Test
    public void testEveryPersonCopiedExistsInOriginal() {

        for (Person copy : modifiedPopulationCase1.getPersons().values()) {
            Person original = originalPopulationCase1.getPersons().get(copy.getId());
            Assert.assertTrue("Person " + copy.getId() + " does not exist in the original file",
                    original != null);
        }
    }

    @Test
    public void testOnlyTransferSelectedPlan() {

        for (Person copy : modifiedPopulationCase2.getPersons().values()) {
            Person original = originalPopulationCase2.getPersons().get(copy.getId());
            Assert.assertEquals("More than 1 plan", 1, copy.getPlans().size());
            comparePlansWithoutRoutes(original.getSelectedPlan(), copy.getSelectedPlan());
        }
    }

    @Test
    public void testNotOnlyTransferSelectedPlan() {
        //also tests if all plans were copied correctly

        for (Person copy : modifiedPopulationCase1.getPersons().values()) {
            Person original = originalPopulationCase1.getPersons().get(copy.getId());
            Assert.assertEquals("Not the same amount of plans",
                    original.getPlans().size(), copy.getPlans().size());
            comparePlans(original.getSelectedPlan(), copy.getSelectedPlan());
            for (int i = 0; i < original.getPlans().size(); i++) {
                Plan originalPlan = original.getPlans().get(i);
                Plan modifiedPlan = copy.getPlans().get(i);
                comparePlans(originalPlan, modifiedPlan);
            }
        }
    }

    @Test
    public void testConsiderHomeStayingAgents() {

        boolean atLeastOneHomeStayingPerson = false;
        for (Person copy : modifiedPopulationCase2.getPersons().values()) {
            if (copy.getSelectedPlan().getPlanElements().size() <= 1)
                atLeastOneHomeStayingPerson = true;
        }
        Assert.assertTrue("No home staying person found", atLeastOneHomeStayingPerson);
    }

    @Test
    public void testNotConsiderHomeStayingAgents() {

        for (Person copy : modifiedPopulationCase1.getPersons().values()) {
            Assert.assertTrue("No home staying agents allowed",
                    copy.getSelectedPlan().getPlanElements().size() > 1);
        }
    }

    @Test
    public void testIncludeStayHomePlans() {

        boolean atLeastOneHomeStayingPlan = false;
        for (Person copy : modifiedPopulationCase1.getPersons().values()) {
            for (Plan plan : copy.getPlans())
                if (plan.getPlanElements().size() <= 1)
                    atLeastOneHomeStayingPlan = true;
        }
        Assert.assertTrue("No home staying plan found", atLeastOneHomeStayingPlan);
    }

    @Test
    public void testNotIncludeStayHomePlans() {

        for (Person copy : modifiedPopulationCase2.getPersons().values()) {
            for (Plan plan : copy.getPlans())
                if (!plan.equals(copy.getSelectedPlan()))
                Assert.assertTrue("No home staying plans allowed",
                        plan.getPlanElements().size() > 1);
        }
    }

    @Test
    public void testOnlyConsiderPeopleAlwaysGoingByCar() {

        for (Person copy : modifiedPopulationCase2.getPersons().values()) {
            for (Plan plan : copy.getPlans())
                for (PlanElement planElement : plan.getPlanElements()) {
                    if (planElement instanceof Leg) {
                        Assert.assertTrue("No other mode than car allowed",
                                (((Leg) planElement).getMode().equals(TransportMode.car)));
                    }
                }
        }
    }

    @Test
    public void testNotOnlyConsiderPeopleAlwaysGoingByCar() {

        boolean otherModeThanCarConsidered = false;
        for (Person copy : modifiedPopulationCase1.getPersons().values()) {
            for (Plan plan : copy.getPlans())
                for (PlanElement planElement : plan.getPlanElements()) {
                    if (planElement instanceof Leg && !(((Leg) planElement).getMode().equals(TransportMode.car))) {
                        otherModeThanCarConsidered = true;
                    }
                }
        }
        Assert.assertTrue("There should be other modes than car", otherModeThanCarConsidered);
    }

    @Test
    public void testRemoveLinksAndRoutes() {

        for (Person copy : modifiedPopulationCase2.getPersons().values()) {
            for (Plan plan : copy.getPlans())
                for (PlanElement planElement : plan.getPlanElements()) {
                    if (planElement instanceof Leg) {
                        Assert.assertTrue("There should not be a route left",
                                (((Leg) planElement).getRoute() == null));
                    }
                }
        }
    }

    @Test
    public void testNotRemoveLinksAndRoutes() {

        boolean routeFound = false;
        for (Person copy : modifiedPopulationCase1.getPersons().values()) {
            for (Plan plan : copy.getPlans())
                for (PlanElement planElement : plan.getPlanElements()) {
                    if (planElement instanceof Leg && (((Leg) planElement).getRoute() != null)) {
                        routeFound = true;
                    }
                }
        }
        Assert.assertTrue("There should be at minimum one route left", routeFound);
    }

    private void comparePlans(Plan original, Plan copy) {

        Assert.assertEquals("Plans are not the same", original.toString(), copy.toString());
        for (int i = 0; i < original.getPlanElements().size(); i++) {
            Assert.assertEquals("PlanElements are not the same", original.getPlanElements().get(i).toString(),
                    copy.getPlanElements().get(i).toString());
        }
    }

    private void comparePlansWithoutRoutes(Plan original, Plan copy) {

        Assert.assertEquals("Plans are not the same", original.toString(), copy.toString());
    }

    private static Population readPopulationFromFile(String populationFile) {

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new PopulationReader(scenario).readFile(populationFile);
        return scenario.getPopulation();
    }
}
