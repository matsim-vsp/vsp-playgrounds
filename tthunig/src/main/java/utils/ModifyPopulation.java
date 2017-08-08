/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DefaultControlerModules.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package utils;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.scenario.ScenarioUtils;

import contrib.baseline.lib.PopulationUtils;

/**
 * Class to modify a population file.
 * It removes all link information of activities and all routes.
 * 
 * @author tthunig
 */
public class ModifyPopulation {

	private static final String INPUT_BASE_DIR = "../../runs-svn/berlin_scenario_2016/be_218/";
	
	public static void main(String[] args) {
		
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(INPUT_BASE_DIR + "be_218.output_network.xml.gz");
		config.plans().setInputFile(INPUT_BASE_DIR + "be_218.output_plans.xml.gz");
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
//		removeAllLinkInfos(scenario.getPopulation());		
		
		new PopulationWriter(onlyKeepSelectedPlanAndCarUsers(scenario.getPopulation())).write(INPUT_BASE_DIR + "be_218.output_plans_selected_carOnly.xml.gz");
	}
	
	public static Population onlyKeepSelectedPlanAndCarUsers(Population population){
		Population carPop = PopulationUtils.getEmptyPopulation();
		for (Person p : population.getPersons().values()){
			Plan plan = p.getSelectedPlan();
			for (PlanElement pe : plan.getPlanElements()){
				if (pe instanceof Leg){
					if (((Leg) pe).getMode().equals(TransportMode.car)){
						Person carP = carPop.getFactory().createPerson(p.getId());
						carP.addPlan(plan);
						carPop.addPerson(carP);
						break;
					}
				}
			}
		}
		return carPop;
	}
	
	public static void removeAllLinkInfos(Population population){
		for (Person p : population.getPersons().values()) {
			for (Plan plan : p.getPlans()) {
				for (PlanElement pe : plan.getPlanElements()) {
					if (pe instanceof Activity) {
						// remove link
						((Activity) pe).setLinkId(null);
					}
					if (pe instanceof Leg) {
						// remove route
						((Leg) pe).setRoute(null);
					}
				}
			}
		}
	}
	
	public static void removeRoutesLeaveFirstPlan(Population population){
		
		for (Person person : population.getPersons().values()){
			boolean firstPlanHandled = false;
			for (Plan plan : person.getPlans()){
				// remove all plans except the first
				if (firstPlanHandled){
					person.removePlan(plan);
				} else {
					// remove route of the first plan
					for (PlanElement pe : plan.getPlanElements()) {
						if (pe instanceof Leg) {
							((Leg) pe).setRoute(null);
						}
					}
				}
			}
		}		
	}

}
