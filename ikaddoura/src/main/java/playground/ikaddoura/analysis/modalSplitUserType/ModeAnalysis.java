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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;

/**
 * 
 * @author ikaddoura
 *
 */
public class ModeAnalysis {
	
	private static final Logger log = Logger.getLogger(ModeAnalysis.class);	
	private Scenario scenario;
	private AgentFilter filter;
	
	private Map<String, Integer> mode2TripCounterFiltered = new HashMap<>();		
	private Map<String, List<Double>> mode2TripRouteDistancesFiltered = new HashMap<>();
	private Map<String, List<Double>> mode2TripEuclideanDistancesFiltered = new HashMap<>();		

	private double totalTripsFiltered = 0.;

	public ModeAnalysis(Scenario scenario, AgentAnalysisFilter filter) {
		this.scenario = scenario;
		this.filter = filter;
	}

	public static void main(String[] args) {
		
		final String runDirectory = "/Users/ihab/Desktop/test-run-directory/";
		final String outputDirectory = "/Users/ihab/Desktop/modal-split-analysis/";
		final String runId = "test";
		
		// optional: Provide a personAttributes file which is used instead of the normal output person attributes file
		final String personAttributesFile = "/Users/ihab/Desktop/test-run-directory/test.output_personAttributes.xml.gz";
		
		Scenario scenario = loadScenario(runDirectory, runId, personAttributesFile);
		
		AgentAnalysisFilter filter = new AgentAnalysisFilter(scenario);
		
		filter.setSubpopulation("person");
		
		filter.setPersonAttribute("berlin");
		filter.setPersonAttributeName("home-activity-zone");
		
		filter.setZoneFile(null);
		filter.setRelevantActivityType(null);
		
		filter.preProcess(scenario);
				
		ModeAnalysis analysis = new ModeAnalysis(scenario, filter);
		analysis.run();
		
		File directory = new File(outputDirectory);
		directory.mkdirs();
		
		analysis.writeModeShares(outputDirectory);
		analysis.writeTripRouteDistances(outputDirectory);
		analysis.writeTripEuclideanDistances(outputDirectory);
		
		List<Tuple<Double, Double>> distanceGroups = new ArrayList<>();
		distanceGroups.add(new Tuple<>(0., 1000.));
		distanceGroups.add(new Tuple<>(1000., 3000.));
		distanceGroups.add(new Tuple<>(3000., 5000.));
		distanceGroups.add(new Tuple<>(5000., 10000.));
		distanceGroups.add(new Tuple<>(10000., 20000.));
		distanceGroups.add(new Tuple<>(20000., 100000.));
		analysis.writeTripRouteDistances(outputDirectory, distanceGroups);
		analysis.writeTripEuclideanDistances(outputDirectory, distanceGroups);
	}
		
	public void run() {
		
		int counter = 0;
		for (Person person : scenario.getPopulation().getPersons().values()) {
			
			if (counter%10000 == 0) {
				log.info("Person #" + counter);
			}
			
			if (filter == null || filter.considerAgent(person)) {
				Leg previousMainModeLeg = null;
				Activity previousRealActivity = null;
				double previousLegTotalDistance = 0.;

				for (PlanElement pE : person.getSelectedPlan().getPlanElements()) {
									
					if (pE instanceof Leg) {
						Leg leg = (Leg) pE;
						
						if (leg.getMode().equals("transit_walk") || leg.getMode().equals("egress_walk") || leg.getMode().equals("access_walk")) {
							// skipping help leg modes
							
						} else {
							if (previousMainModeLeg == null) {
								previousMainModeLeg = leg;
								previousLegTotalDistance = leg.getRoute().getDistance();
							} else {
								if (previousMainModeLeg.getMode().equals(leg.getMode())) {
									previousMainModeLeg = leg;
									previousLegTotalDistance = previousLegTotalDistance + leg.getRoute().getDistance();
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
								
								totalTripsFiltered++;

								if (mode2TripCounterFiltered.containsKey(previousMainModeLeg.getMode())) {
									
									mode2TripCounterFiltered.put(previousMainModeLeg.getMode(), mode2TripCounterFiltered.get(previousMainModeLeg.getMode()) + 1);
									
									mode2TripRouteDistancesFiltered.get(previousMainModeLeg.getMode()).add(previousLegTotalDistance);
									
									double euclideanDistance = CoordUtils.calcEuclideanDistance(previousRealActivity.getCoord(), activity.getCoord());
									mode2TripEuclideanDistancesFiltered.get(previousMainModeLeg.getMode()).add(euclideanDistance);
									
								} else {
									
									mode2TripCounterFiltered.put(previousMainModeLeg.getMode(), 1);
									
									List<Double> routeDistances = new ArrayList<>();
									routeDistances.add(previousLegTotalDistance);
									mode2TripRouteDistancesFiltered.put(previousMainModeLeg.getMode(), routeDistances);
									
									List<Double> euclideanDistances = new ArrayList<>();
									double euclideanDistance = CoordUtils.calcEuclideanDistance(previousRealActivity.getCoord(), activity.getCoord());
									euclideanDistances.add(euclideanDistance);
									mode2TripEuclideanDistancesFiltered.put(previousMainModeLeg.getMode(), euclideanDistances);
								}
								
								previousMainModeLeg = null;
								previousLegTotalDistance = 0.;
							}
							
							previousRealActivity = activity;
						}
					}
				}
			}			
			counter++;
		}
	}
	
	public void writeModeShares(String outputDirectory) {		
		String outputFileName;
		if (filter == null) {
			outputFileName = "tripModeAnalysis.csv";
		} else {
			outputFileName = "tripModeAnalysis_" + filter.toFileName() + ".csv";
		}
		
		File file = new File(outputDirectory + outputFileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("mode ; number of trips ; trip share ");
			bw.newLine();
			
			for (String mode : this.mode2TripCounterFiltered.keySet()) {
				bw.write(mode + ";" + this.mode2TripCounterFiltered.get(mode) + ";" + (this.mode2TripCounterFiltered.get(mode) / this.totalTripsFiltered));
				bw.newLine();
			}
			
			bw.newLine();
			
			bw.close();
			log.info("Output written.");
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeTripRouteDistances(String outputDirectory) {
		
		for (String mode : this.mode2TripRouteDistancesFiltered.keySet()) {
			String outputFileName;
			if (filter == null) {
				outputFileName = mode + "_tripRouteDistances.csv";
			} else {
				outputFileName = mode + "_tripRouteDistances_" + filter.toFileName() + ".csv";
			}
			File file = new File(outputDirectory + outputFileName);

			try {
				BufferedWriter bw = new BufferedWriter(new FileWriter(file));
				bw.write("trip distance [m]");
				bw.newLine();
				
				for (Double distance : this.mode2TripRouteDistancesFiltered.get(mode)) {
					bw.write(String.valueOf(distance));
					bw.newLine();
				}
								
				bw.close();
				log.info("Output written.");
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void writeTripEuclideanDistances(String outputDirectory) {
		
		for (String mode : this.mode2TripEuclideanDistancesFiltered.keySet()) {
			String outputFileName;
			if (filter == null) {
				outputFileName = mode + "_tripEuclideanDistances.csv";
			} else {
				outputFileName = mode + "_tripEuclideanDistances_" + filter.toFileName() + ".csv";
			}
			File file = new File(outputDirectory + outputFileName);

			try {
				BufferedWriter bw = new BufferedWriter(new FileWriter(file));
				bw.write("trip distance [m]");
				bw.newLine();
				
				for (Double distance : this.mode2TripEuclideanDistancesFiltered.get(mode)) {
					bw.write(String.valueOf(distance));
					bw.newLine();
				}
								
				bw.close();
				log.info("Output written.");
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void writeDistances(String outputDirectory, String outputFileName, List<Tuple<Double, Double>> distanceGroups, Map<String, List<Double>> mode2TripRouteDistancesFiltered) {
		
		SortedMap<Integer, Map<String, Integer>> distanceGroupIndex2mode2trips = new TreeMap<>();	
		
		// initialize
		int index = 0;
		for (Tuple<Double, Double> distanceGroup : distanceGroups) {
			Map<String, Integer> mode2trips = new HashMap<>();

			for (String mode : mode2TripRouteDistancesFiltered.keySet()) {
				log.info("index: " + index + " - distance group: " + distanceGroup + " - mode: " + mode);

				mode2trips.put(mode, 0);
				distanceGroupIndex2mode2trips.put(index, mode2trips);
			}
			index++;
		}

		// fill
		for (String mode : mode2TripRouteDistancesFiltered.keySet()) {
			for (Double distance : mode2TripRouteDistancesFiltered.get(mode)) {
				
				for (Integer distanceGroupIndex : distanceGroupIndex2mode2trips.keySet()) {
			
					if (distance >= distanceGroups.get(distanceGroupIndex).getFirst() && distance < distanceGroups.get(distanceGroupIndex).getSecond()) {
						int tripsUpdated = distanceGroupIndex2mode2trips.get(distanceGroupIndex).get(mode) + 1;
						distanceGroupIndex2mode2trips.get(distanceGroupIndex).put(mode, tripsUpdated);
					}
				}
			}
		}

		// write
		
		File file = new File(outputDirectory + outputFileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("trip distance group index ; distance - from [m] ; distance to [m] ");
			
			for (String mode : distanceGroupIndex2mode2trips.get(0).keySet()) {
				bw.write(" ; " + mode);
			}
			bw.newLine();
			
			for (Integer distanceGroupIndex : distanceGroupIndex2mode2trips.keySet()) {
				bw.write( distanceGroupIndex + " ; " + distanceGroups.get(distanceGroupIndex).getFirst() + " ; " + distanceGroups.get(distanceGroupIndex).getSecond() );

				for (String mode : distanceGroupIndex2mode2trips.get(distanceGroupIndex).keySet()) {
					bw.write(" ; " + distanceGroupIndex2mode2trips.get(distanceGroupIndex).get(mode));
				}
				bw.newLine();
			}

			bw.close();
			log.info("Output written.");	
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeTripEuclideanDistances(String outputDirectory, List<Tuple<Double, Double>> distanceGroups) {
		
		String outputFileName;
		if (filter == null) {
			outputFileName = "tripsPerModeAndEuclideanDistanceGroup.csv";
		} else {
			outputFileName = "tripsPerModeAndEuclideanDistanceGroup_" + filter.toFileName() + ".csv";
		}
		writeDistances(outputDirectory, outputFileName, distanceGroups, this.mode2TripEuclideanDistancesFiltered);
	}
	
	public void writeTripRouteDistances(String outputDirectory, List<Tuple<Double, Double>> distanceGroups) {
		
		String outputFileName;
		if (filter == null) {
			outputFileName = "tripsPerModeAndRouteDistanceGroup.csv";
		} else {
			outputFileName = "tripsPerModeAndRouteDistanceGroup_" + filter.toFileName() + ".csv";
		}
		writeDistances(outputDirectory, outputFileName, distanceGroups, this.mode2TripRouteDistancesFiltered);

	}

	private static Scenario loadScenario(String runDirectory, String runId, String personAttributesFile) {
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
