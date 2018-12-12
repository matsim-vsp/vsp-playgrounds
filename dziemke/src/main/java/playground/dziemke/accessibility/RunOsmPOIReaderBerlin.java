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
import java.util.Map;

import org.matsim.contrib.accessibility.osm.OsmKeys;
import org.matsim.contrib.accessibility.osm.OsmPoiReader;
import org.matsim.contrib.accessibility.utils.AccessibilityFacilityUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

/**
 * @author dziemke
 */
public class RunOsmPOIReaderBerlin {

	public static void main(String[] args) {		
		// Input and output
		String osmInputFile = "../../shared-svn/projects/accessibility_berlin/osm/berlin/2018-05-30_berlin.osm";
		String facilityFile = "../../shared-svn/projects/accessibility_berlin/osm/berlin/amenities/2018-05-30/facilities_coarse.xml";
		
		// Parameters
		String crs = "EPSG:31468";
		
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84", crs);
		OsmPoiReader osmPoiReader = null;
		try {
			osmPoiReader = new OsmPoiReader(osmInputFile, ct);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		
//		Map<String, String> osmAmenityToMatsimTypeMap = AccessibilityFacilityUtils.buildOsmAmenityToMatsimTypeMapV2FinerClassification();
		Map<String, String> osmAmenityToMatsimTypeMap = AccessibilityFacilityUtils.buildOsmAmenityToMatsimTypeMap();
//		Map<String, String> osmLeisureToMatsimTypeMap = AccessibilityFacilityUtils.buildOsmLeisureToMatsimTypeMapV2FinerClassification();
//		Map<String, String> osmTourismToMatsimTypeMap = AccessibilityFacilityUtils.buildOsmTourismToMatsimTypeMapV2FinerClassification();
//		Map<String, String> osmShopToMatsimTypeMap = AccessibilityFacilityUtils.buildOsmShopToMatsimTypeMapV2FinerClassification();
		Map<String, String> osmShopToMatsimTypeMap = AccessibilityFacilityUtils.buildOsmShopToMatsimTypeMapV2();

		osmPoiReader.parseOsmFileAndAddFacilities(osmAmenityToMatsimTypeMap, OsmKeys.AMENITY);
//		osmPoiReader.parseOsmFileAndAddFacilities(osmLeisureToMatsimTypeMap, OsmKeys.LEISURE);
//		osmPoiReader.parseOsmFileAndAddFacilities(osmTourismToMatsimTypeMap, OsmKeys.TOURISM);
//		osmPoiReader.setUseGeneralTypeIsSpecificTypeUnknown(true);
		osmPoiReader.parseOsmFileAndAddFacilities(osmShopToMatsimTypeMap, OsmKeys.SHOP);
		osmPoiReader.writeFacilities(facilityFile);		
	}
}