/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package playground.gleich.analysis.drt.operationTime;

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;

/**
 * Runner that guesses configuration based on output directory path (1 arg)
 * Otherwise detailed configuration (7 args[]) is also available.
 * 
 * 
 * 
 * TODO: run without transit schedule, move to contribs/analysis, multiple drt modes (-> stageActivities), test
 * 
 * @author gleich
 *
 */
public class RunDrtOperationtimeCalculator {
	private final static Logger log = Logger.getLogger(RunDrtOperationtimeCalculator.class);
	private final String eventsFile;

	public RunDrtOperationtimeCalculator(String eventsFile) {
		this.eventsFile = eventsFile;
	}

	public static void main(String[] args) {
		RunDrtOperationtimeCalculator runner = new RunDrtOperationtimeCalculator(args[0]);
		runner.calcDrtOperationTime();
	}

	public void calcDrtOperationTime() {
		// Analysis
		EventsManager events = EventsUtils.createEventsManager();

		DrtOperationTimeEventHandler eventHandler = new DrtOperationTimeEventHandler();
		events.addHandler(eventHandler);
		new EventsReaderXMLv1(events).readFile(eventsFile);

		Map<Id<Person>, Double> drtVehDriver2OperationTime = eventHandler.getDrtVehDriver2OperationTime();
		
		double totalOperationTimeHH = 0.0;
		for (Double time: drtVehDriver2OperationTime.values()) {
			totalOperationTimeHH = totalOperationTimeHH + time / 3600;
		}
		System.out.println("num drtVeh: " + drtVehDriver2OperationTime.size() + " operation time: " + totalOperationTimeHH/drtVehDriver2OperationTime.size() + " h / veh, total: " + totalOperationTimeHH + " h");
	}
	
	// Tested with code-examples RunDrtExample: num drtVeh: 3 operation time: 1450.6666666666667 sec / veh, total: 4352.0 sec
	// num drtVeh: 3 operation time: 0.40296296296296297 h / veh, total: 1.208888888888889 h
	
}
