package playground.dziemke.accessibility.input;

import java.io.BufferedReader;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesFactoryImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.FacilitiesWriter;

/**
 * @author dziemke
 */
public class CreateFacilitiesWithWaitingTimes {
	private static final Logger LOG = Logger.getLogger(CreateFacilitiesWithWaitingTimes.class);
	
	private static final String DIR = "../../shared-svn/projects/accessibility_berlin/av/waittimes_500_access_grid_rebal_180/";
//	private static final String DIR = "../../shared-svn/projects/accessibility_berlin/av/waittimes_500_access_grid/";
//	private static final String INPUT_FILE = DIR + "merged_10_rebalancing.csv";
	private static final String INPUT_FILE = DIR + "merged_10_rebal_180.csv";
//	private static final String INPUT_FILE = DIR + "merged_10.csv";
//	private static final String INPUT_FILE = DIR + "merged_10_short.csv";
	private static final String OUTPUT_FILE = DIR + "facilities_2.xml";
//	private static final String OUTPUT_FILE = DIR + "facilities_short.xml";

	public static void main(String[] args) {
		
		ActivityFacilities activityFacilities = FacilitiesUtils.createActivityFacilities();
		
		double waitingTimeIfNotReported = 30*60.;
		int columnIndex = 15;
//		double additionalTimeOffset = 5*60.;
		double additionalTimeOffset = 0.;
		
		BufferedReader reader = null;
		try {
			reader = IOUtils.getBufferedReader(INPUT_FILE);
			String header = reader.readLine(); // header
			String[] headerEntries = header.split(";");
			if (!headerEntries[columnIndex].equals("8")) {
				throw new RuntimeException("Wrong column index");
			}
			String line = reader.readLine(); // Read line
			int facId = 1;
			
			while (line != null) {
				String[] entries = line.split(";");
				
				ActivityFacility actFac = new ActivityFacilitiesFactoryImpl().createActivityFacility(Id.create(facId, ActivityFacility.class), 
						new Coord(Double.valueOf(entries[0]), Double.valueOf(entries[1])));
				
				double waitingTime;
				if (entries.length <= columnIndex) {
					waitingTime = waitingTimeIfNotReported;
				} else {
					double waitingTimeValue = Double.valueOf(entries[columnIndex]);
					if (waitingTimeValue == 0.) {
						waitingTime = waitingTimeIfNotReported;
					} else {
						waitingTime = waitingTimeValue;
					}
				}
				
				actFac.getAttributes().putAttribute("waitingTime_s", waitingTime + additionalTimeOffset);
				
				activityFacilities.addActivityFacility(actFac);
								
				line = reader.readLine(); // Next line
				facId++;
			}
			reader.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		FacilitiesWriter facilitiesWriter = new FacilitiesWriter(activityFacilities);
		facilitiesWriter.write(OUTPUT_FILE);
		LOG.info("Facility file written.");
	}
}