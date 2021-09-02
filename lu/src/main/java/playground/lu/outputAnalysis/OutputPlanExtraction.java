package playground.lu.outputAnalysis;

import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import playground.lu.readShapeFile.ShapeFileReadingUtils;

import java.io.FileWriter;
import java.io.IOException;

public class OutputPlanExtraction {
    private static final String PATH_TO_PLANS = "/Users/luchengqi/Documents/MATSimScenarios/Kelheim/output/030/030.output_experienced_plans.xml";
    private static final String PATH_TO_NETWORK = "/Users/luchengqi/Documents/MATSimScenarios/Kelheim/kelheim-v1.0-network-with-pt.xml.gz";
    private static final String PATH_TO_SHAPEFILE = "/Users/luchengqi/Documents/MATSimScenarios/Kelheim/shape-file/dilutionArea.shp";
    private static final String OUTPUT_PATH = "/Users/luchengqi/Documents/MATSimScenarios/Kelheim/output/030/filtered-plans.xml.gz";

    public static void main(String[] args) throws IOException {
        Config config = ConfigUtils.createConfig();
        config.global().setCoordinateSystem("EPSG:25832");
        config.network().setInputFile(PATH_TO_NETWORK);
        Scenario emptyScenario = ScenarioUtils.loadScenario(config);
        config.plans().setInputFile(PATH_TO_PLANS);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Network network = scenario.getNetwork();
        Geometry city = ShapeFileReadingUtils.getGeometryFromShapeFile(PATH_TO_SHAPEFILE);

        FileWriter csvWriter = new FileWriter("/Users/luchengqi/Documents/MATSimScenarios/Kelheim/output/030/agents-living-in-area.csv");
        csvWriter.append("person");
        csvWriter.append("\n");

        Population plans = scenario.getPopulation();
        Population outPlans = emptyScenario.getPopulation();
        for (Person person : plans.getPersons().values()) {
            if (person.getSelectedPlan().getPlanElements().isEmpty()) {
                continue;
            }
            PlanElement planElement = person.getSelectedPlan().getPlanElements().get(0);
            if (planElement instanceof Activity) {
                Link homeLink = network.getLinks().get(((Activity) planElement).getLinkId());
                if (homeLink != null && ShapeFileReadingUtils.isLinkWithinGeometry(homeLink, city)) {
                    outPlans.addPerson(person);
                    csvWriter.append(person.getId().toString());
                    csvWriter.append("\n");
                }
            }
        }

        csvWriter.close();

        System.out.println("Writing population");
        new PopulationWriter(outPlans).write(OUTPUT_PATH);


    }
}
