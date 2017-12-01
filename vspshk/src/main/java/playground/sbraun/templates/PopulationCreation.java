package playground.sbraun.templates;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;

/**
 * @author soehnke
 *
 */
public class PopulationCreation {
	public static void main(String[] args) {
		
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		PopulationFactory popfac = scenario.getPopulation().getFactory();
		
		for(int i=0; i < 100; i++) {
			Person p = popfac.createPerson(Id.createPersonId(i));
			
			scenario.getPopulation().addPerson(p);
			Plan plan = popfac.createPlan();
			p.addPlan(plan);
			
			Activity home = popfac.createActivityFromCoord("home", CoordUtils.createCoord(0, 0));
			home.setEndTime(8*3600);
			plan.addActivity(home);
			
			Leg leg1 = popfac.createLeg("bike");
			plan.addLeg(leg1);
			
			
			Activity work = popfac.createActivityFromCoord("work", CoordUtils.createCoord(1000, 750));
			work.setEndTime(10*3600);
			plan.addActivity(work);
			
			Leg leg2 = popfac.createLeg("bike");
			plan.addLeg(leg2);
			
			Activity home2 = popfac.createActivityFromCoord("home", new Coord(0,0));
			plan.addActivity(home2);
			
		}
		
		new PopulationWriter(scenario.getPopulation()).write("C:/Users/braun/Desktop/Test/input/small_pop.xml");
		
	}

}
