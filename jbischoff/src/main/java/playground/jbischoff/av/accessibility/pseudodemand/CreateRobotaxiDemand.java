
package playground.jbischoff.av.accessibility.pseudodemand;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.router.TransitActsRemover;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

public class CreateRobotaxiDemand {


    /**
     * This class can be used to modify the modes of a certain share of current
     * users of a population and replace it by a different mode.
     */


    public static void main(String[] args) {

        String inputPopulation = "D:\\runs-svn\\avsim\\av_accessibility\\input\\b5_22.output_plans.xml.gz";
        String inputAttributes = "D:\\runs-svn\\avsim\\av_accessibility\\input\\b5_22.output_personAttributes.xml.gz";
        String outputPopulation = "D:\\runs-svn\\avsim\\av_accessibility\\input/taxiplans";
        new CreateRobotaxiDemand().run(inputPopulation, inputAttributes, outputPopulation, 0.1);

    }

    public void run(String inputPopulationFile, String inputAttributesFile, String outputPopulationFile, Double robotaxiShare) {
        for (int i = 5; i < 10; i++) {
            Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
            new PopulationReader(scenario).readFile(inputPopulationFile);
            new ObjectAttributesXmlReader(scenario.getPopulation().getPersonAttributes()).readFile(inputAttributesFile);
            replaceMode(TransportMode.car, "taxi", robotaxiShare, scenario, new Random(42 - i));
            new PopulationWriter(scenario.getPopulation()).write(outputPopulationFile + "_" + i + ".xml.gz");

        }
    }


    /**
     * Parses through the population to get all users of a certain fromMode (car), then
     * randomly selects a share of them and replaces all trips of the fromMode with
     * a new one (robotaxi). All unselected plans are removed in this process.
     * It also takes away the car availability of people who have switched from car to robotaxi.
     * It doesn't touch freight or external, but does switch airport car users to robotaxi.
     *
     * @param fromMode
     * @param toMode
     * @param share
     */
    public void replaceMode(String fromMode, String toMode, double share, Scenario scenario, Random random) {
        List<Person> modeUsers = new ArrayList<>();
        for (Person p : scenario.getPopulation().getPersons().values()) {
            PersonUtils.removeUnselectedPlans(p);
            String lives = (String) scenario.getPopulation().getPersonAttributes().getAttribute(p.getId().toString(), "home-activity-zone");
            lives = String.valueOf(lives);
            if (lives.equals("berlin")) {
                Plan plan = p.getSelectedPlan();
                for (PlanElement pe : plan.getPlanElements()) {
                    if (pe instanceof Leg) {
                        if (((Leg) pe).getMode().equals(fromMode)) {
                            modeUsers.add(p);
                            break;
                        }
                    }
                }
            }
        }
        Collections.shuffle(modeUsers, random);
        int numberOfPersons = (int) Math.ceil(modeUsers.size() * share);
        if (numberOfPersons > modeUsers.size())
            numberOfPersons = modeUsers.size();

        for (int i = 0; i < numberOfPersons; i++) {
            Person p = modeUsers.get(i);
            new TransitActsRemover().run(p.getSelectedPlan(), true);

            p.getSelectedPlan().getPlanElements().stream().filter(Leg.class::isInstance).filter(isMode(fromMode))
                    .forEach(l -> {
                        Leg leg = (Leg) l;
                        leg.setMode(toMode);
                        leg.setRoute(null);
                    });

            PersonUtils.setCarAvail(p, "never");
        }


    }


    Predicate<PlanElement> isMode(String mode) {
        return (Predicate<PlanElement>) l -> ((Leg) l).getMode().equals(mode);

    }
}
