package playground.lu.inputPlanPreparation;

import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

public class GenerateTestingPlanFile {
	private static final Logger log = Logger.getLogger(GenerateTestingPlanFile.class);

	private final static String INPUT_CONFIG_FILE = "C:/Users/cluac/MATSimScenarios/Berlin/berlin-drt-v5.5-10pct.config.xml";
	private final static String OUTPUT_PATH = "C:/Users/cluac/MATSimScenarios/Berlin/testingPlan.xml";
	private final static int MAX_NUMBER_OF_AGENTS = 20;
	private final static double PROBABILITY = 0.05;

	public static void main(String[] args) {
		int counter = 0;
		Random rnd = new Random(1234);

		Config config = ConfigUtils.loadConfig(INPUT_CONFIG_FILE);
		Scenario scenario = ScenarioUtils.loadScenario(config);

		Scenario outputScenario = ScenarioUtils.loadScenario(config);
		Population outputPlans = outputScenario.getPopulation();
		for (Id<Person> personId : scenario.getPopulation().getPersons().keySet()) {
			outputPlans.removePerson(personId);
		}

		for (Person person : scenario.getPopulation().getPersons().values()) {
			if (rnd.nextDouble() < PROBABILITY) {
				outputPlans.addPerson(person);
				counter += 1;
				log.info("Person #" + counter + " is added to the test plan");
				if (counter >= MAX_NUMBER_OF_AGENTS) {
					break;
				}
			}
		}

		log.info("Writing population...");
		new PopulationWriter(outputScenario.getPopulation()).write(OUTPUT_PATH);
		log.info("Writing population... Done.");
	}
}
