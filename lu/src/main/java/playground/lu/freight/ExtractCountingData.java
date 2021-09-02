package playground.lu.freight;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class ExtractCountingData {
    public static void main(String[] args) throws IOException {
        BufferedReader csvReader = new BufferedReader(new FileReader("/Users/luchengqi/Documents/MATSimScenarios/GermanFreight/Freight-Traffic-Count/Jawe2019.csv"));
        String[] title = csvReader.readLine().split(";");
        System.out.println("Coordinates: " + title[156] + ", " + title[157]);
        System.out.println("SV data, total: " + title[37] + ", Richtung 1: " + title[38] + ", Richtung 2: " + title[39]);
        System.out.println("Road type: " + title[5]);
        CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation("EPSG:25832", "EPSG:5677");

        Config config = ConfigUtils.createConfig();
        config.network().setInputFile("/Users/luchengqi/Documents/MATSimScenarios/GermanFreight/Freight-Traffic-Count/german-primary-road.network.xml.gz");
        config.global().setCoordinateSystem("EPSG:5677");
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Network network = scenario.getNetwork();


        FileWriter csvWriter = new FileWriter("/Users/luchengqi/Documents/MATSimScenarios/GermanFreight/Freight-Traffic-Count/countData.csv");
        csvWriter.append("Count station number");
        csvWriter.append(",");
        csvWriter.append("Count station name");
        csvWriter.append(",");
        csvWriter.append("Road type");
        csvWriter.append(",");
        csvWriter.append("X");
        csvWriter.append(",");
        csvWriter.append("Y");
        csvWriter.append(",");
        csvWriter.append("total count");
        csvWriter.append(",");
        csvWriter.append("direction 1 count");
        csvWriter.append(",");
        csvWriter.append("direction 2 count");
        csvWriter.append("\n");


        Counts counts = new Counts();
        Counts motorwayCounts = new Counts();
        counts.setName("BASt Automatische Zählstellen 2019");
        counts.setYear(2019);
        motorwayCounts.setName("BASt Automatische Zählstellen 2019 Motorway only");
        motorwayCounts.setYear(2019);

        int counter = 1;
        List<Id<Link>> processed = new ArrayList<>();
        while (true) {
            String dataEntry = csvReader.readLine();
            if (dataEntry == null) {
                break;
            }
            String[] data = dataEntry.split(";");
            String roadName = data[2];
            String totalCount = data[37].replace(".", "");
            String countDirection1 = data[38].replace(".", "");
            String countDirection2 = data[39].replace(".", "");
            String roadType = data[5];

            if (totalCount != "") {
                // write csv file
                String coord_x = data[156];
                String x = coord_x.replace(".", "");
                String coord_y = data[157];
                String y = coord_y.replace(".", "");
                Coord originalCoord = new Coord(Double.parseDouble(x), Double.parseDouble(y));
                Coord coord = coordinateTransformation.transform(originalCoord);
                csvWriter.append(Integer.toString(counter));
                csvWriter.append(",");
                csvWriter.append(roadName);
                csvWriter.append(",");
                csvWriter.append(roadType);
                csvWriter.append(",");
                csvWriter.append(Double.toString(coord.getX()));
                csvWriter.append(",");
                csvWriter.append(Double.toString(coord.getY()));
                csvWriter.append(",");
                csvWriter.append(totalCount);
                csvWriter.append(",");
                csvWriter.append(countDirection1);
                csvWriter.append(",");
                csvWriter.append(countDirection2);
                csvWriter.append("\n");
                counter += 1;

                // prepare count data xml
                Link link = NetworkUtils.getNearestLink(network, coord);
                Id<Link> linkId = link.getId();
                if (processed.contains(linkId)) {
                    continue;
                }
                processed.add(linkId);
                Count count = counts.createAndAddCount(linkId, roadName);
                double hourlyValue = Math.floor(Double.parseDouble(countDirection1) / 24 + 0.5); //TODO Use average value for the first step
                for (int i = 1; i < 25; i++) {
                    count.createVolume(i, hourlyValue);
                }

                if (roadType.equals("A")) {
                    Count motorwayCount = motorwayCounts.createAndAddCount(linkId, roadName);
                    for (int i = 1; i < 25; i++) {
                        motorwayCount.createVolume(i, hourlyValue);
                    }
                }
            }
        }
        csvWriter.flush();
        csvWriter.close();

        CountsWriter writer = new CountsWriter(counts);
        writer.write("/Users/luchengqi/Documents/MATSimScenarios/GermanFreight/Freight-Traffic-Count/count.xml");

        CountsWriter writer2 = new CountsWriter(motorwayCounts);
        writer2.write("/Users/luchengqi/Documents/MATSimScenarios/GermanFreight/Freight-Traffic-Count/count-autobahn.xml");
    }
}
