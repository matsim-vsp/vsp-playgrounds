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
	
	private final String runDirectory = "/Users/ihab/Documents/workspace/runs-svn/optAV/output/output_v0_SAVuserOpCostPricingF_SAVuserExtCostPricingF_SAVdriverExtCostPricingF_CCuserExtCostPricingF/";
	private final String runId = "run0";
	
	private String subpopulation = "potentialSAVuser";
	
	private final String outputFileName = runId + ".modeAnalysis_" + subpopulation + ".csv";
	
	private static final Logger log = Logger.getLogger(ModeAnalysisRun.class);
	
	public static void main(String[] args) throws IOException {
		ModeAnalysisRun main = new ModeAnalysisRun();
		main.run();
	}
		
	public void run() throws IOException {
		
		log.info("Loading scenario...");
		Scenario scenario = loadScenario();
		log.info("Loading scenario... Done.");
		
		double allModes = 0;
		double car = 0;
		double pt = 0;
		double walkBicycle = 0;
		double taxi = 0;
		
		for (Person person : scenario.getPopulation().getPersons().values()) {
			
			if (scenario.getPopulation().getPersonAttributes().getAttribute(person.getId().toString(), "subpopulation").toString().equals(subpopulation)) {
				
				for (PlanElement pE : person.getSelectedPlan().getPlanElements()) {
					if (pE instanceof Leg) {
						Leg leg = (Leg) pE;
						
						allModes++;
						
						if (leg.getMode().equals("car")) {
							car++;
						} else if (leg.getMode().equals("taxi")) {
							taxi++;
						} else if (leg.getMode().contains("pt")) {
							pt++;
						} else if (leg.getMode().equals("bicycle") || leg.getMode().equals("walk")) {
							walkBicycle++;
						}
					}
				}
			}
		}
		
		
		File file = new File(runDirectory + outputFileName);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			bw.write("mode;trip share");
			bw.newLine();

			bw.write("car;" + car / allModes);
			bw.newLine();

			bw.write("pt;" + pt / allModes);
			bw.newLine();

			bw.write("SAV;" + taxi / allModes);
			bw.newLine();

			bw.write("walk / bicycle;" + walkBicycle / allModes);
			bw.newLine();
			
			bw.close();
			log.info("Output written.");
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private Scenario loadScenario() {
		Config config = ConfigUtils.loadConfig(runDirectory + runId + ".output_config.xml.gz");
		config.network().setInputFile(runDirectory + runId + ".output_network.xml.gz");
		config.plans().setInputFile(runDirectory + runId + ".output_plans.xml.gz");
		config.plans().setInputPersonAttributeFile(runDirectory + runId + ".output_personAttributes.xml.gz");
		config.vehicles().setVehiclesFile(null);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		return scenario;
	}
		
}
