package playground.dziemke.utils;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.NetworkSimplifier;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;

public class DZNetworkCreator {

	public static void main(String[] args) {
//		String osmFile = "../../shared-svn/projects/silo/maryland/other/osm/md_and_surroundings.osm";
//		String networkFile = "../../shared-svn/projects/silo/maryland/network/10/network.xml.gz";
		String osmFile = "../../shared-svn/studies/countries/de/open_berlin_scenario/input/osm/2017-08-06_BB_BE.osm";
		String networkFile = "../../shared-svn/studies/countries/de/open_berlin_scenario/input/osm/2017-08-06_BB_BE_network_uncleaned.xml.gz";

		String inputCRS = "EPSG:4326"; // WGS84
		String outputCRS = "EPSG:31468"; // DHDN GK4, for Berlin; DE
//		String outputCRS = TransformationFactory.WGS84_SA_Albers;
//		String outputCRS = "EPSG:21037"; // Arc 1960 / UTM zone 37S, for Nairobi, KE
//		String outputCRS = "EPSG:26918"; // NAD83 / UTM zone 18N, for Maryland, US

		Network network = (ScenarioUtils.createScenario(ConfigUtils.createConfig())).getNetwork();
		CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(inputCRS, outputCRS);
		
		// "useHighwayDefaults" needs to be set to false to be able to set own values below.
		OsmNetworkReader osmNetworkReader = new OsmNetworkReader(network, coordinateTransformation, true, true);

//		osmNetworkReader.setHighwayDefaults(1, "motorway",      2, 100.0/3.6, 1.0, 2000, true); // 100 instead of 120
//		osmNetworkReader.setHighwayDefaults(1, "motorway_link", 1,  80.0/3.6, 1.0, 1500, true);
//		osmNetworkReader.setHighwayDefaults(2, "trunk",         1,  80.0/3.6, 1.0, 2000);
//		osmNetworkReader.setHighwayDefaults(2, "trunk_link",    1,  50.0/3.6, 1.0, 1500);
//		osmNetworkReader.setHighwayDefaults(3, "primary",       1,  80.0/3.6, 1.0, 1500);
//		osmNetworkReader.setHighwayDefaults(3, "primary_link",  1,  60.0/3.6, 1.0, 1500);
//		osmNetworkReader.setHighwayDefaults(4, "secondary",     1,  60.0/3.6, 1.0, 1000);
//		osmNetworkReader.setHighwayDefaults(5, "tertiary",      1,  45.0/3.6, 1.0,  600);
		// further: minor, unclassified, residential, living_street"

		osmNetworkReader.parse(osmFile);

//		new NetworkCleaner().run(network);
//		new NetworkSimplifier().run(network);

		new NetworkWriter(network).write(networkFile);
	}
}