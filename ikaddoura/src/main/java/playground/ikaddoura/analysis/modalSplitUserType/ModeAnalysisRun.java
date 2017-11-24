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
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * 
 * @author ikaddoura
 *
 */
public class ModeAnalysisRun {
	
	private final String runDirectory = "/Users/ihab/Desktop/ils4/kaddoura/optAV/output_user-specific-mode-choice/output_v0_SAVuserOpCostPricingF_SAVuserExtCostPricingF_SAVdriverExtCostPricingF_CCuserExtCostPricingT/";
	private final String runId = "run1";
	private String subpopulation = "potentialSAVuser";
	
//	private final String runDirectory = "/Users/ihab/Documents/workspace/runs-svn/cne/berlin-dz-1pct-simpleNetwork/output-FINAL/m_r_output_run0_bln_bc/";
//	private final String runId = null;
//	private String subpopulation = null;
	
	private String outputFileName;
	private static final Logger log = Logger.getLogger(ModeAnalysisRun.class);
	
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
		Scenario scenario = loadScenario();
		log.info("Loading scenario... Done.");
		
		Map<String, Integer> mode2TripCounterSubpopulation = new HashMap<>();		
		Map<String, Integer> mode2TripCounterOthers = new HashMap<>();
		Map<String, Integer> mode2TripCounterAll = new HashMap<>();

		double allModesSubpopulation = 0.;
		double allModesOthers = 0.;
		double allModesAll = 0.;
		
		for (Person person : scenario.getPopulation().getPersons().values()) {
			
			for (PlanElement pE : person.getSelectedPlan().getPlanElements()) {
				if (pE instanceof Leg) {
					Leg leg = (Leg) pE;
					
					allModesAll++;
					
					if (mode2TripCounterAll.containsKey(leg.getMode())) {
						mode2TripCounterAll.put(leg.getMode(), mode2TripCounterAll.get(leg.getMode()) + 1);
					} else {
						mode2TripCounterAll.put(leg.getMode(), 1);
					}
				}
			}
			
			if (subpopulation != null && scenario.getPopulation().getPersonAttributes().getAttribute(person.getId().toString(), "subpopulation").toString().equals(subpopulation)) {
				
				for (PlanElement pE : person.getSelectedPlan().getPlanElements()) {
					if (pE instanceof Leg) {
						Leg leg = (Leg) pE;
						
						allModesSubpopulation++;
						
						if (mode2TripCounterSubpopulation.containsKey(leg.getMode())) {
							mode2TripCounterSubpopulation.put(leg.getMode(), mode2TripCounterSubpopulation.get(leg.getMode()) + 1);
						} else {
							mode2TripCounterSubpopulation.put(leg.getMode(), 1);
						}
					}
				}
				
			} else {
				for (PlanElement pE : person.getSelectedPlan().getPlanElements()) {
					if (pE instanceof Leg) {
						Leg leg = (Leg) pE;
						
						allModesOthers++;
						
						if (mode2TripCounterOthers.containsKey(leg.getMode())) {
							mode2TripCounterOthers.put(leg.getMode(), mode2TripCounterOthers.get(leg.getMode()) + 1);
						} else {
							mode2TripCounterOthers.put(leg.getMode(), 1);
						}
					}
				}
			}
		}
		
		
		File file = new File(runDirectory + outputFileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("mode;number of trips (subpopulation: " + subpopulation + " ); number of trips (other agents);number of trips (all agents)");
			bw.newLine();
			
			for (String mode : mode2TripCounterAll.keySet()) {
				
				double modeSubpop = 0.;				
				if (mode2TripCounterSubpopulation.get(mode) != null) {
					modeSubpop = mode2TripCounterSubpopulation.get(mode);
				}
				
				double modeOthers = 0.;
				if (mode2TripCounterOthers.get(mode) != null) {
					modeOthers = mode2TripCounterOthers.get(mode);
				}
				
				bw.write(mode + ";" + modeSubpop + ";" + modeOthers + ";" + mode2TripCounterAll.get(mode));
				bw.newLine();
			}
			
			bw.write("all modes;" + allModesSubpopulation + ";" + allModesOthers + ";" + allModesAll);
			bw.newLine();
			
			bw.close();
			log.info("Output written.");
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private Scenario loadScenario() {
		Scenario scenario;
		if (runId == null) {
			Config config = ConfigUtils.loadConfig(runDirectory + "output_config.xml");
			config.network().setInputFile(runDirectory + "output_network.xml.gz");
			config.plans().setInputFile(runDirectory + "output_plans.xml.gz");
			config.plans().setInputPersonAttributeFile(runDirectory + "output_personAttributes.xml.gz");
			config.vehicles().setVehiclesFile(null);
			scenario = ScenarioUtils.loadScenario(config);
			return scenario;
			
		} else {
			Config config = ConfigUtils.loadConfig(runDirectory + runId + ".output_config.xml");
			config.network().setInputFile(runDirectory + runId + ".output_network.xml.gz");
			config.plans().setInputFile(runDirectory + runId + ".output_plans.xml.gz");
			config.plans().setInputPersonAttributeFile(runDirectory + runId + ".output_personAttributes.xml.gz");
			config.vehicles().setVehiclesFile(null);
			scenario = ScenarioUtils.loadScenario(config);
			return scenario;
		}
	}
		
}
