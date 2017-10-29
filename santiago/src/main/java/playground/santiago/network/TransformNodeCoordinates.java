package playground.santiago.network;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.opengis.feature.simple.SimpleFeature;

public class TransformNodeCoordinates {

	private static Logger log = Logger.getLogger(TransformNodeCoordinates.class);

	
	private static String networkSize = "Big";
	private static String networkLevelOfDetail = "Coarse";
	
	private static String workingDir = "../../../mapMatching/0_networks/";
	private static String MATSimNetworkDir = workingDir + "1_toMATSim/" + networkSize + "/fromJOSM/" + networkSize + "Santiago" + networkLevelOfDetail + ".xml";
	private static String outputDir = workingDir + "1_toMATSim/" + networkSize + "/" + "Transformed" + networkSize + "Santiago" + networkLevelOfDetail + ".xml";

	
	public static void main(String[] args) {
		CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation
				("EPSG:3857", "EPSG:32719");
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(MATSimNetworkDir);
		Network network = (Network) scenario.getNetwork();
		
		for (Node node : network.getNodes().values()) {
			Coord originalCoord = node.getCoord();
			Coord transformedCoord = transformation.transform(originalCoord);
			node.setCoord(transformedCoord);
		}

		new NetworkWriter(network).write(outputDir);
		
		

	}

}
