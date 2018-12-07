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

import org.matsim.contrib.accessibility.osm.AmenityReader;
import org.matsim.contrib.accessibility.utils.AccessibilityFacilityUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

/**
 * @author dziemke
 */
public class RunOsmAmenityReaderBerlin {

	public static void main(String[] args) {		
		// Input and output
		String osmFile = "../../shared-svn/projects/accessibility_berlin/osm/berlin/2018-05-30_berlin.osm";
		String facilityFile = "../../shared-svn/projects/accessibility_berlin/osm/berlin/amenities/2018-05-30/facilities.xml";
		
		// Parameters
		String crs = "EPSG:31468";
		
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84", crs);
		AmenityReader amenityReader = new AmenityReader(osmFile, ct, AccessibilityFacilityUtils.buildOsmAmenityToMatsimTypeMap());
		try {
			amenityReader.parseAmenity(osmFile);
			amenityReader.writeFacilities(facilityFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}		
	}
}