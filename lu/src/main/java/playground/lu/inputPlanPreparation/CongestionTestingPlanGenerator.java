package playground.lu.inputPlanPreparation;

import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
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

	private final static String INPUT_CONFIG_FILE = "C:\\Users\\cluac\\MATSimScenarios\\CongestionTesting\\config.xml";
	private final static String OUTPUT_PATH = "C:\\Users\\cluac\\MATSimScenarios\\CongestionTesting\\plans.xml";
	private final static int NUMBER_OF_TRIPS = 1000;

	public static void main(String[] args) {
		Random rnd = new Random(1234);

		Config config = ConfigUtils.loadConfig(INPUT_CONFIG_FILE);
//		Network network = NetworkUtils.createNetwork(config);

		Scenario outputScenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
		PopulationFactory populationFactory = outputScenario.getPopulation().getFactory();
		Population outputPlans = outputScenario.getPopulation();

		for (int i = 0; i < NUMBER_OF_TRIPS; i++) {
			Person dummyPerson = populationFactory
					.createPerson(Id.create("dummy_person_" + Integer.toString(i), Person.class));
			Plan dummyPlan = populationFactory.createPlan();
			Activity act0 = populationFactory.createActivityFromLinkId("dummy", Id.create("74", Link.class));
			act0.setEndTime(21600 + rnd.nextInt(3600));
			Leg leg = populationFactory.createLeg("drt");
			Activity act1 = populationFactory.createActivityFromLinkId("dummy", Id.create("14", Link.class));
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
