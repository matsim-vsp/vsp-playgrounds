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

/**
 * 
 */
package playground.jbischoff.csberlin.plans.preprocess;

import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;

import com.vividsolutions.jts.geom.Geometry;

import playground.jbischoff.utils.JbUtils;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class AssignAVModeToSample {
public static void main(String[] args) {
	Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	Population pop2 = PopulationUtils.createPopulation(ConfigUtils.createConfig());
	Random r = MatsimRandom.getRandom();
	double p = 0.2;
	StreamingPopulationReader spr = new StreamingPopulationReader(scenario);
	spr.addAlgorithm(new PersonAlgorithm() {
		
		@Override
		public void run(Person person) {
			pop2.addPerson(person);
			if (r.nextDouble()< p)
			{
			for (Plan plan : person.getPlans()){
				for (PlanElement pe : plan.getPlanElements()){
					if (pe instanceof Leg){
						Leg l = (Leg) pe;
						if (l.getMode().equals(TransportMode.car)){
							l.setMode("av");
						}
					}
					}
				}
			}
			}
		
	});
	spr.readFile("C:/Users/Joschka/Documents/shared-svn/projects/bmw_carsharing/data/avparking/klaus-population.xml.gz");
	new PopulationWriter(pop2).write("C:/Users/Joschka/Documents/shared-svn/projects/bmw_carsharing/data/avparking/klaus-population_av_"+p+".xml.gz");
}

}
