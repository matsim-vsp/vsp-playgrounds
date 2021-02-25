package playground.lu.inputPlanPreparation;

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

	private final static String INPUT_PLAN_FILE = "C:\\Users\\cluac\\MATSimScenarios\\Dusseldorf\\output\\DebugOutput\\WithExperiencedPlan3\\duesseldorf-10pct-no-lanes.output_plans.xml.gz";
	private final static String OUTPUT_PATH = "C:\\Users\\cluac\\MATSimScenarios\\Dusseldorf\\output\\DebugOutput\\WithExperiencedPlan3\\selected_plans.xml";
	private final static String CRS = "EPSG:25832";
	private final static String[] PERSON_IDS = { "44510", "47012", "33743", "26873", "42550", "27465", "199359",
			"31181" };

	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		config.plans().setInputFile(INPUT_PLAN_FILE);
		config.global().setCoordinateSystem(CRS);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		PopulationFactory populationFactory = scenario.getPopulation().getFactory();

		Scenario outputScenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
		Population outputPlans = outputScenario.getPopulation();

		for (Person person : scenario.getPopulation().getPersons().values()) {
			for (String personId : PERSON_IDS) {
				if (person.getId().toString().equals(personId)) {
					Person outputPerson = populationFactory.createPerson(person.getId());
					outputPerson.addPlan(person.getSelectedPlan());
					outputPlans.addPerson(outputPerson);
					break;
				}
			}
		}

		log.info("Writing population...");
		new PopulationWriter(outputScenario.getPopulation()).write(OUTPUT_PATH);
		log.info("Writing population... Done.");

	}

}
