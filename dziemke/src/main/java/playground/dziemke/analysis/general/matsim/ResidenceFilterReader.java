package playground.dziemke.analysis.general.matsim;

import org.junit.Assert;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

public class ResidenceFilterReader {

    public static void main(String[] args) {

        String baseDir = "C:\\Users\\gthunig\\VSP\\matsim\\runs-svn\\open_berlin_scenario\\be_257\\";
        String inputExperiencedPlansFile = baseDir + "be_257.experiencedPlansWithResidence.xml.gz";
        ResidenceFilterReader residenceFilterReader = new ResidenceFilterReader(inputExperiencedPlansFile);
        Population population = residenceFilterReader.filter("BERLIN");
        Assert.assertNotNull(population);
    }

    private Population population;

    public ResidenceFilterReader(String inputExperiencedPlansFile) {

        readPopulationFromFile(inputExperiencedPlansFile);
    }

    private void readPopulationFromFile(String populationFile) {

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new PopulationReader(scenario).readFile(populationFile);
        population = scenario.getPopulation();
    }

    public Population filter(String filterValue) {

        Population filteredPopulation = PopulationUtils.createPopulation(ConfigUtils.createConfig());

        for (Person current : population.getPersons().values()) {

            if (current.getAttributes().getAttribute(ResidenceFilterWriter.RESIDENCE_ATTRIBUTE_NAME).equals(filterValue))
                filteredPopulation.addPerson(current);
        }

        return filteredPopulation;
    }
}
