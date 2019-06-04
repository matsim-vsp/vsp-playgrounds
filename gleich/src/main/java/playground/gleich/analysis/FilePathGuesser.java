package playground.gleich.analysis;

import java.io.IOException;

import playground.gleich.analysis.experiencedTrips.RunExperiencedTripsAnalysis;
import playground.gleich.analysis.pt.PaxCount.PaxCountFromEvents;

public class FilePathGuesser {

	public static void main(String[] args) {
		String run = "/home/gregor/git/capetown/output-minibus-w-transit/routeDesignScoring_area_50_1.0_stop2stop_2.4_2.0_NcostSellVeh_FreightAsPt_korrigiert/";
		String iteration = "600";
		String monitoredModes = "pt,transit_walk,access_walk,egress_walk";
		String coordinateSystem = "SA_Lo19";
		
		String networkFile = run + "output_network.xml.gz";
		String scheduleFile = run + "ITERS/it." + iteration + "/" + iteration + ".transitSchedule.xml.gz";
		// minibus old
		String transitVehiclesFile = run + "ITERS/it." + iteration + "/" + iteration + ".vehicles.xml.gz";
		// minibus new
//		String vehicleFile = run + "ITERS/it." + iteration + "/" + iteration + ".transitVehicles.xml.gz";
		String plansFile = run + "ITERS/it." + iteration + "/" + iteration + ".plans.xml.gz";
		String eventsFile = run + "ITERS/it." + iteration + "/" + iteration + ".events.xml.gz";
		String outputFile =  run + "ITERS/it." + iteration + "/" + iteration + ".";
		
//		String[] argsToPass = {networkFile, scheduleFile, eventsFile, outputFile + "PaxCountFromEvents.csv"};
//		PaxCountFromEvents.main(argsToPass);
		
//		String[] argsToPass = {run};
//		String[] argsToPass = {networkFile, scheduleFile, eventsFile, null, monitoredModes, outputFile + "experiencedTrips.csv", null};
//		RunExperiencedTripsAnalysis.main(argsToPass);
		
//		AnalysisRunner anaRunner = new AnalysisRunner(networkFile, scheduleFile, transitVehiclesFile, plansFile, 
//				coordinateSystem, eventsFile, outputFile + "AnalysisRunner");
//		try {
//			anaRunner.runAllAnalyzers();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}

}
