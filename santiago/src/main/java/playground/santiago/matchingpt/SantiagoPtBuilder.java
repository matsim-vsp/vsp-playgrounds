package playground.santiago.matchingpt;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.gtfs.GtfsConverter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;

import java.time.LocalDate;

import org.apache.log4j.Logger;

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
	private static String toFile = "../../../mapMatching/1_output/mapMatchedTransitSchedule.xml.gz";
	private static final Logger log = Logger.getLogger(SantiagoPtBuilder.class);
	
	public static void main(String[] args) {
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		GTFSFeed feed = GTFSFeed.fromFile(fromFile);

		CoordinateTransformation transform  = TransformationFactory.getCoordinateTransformation("EPSG:4326", SantiagoScenarioConstants.toCRS);
		boolean useExtendedRouteTypes = false;
		
		GtfsConverter santiagoConverter = new GtfsConverter(feed, scenario, transform, useExtendedRouteTypes);
		santiagoConverter.setDate(LocalDate.of(2013, 6, 1)) ;
		santiagoConverter.convert();
		TransitScheduleWriter writer = new TransitScheduleWriter(scenario.getTransitSchedule());
		writer.writeFile(toFile);
		
				
	}
	
}
