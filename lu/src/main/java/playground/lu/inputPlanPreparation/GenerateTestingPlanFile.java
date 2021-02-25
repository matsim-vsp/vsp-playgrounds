package playground.lu.inputPlanPreparation;

import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

public class GenerateTestingPlanFile {
	private static final Logger log = Logger.getLogger(GenerateTestingPlanFile.class);

	private final static String INPUT_PLAN_FILE = "C:\\Users\\cluac\\MATSimScenarios\\Dusseldorf\\Scenario\\v2.0\\PreparedScenario\\duesseldorf-v2.0-25pct-augmented.plans.xml.gz";
	private final static String OUTPUT_PATH = "C:\\Users\\cluac\\MATSimScenarios\\Dusseldorf\\Scenario\\v2.0\\PreparedScenario\\testingPlan.xml";
	private final static String CRS = "EPSG:25832";
	private final static int MAX_NUMBER_OF_AGENTS = 10;
	private final static double PROBABILITY = 0.01;

	public static void main(String[] args) {
		int counter = 0;
		Random rnd = new Random(1234);

		Config config = ConfigUtils.createConfig();
		config.plans().setInputFile(INPUT_PLAN_FILE);
		config.global().setCoordinateSystem(CRS);
		Scenario scenario = ScenarioUtils.loadScenario(config);

		Scenario outputScenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
		Population outputPlans = outputScenario.getPopulation();

		for (Person person : scenario.getPopulation().getPersons().values()) {
			if (rnd.nextDouble() < PROBABILITY) {
				boolean hasCarTrip = false;
				for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
					if (planElement instanceof Leg) {
						((Leg) planElement).setTravelTimeUndefined();
						if (((Leg) planElement).getMode().equals("car")) {
							hasCarTrip = true;
						}
					}
				}

				if (hasCarTrip) {
					outputPlans.addPerson(person);
					counter += 1;
					log.info("Person #" + counter + " is added to the test plan");
				}

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
