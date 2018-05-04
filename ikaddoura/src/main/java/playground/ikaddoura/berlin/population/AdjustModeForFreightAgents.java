/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.ikaddoura.berlin.population;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
* @author ikaddoura
*/

public class AdjustModeForFreightAgents {
	
	private static final Logger log = Logger.getLogger(AdjustModeForFreightAgents.class);
	
	private final static String inputPlans = "/Users/ihab/Documents/workspace/shared-svn/studies/countries/de/open_berlin_scenario/be_5/population/be_500_c_10pct.selected_plans.xml.gz";
	private final static String outputPlans = "/Users/ihab/Documents/workspace/shared-svn/studies/countries/de/open_berlin_scenario/be_5/population/be_500_c_10pct.selected_plans_with-freight-as-mode.xml.gz";
//	private static final String[] attributes = {"OpeningClosingTimes"};
	private static final String[] attributes = {};
	
	public static void main(String[] args) {
		
		AdjustModeForFreightAgents filter = new AdjustModeForFreightAgents();
		filter.run(inputPlans, outputPlans, attributes);
	}
	
	public void run (final String inputPlans, final String outputPlans, final String[] attributes) {
		
		log.info("Accounting for the following attributes:");
		for (String attribute : attributes) {
			log.info(attribute);
		}
		log.info("Other person attributes will not appear in the output plans file.");
				
		Config config = ConfigUtils.createConfig();
		config.plans().setInputFile(inputPlans);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		for (Person p : scenario.getPopulation().getPersons().values()){
			if (p.getId().toString().startsWith("freight")) {
				
				for (Plan plan : p.getPlans()) {
					
					for (PlanElement pE : plan.getPlanElements()) {
						if (pE instanceof Leg) {
							Leg leg = (Leg) pE;
							leg.setMode("freight");
						}
					}
				}
			}
						
		}
		
		log.info("Writing population...");
		new PopulationWriter(scenario.getPopulation()).write(outputPlans);
		log.info("Writing population... Done.");
	}

}

