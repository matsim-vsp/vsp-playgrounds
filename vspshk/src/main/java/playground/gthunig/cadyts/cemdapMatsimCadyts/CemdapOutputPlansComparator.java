package playground.gthunig.cadyts.cemdapMatsimCadyts;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CemdapOutputPlansComparator {

    private final static String PLANS_FILE_1 = "C:\\Users\\gthunig\\Desktop\\Vsp\\86-86b Compare/plans86small.xml";
    private final static String PLANS_FILE_2 = "C:\\Users\\gthunig\\Desktop\\Vsp\\86-86b Compare/plans86bsmall.xml";

    public static void main(String[] args) {

        Config config = ConfigUtils.createConfig();
        config.plans().setInputFile(PLANS_FILE_1);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Map<Id<Person>, ? extends Person> persons1 = scenario.getPopulation().getPersons();

        config = ConfigUtils.createConfig();
        config.plans().setInputFile(PLANS_FILE_2);
        scenario = ScenarioUtils.loadScenario(config);
        Map<Id<Person>, ? extends Person> persons2 = scenario.getPopulation().getPersons();

        new CemdapOutputPlansComparator().compare(persons1, persons2);
    }

    private int differentActivities = 0;
    private int comparedPersons = 0;
    private int differentEndTimes = 0;
    private int comparedEndTimes = 0;

    private void compare(Map<Id<Person>, ? extends Person> persons1, Map<Id<Person>, ? extends Person> persons2) {

        for (Map.Entry<Id<Person>, ? extends Person> entry : persons1.entrySet()) {
            compare(entry.getValue(), persons2.get(entry.getKey()));
        }

        System.out.println("Percentage of different Activities: " + (double)differentActivities/comparedPersons);
        System.out.println("Percentage of different EndTimes: " + (double)differentEndTimes/comparedEndTimes);
    }

    private void compare(Person person1, Person person2) {

        List<PlanElement> plan1 = person1.getSelectedPlan().getPlanElements();
        List<PlanElement> plan2 = person2.getSelectedPlan().getPlanElements();

        List<Activity> activities1 = new ArrayList<>();
        List<Activity> activities2 = new ArrayList<>();

        for (PlanElement element : plan1) {
            if (element instanceof Activity)
                activities1.add((Activity)element);
        }

        for (PlanElement element : plan2) {
            if (element instanceof Activity)
                activities2.add((Activity)element);
        }

        comparedPersons++;

        if (activities1.size() != activities2.size()) {
            differentActivities++;
        } else {
            for (int i = 0; i < activities1.size(); i++) {
                if (!activities1.get(i).getType().equals(activities2.get(i).getType())) {
                    differentActivities++;
                    return;
                } else {
                    comparedEndTimes++;
                    if (activities1.get(i).getEndTime() != activities2.get(i).getEndTime()) {
                        differentEndTimes++;
                    }
                }
            }
        }
    }
}
