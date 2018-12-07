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
package playground.tschlenther.createNetwork.OSM;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.NetworkSimplifier;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsReaderMatsimV1;
import org.matsim.counts.CountsWriter;

import playground.tschlenther.createNetwork.CountLinkFinder;

/**
 * @author tschlenther
 *
 */
public class CreateNetFromOSMKeepingCountLinks {
	
	private static Logger log = Logger.getLogger(CreateNetFromOSMKeepingCountLinks.class);
	
	/*
	 * there are two usecases for this class. in either case, you want to create a new matsim network out of an osm file
	 * keeping links and that counts (in the old network) lie on, so you don't have to rematch count stations.
	 * 
	 * 1) you have an osm-file, and a csv file (see method readNodeIDsFromCSV for needed structure) that contains information
	 * 	  about count location.
	 * 		=> set the field variable readNodeIDsFromCSV = true 
	 * 
	 * 2) you have an osm-file, and you have the old network file as well as the old count file
	 * 		=> => set the field variable readNodeIDsFromCSV = false		
	 */

	
	//-------------------INPUT---------------------------------------
	
	private static final String INPUT_OSMFILE = "C:/Users/Work/git/NEMO-gitfat/data/input/network/allWaysNRW/allWaysNRW.osm";

	//		USECASE 1)
	private static final String INPUT_COUNT_NODES= "C:/Users/Work/git/NEMO-gitfat/data/input/counts/mapmatching/Nemo_kurzfristZaehlstellen_OSMNodeIDs_UTM33N-Master.csv";
	
	//		USECASE 2)
	private static final String PATH_OLD_NETFILE = "C:/Users/Work/git/NEMO-gitfat/data/input/network/allWaysNRW/tertiaryNemo_10112017_EPSG_25832_filteredcleaned_network.xml.gz";
	private static final String PATH_OLD_COUNTSFILE = "C:/Users/Work/git/NEMO-gitfat/data/input/counts/24112017/NemoCounts_data_allCounts_KFZ.xml";
	
	
	//--------------------OUTPUT----------------------------------------------------
	private static final String OUTPUT_NETWORK = "C:/Users/Work/VSP/OSM/test_newNet_CSV.xml.gz";
	private static final String OUTPUT_COUNTS = "C:/Users/Work/VSP/OSM/test_newCounts_CSV.xml";
	
	//----------------GENERAL SETTINGS-------------------------------------------
	//set true, if csv file is given that contains information about count location. set to false if an old network and counts file is given, to extract the info from.
	private static boolean readNodeIDsFromCSV = false;
	
	private static final boolean keepPaths = false;
	
	//at the moment, the simplifier is not considered to be stable in term of creating gapless networks, tschlenther dec' 17
	private static final boolean doSimplify = false;
	
	private static final CoordinateTransformation TRANSFORMATION = 
			TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:25832");
	
	
	//-----------------------------------------------------------
	private static Map<Id<Link>,Tuple<Id<Node>,Id<Node>>> nodesOfCounts = new HashMap<Id<Link>, Tuple<Id<Node>, Id<Node>>>();
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		readAndWrite();
	}
	
	
	private static void readAndWrite(){
		//set the coordinate system you want the network to be translated to as second parameter
		
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);

		Network network = scenario.getNetwork();
		
		OsmNetworkReader onr = new OsmNetworkReader(network,TRANSFORMATION);
		onr.setKeepPaths(keepPaths);
		
		Set<Long> nodeIDsToKeep = new HashSet<Long>();

		Counts<Link> counts = new Counts();
		if(readNodeIDsFromCSV){
			nodeIDsToKeep = readNodeIDsFromCSV(INPUT_COUNT_NODES, ";");
		} else{
			//read in counts
			CountsReaderMatsimV1 countsReader = new CountsReaderMatsimV1(counts);
			countsReader.readFile(PATH_OLD_COUNTSFILE);
			nodeIDsToKeep = readNodeIDsFromOldNet(PATH_OLD_NETFILE, counts);
		}
		
//		onr.setHierarchyLayer(7);
		onr.setNodeIDsToKeep(nodeIDsToKeep);
		onr.parse(INPUT_OSMFILE);
		
		if(doSimplify){
			//simplify network: merge links that are shorter than the given threshold
			NetworkSimplifier simp = new NetworkSimplifier();
			simp.setNodesNotToMerge(nodeIDsToKeep);
			simp.run(network);
		}
		
		/*
		 * Clean the Network. Cleaning means removing disconnected components, so that afterwards there is a route from every link
		 * to every other link. This may not be the case in the initial network converted from OpenStreetMap.
		 */
		new NetworkCleaner().run(network);
		new NetworkWriter(network).write(OUTPUT_NETWORK);

		writeUpdatedCountsFile(network, counts);
		log.info("FINISHED");
	}
	
	
	private static void writeUpdatedCountsFile(Network network, Counts oldCounts){
		log.info("updating counts...");
		Counts updatedCounts = new Counts();
		for(Id<Link> countLocation : nodesOfCounts.keySet()){
			
			//determine the new link id
			CountLinkFinder linkFinder = new CountLinkFinder(network);
			Id<Link> newLinkId = null;
			Node fromNode = network.getNodes().get(nodesOfCounts.get(countLocation).getFirst());
			Id<Node> toNodeID = nodesOfCounts.get(countLocation).getSecond();		
			
			for(Link outlink : fromNode.getOutLinks().values()){
				if(outlink.getToNode().getId().equals(toNodeID)){
					newLinkId = outlink.getId();
				}
			}
			if(newLinkId == null){
				newLinkId = linkFinder.getFirstLinkOnTheWayFromNodeToNode(fromNode, network.getNodes().get(toNodeID));
			}
			
			//copy old volumes if available
			if(readNodeIDsFromCSV){
				Count updatedCount = updatedCounts.createAndAddCount(newLinkId, countLocation.toString());
				updatedCounts.setYear(LocalDate.now().getYear());
			} else{
				updatedCounts.setYear(oldCounts.getYear());
				updatedCounts.setDescription(oldCounts.getDescription() + "\t updated this file on" + LocalDate.now().toString());
				Count count = oldCounts.getCount(countLocation);
				Count updatedCount = updatedCounts.createAndAddCount(newLinkId, count.getCsLabel());
				for(Integer i : (Set<Integer>) count.getVolumes().keySet()){
					updatedCount.createVolume(i, count.getVolume(i).getValue());
				}
			}
		}
		log.info("writing new counts file to " + OUTPUT_COUNTS);
		CountsWriter writer = new CountsWriter(updatedCounts);
		writer.write(OUTPUT_COUNTS);
	}
	
	private static Set<Long> readNodeIDsFromOldNet(String pathToOldNetworkFile, Counts<Link> counts) {
		log.info("start reading in node id's by using old network and counts file...");
		Set<Long> allNodeIDs = new HashSet<Long>();
		
		//read in Network
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario.getNetwork());
		networkReader.readFile(pathToOldNetworkFile); 
		Network net = scenario.getNetwork();
		
		for(Id<Link> id : counts.getCounts().keySet()){
			Id<Node> fromNode = net.getLinks().get(id).getFromNode().getId();
			Id<Node> toNode = net.getLinks().get(id).getToNode().getId();

			nodesOfCounts.put(id,new Tuple<Id<Node>, Id<Node>>(fromNode,toNode));
			allNodeIDs.add(parseNodeID(fromNode));
			allNodeIDs.add(parseNodeID(toNode));
		}
		
		log.info("finished reading in node id's ...");
		return allNodeIDs;
	}

	private static Long parseNodeID(Id<Node> id){
		return Long.parseLong(id.toString());
	}
	
	/**
	 * expects a path to csv file that has the following structure: <br><br>
	 * 
	 * <i>COUNT-ID {@linkplain delimiter} OSM_FROMNODE_ID {@linkplain delimiter} OSM_TONODE_ID </i><br><br>
	 * 
	 * It is assumed that the csv file contains a header line.
	 * Returns a set of all mentioned osm-node-ids.
	 * 
	 * @param pathToCSVFile
	 * @return
	 */
	private static Set<Long> readNodeIDsFromCSV(String pathToCSVFile, String delimiter){
		log.info("start reading in node id's from given csv file...");
		Set<Long> allNodeIDs = new HashSet<Long>();
		
		TabularFileParserConfig config = new TabularFileParserConfig();
	    config.setDelimiterTags(new String[] {delimiter});
	    config.setFileName(pathToCSVFile);
	    new TabularFileParser().parse(config, new TabularFileHandler() {
	    	boolean header = true;
			@Override
			public void startRow(String[] row) {
				if(!header){
					allNodeIDs.add( Long.parseLong(row[1]) );
					allNodeIDs.add( Long.parseLong(row[2]) );
					nodesOfCounts.put(Id.createLinkId(row[0]),new Tuple<Id<Node>, Id<Node>>(Id.createNodeId(row[1]),Id.createNodeId(row[2])));
				}
				header = false;				
			}
		});
	    log.info("finished reading in node id's from given csv file...");
	    return allNodeIDs;
		
	}
}
