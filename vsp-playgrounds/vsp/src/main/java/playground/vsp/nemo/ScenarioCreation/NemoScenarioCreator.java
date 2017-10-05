/* *********************************************************************** *
 * project: org.matsim.*
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

/**
 * 
 */
package playground.vsp.nemo.ScenarioCreation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.NetworkSimplifier;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author tschlenther
 *
 */
	class NemoScenarioCreator {

	private final static String INPUT_OSMFILE = "C:/Users/Work/svn/shared-svn/projects/nemo_mercator/40_Data/counts/allWaysNRW.osm";
//	private final static String INPUT_OSMFILE = "C:/Users/Work/VSP/OSM/Reinickendorf_Flottenstr.osm";
	
	private final static String INPUT_COUNT_NODES_MAPPING_CSV= "C:/Users/Work/svn/shared-svn/projects/nemo_mercator/40_Data/counts/OSMNodeIDsALL_EDIT_Tilmann.csv";
//	private final static String INPUT_COUNT_NODES_MAPPING_CSV= "C:/Users/Work/VSP/OSM/fullNetwork_testCountingStations.csv";
	
	private final static String INPUT_COUNT_DATA_ROOT_DIR = "C:/Users/Work/svn/shared-svn/projects/nemo_mercator/40_Data/counts/LandesbetriebStrassenbauNRW_Verkehrszentrale";

	private final static String INPUT_NETWORK = "C:/Users/Work/VSP/Nemo/Verkehrszaehlung_2015/allWaysNRWkeeppaths.xml.gz";
	private final static String OUTPUT_NETWORK = "C:/Users/Work/VSP/Nemo/completeRun/includeShortTerm_0410/Nemo_network_Simpl_Clean_UTM32N.xml.gz";
	
	private final static String OUTPUT_COUNTS_DIR = "C:/Users/Work/VSP/Nemo/completeRun/includeShortTerm_0410/Nemo_041017_LongTermCounts/";
	
	//	dates are included in aggregation									year, month, dayOfMonth
	private final static LocalDate firstDayOfDataAggregation = LocalDate.of(2014, 1, 1);
	private final static LocalDate lastDayOfDataAggregation = LocalDate.of(2014, 10, 1);
	
	private static List<LocalDate> datesToIgnore = new ArrayList<LocalDate>();

	private static String INPUT_SHORTTERM_COUNT_DATA_ROOT_DIR = "C:/Users/Work/VSP/Nemo/Verkehrszaehlung_2015/nemo-master-ebd8d0a92610eb3fde0fd80bfe89fa12f1a68c31/data/input/counts/verkehrszaehlung_2015";

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		Network network = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getNetwork();
//		MatsimNetworkReader netReader = new MatsimNetworkReader(network);
//		netReader.readFile(INPUT_NETWORK);
		
		NemoNetworkCreator netCreator = new NemoNetworkCreator(INPUT_OSMFILE,INPUT_COUNT_NODES_MAPPING_CSV);
		
//		NemoNetworkCreator netCreator = new NemoNetworkCreator(network,INPUT_COUNT_NODES_MAPPING_CSV);
		network = netCreator.createAndWriteNetwork(true,true,OUTPUT_NETWORK);
		
		NemoCountsCreator countCreator = new NemoCountsCreator(network, INPUT_COUNT_DATA_ROOT_DIR, INPUT_COUNT_NODES_MAPPING_CSV, OUTPUT_COUNTS_DIR);
		countCreator.setFirstDayOfAnalysis(firstDayOfDataAggregation);
		countCreator.setLastDayOfAnalysis(lastDayOfDataAggregation);
		countCreator.setDatesToIgnore(datesToIgnore);
		countCreator.run();
		
		String shortTermCountMappingFile = "C:/Users/Work/VSP/Nemo/Verkehrszaehlung_2015/nemo-master-ebd8d0a92610eb3fde0fd80bfe89fa12f1a68c31/data/input/counts/test/Nemo_kurzfristZaehlstellen_OSMNodeIDs_Test.csv";
		NemoShortTermCountsCreator shortTermCounts = new NemoShortTermCountsCreator(network, INPUT_SHORTTERM_COUNT_DATA_ROOT_DIR , shortTermCountMappingFile,
				"C:/Users/Work/VSP/Nemo/completeRun/includeShortTerm_0410/shortTermCounts/", 2011, 2011);
		
		shortTermCounts.run();
	}

}
