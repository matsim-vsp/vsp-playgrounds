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
package optimize.cten.convert.Matsim2cten;

import utils.OutputUtils;

/**
 * Class to convert the Cottbus scenario into KS format.
 * Uses the general conversion tool TtMatsim2KS2015.
 * 
 * @author dgrether
 * @author tthunig
 * 
 */
public class RunCottbus2Cten {

	public static void main(String[] args) throws Exception {
		// input files
//		String signalSystemsFilename = "../../shared-svn/projects/cottbus/data/scenarios/cottbus_scenario/signal_systems_no_13.xml";
		String signalSystemsFilename = "../../shared-svn/projects/cottbus/data/scenarios/cottbus_scenario/signal_systems_no_13_v4.xml";
//		String signalGroupsFilename = "../../shared-svn/projects/cottbus/data/scenarios/cottbus_scenario/signal_groups_no_13.xml";
//		String signalGroupsFilename = "../../shared-svn/projects/cottbus/data/scenarios/cottbus_scenario/signal_groups_laemmerLinkBased.xml";
//		String signalGroupsFilename = "../../shared-svn/projects/cottbus/data/scenarios/cottbus_scenario/signal_groups_laemmer2phases_6.xml";
		String signalGroupsFilename = "../../shared-svn/projects/cottbus/data/scenarios/cottbus_scenario/signal_groups_no_13_v4.xml";
//		String signalControlFilename = "../../shared-svn/projects/cottbus/data/scenarios/cottbus_scenario/signal_control_no_13.xml";
		String signalControlFilename = "../../shared-svn/projects/cottbus/data/scenarios/cottbus_scenario/signal_control_laemmer.xml";
//		String signalControlFilename = "../../shared-svn/projects/cottbus/data/scenarios/cottbus_scenario/signal_control_no_13_v4.xml";
		String signalConflictFilename = "../../shared-svn/projects/cottbus/data/scenarios/cottbus_scenario/conflictData_fromBtu2018-05-03_basedOnMSconflicts_v4.xml";
//		String networkFilename = "../../shared-svn/projects/cottbus/data/scenarios/cottbus_scenario/network_wgs84_utm33n.xml.gz";
		String networkFilename = "../../shared-svn/projects/cottbus/data/scenarios/cottbus_scenario/network_wgs84_utm33n_v4.xml";
//		String lanesFilename = "../../shared-svn/projects/cottbus/data/scenarios/cottbus_scenario/lanes.xml";
		String lanesFilename = "../../shared-svn/projects/cottbus/data/scenarios/cottbus_scenario/lanes_v1-4_long.xml";
		// change run number here to use another base case
//		String populationFilename = "../../runs-svn/cottbus/before2015/run1728/1728.output_plans.xml.gz";
//		String populationFilename = "../../shared-svn/projects/cottbus/data/scenarios/cottbus_scenario/cb_spn_gemeinde_nachfrage_landuse_woMines/"
//				+ "commuter_population_wgs84_utm33n_car_only_100it_MS_cap0.7.xml.gz";
		String populationFilename = "../../runs-svn/cottbus/createNewBC/2018-04-27-14-50-32_100it_netV4_tbs900_stuck120_beta2_MS_cap07/1000.output_plans.xml.gz";

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
		/* Okt'14: sBB 500 instead of 50 to avoid effect that travelers drive from the
		 * ring around cottbus outside and inside again to jump in time */
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
		String scenarioDescription = "changed: Dreifertstrasse and Wilhem-Kuelz_Str. run 100it 0.7cap output plans between 05:30 and 09:30";
		// String scenarioDescription =
		// "run run1728 output plans between 13:30 and 18:30";

		TtMatsim2KS2015.convertMatsim2KS(signalSystemsFilename,
				signalGroupsFilename, signalControlFilename, signalConflictFilename, networkFilename,
				lanesFilename, populationFilename, startTime, endTime,
				signalsBoundingBoxOffset, cuttingBoundingBoxOffset,
				freeSpeedFilter, useFreeSpeedTravelTime, maximalLinkLength,
				matsimPopSampleSize, ksModelCommoditySampleSize,
				minCommodityFlow, simplifyNetwork, cellsX, cellsY, scenarioDescription,
				dateFormat, outputDirectory);
	}

}
