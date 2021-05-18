package playground.lu.teaching.its;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.Random;

public class Homework1 {

    public static void main(String[] args) {
        String outputPath = "/Users/luchengqi/Documents/TU-Berlin/Files/Homework1/plans.xml";
        Random rnd = new Random(1234);
        String[] enterLinks = {"1", "7", "13", "19", "25", "31", "106", "112", "118", "124", "130", "136"};
        String[] exitLinks = {"6", "12", "18", "24", "30", "36", "101", "107", "113", "119", "125", "131"};
        double startTime = 18 * 3600;
        int timeWindow = 2 * 3600;

        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Population population = scenario.getPopulation();
        PopulationFactory populationFactory = population.getFactory();

        int personCounter = 0;
        for (String startLink : enterLinks) {
            int incomingFlow = 600 + rnd.nextInt(3000);
            System.out.println("Incoming flow for " + startLink + " is " + incomingFlow);

            int generated = 0;
            while (generated < incomingFlow) {
                Person person = populationFactory.createPerson(Id.createPersonId(Integer.toString(personCounter)));
                Plan plan = populationFactory.createPlan();
                Activity act0 = populationFactory.createActivityFromLinkId("h", Id.createLinkId(startLink));
                act0.setEndTime(startTime + rnd.nextInt(timeWindow));
                int exitLinkIdx = rnd.nextInt(exitLinks.length);
                Activity act1 = populationFactory.
                        createActivityFromLinkId("h", Id.createLinkId(exitLinks[exitLinkIdx]));
                Leg leg = populationFactory.createLeg("car");
                plan.addActivity(act0);
                plan.addLeg(leg);
                plan.addActivity(act1);
                person.addPlan(plan);
                population.addPerson(person);

                generated += 1;
                personCounter += 1;
            }
        }

        new PopulationWriter(scenario.getPopulation()).write(outputPath);


    }
}
