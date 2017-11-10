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

package playground.cfuehrer;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * ikaddoura, cfuehrer
 * 
 */
public class WeirdPlanAnalysis {
	private final static Logger log = Logger.getLogger(WeirdPlanAnalysis.class);
	
	// see if the link works...
	private final static String populationFile = "./NEMO/data/input/matsim_initial_plans_additionalStayHomePlans/plans_1pct_fullChoiceSet_coordsAssigned.xml.gz";

    public static void main(String[] args) throws IOException {
        WeirdPlanAnalysis weirdPlanAnalysis = new WeirdPlanAnalysis();
        weirdPlanAnalysis.analyze();
    }

    public void analyze() throws IOException {
    
		Config config = ConfigUtils.createConfig();	
		config.plans().setInputFile(populationFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);

		int weirdPlans = 0;
		int totalPlans = 0;
		
		for (Person person : scenario.getPopulation().getPersons().values()) {
			for (PlanElement pE : person.getSelectedPlan().getPlanElements()) {
				if (pE instanceof Activity) {
					Activity act = (Activity) pE;
					// TODO
					
				} else if (pE instanceof Leg) {
					Leg leg = (Leg) pE;
					// TODO
					
				} else {
					throw new RuntimeException("Unknown plan Element: " + pE.toString() + ". Aborting...");
				}
			}				
		}
		
		log.info("Number of weird plans: " + weirdPlans);
		log.info("Number of total plans: " + totalPlans);

    }
}
