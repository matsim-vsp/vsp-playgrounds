/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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
  
package playground.kturner.utils;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

class ConvertNetwork {

	public static void main(String[] args) {
		Network network = NetworkUtils.createNetwork();
		MatsimNetworkReader reader = new MatsimNetworkReader(TransformationFactory.WGS84, network);
		String filename = "../../OutputKMT/projects/freight/studies/reAnalysing_MA/MATSim/CEP-Wilmersdorf_Bike/MultipleTours/Run_1/output_network.xml";
		reader.readFile(filename );

		NetworkWriter writer = new NetworkWriter(network);
		writer.writeFileV2("../../OutputKMT/projects/freight/studies/reAnalysing_MA/MATSim/CEP-Wilmersdorf_Bike/MultipleTours/Run_1/output_network_WGS84.xml.gz");
	}

}
