/* *********************************************************************** *
* project: org.matsim.*
* firstControler
* *
* *********************************************************************** *
* *
* copyright : (C) 2007 by the members listed in the COPYING, *
* LICENSE and WARRANTY file. *
* email : info at matsim dot org *
* *
* *********************************************************************** *
* *
* This program is free software; you can redistribute it and/or modify *
* it under the terms of the GNU General Public License as published by *
* the Free Software Foundation; either version 2 of the License, or *
* (at your option) any later version. *
* See also COPYING, LICENSE and WARRANTY file *
* *
* *********************************************************************** */ 

package playground.ikaddoura.analysis.distanceFromEvents;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;



public class IKAnalysisMain1 {
	
	static String path = "/Users/ihab/Documents/workspace/runs-svn/optAV/output/output_av_0.5_v10000_cn/";

	static String networkFile = path + "output_network.xml.gz";
	static String eventsFile = path + "output_events.xml.gz";
				
	public static void main(String[] args) {
		IKAnalysisMain1 anaMain = new IKAnalysisMain1();
		anaMain.run();
	}

	private void run() {
	
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(networkFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		EventsManager events = EventsUtils.createEventsManager();
		
		IKEventHandler handler1 = new IKEventHandler(scenario.getNetwork());
		events.addHandler(handler1);
		
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
		
		System.out.println("total distance: " + handler1.getTotalDistance());
					
	}
			 
}
		

