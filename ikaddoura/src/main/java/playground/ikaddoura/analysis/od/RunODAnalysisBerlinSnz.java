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

package playground.ikaddoura.analysis.od;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;

/**
* @author ikaddoura
*/

public class RunODAnalysisBerlinSnz {
	
	public static void main(String[] args) throws IOException {

		final String runId = "snz-drt-0";
		final String runDirectory = "../../runs-svn/avoev/2019-05/output_2019-05-08_snz-drt-0/";
		final String shapeFile = "../../shared-svn/projects/avoev/data/berlkoenig-od-trips/Bezirksregionen_zone_UTM32N/Bezirksregionen_zone_UTM32N_fixed.shp";		
		final String crs = "EPSG:25832";
		final double scaleFactor = 4.;
		final String[] helpLegModes = {TransportMode.transit_walk, TransportMode.access_walk, TransportMode.egress_walk};
		final String stageActivitySubString = "interaction";
		final String zoneId = "NO";
		
		final List<String> modes = new ArrayList<>();
		modes.add(TransportMode.drt);
				
		EventsManager events = EventsUtils.createEventsManager();

		ODEventAnalysisHandler handler1 = new ODEventAnalysisHandler(helpLegModes, stageActivitySubString);
		events.addHandler(handler1);

		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(runDirectory + runId + ".output_events.xml.gz");
		
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(runDirectory + runId + ".output_network.xml.gz");
		config.network().setInputCRS(crs);
		config.global().setCoordinateSystem(crs);
		Network network = ScenarioUtils.loadScenario(config).getNetwork();
		
		ODAnalysis odAnalysis = new ODAnalysis(runDirectory, network, runId, shapeFile, crs , zoneId, modes, scaleFactor);
		odAnalysis.process(handler1);
	}

}

