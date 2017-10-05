package playground.santiago.analysis.others;

import java.io.File;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import playground.agarwalamit.analysis.modalShare.ModalShareFromEvents;


public class SantiagoModalSplitAnalysis {
	private String runDir;	
	private String outputDir;
	private String analysisDir;
//	private List<Id<Person>> stuckAgents;
	
	public SantiagoModalSplitAnalysis(String caseName, String stepName/*, List<Id<Person>> stuckAgents*/){
		this.runDir = "../../../runs-svn/santiago/" + caseName + "/";
		this.outputDir = runDir + "outputOf" + stepName + "/";
		this.analysisDir = outputDir + "analysis/";	
//		this.stuckAgents=stuckAgents;
		
	}
	
	private void createDir(File file) {
		file.mkdirs();	
	}
	
	
	/**
	 * writeModalShare uses ModalShareFromEvents from playground.agarwalamit.analysis.modalShare.ModalShareFromEvents. Stuck agents are considered, be aware.
	 */
	public void writeModalShare(int it, int itAux){

		File analysisDir = new File(this.analysisDir);
		if(!analysisDir.exists()) createDir(analysisDir);
		String eventsFile = outputDir + "ITERS/it." + String.valueOf(it) + "/" + String.valueOf(it) + ".events.xml.gz";
		ModalShareFromEvents msc = new ModalShareFromEvents(eventsFile);
		msc.run();
		String outputFile = this.analysisDir + String.valueOf(itAux) + ".modalSplit.txt";
		msc.writeResults(outputFile);


	}
}
