package playground.lu.inputPlanPreparation;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

public class RemoveRequestsFromMotorway {
	private static final Logger log = Logger.getLogger(RemoveRequestsFromMotorway.class);
	private static final String PLAN_PATH = "C:\\Users\\cluac\\MATSimScenarios\\Vulkaneifel\\plans_for_rebalance_study.xml";
	private static final String NETWWORK_PATH = "C:\\Users\\cluac\\MATSimScenarios\\Vulkaneifel\\network.xml.gz";
	private static final String OUTPUT_PLANS_PATH = "C:\\Users\\cluac\\MATSimScenarios\\Vulkaneifel\\adjusted_plans_for_rebalance_study.xml";

	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(NETWWORK_PATH);
		config.global().setCoordinateSystem("epsg:25832");
		config.plans().setInputFile(PLAN_PATH);
		Scenario scenario = ScenarioUtils.loadScenario(config);

		Network network = scenario.getNetwork();
		Population plans = scenario.getPopulation();
		Population outputPlans = ScenarioUtils.loadScenario(ConfigUtils.createConfig()).getPopulation();

		int removedRequestsCounter = 0;
		int nonDrtPersonCounter = 0;
		int drtPersonCounter = 0;
		for (Person person : plans.getPersons().values()) {
			Activity startAct = (Activity) person.getSelectedPlan().getPlanElements().iterator().next();
			Link departureLink = network.getLinks().get(startAct.getLinkId());
			if (departureLink.getFreespeed() < 20 && startAct.getEndTime().orElse(108000) < 97200) {
				outputPlans.addPerson(person);
				if (person.getAttributes().getAttribute("subpopulation").equals("non-drt-person")) {
					nonDrtPersonCounter += 1;
				} else {
					drtPersonCounter += 1;
				}
			} else {
				removedRequestsCounter += 1;
			}
		}

		log.info("There are " + removedRequestsCounter + " requests that are removed, becasue they are on motor way");
		log.info("There are " + drtPersonCounter + " DRT requests");
		log.info("There are " + nonDrtPersonCounter + " non-DRT requests");

		log.info("Writing population file...");
		PopulationWriter pw = new PopulationWriter(outputPlans);
		pw.write(OUTPUT_PLANS_PATH);

	}
}
