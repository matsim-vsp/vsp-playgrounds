package playground.gleich.analysis;

import playground.gleich.analysis.experiencedTrips.RunExperiencedTripsAnalysis;
import playground.gleich.analysis.pt.PaxCount.PaxCountFromEvents;

public class FilePathGuesser {

	public static void main(String[] args) {
		String run = "/home/gregor/git/capetown/output-minibus-w-transit/routeDesignScoring_area_50_1.0_stop2stop_2.4_2.0_NcostSellVeh_FreightAsPt_korrigiert/";
		String iteration = "600";
		String monitoredModes = "pt,transit_walk,access_walk,egress_walk";
		
		String networkFile = run + "output_network.xml.gz";
		String scheduleFile = run + "ITERS/it." + iteration + "/" + iteration + ".transitSchedule.xml.gz";
		String eventsFile = run + "ITERS/it." + iteration + "/" + iteration + ".events.xml.gz";
		String outputFile =  run + "ITERS/it." + iteration + "/" + iteration + ".";
		
//		String[] argsToPass = {networkFile, scheduleFile, eventsFile, outputFile + "PaxCountFromEvents.csv"};
//		PaxCountFromEvents.main(argsToPass);
		
//		String[] argsToPass = {run};
		String[] argsToPass = {networkFile, scheduleFile, eventsFile, null, monitoredModes, outputFile + "experiencedTrips.csv", null};
		RunExperiencedTripsAnalysis.main(argsToPass);
	}

}
