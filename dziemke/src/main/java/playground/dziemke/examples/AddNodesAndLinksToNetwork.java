package playground.dziemke.examples;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

/**
 * @author dziemke, cliesenjohann
 */
public class AddNodesAndLinksToNetwork {

	public static void main(String[] args) {
		String inputNetworkFileName = "path-to-input-network-file/network.xml.gz";
		String outputNetworkFileName = "path-to-output-network-file/network-with-fct.xml.gz";
		double bicycleSpeed = 25.0 / 3.6;

		CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation("EPSG:4326", "EPSG:31464");

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario.getNetwork());
		networkReader.readFile(inputNetworkFileName);

		Network network = scenario.getNetwork();

		// ---

		Node node31 = NetworkUtils.createNode(Id.createNodeId("fct_31"), transformation.transform(new Coord(13.091243, 52.413506)));
		network.addNode(node31);
		Node node32 = NetworkUtils.createNode(Id.createNodeId("fct_32"), transformation.transform(new Coord(13.135272, 52.396828)));
		network.addNode(node32);
		// ... more nodes accorinding to list...

		Link link301 = createBicycleLink(network,"fct_301", node31, node32, bicycleSpeed);
		network.addLink(link301);
		// Link link = createBicycleLink(network,"fct_301", node31, node32, bicylceSpeed);
		// ... more links accorinding to list...

		// ---

		// To create PBL link "parallel" to existing link
		Link pblLink = createPBLLink(network, "8770", bicycleSpeed);

		// ---

		NetworkWriter writer = new NetworkWriter(scenario.getNetwork());
		writer.writeV2(outputNetworkFileName);


	}

	private static Link createBicycleLink(Network network, String linkName, Node fromNode, Node toNode, double bicycleSpeed) {
			return NetworkUtils.createAndAddLink(
					network, Id.createLinkId(linkName), fromNode, toNode,
					CoordUtils.calcEuclideanDistance(fromNode.getCoord(), toNode.getCoord()),
					bicycleSpeed, 1800., 1, null, "bicycle");
	}

	private static Link createPBLLink(Network network, String linkName, double bicycleSpeed) {
		Link extistinglink = network.getLinks().get(Id.createLinkId(linkName));
		return NetworkUtils.createAndAddLink(network, extistinglink.getId(), extistinglink.getFromNode(), extistinglink.getToNode(),
				extistinglink.getLength(), bicycleSpeed, 1800., 1, null, "bicycle");
	}
}