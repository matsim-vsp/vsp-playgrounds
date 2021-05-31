package playground.lu.drtVehicleGeneration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.fleet.FleetWriter;
import org.matsim.contrib.dvrp.fleet.ImmutableDvrpVehicleSpecification;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

public class GenerateDrtVehiclesCongestionTest {
	private static final int FLEET_SIZE = 2000;
	private static final String OUTPUT_FILE_NAME = "/Users/luchengqi/Documents/MATSimScenarios/Mielec/vehicles-2000.xml";
	private static final String CONFIG_FILE = "/Users/luchengqi/Documents/MATSimScenarios/Mielec/config.xml";
	private static final Random RND = new Random();

	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig(CONFIG_FILE);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Network network = scenario.getNetwork();
		List<Link> links = new ArrayList<>();
		links.addAll(network.getLinks().values());
		int numOfLinks = links.size();
		List<DvrpVehicleSpecification> vehicles = new ArrayList<>();
		for (int i = 0; i < FLEET_SIZE; i++) {
			Link startLink = links.get(RND.nextInt(numOfLinks));
			vehicles.add(ImmutableDvrpVehicleSpecification.newBuilder()
					.id(Id.create("drt_" + Integer.toString(i), DvrpVehicle.class)).startLinkId(startLink.getId())
					.capacity(1).serviceBeginTime(Math.round(1)).serviceEndTime(Math.round(33 * 3600)).build());
		}
		new FleetWriter(vehicles.stream()).write(OUTPUT_FILE_NAME);
	}
}
