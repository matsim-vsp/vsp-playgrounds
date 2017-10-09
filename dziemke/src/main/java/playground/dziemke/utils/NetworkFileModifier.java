/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.dziemke.utils;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

/**
 * @author dziemke
 */
public class NetworkFileModifier {

private final static Logger LOG = Logger.getLogger(PlanFileModifier.class);
	
	public static void main(String[] args) {
		// Check if args has an interpretable length
		if (args.length != 0) { // TODO
			throw new IllegalArgumentException("Arguments array must have a length of 0, ....!"); // TODO
		}
		
		// Local use
		String inputNetworkFileName = "../../upretoria/data/capetown/scenario_2017/original/network.xml.gz";
		String outputNetworkFileName = "../../upretoria/data/capetown/scenario_2017/network_32734.xml.gz";
		String inputCRS = TransformationFactory.HARTEBEESTHOEK94_LO19;
		String outputCRS = "EPSG:32734";
		
		CoordinateTransformation ct;
		if (inputCRS == null && outputCRS == null) {
			ct = new IdentityTransformation();
		} else {
			ct = TransformationFactory.getCoordinateTransformation(inputCRS, outputCRS);
		}
		
		// Server use, version without CRS transformation // TODO
//		if (args.length == 3) {
//			inputNetworkFile = args[0];
//		}
		
		modifyNetwork(inputNetworkFileName, outputNetworkFileName, ct);
	}
		
	public static void modifyNetwork (String inputNetworkFileName, String outputNetworkFileName, CoordinateTransformation ct) {
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader matsimNetworkReader = new MatsimNetworkReader(scenario.getNetwork());
		
		matsimNetworkReader.readFile(inputNetworkFileName);
		
		for (Node node : scenario.getNetwork().getNodes().values()) {
			node.setCoord(ct.transform(node.getCoord()));
		}
		
		NetworkWriter networkWriter = new NetworkWriter(scenario.getNetwork());
		networkWriter.writeV2(outputNetworkFileName);
		LOG.info("Done the network file.");
	}
}