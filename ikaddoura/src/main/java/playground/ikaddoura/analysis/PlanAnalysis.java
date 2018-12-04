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

package playground.ikaddoura.analysis;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
* @author ikaddoura
*/

public class PlanAnalysis {

	public static void main(String[] args) {
		String plansFile = "";
		
		Config config = ConfigUtils.loadConfig("output-config-file");
		config.plans().setInputFile(plansFile);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		double scoreSum = 0.;
		double tt = 0.;
		
		for (Person person : scenario.getPopulation().getPersons().values()) {
			scoreSum += person.getSelectedPlan().getScore();
			Plan plan = person.getSelectedPlan();
			
			for (PlanElement pE : plan.getPlanElements()) {
				if (pE instanceof Leg) {
					Leg leg = (Leg) pE;
					if (leg.getMode().equals("car")) {
						
						String[] linkIdsFromRoute = leg.getRoute().getRouteDescription().split(" ");
						for (String linkId : linkIdsFromRoute) {
							if (linkId.equals("baustellen-link")) {
								tt += leg.getTravelTime();
							}
						}
					}
 				}
			}
		}
		
		System.out.println("scoreSum: " + scoreSum);
		System.out.println("travel time of agents traveling on certain links: " + tt);
	}
}

