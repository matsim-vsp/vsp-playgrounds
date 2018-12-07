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

/**
 * 
 */
package playground.jbischoff.examples;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.network.filter.NetworkLinkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;

import playground.vsp.andreas.mzilske.bvg09.MergeNetworks;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class FilterNetwork {
public static void main(String[] args) {
	Network network = NetworkUtils.createNetwork();
	new MatsimNetworkReader(network).readFile("D:/cemdap-vw/input/networkpt-av-nov17.xml.gz");
	
	NetworkFilterManager nfm = new NetworkFilterManager(network);
	nfm.addLinkFilter(new NetworkLinkFilter() {
		
		@Override
		public boolean judgeLink(Link l) {
			if (l.getAllowedModes().contains("car"))
//			if (l.getAllowedModes().contains("pt"))
			return true;
			else return false;
		}
	});
	Network net2 = nfm.applyFilters();
	new NetworkCleaner().run(net2);
	new NetworkWriter(net2).write("D:/cemdap-vw/input/network_car_av_cleaned.xml.gz");
	
	Network networkpt = NetworkUtils.createNetwork();
	new MatsimNetworkReader(networkpt).readFile("D:/cemdap-vw/input/networkpt.xml.gz");
	MergeNetworks.merge(net2, "", networkpt);
	new NetworkWriter(net2).write("D:/cemdap-vw/input/networkpt-av-nov17_cleaned.xml.gz");

}
}
