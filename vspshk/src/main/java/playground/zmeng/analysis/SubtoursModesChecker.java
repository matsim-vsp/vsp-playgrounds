package playground.zmeng.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;

/*

this class is used to check if in a subtour a violation of chain-based-mode rule exist.
How to use the following code? see below:

    public static void main(String[] args) {
        String plansFile = "/System/Volumes/Data/Users/zhuoxiaomeng/Downloads/berlin-v5.4-1pct.output_plans.xml";
        String configFile = "/System/Volumes/Data/Users/zhuoxiaomeng/Downloads/berlin-v5.4-1pct.output_config_reduced.xml";
        StageActivityTypes stageActivityTypes = new StageActivityTypesImpl();
        MainModeIdentifier mainModeIdentifier = new MainModeIdentifierImpl();

        SubtoursModesChecker subtoursModesChecker = new SubtoursModesChecker(plansFile, configFile, stageActivityTypes, mainModeIdentifier);
        subtoursModesChecker.check();
    }

results will be written like following:
     number of persons = n1, number of total subtours = n2
     n3(n4%) person violated the chain-based-mode rule
     n5(n6%) subtours violated the chain-based-mode rule

 */

/**
 * @author zmeng
 */

public class SubtoursModesChecker {
    Logger logger = Logger.getLogger(SubtoursModesChecker.class);
    private final String PLANSF_ILE;
    private final String CONFIG_FILE;
    private final Set<String> stageActivityTypes;
    private final MainModeIdentifier mainModeIdentifier;
    private String[] chainBasedModes;

    private Scenario scenario;
    private int chainBasedModesRuleViolationPersonCount;
    private int chainBasedModesRuleViolationSubtoursCount;
    private int totalSubtoursCount;
    private Map<Id<Person>, Plan> personId2SelectedPlan = new HashMap<>();
    private Map<Id<Person>, Collection<TripStructureUtils.Subtour>> personId2Subtours = new HashMap<>();
    private Map<Id<Person>, List<List<String>>> personId2SubtoursModes = new HashMap<>();

    SubtoursModesChecker(String plansFile, String configFile, Set<String> stageActivityTypes,
            MainModeIdentifier mainModeIdentifier) {

        this.PLANSF_ILE = plansFile;
        this.CONFIG_FILE = configFile;

        Config config = ConfigUtils.loadConfig(configFile);
        config.plans().setInputFile(plansFile);
        this.scenario = ScenarioUtils.loadScenario(config);
        this.chainBasedModes = config.subtourModeChoice().getChainBasedModes();
        this.stageActivityTypes = stageActivityTypes;
        this.mainModeIdentifier = mainModeIdentifier;

        createPersonId2Plan();
        createPersonId2Subtours();
        createPersonId2SubtoursModes();
    }

    public void check() {
        for (Id<Person> personId : this.personId2SubtoursModes.keySet()) {
            boolean personIsCounted = false;
            for (List<String> modes :
                    this.personId2SubtoursModes.get(personId)) {
                if (violateExist(modes)) {
                    if (!personIsCounted) {
                        this.chainBasedModesRuleViolationPersonCount++;
                        personIsCounted = true;
                    }
                    this.chainBasedModesRuleViolationSubtoursCount++;
                }

            }
        }
        logger.info("number of persons = " + this.personId2SubtoursModes.keySet().size() + ", number of total subtours = " + this.totalSubtoursCount);
        logger.info(chainBasedModesRuleViolationPersonCount + "(" + (double) chainBasedModesRuleViolationPersonCount / (double) this.personId2SubtoursModes.keySet().size() * 100 + "%)" + " person violated the chain-based-mode rule");
        logger.info(chainBasedModesRuleViolationSubtoursCount + "(" + (double) chainBasedModesRuleViolationSubtoursCount / (double) this.totalSubtoursCount * 100 + "%)" + " subtours violated the chain-based-mode rule");
    }

    private void createPersonId2Plan() {
        for (Id<Person> personId :
                this.scenario.getPopulation().getPersons().keySet()) {
            this.personId2SelectedPlan.put(personId,
                    scenario.getPopulation().getPersons().get(personId).getSelectedPlan());
        }
    }

    private void createPersonId2Subtours() {
        for (Id<Person> personId :
                this.personId2SelectedPlan.keySet()) {
            Collection<TripStructureUtils.Subtour> subtours = TripStructureUtils.getSubtours(this.personId2SelectedPlan.get(personId), this.stageActivityTypes);
            this.personId2Subtours.put(personId, subtours);
            this.totalSubtoursCount += subtours.size();
        }
    }

    private void createPersonId2SubtoursModes() {
        for (Id<Person> personId : this.personId2Subtours.keySet()) {
            List<List<String>> modesList = new ArrayList<>();
            for (TripStructureUtils.Subtour subtour :
                    this.personId2Subtours.get(personId)) {
                List<String> modes = getSubtourModes(subtour);
                modesList.add(modes);
            }
            this.personId2SubtoursModes.put(personId, modesList);
        }
    }

    private List<String> getSubtourModes(TripStructureUtils.Subtour subtour) {
        List<String> modes = new ArrayList<>();
        for (TripStructureUtils.Trip trip : subtour.getTripsWithoutSubSubtours()) {
            String mode = mainModeIdentifier.identifyMainMode(trip.getTripElements());
            modes.add(mode);
        }
        return modes;
    }

    private boolean violateExist(List<String> modes) {
        boolean violateExist = false;
        List<String> list1 = modes.stream().distinct().collect(Collectors.toList());
        if (list1.size() > 1) {
            List<String> list2 = list1.stream().filter(s -> Arrays.asList(chainBasedModes).contains(s)).collect(Collectors.toList());
            if (!list2.isEmpty())
                violateExist = true;
        }
        return violateExist;
    }

}
