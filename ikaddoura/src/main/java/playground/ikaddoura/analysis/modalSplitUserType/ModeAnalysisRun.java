package playground.ikaddoura.analysis.modalSplitUserType;
/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * 
 * @author ikaddoura
 *
 */
public class ModeAnalysisRun {
	
	private final String runDirectory = "/Users/ihab/Desktop/ils4a/ziemke/open_berlin_scenario/output/be300mt_1/";
	private final String outputDirectory = "/Users/ihab/Desktop/modal-split-analysis/";
	private final String runId = "be300mt_1";
	
	// optional: Provide a subpopulation
	private final String subpopulation = "person";
	
	// optional: Provide a zoneFile to compute area-specific mode shares (needs to be in the same CRS)
    private final String zoneFile = null;
//    private final String zoneFile = "/Users/ihab/Documents/workspace/shared-svn/studies/countries/de/open_berlin_scenario/input/shapefiles/2013/Berlin_DHDN_GK4.shp"; // needs to be in the same CRS
    private final String homeActivityPrefix = "home";
	
	// optional: Provide a person attribute which marks the agent's home area to compute area-specific mode shares
	private final String personAttributeForSpatialAnalysis = "home-activity-in-berlin";
	
	// optional: Provide a personAttributes file which is used instead of the normal output person attributes file
	private final String personAttributesFile = "/Users/ihab/Documents/workspace/shared-svn/studies/countries/de/open_berlin_scenario/be_3/population/personAttributes_with-home-area_300_person_freight_10pct.xml.gz";

	private String outputFileName;
	private static final Logger log = Logger.getLogger(ModeAnalysisRun.class);	
	private Scenario scenario;
	
	public static void main(String[] args) throws IOException {
		ModeAnalysisRun main = new ModeAnalysisRun();
		main.run();
	}
		
	public void run() throws IOException {
		
		if(runId == null) {
			outputFileName = "tripModeAnalysis_" + subpopulation + "_others_all.csv";
		} else {
			outputFileName = runId + ".tripModeAnalysis_" + subpopulation + "_others_all.csv";
		}
		
		log.info("Loading scenario...");
		scenario = loadScenario();
		log.info("Loading scenario... Done.");
		
	    final Map<Id<Person>, Coord> personId2homeCoord = new HashMap<>();
	    if (zoneFile != null || personAttributeForSpatialAnalysis != null) {
	    		log.info("Getting persons' home coordinates...");
			for (Person person : scenario.getPopulation().getPersons().values()) {
				Activity act = (Activity) person.getSelectedPlan().getPlanElements().get(0);
				if (act.getType().startsWith(homeActivityPrefix)) {
					personId2homeCoord.put(person.getId(), act.getCoord());
				}
			}
			if (personId2homeCoord.isEmpty()) log.warn("No person with home activity.");
			log.info("Getting persons' home coordinates... Done.");
	    }
	    	    
	    final Map<String, Geometry> zoneFeatures = new HashMap<>();
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
		
		Map<String, Integer> mode2TripCounterSubpopulation = new HashMap<>();		
		Map<String, Integer> mode2TripCounterOtherSubpopulations = new HashMap<>();
		Map<String, Integer> mode2TripCounterInsideArea = new HashMap<>();		
		Map<String, Integer> mode2TripCounterOutsideArea = new HashMap<>();
		Map<String, Integer> mode2TripCounterPersonsWithHomeActivity = new HashMap<>();
		Map<String, Integer> mode2TripCounterAll = new HashMap<>();

		double allModesSubpopulation = 0.;
		double allModesOtherSubpopulations = 0.;
		double allModesInsideArea = 0.;
		double allModesOutsideArea = 0.;
		double allModesPersonWithHomeActivity = 0.;
		double allModesAll = 0.;
		
		int counter = 0;
		for (Person person : scenario.getPopulation().getPersons().values()) {
			
			if (counter%10000 == 0) {
				log.info("Person #" + counter);
			}
			Leg previousMainModeLeg = null;

			for (PlanElement pE : person.getSelectedPlan().getPlanElements()) {
								
				if (pE instanceof Leg) {
					Leg leg = (Leg) pE;
					
					if (leg.getMode().equals("transit_walk") || leg.getMode().equals("egress_walk") || leg.getMode().equals("access_walk")) {
						// skipping help leg modes
						
					} else {
						if (previousMainModeLeg == null) {
							previousMainModeLeg = leg;
						} else {
							if (previousMainModeLeg.getMode().equals(leg.getMode())) {
								previousMainModeLeg = leg;
							} else {
								throw new RuntimeException("Two different main leg modes found for the same trip: " + leg.getMode() + " and " + previousMainModeLeg.getMode() + ". Aborting...");
							}
						}
					}
										
				} else if (pE instanceof Activity) {
					Activity activity = (Activity) pE;
					
					if (activity.getType().contains("interaction")) {
						// the actual trip is not completed
					} else {
												
						if (previousMainModeLeg != null) {
							
							allModesAll++;

							if (mode2TripCounterAll.containsKey(previousMainModeLeg.getMode())) {
								mode2TripCounterAll.put(previousMainModeLeg.getMode(), mode2TripCounterAll.get(previousMainModeLeg.getMode()) + 1);
							} else {
								mode2TripCounterAll.put(previousMainModeLeg.getMode(), 1);
							}
							
							if (subpopulation != null && scenario.getPopulation().getPersonAttributes().getAttribute(person.getId().toString(), "subpopulation").toString().equals(subpopulation)) {
								allModesSubpopulation++;
								
								if (mode2TripCounterSubpopulation.containsKey(previousMainModeLeg.getMode())) {
									mode2TripCounterSubpopulation.put(previousMainModeLeg.getMode(), mode2TripCounterSubpopulation.get(previousMainModeLeg.getMode()) + 1);
								} else {
									mode2TripCounterSubpopulation.put(previousMainModeLeg.getMode(), 1);
								}
								
							} else {
								allModesOtherSubpopulations++;
								
								if (mode2TripCounterOtherSubpopulations.containsKey(previousMainModeLeg.getMode())) {
									mode2TripCounterOtherSubpopulations.put(previousMainModeLeg.getMode(), mode2TripCounterOtherSubpopulations.get(previousMainModeLeg.getMode()) + 1);
								} else {
									mode2TripCounterOtherSubpopulations.put(previousMainModeLeg.getMode(), 1);
								}
							}
							
							if (zoneFile != null || personAttributeForSpatialAnalysis != null) {
								
								if (personId2homeCoord.get(person.getId()) != null) {
									// person with home activity
									
									allModesPersonWithHomeActivity++;
									
									if (mode2TripCounterPersonsWithHomeActivity.containsKey(previousMainModeLeg.getMode())) {
										mode2TripCounterPersonsWithHomeActivity.put(previousMainModeLeg.getMode(), mode2TripCounterPersonsWithHomeActivity.get(previousMainModeLeg.getMode()) + 1);
									} else {
										mode2TripCounterPersonsWithHomeActivity.put(previousMainModeLeg.getMode(), 1);
									}
									
									if (isInsideArea(zoneFeatures, personId2homeCoord.get(person.getId()), person.getId())) {
										// inside
										
										allModesInsideArea++;
										
										if (mode2TripCounterInsideArea.containsKey(previousMainModeLeg.getMode())) {
											mode2TripCounterInsideArea.put(previousMainModeLeg.getMode(), mode2TripCounterInsideArea.get(previousMainModeLeg.getMode()) + 1);
										} else {
											mode2TripCounterInsideArea.put(previousMainModeLeg.getMode(), 1);
										}
										
									} else {
										// outside
										
										allModesOutsideArea++;
										
										if (mode2TripCounterOutsideArea.containsKey(previousMainModeLeg.getMode())) {
											mode2TripCounterOutsideArea.put(previousMainModeLeg.getMode(), mode2TripCounterOutsideArea.get(previousMainModeLeg.getMode()) + 1);
										} else {
											mode2TripCounterOutsideArea.put(previousMainModeLeg.getMode(), 1);
										}
									}
								} else {
									// skipping agents without a home activity
								}
							}
							
							previousMainModeLeg = null;
						}						
					}
					
				}
			}
			
			counter++;
		}
		
		File directory = new File(outputDirectory);
		directory.mkdirs();
		
		File file = new File(outputDirectory + outputFileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("mode ; number of trips (subpopulation = " + subpopulation + " ) ; trip share (subpopulation = " + subpopulation + "); number of trips (all other subpopulations) ; trip share (all other subpopulations) ; number of trips (all agents); trip share (all agents) ");
			bw.newLine();
			
			for (String mode : mode2TripCounterAll.keySet()) {
				
				double modeSubpop = 0.;				
				if (mode2TripCounterSubpopulation.get(mode) != null) {
					modeSubpop = mode2TripCounterSubpopulation.get(mode);
				}
				
				double modeOthers = 0.;
				if (mode2TripCounterOtherSubpopulations.get(mode) != null) {
					modeOthers = mode2TripCounterOtherSubpopulations.get(mode);
				}
				
				bw.write(mode + ";" + modeSubpop + ";" + (modeSubpop / allModesSubpopulation) + ";" + modeOthers + ";" + (modeOthers / allModesOtherSubpopulations) + ";" + mode2TripCounterAll.get(mode) + ";" + (mode2TripCounterAll.get(mode) / allModesAll));
				bw.newLine();
			}
			
			bw.write("sum ; " + allModesSubpopulation + ";" + (allModesSubpopulation / allModesSubpopulation) + ";" + allModesOtherSubpopulations + ";" + (allModesOtherSubpopulations / allModesOtherSubpopulations) + ";" + allModesAll + ";" + (allModesAll / allModesAll));
			bw.newLine();
			
			bw.newLine();

			if (zoneFile != null || personAttributeForSpatialAnalysis != null) {
				
				bw.write("mode ; number of trips (home activity inside area) ; trip share (home activity inside area) ; number of trips (home activity outside area) ; trip share (home activity outside area) ; number of trips (all agents with home activity); trip share (all agents with home activity) ");
				bw.newLine();
				
				for (String mode : mode2TripCounterAll.keySet()) {
					
					double modeInside = 0.;				
					if (mode2TripCounterInsideArea.get(mode) != null) {
						modeInside = mode2TripCounterInsideArea.get(mode);
					}
					
					double modeOutside = 0.;
					if (mode2TripCounterOutsideArea.get(mode) != null) {
						modeOutside = mode2TripCounterOutsideArea.get(mode);
					}
					
					double modePersonsWithHomeActivity = 0.;
					if (mode2TripCounterPersonsWithHomeActivity.get(mode) != null) {
						modePersonsWithHomeActivity = mode2TripCounterPersonsWithHomeActivity.get(mode);
					}
					
					bw.write(mode + ";" + modeInside + ";" + (modeInside / allModesInsideArea) + ";" + modeOutside + ";" + (modeOutside / allModesOutsideArea) + ";" + modePersonsWithHomeActivity + ";" + (modePersonsWithHomeActivity / allModesPersonWithHomeActivity));
					bw.newLine();
				}
				
				bw.write("sum ; " + allModesInsideArea + ";" + (allModesInsideArea / allModesInsideArea) + ";" + allModesOutsideArea + ";" + (allModesOutsideArea / allModesOutsideArea) + ";" + allModesPersonWithHomeActivity + ";" + (allModesPersonWithHomeActivity / allModesPersonWithHomeActivity));
				bw.newLine();
			}
			
			bw.close();
			log.info("Output written.");
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private boolean isInsideArea(Map<String, Geometry> zoneFeatures, Coord coord, Id<Person> personId) {
		
		if (personAttributeForSpatialAnalysis != null) {
			if (this.scenario.getPopulation().getPersonAttributes().getAttribute(personId.toString(), personAttributeForSpatialAnalysis) != null) {
				boolean insideArea = (boolean) this.scenario.getPopulation().getPersonAttributes().getAttribute(personId.toString(), personAttributeForSpatialAnalysis);
				return insideArea;
			} else {
				return false;
			}
		} else {
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

	private Scenario loadScenario() {
		Scenario scenario;
		if (runId == null) {
			Config config = ConfigUtils.loadConfig(runDirectory + "output_config.xml");
			config.network().setInputFile(null);
			config.plans().setInputFile(runDirectory + "output_plans.xml.gz");
			if (personAttributesFile == null) {
				config.plans().setInputPersonAttributeFile(runDirectory + "output_personAttributes.xml.gz");
			} else {
				config.plans().setInputPersonAttributeFile(personAttributesFile);
			}
			config.vehicles().setVehiclesFile(null);
			config.transit().setTransitScheduleFile(null);
			config.transit().setVehiclesFile(null);
			scenario = ScenarioUtils.loadScenario(config);
			return scenario;
			
		} else {
			Config config = ConfigUtils.loadConfig(runDirectory + runId + ".output_config.xml");
			config.network().setInputFile(null);
			config.plans().setInputFile(runDirectory + runId + ".output_plans.xml.gz");
			if (personAttributesFile == null) {
				config.plans().setInputPersonAttributeFile(runDirectory + runId + ".output_personAttributes.xml.gz");
			} else {
				config.plans().setInputPersonAttributeFile(personAttributesFile);
			}
			config.vehicles().setVehiclesFile(null);
			config.transit().setTransitScheduleFile(null);
			config.transit().setVehiclesFile(null);
			scenario = ScenarioUtils.loadScenario(config);
			return scenario;
		}
	}
		
}
