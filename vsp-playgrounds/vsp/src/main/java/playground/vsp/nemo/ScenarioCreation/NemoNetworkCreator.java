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

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.NetworkSimplifier;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;


/**
 * @author tschlenther
 *
 */
public class NemoNetworkCreator {

	private String INPUT_OSMFILE = "C:/Users/Work/svn/shared-svn/studies/fzwick/berlinHighways.osm";
	private String INPUT_COUNT_NODES= "C:/Users/Work/svn/shared-svn/studies/countries/de/berlin_scenario_2016/network_counts/counts-osm-mapping/2017-06-17/OSM-nodes.csv";
	
	private Logger log = Logger.getLogger(NemoNetworkCreator.class);
	
	double linkLengthMinThreshold = Double.POSITIVE_INFINITY;
	
	private Network network = null;
	
	/**
	 * 
	 */
	public NemoNetworkCreator(String inputOSMFile, String inputCountNodeMappingFile) {
		this.INPUT_OSMFILE = inputOSMFile;
		this.INPUT_COUNT_NODES = inputCountNodeMappingFile;
	}
	
	private NemoNetworkCreator(){
	}
	
	public NemoNetworkCreator(Network network, String inputCountNodeMappingFile){
		this.network = network;
		this.INPUT_COUNT_NODES = inputCountNodeMappingFile;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new NemoNetworkCreator().createAndWriteNetwork(true, true, args[0]);
	}
	
	public Network createAndWriteNetwork(boolean doSimplify, boolean doCleaning, String outputPath){
		
		CoordinateTransformation ct = 
			 TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, "EPSG:32632");
		
		Set<Long> nodeIDsToKeep = readNodeIDs(INPUT_COUNT_NODES);
		
		if(this.network == null){
			Config config = ConfigUtils.createConfig();
			Scenario scenario = ScenarioUtils.createScenario(config);
			network = scenario.getNetwork();
			log.info("start reading from osm file " + INPUT_OSMFILE);
			OsmNetworkReader onr = new OsmNetworkReader(network,ct);
			onr.setKeepPaths(true);
			onr.setNodeIDsToKeep(nodeIDsToKeep);
			onr.parse(INPUT_OSMFILE);
		}
		
		log.info("checking if all count nodes are in the network..");
		for(Long id : nodeIDsToKeep){
			if(!network.getNodes().containsKey(Id.createNodeId(id))){
				log.error("COULD NOT FIND NODE " + id + " IN THE NETWORK BEFORE SIMPLIFYING AND CLEANING");
			}
		}
		
		if(doSimplify){
				/*
				 * simplify network: merge links that are shorter than the given threshold
				 */
			NetworkSimplifier simp = new NetworkSimplifier();
			simp.setNodesNotToMerge(nodeIDsToKeep);
			simp.setMergeLinkStats(false);
			simp.run(network);
			
			log.info("checking if all count nodes are in the network..");
			for(Long id : nodeIDsToKeep){
				if(!network.getNodes().containsKey(Id.createNodeId(id))){
					log.error("COULD NOT FIND NODE " + id + " IN THE NETWORK AFTER SIMPLIFYING");
				}
			}
		}
		if(doCleaning){
				/*
				 * Clean the Network. Cleaning means removing disconnected components, so that afterwards there is a route from every link
				 * to every other link. This may not be the case in the initial network converted from OpenStreetMap.
				 */
			log.info("attempt to clean the network");
			new NetworkCleaner().run(network);
		}
		
		log.info("checking if all count nodes are in the network..");
		for(Long id : nodeIDsToKeep){
			if(!network.getNodes().containsKey(Id.createNodeId(id))){
				log.error("COULD NOT FIND NODE " + id + " IN THE NETWORK AFTER NETWORK CREATION");
			}
		}
		
		new NetworkWriter(network).write(outputPath);
		return network;
	}

	/**
	 * sets the link length threshold for the internally used {@link NetworkSimplifier}
	 */
	public void setLinkLenghtMinThreshold(double minLinkLength){
		this.linkLengthMinThreshold = minLinkLength;
	}
	
	/**
	 * expects a path to a csv file that has the following structure: <br><br>
	 * 
	 * COUNT-ID;OSM_FROMNODE_ID;OSM_TONODE_ID <br><br>
	 * 
	 * It is assumed that the csv file contains a header line.
	 * Returns a set of all mentioned osm-node-ids.
	 * 
	 * @param pathToCSVFile
	 * @return
	 */
	private Set<Long> readNodeIDs(String pathToCSVFile){
		Set<Long> allNodeIDs = new HashSet<Long>();
		
		TabularFileParserConfig config = new TabularFileParserConfig();
	    config.setDelimiterTags(new String[] {";"});
	    config.setFileName(pathToCSVFile);
	    new TabularFileParser().parse(config, new TabularFileHandler() {
	    	int rowNr = 0;
	    	boolean header = true;
			@Override
			public void startRow(String[] row) {
				System.out.println("reading row nr " + rowNr + "of osm mapping input file");
				if(!header){
					allNodeIDs.add( Long.parseLong(row[1]) );
					allNodeIDs.add( Long.parseLong(row[2]) );
				}
				header = false;				
				rowNr++;
			}
		});
	    return allNodeIDs;
	}
	
	public Network getNetwork(){
		return this.network;
	}
}
