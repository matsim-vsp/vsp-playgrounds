package playground.santiago.analysis.trafficVolumes;

import java.io.BufferedWriter;
import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
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

import playground.santiago.analysis.eventHandlers.trafficVolumes.SantiagoLinkVolumeHandler;




public class SantiagoTrafficVolumesAnalysis {

	private String runDir;	
	private String outputDir;
	private String analysisDir;
//	private List<Id<Person>> stuckAgents;
	
	public SantiagoTrafficVolumesAnalysis (String caseName, String stepName, List<Id<Person>> stuckAgents){

		this.runDir = "../../../runs-svn/santiago/" + caseName + "/";
		this.outputDir = runDir + "outputOf" + stepName + "/";
		this.analysisDir = outputDir + "analysis/";	
//		this.stuckAgents=stuckAgents;
		
	}
	
	private void createDir(File file) {
		file.mkdirs();	
	}
	
	private Network readNetwork(String networkFile){
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(sc.getNetwork()).readFile(networkFile);
		return sc.getNetwork();
	}
	
	public void writeLinkLenghts(){

		String networkFile = this.outputDir + "output_network.xml.gz";
		String outputFile = analysisDir + "linkLengths.txt";

		Network network = readNetwork( networkFile );	
		Link[] links = NetworkUtils.getSortedLinks(network);

		try (BufferedWriter writer = IOUtils.getBufferedWriter(outputFile)) {
			writer.write("linkId\tLength\tCapacity\n");

			for (Link link: links){										
				writer.write(link.getId() +"\t"+  link.getLength() + "\t" + link.getCapacity() + "\n");
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written. Reason "+e );
		}		

	}

	/**
	 * It considers stuck agents. Be aware.
	 * @param it
	 * @param scaleFactor
	 */
	public void writeCountsCompare (int it, int itAux, double scaleFactor){

		File analysisDir = new File(this.analysisDir);
		if(!analysisDir.exists()) createDir(analysisDir);
		
		String networkFile = this.outputDir + "output_network.xml.gz";
		
		String outputFile = this.analysisDir + String.valueOf(itAux) + ".countsCompare.txt";
		
		Network network = readNetwork(networkFile);
		Counts counts = readCounts();
		
		VolumesAnalyzer volumes = readVolumes(it, network);
		
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

	private VolumesAnalyzer readVolumes (int it, Network network) {
		

		String eventsFile = this.outputDir + "ITERS/it." + String.valueOf(it) + "/" + String.valueOf(it) + ".events.xml.gz";
		final VolumesAnalyzer volumes = new VolumesAnalyzer( 3600 , 24 * 3600 - 1 , network );
		final EventsManager events = EventsUtils.createEventsManager();
		events.addHandler( volumes );
		new MatsimEventsReader( events ).readFile( eventsFile );
		return volumes;
	}

	private Counts readCounts() {
		String countsFile = this.outputDir + "output_counts.xml.gz";
		final Counts counts = new Counts();
		new MatsimCountsReader( counts ).readFile( countsFile );
		return counts;
	}

	
	public void writeFileForLinkVolumes(int it, int itAux){

		File analysisDir = new File(this.analysisDir);
		if(!analysisDir.exists()) createDir(analysisDir);

		String eventsFile = this.outputDir + "ITERS/it." + String.valueOf(it) + "/" + String.valueOf(it) + ".events.xml.gz";
		String outputFile = analysisDir + String.valueOf(itAux) + ".linksVolumes.txt";

		SantiagoLinkVolumeHandler handler = new SantiagoLinkVolumeHandler();
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(handler);
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
		Map<Id<Link>, Map<Integer, Double>> linksVolumes = handler.getLinksVolumes();


		try (BufferedWriter writer = IOUtils.getBufferedWriter(outputFile)) {
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


}
