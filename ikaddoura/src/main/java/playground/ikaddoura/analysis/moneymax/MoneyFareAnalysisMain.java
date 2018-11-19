/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.ikaddoura.analysis.moneymax;

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

/**
 * @author ikaddoura
 *
 */
public class MoneyFareAnalysisMain {
	private static final Logger log = Logger.getLogger(MoneyFareAnalysisMain.class);

	private String outputDirectory = "/Users/ihab/Documents/workspace/runs-svn/b5-congestion-pricing/output_p10_rtm_13/";
	private String runId = "p10_rtm_13";
	
	public static void main(String[] args) {
		MoneyFareAnalysisMain anaMain = new MoneyFareAnalysisMain();
		anaMain.run();		
	}

	private void run() {
		
		if (!outputDirectory.endsWith("/")) {
			outputDirectory = outputDirectory + "/";
		}
			
		EventsManager eventsManager = EventsUtils.createEventsManager();
		
		MoneyFareHandler handler = new MoneyFareHandler();
		eventsManager.addHandler(handler);
		
		MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
		reader.readFile(outputDirectory + runId + ".output_events.xml.gz");
		
		log.info("runId: " + runId + " // maximum toll payment: " + handler.getMaxToll());
	} 
}
		

