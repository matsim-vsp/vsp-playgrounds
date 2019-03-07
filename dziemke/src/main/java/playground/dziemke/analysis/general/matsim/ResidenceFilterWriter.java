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
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import playground.dziemke.utils.ShapeReader;

import java.util.Map;

public class ResidenceFilterWriter {

    public static void main(String[] args) {

        String baseDir = "C:\\Users\\gthunig\\VSP\\matsim\\runs-svn\\open_berlin_scenario\\be_257\\";

        Config config = ConfigUtils.createConfig();
        config.network().setInputFile(baseDir + "be_257.output_network.xml.gz");
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Network network = scenario.getNetwork();
        String areaShapeFile = "../../shared-svn/studies/countries/de/open_berlin_scenario/input/shapefiles/2013/Berlin_DHDN_GK4.shp";
        int areaId = 11000000;
        String inputExperiencedPlansFile = baseDir + "be_257.experiencedPlans.xml.gz";
        String outputExperiencedPlansFile = baseDir + "be_257.experiencedPlansWithResidence.xml.gz";

        ResidenceFilterWriter residenceFilterWriter = new ResidenceFilterWriter(network, areaShapeFile, areaId);
        residenceFilterWriter.changeIdentifier("BERLIN", "BRANDENBURG", "unknown");
        residenceFilterWriter.filterAndWrite(inputExperiencedPlansFile, outputExperiencedPlansFile);
    }

    public static final String INTERIOR_OF_AREA = "inside";
    public static final String EXTERIOR_OF_AREA = "outside";
    public static final String UNKNOWN_AREA = "unknown";

    private Network network;
    private Geometry areaGeometry;
    private Population population;
    public static final String RESIDENCE_ATTRIBUTE_NAME = "home";
    private String interiorOfArea = INTERIOR_OF_AREA;
    private String exteriorOfArea = EXTERIOR_OF_AREA;
    private String unknownArea = UNKNOWN_AREA;

    public ResidenceFilterWriter(Network network, String areaShapeFile, int areaId) {

        this.network = network;
        assignAreaGeometry(areaShapeFile, areaId);
    }

    private void assignAreaGeometry(String areaShapeFile, int areaId) {
        Map<Integer, Geometry> zoneGeometries = ShapeReader.read(areaShapeFile, "NR");
        this.areaGeometry = zoneGeometries.get(areaId);
    }

    public void changeIdentifier(String interiorOfArea, String exteriorOfArea, String unknownArea) {

        this.interiorOfArea = interiorOfArea;
        this.exteriorOfArea = exteriorOfArea;
        this.unknownArea = unknownArea;
    }

    public void filterAndWrite(String inputExperiencedPlansFile, String outputExperiencedPlansFile) {

        readPopulationFromFile(inputExperiencedPlansFile);
        filter();
        write(outputExperiencedPlansFile);
    }

    private void filter() {

        for (Person current : population.getPersons().values()) {

            if (current.getSelectedPlan().getPlanElements().size() > 0) {

                Point homeLocation = getHomePoint(current);
                calculateAndAddResidenceAttribute(current, homeLocation);
            } else {
                addResidenceAttribute(current, unknownArea);
            }
        }
    }

    private Point getHomePoint(Person current) {

        Activity activity = (Activity)current.getSelectedPlan().getPlanElements().get(0);
        Id<Link> homeLinkId = activity.getLinkId();
        Link homeLink = network.getLinks().get(homeLinkId);
        double homeCoordX = homeLink.getCoord().getX();
        double homeCoordY = homeLink.getCoord().getY();

        return MGC.xy2Point(homeCoordX, homeCoordY);
    }

    private void calculateAndAddResidenceAttribute(Person person, Point homeLocation) {

        if (areaGeometry.contains(homeLocation))
            addResidenceAttribute(person, interiorOfArea);
        else
            addResidenceAttribute(person, exteriorOfArea);

    }

    private void addResidenceAttribute(Person person, String value) {

        person.getAttributes().putAttribute(RESIDENCE_ATTRIBUTE_NAME, value);
    }

    private void write(String outputExperiencedPlansFile) {

        PopulationWriter writer = new PopulationWriter(population, null);
        writer.write(outputExperiencedPlansFile);
    }

    private void readPopulationFromFile(String populationFile) {

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new PopulationReader(scenario).readFile(populationFile);
        population = scenario.getPopulation();
    }
}
