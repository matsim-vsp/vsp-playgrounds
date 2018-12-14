package playground.dziemke.accessibility.input;

import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author dziemke
 */
public class PlanModeSwitcher {
	private static final Logger LOG = Logger.getLogger(PlanModeSwitcher.class);

	public static void main (String[] args) {
		String inputPlansFile = "../../runs-svn/avsim/av_accessibility/input/taxiplans.xml.gz";
		String outputPlansFile = "../../runs-svn/avsim/av_accessibility/input/taxiplans_pt2car05.xml.gz";
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		PopulationReader populationReader = new PopulationReader(scenario);
		populationReader.readFile(inputPlansFile);
		
		Random random = MatsimRandom.getLocalInstance();
		
		int counter = 0;
		
		for (Person person : scenario.getPopulation().getPersons().values()) {
			Id<Person> personId = person.getId();
			if (Character.isDigit(personId.toString().charAt(0))) {
				boolean pt2car = false;
				
				if (random.nextDouble() > 0.5) {
					pt2car = true;
				}

				if (person.getPlans().size() > 1) {
					throw new RuntimeException("If person has more than one plan, we need to update the code.");
				}
				Plan plan = person.getPlans().get(0);
				for (PlanElement planElement : plan.getPlanElements()) {
					if (planElement instanceof Leg) {
						Leg leg = (Leg) planElement;
						if (leg.getMode() == TransportMode.pt) {
							if (pt2car) {
								leg.setMode(TransportMode.car);
								leg.setRoute(null);
							} else {
								leg.setMode(TransportMode.ride);
								leg.setRoute(null);
							}
							counter ++;
						}
					}
				}
			}
		}
		
		PopulationWriter populationWriter = new PopulationWriter(scenario.getPopulation());
		populationWriter.write(outputPlansFile);
		LOG.info(counter + " legs have been switched from pt to car.");
	}
}