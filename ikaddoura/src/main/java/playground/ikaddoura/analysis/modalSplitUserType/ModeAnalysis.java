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
import org.matsim.api.core.v01.TransportMode;
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
		
//		final String runDirectory = "/Users/ihab/Desktop/test-run-directory_transit-walk/";
//		final String outputDirectory = "/Users/ihab/Desktop/modal-split-analysis-transit-walk/";
//		final String runId = "test";
		
		final String runId = "b5_1";
		final String runDirectory = "/Users/ihab/Documents/workspace/matsim-berlin/scenarios/berlin-v5.0-1pct-2018-06-18/output_from-reduced-config_FlowStorageCapacityFactor0.02_2018-07-04/";
		
		// if iteration < 0 --> analysis of the final iteration
		int iteration = -1;
		
		final String outputDirectory;
		if (iteration >= 0) {
			outputDirectory = runDirectory + "/modal-split-analysis_" + "it." + iteration + "/";
		} else {
			outputDirectory = runDirectory + "/modal-split-analysis/";
		}
		
		// optional: Provide a personAttributes file which is used instead of the normal output person attributes file; null --> using the output person attributes file
//		final String personAttributesFile = "/Users/ihab/Desktop/ils4a/ziemke/open_berlin_scenario/input/be_5_ik/population/personAttributes_500_10pct.xml.gz";
		final String personAttributesFile = null;

		Scenario scenario = loadScenario(runDirectory, runId, personAttributesFile, iteration);
		
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
		
		final List<Tuple<Double, Double>> distanceGroups = new ArrayList<>();
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
			
			if (counter%100000 == 0) {
				log.info("Person #" + counter);
			}
			
			if (filter == null || filter.considerAgent(person)) {
				Activity previousRealActivity = null;
				
				double currentLegTotalRouteDistance = 0.;
				String currentLegMode = null;

				for (PlanElement pE : person.getSelectedPlan().getPlanElements()) {
									
					if (pE instanceof Leg) {
						Leg leg = (Leg) pE;
						
						currentLegTotalRouteDistance += leg.getRoute().getDistance();
						
						if (currentLegMode == null) {
							// first leg after a 'real' activity
							currentLegMode = leg.getMode();
						} else {
							// at least second leg after a 'real' activity
							
							if (currentLegMode.equals(leg.getMode())) {
								// same mode, nothing to do
								
							} else {
								
								if (currentLegMode.equals(TransportMode.access_walk)
										|| currentLegMode.equals(TransportMode.egress_walk)
										|| currentLegMode.equals(TransportMode.transit_walk)) {
									// update the current leg mode by the 'real' trip mode
									currentLegMode = leg.getMode();
									
								} else if (leg.getMode().equals(TransportMode.access_walk)
										|| leg.getMode().equals(TransportMode.egress_walk)
										|| leg.getMode().equals(TransportMode.transit_walk)) {
									// current leg mode already set to the 'real' trip mode
									
								} else {
									log.warn("Two different leg modes found for the same trip (between two 'real' activities): " + leg.getMode() + " and " + currentLegMode + ".");
								}
							}
						}
											
					} else if (pE instanceof Activity) {
						Activity activity = (Activity) pE;
						
						if (activity.getType().contains("interaction")) {
							// the actual trip is not completed
						} else {
																				
							if (currentLegMode != null) {
								
								totalTripsFiltered++;

								if (mode2TripCounterFiltered.containsKey(currentLegMode)) {
									
									mode2TripCounterFiltered.put(currentLegMode, mode2TripCounterFiltered.get(currentLegMode) + 1);
									
									mode2TripRouteDistancesFiltered.get(currentLegMode).add(currentLegTotalRouteDistance);
									
									double euclideanDistance = CoordUtils.calcEuclideanDistance(previousRealActivity.getCoord(), activity.getCoord());
									mode2TripEuclideanDistancesFiltered.get(currentLegMode).add(euclideanDistance);
									
								} else {
									
									mode2TripCounterFiltered.put(currentLegMode, 1);
									
									List<Double> routeDistances = new ArrayList<>();
									routeDistances.add(currentLegTotalRouteDistance);
									mode2TripRouteDistancesFiltered.put(currentLegMode, routeDistances);
									
									List<Double> euclideanDistances = new ArrayList<>();
									double euclideanDistance = CoordUtils.calcEuclideanDistance(previousRealActivity.getCoord(), activity.getCoord());
									euclideanDistances.add(euclideanDistance);
									mode2TripEuclideanDistancesFiltered.put(currentLegMode, euclideanDistances);
								}
								
								currentLegMode = null;
								currentLegTotalRouteDistance = 0.;
							}
							
							previousRealActivity = activity;
						}
					}
				}
			}			
			counter++;
		}
	}
	
	public Map<String, Integer> getMode2TripCounterFiltered() {
		return mode2TripCounterFiltered;
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
	
	private void writeDistances(String outputDirectory, String outputFileName, List<Tuple<Double, Double>> distanceGroups, Map<String, List<Double>> mode2TripDistances) {
		
		if (mode2TripDistances == null || mode2TripDistances.isEmpty()) {
			log.info("mode2TripDistances is empty. " + outputDirectory + outputFileName + " will not be written.");
			
		} else {
			SortedMap<Integer, Map<String, Integer>> distanceGroupIndex2mode2trips = new TreeMap<>();	
			
			// initialize
			int index = 0;
			for (Tuple<Double, Double> distanceGroup : distanceGroups) {
				Map<String, Integer> mode2trips = new HashMap<>();

				for (String mode : mode2TripDistances.keySet()) {
					log.info("index: " + index + " - distance group: " + distanceGroup + " - mode: " + mode);

					mode2trips.put(mode, 0);
					distanceGroupIndex2mode2trips.put(index, mode2trips);
				}
				index++;
			}

			// fill
			for (String mode : mode2TripDistances.keySet()) {
				for (Double distance : mode2TripDistances.get(mode)) {
					
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

	private static Scenario loadScenario(String runDirectory, String runId, String personAttributesFile, int iteration) {
		Scenario scenario;
		if (iteration < 0) {
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
		} else {
			Config config = ConfigUtils.createConfig();

			if (runId == null) {
				config.plans().setInputFile(runDirectory + "ITERS/it." + iteration + "/" + iteration + "." + "plans.xml.gz");
				if (personAttributesFile == null) {
					throw new RuntimeException("Person attributes file required. Aborting...");
				} else {
					config.plans().setInputPersonAttributeFile(personAttributesFile);
				}
				scenario = ScenarioUtils.loadScenario(config);
				return scenario;
				
			} else {
				config.plans().setInputFile(runDirectory + "ITERS/it." + iteration + "/" + runId + "." + iteration + "." + "plans.xml.gz");
				if (personAttributesFile == null) {
					throw new RuntimeException("Person attributes file required. Aborting...");
				} else {
					config.plans().setInputPersonAttributeFile(personAttributesFile);
				}
				scenario = ScenarioUtils.loadScenario(config);
				return scenario;
			}
		}
		
	}
		
}
