package playground.santiago.analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import contrib.baseline.lib.NetworkUtils;
import playground.santiago.analysis.eventHandlers.others.SantiagoStuckAndAbortEventHandler;
import playground.santiago.analysis.eventHandlers.travelDistances.SantiagoModeTripTravelDistanceHandler;
import playground.santiago.analysis.travelDistances.SantiagoPTDistanceFromPlans;


public class Santiago10pctDistanceAnalysis {

//	final static String CASE_NAME = "baseCase10pct";
//	final static String STEP_NAME = "Step1";

//	final static String RUN_DIR = "../../../runs-svn/santiago/" + CASE_NAME + "/";
//	final static String OUTPUT_DIR = RUN_DIR + "outputOf" + STEP_NAME + "/";
//	final static String ANALYSIS_DIR = OUTPUT_DIR + "analysis/";

//	final static int FIRST_IT = 100;
//	final static int LAST_IT = 400;
	
	static String RUN_DIR;
	static String OUTPUT_DIR;
	static String ANALYSIS_DIR;

	public static void main(String[]args){
		
		String CASE_NAME = args[0];
		String STEP_NAME = args[1];
		
		RUN_DIR="/net/ils4/lcamus/runs-svn/santiago/" + CASE_NAME + "/";
		OUTPUT_DIR = RUN_DIR + "outputOf" + STEP_NAME + "/";
		ANALYSIS_DIR = OUTPUT_DIR + "analysis/";
		
		int IT_TO_EVALUATE = Integer.parseInt(args[2]);
		int REFERENCE_IT = Integer.parseInt(args[3]);
			
		
		int it=IT_TO_EVALUATE;
		int itAux=REFERENCE_IT+IT_TO_EVALUATE;
//		while(itAux<=LAST_IT){
			List<Id<Person>> stuckAgents = getStuckAgents(it);
			writeFileForNonPublicLegDistances(it,itAux,stuckAgents);
			writeFileForPublicLegsDistances(it,itAux,stuckAgents);
//			it+=50;
//			itAux+=50;
//		}
	}


	private static void createDir(File file) {
		file.mkdirs();	
	}

	private static List<Id<Person>> getStuckAgents(int it){


		String eventsFile = OUTPUT_DIR + "ITERS/it." + it + "/" + it + ".events.xml.gz";

		SantiagoStuckAndAbortEventHandler stuckHandler = new SantiagoStuckAndAbortEventHandler();
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(stuckHandler);
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);		
		return stuckHandler.getAgentsStuck();
	}

	private static void writeFileForNonPublicLegDistances(int it, int itAux, List<Id<Person>> stuckAgents){	

		File analysisDir = new File(ANALYSIS_DIR);
		if(!analysisDir.exists()) createDir(analysisDir);
		
		String configFile = OUTPUT_DIR + "output_config.xml.gz";
		Config config = ConfigUtils.loadConfig(configFile);
		String netFile = OUTPUT_DIR + "output_network.xml.gz";
		Network network = NetworkUtils.readNetwork(netFile);
		String eventsFile = OUTPUT_DIR + "ITERS/it." + it + "/" + it + ".events.xml.gz";


		SantiagoModeTripTravelDistanceHandler handler = new SantiagoModeTripTravelDistanceHandler(config, network, stuckAgents);
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(handler);
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);		

		SortedMap<String,Map<Id<Person>,List<String>>> privateTravelDistanceByMode = handler.getMode2PersonId2TravelDistances();


		String outputFile = ANALYSIS_DIR + itAux + ".nonPublicModeTravelDistances.txt";

		try (BufferedWriter writer = IOUtils.getBufferedWriter(outputFile)) {
			writer.write("mode\tpersonId\tstartTime-distance\n");

			for(String mode : privateTravelDistanceByMode.keySet()){				
				for (Id<Person> person: privateTravelDistanceByMode.get(mode).keySet()){
					for (String distances: privateTravelDistanceByMode.get(mode).get(person)){
						writer.write(mode+"\t"+  person + "\t" + distances + "\n");					
					}	
				}
			}

			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written. Reason "+e );
		}

	}

	private static void writeFileForPublicLegsDistances(int it, int itAux, List<Id<Person>> stuckAgents){

		String popFile = OUTPUT_DIR + "ITERS/it." + String.valueOf(it) + "/" + String.valueOf(it) + ".plans.xml.gz";
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader popReader = new PopulationReader(scenario);
		popReader.readFile(popFile);
		Population population =  scenario.getPopulation();

		String outputFile = ANALYSIS_DIR + itAux + ".publicModeTravelDistances.txt";

		SantiagoPTDistanceFromPlans ptDistances = new SantiagoPTDistanceFromPlans(population);
		SortedMap<String,Map<Id<Person>,List<String>>> publicTravelDistanceByMode = ptDistances.getPt2PersonId2TravelDistances(stuckAgents);

		try (BufferedWriter writer = IOUtils.getBufferedWriter(outputFile)) {
			writer.write("mode\tpersonId\tstartTime-distance\n");

			for(String mode : publicTravelDistanceByMode.keySet()){				
				for (Id<Person> person: publicTravelDistanceByMode.get(mode).keySet()){
					for (String distances: publicTravelDistanceByMode.get(mode).get(person)){
						writer.write(mode+"\t"+  person + "\t" + distances + "\n");					
					}	
				}
			}

			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written. Reason "+e );
		}


	}

}
