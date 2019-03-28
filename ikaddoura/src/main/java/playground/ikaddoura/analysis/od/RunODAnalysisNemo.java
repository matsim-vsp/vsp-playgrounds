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

import org.matsim.analysis.od.ODAnalysis;
import org.matsim.api.core.v01.TransportMode;

/**
* @author ikaddoura
*/

public class RunODAnalysisNemo {
	
	public static void main(String[] args) throws IOException {
				
		final String runDirectory = "/Users/ihab/Desktop/nemo/without-bridge/";
		final String runId = "nemo_bike-Highways_001";

//		final String shapeFile = "/Users/ihab/Documents/workspace/shared-svn/projects/nemo_mercator/data/original_files/shapeFiles/plzBasedPopulation/plz-gebiete_Ruhrgebiet/sameCRS/plz.shp";		
		final String shapeFile = "/Users/ihab/Documents/workspace/shared-svn/projects/nemo_mercator/data/original_files/shapeFiles/grids/grid4/grid4.shp";		

		final String[] helpLegModes = {TransportMode.transit_walk, TransportMode.access_walk, TransportMode.egress_walk};
		final String stageActivitySubString = "interaction";
		
		final String zoneId = "ID";
		
		final List<String> modes = new ArrayList<>();
//		modes.add(TransportMode.car);
				
		ODAnalysis reader = new ODAnalysis(runDirectory, runDirectory, runId, shapeFile, "EPSG:25832", zoneId, modes, helpLegModes, stageActivitySubString, 100.);
		reader.run();
	}

}

