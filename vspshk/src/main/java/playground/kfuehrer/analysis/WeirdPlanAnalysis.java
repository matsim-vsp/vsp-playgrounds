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

package playground.kfuehrer.analysis;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;

/**
 * ikaddoura, kfuehrer
 * 
 */
public class WeirdPlanAnalysis {
	private final static Logger log = Logger.getLogger(WeirdPlanAnalysis.class);
	
	private final static String populationFile = "C:/Users/Karoline/vsp-files/plans_1pct_fullChoiceSet_coordsAssigned.xml.gz";
	
	private int weirdPlans = 0;
	private int weirdPersons = 0;
	private int totalPlans = 0;
	private int totalPersons = 0;

	private final double beelineDistanceFactor = 1.3;
	private final double speedForTravelTimeComputation = 13.8888888888889; //50 km/h

    public static void main(String[] args) throws IOException {
        WeirdPlanAnalysis weirdPlanAnalysis = new WeirdPlanAnalysis();
        weirdPlanAnalysis.analyze(populationFile);
    }

    public void analyze(String populationFile) throws IOException {
    
		Config config = ConfigUtils.createConfig();	
		config.plans().setInputFile(populationFile);
		config.network().setInputFile(null);
		Scenario scenario = ScenarioUtils.loadScenario(config);

		double diff = 0; //difference between estimated arrival and departure time
		double dist;
		
		
		for (Person person : scenario.getPopulation().getPersons().values()) {
			
			totalPersons++; 
			
			log.info("Person ID: " + person.getId());
			
			int weirdPlansOfThisPerson = 0;
			
			
			for (Plan plan : person.getPlans()) {
				
				//System.out.println(plan.toString());
				totalPlans ++;
				
				Activity previousAct = null;
				int weirdAct = 0;
		
				
				for (PlanElement pE : plan.getPlanElements()) {
					
					//System.out.println(pE.toString());
					
					if (pE instanceof Activity) {
						
						Activity act = (Activity) pE;
						
						if (previousAct != null) {
							//Distance calculation with beeline distance factor
							dist = CoordUtils.calcEuclideanDistance(act.getCoord(), previousAct.getCoord()) * beelineDistanceFactor;
							//log.info("distance: "+dist);
							
							
							//estimated arrival time
							double traveltime = dist / speedForTravelTimeComputation;
							double estArrival = previousAct.getEndTime() + traveltime;
							
							//find weird Activities
							if (act.getEndTime() - estArrival < diff) {
								weirdAct++;
						}
						
						// log.info("previous activity: " + previousAct);
						// log.info("current activity: " + act);
						
						}
					
						previousAct = act;
						
					} else if (pE instanceof Leg) {
						Leg leg = (Leg) pE;
						// TODO
						
					} else {
						throw new RuntimeException("Unknown plan Element: " + pE.toString() + ". Aborting...");
					}
				}	
				if (weirdAct > 0) {
					weirdPlans++;
					weirdPlansOfThisPerson++;
				}	
			}
			log.info("Weird Plans of this Person: " + weirdPlansOfThisPerson);
			
			if (weirdPlansOfThisPerson > 0) {
				weirdPersons++;
			}
		}
		
		log.info("Number of weird plans: " + weirdPlans);
		log.info("Number of total plans: " + totalPlans);
		log.info("Number of weird persons: " + weirdPersons);
		log.info("Number of total persons: " + totalPersons);

    }

	public int getWeirdPlans() {
		return weirdPlans;
	}

	public int getTotalPlans() {
		return totalPlans;
	}
    
}
