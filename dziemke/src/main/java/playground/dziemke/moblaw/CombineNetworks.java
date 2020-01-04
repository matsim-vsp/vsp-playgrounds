/**
 * 
 */
package playground.dziemke.moblaw;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;

/**
 * @author tthunig
 */
public class CombineNetworks {

	private static final Logger LOG = Logger.getLogger(CombineNetworks.class);
	
	static final String FAST_CYCLE_TRACK = "fct";
	static final String PROTECTED_BICYCLE_LANE = "pbl";
	static final String NORMAL_PATH = "np";
	static final String BICYCLE_CONNECTION = "bc";

	static final double FCT_BICYCLE_SPEED = 25.0 / 3.6; // TODO 23.0 / 3.6;
	static final double PBL_BICYCLE_SPEED = 20.0 / 3.6; // TODO 18.0 / 3.6;
	static final double NP_BICYCLE_SPEED = 15.0 / 3.6;
	static final double BC_BICYCLE_SPEED = 15.0 / 3.6;
	
	
	public static void main(String[] args) {
		String pathToInputDir = "examples/bicyclesBerlinMoblaw/";
		String inputNetworkFileName = pathToInputDir + "berlin-v5.5-network-with-bicycles.xml.gz";
//		String outputNetworkBicycleOnly = pathToInputDir + "berlin-only-new-bicycle-links-scenario2.xml.gz";
//		String outputNetworkCombined = pathToInputDir + "berlin-v5.5-network-with-bicycles-and-new-links-scenario2.xml.gz";
		String outputNetworkBicycleOnly = pathToInputDir + "berlin-only-new-bicycle-links-V2.xml.gz";
		String outputNetworkCombined = pathToInputDir + "berlin-v5.5-network-with-bicycles-and-new-links-V2.xml.gz";

		Network networkBicycleOnly = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getNetwork();

		LOG.info("Start creating nodes and links for the new bicycle network...");
//		AddNodesAndLinksToNetwork_Scenario2.createNewBicycleNodesAndLinks(networkBicycleOnly); 
		AddNodesAndLinksToNetwork_V2.createNewBicycleNodesAndLinks(networkBicycleOnly);

		LOG.info("Write out a network only with the new bicycle links.");
		new NetworkWriter(networkBicycleOnly).write(outputNetworkBicycleOnly);
		
		LOG.info("Read in the regular berlin network.");
		Network networkWithoutNewBicycleLinks = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getNetwork();
		new MatsimNetworkReader(networkWithoutNewBicycleLinks).readFile(inputNetworkFileName);
		
		LOG.info("Prepare the combined network.");
		Network networkCombined = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getNetwork();
		MatsimNetworkReader networkReader = new MatsimNetworkReader(networkCombined);
		networkReader.readFile(inputNetworkFileName);
		networkReader.readFile(outputNetworkBicycleOnly);
		
		LOG.info("Add combination links to that network which connect the new bicycle nodes with their nearest nodes in the regular berlin network.");
		createCombinationsBetweenBothNetworks(networkWithoutNewBicycleLinks, networkBicycleOnly, networkCombined);
		
		LOG.info("Write out the combined network containing old berlin links, new bicycle links and connections between both networks.");
		new NetworkWriter(networkCombined).write(outputNetworkCombined);
	}
	
	private static void createCombinationsBetweenBothNetworks(Network networkWithoutNewBicycleLinks, Network networkBicycleOnly, Network networkCombined) {
		for (Node bicycleNode : networkBicycleOnly.getNodes().values()) {
			// find nearest node in the other network which is not pt
			Node nearestNodeInOtherNetwork = NetworkUtils.getNearestNode(networkWithoutNewBicycleLinks, bicycleNode.getCoord());
			while (nearestNodeInOtherNetwork.getId().toString().startsWith("pt")) {
				LOG.info("The nearest node of bicycle node " + bicycleNode.getId() + " is a pt node. Get the next nearest non pt node.");
				networkWithoutNewBicycleLinks.removeNode(nearestNodeInOtherNetwork.getId());
				nearestNodeInOtherNetwork = NetworkUtils.getNearestNode(networkWithoutNewBicycleLinks, bicycleNode.getCoord());
			}
			// create two dummy bicycle links between them for both directions
			createAndAddBicycleLink(networkCombined, BICYCLE_CONNECTION + "_" + bicycleNode.getId() + "_" + nearestNodeInOtherNetwork.getId(), 
					bicycleNode, nearestNodeInOtherNetwork, BC_BICYCLE_SPEED, BICYCLE_CONNECTION);
			createAndAddBicycleLink(networkCombined, BICYCLE_CONNECTION + "_" + bicycleNode.getId() + "_" + nearestNodeInOtherNetwork.getId() + "_", 
					nearestNodeInOtherNetwork, bicycleNode, BC_BICYCLE_SPEED, BICYCLE_CONNECTION);
		}
	}

	static Link createAndAddBicycleLink(Network network, String linkName, Node fromNode, Node toNode, double bicycleSpeed, String type) {
		Link newLink = NetworkUtils.createAndAddLink(network, Id.createLinkId(linkName), fromNode, toNode,
				CoordUtils.calcEuclideanDistance(fromNode.getCoord(), toNode.getCoord()), bicycleSpeed, 1800., 1, null, type);
		Set<String> modes = new HashSet<>();
		modes.add("bicycle");
		newLink.setAllowedModes(modes);
		return newLink;
	}
	
}
