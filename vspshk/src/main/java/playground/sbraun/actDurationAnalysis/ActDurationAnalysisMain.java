package playground.sbraun.actDurationAnalysis;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;



public class IKAnalysisMain1 {

	static String configFile = "C:\\Users\\braun\\Documents\\VSP\\Skillbuilding\\example1\\Sample\\config.xml";
	static String eventsFile = "C:\\Users\\braun\\Documents\\VSP\\Skillbuilding\\example1\\Sample\\output\\run01\\output_events.xml.gz";
				
	public static void main(String[] args) {
		IKAnalysisMain1 anaMain = new IKAnalysisMain1();
		anaMain.run();
	}

	private void run() {
	
		Config config = ConfigUtils.loadConfig(configFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		EventsManager events = EventsUtils.createEventsManager();
		
		IKEventHandler handler1 = new IKEventHandler(scenario.getNetwork());
		events.addHandler(handler1);
		// add more handlers here
		
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
		
		handler1.output("C:\\\\Users\\\\braun\\\\Documents\\\\VSP\\\\Skillbuilding\\example1\\Sample\\ActivityTimes.csv");
					
	}
			 
}
		

