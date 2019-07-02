/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.kturner.zerocuts;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.io.IOUtils;


class CountVehicles {

	private static enum AnalysisType { v52, v53 }
	private static final AnalysisType analysisType = AnalysisType.v53 ;

	static Logger log = Logger.getLogger(CountVehicles.class);

	public static void main(String[] args) throws Exception {
		log.setLevel(Level.INFO);

		final String inputFileEvents  ;

		switch ( analysisType ) {
		case v52:
			log.info("Analysing Scenario: " + analysisType);
			inputFileEvents  = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.2-10pct/"
					+ "output-berlin-v5.2-10pct/berlin-v5.2-10pct.output_events.xml.gz" ;
			break;
		case v53:
			log.info("Analysing Scenario: " + analysisType);
			inputFileEvents  = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.3-10pct/"
					+ "output-berlin-v5.3-10pct/berlin-v5.3-10pct.output_events.xml.gz" ;
			break;
		default:
			throw new RuntimeException("undefined") ;
		}

		EventsManager eventsManager = EventsUtils.createEventsManager();

		EventHandlerCountVehicles handlerCountVehicles = new EventHandlerCountVehicles();
		eventsManager.addHandler(handlerCountVehicles);

		MatsimEventsReader eventsReader = new MatsimEventsReader(eventsManager);
		eventsReader.readURL( IOUtils.newUrl( null, inputFileEvents ) );

		eventsManager.finishProcessing();
		
		System.out.println("Number of different vehicles (cars) in Events - without freight and transit: " + handlerCountVehicles.getNumberOfCars());
		System.out.println("Number of different freight vehicles in Events: " + handlerCountVehicles.getNumberOfFreightVehicles());
		System.out.println("Number of different transit vehicles in Events: " + handlerCountVehicles.getNumberOfTransitVehicles());

		System.out.println("### Done");

	}

}
