package playground.sbraun.actDurationAnalysis;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;


/**
 * @author Soehnke
 *
 */

public class ActDurationAnalysisMain {

	static String eventsFile = "C://Users//braun//Documents//VSP//Skillbuilding//example1//Sample//output//run01//output_events.xml.gz";
				
	public static void main(String[] args) {
		ActDurationAnalysisMain anaMain = new ActDurationAnalysisMain();
		anaMain.run();
	}

	private void run() {
	
		EventsManager events = EventsUtils.createEventsManager();
		ActDurationHandler handler1 = new ActDurationHandler();
		events.addHandler(handler1);
		// add more handlers here
		
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
		
		handler1.output("C://Users//braun//Documents//VSP//Skillbuilding//example1//Sample//ActivityTimes.csv");
					
	}
			 
}
		

