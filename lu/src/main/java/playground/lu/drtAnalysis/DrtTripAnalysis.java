package playground.lu.drtAnalysis;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.matsim.contrib.drt.util.DrtEventsReaders;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.ParallelEventsManager;

public class DrtTripAnalysis {
	private final int eventsQueueSize = 1200000 * 32;

	public static void main(String[] args) throws IOException {
		String eventsFile = args[0];
		DrtTripAnalysis drtTripAnalysis = new DrtTripAnalysis();
		drtTripAnalysis.run(eventsFile);
	}

	public void run(String eventsFile) throws IOException {
		ParallelEventsManager eventManager = new ParallelEventsManager(false, eventsQueueSize);
		DrtTripsEventHandler drtTripsEventHandler = new DrtTripsEventHandler();
		eventManager.addHandler(drtTripsEventHandler);
		eventManager.initProcessing();
		
		MatsimEventsReader matsimEventsReader = DrtEventsReaders.createEventsReader(eventManager);
		matsimEventsReader.readFile(eventsFile);

		List<DrtTrip> drtTrips = drtTripsEventHandler.getDrtTrips();
		System.out.println("There are " + drtTrips.size() + " DRT trips");
		// Export data to CSV file
		writeResultIntoCSVFile(drtTrips);
	}

	private void writeResultIntoCSVFile(List<DrtTrip> drtTrips) throws IOException {
		System.out.println("Writing CSV File now");
		FileWriter csvWriter = new FileWriter("./output/DRTTripsAnalysis/drtTrips.csv");
		csvWriter.append("Request ID");
		csvWriter.append(",");
		csvWriter.append("Submission Time");
		csvWriter.append(",");
		csvWriter.append("From Link");
		csvWriter.append(",");
		csvWriter.append("To Link");
		csvWriter.append(",");
		csvWriter.append("Accepted Time");
		csvWriter.append(",");
		csvWriter.append("Scheduled Picked up Time");
		csvWriter.append(",");
		csvWriter.append("Actual Picked up Time");
		csvWriter.append(",");
		csvWriter.append("Actual Dropped off Time");
		csvWriter.append(",");
		csvWriter.append("Waiting Time");
		csvWriter.append(",");
		csvWriter.append("Rejected Time");
		csvWriter.append("\n");

		for (DrtTrip drtTrip : drtTrips) {
			List<String> elements = new ArrayList<>();
			elements.add(drtTrip.getRequestId());
			elements.add(Double.toString(drtTrip.getSubmissionTime()));
			elements.add(drtTrip.getFromLinkId());
			elements.add(drtTrip.getToLinkId());
			elements.add(drtTrip.getAcceptedTime().toString());
			elements.add(drtTrip.getPickUpTime().toString());
			elements.add(drtTrip.getPickUpTime().toString());
			elements.add(drtTrip.getDropOffTime().toString());
			elements.add(Double.toString(drtTrip.getWaitTime()));
			elements.add(drtTrip.getRejectedTime().toString());

			csvWriter.append(String.join(",", elements));
			csvWriter.append("\n");
		}

		csvWriter.flush();
		csvWriter.close();
	}

}
