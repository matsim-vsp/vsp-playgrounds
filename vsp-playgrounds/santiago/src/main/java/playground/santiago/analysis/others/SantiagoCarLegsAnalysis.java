package playground.santiago.analysis.others;

import java.io.BufferedWriter;
import java.io.File;
import java.util.List;
import java.util.SortedMap;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.io.IOUtils;

import playground.santiago.analysis.eventHandlers.others.SantiagoCarLegsByStartTimeEventHandler;



public class SantiagoCarLegsAnalysis {
	private String runDir;	
	private String outputDir;
	private String analysisDir;
//	private List<Id<Person>> stuckAgents;
	
	public SantiagoCarLegsAnalysis(String caseName, String stepName/*, List<Id<Person>> stuckAgents*/){
		this.runDir = "../../../runs-svn/santiago/" + caseName + "/";
		this.outputDir = runDir + "outputOf" + stepName + "/";
		this.analysisDir = outputDir + "analysis/";	
//		this.stuckAgents=stuckAgents;
		
	}
	
	private void createDir(File file) {
		file.mkdirs();	
	}
	
	public void writeCarLegs(int it, int itAux){

		File analysisDir = new File(this.analysisDir);
		if(!analysisDir.exists()) createDir(analysisDir);
		String eventsFile = outputDir + "ITERS/it." + String.valueOf(it) + "/" + String.valueOf(it) + ".events.xml.gz";		
		SantiagoCarLegsByStartTimeEventHandler handler = new SantiagoCarLegsByStartTimeEventHandler();
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(handler);
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);

		SortedMap<String,List<Double>> carLegs2StartTime = handler.getCarLegs2StartTime();				

		String outputFile = this.analysisDir + String.valueOf(itAux) + ".carLegs2startTime.txt";

		try (BufferedWriter writer = IOUtils.getBufferedWriter(outputFile)) {
			writer.write("mode\tstartTime\n");
			for(String mode : carLegs2StartTime.keySet()){
				for (double startTime: carLegs2StartTime.get(mode)){
					writer.write(mode+"\t"+ startTime + "\n");
				}
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written. Reason "+e );
		}


	}
	
	
}
