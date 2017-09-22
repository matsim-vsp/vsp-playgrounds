/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.dziemke.other;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;

/**
 * @author dziemke
 */
class CreateBicycleEquilPopulation {

	public static void main(String[] args) {
		final int numberOfAgents = 1200;
		final String plansFile = "../../shared-svn/studies/countries/de/berlin-bike/input/network/equil/population_" + numberOfAgents + ".xml";
		
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		Population population = scenario.getPopulation();
		
		Coord homeCoord = CoordUtils.createCoord(-20000, -10000, 0);
		Coord workCoord = CoordUtils.createCoord(5000, -10000, 0);
		
		double homeActivityEnd = 9 * 60 * 60;
		double workActivityEnd = 17 * 60 * 60;
		
		for (int i = 1; i <= numberOfAgents; i++) {
			Person person = population.getFactory().createPerson(Id.create(i, Person.class));
			Plan plan = population.getFactory().createPlan();
			{
				Activity activity = population.getFactory().createActivityFromCoord("home", homeCoord);
				activity.setEndTime(homeActivityEnd + (i * 3));
				plan.addActivity(activity);
			}{
				Leg leg = population.getFactory().createLeg("bicycle");
				plan.addLeg(leg);
			}{
				Activity activity = population.getFactory().createActivityFromCoord("work", workCoord);
				activity.setEndTime(workActivityEnd + (i * 3));
				plan.addActivity(activity);
			}{
				Leg leg = population.getFactory().createLeg("bicycle");
				plan.addLeg(leg);
			}
			{
				Activity activity = population.getFactory().createActivityFromCoord("home", homeCoord);
				plan.addActivity(activity);
			}
			person.addPlan(plan);
			population.addPerson(person);
		}
		MatsimWriter popWriter = new PopulationWriter(population);
		popWriter.write(plansFile);
	}
}
