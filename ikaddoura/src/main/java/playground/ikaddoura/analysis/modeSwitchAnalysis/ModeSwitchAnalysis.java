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
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;

import playground.ikaddoura.analysis.detailedPersonTripAnalysis.IKEventsReader;
import playground.ikaddoura.analysis.detailedPersonTripAnalysis.handler.BasicPersonTripAnalysisHandler;
import playground.ikaddoura.analysis.detailedPersonTripAnalysis.handler.PersonMoneyLinkHandler;

/**
 * ikaddoura
 * 
 */
public class ModeSwitchAnalysis {
	private final static Logger log = Logger.getLogger(ModeSwitchAnalysis.class);
	
    public static void main(String[] args) throws IOException {
        ModeSwitchAnalysis modeSwitcherAnalyser = new ModeSwitchAnalysis();
        modeSwitcherAnalyser.analyze();
    }

    public void analyze() throws IOException {
    	
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

    	Map<Id<Person>, Map<Integer, Coord>> personId2actNr2coord = new HashMap<>();
    	Map<Id<Person>, Coord> personId2homeActCoord = new HashMap<>();

    	// ################

    	int finalIteration = 200;
    	
        // 0: base case
    	String directory0 = "/Users/ihab/Documents/workspace/runs-svn/optAV/output/output_v0_SAVuserOpCostPricingF_SAVuserExtCostPricingF_SAVdriverExtCostPricingF_CCuserExtCostPricingF/";
    	String runId0 = "run0";
    	
		// 1: policy case
    	String directory1 = "/Users/ihab/Documents/workspace/runs-svn/optAV/output/output_v0_SAVuserOpCostPricingF_SAVuserExtCostPricingF_SAVdriverExtCostPricingF_CCuserExtCostPricingT/";
    	String runId1 = "run1";
    	
    	// ################
    	
		String dir0lastIterationFile = directory0 + "ITERS/it." + finalIteration +"/"+ runId0 + "." + finalIteration +".events.xml.gz";
	    String dir0networkFile = directory0 + runId0 + ".output_network.xml.gz";
	    String dir0populationFile = directory0 + runId0 + ".output_plans.xml.gz";
	    
	    String dir1lastIterationFile = directory1 + "ITERS/it." + finalIteration +"/"+ runId1 + "." + finalIteration +".events.xml.gz";
	    String dir1networkFile = directory1 + runId1 + ".output_network.xml.gz";
	    String dir1populationFile = directory1 + runId1 + ".output_plans.xml.gz";	  
		
		String analysisOutputFolder = "modeSwitchAnalysis/";
		File f = new File(directory1 + analysisOutputFolder);
		f.mkdirs();
		
		BasicPersonTripAnalysisHandler basicHandler0;
		BasicPersonTripAnalysisHandler basicHandler1;
		
		PersonMoneyLinkHandler moneyHandler0;
		PersonMoneyLinkHandler moneyHandler1;
		
		Scenario scenario0;
		{
			log.info("Loading scenario0 and reading events...");

			Config config = ConfigUtils.createConfig();	
			config.plans().setInputFile(dir0populationFile);
			config.network().setInputFile(dir0networkFile);
			
			scenario0 = ScenarioUtils.loadScenario(config);
	        
	        basicHandler0 = new BasicPersonTripAnalysisHandler();
			basicHandler0.setScenario(scenario0);
			
			moneyHandler0 = new PersonMoneyLinkHandler();
			moneyHandler0.setBasicHandler(basicHandler0);
			
			EventsManager events = EventsUtils.createEventsManager();
			events.addHandler(basicHandler0);
			events.addHandler(moneyHandler0);
			
			IKEventsReader reader = new IKEventsReader(events);
			reader.readFile(dir0lastIterationFile);
			log.info("Loading scenario0 and reading events... Done.");
		}
		
		Scenario scenario1;
		{
			log.info("Loading scenario1 and reading events...");
			Config config = ConfigUtils.createConfig();	
			config.plans().setInputFile(dir1populationFile);
			config.network().setInputFile(dir1networkFile);
			
			scenario1 = ScenarioUtils.loadScenario(config);
	        
	        basicHandler1 = new BasicPersonTripAnalysisHandler();
			basicHandler1.setScenario(scenario1);
			
			moneyHandler1 = new PersonMoneyLinkHandler();
			moneyHandler1.setBasicHandler(basicHandler1);
			
			EventsManager events = EventsUtils.createEventsManager();
			events.addHandler(basicHandler1);
			events.addHandler(moneyHandler1);
			
			IKEventsReader reader = new IKEventsReader(events);
			reader.readFile(dir1lastIterationFile);
			
			log.info("Loading scenario1 and reading events... Done.");

			log.info("Getting home coordinates from plans...");
			for (Person person : scenario1.getPopulation().getPersons().values()) {
				int actCounter = 1;
				for (PlanElement pE : person.getSelectedPlan().getPlanElements()) {
					if (pE instanceof Activity) {
						Activity act = (Activity) pE;
						
						if (act.getType().startsWith("home")) {
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
			log.info("Getting home coordinates from plans... Done.");
		}
		
        BufferedWriter writerCar = IOUtils.getBufferedWriter( directory1 + analysisOutputFolder + "modeSwitchAnalysis_car.csv");
        BufferedWriter writerX2Car = IOUtils.getBufferedWriter( directory1 + analysisOutputFolder + "modeSwitchAnalysis_x2car.csv");
        BufferedWriter writerCar2X = IOUtils.getBufferedWriter( directory1 + analysisOutputFolder + "modeSwitchAnalysis_car2x.csv");
        BufferedWriter writerCar2Car = IOUtils.getBufferedWriter( directory1 + analysisOutputFolder + "modeSwitchAnalysis_car2car.csv");
        BufferedWriter writerX2X = IOUtils.getBufferedWriter( directory1 + analysisOutputFolder + "modeSwitchAnalysis_x2x.csv");

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
		log.info("Comparing the two scenarios for each person... (total number of persons: " + basicHandler1.getPersonId2tripNumber2legMode().size() + ")");
		
		Map<Id<Person>, Integer> car2carAgents = new HashMap<>();
		Map<Id<Person>, Integer> x2carAgents = new HashMap<>();
		Map<Id<Person>, Integer> car2xAgents = new HashMap<>();
		Map<Id<Person>, Integer> x2xAgents = new HashMap<>();

		int personCounter = 0;
		for (Id<Person> personId : basicHandler1.getPersonId2tripNumber2legMode().keySet()) {
			
			Map<Integer, String> tripNr2legMode = basicHandler1.getPersonId2tripNumber2legMode().get(personId);
			for (Integer tripNr : tripNr2legMode.keySet()) {
				String mode1 = tripNr2legMode.get(tripNr);
				
				if (basicHandler0.getPersonId2tripNumber2legMode().get(personId) == null) {
					throw new RuntimeException("Person " + personId + " from run directory1 " + directory1 + "doesn't exist in run directory0 " + directory0 + ". Are you comparing the same scenario? Aborting...");
				}

				String mode0 = "unknown";
				if (basicHandler0.getPersonId2tripNumber2legMode().get(personId).get(tripNr) == null) {
					log.warn("Could not identify the trip mode of person " + personId + " and trip number " + tripNr + ". Setting mode to 'unknown'.");
				} else {
					mode0 = basicHandler0.getPersonId2tripNumber2legMode().get(personId).get(tripNr);
				}
							
				if (mode1.equals(TransportMode.car) || mode0.equals(TransportMode.car)) {
					// at least one trip was a car trip
					writerCar.write(personId + ";" + tripNr + ";"
				+ mode0 + ";" + mode1 + ";" 
				+ basicHandler0.getPersonId2tripNumber2tripDistance().get(personId).get(tripNr) + ";" + basicHandler1.getPersonId2tripNumber2tripDistance().get(personId).get(tripNr) + ";"
				+ basicHandler0.getPersonId2tripNumber2travelTime().get(personId).get(tripNr) + ";" + basicHandler1.getPersonId2tripNumber2travelTime().get(personId).get(tripNr) + ";"
				+ basicHandler0.getPersonId2tripNumber2payment().get(personId).get(tripNr) + ";" + basicHandler1.getPersonId2tripNumber2payment().get(personId).get(tripNr) + ";"
				);
					writerCar.newLine();
				}
				
				if (mode1.equals(TransportMode.car) && !mode0.equals(TransportMode.car)) {
					// x --> car
					writerX2Car.write(personId + ";" + tripNr + ";"
				+ mode0 + ";" + mode1 + ";" 
				+ basicHandler0.getPersonId2tripNumber2tripDistance().get(personId).get(tripNr) + ";" + basicHandler1.getPersonId2tripNumber2tripDistance().get(personId).get(tripNr) + ";"
				+ basicHandler0.getPersonId2tripNumber2travelTime().get(personId).get(tripNr) + ";" + basicHandler1.getPersonId2tripNumber2travelTime().get(personId).get(tripNr) + ";"
				+ basicHandler0.getPersonId2tripNumber2payment().get(personId).get(tripNr) + ";" + basicHandler1.getPersonId2tripNumber2payment().get(personId).get(tripNr) + ";"
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
				+ basicHandler0.getPersonId2tripNumber2tripDistance().get(personId).get(tripNr) + ";" + basicHandler1.getPersonId2tripNumber2tripDistance().get(personId).get(tripNr) + ";"
				+ basicHandler0.getPersonId2tripNumber2travelTime().get(personId).get(tripNr) + ";" + basicHandler1.getPersonId2tripNumber2travelTime().get(personId).get(tripNr) + ";"
				+ basicHandler0.getPersonId2tripNumber2payment().get(personId).get(tripNr) + ";" + basicHandler1.getPersonId2tripNumber2payment().get(personId).get(tripNr) + ";"
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
				+ basicHandler0.getPersonId2tripNumber2tripDistance().get(personId).get(tripNr) + ";" + basicHandler1.getPersonId2tripNumber2tripDistance().get(personId).get(tripNr) + ";"
				+ basicHandler0.getPersonId2tripNumber2travelTime().get(personId).get(tripNr) + ";" + basicHandler1.getPersonId2tripNumber2travelTime().get(personId).get(tripNr) + ";"
				+ basicHandler0.getPersonId2tripNumber2payment().get(personId).get(tripNr) + ";" + basicHandler1.getPersonId2tripNumber2payment().get(personId).get(tripNr) + ";"
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
				+ basicHandler0.getPersonId2tripNumber2tripDistance().get(personId).get(tripNr) + ";" + basicHandler1.getPersonId2tripNumber2tripDistance().get(personId).get(tripNr) + ";"
				+ basicHandler0.getPersonId2tripNumber2travelTime().get(personId).get(tripNr) + ";" + basicHandler1.getPersonId2tripNumber2travelTime().get(personId).get(tripNr) + ";"
				+ basicHandler0.getPersonId2tripNumber2payment().get(personId).get(tripNr) + ";" + basicHandler1.getPersonId2tripNumber2payment().get(personId).get(tripNr) + ";"
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
		
		log.info("Comparing the two scenarios for each person... Done.");
		
		{
			BufferedWriter writer = IOUtils.getBufferedWriter(directory1 + analysisOutputFolder + "/personAnalysis_car2car.csv");
	        writer.write("PersonId;totalTrips;car2carTrips;score0;score1");
	        writer.newLine();
	       
	        double score0Sum = 0.;
	        double score1Sum = 0.;
	        
			for (Id<Person> personId : car2carAgents.keySet()) {
				
				double score0 = scenario0.getPopulation().getPersons().get(personId).getSelectedPlan().getScore();
		        double score1 = scenario1.getPopulation().getPersons().get(personId).getSelectedPlan().getScore();
				
	        	writer.write(personId + ";"
	        + basicHandler1.getPersonId2tripNumber2legMode().get(personId).size() + ";"
			+ car2carAgents.get(personId) + ";"
			+ score0 + ";"
			+ score1
			);
	        	writer.newLine();
	        	
	        	score0Sum += score0;
	        	score1Sum += score1;
	        } 
	        writer.close();
	        
        	writer.newLine();
        	writer.write("Average score difference: " + (score1Sum - score0Sum) / (double) car2carAgents.size() );
        	log.info("car2car agents: Average score difference: " + (score1Sum - score0Sum) / (double) car2carAgents.size() );
		}
		
		{
			BufferedWriter writer = IOUtils.getBufferedWriter(directory1 + analysisOutputFolder + "/personAnalysis_x2car.csv");
	        writer.write("PersonId;totalTrips;x2carTrips;score0;score1");
	        writer.newLine();
	       
	        double score0Sum = 0.;
	        double score1Sum = 0.;
	        
			for (Id<Person> personId : x2carAgents.keySet()) {
				
				double score0 = scenario0.getPopulation().getPersons().get(personId).getSelectedPlan().getScore();
		        double score1 = scenario1.getPopulation().getPersons().get(personId).getSelectedPlan().getScore();
				
	        	writer.write(personId + ";"
	        + basicHandler1.getPersonId2tripNumber2legMode().get(personId).size() + ";"
			+ x2carAgents.get(personId) + ";"
			+ score0 + ";"
			+ score1
			);
	        	writer.newLine();
	        	
	        	score0Sum += score0;
	        	score1Sum += score1;
	        } 
	        writer.close();
	        
        	writer.newLine();
        	writer.write("Average score difference: " + (score1Sum - score0Sum) / (double) x2carAgents.size() );
        	log.info("x2car agents: Average score difference: " + (score1Sum - score0Sum) / (double) x2carAgents.size() );
		}
		
		{
			BufferedWriter writer = IOUtils.getBufferedWriter(directory1 + analysisOutputFolder + "/personAnalysis_car2x.csv");
	        writer.write("PersonId;totalTrips;car2xTrips;score0;score1");
	        writer.newLine();
	       
	        double score0Sum = 0.;
	        double score1Sum = 0.;
	        
			for (Id<Person> personId : car2xAgents.keySet()) {
				
				double score0 = scenario0.getPopulation().getPersons().get(personId).getSelectedPlan().getScore();
		        double score1 = scenario1.getPopulation().getPersons().get(personId).getSelectedPlan().getScore();
				
	        	writer.write(personId + ";"
	        + basicHandler1.getPersonId2tripNumber2legMode().get(personId).size() + ";"
			+ car2xAgents.get(personId) + ";"
			+ score0 + ";"
			+ score1 
			);
	        	writer.newLine();
	        	
	        	score0Sum += score0;
	        	score1Sum += score1;
	        } 
	        writer.close();
	        
        	writer.newLine();
        	writer.write("Average score difference: " + (score1Sum - score0Sum) / (double) car2xAgents.size() );
        	log.info("car2x agents: Average score difference: " + (score1Sum - score0Sum) / (double) car2xAgents.size() );
		}
		
		{
			BufferedWriter writer = IOUtils.getBufferedWriter(directory1 + analysisOutputFolder + "/personAnalysis_x2x.csv");
	        writer.write("PersonId;totalTrips;x2xTrips;score0;score1");
	        writer.newLine();
	       
	        double score0Sum = 0.;
	        double score1Sum = 0.;
	        
			for (Id<Person> personId : x2xAgents.keySet()) {
				
				double score0 = scenario0.getPopulation().getPersons().get(personId).getSelectedPlan().getScore();
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
	        writer.close();
	        
        	writer.newLine();
        	writer.write("Average score difference: " + (score1Sum - score0Sum) / (double) x2xAgents.size() );
        	log.info("x2x agents: Average score difference: " + (score1Sum - score0Sum) / (double) x2xAgents.size() );
		}
        	
		printCoordinates(car2carOrigin, directory1 + analysisOutputFolder + "/spatialModeSwitchAnalysis_actCoord_car2car_origin.csv");
        printCoordinates(car2carDestination, directory1 + analysisOutputFolder + "/spatialModeSwitchAnalysis_actCoord_car2car_destination.csv");
        printCoordinates(car2carHomeCoord, directory1 + analysisOutputFolder + "/spatialModeSwitchAnalysis_car2car_homeCoord.csv");
        printODLines(car2carOrigin, car2carDestination, directory1 + analysisOutputFolder + "/spatialModeSwitchAnalysis_car2car_OD.shp");
        
        printCoordinates(car2xOrigin, directory1 + analysisOutputFolder + "/spatialModeSwitchAnalysis_actCoord_car2x_origin.csv");
        printCoordinates(car2xDestination, directory1 + analysisOutputFolder + "/spatialModeSwitchAnalysis_actCoord_car2x_destination.csv");
        printCoordinates(car2xHomeCoord, directory1 + analysisOutputFolder + "/spatialModeSwitchAnalysis_car2x_homeCoord.csv");
        printODLines(car2xOrigin, car2xDestination, directory1 + analysisOutputFolder + "/spatialModeSwitchAnalysis_car2x_OD.shp");

        printCoordinates(x2carOrigin, directory1 + analysisOutputFolder + "/spatialModeSwitchAnalysis_actCoord_x2car_origin.csv");
        printCoordinates(x2carDestination, directory1 + analysisOutputFolder + "/spatialModeSwitchAnalysis_actCoord_x2car_destination.csv");
        printCoordinates(x2carHomeCoord, directory1 + analysisOutputFolder + "/spatialModeSwitchAnalysis_x2car_homeCoord.csv");
        printODLines(x2carOrigin, x2carDestination, directory1 + analysisOutputFolder + "/spatialModeSwitchAnalysis_x2car_OD.shp");
        
        printCoordinates(x2xOrigin, directory1 + analysisOutputFolder + "/spatialModeSwitchAnalysis_actCoord_x2x_origin.csv");
        printCoordinates(x2xDestination, directory1 + analysisOutputFolder + "/spatialModeSwitchAnalysis_actCoord_x2x_destination.csv");
        printCoordinates(x2xHomeCoord, directory1 + analysisOutputFolder + "/spatialModeSwitchAnalysis_x2x_homeCoord.csv");
        printODLines(x2xOrigin, x2xDestination, directory1 + analysisOutputFolder + "/spatialModeSwitchAnalysis_x2x_OD.shp");
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
}
