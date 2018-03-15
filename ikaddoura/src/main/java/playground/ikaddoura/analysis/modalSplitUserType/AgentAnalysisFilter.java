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

package playground.ikaddoura.analysis.modalSplitUserType;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
* @author ikaddoura
*/

public class AgentAnalysisFilter implements AgentFilter {
	private static final Logger log = Logger.getLogger(AgentAnalysisFilter.class);	

	// subpopulation
	private String subpopulation = null;
	
	// filter by person attribute
	private String personAttributeName = null;
	private String personAttribute = null;
	
	// filter by shape file
	private String zoneFile = null;
	private String relevantActivityTypePrefix = null;
	
	private final Scenario scenario;
	
	private boolean dataPreprocessed = false;
	private final Map<String, Geometry> zoneFeatures = new HashMap<>();
	private final Map<Id<Person>, Coord> personId2homeCoord = new HashMap<>();
	
	public AgentAnalysisFilter(Scenario scenario) {
		this.scenario = scenario;
	}
	public String getSubpopulation() {
		return subpopulation;
	}
	public void setSubpopulation(String subpopulation) {
		this.subpopulation = subpopulation;
	}
	public String getPersonAttributeName() {
		return personAttributeName;
	}
	public void setPersonAttributeName(String personAttributeName) {
		this.personAttributeName = personAttributeName;
	}
	public String getPersonAttribute() {
		return personAttribute;
	}
	public void setPersonAttribute(String personAttribute) {
		this.personAttribute = personAttribute;
	}
	public String getZoneFile() {
		return zoneFile;
	}
	public void setZoneFile(String zoneFile) {
		this.zoneFile = zoneFile;
	}
	public String getRelevantActivityType() {
		return relevantActivityTypePrefix;
	}
	public void setRelevantActivityType(String relevantActivityType) {
		this.relevantActivityTypePrefix = relevantActivityType;
	}
	
	@Override
	public boolean considerAgent(Person person) {
		
		if (dataPreprocessed == false) {
			throw new RuntimeException("Can't use the filter without pre-processing. Aborting...");
		}
		
		// subpopulation
		if (this.subpopulation != null ) {
			if (this.scenario.getPopulation().getPersonAttributes().getAttribute(person.getId().toString(), "subpopulation") != null) {
				String subPopulationName = (String) this.scenario.getPopulation().getPersonAttributes().getAttribute(person.getId().toString(), "subpopulation");
				if (!subPopulationName.equals(this.subpopulation)) {
					return false;
				}
			}
		}
		
		if (this.personAttributeName != null && this.personAttribute != null) {
			if (this.scenario.getPopulation().getPersonAttributes().getAttribute(person.getId().toString(), personAttributeName) != null) {
				String homeZoneName = (String) this.scenario.getPopulation().getPersonAttributes().getAttribute(person.getId().toString(), personAttributeName);
				if (!homeZoneName.equals(this.personAttribute)) {
					return false;
				}
			} else {
				return false;
			}
		}
		
		if (this.zoneFile != null && this.relevantActivityTypePrefix != null) {
			
			// assuming the same CRS!
			boolean pointWithinGeometry = false;
			for (Geometry geometry : zoneFeatures.values()) {
				Point point = MGC.coord2Point(this.personId2homeCoord.get(person.getId()));
				if (point.within(geometry)) {
					pointWithinGeometry = true;
					break;
				}
			}
			if (pointWithinGeometry == false) return false;
		}
		
		return true;
	}
	
	public void preProcess(Scenario scenario) {
		this.dataPreprocessed = true;
		
	    if (zoneFile != null) {
	    		log.info("Getting persons' home coordinates...");
			for (Person person : scenario.getPopulation().getPersons().values()) {
				Activity act = (Activity) person.getSelectedPlan().getPlanElements().get(0);
				if (act.getType().startsWith(relevantActivityTypePrefix)) {
					personId2homeCoord.put(person.getId(), act.getCoord());
				}
			}
			if (personId2homeCoord.isEmpty()) log.warn("No person with home activity.");
			log.info("Getting persons' home coordinates... Done.");
	    }
	    	    
		if (zoneFile != null) {					
			log.info("Reading shape file...");
			Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(zoneFile);
			int counter = 0;
			for (SimpleFeature feature : features) {
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                zoneFeatures.put(String.valueOf(counter), geometry);
                counter++;
            }
			log.info("Reading shape file... Done.");	
		}
		
	}
	
	@Override
	public String toFileName() {
		String fileName = "FILTER";
		
		boolean atLeastOneFilterApplied = false;
		
		if (zoneFile != null && this.relevantActivityTypePrefix != null) {
			atLeastOneFilterApplied = true;
			fileName = fileName + "_activityTypePrefix-" + this.relevantActivityTypePrefix + "-within-provided-zone-file";	
		}
		
		if (this.subpopulation != null) {
			atLeastOneFilterApplied = true;
			fileName = fileName + "_subpopulation=" + this.subpopulation;
		}
		
		if (this.personAttributeName != null) {
			atLeastOneFilterApplied = true;
			fileName = fileName + "_" + personAttributeName + "=" + personAttribute;
		}
		
		if (atLeastOneFilterApplied == false) {
			fileName = "";
		}
	
		return fileName;
	}

}

