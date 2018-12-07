package playground.gleich.network.osmNetworkConvert;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;

public class RunOsmNetworkConverter {

	public static void main(String[] args) {
		String osmFilePath = "C://Users/blueberry/MatSim/OSMConverter/map.osm";
		String outputNetworkPath = "C://Users/blueberry/MatSim/OSMConverter/network-3.xml";
		
		// Get an empty Matsim network
		Network network = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getNetwork();
		
		// TODO: Find out why GeoTools does not work properly on that machine (virus scanner blocks Eclipse internet access?)
		// Get an OsmNetworkReader instance, "EPSG:4120" is what we wanted for Greece, but lead to an exception in GeoTools
		OsmNetworkReader reader = new OsmNetworkReader(network, TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.CH1903_LV03), true);
		
		// Start converting the osm data to a Matsim network using the OsmNetworkReader instance
		reader.parse(osmFilePath);
		
		// Clean the network, that means remove oneway dead ends and similar problems
		(new NetworkCleaner()).run(network);
		
		// Our maximum speed urban/rural program
		(new UrbanAreaSpeedLimiter()).findAndSetSpeedLimitsInUrbanAreas(network);
		
		// Write the output network
		(new NetworkWriter(network)).write(outputNetworkPath);

	}

}
