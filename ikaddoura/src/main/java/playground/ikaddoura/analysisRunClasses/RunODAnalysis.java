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

package playground.ikaddoura.analysisRunClasses;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.matsim.analysis.od.ODAnalysis;
import org.matsim.analysis.od.ODEventAnalysisHandler;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

/**
* @author ikaddoura
*/

public class RunODAnalysis {
	
	public static void main(String[] args) throws IOException {
		
		String rootDirectory = null;
		
		if (args.length == 1) {
			rootDirectory = args[0];
		} else {
			throw new RuntimeException("Please set the root directory. Aborting...");
		}
		
		if (!rootDirectory.endsWith("/")) rootDirectory = rootDirectory + "/";

		final String runDirectory = rootDirectory + "public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.2-10pct/output-berlin-v5.2-10pct/";
		final String runId = "berlin-v5.2-10pct";
		final String shapeFile = rootDirectory + "public-svn/matsim/scenarios/countries/de/berlin/projects/avoev/berlin-sav-v5.2-10pct/input/shp-berlin-hundekopf-areas/berlin_hundekopf.shp";
		final String[] helpLegModes = {TransportMode.transit_walk, TransportMode.access_walk, TransportMode.egress_walk};
		final String stageActivitySubString = "interaction";
		
		final String zoneId = "SCHLUESSEL";
		final List<String> modes = new ArrayList<>();
//		modes.add(TransportMode.car);
//		modes.add(TransportMode.pt);
//		modes.add(TransportMode.walk);
//		modes.add("bicycle");
//		modes.add(TransportMode.ride);
				
		EventsManager events = EventsUtils.createEventsManager();

		ODEventAnalysisHandler handler1 = new ODEventAnalysisHandler(helpLegModes, stageActivitySubString);
		events.addHandler(handler1);

		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(runDirectory + runId + ".output_events.xml.gz");
		
		ODAnalysis odAnalysis = new ODAnalysis(runDirectory, null, runId, shapeFile, "GK4", zoneId, modes, 10.);
		odAnalysis.process(handler1);
		
	}

}

