package playground.santiago.analysis.travelDistances;


import java.io.BufferedWriter;
import java.io.File;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.utils.io.IOUtils;

import playground.santiago.analysis.eventHandlers.travelDistances.SantiagoOSMDistanceHandler;


public class SantiagoOSMDistancesAnalysis {

	private String runDir;	
	private String outputDir;
	private String analysisDir;
	private String shapeFile;
	private List<Id<Person>> stuckAgents;

	public SantiagoOSMDistancesAnalysis(String caseName, String stepName, List<Id<Person>> stuckAgents,String shapeFile) {		
		this.runDir = "../../runs-svn/santiago/" + caseName + "/";
		this.outputDir = runDir + "outputOf" + stepName + "/";
		this.analysisDir = outputDir + "analysis/";	
		this.stuckAgents=stuckAgents;
		this.shapeFile = shapeFile;
	}

	private void createDir(File file) {
		file.mkdirs();	
	}

	public void writeFileForTravelDistancesByCategory(int it, int itAux){

		File analysisDir = new File(this.analysisDir);
		if(!analysisDir.exists()) createDir(analysisDir);

		String netFile = outputDir + "output_network.xml.gz";
		String eventsFile = outputDir + "ITERS/it." + String.valueOf(it) + "/" + String.valueOf(it) + ".events.xml.gz";
		String outputFile = this.analysisDir + "linksCategories/" + String.valueOf(itAux) + ".travelDistancesByOSMCategory.txt";

		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(netFile);
		
		SantiagoOSMDistanceHandler handler = new SantiagoOSMDistanceHandler(network, stuckAgents, shapeFile);
		
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(handler);
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
		
		double sum_trunk = handler.getTrunkDistance();
		double sum_motorway = handler.getMotorwayDistance();
		double sum_trunk_primary_link = handler.getTrunkPrimaryLinkDistance();
		double sum_primary = handler.getPrimaryDistance();
		double sum_motorway_link = handler.getMotorwayLinkDistance();
		double sum_secondary = handler.getSecondaryDistance();
		double sum_tertiary = handler.getTertiaryDistance();
		
		try (BufferedWriter writer = IOUtils.getBufferedWriter(outputFile)) {
			
			writer.write("category\ttotal_distance\n");
			writer.write("trunk\t" + String.valueOf(sum_trunk) + "\n");
			writer.write("motorway\t" + String.valueOf(sum_motorway) + "\n");			
			writer.write("trunk_link_primary_link\t" + String.valueOf(sum_trunk_primary_link) + "\n");			
			writer.write("primary\t" + String.valueOf(sum_primary) + "\n");			
			writer.write("motorway_link\t" + String.valueOf(sum_motorway_link) + "\n");
			writer.write("secondary\t" + String.valueOf(sum_secondary) + "\n");
			writer.write("tertiary\t" + String.valueOf(sum_tertiary) + "\n");
			writer.close();
			
		} catch (Exception e) {
			throw new RuntimeException("Data is not written. Reason "+e );
		}

	}


}
