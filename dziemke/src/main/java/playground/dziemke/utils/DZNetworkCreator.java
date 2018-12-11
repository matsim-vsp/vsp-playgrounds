package playground.dziemke.utils;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;

public class DZNetworkCreator {

	public static void main(String[] args) {
		// Input and output
//		String osmFile = "../../shared-svn/projects/silo/maryland/other/osm/md_and_surroundings.osm";
//		String networkFile = "../../shared-svn/projects/silo/maryland/network/10/network.xml.gz";
		String osmFile = "/Users/dominik/Downloads/oberbayern-latest.osm";
		String networkFile = "/Users/dominik/Downloads/network_coarse.xml";
		
		// Parameters
		String inputCRS = "EPSG:4326"; // WGS84
		String outputCRS = "EPSG:31468"; // DHDN GK4, for Berlin; DE
//		String outputCRS = TransformationFactory.WGS84_SA_Albers;
//		String outputCRS = "EPSG:21037"; // Arc 1960 / UTM zone 37S, for Nairobi, KE
//		String outputCRS = "EPSG:26918"; // NAD83 / UTM zone 18N, for Maryland, US

		// Infrastructure
		Network network = (ScenarioUtils.createScenario(ConfigUtils.createConfig())).getNetwork();
		CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(inputCRS, outputCRS);
		
		// "useHighwayDefaults" needs to be set to false to be able to set own values below.
		OsmNetworkReader osmNetworkReader = new OsmNetworkReader(network, coordinateTransformation, false);

		// Set values
		osmNetworkReader.setHighwayDefaults(1, "motorway",      2, 100.0/3.6, 1.0, 2000, true); // 100 instead of 120
		osmNetworkReader.setHighwayDefaults(1, "motorway_link", 1,  80.0/3.6, 1.0, 1500, true);
		osmNetworkReader.setHighwayDefaults(2, "trunk",         1,  80.0/3.6, 1.0, 2000);
		osmNetworkReader.setHighwayDefaults(2, "trunk_link",    1,  50.0/3.6, 1.0, 1500);
//		osmNetworkReader.setHighwayDefaults(3, "primary",       1,  80.0/3.6, 1.0, 1500);
//		osmNetworkReader.setHighwayDefaults(3, "primary_link",  1,  60.0/3.6, 1.0, 1500);
//		osmNetworkReader.setHighwayDefaults(4, "secondary",     1,  60.0/3.6, 1.0, 1000);
//		osmNetworkReader.setHighwayDefaults(5, "tertiary",      1,  45.0/3.6, 1.0,  600);
		// minor, unclassified, residential, living_street" are left out here, whereas they are used by defaults.
			
		// additional to defaults
//		osmNetworkReader.setHighwayDefaults(4, "secondary_link", 1, 60.0/3.6, 1.0, 1000); // same values as "secondary"
//		osmNetworkReader.setHighwayDefaults(5, "tertiary_link", 1, 45.0/3.6, 1.0,  600); // same values as "tertiary"

		// Read OSM file
		osmNetworkReader.parse(osmFile); 
		new NetworkCleaner().run(network);
		
		// Write network XML file
		(new NetworkWriter(network)).write(networkFile);
	}
}