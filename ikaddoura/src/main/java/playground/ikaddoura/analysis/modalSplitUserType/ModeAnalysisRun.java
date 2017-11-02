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
	
//	private final String runDirectory = "/Users/ihab/Documents/workspace/runs-svn/optAV/output/output_v0_SAVuserOpCostPricingF_SAVuserExtCostPricingF_SAVdriverExtCostPricingF_CCuserExtCostPricingF/";
//	private final String runId = "run0";
//	private String subpopulation = "potentialSAVuser";
	
	private final String runDirectory = "/Users/ihab/Documents/workspace/runs-svn/cne/berlin-dz-1pct-simpleNetwork/output-FINAL/m_r_output_run0_bln_bc/";
	private final String runId = null;
	private String subpopulation = null;
	
	private String outputFileName;
	private static final Logger log = Logger.getLogger(ModeAnalysisRun.class);
	
	public static void main(String[] args) throws IOException {
		ModeAnalysisRun main = new ModeAnalysisRun();
		main.run();
	}
		
	public void run() throws IOException {
		
		if(runId == null) {
			outputFileName = "tripModeAnalysis_" + subpopulation + "_others.csv";
		} else {
			outputFileName = runId + ".tripModeAnalysis_" + subpopulation + "_others.csv";
		}
		
		log.info("Loading scenario...");
		Scenario scenario = loadScenario();
		log.info("Loading scenario... Done.");
		
		double allModes = 0;
		double car = 0;
		double pt = 0;
		double walkBicycle = 0;
		double taxi = 0;
		
		double allModesOthers = 0;
		double carOthers = 0;
		double ptOthers = 0;
		double walkBicycleOthers = 0;
		double taxiOthers = 0;
		
		for (Person person : scenario.getPopulation().getPersons().values()) {
			
			if (subpopulation != null && scenario.getPopulation().getPersonAttributes().getAttribute(person.getId().toString(), "subpopulation").toString().equals(subpopulation)) {
				
				for (PlanElement pE : person.getSelectedPlan().getPlanElements()) {
					if (pE instanceof Leg) {
						Leg leg = (Leg) pE;
						
						allModes++;
						
						if (leg.getMode().equals("car")) {
							car++;
						} else if (leg.getMode().equals("taxi")) {
							taxi++;
						} else if (leg.getMode().contains("pt") || leg.getMode().contains("Pt") || leg.getMode().contains("PT")) {
							pt++;
						} else if (leg.getMode().equals("bicycle") || leg.getMode().equals("walk") || leg.getMode().equals("bike")) {
							walkBicycle++;
						} else {
							log.warn("Unknown mode: " + leg.getMode());
						}
					}
				}
			} else {
				for (PlanElement pE : person.getSelectedPlan().getPlanElements()) {
					if (pE instanceof Leg) {
						Leg leg = (Leg) pE;
						
						allModesOthers++;
						
						if (leg.getMode().equals("car")) {
							carOthers++;
						} else if (leg.getMode().equals("taxi")) {
							taxiOthers++;
						} else if (leg.getMode().contains("pt") || leg.getMode().contains("Pt") || leg.getMode().contains("PT")) {
							ptOthers++;
						} else if (leg.getMode().equals("bicycle") || leg.getMode().equals("walk") || leg.getMode().equals("bike")) {
							walkBicycleOthers++;
						} else {
							log.warn("Unknown mode: " + leg.getMode());
						}
					}
				}
			}
		}
		
		
		File file = new File(runDirectory + outputFileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("mode;number of trips (subpopulation: " + subpopulation + " ); number of trips (others)");
			bw.newLine();

			bw.write("car;" + car + " ; " + carOthers);
			bw.newLine();

			bw.write("pt;" + pt + " ; " + ptOthers);
			bw.newLine();

			bw.write("taxi/SAV;" + taxi + " ; " + taxiOthers);
			bw.newLine();

			bw.write("walk/bicycle;" + walkBicycle + " ; " + walkBicycleOthers);
			bw.newLine();
			
			bw.write("all modes;" + allModes + " ; " + allModesOthers);
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
			Config config = ConfigUtils.loadConfig(runDirectory + "output_config.xml.gz");
			config.network().setInputFile(runDirectory + "output_network.xml.gz");
			config.plans().setInputFile(runDirectory + "output_plans.xml.gz");
			config.plans().setInputPersonAttributeFile(runDirectory + "output_personAttributes.xml.gz");
			config.vehicles().setVehiclesFile(null);
			scenario = ScenarioUtils.loadScenario(config);
			return scenario;
			
		} else {
			Config config = ConfigUtils.loadConfig(runDirectory + runId + ".output_config.xml.gz");
			config.network().setInputFile(runDirectory + runId + ".output_network.xml.gz");
			config.plans().setInputFile(runDirectory + runId + ".output_plans.xml.gz");
			config.plans().setInputPersonAttributeFile(runDirectory + runId + ".output_personAttributes.xml.gz");
			config.vehicles().setVehiclesFile(null);
			scenario = ScenarioUtils.loadScenario(config);
			return scenario;
		}
	}
		
}
