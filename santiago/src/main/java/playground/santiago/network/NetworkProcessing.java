package playground.santiago.network;

import java.util.HashSet;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;

public class NetworkProcessing {
	
	
	public static void main(String[] args) {
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile("../../../mapMatching/0_networks/toMATSim/fullNetwork_v2.xml");
		Network network = (Network) scenario.getNetwork();
		
		HashSet <String> modes = new HashSet<>();
		modes.add(TransportMode.pt);
		modes.add(TransportMode.ride);
		modes.add(TransportMode.other);
		modes.add("colectivo");
		modes.add("taxi");
		
		for(Link ll : network.getLinks().values()){
			ll.setAllowedModes(modes);
		}
		
		new NetworkWriter(network).write("../../../mapMatching/0_networks/toMATSim/fullNetwork_v3.xml");

	}

}
