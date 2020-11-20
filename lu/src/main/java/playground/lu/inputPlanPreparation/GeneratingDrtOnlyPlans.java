package playground.lu.inputPlanPreparation;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

public class GeneratingDrtOnlyPlans {
	private static final Logger log = Logger.getLogger(GeneratingDrtOnlyPlans.class);

	private static final String INPUT_CONFIG = "C:\\Users\\cluac\\MATSimScenarios\\Vulkaneifel\\Vulkaneifel.config.xml";
	private static final String OUTPUT_PLANS_PATH = "C:\\Users\\cluac\\MATSimScenarios\\Vulkaneifel\\drtOnlyPlans.xml";

	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig(INPUT_CONFIG);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Population originalPlan = scenario.getPopulation();
		PopulationFactory populationFactory = scenario.getPopulation().getFactory();

		Config emptyConfig = ConfigUtils.createConfig();
		Scenario emptyScenario = ScenarioUtils.loadScenario(emptyConfig);
		Population outputPlans = emptyScenario.getPopulation();

		int personCounter = 0;
		for (Person person : originalPlan.getPersons().values()) {
			for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
				if (planElement instanceof Leg) {
					if (((Leg) planElement).getMode().equals("drt")) {
						Person drtPerson = populationFactory.createPerson(
								Id.create("dummy_person_" + Integer.toString(personCounter), Person.class));
						Plan drtPlan = populationFactory.createPlan();
						Activity act0 = populationFactory.createActivityFromLinkId("dummy",
								((Leg) planElement).getRoute().getStartLinkId());
						act0.setEndTime(((Leg) planElement).getDepartureTime().orElse(129600));
						Leg leg = populationFactory.createLeg("drt");
						Activity act1 = populationFactory.createActivityFromLinkId("dummy",
								((Leg) planElement).getRoute().getEndLinkId());
						drtPlan.addActivity(act0);
						drtPlan.addLeg(leg);
						drtPlan.addActivity(act1);
						drtPerson.addPlan(drtPlan);
						outputPlans.addPerson(drtPerson);
						personCounter += 1;
					}
				}
			}
		}
		log.info("There are " + personCounter + " DRT dummy persons");

		log.info("Writing population file...");
		PopulationWriter pw = new PopulationWriter(outputPlans);
		pw.write(OUTPUT_PLANS_PATH);
	}

}
