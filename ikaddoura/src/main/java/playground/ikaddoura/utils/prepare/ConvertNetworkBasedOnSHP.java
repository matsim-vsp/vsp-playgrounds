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

package playground.ikaddoura.utils.prepare;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.UncheckedIOException;
import org.opengis.feature.simple.SimpleFeature;

/**
* @author ikaddoura
*/

public class ConvertNetworkBasedOnSHP {
	
	private static final Logger log = Logger.getLogger(ConvertNetworkBasedOnSHP.class);
	
	private final Map<Integer, SimpleFeature> features = new HashMap<>();
	
	public static void main(String[] args) throws IOException {	
		ConvertNetworkBasedOnSHP converter = new ConvertNetworkBasedOnSHP();
		converter.run();
	}

	private void run() throws UncheckedIOException, IOException {
		String input = "/Users/ihab/Documents/workspace/runs-svn/incidents-longterm-shortterm/input/longterm-vs-shortterm_incidentData_berlin_2016-02-11/network_2016-02-11.xml.gz";
		String output = "/Users/ihab/Documents/workspace/runs-svn/incidents-longterm-shortterm/input/policy_network_2016-02-11.xml.gz";
		String shapeFile = "/Users/ihab/Documents/workspace/runs-svn/incidents-longterm-shortterm/input/hundekopf-shp/hundekopf.shp";

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(input);

		log.info("Reading shp file...");
		
		SimpleFeatureIterator it = ShapeFileReader.readDataFile(shapeFile).getFeatures().features();
		int counter = 0;
		
		while (it.hasNext()) {
			SimpleFeature ft = it.next();
			features.put(counter, ft);
			counter++;
		}
		it.close();

		log.info("Reading shp file... Done.");
		
		int adjustedLinks = 0;
		for (Link link : scenario.getNetwork().getLinks().values()) {
			
			if (link.getFreespeed() < 14 && link.getFreespeed() > 8.3333333) {
				if (isLinkInArea(link)) {
					adjustedLinks++;
					link.setFreespeed(8.33333332211);
				}
			}
		}
		
		log.info("Adjusted links: " + adjustedLinks);
		new NetworkWriter(scenario.getNetwork()).write(output);
	}

	private boolean isLinkInArea(Link link) {
			
		for (SimpleFeature ft : features.values()) {
			if (ft.getBounds().contains(link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY())
					|| ft.getBounds().contains(link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY())) {
				return true;
			}
		}
		
		return false;
	}
}

