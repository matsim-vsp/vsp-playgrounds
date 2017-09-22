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

import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

/**
 * Separate additional attributes from network file into ObjectAttributes file such that they can be displayed in Via.
 * 
 * @author dziemke
 */
public class NetworkAttributesSeparator {
	private final static Logger LOG = Logger.getLogger(NetworkAttributesSeparator.class);
	private final String networkFile;
	private final String networkSeparatedFile;
	private final String linkAttributesFile;
	private final List<String> attributesToSeparate;


	public static void main(String[] args) {
		String networkFile = "../../shared-svn/studies/countries/de/berlin-bike/equil/network-f.xml";
		String networkSeparatedFile = "../../shared-svn/studies/countries/de/berlin-bike/equil/network-f_separated.xml";
		String linkAttributesFile = "../../shared-svn/studies/countries/de/berlin-bike/equil/network-f_attributes.xml";
		List<String> attributesToSeparate = Arrays.asList(new String[]{"type", "surface", "cycleway"});
		
		NetworkAttributesSeparator networkAttributesSeparator = new NetworkAttributesSeparator(
				networkFile, networkSeparatedFile, linkAttributesFile, attributesToSeparate);
		networkAttributesSeparator.separate();
	}
	
	public NetworkAttributesSeparator(String networkFile, String networkSeparatedFile, String linkAttributesFile,
			List<String>attributesToSeparate) {
		this.networkFile = networkFile;
		this.networkSeparatedFile = networkSeparatedFile;
		this.linkAttributesFile = linkAttributesFile;
		this.attributesToSeparate = attributesToSeparate;
	}
		
	public void separate() {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader matsimNetworkReader = new MatsimNetworkReader(scenario.getNetwork());
		NetworkWriter networkWriter = new NetworkWriter(scenario.getNetwork());
		ObjectAttributes linkAttributes = new ObjectAttributes();
		ObjectAttributesXmlWriter objectAttributesXmlWriter = new ObjectAttributesXmlWriter(linkAttributes);
		
		matsimNetworkReader.readFile(networkFile);
		
		for (Link link : scenario.getNetwork().getLinks().values()) {
			for (String attributeId : attributesToSeparate) {
				Object attribute = link.getAttributes().getAttribute(attributeId);
				if (attribute != null) {
					linkAttributes.putAttribute(link.getId().toString(), attributeId, attribute);
					link.getAttributes().removeAttribute(attributeId);
				}
			}
		}
	
		objectAttributesXmlWriter.writeFile(linkAttributesFile);
		networkWriter.writeV2(networkSeparatedFile);
		
		LOG.info("Done separating attributed network into (purer); network and extra object attributes file.");
	}
}