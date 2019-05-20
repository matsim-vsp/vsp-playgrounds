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

/**
* @author ikaddoura
*/

public class RunODAnalysis {
	
	public static void main(String[] args) throws IOException {

		final String runId = "snz-drt-0";
		final String runDirectory = "../runs-svn/avoev/2019-05/output_2019-05-08_snz-drt-0/";
		final String shapeFile = "../shared-svn/projects/avoev/data/berlkoenig-od-trips/Bezirksregionen_zone_UTM32N/Bezirksregionen_zone_UTM32N_fixed.shp";		
		final String crs = "EPSG:25832";
		final double scaleFactor = 4.;
		final String[] helpLegModes = {TransportMode.transit_walk, TransportMode.access_walk, TransportMode.egress_walk};
		final String stageActivitySubString = "interaction";
		final String zoneId = "NO";
		
		final List<String> modes = new ArrayList<>();
		modes.add(TransportMode.drt);
				
		ODAnalysis reader = new ODAnalysis(runDirectory, runDirectory, runId, shapeFile, crs , zoneId, modes, helpLegModes, stageActivitySubString, scaleFactor);
		reader.run();
	}

}

