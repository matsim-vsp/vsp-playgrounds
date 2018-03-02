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

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.population.io.StreamingPopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.router.TransitActsRemover;

public class RemoveNonPtPlans {

	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		StreamingPopulationReader spr = new StreamingPopulationReader(scenario);
		StreamingPopulationWriter spw = new StreamingPopulationWriter();
		spw.startStreaming("C:\\Users\\Joschka\\Documents\\shared-svn\\projects\\ptrouting\\niedersachsen_sample_scenario\\orig_data\\pt_plans.xml.gz");
		spr.addAlgorithm(new PersonAlgorithm() {
			
			@Override
			public void run(Person person) {
				PersonUtils.removeUnselectedPlans(person);
				Plan plan = person.getSelectedPlan();
				new TransitActsRemover().run(plan);
				
				for (PlanElement pe : plan.getPlanElements()) {
					if (pe instanceof Leg) {
						if (((Leg) pe).getMode().equals(TransportMode.pt)) {
							spw.writePerson(person);
							break;
						}
					}
				}
			}
		});
		spr.readFile("C:\\Users\\Joschka\\Documents\\shared-svn\\projects\\ptrouting\\niedersachsen_sample_scenario\\orig_data\\vw214.output_plans.xml.gz");
		spw.closeStreaming();

	}
}
