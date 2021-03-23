package playground.lu.inputPlanPreparation;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

public class PrepareNetwork {
	private static final String INPUT_NETWORK = "C:\\Users\\cluac\\MATSimScenarios\\Dusseldorf\\Scenario\\duesseldorf-v1.0-network-with-pt.xml.gz";
	private final static String OUTPUT_PATH = "C:\\Users\\cluac\\MATSimScenarios\\Dusseldorf\\Scenario\\duesseldorf-v1.0-network-with-freight.xml.gz";

	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		config.global().setCoordinateSystem("EPSG:25832");
		config.network().setInputFile(INPUT_NETWORK);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Network network = scenario.getNetwork();
		for (Link link : network.getLinks().values()) {
			if (link.getAllowedModes().contains("car")) {
				Set<String> modes = new HashSet<>();
				modes.addAll(link.getAllowedModes());
				modes.add("freight");
				link.setAllowedModes(modes);
			}
		}

		NetworkWriter networkWriter = new NetworkWriter(network);
		networkWriter.write(OUTPUT_PATH);

	}
}
