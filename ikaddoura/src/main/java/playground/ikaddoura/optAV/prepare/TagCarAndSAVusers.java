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

package playground.ikaddoura.optAV.prepare;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
* @author ikaddoura
*/

public class TagCarAndSAVusers {

	private static final Logger log = Logger.getLogger(TagCarAndSAVusers.class);
	
	private final String inputDirectory = "/Users/ihab/Documents/workspace/runs-svn/optAV_be5/input/population/";
	private final String outputDirectory = "/Users/ihab/Documents/workspace/runs-svn/optAV_be5/input/population/";
	
	private final String inputPlansFile = "berlin-5.1-1pct_plans.xml.gz";
	private final String outputPlansFile = "berlin-5.1-1pct_plans_taggedCarUsers_new.xml.gz";
	
	private final String outputPersonAttributesFile = "berlin-5.0_person-attributes_potentialSAVusers_new.xml.gz";

	private final String areaOfPotentialSAVusersSHPFile = "/Users/ihab/Documents/workspace/shared-svn/projects/audi_av/shp/untersuchungsraumAll.shp";
	private final String crsSHPFile = "EPSG:25833";
	private final String crsPopulation = TransformationFactory.DHDN_GK4;

	// Optional: provide input person attributes file...
	private final String inputPersonAttributesFile = "berlin-5.0_person-attributes.xml.gz";
	private final String inputPersonAttributesSubpopulationPerson = "person";
	
	// ####################################################################
		
	private String inputPlans;
	private String outputPlans;
	private String inputPersonAttributes;
	private String outputPersonAttributes;
	
	private Scenario scenario;
	
	private int potentialSAVusers = 0;
	private int noPotentialSAVusers = 0;
	private int carUsers = 0;
	private int noCarUsers = 0;
	
	public static void main(String[] args) {			
				
		TagCarAndSAVusers generateAVDemand = new TagCarAndSAVusers();
		generateAVDemand.run();
	}

	private void run() {
		
		inputPlans = inputDirectory + inputPlansFile;
		outputPlans = outputDirectory + outputPlansFile;
		inputPersonAttributes = inputDirectory + inputPersonAttributesFile;
		outputPersonAttributes = outputDirectory + outputPersonAttributesFile;
		
		Config config = ConfigUtils.createConfig();
		config.plans().setInputFile(inputPlans);
		if (inputPersonAttributesFile != null) config.plans().setInputPersonAttributeFile(inputPersonAttributes);
		scenario = ScenarioUtils.loadScenario(config);
		
		tagCarUsers();
		tagPotentialSAVusers();
		
		log.info("Potential SAV users: " + potentialSAVusers);
		log.info("No potential SAV users: " + noPotentialSAVusers);
		log.info("Car users: " + carUsers);
		log.info("No car users: " + noCarUsers);
		
		new PopulationWriter(scenario.getPopulation()).write(outputPlans);
		new ObjectAttributesXmlWriter(scenario.getPopulation().getPersonAttributes()).writeFile(outputPersonAttributes);
	}

	private void tagPotentialSAVusers() {
		
		log.info("Tagging potential SAV users...");

		final CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(crsPopulation, crsSHPFile);
		final Map<Integer, Geometry> zoneId2geometry = new HashMap<Integer, Geometry>();
		
		Collection<SimpleFeature> features;
		features = ShapeFileReader.getAllFeatures(areaOfPotentialSAVusersSHPFile);
		int featureCounter = 0;
		for (SimpleFeature feature : features) {
			zoneId2geometry.put(featureCounter, (Geometry) feature.getDefaultGeometry());
			featureCounter++;
		}
		
		log.info("Going through the population and analyzing the activity coordinates...");
		double popsize = this.scenario.getPopulation().getPersons().size();

		int counter = 0;
		for (Person person : this.scenario.getPopulation().getPersons().values()) {
			counter++;
			
			if (counter % 10000 == 0) {
				log.info("# " + counter / popsize);
			}
			Plan selectedPlan = person.getSelectedPlan();
			if (selectedPlan == null) {
				throw new RuntimeException("No selected plan. Aborting...");
			}
			
			boolean allActivitiesInArea = true;

			for (PlanElement pE : selectedPlan.getPlanElements()) {
				
				if (allActivitiesInArea) {
					if (pE instanceof Activity) {
						Activity activity = (Activity) pE;
						
						if (activity.getType().toString().contains("interaction")) {
							// skip
						} else {
							boolean activityInArea = false;
							for (Geometry geometry : zoneId2geometry.values()) {
								Point p = MGC.coord2Point(ct.transform(activity.getCoord())); 
								
								if (p.within(geometry)) {
									activityInArea = true;
								}
							}
							
							if (!activityInArea) {
								allActivitiesInArea = false;
							}	
						}		
					}	
				}
			}
			
			if (inputPersonAttributesFile == null) {
				
				if (allActivitiesInArea) { 
					scenario.getPopulation().getPersonAttributes().putAttribute(person.getId().toString(), scenario.getConfig().plans().getSubpopulationAttributeName(), "potentialSAVuser");
					potentialSAVusers++;
				} else {
					scenario.getPopulation().getPersonAttributes().putAttribute(person.getId().toString(), scenario.getConfig().plans().getSubpopulationAttributeName(), "noPotentialSAVuser");
					noPotentialSAVusers++;
				}
			} else {
				
				String subpopulation = (String) scenario.getPopulation().getPersonAttributes().getAttribute(person.getId().toString(), scenario.getConfig().plans().getSubpopulationAttributeName());
				if (subpopulation.equals(inputPersonAttributesSubpopulationPerson)) {
					if (allActivitiesInArea) { 
						scenario.getPopulation().getPersonAttributes().putAttribute(person.getId().toString(), scenario.getConfig().plans().getSubpopulationAttributeName(), "person_potentialSAVuser");
						potentialSAVusers++;
					} else {
						scenario.getPopulation().getPersonAttributes().putAttribute(person.getId().toString(), scenario.getConfig().plans().getSubpopulationAttributeName(), "person_noPotentialSAVuser");
						noPotentialSAVusers++;
					}
				}
			}
		}
		
		log.info("Tagging potential SAV users... Done.");
		
	}

	private void tagCarUsers() {
		
		log.info("Tagging car users...");
		
		for (Person person : scenario.getPopulation().getPersons().values()) {
			Plan selectedPlan = person.getSelectedPlan();
			if (selectedPlan == null) {
				throw new RuntimeException("No selected plan. Aborting...");
			}
			
			boolean personHasCarTrip = false;
			
			for (PlanElement pE : selectedPlan.getPlanElements()) {
				
				if (pE instanceof Leg) {
					Leg leg = (Leg) pE;
					if (leg.getMode().equals(TransportMode.car)) {
						personHasCarTrip = true;
					}	
				}	
			}
			person.getAttributes().putAttribute("CarOwnerInBaseCase", personHasCarTrip);
			if (personHasCarTrip) {
				carUsers++;
			} else {
				noCarUsers++;
			}
		}		
		log.info("Tagging car users... Done.");
	}

}

