package playground.santiago.matchingpt;

import org.matsim.api.core.v01.Scenario;

import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import com.conveyal.gtfs.GTFSFeed;

import playground.santiago.SantiagoScenarioConstants;

/**
 * @author LeoCamus
 * This class is intended to create a proper transit schedule for the Santiago scenario. 
 * stop_facilities, stop_sequence and departures come from GTFS feed (2013_06)
 * paths come from DTPM shape file (2013_06)
 * TODO: Include links to input files used to create the transit_schedule.
 */
public class SantiagoPtBuilder {

	private static String fromFile = "../../../shared-svn/projects/santiago/scenario/inputFromElsewhere/gtfs/gtfs_201306.zip";
	private static GTFSFeed feed = GTFSFeed.fromFile(fromFile);		
	private static CoordinateTransformation transform  = TransformationFactory.getCoordinateTransformation("EPSG:4326", SantiagoScenarioConstants.toCRS);
	private static boolean useExtendedRouteTypes = false;
	
	public static void main(String[] args) {
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
//		GtfsConverter santiagoConverter = new GtfsConverter(feed, scenario, transform, useExtendedRouteTypes);
	}
	
}
