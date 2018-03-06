/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.ikaddoura.berlin;

import java.io.IOException;

import org.apache.log4j.Logger;

import playground.ikaddoura.analysis.IKAnalysisRun;


public class IKAnalysisRunBerlin {
	private static final Logger log = Logger.getLogger(IKAnalysisRunBerlin.class);
			
	public static void main(String[] args) throws IOException {
			
		final String runDirectory = "/Users/ihab/Documents/workspace/runs-svn/open_berlin_scenario/be_300_c/";
		final String runId = "be_300_c";
		final String runDirectoryToCompareWith = null;
		final String runIdToCompareWith = null;
		final String scenarioCRS = null;	
		final String shapeFileZones = null;
		final String zonesCRS = null;
		final String homeActivity = null;
		final int scalingFactor = 10;
		
		IKAnalysisRun analysis = new IKAnalysisRun(runDirectory,
				runId,
				runDirectoryToCompareWith,
				runIdToCompareWith,
				scenarioCRS,
				shapeFileZones,
				zonesCRS,
				homeActivity,
				scalingFactor);
		analysis.run();
	
		log.info("Done.");
	}
}
		

