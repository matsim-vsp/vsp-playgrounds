package playground.santiago.analysis.travelTimes;

import java.io.BufferedWriter;
import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;

import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;
import org.matsim.counts.algorithms.CountSimComparisonTableWriter;
import org.matsim.counts.algorithms.CountsComparisonAlgorithm;

import playground.agarwalamit.analysis.modalShare.ModalShareFromEvents;
import playground.agarwalamit.analysis.tripDistance.TripDistanceHandler;
import playground.santiago.analysis.eventHandlers.others.SantiagoStuckAndAbortEventHandler;
import playground.santiago.analysis.eventHandlers.trafficVolumes.SantiagoLinkVolumeHandler;
import playground.santiago.analysis.eventHandlers.travelDistances.SantiagoModeTripTravelDistanceHandler;
import playground.santiago.analysis.eventHandlers.travelTimes.SantiagoModeTripTravelTimeHandler;


public class SantiagoTravelTimesAnalysis {
	
	private String runDir;	
	private String outputDir;
	private String analysisDir;
	private List<Id<Person>> stuckAgents;
	
	public SantiagoTravelTimesAnalysis(String caseName, String stepName, List<Id<Person>> stuckAgents){

		this.runDir = "../../../runs-svn/santiago/" + caseName + "/";
		this.outputDir = runDir + "outputOf" + stepName + "/";
		this.analysisDir = outputDir + "analysis/";	
		this.stuckAgents=stuckAgents;
	}
	
	private void createDir(File file) {
		file.mkdirs();	
	}
	
	public void writeFileForTravelTimesByMode(int it, int itAux){
		
	File analysisDir = new File(this.analysisDir);
	if(!analysisDir.exists()) createDir(analysisDir);	
	
	String eventsFile = outputDir + "ITERS/it." + String.valueOf(it) + "/" + String.valueOf(it) + ".events.xml.gz";
	
	SantiagoModeTripTravelTimeHandler handler = new SantiagoModeTripTravelTimeHandler(stuckAgents);
	EventsManager events = EventsUtils.createEventsManager();
	events.addHandler(handler);
	MatsimEventsReader reader = new MatsimEventsReader(events);
	

	reader.readFile(eventsFile);
	
	
	SortedMap<String, Map<Id<Person>, List<String>>> travelTimesByMode = handler.getLegModePesonIdTripDepartureTravelTimes ();

	String outputFile = this.analysisDir + String.valueOf(itAux) + ".modeTravelTimes.txt";

	try (BufferedWriter writer = IOUtils.getBufferedWriter(outputFile)) {
		writer.write("mode\tpersonId\tstartTime-travelTime\n");

		for(String mode : travelTimesByMode.keySet()){				
			for (Id<Person> person: travelTimesByMode.get(mode).keySet()){
				for (String time: travelTimesByMode.get(mode).get(person)){						
					writer.write(mode+"\t"+  person + "\t" + time + "\n");
				}

			}
		}
		writer.close();
	} catch (Exception e) {
		throw new RuntimeException("Data is not written. Reason "+e );
	}
	}

	
	

	
//	public static void main (String[]arg){
//		
//		int it = 0;	
//		int itAux = FIRST_IT;
//		
//	//	String linkLengthsOutputDir = ANALYSIS_DIR + "networkLinks.txt";		
//	//	writeLinkLenghts(NET_FILE, linkLengthsOutputDir);
//				
//		while (it<=LENGTH){
//			
//		String itFolder = OUTPUT_FOLDER + "ITERS/it." + it + "/";		
//		String events = itFolder + it +".events.xml.gz";
//		String plans = itFolder + it + ".plans.xml.gz";
//		
//	//	String modalShareOutputDir = ANALYSIS_DIR +"modalSplit_It"+ itAux  + ".txt"; //TODO: BE AWARE!!
//	//	String countsCompareOutputDir = ANALYSIS_DIR + itAux + ".countscompare.txt"; //TODO: BE AWARE!!
//	//	String flowPatternOutputDir = ANALYSIS_DIR + itAux + ".link2Vol.txt";		 //TODO: BE AWARE!!
//		String departureTravelTimesOutputDir = ANALYSIS_DIR + itAux + ".modeTravelTimes.txt";	//TODO: BE AWARE!!
//		String travelDistanceOutputDir = ANALYSIS_DIR + itAux + ".modeTravelDistances.txt";	//TODO: BE AWARE!!
////		String agentsStuckOutputDir =  ANALYSIS_DIR + itAux + ".agentsStucked.txt"; //TODO: BE AWARE!!
//		
//		
//	//	writeModalShare(events, modalShareOutputDir);								 //TODO: BE AWARE!!
//	//	writeCountsCompare(events, countsCompareOutputDir);							 //TODO: BE AWARE!!
//	//	processEventsAndWriteFileForLinkVolumes(events,flowPatternOutputDir);		 //TODO: BE AWARE!!
//		List<Id<Person>> stuckAgents = getStuckAgents(events);
//		writeFileForTravelTimesByMode(events,departureTravelTimesOutputDir, stuckAgents);		 //TODO: BE AWARE!!
//		writeFileForTravelDistanceByMode(events,plans, travelDistanceOutputDir, stuckAgents);		 //TODO: BE AWARE!!
////		writeStuckEvents(events,agentsStuckOutputDir);							//TODO: BE AWARE!!
//		
//		it = it + 50;
//		itAux = itAux + 50;
//		
//		}
//
//
//	}
//	
//	TODO

//	

//	

//	
	
}
