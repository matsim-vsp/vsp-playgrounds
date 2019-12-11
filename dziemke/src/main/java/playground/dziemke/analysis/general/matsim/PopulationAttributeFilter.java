package playground.dziemke.analysis.general.matsim;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;
import playground.dziemke.utils.ShapeFileUtils;

import java.util.Collection;

/**
 * @author dziemke
 */
public class PopulationAttributeFilter {

    private Population population;
    private String attributeFilterLabel;

    public static void main(String[] args) {

        String baseDir = "../../runs-svn/open_berlin_scenario/v5.5-bicycle/bc-15/output/";
        String runId = "berlin-v5.5-1pct-15";
//        String inputPopulationFile = baseDir + runId + ".output_plans.xml.gz";
//        String outputPopulationFile = baseDir + runId + ".output_plans_no-freight.xml.gz";
        String inputPopulationFile = baseDir + runId + ".output_plans_no-freight.xml.gz";
        String outputPopulationFile = baseDir + runId + ".output_plans_no-freight_berlin.xml.gz";

//        PopulationAttributeFilter populationAttributeFilter = new PopulationAttributeFilter(inputPopulationFile, "subpopulation");
//        Population filteredPopulation = populationAttributeFilter.getFilteredPopulation("person");
        PopulationAttributeFilter populationAttributeFilter = new PopulationAttributeFilter(inputPopulationFile, "home-activity-zone");
        Population filteredPopulation = populationAttributeFilter.getFilteredPopulation("berlin");
        populationAttributeFilter.writePopulation(filteredPopulation, outputPopulationFile);
    }

    public PopulationAttributeFilter(String inputPopulationFile, String attributeFilterLabel) {
        Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
        new PopulationReader(scenario).readFile(inputPopulationFile);
        this.population = scenario.getPopulation();
        this.attributeFilterLabel = attributeFilterLabel;

        for (Person person : population.getPersons().values()) {
            if (person.getAttributes().getAttribute(attributeFilterLabel) == null) {
                throw new IllegalArgumentException("Filter attribute must already exist.");
            }
        }
    }

    public Population getFilteredPopulation(String filterValue) {
        Population filteredPopulation = PopulationUtils.createPopulation(ConfigUtils.createConfig());

        for (Person current : population.getPersons().values()) {
            if (current.getAttributes().getAttribute(attributeFilterLabel).equals(filterValue))
                filteredPopulation.addPerson(current);
        }
        return filteredPopulation;
    }

    public void writePopulation(Population population, String outputPopulationFile) {
        (new PopulationWriter(population, null)).write(outputPopulationFile);
    }
}