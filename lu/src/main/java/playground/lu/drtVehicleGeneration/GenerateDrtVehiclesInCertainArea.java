package playground.lu.drtVehicleGeneration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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

public class GenerateDrtVehiclesInCertainArea {
	private static final String INPUT_CONFIG = "/Users/luchengqi/Desktop/Homework2-package/Task3-Cottbus/config.xml";
	private static final int[] FLEET_SIZES = { 100, 150, 200, 250, 300, 350, 400, 450, 500, 550, 600 };
	private static final String VEHICLE_FILE_NAME_HEADING = "/Users/luchengqi/Desktop/Homework2-package/Task3-Cottbus/drt-vehicles/fleet-size-";
	private static final String PATH_TO_SERVICEAREA = "/Users/luchengqi/Documents/MATSimScenarios/Cottbus/shapeFiles/cottbus.shp";

	private static final Random RND = new Random();

	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig(INPUT_CONFIG);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Network network = scenario.getNetwork();
		Geometry serviceArea = ShapeFileReadingUtils.getGeometryFromShapeFile(PATH_TO_SERVICEAREA);

		// Put all links within the service area to a list
		List<Link> linksWithinServiceArea = new ArrayList<>();
		for (Link link : network.getLinks().values()) {
			if (ShapeFileReadingUtils.isLinkWithinGeometry(network, link.getId(), serviceArea)
					&& link.getAllowedModes().contains("car")) {
				linksWithinServiceArea.add(link);
			}
		}
		int numOfLinks = linksWithinServiceArea.size();

		for (int fleetSize : FLEET_SIZES) {
			System.out.println("Now writing vehicle file with fleet size of " + fleetSize + "...");
			List<DvrpVehicleSpecification> vehicles = new ArrayList<>();
			int counter = 0;
			while (counter < fleetSize) {
				Link startLink = linksWithinServiceArea.get(RND.nextInt(numOfLinks));
				vehicles.add(ImmutableDvrpVehicleSpecification.newBuilder()
						.id(Id.create("taxi" + counter, DvrpVehicle.class)).startLinkId(startLink.getId()).capacity(8)
						.serviceBeginTime(Math.round(1)).serviceEndTime(Math.round(24 * 3600)).build());
				counter += 1;
			}
			String fileNameBase = VEHICLE_FILE_NAME_HEADING + Integer.toString(fleetSize);
			new FleetWriter(vehicles.stream()).write(fileNameBase + ".xml");
		}
	}
}
