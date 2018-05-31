package playground.santiago.analysis.others;

import java.io.BufferedWriter;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.io.IOUtils;

import playground.santiago.analysis.eventHandlers.others.SantiagoStuckAndAbortEventHandler;

public class SantiagoStuckAgentsAnalysis {
	
	private String runDir;	
	private String outputDir;
	private String analysisDir;
	
	public SantiagoStuckAgentsAnalysis(String caseName, String stepName){

		this.runDir = "../../runs-svn/santiago/" + caseName + "/";
		this.outputDir = runDir + "outputOf" + stepName + "/";
		this.analysisDir = outputDir + "analysis/";	

	}
	
	private void createDir(File file) {
		file.mkdirs();	
	}
	
	public void writeStuckEvents(int it, int itAux){
		File analysisDir = new File(this.analysisDir);
		if(!analysisDir.exists()) createDir(analysisDir);

		String eventsFile = outputDir + "ITERS/it." + String.valueOf(it) + "/" + String.valueOf(it) + ".events.xml.gz";

		SantiagoStuckAndAbortEventHandler handler = new SantiagoStuckAndAbortEventHandler();
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(handler);
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);

		SortedMap<String, Map<Id<Person>, List<Double>>> mode2PersonId2Times = handler.getMode2IdAgentsStuck2Time();

		String outputFile = this.analysisDir + String.valueOf(itAux) + ".modeStuckAgents.txt";
		try (BufferedWriter writer = IOUtils.getBufferedWriter(outputFile)) {
			writer.write("mode\tpersonId\teventTime\n");
			for(String mode : mode2PersonId2Times.keySet()){				
				for (Id<Person> person: mode2PersonId2Times.get(mode).keySet()){
					for (double eventTime: mode2PersonId2Times.get(mode).get(person)){
						writer.write(mode+"\t"+  person + "\t" + eventTime + "\n");		
					}					
				}
			}

			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written. Reason "+e );
		}
	}

	public List<Id<Person>> getStuckAgents (int it){
		String eventsFile = outputDir + "ITERS/it." + String.valueOf(it) + "/" + String.valueOf(it) + ".events.xml.gz";
		SantiagoStuckAndAbortEventHandler handler = new SantiagoStuckAndAbortEventHandler();
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(handler);
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);		
		return handler.getAgentsStuck();
	}
}
