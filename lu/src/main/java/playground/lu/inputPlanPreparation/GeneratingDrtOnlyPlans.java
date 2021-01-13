package playground.lu.inputPlanPreparation;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
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
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;

import playground.lu.readShapeFile.ShapeFileReadingUtils;

public class GeneratingDrtOnlyPlans {
	private static final Logger log = Logger.getLogger(GeneratingDrtOnlyPlans.class);

	private static final String INPUT_CONFIG = "C:\\Users\\cluac\\MATSimScenarios\\Vulkaneifel\\Vulkaneifel.config.xml";
	private static final String OUTPUT_PLANS_PATH = "C:\\Users\\cluac\\MATSimScenarios\\Vulkaneifel\\plans_for_rebalance_study.xml";
	private static final String PATH_TO_SHAPEFILE = "C:\\Users\\cluac\\MATSimScenarios\\Vulkaneifel\\ServiceArea\\vulkaneifel.shp";

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
						PopulationUtils.putPersonAttribute(drtPerson, "subpopulation", "drt-person");
						outputPlans.addPerson(drtPerson);
						personCounter += 1;
					}
				}
			}
		}
		log.info("There are " + personCounter + " DRT dummy persons (DRT legs)");

		// Adding walk trips (this is used to simulate daily
		// fluctuation of the requests)
		Geometry serviceArea = ShapeFileReadingUtils.getGeometryFromShapeFile(PATH_TO_SHAPEFILE);
		Network network = scenario.getNetwork();
		int additionalPersonCounter = 0;
		for (Person person : originalPlan.getPersons().values()) {
			for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
				if (additionalPersonCounter >= personCounter) {
					break;
				}
				if (planElement instanceof Leg) {
					if (((Leg) planElement).getMode().equals("car") || ((Leg) planElement).getMode().equals("bike")) {
						Id<Link> startLinkId = ((Leg) planElement).getRoute().getStartLinkId();
						Id<Link> endLinkId = ((Leg) planElement).getRoute().getEndLinkId();
						if (ShapeFileReadingUtils.isLinkWithinGeometry(network, startLinkId, serviceArea)
								&& ShapeFileReadingUtils.isLinkWithinGeometry(network, endLinkId, serviceArea)) {
							Person nonDrtPerson = populationFactory.createPerson(
									Id.create("additional_dummy_person_" + Integer.toString(additionalPersonCounter),
											Person.class));
							Plan nonDrtPlan = populationFactory.createPlan();
							Activity act0 = populationFactory.createActivityFromLinkId("dummy", startLinkId);
							act0.setEndTime(((Leg) planElement).getDepartureTime().orElse(129600));
							Leg leg = populationFactory.createLeg("walk");
							Activity act1 = populationFactory.createActivityFromLinkId("dummy", endLinkId);
							nonDrtPlan.addActivity(act0);
							nonDrtPlan.addLeg(leg);
							nonDrtPlan.addActivity(act1);
							nonDrtPerson.addPlan(nonDrtPlan);
							PopulationUtils.putPersonAttribute(nonDrtPerson, "subpopulation", "non-drt-person");
							outputPlans.addPerson(nonDrtPerson);
							additionalPersonCounter += 1;
						}
					}
				}
			}
		}

		log.info("There are " + additionalPersonCounter + " non DRT dummy persons (walk legs)");

		log.info("Writing population file...");
		PopulationWriter pw = new PopulationWriter(outputPlans);
		pw.write(OUTPUT_PLANS_PATH);
	}

}
