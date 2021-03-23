package playground.lu.congestionAwareDrt.berlin;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
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

import playground.lu.readShapeFile.ShapeFileReadingUtils;

public class PrepareVehicles {
	private static final Logger log = Logger.getLogger(PrepareVehicles.class);
	
	private static final String OUTPUT_VEHICLE_FILE = "C:\\Users\\cluac\\MATSimScenarios\\CongestionAwareDrt\\"
			+ "Berlin\\vehicles.xml";
	private static final String SHAPEFILE = "C:\\Users\\cluac\\MATSimScenarios\\CongestionAwareDrt\\"
			+ "Berlin\\shp-berlin-hundekopf-areas\\berlin-hundekopf.shp";
	private static final String NETWORK_FILE = "C:\\Users\\cluac\\MATSimScenarios\\CongestionAwareDrt\\"
			+ "Berlin\\network.xml.gz";
	
	private final static Random RND = new Random(1234);
	
	private static final int NUM_OF_VEHICLES = 10000;
	
	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		config.global().setCoordinateSystem("EPSG:31468");
		config.network().setInputFile(NETWORK_FILE);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Network network = scenario.getNetwork();
		
		// Extracting links within the dog head area
		log.info("Loading shapefile...");
		List<Link> linksInBerlinRing = new ArrayList<>();
		Geometry hundekopf = ShapeFileReadingUtils.getGeometryFromShapeFile(SHAPEFILE);

		log.info("Shapefile successfully loaded");
		log.info("Begin extracting links within the Berlin Ring...");
		for (Link link : network.getLinks().values()) {
			if (ShapeFileReadingUtils.isLinkWithinGeometry(link, hundekopf) && link.getAllowedModes().contains("car")) {
				linksInBerlinRing.add(link);
			}
		}
		int numOfLinks = linksInBerlinRing.size();
		
		
		log.info("Writing vehicle file...");
		
		List<DvrpVehicleSpecification> vehicles = new ArrayList<>();
		for (int i = 0; i < NUM_OF_VEHICLES; i++) {
			Link startLink = linksInBerlinRing.get(RND.nextInt(numOfLinks));
			vehicles.add(ImmutableDvrpVehicleSpecification.newBuilder()
					.id(Id.create("drt_" + Integer.toString(i), DvrpVehicle.class)).startLinkId(startLink.getId())
					.capacity(1).serviceBeginTime(Math.round(1)).serviceEndTime(Math.round(30 * 3600)).build());
		}
		new FleetWriter(vehicles.stream()).write(OUTPUT_VEHICLE_FILE);
	}
}
