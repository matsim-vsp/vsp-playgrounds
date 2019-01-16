/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DefaultControlerModules.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package scenarios.illustrative.parallel.run;

import java.util.Calendar;

import optimize.cten.convert.Matsim2cten.TtMatsim2KS2015;

/**
 * Class to convert the parallel scenario into KS format.
 * Uses the general conversion tool TtMatsim2KS2015.
 * 
 * @author tthunig 
 */
public class ConvertParallel2KS {

	public static void main(String[] args) throws Exception {
		String inputDir = "../../shared-svn/projects/cottbus/data/scenarios/parallel_scenario/parallel_secondOD_greenWaves_interG3_200m_15ms_branchPulk_longInitLinks/";
		// input files
		String signalSystemsFilename = inputDir + "signal_systems.xml";
		String signalGroupsFilename = inputDir + "signal_groups.xml";
		String signalControlFilename = inputDir + "signal_control.xml";
		String networkFilename = inputDir + "network.xml";
		String lanesFilename = inputDir + "lanes.xml";
		String populationFilename = inputDir + "plans1360.xml";

		// output files
		String outputDirectory = "../../shared-svn/projects/cottbus/data/optimization/parallel2ks/";
		
		// get the current date in format "yyyy-mm-dd"
		Calendar cal = Calendar.getInstance();
		// this class counts months from 0, but days from 1
		int month = cal.get(Calendar.MONTH) + 1;
		String monthStr = month + "";
		if (month < 10)
			monthStr = "0" + month;
		String date = cal.get(Calendar.YEAR) + "-"	+ monthStr + "-" + cal.get(Calendar.DAY_OF_MONTH);
				
		/* parameters for the time interval */
		double startTime = 8 * 3600.0;
		double endTime = 9 * 3600.0;
		/* parameters for the network area */
		double signalsBoundingBoxOffset = 1500; // no node is more than 1500m away from another node in this network
		double cuttingBoundingBoxOffset = 1500;
		/* parameters for the interior link filter */
		double freeSpeedFilter = 1.0; // = default value
		boolean useFreeSpeedTravelTime = true; // = default value
		double maximalLinkLength = Double.MAX_VALUE; // = default value
		/* parameters for the demand filter */
		double matsimPopSampleSize = 1.0; // = default value
		double ksModelCommoditySampleSize = 1.0; // = default value
		double minCommodityFlow = 1.0; // = default value
		boolean simplifyNetwork = false;
		int cellsX = 5; // = default value
		int cellsY = 5; // = default value
		/* other parameters */
		String scenarioDescription = "parallel network; 1620 agents; capacity 3600; green wave appropriate travel times; signal groups for oncoming traffic; 3s intergreen time; 60s cycle time";

		TtMatsim2KS2015.convertMatsim2KS(signalSystemsFilename,
				signalGroupsFilename, signalControlFilename, null, networkFilename,
				lanesFilename, populationFilename, startTime, endTime,
				signalsBoundingBoxOffset, cuttingBoundingBoxOffset,
				freeSpeedFilter, useFreeSpeedTravelTime, maximalLinkLength,
				matsimPopSampleSize, ksModelCommoditySampleSize,
				minCommodityFlow, simplifyNetwork, cellsX, cellsY, scenarioDescription,
				date, outputDirectory);
	}
}
