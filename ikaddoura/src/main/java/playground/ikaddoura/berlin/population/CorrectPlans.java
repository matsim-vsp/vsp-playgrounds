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
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
* @author ikaddoura
*/

public class CorrectPlans {
	
	private static final Logger log = Logger.getLogger(CorrectPlans.class);
	
	private final static String inputPlans = "/Users/ihab/Documents/workspace/shared-svn/studies/countries/de/open_berlin_scenario/be_3/population/population_300_person_freight_10pct.xml.gz";
	private final static String outputPlans = "/Users/ihab/Documents/workspace/shared-svn/studies/countries/de/open_berlin_scenario/be_3/population/population_300_person_freight_10pct_correctedFreightAgents.xml.gz";
//	private static final String[] attributes = {"OpeningClosingTimes"};
	private static final String[] attributes = {};
	
	public static void main(String[] args) {
		
		CorrectPlans filter = new CorrectPlans();
		filter.run(inputPlans, outputPlans, attributes);
	}
	
	public void run (final String inputPlans, final String outputPlans, final String[] attributes) {
		
		log.info("Accounting for the following attributes:");
		for (String attribute : attributes) {
			log.info(attribute);
		}
		log.info("Other person attributes will not appear in the output plans file.");
		
		Scenario scOutput;
		
		Config config = ConfigUtils.createConfig();
		config.plans().setInputFile(inputPlans);
		Scenario scInput = ScenarioUtils.loadScenario(config);
		
		scOutput = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population popOutput = scOutput.getPopulation();
		
		for (Person p : scInput.getPopulation().getPersons().values()){
			PopulationFactory factory = popOutput.getFactory();
			Person personNew = factory.createPerson(p.getId());
			
			for (String attribute : attributes) {
				personNew.getAttributes().putAttribute(attribute, p.getAttributes().getAttribute(attribute));
			}
									
			popOutput.addPerson(personNew);
			
			for (Plan plan : p.getPlans()) {
				boolean previousElementIsActivity = false;
				Activity previousActivity = null;
				boolean deleteFirstActivity = false;
				
				for (PlanElement pE : plan.getPlanElements()) {
					if (pE instanceof Activity) {
						
						Activity act = (Activity) pE;
						
						if (previousElementIsActivity) {
							
							if (act.toString().equals(previousActivity.toString())) {
								deleteFirstActivity = true;
							} else {
								throw new RuntimeException("Should be the same...");
							}
						}
						
						previousElementIsActivity = true;
						previousActivity = act;
						
					} else if (pE instanceof Leg) {
						previousElementIsActivity = false;
					} else {
						throw new RuntimeException("Unknown plan element. Aborting...");
					}
				}
							
				if (deleteFirstActivity) {
					
					log.info("Before: ");
					log.info(plan.toString());

					plan.getPlanElements().remove(0);
					
					log.info("After: ");
					log.info(plan.toString());

				}
				
				personNew.addPlan(plan);
			}
						
		}
		
		log.info("Writing population...");
		new PopulationWriter(scOutput.getPopulation()).write(outputPlans);
		log.info("Writing population... Done.");
	}

}

