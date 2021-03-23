package playground.lu.inputPlanPreparation;

import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

public class ExtractPersonsFromLargePopulation {
	private static final Logger log = Logger.getLogger(ExtractPersonsFromLargePopulation.class);

	private final static String INPUT_PLAN_FILE = "C:\\Users\\cluac\\MATSimScenarios\\Freight-Germany\\testing-german-long-distance-freight.xml";
	private final static String OUTPUT_PATH = "C:\\Users\\cluac\\MATSimScenarios\\Freight-Germany\\testing.freight.plans.xml";
	private final static String CRS = "EPSG:5677";
	private final static int NUM_OF_TRIPS = 100;
	private final static double VALUE = 0.001;
	private final static Random RND = new Random(4711);

	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		config.plans().setInputFile(INPUT_PLAN_FILE);
		config.global().setCoordinateSystem(CRS);
//		config.global().setCoordinateSystem("GK4");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		PopulationFactory populationFactory = scenario.getPopulation().getFactory();

		Scenario outputScenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
		Population outputPlans = outputScenario.getPopulation();

		int counter = 0;
		for (Person person : scenario.getPopulation().getPersons().values()) {
			if (RND.nextDouble() > VALUE) {
				continue;
			}
			Person outputPerson = populationFactory.createPerson(person.getId());
			outputPerson.addPlan(person.getSelectedPlan());
			outputPlans.addPerson(outputPerson);
			counter += 1;
			if (counter >= NUM_OF_TRIPS) {
				break;
			}
		}

		log.info("Writing population...");
		new PopulationWriter(outputScenario.getPopulation()).write(OUTPUT_PATH);
		log.info("Writing population... Done.");

	}

}
