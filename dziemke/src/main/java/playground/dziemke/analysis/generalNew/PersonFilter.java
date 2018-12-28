package playground.dziemke.analysis.generalNew;

import org.matsim.api.core.v01.population.Person;

public class PersonFilter {

    private boolean onlyAnalyzeTripsDoneByPeopleInAgeRange; // "age"; this requires setting a CEMDAP file
    private int minAge = -1; // typically "x0"
    private int maxAge = -1; // typically "x9"; highest number usually chosen is 119

    public void activateAge(int minAge, int maxAge) {
        onlyAnalyzeTripsDoneByPeopleInAgeRange = true;
        this.minAge = minAge;
        this.maxAge = maxAge;
    }

    public boolean isPersonValid(Person person) {

        if (onlyAnalyzeTripsDoneByPeopleInAgeRange) {
            int age = SurveyAdditionalAttributesUtils.getAge(person);
            if (age > maxAge || age < minAge) return false;
        }
        return true;
    }
}
