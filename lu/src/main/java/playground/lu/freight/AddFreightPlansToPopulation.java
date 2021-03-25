package playground.lu.freight;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

public class AddFreightPlansToPopulation {
	// This script will add the (relevant) freight plans to the population file of the scenario
	// Note: the CRS transformation will be performed in this step. 
	
	private static final String POPULATIAON_FILE = "C:\\Users\\cluac\\MATSimScenarios\\"
			+ "Dusseldorf\\Scenario\\duesseldorf-v1.2-10pct.plans.xml.gz";
	private static final String FREIGHT_ONLY_FILE = "C:\\Users\\cluac\\MATSimScenarios\\"
			+ "Dusseldorf\\freight\\10pct-freight-only-plans.xml";
	private static final String OUTPUT_PATH = "C:\\Users\\cluac\\MATSimScenarios\\"
			+ "Dusseldorf\\Scenario\\duesseldorf-v1.3-10pct.plans.xml.gz";

	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		config.global().setCoordinateSystem("EPSG:25832");
		config.plans().setInputFile(POPULATIAON_FILE);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Population population = scenario.getPopulation();

		Config freightConfig = ConfigUtils.createConfig();
		freightConfig.global().setCoordinateSystem("EPSG:25832");
		freightConfig.plans().setInputFile(FREIGHT_ONLY_FILE);
		freightConfig.plans().setInputCRS("EPSG:5677");
		Scenario freightScenario = ScenarioUtils.loadScenario(freightConfig);
		Population freightOnlyPlans = freightScenario.getPopulation();

		for (Person person : freightOnlyPlans.getPersons().values()) {
			population.addPerson(person);
		}

		// Write new population file
		// Write population
		System.out.println("Writing population file...");
		PopulationWriter pw = new PopulationWriter(population);
		pw.write(OUTPUT_PATH);

	}
	
}
