package playground.lu.helloworld;

import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

public class NetworkChecking {
	public static void main(String[] args) {
		System.out.println("Running network check");
		Config config = ConfigUtils.loadConfig("C:\\Users\\cluac\\MATSimScenarios\\Vulkaneifel\\drtOnly.config.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Network network = scenario.getNetwork();
		Id<Link> linkId = Id.create("110938642", Link.class);
		Set<String> allowedModes = network.getLinks().get(linkId).getAllowedModes();
		for (String mode : allowedModes) {
			System.out.println(mode);
		}

	}
}
