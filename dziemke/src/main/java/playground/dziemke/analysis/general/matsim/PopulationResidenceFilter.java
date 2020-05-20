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
public class PopulationResidenceFilter {
    public static final String INTERIOR_OF_AREA = "interior";
    public static final String EXTERIOR_OF_AREA = "exterior";
    public static final String UNKNOWN_AREA = "unknown";

    private Network network;
    private Population population;
    private Geometry areaGeometry;

    public static final String RESIDENCE_ATTRIBUTE_LABEL = "residence";
    private String interiorOfArea = INTERIOR_OF_AREA;
    private String exteriorOfArea = EXTERIOR_OF_AREA;
    private String unknownArea = UNKNOWN_AREA;

    public static void main(String[] args) {
        String baseDir = "../../runs-svn/open_berlin_scenario/v5.3-policies/output/b-01/";
        String runId = "berlin-v5.3-10pct-ctd-b-01";
        String networkFile = baseDir + runId + ".output_network.xml.gz";
        String inputPopulationFile = baseDir + runId + ".experiencedPlans.xml.gz";
        String outputPopulationFile = baseDir + runId + ".experiencedPlansWithResidence.xml.gz";

        String areaShapeFile = "../../shared-svn/studies/countries/de/open_berlin_scenario/input/shapefiles/2013/Berlin_DHDN_GK4.shp";
        String attributeCaption = "NR";
        String distinctiveFeatureId = "11000000";

        PopulationResidenceFilter populationResidenceFilter = new PopulationResidenceFilter(networkFile, inputPopulationFile,
                areaShapeFile, attributeCaption, distinctiveFeatureId);
        populationResidenceFilter.changeIdentifier("berlin", "brandenburg", "unknown");
        populationResidenceFilter.assignResidenceAttribute();
        populationResidenceFilter.writePopulation(outputPopulationFile);
    }

    public PopulationResidenceFilter(String networkFile, String populationFile, String areaShapeFile, String attributeCaption, String distinctiveFeatureId) {
        Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
        MatsimNetworkReader matsimNetworkReader = new MatsimNetworkReader(scenario.getNetwork());
        matsimNetworkReader.readFile(networkFile);
        this.network = scenario.getNetwork();

        Collection<SimpleFeature> features = (new ShapeFileReader()).readFileAndInitialize(areaShapeFile);
        this.areaGeometry = ShapeFileUtils.getGeometryByValueOfAttribute(features, attributeCaption, distinctiveFeatureId);

        new PopulationReader(scenario).readFile(populationFile);
        this.population = scenario.getPopulation();
    }

    public void changeIdentifier(String interiorOfArea, String exteriorOfArea, String unknownArea) {
        this.interiorOfArea = interiorOfArea;
        this.exteriorOfArea = exteriorOfArea;
        this.unknownArea = unknownArea;
    }

    public void assignResidenceAttribute() {
        for (Person person : population.getPersons().values()) {
            if (person.getSelectedPlan().getPlanElements().size() > 0) {
                Point homeLocation = getResidenceAsPoint(person);
                if (areaGeometry.contains(homeLocation)) {
                    addResidenceAttribute(person, interiorOfArea);
                } else {
                    addResidenceAttribute(person, exteriorOfArea);
                }
            } else {
                addResidenceAttribute(person, unknownArea);
            }
        }
    }

    public Population getFilteredPopulation(String filterValue) {
        Population filteredPopulation = PopulationUtils.createPopulation(ConfigUtils.createConfig());

        for (Person current : population.getPersons().values()) {
            if (current.getAttributes().getAttribute(RESIDENCE_ATTRIBUTE_LABEL).equals(filterValue))
                filteredPopulation.addPerson(current);
        }
        return filteredPopulation;
    }

    public Population getFullPopulation() {
        return population;
    }

    public void writePopulation(String outputPopulationFile) {
        (new PopulationWriter(population, null)).write(outputPopulationFile);
    }

    private Point getResidenceAsPoint(Person person) {
        Activity activity = (Activity)person.getSelectedPlan().getPlanElements().get(0);
        Id<Link> homeLinkId = activity.getLinkId();
        Link homeLink = network.getLinks().get(homeLinkId);
        double homeCoordX = homeLink.getCoord().getX();
        double homeCoordY = homeLink.getCoord().getY();
        return MGC.xy2Point(homeCoordX, homeCoordY);
    }

    private void addResidenceAttribute(Person person, String value) {
        person.getAttributes().putAttribute(RESIDENCE_ATTRIBUTE_LABEL, value);
    }
}