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
package scenarios.illustrative.braess.conversion;

import java.util.Calendar;

import optimize.cten.convert.Matsim2cten.TtMatsim2KS2015;

/**
 * Class to convert the Braess scenario into KS format.
 * Uses the general conversion tool TtMatsim2KS2015.
 * 
 * @author tthunig 
 */
public class ConvertBraess2KS2015 {

	public static void main(String[] args) throws Exception {
		String inputDir = "../../shared-svn/projects/cottbus/data/scenarios/braess_scenario/cap3600-1800_noSignals/";
		// input files
		String signalSystemsFilename = inputDir + "signalSystems.xml";
		String signalGroupsFilename = inputDir + "signalGroups.xml";
		String signalControlFilename = inputDir + "signalControl.xml";
		String networkFilename = inputDir + "network.xml";
//		String lanesFilename = inputDir + "realisticLanes.xml";
		String lanesFilename = null;
		String populationFilename = inputDir + "plans3600.xml";

		// output files
		String outputDirectory = "../../shared-svn/projects/cottbus/data/optimization/braess2ks/";
		
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
		double endTime = 9 * 3600.0 + 24 * 60;
		/* parameters for the network area */
		double signalsBoundingBoxOffset = 650; // the maximal distance between a node and the signal at node 4 is 600m
		double cuttingBoundingBoxOffset = 650;
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
		String scenarioDescription = "braess with 3600 agents, capacity 1800 and one all green signal";

		TtMatsim2KS2015.convertMatsim2KS(signalSystemsFilename,
				signalGroupsFilename, signalControlFilename, networkFilename,
				lanesFilename, populationFilename, startTime, endTime,
				signalsBoundingBoxOffset, cuttingBoundingBoxOffset,
				freeSpeedFilter, useFreeSpeedTravelTime, maximalLinkLength,
				matsimPopSampleSize, ksModelCommoditySampleSize,
				minCommodityFlow, simplifyNetwork, cellsX, cellsY, scenarioDescription,
				date, outputDirectory);
	}
}
