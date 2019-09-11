package playground.dziemke.utils;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

public class DZNetworkConverter {

    public static void main (String[] args) {
        String networkInputFile = "../../shared-svn/projects/silo/maryland/network/cube/Matsim_Network/network_2248.xml.gz";
        String networkOutputFile = "../../shared-svn/projects/silo/maryland/network/cube/Matsim_Network/network_26918.xml.gz";
        String inputCRS = "EPSG:2248"; // NAD83 / Maryland (ftUS)
        String outputCRS = "EPSG:26918"; // NAD83 / UTM zone 18N

        CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(inputCRS, outputCRS);

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        MatsimNetworkReader reader = new MatsimNetworkReader(scenario.getNetwork());
        reader.readFile(networkInputFile);

        for (Node node : scenario.getNetwork().getNodes().values()) {
            node.setCoord(ct.transform(node.getCoord()));
        }

        NetworkWriter writer = new NetworkWriter(scenario.getNetwork());
        writer.write(networkOutputFile);
    }
}