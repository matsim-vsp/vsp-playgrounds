/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.ikaddoura.networkDesign;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.run.RunBerlinScenario;

/**
* @author ikaddoura
* 
*/
public class CreateGridNetwork {

//	private static final String configFile = "/Users/ihab/Documents/workspace/runs-svn/networkDesign/input/berlin-v5.2-1pct-networkDesign.config.xml";
	private static final String configFile = null;
	private static final double gridsize = 500.;
	private static final Logger log = Logger.getLogger(CreateGridNetwork.class);
	
	public static void main(String[] args) {
		
		RunBerlinScenario berlin = new RunBerlinScenario(configFile, null);		
		final Scenario scenario = berlin.prepareScenario();
		
		
		double minX;
		double maxX;
		double minY;
		double maxY;
		
		if (configFile != null) {

			minX = Double.MAX_VALUE;
			maxX = Double.MIN_VALUE;
			minY = Double.MAX_VALUE;
			maxY = Double.MIN_VALUE;
			
			for (Link link : scenario.getNetwork().getLinks().values()) {
				if (!link.getId().toString().startsWith("pt")) {
					if (link.getCoord().getX() < minX) minX = link.getCoord().getX();
					if (link.getCoord().getY() < minY) minY = link.getCoord().getY();
					
					if (link.getCoord().getX() > maxX) maxX = link.getCoord().getX();
					if (link.getCoord().getY() > maxY) maxY = link.getCoord().getY();
				}	
			}
			
		} else {
			
			minX = 4565039.;
			maxX = 4632739.;
			minY = 5801108.;
			maxY = 5845708.;
		}
		
		Network network = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getNetwork();
		NetworkFactory networkFactory = network.getFactory();
		int counter = 0;
		for (double x = minX ; x <= maxX; x = x+gridsize) {
			for (double y = minY ; y <= maxY; y = y+gridsize) {
				Node node = networkFactory.createNode(Id.createNodeId(counter), new Coord(x, y));
				network.addNode(node);
				
				counter++;
				if (counter % 1000 == 0.) log.info("Writing node #" + counter);
			}
		}
		
		Set<String> modes = new HashSet<>();
		modes.add("car");
		modes.add("freight");
		modes.add("ride");
		for (Node node : network.getNodes().values()) {
//			log.info("node: " + node);
			for (Node nearestNode : NetworkUtils.getNearestNodes(network, node.getCoord(), gridsize + 50.)) {
//				log.info("nearestNode: " + nearestNode);

				if (!node.getId().toString().equals(nearestNode.getId().toString())) {
					Link link1 = networkFactory.createLink(Id.createLinkId(node.getId() + "-" + nearestNode.getId()), node, nearestNode);
					link1.setCapacity(1000);
					link1.setFreespeed(50/3.6);
					link1.setAllowedModes(modes);
					if (network.getLinks().get(link1.getId()) == null) network.addLink(link1);
					
					Link link2 = networkFactory.createLink(Id.createLinkId(nearestNode.getId() + "-" + node.getId()), nearestNode, node);
					link2.setCapacity(1000);
					link2.setFreespeed(50/3.6);
					link2.setAllowedModes(modes);
					if (network.getLinks().get(link2.getId()) == null) network.addLink(link2);
				}
			}
		}
		
		
		new NetworkWriter(network).write("/Users/ihab/Desktop/" + "initial" + ".grid-network.xml.gz");
		log.info("Done.");
	}	
}

