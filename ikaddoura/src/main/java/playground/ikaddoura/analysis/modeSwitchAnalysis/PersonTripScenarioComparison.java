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

package playground.ikaddoura.analysis.modeSwitchAnalysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;

import playground.ikaddoura.analysis.detailedPersonTripAnalysis.handler.BasicPersonTripAnalysisHandler;

/**
 * ikaddoura
 * 
 */
public class PersonTripScenarioComparison {
	private final static Logger log = Logger.getLogger(PersonTripScenarioComparison.class);
	
	private final String analysisOutputDirectory;
	private final Scenario scenario1;
	private final BasicPersonTripAnalysisHandler basicHandler1;
	private final Scenario scenarioToCompareWith;
	private final BasicPersonTripAnalysisHandler basicHandlerToCompareWith;
	
	private final Map<Id<Person>, Map<Integer, Coord>> personId2actNr2coord;
	private final Map<Id<Person>, Coord> personId2homeActCoord;
	
    public PersonTripScenarioComparison(String homeActivity,
    		String analysisOutputDirectory,
    		Scenario scenario1,
    		BasicPersonTripAnalysisHandler basicHandler1,
    		Scenario scenarioToCompareWith,
    		BasicPersonTripAnalysisHandler basicHandlerToCompareWith) {
    	
		this.analysisOutputDirectory = analysisOutputDirectory;
		this.scenario1 = scenario1;
		this.basicHandler1 = basicHandler1;
		this.scenarioToCompareWith = scenarioToCompareWith;
		this.basicHandlerToCompareWith = basicHandlerToCompareWith;
		
		log.info("Getting activity coordinates from plans...");
		
		personId2actNr2coord = new HashMap<>();
    		personId2homeActCoord = new HashMap<>();
    	  		
    		for (Person person : scenarioToCompareWith.getPopulation().getPersons().values()) {
			int actCounter = 1;
			for (PlanElement pE : person.getSelectedPlan().getPlanElements()) {
				if (pE instanceof Activity) {
					Activity act = (Activity) pE;
					
					if (act.getType().startsWith(homeActivity)) {
						personId2homeActCoord.put(person.getId(), act.getCoord());
					}
					
					if (actCounter == 1) {
						Map<Integer, Coord> actNr2Coord = new HashMap<>();
						actNr2Coord.put(actCounter, act.getCoord());
						personId2actNr2coord.put(person.getId(), actNr2Coord);
					
					} else {
						personId2actNr2coord.get(person.getId()).put(actCounter, act.getCoord());
					}
					
					actCounter++;
				}
			}				
		}
    		log.info("Getting activity coordinates from plans... Done.");
	}

	public void analyzeByMode() throws IOException {
    	
	    	final Map<String,Coord> car2carOrigin = new HashMap<>();
	    	final Map<String,Coord> car2carDestination = new HashMap<>();
	    	final Map<String,Coord> car2carHomeCoord = new HashMap<>();
	
	    	final Map<String,Coord> car2xOrigin = new HashMap<>();
	    	final Map<String,Coord> car2xDestination = new HashMap<>();    	
	    	final Map<String,Coord> car2xHomeCoord = new HashMap<>();
	    	
	    	final Map<String,Coord> x2carOrigin = new HashMap<>();
	    	final Map<String,Coord> x2carDestination = new HashMap<>();
	    	final Map<String,Coord> x2carHomeCoord = new HashMap<>();
	    	
	    	final Map<String,Coord> x2xOrigin = new HashMap<>();
	    	final Map<String,Coord> x2xDestination = new HashMap<>();
	    	final Map<String,Coord> x2xHomeCoord = new HashMap<>();
	
	    	BufferedWriter writerCar = IOUtils.getBufferedWriter( analysisOutputDirectory + "modeSwitchAnalysis_car.csv");
	    	BufferedWriter writerX2Car = IOUtils.getBufferedWriter( analysisOutputDirectory + "modeSwitchAnalysis_x2car.csv");
	    BufferedWriter writerCar2X = IOUtils.getBufferedWriter( analysisOutputDirectory + "modeSwitchAnalysis_car2x.csv");
	    BufferedWriter writerCar2Car = IOUtils.getBufferedWriter( analysisOutputDirectory + "modeSwitchAnalysis_car2car.csv");
	    BufferedWriter writerX2X = IOUtils.getBufferedWriter( analysisOutputDirectory + "modeSwitchAnalysis_x2x.csv");
	
	    writerCar.write("personId;tripNr;mode0;mode1;distance0;distance1;time0;time1;payments0;payments1"); 
	    writerCar.newLine();
	        
	    writerX2Car.write("personId;tripNr;mode0;mode1;distance0;distance1;time0;time1;payments0;payments1");
	    writerX2Car.newLine();
	        
	    writerCar2X.write("personId;tripNr;mode0;mode1;distance0;distance1;time0;time1;payments0;payments1");
	    writerCar2X.newLine();
	    
	    writerCar2Car.write("personId;tripNr;mode0;mode1;distance0;distance1;time0;time1;payments0;payments1");
	    writerCar2Car.newLine();
	        
	    writerX2X.write("personId;tripNr;mode0;mode1;distance0;distance1;time0;time1;payments0;payments1");
	    writerX2X.newLine();
	        
		// mode switch analysis
		log.info("Comparing the two scenarios for each trip and person... (total number of persons: " + basicHandler1.getPersonId2tripNumber2legMode().size() + ")");
			
		Map<Id<Person>, Integer> car2carAgents = new HashMap<>();
		Map<Id<Person>, Integer> x2carAgents = new HashMap<>();
		Map<Id<Person>, Integer> car2xAgents = new HashMap<>();
		Map<Id<Person>, Integer> x2xAgents = new HashMap<>();
	
		int personCounter = 0;
		for (Id<Person> personId : basicHandler1.getPersonId2tripNumber2legMode().keySet()) {
			
			Map<Integer, String> tripNr2legMode = basicHandler1.getPersonId2tripNumber2legMode().get(personId);
		
			for (Integer tripNr : tripNr2legMode.keySet()) {
				String mode1 = tripNr2legMode.get(tripNr);
				
				if (basicHandlerToCompareWith.getPersonId2tripNumber2legMode().get(personId) == null) {
					throw new RuntimeException("Person " + personId + " from run directory1 " + analysisOutputDirectory + "doesn't exist in run directory0 " + analysisOutputDirectory + ". Are you comparing the same scenario? Aborting...");
				}

				String mode0 = "unknown";
				if (basicHandlerToCompareWith.getPersonId2tripNumber2legMode().get(personId).get(tripNr) == null) {
					log.warn("Could not identify the trip mode of person " + personId + " and trip number " + tripNr + ". Setting mode to 'unknown'.");
				} else {
					mode0 = basicHandlerToCompareWith.getPersonId2tripNumber2legMode().get(personId).get(tripNr);
				}
							
				if (mode1.equals(TransportMode.car) || mode0.equals(TransportMode.car)) {
					// at least one trip was a car trip
					writerCar.write(personId + ";" + tripNr + ";"
				+ mode0 + ";" + mode1 + ";" 
				+ basicHandlerToCompareWith.getPersonId2tripNumber2tripDistance().get(personId).get(tripNr) + ";" + basicHandler1.getPersonId2tripNumber2tripDistance().get(personId).get(tripNr) + ";"
				+ basicHandlerToCompareWith.getPersonId2tripNumber2travelTime().get(personId).get(tripNr) + ";" + basicHandler1.getPersonId2tripNumber2travelTime().get(personId).get(tripNr) + ";"
				+ basicHandlerToCompareWith.getPersonId2tripNumber2payment().get(personId).get(tripNr) + ";" + basicHandler1.getPersonId2tripNumber2payment().get(personId).get(tripNr) + ";"
				);
					writerCar.newLine();
				}
				
				if (mode1.equals(TransportMode.car) && !mode0.equals(TransportMode.car)) {
					// x --> car
					writerX2Car.write(personId + ";" + tripNr + ";"
				+ mode0 + ";" + mode1 + ";" 
				+ basicHandlerToCompareWith.getPersonId2tripNumber2tripDistance().get(personId).get(tripNr) + ";" + basicHandler1.getPersonId2tripNumber2tripDistance().get(personId).get(tripNr) + ";"
				+ basicHandlerToCompareWith.getPersonId2tripNumber2travelTime().get(personId).get(tripNr) + ";" + basicHandler1.getPersonId2tripNumber2travelTime().get(personId).get(tripNr) + ";"
				+ basicHandlerToCompareWith.getPersonId2tripNumber2payment().get(personId).get(tripNr) + ";" + basicHandler1.getPersonId2tripNumber2payment().get(personId).get(tripNr) + ";"
				);
					writerX2Car.newLine();
					
					if (x2carAgents.get(personId) == null) {
						x2carAgents.put(personId, 1);
					} else {
						x2carAgents.put(personId, x2carAgents.get(personId) + 1);
					}
					
					x2carOrigin.put(personId + "Trip" + tripNr, personId2actNr2coord.get(personId).get(tripNr));
                	x2carDestination.put(personId + "Trip" + (tripNr), personId2actNr2coord.get(personId).get(tripNr + 1));
                	
                	if (personId2homeActCoord.get(personId) != null) {
						x2carHomeCoord.put(personId.toString(), personId2homeActCoord.get(personId));
                	} else {
						log.warn("No home activity coordinate for person " + personId);
					}
					
				} else if (!mode1.equals(TransportMode.car) && mode0.equals(TransportMode.car)) {
					// car --> x
					writerCar2X.write(personId + ";" + tripNr + ";"
				+ mode0 + ";" + mode1 + ";" 
				+ basicHandlerToCompareWith.getPersonId2tripNumber2tripDistance().get(personId).get(tripNr) + ";" + basicHandler1.getPersonId2tripNumber2tripDistance().get(personId).get(tripNr) + ";"
				+ basicHandlerToCompareWith.getPersonId2tripNumber2travelTime().get(personId).get(tripNr) + ";" + basicHandler1.getPersonId2tripNumber2travelTime().get(personId).get(tripNr) + ";"
				+ basicHandlerToCompareWith.getPersonId2tripNumber2payment().get(personId).get(tripNr) + ";" + basicHandler1.getPersonId2tripNumber2payment().get(personId).get(tripNr) + ";"
				);
					
					writerCar2X.newLine();
					
					if (car2xAgents.get(personId) == null) {
						car2xAgents.put(personId, 1);
					} else {
						car2xAgents.put(personId, car2xAgents.get(personId) + 1);
					}
					
					car2xOrigin.put(personId + "Trip" + tripNr, personId2actNr2coord.get(personId).get(tripNr));
                	car2xDestination.put(personId + "Trip" + (tripNr), personId2actNr2coord.get(personId).get(tripNr + 1));	
                	
                	if (personId2homeActCoord.get(personId) != null) {
						car2xHomeCoord.put(personId.toString(), personId2homeActCoord.get(personId));
                	} else {
						log.warn("No home activity coordinate for person " + personId);
					}
                	
				} else if (mode1.equals(TransportMode.car) && mode0.equals(TransportMode.car)) {
					// car --> car
					writerCar2Car.write(personId + ";" + tripNr + ";"
				+ mode0 + ";" + mode1 + ";" 
				+ basicHandlerToCompareWith.getPersonId2tripNumber2tripDistance().get(personId).get(tripNr) + ";" + basicHandler1.getPersonId2tripNumber2tripDistance().get(personId).get(tripNr) + ";"
				+ basicHandlerToCompareWith.getPersonId2tripNumber2travelTime().get(personId).get(tripNr) + ";" + basicHandler1.getPersonId2tripNumber2travelTime().get(personId).get(tripNr) + ";"
				+ basicHandlerToCompareWith.getPersonId2tripNumber2payment().get(personId).get(tripNr) + ";" + basicHandler1.getPersonId2tripNumber2payment().get(personId).get(tripNr) + ";"
				);
					
					writerCar2Car.newLine();
					
					if (car2carAgents.get(personId) == null) {
						car2carAgents.put(personId, 1);
					} else {
						car2carAgents.put(personId, car2carAgents.get(personId) + 1);
					}
					
					car2carOrigin.put(personId + "Trip" + tripNr, personId2actNr2coord.get(personId).get(tripNr));
					car2carDestination.put(personId + "Trip" + (tripNr), personId2actNr2coord.get(personId).get(tripNr + 1));		
					
					if (personId2homeActCoord.get(personId) != null) {
						car2carHomeCoord.put(personId.toString(), personId2homeActCoord.get(personId));
					} else {
						log.warn("No home activity coordinate for person " + personId);
					}
				
				} else {
					// x --> x
					writerX2X.write(personId + ";" + tripNr + ";"
				+ mode0 + ";" + mode1 + ";" 
				+ basicHandlerToCompareWith.getPersonId2tripNumber2tripDistance().get(personId).get(tripNr) + ";" + basicHandler1.getPersonId2tripNumber2tripDistance().get(personId).get(tripNr) + ";"
				+ basicHandlerToCompareWith.getPersonId2tripNumber2travelTime().get(personId).get(tripNr) + ";" + basicHandler1.getPersonId2tripNumber2travelTime().get(personId).get(tripNr) + ";"
				+ basicHandlerToCompareWith.getPersonId2tripNumber2payment().get(personId).get(tripNr) + ";" + basicHandler1.getPersonId2tripNumber2payment().get(personId).get(tripNr) + ";"
				);
								
					writerX2X.newLine();
					
					if (x2xAgents.get(personId) == null) {
						x2xAgents.put(personId, 1);
					} else {
						x2xAgents.put(personId, x2xAgents.get(personId) + 1);
					}
								
					x2xOrigin.put(personId + "Trip" + tripNr, personId2actNr2coord.get(personId).get(tripNr));
					x2xDestination.put(personId + "Trip" + (tripNr), personId2actNr2coord.get(personId).get(tripNr + 1));		
								
					if (personId2homeActCoord.get(personId) != null) {
						x2xHomeCoord.put(personId.toString(), personId2homeActCoord.get(personId));
					} else {
						log.warn("No home activity coordinate for person " + personId);
					}
				}
			}
			
			if (personCounter%100000 == 0) {
				log.info("person #" + personCounter);
			}
			personCounter++;
		}
		writerCar.close();
		writerX2Car.close();
		writerCar2X.close();
		writerCar2Car.close();
		writerX2X.close();
		
		log.info("Comparing the two scenarios for each trip and person... Done.");
		
		log.info("Comparing the two scenarios for each person...");
		
		{
			BufferedWriter writer = IOUtils.getBufferedWriter(analysisOutputDirectory + "/winner-loser-analysis_all.csv");
	        writer.write("PersonId;homeCoordX;homeCoordY;totalTrips;score0 [utils];score1 [utils]");
	        writer.newLine();
	       
	        double score0Sum = 0.;
	        double score1Sum = 0.;
	        
			for (Id<Person> personId : scenario1.getPopulation().getPersons().keySet()) {
				
				double score0 = scenarioToCompareWith.getPopulation().getPersons().get(personId).getSelectedPlan().getScore();
		        double score1 = scenario1.getPopulation().getPersons().get(personId).getSelectedPlan().getScore();
				
		        int numberOfTrips = 0;
		        if (basicHandler1.getPersonId2tripNumber2legMode().get(personId) != null) {
		        		numberOfTrips = basicHandler1.getPersonId2tripNumber2legMode().get(personId).size();
		        }
		        
				writer.write(personId + ";"
    	        + personId2homeActCoord.get(personId).getX() + ";"
    	        	+ personId2homeActCoord.get(personId).getY() + ";"    
	        	+ numberOfTrips + ";"
			+ score0 + ";"
			+ score1
			);
	        		writer.newLine();
	        	
	        		score0Sum += score0;
	        		score1Sum += score1;
	        } 
			
			writer.newLine();
        		writer.write("Average score difference: " + (score1Sum - score0Sum) / (double) scenario1.getPopulation().getPersons().size() );
        		log.info("all agents: Average score difference: " + (score1Sum - score0Sum) / (double) scenario1.getPopulation().getPersons().size() );
		
        		writer.close();
		}
		
		{
			BufferedWriter writer = IOUtils.getBufferedWriter(analysisOutputDirectory + "/winner-loser-analysis_car2car.csv");
	        writer.write("PersonId;homeCoordX;homeCoordY;totalTrips;car2carTrips;score0 [utils];score1 [utils]");
	        writer.newLine();
	       
	        double score0Sum = 0.;
	        double score1Sum = 0.;
	        
			for (Id<Person> personId : car2carAgents.keySet()) {
				
				double score0 = scenarioToCompareWith.getPopulation().getPersons().get(personId).getSelectedPlan().getScore();
		        double score1 = scenario1.getPopulation().getPersons().get(personId).getSelectedPlan().getScore();
				
		        writer.write(personId + ";"
	    	    + personId2homeActCoord.get(personId).getX() + ";"
	    		+ personId2homeActCoord.get(personId).getY() + ";"
	        	+ basicHandler1.getPersonId2tripNumber2legMode().get(personId).size() + ";"
			+ car2carAgents.get(personId) + ";"
			+ score0 + ";"
			+ score1
			);
	        		writer.newLine();
	        	
	        		score0Sum += score0;
	        		score1Sum += score1;
	        } 
			
			writer.newLine();
        		writer.write("Average score difference: " + (score1Sum - score0Sum) / (double) car2carAgents.size() );
        		log.info("car2car agents: Average score difference: " + (score1Sum - score0Sum) / (double) car2carAgents.size() );
		
        		writer.close();
		}
		
		{
			BufferedWriter writer = IOUtils.getBufferedWriter(analysisOutputDirectory + "/winner-loser-analysis_x2car.csv");
	        writer.write("PersonId;homeCoordX;homeCoordY;totalTrips;x2carTrips;score0 [utils];score1 [utils]");
	        writer.newLine();
	       
	        double score0Sum = 0.;
	        double score1Sum = 0.;
	        
			for (Id<Person> personId : x2carAgents.keySet()) {
				
				double score0 = scenarioToCompareWith.getPopulation().getPersons().get(personId).getSelectedPlan().getScore();
		        double score1 = scenario1.getPopulation().getPersons().get(personId).getSelectedPlan().getScore();
				
		        writer.write(personId + ";"
	    	    	+ personId2homeActCoord.get(personId).getX() + ";"
	    	    	+ personId2homeActCoord.get(personId).getY() + ";"
	        	+ basicHandler1.getPersonId2tripNumber2legMode().get(personId).size() + ";"
			+ x2carAgents.get(personId) + ";"
			+ score0 + ";"
			+ score1
			);
	        		writer.newLine();
	        	
	        		score0Sum += score0;
	        		score1Sum += score1;
	        } 
	        
			writer.newLine();
			writer.write("Average score difference: " + (score1Sum - score0Sum) / (double) x2carAgents.size() );
			log.info("x2car agents: Average score difference: " + (score1Sum - score0Sum) / (double) x2carAgents.size() );
		
	        writer.close();

		}
		
		{
			BufferedWriter writer = IOUtils.getBufferedWriter(analysisOutputDirectory + "/winner-loser-analysis_car2x.csv");
	        writer.write("PersonId;homeCoordX;homeCoordY;totalTrips;car2xTrips;score0 [utils];score1 [utils]");
	        writer.newLine();
	       
	        double score0Sum = 0.;
	        double score1Sum = 0.;
	        
			for (Id<Person> personId : car2xAgents.keySet()) {
				
				double score0 = scenarioToCompareWith.getPopulation().getPersons().get(personId).getSelectedPlan().getScore();
		        double score1 = scenario1.getPopulation().getPersons().get(personId).getSelectedPlan().getScore();
				
		        writer.write(personId + ";"
		    + personId2homeActCoord.get(personId).getX() + ";"
		    + personId2homeActCoord.get(personId).getY() + ";"
	        + basicHandler1.getPersonId2tripNumber2legMode().get(personId).size() + ";"
			+ car2xAgents.get(personId) + ";"
			+ score0 + ";"
			+ score1 
			);
		        writer.newLine();
	        	
	        		score0Sum += score0;
	        		score1Sum += score1;
	        } 
	        
        		writer.newLine();
        		writer.write("Average score difference: " + (score1Sum - score0Sum) / (double) car2xAgents.size() );
        		log.info("car2x agents: Average score difference: " + (score1Sum - score0Sum) / (double) car2xAgents.size() );
		
	        writer.close();

		}
		
		{
			BufferedWriter writer = IOUtils.getBufferedWriter(analysisOutputDirectory + "/winner-loser-analysis_x2x.csv");
	        writer.write("PersonId;homeCoordX;homeCoordY;totalTrips;x2xTrips;score0 [utils];score1 [utils]");
	        writer.newLine();
	       
	        double score0Sum = 0.;
	        double score1Sum = 0.;
	        
			for (Id<Person> personId : x2xAgents.keySet()) {
				
				double score0 = scenarioToCompareWith.getPopulation().getPersons().get(personId).getSelectedPlan().getScore();
		        double score1 = scenario1.getPopulation().getPersons().get(personId).getSelectedPlan().getScore();
				
		        writer.write(personId + ";"
			+ basicHandler1.getPersonId2tripNumber2legMode().get(personId).size() + ";"
			+ x2xAgents.get(personId) + ";"
			+ score0 + ";"
			+ score1
			);
		        writer.newLine();
	        	
	        		score0Sum += score0;
	        		score1Sum += score1;
	        } 
			
			writer.newLine();
			writer.write("Average score difference: " + (score1Sum - score0Sum) / (double) x2xAgents.size() );
			log.info("x2x agents: Average score difference: " + (score1Sum - score0Sum) / (double) x2xAgents.size() );
		
	        writer.close();

		}
		log.info("Comparing the two scenarios for each person... Done.");
        	
		printCoordinates(car2carOrigin, analysisOutputDirectory  + "/modeSwitchAnalysis_actCoord_car2car_origin.csv");
        printCoordinates(car2carDestination, analysisOutputDirectory  + "/modeSwitchAnalysis_actCoord_car2car_destination.csv");
        printCoordinates(car2carHomeCoord, analysisOutputDirectory  + "/modeSwitchAnalysis_car2car_homeCoord.csv");
        printODLines(car2carOrigin, car2carDestination, analysisOutputDirectory  + "/modeSwitchAnalysis_car2car_OD.shp");
        
        printCoordinates(car2xOrigin, analysisOutputDirectory + "/modeSwitchAnalysis_actCoord_car2x_origin.csv");
        printCoordinates(car2xDestination, analysisOutputDirectory + "/modeSwitchAnalysis_actCoord_car2x_destination.csv");
        printCoordinates(car2xHomeCoord, analysisOutputDirectory + "/modeSwitchAnalysis_car2x_homeCoord.csv");
        printODLines(car2xOrigin, car2xDestination, analysisOutputDirectory + "/modeSwitchAnalysis_car2x_OD.shp");

        printCoordinates(x2carOrigin, analysisOutputDirectory + "/modeSwitchAnalysis_actCoord_x2car_origin.csv");
        printCoordinates(x2carDestination, analysisOutputDirectory + "/modeSwitchAnalysis_actCoord_x2car_destination.csv");
        printCoordinates(x2carHomeCoord, analysisOutputDirectory + "/modeSwitchAnalysis_x2car_homeCoord.csv");
        printODLines(x2carOrigin, x2carDestination, analysisOutputDirectory + "/modeSwitchAnalysis_x2car_OD.shp");
        
        printCoordinates(x2xOrigin, analysisOutputDirectory + "/modeSwitchAnalysis_actCoord_x2x_origin.csv");
        printCoordinates(x2xDestination, analysisOutputDirectory + "/modeSwitchAnalysis_actCoord_x2x_destination.csv");
        printCoordinates(x2xHomeCoord, analysisOutputDirectory + "/modeSwitchAnalysis_x2x_homeCoord.csv");
        printODLines(x2xOrigin, x2xDestination, analysisOutputDirectory + "/modeSwitchAnalysis_x2x_OD.shp");
    }

	private void printCoordinates(Map<String, Coord> id2Coord, String fileName) throws IOException {
        BufferedWriter writer = IOUtils.getBufferedWriter(fileName);
        writer.write("Id;xCoord;yCoord");
        writer.newLine();
        for (String personTripNr : id2Coord.keySet()) {
        		writer.write(personTripNr + ";" + id2Coord.get(personTripNr).getX() + ";" + id2Coord.get(personTripNr).getY());
        		writer.newLine();
        } 
        writer.close();
	}
	
	private void printODLines(Map<String, Coord> id2CoordOrigin, Map<String, Coord> id2CoordDestination, String fileName) throws IOException {
        
		PolylineFeatureFactory factory = new PolylineFeatureFactory.Builder()
        		.setCrs(MGC.getCRS(TransformationFactory.DHDN_GK4))
        		.setName("TripOD")
        		.addAttribute("PersTripId", String.class)
        		.create();
        		
        		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
        						
                for (String personTripNr : id2CoordOrigin.keySet()) {
                	SimpleFeature feature = factory.createPolyline(
    						
                			new Coordinate[] {
    								new Coordinate(MGC.coord2Coordinate(id2CoordOrigin.get(personTripNr))),
    								new Coordinate(MGC.coord2Coordinate(id2CoordDestination.get(personTripNr))) }
    						
    						, new Object[] {personTripNr}
                			, null
    				);
    				features.add(feature);
        		}
        		
        		ShapeFileWriter.writeGeometries(features, fileName);
	}

	public void analyzeByScore(double scoreDifferenceTolerance) {
		
		Map<Id<Person>, Tuple<Double, Double>> person2score0score1 = new HashMap<>();
		Set<Id<Person>> winners = new HashSet<>();
		Set<Id<Person>> losers = new HashSet<>();
		Set<Id<Person>> sameScorePersons = new HashSet<>();
		
		for (Person person : this.scenario1.getPopulation().getPersons().values()) {
			
			double score1 = person.getSelectedPlan().getScore();
			double score0 = this.scenarioToCompareWith.getPopulation().getPersons().get(person.getId()).getSelectedPlan().getScore();
			
			person2score0score1.put(person.getId(), new Tuple<Double, Double>(score0, score1));
			
			if (score1 > score0 + scoreDifferenceTolerance) {
				winners.add(person.getId());
				
			} else if (score0 > score1 + scoreDifferenceTolerance) {
				losers.add(person.getId());
			
			} else {
				sameScorePersons.add(person.getId());
			}
		}
	
		try {
			printCSVFile(sameScorePersons, person2score0score1, this.analysisOutputDirectory + "winner-loser-analysis_same-score-persons_score-tolerance-" + scoreDifferenceTolerance + ".csv");
			printCSVFile(winners, person2score0score1, this.analysisOutputDirectory + "winner-loser-analysis_winners_score-tolerance-" + scoreDifferenceTolerance + ".csv");
			printCSVFile(losers, person2score0score1, this.analysisOutputDirectory + "winner-loser-analysis_losers_score-tolerance-" + scoreDifferenceTolerance + ".csv");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void printCSVFile(Set<Id<Person>> persons, Map<Id<Person>, Tuple<Double, Double>> person2score0score1, String fileName) throws IOException {
		BufferedWriter writer = IOUtils.getBufferedWriter(fileName);
        writer.write("Id;homeCoordX;homeCoordY;score0 [utils];score1 [utils];score-difference [utils]");
        writer.newLine();
        for (Id<Person> personId : persons) {
        		writer.write(personId + ";"
        + this.personId2homeActCoord.get(personId).getX()
        + ";" + this.personId2homeActCoord.get(personId).getY()
        + ";" + person2score0score1.get(personId).getFirst()
        + ";" + person2score0score1.get(personId).getSecond()
        + ";" + (person2score0score1.get(personId).getSecond() - person2score0score1.get(personId).getFirst())
        );
        		writer.newLine();
        } 
        writer.close();
	}
}
