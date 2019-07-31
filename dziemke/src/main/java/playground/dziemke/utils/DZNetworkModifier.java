package playground.dziemke.utils;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.ArrayList;

public class DZNetworkModifier {

    public static void main (String[] args) {
        String networkInputFile = "../../public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.3-10pct/input/berlin-v5-network.xml.gz";
        String networkOutputFile = "../../public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.3-10pct/input/berlin-v5-network-sec2.xml.gz";

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        MatsimNetworkReader reader = new MatsimNetworkReader(scenario.getNetwork());
        reader.readFile(networkInputFile);

        for (Link link : new ArrayList<>(scenario.getNetwork().getLinks().values())) {
            if (link.getAllowedModes().contains(TransportMode.car)) {
                String type = link.getAttributes().getAttribute("type").toString();
                if (type.equals("tertiary") || type.equals("tertiary_link") || type.equals("unclassified")
                        || type.equals("residential") || type.equals("living_street")) {
                    scenario.getNetwork().removeLink(link.getId());
                }
            }
        }

        NetworkWriter writer = new NetworkWriter(scenario.getNetwork());
        writer.write(networkOutputFile);
    }
}