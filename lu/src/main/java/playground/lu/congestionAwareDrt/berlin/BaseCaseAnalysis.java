package playground.lu.congestionAwareDrt.berlin;

import java.util.Map;

import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.ParallelEventsManager;

public class BaseCaseAnalysis {
	public static final int QUEUE_SIZE = 1048576 * 32;
	public static final String EVENTS_FILE = "C:\\Users\\cluac\\MATSimScenarios\\CongestionAwareDrt\\"
			+ "TestingScenario\\output\\base-case\\output_events.xml.gz";
//	public static final String NETWORK_FILE = "C:\\Users\\cluac\\MATSimScenarios\\CongestionAwareDrt\\"
//			+ "TestingScenario\\network.xml";
//	public static final String INPUT_PLANS = "C:\\Users\\cluac\\MATSimScenarios\\CongestionAwareDrt\\"
//			+ "TestingScenario\\base-case.plans.xml";

	public static void main(String[] args) {
//		Config config = ConfigUtils.createConfig();
////		config.global().setCoordinateSystem("EPSG:31468");
//		config.network().setInputFile(NETWORK_FILE);
//		config.plans().setInputFile(INPUT_PLANS);

		ParallelEventsManager eventManager = new ParallelEventsManager(true, QUEUE_SIZE);
		CarTripHandler carTripHandler = new CarTripHandler();
		eventManager.addHandler(carTripHandler);
		eventManager.initProcessing();
		new MatsimEventsReader(eventManager).readFile(EVENTS_FILE);
		Map<String, Double> carTrips = carTripHandler.getCarTripDataMap();

		double totalTravelTime = 0;
		for (double traveltime : carTrips.values()) {
			totalTravelTime += traveltime;
		}

		double meanTravelTime = totalTravelTime / carTrips.size();
		System.out.println("There are " + carTrips.size() + " trips in total");
		System.out.println("Mean Travel Time is " + meanTravelTime);
	}
}
