package playground.zmeng.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.*;
import java.util.stream.Collectors;

public class SubtoursModesChecker {
    private final String plansFile;
    private final StageActivityTypes stageActivityTypes;
    private final MainModeIdentifier mainModeIdentifier;
    private List<String> chainBasedModes;

    private int chainBasedModesRuleViolationPersonCount;
    private int chainBasedModesRuleViolationSubtoursCount;
    private int totalSubtoursCount;
    Map<Id<Person>, Plan> personId2SelectedPlan = new HashMap<>();
    Map<Id<Person>, Collection<TripStructureUtils.Subtour>> personId2Subtours = new HashMap<>();
    Map<Id<Person>, List<List<String>>> personId2SubtoursModes = new HashMap<>();

    SubtoursModesChecker(String plansFile, List<String> chainBasedModes, StageActivityTypes stageActivityTypes, MainModeIdentifier mainModeIdentifier){

        this.plansFile = plansFile;
        this.chainBasedModes = chainBasedModes;
        this.stageActivityTypes = stageActivityTypes;
        this.mainModeIdentifier = mainModeIdentifier;

        createPersonId2Plan();
        createPersonId2Subtours();
        createPersonId2SubtoursModes();
    }

    public void check(){
        for(Id<Person> personId: this.personId2SubtoursModes.keySet()){
            boolean personIsCounted = false;
            for (List<String> modes :
                    this.personId2SubtoursModes.get(personId)) {
                if(violateExist(modes)){
                    if(!personIsCounted){
                        this.chainBasedModesRuleViolationPersonCount ++;
                        personIsCounted = true;
                    }
                    this.chainBasedModesRuleViolationSubtoursCount++;
                }

            }
        }
        System.out.println("number of Persons = " + this.personId2SubtoursModes.keySet().size() + ", number of total subtours = " + this.totalSubtoursCount);
        System.out.println(chainBasedModesRuleViolationPersonCount + "("+(double)chainBasedModesRuleViolationPersonCount/(double)this.personId2SubtoursModes.keySet().size()*100 +"%)"+ " person violated the chain-based-mode rule");
        System.out.println(chainBasedModesRuleViolationSubtoursCount + "("+(double)chainBasedModesRuleViolationSubtoursCount/(double)this.totalSubtoursCount*100 +"%)"+ " subtours violated the chain-based-mode rule");
    }

    private void createPersonId2Plan() {
        Config config = ConfigUtils.createConfig();
        config.plans().setInputFile(this.plansFile);
        Scenario scenario = ScenarioUtils.loadScenario(config);

        for (Id<Person> personId :
                scenario.getPopulation().getPersons().keySet()) {
            this.personId2SelectedPlan.put(personId,
                    scenario.getPopulation().getPersons().get(personId).getSelectedPlan());
        }
    }

    private void createPersonId2Subtours() {
        for (Id<Person> personId :
                this.personId2SelectedPlan.keySet()) {
            Collection<TripStructureUtils.Subtour> subtours = TripStructureUtils.getSubtours(this.personId2SelectedPlan.get(personId), this.stageActivityTypes);
            this.personId2Subtours.put(personId,subtours);
            this.totalSubtoursCount += subtours.size();
        }
    }

    private void createPersonId2SubtoursModes(){
        for(Id<Person> personId: this.personId2Subtours.keySet()){
            List<List<String>> modesList = new ArrayList<>();
            for (TripStructureUtils.Subtour subtour:
                    this.personId2Subtours.get(personId)) {
                List<String> modes = getSubtourModes(subtour);
                modesList.add(modes);
            }
            this.personId2SubtoursModes.put(personId, modesList);
        }
    }
    private List<String> getSubtourModes(TripStructureUtils.Subtour subtour){
        List<String> modes = new ArrayList<>();
        for(TripStructureUtils.Trip trip: subtour.getTripsWithoutSubSubtours()){
            String mode = mainModeIdentifier.identifyMainMode(trip.getTripElements());
            modes.add(mode);
        }
        return modes;
    }

    private boolean violateExist(List<String> modes){
        boolean violateExist = false;
        List<String> list1 = modes.stream().distinct().collect(Collectors.toList());
        if(list1.size()> 1){
            List<String> list2 = list1.stream().filter(s -> chainBasedModes.contains(s)).collect(Collectors.toList());
            if(!list2.isEmpty())
                violateExist = true;
        }
        return violateExist;
    }

}
