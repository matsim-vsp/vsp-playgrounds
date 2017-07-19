package playground.santiago.analysis;

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
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.counts.Counts;
import org.matsim.counts.MatsimCountsReader;
import org.matsim.counts.algorithms.CountSimComparisonTableWriter;
import org.matsim.counts.algorithms.CountsComparisonAlgorithm;

import playground.agarwalamit.analysis.modalShare.ModalShareFromEvents;
import playground.agarwalamit.analysis.tripDistance.TripDistanceHandler;
import playground.santiago.analysis.eventHandlers.SantiagoLinkVolumeHandler;
import playground.santiago.analysis.eventHandlers.SantiagoModeTripTravelTimeHandler;
import playground.santiago.analysis.eventHandlers.SantiagoModeTripTravelDistanceHandler;

/** 
 * 
 * Class to write ALL THE NECESSARY ANALYSIS FILES. Currently: Modal split, counts comparison and link volumes per hour from an events file.
 * 
 */

public class SantiagoAnalysis {
	//Fields related to the scenario and its steps - they must be changed depending on the step . a comment
	private static final String CASE_NAME = "testingCase";
	private static final String STEP_NAME = "StepTesting";
	private static final int FIRST_IT = 0;
	private static final int LAST_IT = 0;
	private static final int LENGTH = LAST_IT - FIRST_IT;
	
	//Fields related to the outputDir - Do not change these.
	private static final String RUN_DIR = "../../../runs-svn/santiago/" + CASE_NAME + "/";	
	private static final String OUTPUT_FOLDER = RUN_DIR + "outputOf" + STEP_NAME + "/";	
	private static final String ANALYSIS_DIR = OUTPUT_FOLDER + "analysis/";

	//Fields related to the inputFiles	
	private static final String NET_FILE = OUTPUT_FOLDER + "output_network.xml.gz";
	private static final String CONFIG_FILE = OUTPUT_FOLDER + "output_config.xml.gz";
	private static final String COUNTS_FILE = OUTPUT_FOLDER + "output_counts.xml.gz";
	
	
	public static void main (String[]arg){
		
		int it = 0;	
		int itAux = FIRST_IT;
		
	//	String linkLengthsOutputDir = ANALYSIS_DIR + "networkLinks.txt";		
	//	writeLinkLenghts(NET_FILE, linkLengthsOutputDir);
				
		while (it<=LENGTH){
			
		String itFolder = OUTPUT_FOLDER + "ITERS/it." + it + "/";		
		String events = itFolder + it +".events.xml.gz";	
		String modalShareOutputDir = ANALYSIS_DIR +"modalSplit_It"+ itAux  + ".txt"; //TODO: BE AWARE!!
	//	String countsCompareOutputDir = ANALYSIS_DIR + itAux + ".countscompare.txt"; //TODO: BE AWARE!!
	//	String flowPatternOutputDir = ANALYSIS_DIR + itAux + ".link2Vol.txt";		 //TODO: BE AWARE!!
	//	String enterTravelTimesOutputDir = ANALYSIS_DIR + itAux + ".modeTravelTimes.txt";	//TODO: BE AWARE!!
	//	String travelDistanceOutputDir = ANALYSIS_DIR + itAux + ".modeTravelDistances.txt";	//TODO: BE AWARE!!
		
		writeModalShare(events, modalShareOutputDir);								 //TODO: BE AWARE!!
	//	writeCountsCompare(events, countsCompareOutputDir);							 //TODO: BE AWARE!!
	//	processEventsAndWriteFileForLinkVolumes(events,flowPatternOutputDir);		 //TODO: BE AWARE!!
	//	writeFileForTravelTimesByMode(events,enterTravelTimesOutputDir);		 //TODO: BE AWARE!!
	//	writeFileForTravelDistanceByMode(events,travelDistanceOutputDir);		 //TODO: BE AWARE!!
		
		it = it + 50;
		itAux = itAux + 50;
		
		}


	}
	
	private static void createDir(File file) {
		file.mkdirs();	
	}
	
	private static void writeLinkLenghts(String networkFile, String outFile){
		Network network = readNetwork( networkFile );
		Link[] links = NetworkUtils.getSortedLinks(network);
		
		try (BufferedWriter writer = IOUtils.getBufferedWriter(outFile)) {
			writer.write("linkId\tLength\tCapacity\n");
			
			for (Link link: links){										
				writer.write(link.getId() +"\t"+  link.getLength() + "\t" + link.getCapacity() + "\n");
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written. Reason "+e );
		}		
	
	}
	
	
	
	private static void writeModalShare(String eventsFile , String outputFile){
			
		File analysisDir = new File(ANALYSIS_DIR);
		if(!analysisDir.exists()) createDir(analysisDir);			
		ModalShareFromEvents msc = new ModalShareFromEvents(eventsFile);
		msc.run();
		msc.writeResults(outputFile);

			

	}
	
	private static void writeCountsCompare (String eventsFile, String outputFile){
		
		File analysisDir = new File(ANALYSIS_DIR);
		if(!analysisDir.exists()) createDir(analysisDir);	
		Network network = readNetwork( NET_FILE );
		Counts counts = readCounts( COUNTS_FILE );
		VolumesAnalyzer volumes = readVolumes( network , eventsFile );
		double scaleFactor;
		if (CASE_NAME.substring(8,10).equals("10")){
			scaleFactor = 10;
		}else{
			scaleFactor = 1;
		}


	
	final CountsComparisonAlgorithm cca =
			new CountsComparisonAlgorithm(
					volumes,
					counts,
					network,
					scaleFactor );

		cca.run();
		
		try {
			final CountSimComparisonTableWriter ctw=
				new CountSimComparisonTableWriter(
						cca.getComparison(),
						Locale.ENGLISH);
			ctw.writeFile( outputFile );
		}
		catch ( Exception e ) {

		}
	}
	
	private static VolumesAnalyzer readVolumes ( Network network, String eventsFile ) {
		final VolumesAnalyzer volumes = new VolumesAnalyzer( 3600 , 24 * 3600 - 1 , network );
		final EventsManager events = EventsUtils.createEventsManager();
		events.addHandler( volumes );
		new MatsimEventsReader( events ).readFile( eventsFile );
		return volumes;
	}
	
	private static Counts readCounts(final String countsFile) {
		final Counts counts = new Counts();
		new MatsimCountsReader( counts ).readFile( countsFile );
		return counts;
	}

	private static Network readNetwork(final String netFile) {
		final Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new MatsimNetworkReader(sc.getNetwork()).readFile( netFile );
		return sc.getNetwork();
	}
	
	/*Trying extracts of Amit's codes to write the link volumes*/
	private static void processEventsAndWriteFileForLinkVolumes(String eventsFile, String outFile){
		
		File analysisDir = new File(ANALYSIS_DIR);
		if(!analysisDir.exists()) createDir(analysisDir);	
		
		/*running*/		
		SantiagoLinkVolumeHandler handler = new SantiagoLinkVolumeHandler();
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(handler);
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
		Map<Id<Link>, Map<Integer, Double>> linksVolumes = handler.getLinksVolumes();
		
		/*writing*/
		try (BufferedWriter writer = IOUtils.getBufferedWriter(outFile)) {
			writer.write("linkId\tTimeSlot\tVolume\n");
			for(Id<Link> l : linksVolumes.keySet()){				
				for (int timeSlot: linksVolumes.get(l).keySet()){
					writer.write(l+"\t"+  timeSlot + "\t" + linksVolumes.get(l).get(timeSlot) + "\n" );
				}
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written. Reason "+e );
		}
	}
	
	
	private static void writeFileForTravelTimesByMode(String eventsFile, String outFile){
		File analysisDir = new File(ANALYSIS_DIR);
		if(!analysisDir.exists()) createDir(analysisDir);
		
		/*running Amit's handler*/
		
		SantiagoModeTripTravelTimeHandler handler = new SantiagoModeTripTravelTimeHandler();
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(handler);
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
		
		
		SortedMap<String, Map<Id<Person>, List<String>>> travelTimesByMode = handler.getLegModePesonIdTripDepartureTravelTimes ();
		
		/*writing*/		
		try (BufferedWriter writer = IOUtils.getBufferedWriter(outFile)) {
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
	
	private static void writeFileForTravelDistanceByMode(String eventsFile, String outFile){
		
		File analysisDir = new File(ANALYSIS_DIR);
		if(!analysisDir.exists()) createDir(analysisDir);
		Config config = ConfigUtils.loadConfig(CONFIG_FILE);
		Network network = readNetwork( NET_FILE );

		/*running Amit's handler*/
		
		SantiagoModeTripTravelDistanceHandler handler = new SantiagoModeTripTravelDistanceHandler(config,network);
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(handler);
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
		
		
		SortedMap<String, Map<Id<Person>, List<String>>> travelDistanceByMode = handler.getMode2PersonId2TravelDistances();
		
		/*writing*/		
		try (BufferedWriter writer = IOUtils.getBufferedWriter(outFile)) {
			writer.write("mode\tpersonId\tdistance\n");
			
			for(String mode : travelDistanceByMode.keySet()){				
				for (Id<Person> person: travelDistanceByMode.get(mode).keySet()){						
						writer.write(mode+"\t"+  person + "\t" + travelDistanceByMode.get(mode).get(person) + "\n");					
					
				}
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written. Reason "+e );
		}
	}
	
	
}
