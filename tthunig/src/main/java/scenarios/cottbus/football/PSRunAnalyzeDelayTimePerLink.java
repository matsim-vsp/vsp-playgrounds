package scenarios.cottbus.football;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkReaderMatsimV2;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;

import playground.dgrether.analysis.simsimanalyser.TtSimSimTrafficAnalyser;
import playground.dgrether.koehlerstrehlersignal.analysis.TtDelayPerLink;
import playground.dgrether.koehlerstrehlersignal.analysis.TtTotalDelay;

public class PSRunAnalyzeDelayTimePerLink {

	public PSRunAnalyzeDelayTimePerLink() {
	}
	
	public static void main(String[] args) throws IOException {
		String [] baseDirs = { "/home/pschade/Studium/MA/gitlab/runs-svn/cottbus/football/fertig/fixedNetworktrue_flowCap1.0_longLanestrue_stuckTime900_timeBinSize900_signalControl-LAEMMER_NICO/run1200/laemmer_nicoGroups_MSidealPlans"};

				//"/home/pschade/Studium/MA/gitlab/runs-svn/cottbus/football/fertig/fixedNetworktrue_flowCap1.0_longLanestrue_stuckTime900_timeBinSize900_signalControl-FIXED/run1200/fixedTime_MSidealPlans", 
//				"/home/pschade/Studium/MA/gitlab/runs-svn/cottbus/football/fertig/fixedNetworktrue_flowCap1.0_longLanestrue_stuckTime900_timeBinSize900_signalControl-FIXED_IDEAL/run1200/fixedTimeIdeal_MSidealPlans", 
//				"/home/pschade/Studium/MA/gitlab/runs-svn/cottbus/football/fertig/fixedNetworktrue_flowCap1.0_longLanestrue_stuckTime900_timeBinSize900_signalControl-LAEMMER_NICO/run1200/laemmer_nicoGroups_MSidealPlans", 
//				"/home/pschade/Studium/MA/gitlab/runs-svn/cottbus/football/fertig/fixedNetworktrue_flowCap1.0_longLanestrue_stuckTime900_timeBinSize900_signalControl-LAEMMER_FULLY_ADAPTIVE-USE_MAX_LANECOUNT/run1200/laemmer_fullyAdaptive_MSidealPlans", 
//				"/home/pschade/Studium/MA/gitlab/runs-svn/cottbus/football/fertig/fixedNetworktrue_flowCap1.0_longLanestrue_stuckTime900_timeBinSize900_signalControl-LAEMMER_FULLY_ADAPTIVE-PRIORIZE_HIGHER_POSITIONS/run1200/laemmer_fullyAdaptive_MSidealPlans", 
//				"/home/pschade/Studium/MA/gitlab/runs-svn/cottbus/football/fertig/fixedNetworktrue_flowCap1.0_longLanestrue_stuckTime900_timeBinSize900_signalControl-LAEMMER_FULLY_ADAPTIVE-HEURISTIC/run1200/laemmer_fullyAdaptive_MSidealPlans", 
//				"/home/pschade/Studium/MA/gitlab/runs-svn/cottbus/football/fertig/fixedNetworktrue_flowCap1.0_longLanestrue_stuckTime900_timeBinSize900_signalControl-LAEMMER_FULLY_ADAPTIVE-COMBINE_SIMILAR_REGULATIONTIME/run1200/laemmer_fullyAdaptive_MSidealPlans"};//,
//				"/home/pschade/Studium/MA/gitlab/runs-svn/cottbus/football/fertig/fixedNetworktrue_flowCap1.0_longLanestrue_stuckTime900_timeBinSize900_signalControl-SYLVIA_IDEAL/run1200/sylviaIdeal_maxExt1.5_fixedCycle_MSidealPlans"};//,
//				"/home/pschade/Studium/MA/gitlab/runs-svn/cottbus/football/fertig/fixedNetworktrue_flowCap1.0_longLanestrue_stuckTime900_timeBinSize900_signalControl-NONE/run1200/noSignals_MSidealPlans"};
		Arrays.asList(baseDirs).parallelStream().forEach(baseDirRoot->{
			for (int numOfFootballFans=0; numOfFootballFans<=100; numOfFootballFans+=5) {
				String basedir = baseDirRoot+"/"+numOfFootballFans+"_football_fans/";
				Network network = loadNetwork(basedir+"1200_"+numOfFootballFans+"_football_fans.output_network.xml.gz");
				
				//PSDelayCountPerLink analyzer = new PSDelayCountPerLink(network);
				TtDelayPerLink analyzer = new TtDelayPerLink(network);
				EventsManager events = EventsUtils.createEventsManager();
				events.addHandler(analyzer);
				MatsimEventsReader reader = new MatsimEventsReader(events);
				reader.readFile(basedir+"1200_"+numOfFootballFans+"_football_fans.output_events.xml.gz");
		
				FileWriter writer;
				try {
					String outFile = baseDirRoot+"/sumOfDelayesOnSignalizedLinks.csv";
					if(Files.notExists(Paths.get(outFile))) {
						Files.createFile(Paths.get(outFile));
					}
					writer = new FileWriter(outFile, true);
					String[] linkNames = {"10218", "169", "5377", "314", "1451", "8213", "9653", "1281", "2475", "9347", "5153", "7919", "1449", "9345", "3026", "4909", "3503", "6216", "40", "9969", "8342", "10101", "6230", "8251", "6280", "10517", "10616", "6663", "6067", "8952", "10019", "5888", "7871", "5884", "5772", "2759", "583", "6711", "6708", "588", "4587", "1680", "561", "1685", "1760", "9648", "4586", "138", "1777", "10284", "5744", "139", "295", "7414", "7427", "7300", "2515", "1503", "9761", "7428", "1502", "3409", "6015", "7829", "5892", "2501", "3029", "2781", "3416", "3411", "5930", "1756", "6495", "4932", "4510", "6782", "6779", "3874", "7534", "7529", "5934", "593500", "2498", "6667", "5621"};
					Collection<Id<Link>> analyzeLinkIds = new HashSet<>();
					for (String linkName : linkNames){
						analyzeLinkIds.add(Id.createLinkId(linkName));
					}
					Double delaySum = analyzer.getDelayPerLink().entrySet().parallelStream().filter(e->analyzeLinkIds.contains(e.getKey())).mapToDouble(e->e.getValue()).sum();
					writer.write(numOfFootballFans+";"+delaySum+"\n");

					writer.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});
	}
	
	/** @author  tthunig
	 * 
	 * @param networkFile
	 * @return
	 */
	private static Network loadNetwork(String networkFile){
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);
		return scenario.getNetwork();
	}
	
}
