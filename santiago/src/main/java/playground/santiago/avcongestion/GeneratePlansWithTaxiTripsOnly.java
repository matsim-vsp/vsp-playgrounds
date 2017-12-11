/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.santiago.avcongestion;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author michalm
 */
public class GeneratePlansWithTaxiTripsOnly {
	public static void main(String[] args) {
		String dir = "D:\\matsim-eclipse\\runs-svn\\santiago_AT_10pc\\";
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(dir + "car_only_network.xml.gz");
		new PopulationReader(scenario).readFile(dir + "randomized_expanded_plans.xml.gz");
		Population pop2 = convertPopulation(scenario.getPopulation(), scenario.getNetwork());
		new PopulationWriter(pop2).write(dir + "taxi_only_plans.xml.gz");
	}

	private static Population convertPopulation(Population population, Network network) {
		Population newPopulation = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation();

		for (Person p : population.getPersons().values()) {
			Plan plan = p.getSelectedPlan();
			Activity previousAct = null;
			Leg previousLeg = null;
			boolean write = false;
			int i = 0;

			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Activity) {
					Activity currentAct = (Activity)pe;

					if (previousAct != null && write) {

						Link fromLink = NetworkUtils.getNearestLink(network, previousAct.getCoord());
						Link toLink = NetworkUtils.getNearestLink(network, currentAct.getCoord());
						if (fromLink != toLink) {

							Person p1 = newPopulation.getFactory()
									.createPerson(Id.createPersonId((p.getId().toString() + "_" + i)));
							i++;
							Plan plan1 = newPopulation.getFactory().createPlan();
							previousAct.setType("home");
							previousAct.setLinkId(fromLink.getId());
							plan1.addActivity(previousAct);

							previousLeg.setMode("taxi");
							plan1.addLeg(previousLeg);

							currentAct.setType("work");
							currentAct.setLinkId(toLink.getId());
							plan1.addActivity(currentAct);
							p1.addPlan(plan1);
							newPopulation.addPerson(p1);
							write = false;
						}
					}
					previousAct = currentAct;
				} else if (pe instanceof Leg) {
					previousLeg = (Leg)pe;
					write = previousLeg.getMode().equals("car");
				}
			}
		}
		return newPopulation;
	}
}
