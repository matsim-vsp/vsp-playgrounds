package playground.santiago.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;

import playground.santiago.analysis.eventHandlers.others.SantiagoTestDepartures;

public class SantiagoTests {
	
	final static String plansFile = "../../../runs-svn/santiago/baseCase1pct/outputOfStep1/ITERS/it.500/500.plans.xml.gz";
	final static String eventsFile = "../../../runs-svn/santiago/baseCase1pct/outputOfStep1/ITERS/it.500/500.events.xml.gz";
	
	public static void main (String[]args){

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader popReader = new PopulationReader(scenario);
		popReader.readFile(plansFile);
		Population population =  scenario.getPopulation();

		for (Person p: population.getPersons().values()) {

			if(p.getId().equals(Id.createPersonId("14991002_8"))){

				for(PlanElement pe : p.getSelectedPlan().getPlanElements()) {

					if (pe instanceof Activity){
						
						System.out.println(((Activity) pe).getType() + " - startTime: " + ((Activity) pe).getEndTime());
						
					} else if (pe instanceof Leg) {		
						System.out.println(((Leg) pe).getMode());

					}

				}

			}
		}
		
		SantiagoTestDepartures std = new SantiagoTestDepartures();
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(std);
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
		std.printInfo();
	}
}
