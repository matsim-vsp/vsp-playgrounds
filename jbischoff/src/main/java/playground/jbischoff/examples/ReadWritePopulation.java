/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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
  
package playground.jbischoff.examples;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.population.io.StreamingPopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;


public class ReadWritePopulation {

	public static void main(String[] args) {
		StreamingPopulationReader spr = new StreamingPopulationReader(ScenarioUtils.createScenario(ConfigUtils.createConfig()));
		StreamingPopulationWriter spw = new StreamingPopulationWriter(0.3);
		spw.startStreaming("C:/Users/Joschka/git/matsim/contribs/drt/src/main/resources/drt_example/cb-drtplans.xml.gz");
		spr.addAlgorithm(new PersonAlgorithm() {
			
			@Override
			public void run(Person person) {
				boolean c = true;
				Plan plan = person.getSelectedPlan();
				for (PlanElement pe : plan.getPlanElements()) {
					if (pe instanceof Leg) {
						if (!((Leg) pe).getMode().equals("drt")){
							c = false;
							break;
						}
					}
				}
				if (c) {
					spw.run(person);
				}
				}
		});
		spr.readFile("C:/Users/Joschka/git/matsim/contribs/drt/src/main/resources/drt_example/pop2.xml");
		spw.closeStreaming();
	}
	
}
