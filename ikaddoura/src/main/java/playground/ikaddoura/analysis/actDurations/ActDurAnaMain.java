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

package playground.ikaddoura.analysis.actDurations;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;



public class ActDurAnaMain {

	static String populationFile = "/Users/ihab/Desktop/plans.xml";
	static String eventsFile = "/Users/ihab/Desktop/events.xml";
				
	public static void main(String[] args) {
		ActDurAnaMain anaMain = new ActDurAnaMain();
		anaMain.run();
	}

	private void run() {
	
		Config config = ConfigUtils.createConfig();
		config.plans().setInputFile(populationFile);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		EventsManager events = EventsUtils.createEventsManager();
		
		ActDurationHandler handler1 = new ActDurationHandler();
		events.addHandler(handler1);
		// add more handlers here
		
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(eventsFile);
		
		handler1.writeOutput(scenario.getPopulation(), "/Users/ihab/Desktop/act-duration.csv", Double.POSITIVE_INFINITY);
		handler1.writeOutput(scenario.getPopulation(), "/Users/ihab/Desktop/act-duration-short-activities.csv", 900.);
					
	}
			 
}
		

