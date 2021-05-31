package playground.lu.inputPlanPreparation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

public class CongestionTestingPlanGenerator {
    private static final Logger log = Logger.getLogger(CongestionTestingPlanGenerator.class);

    private final static String INPUT_CONFIG_FILE = "/Users/luchengqi/Documents/MATSimScenarios/Mielec/config.xml";
    private final static String OUTPUT_PATH = "/Users/luchengqi/Documents/MATSimScenarios/Mielec/drt.plans.xml";
    private final static int NUMBER_OF_TRIPS = 10000;

    public static void main(String[] args) {
        Random rnd = new Random(1234);

        Config config = ConfigUtils.loadConfig(INPUT_CONFIG_FILE);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Network network = scenario.getNetwork();
        List<Id<Link>> linkIds = new ArrayList<>();
        linkIds.addAll(network.getLinks().keySet());

        int networkSize = network.getLinks().size();

        Scenario outputScenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
        PopulationFactory populationFactory = outputScenario.getPopulation().getFactory();
        Population outputPlans = outputScenario.getPopulation();

        for (int i = 0; i < NUMBER_OF_TRIPS; i++) {
            Person dummyPerson = populationFactory
                    .createPerson(Id.create("dummy_person_" + Integer.toString(i), Person.class));
            Plan dummyPlan = populationFactory.createPlan();
            Activity act0 = populationFactory.createActivityFromLinkId
                    ("dummy", linkIds.get(rnd.nextInt(networkSize)));
            act0.setEndTime(21600 + rnd.nextInt(3600));
            Leg leg = populationFactory.createLeg("drt");
            Activity act1 = populationFactory.createActivityFromLinkId
                    ("dummy", linkIds.get(rnd.nextInt(networkSize)));
            dummyPlan.addActivity(act0);
            dummyPlan.addLeg(leg);
            dummyPlan.addActivity(act1);
            dummyPerson.addPlan(dummyPlan);
            outputPlans.addPerson(dummyPerson);
        }

        log.info("Writing population...");
        new PopulationWriter(outputScenario.getPopulation()).write(OUTPUT_PATH);
        log.info("Writing population... Done.");
    }
}
