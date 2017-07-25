/* *********************************************************************** *
 * project: org.matsim.*
 * RunAmenityReaderForNmbm.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.dziemke.accessibility;

import java.io.FileNotFoundException;

import org.apache.log4j.Logger;
import org.matsim.contrib.accessibility.osm.CombinedOsmReader;
import org.matsim.contrib.accessibility.utils.AccessibilityFacilityUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.facilities.Facility;
import org.matsim.utils.objectattributes.ObjectAttributes;

import playground.dziemke.utils.LogToOutputSaver;

/**
 * @author dziemke
 */
public class RunCombinedOsmReader {
	final private static Logger log = Logger.getLogger(RunCombinedOsmReader.class);

	/**
	 * Implementing the {@link CombinedOsmReader} class. 
	 * @param args The main method requires three arguments:
	 * <ol>
	 * 	<li> the OpenStreetMap file, *.osm;
	 * 	<li> the output MATSim {@link Facility} file;
	 * 	<li> the output {@link ObjectAttributes} file containing the facility 
	 * 		 attributes.
	 * </ol>
	 * 
	 * An optional argument can be provided if you want the WGS84 coordinates
	 * converted into another (projected) coordinate reference system (CRS). 
	 * It is recommended that you <i>do</i> provide a projected CRS as MATSim
	 * works in metres.
	 */
	public static void main(String[] args) {
		// Input and output
//		String osmFile = "/Users/dominik/Workspace/shared-svn/projects/accessibility_berlin/osm/schlesische_str/2015-06-24_schlesische_str.osm";
//		String osmFile = "/Users/dominik/Workspace/shared-svn/projects/accessibility_berlin/osm/kreuzberg/2015-09-13_kreuzberg.osm";
//		String osmFile = "/Users/dominik/Workspace/shared-svn/projects/accessibility_berlin/osm/berlin/2015-09-13_berlin.osm";
//		String osmFile = "/Users/dominik/Accessibility/Data/OSM/2015-10-15_nairobi.osm.xml";
		String osmFile = "../../../../Workspace/data/accessibility/osm/2015-10-15_capetown_central.osm.xml";
		
//		String outputBase = "/Users/dominik/Workspace/shared-svn/projects/accessibility_berlin/osm/schlesische_str/07/";
//		String outputBase = "/Users/dominik/Workspace/shared-svn/projects/accessibility_berlin/osm/kreuzberg/02/";
//		String outputBase = "/Users/dominik/Workspace/shared-svn/projects/accessibility_berlin/osm/berlin/09/";
//		String outputBase = "/Users/dominik/Workspace/shared-svn/projects/accessibility_berlin/osm/berlin/combined/01/";
//		String outputBase = "/Users/dominik/Accessibility/Data/nairobi/combined/02/";
		String outputBase = "../../../../Workspace/data/accessibility/capetown/facilities/01/";
		
		String facilityFile = outputBase + "facilities.xml";
		String attributeFile = outputBase + "facilitiy_attributes.xml";
		
		// Logging
		log.info("Parsing land use from OpenStreetMap.");
		LogToOutputSaver.setOutputDirectory(outputBase);
		
		// Parameters
//		String outputCRS = "EPSG:31468"; // = DHDN GK4, for Berlin
//		String outputCRS = "EPSG:21037"; // = Arc 1960 / UTM zone 37S, for Nairobi, Kenya
		String outputCRS = TransformationFactory.WGS84_SA_Albers; // for South Africa
		
		// building types are either taken from the building itself and, if building does not have a type, taken from
		// the type of land use of the area which the build belongs to.
		double buildingTypeFromVicinityRange = 0.;
		
		
//		String osmFile = args[0];
//		String facilityFile = args[1];
//		String attributeFile = args[2];
//		String outputCRS = "WGS84";
//		if(args.length > 3){
//			outputCRS = args[3];
//		}
		

		CombinedOsmReader combinedOsmReader = new CombinedOsmReader(
				outputCRS,
				AccessibilityFacilityUtils.buildOsmLandUseToMatsimTypeMap(),
				AccessibilityFacilityUtils.buildOsmBuildingToMatsimTypeMap(),
				AccessibilityFacilityUtils.buildOsmAmenityToMatsimTypeMap(),
				AccessibilityFacilityUtils.buildOsmLeisureToMatsimTypeMap(),
				AccessibilityFacilityUtils.buildOsmTourismToMatsimTypeMap(),
				AccessibilityFacilityUtils.buildUnmannedEntitiesList(),
				buildingTypeFromVicinityRange);
		try {
			combinedOsmReader.parseFile(osmFile);
			combinedOsmReader.writeFacilities(facilityFile);
			combinedOsmReader.writeFacilityAttributes(attributeFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}		
	}
}