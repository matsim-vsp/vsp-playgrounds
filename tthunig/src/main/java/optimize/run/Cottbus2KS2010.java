/* *********************************************************************** *
 * project: org.matsim.*
 * DgCb2Ks2010
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package optimize.run;

import optimize.cten.convert.TtMatsim2KS2015;
import utils.OutputUtils;

/**
 * Class to convert the Cottbus scenario into KS format.
 * Uses the general conversion tool TtMatsim2KS2015.
 * 
 * @author dgrether
 * @author tthunig
 * 
 */
public class Cottbus2KS2010 {

	public static void main(String[] args) throws Exception {
		// input files
		String signalSystemsFilename = "../../shared-svn/projects/cottbus/data/scenarios/cottbus_scenario/signal_systems_no_13.xml";
//		String signalGroupsFilename = "../../shared-svn/projects/cottbus/data/scenarios/cottbus_scenario/signal_groups_no_13.xml";
//		String signalGroupsFilename = "../../shared-svn/projects/cottbus/data/scenarios/cottbus_scenario/signal_groups_laemmerLinkBased.xml";
		String signalGroupsFilename = "../../shared-svn/projects/cottbus/data/scenarios/cottbus_scenario/signal_groups_laemmer2phases_6.xml";
//		String signalControlFilename = "../../shared-svn/projects/cottbus/data/scenarios/cottbus_scenario/signal_control_no_13.xml";
		String signalControlFilename = "../../shared-svn/projects/cottbus/data/scenarios/cottbus_scenario/signal_control_laemmer.xml";
		String networkFilename = "../../shared-svn/projects/cottbus/data/scenarios/cottbus_scenario/network_wgs84_utm33n.xml.gz";
		String lanesFilename = "../../shared-svn/projects/cottbus/data/scenarios/cottbus_scenario/lanes.xml";
		// change run number here to use another base case
//		String populationFilename = "../../runs-svn/cottbus/before2015/run1728/1728.output_plans.xml.gz";
		String populationFilename = "../../shared-svn/projects/cottbus/data/scenarios/cottbus_scenario/cb_spn_gemeinde_nachfrage_landuse_woMines/"
				+ "commuter_population_wgs84_utm33n_car_only_100it_MS_cap0.7.xml.gz";

		// output files
		String outputDirectory = "../../shared-svn/projects/cottbus/data/optimization/cb2ks2010/";
		String dateFormat = OutputUtils.getCurrentDate();

		/* parameters for the time interval */
		double startTime = 5.5 * 3600.0;
		double endTime = 9.5 * 3600.0;
		// double startTime = 13.5 * 3600.0;
		// double endTime = 18.5 * 3600.0;
		/* parameters for the network area */
		double signalsBoundingBoxOffset = 500.0;
		/* Okt'14: sBB 500 instead of 50 to avoid effect that travelers drive
		 * from the ring around cottbus outside and inside again to jump in time */
		// an offset >= 31000.0 results in a bounding box that contains the hole network
		double cuttingBoundingBoxOffset = 50.0;
		/* parameters for the interior link filter */
		double freeSpeedFilter = 15.0;
		boolean useFreeSpeedTravelTime = true;
		 double maximalLinkLength = Double.MAX_VALUE; // = default value
		/* parameters for the demand filter */
		 double matsimPopSampleSize = 1.0; // = default value
		 double ksModelCommoditySampleSize = 1.0; // = default value
		double minCommodityFlow = 50.0;
		boolean simplifyNetwork = true;
		int cellsX = 5; // = default value
		int cellsY = 5; // = default value
		/* other parameters */
		String scenarioDescription = "run 100it 0.7cap output plans with flexible signals and 2 phases per signal system between 05:30 and 09:30";
		// String scenarioDescription =
		// "run run1728 output plans between 13:30 and 18:30";

		TtMatsim2KS2015.convertMatsim2KS(signalSystemsFilename,
				signalGroupsFilename, signalControlFilename, networkFilename,
				lanesFilename, populationFilename, startTime, endTime,
				signalsBoundingBoxOffset, cuttingBoundingBoxOffset,
				freeSpeedFilter, useFreeSpeedTravelTime, maximalLinkLength,
				matsimPopSampleSize, ksModelCommoditySampleSize,
				minCommodityFlow, simplifyNetwork, cellsX, cellsY, scenarioDescription,
				dateFormat, outputDirectory);
	}

}
