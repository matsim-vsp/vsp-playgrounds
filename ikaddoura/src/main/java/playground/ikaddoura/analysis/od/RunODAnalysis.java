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
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.sav.analysis.od.ODAnalysis;

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
		
		final String shapeFile = rootDirectory + "public-svn/matsim/scenarios/countries/de/berlin/projects/avoev/berlin-sav-v5.2-10pct/input/shp-berlin-hundekopf-areas/berlin_hundekopf.shp";
		final String runId = "berlin-v5.2-10pct";
		final String runDirectory = rootDirectory + "public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.2-10pct/output-berlin-v5.2-10pct/";
		
		final StageActivityTypes stageActivities = new StageActivityTypesImpl("pt interaction", "car interaction", "ride interaction", "drt interaction");
		final String zoneId = "SCHLUESSEL";
		final List<String> modes = new ArrayList<>();
		modes.add(TransportMode.drt);
				
		ODAnalysis reader = new ODAnalysis(runDirectory, runDirectory, runId, shapeFile, zoneId, stageActivities, modes);
		reader.run();
	}

}

