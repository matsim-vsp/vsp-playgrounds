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

package playground.ikaddoura.berlin.population;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
* @author ikaddoura
*/

public class WriteHomeAreaToPersonAttributesFile {
	private static final Logger log = Logger.getLogger(WriteHomeAreaToPersonAttributesFile.class);

	public static void main(String[] args) {
		
		// subpopulation: persons
		final String inputPlansFile = "/Users/ihab/Documents/workspace/shared-svn/studies/countries/de/open_berlin_scenario/be_3/population/be_400_c_10pct_person_freight.selected_plans.xml.gz";
		final String inputPersonAttributesFile = "/Users/ihab/Documents/workspace/shared-svn/studies/countries/de/open_berlin_scenario/be_3/population/personAttributes_400_person_freight_10pct.xml.gz";
		final String zoneFile = "/Users/ihab/Documents/workspace/shared-svn/studies/countries/de/open_berlin_scenario/input/shapefiles/2013/Berlin_DHDN_GK4.shp"; // needs to be in the same CRS
		final String homeActivityPrefix = "home";
		
		final String outputPersonAttributesFile = "/Users/ihab/Documents/workspace/shared-svn/studies/countries/de/open_berlin_scenario/be_3/population/personAttributes_with-home-area_400_person_freight_10pct.xml.gz";
		
		Config config = ConfigUtils.createConfig();
		config.plans().setInputFile(inputPlansFile);
		config.plans().setInputPersonAttributeFile(inputPersonAttributesFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		log.info("Reading shape file...");
	    final Map<String, Geometry> zoneFeatures = new HashMap<>();
		if (zoneFile != null) {
			Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(zoneFile);
			
			int counter = 0;
			
			for (SimpleFeature feature : features) {
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                zoneFeatures.put(String.valueOf(counter), geometry);
                counter++;
            }
		}
		log.info("Reading shape file... Done.");
				
		log.info("Getting persons' home coordinates...");
	    final Map<Id<Person>, Coord> personId2homeCoord = new HashMap<>();
		if (zoneFile != null) {
			for (Person person : scenario.getPopulation().getPersons().values()) {
				Activity act = (Activity) person.getSelectedPlan().getPlanElements().get(0);
				if (act.getType().startsWith(homeActivityPrefix)) {
					personId2homeCoord.put(person.getId(), act.getCoord());
				}
			}
			if (personId2homeCoord.isEmpty()) log.warn("No person with home activity.");
		}
		log.info("Getting persons' home coordinates... Done.");
		
		int counter = 0;
		for (Person person : scenario.getPopulation().getPersons().values()) {
			
			if (counter%10000 == 0) {
				log.info("Person #" + counter);
			}
			
			if (personId2homeCoord.get(person.getId()) != null) {
				if (isInsideArea(zoneFeatures, personId2homeCoord.get(person.getId()))) {
					scenario.getPopulation().getPersonAttributes().putAttribute(person.getId().toString(), "home-activity-in-berlin", true);
				} else {
					scenario.getPopulation().getPersonAttributes().putAttribute(person.getId().toString(), "home-activity-in-berlin", false);
				}
			}
			counter++;
		}

		ObjectAttributesXmlWriter writer2 = new ObjectAttributesXmlWriter(scenario.getPopulation().getPersonAttributes());
		writer2.writeFile(outputPersonAttributesFile);
		
		log.info("Done.");
	}
		
	private static boolean isInsideArea(Map<String, Geometry> zoneFeatures, Coord coord) {
			
		// assuming the same CRS!
			
		for (Geometry geometry : zoneFeatures.values()) {
			Point point = MGC.coord2Point(coord);
			if (point.within(geometry)) {
				return true;
			}
		}
		return false;
	}

}

