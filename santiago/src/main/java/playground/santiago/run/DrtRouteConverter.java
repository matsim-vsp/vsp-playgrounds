/*
 * *********************************************************************** *
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
 * *********************************************************************** *
 */

package playground.santiago.run;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DrtRouteConverter {
	public static void main(String[] args) {
		Set<String> modesToDvrpMode = new HashSet<>(Arrays.asList("colectivo"));
		String dvrpMode = TransportMode.drt;

		String inputPath = "D:\\matsim-eclipse\\shared-svn\\projects\\santiago\\scenario\\inputForMATSim\\AV_simulation\\";
		String plansFile = inputPath + "0.plans_small.xml.gz";
		String networkFile = inputPath + "network_merged_cl.xml.gz";

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);
		new PopulationReader(scenario).readFile(plansFile);

		for (Person person : scenario.getPopulation().getPersons().values()) {
			for (PlanElement e : person.getSelectedPlan().getPlanElements()) {
				if (e instanceof Leg) {
					Leg leg = (Leg)e;

					if (modesToDvrpMode.contains(leg.getMode())) {
						leg.setMode(TransportMode.drt);
						leg.setDepartureTime(Time.UNDEFINED_TIME);
						leg.setTravelTime(Time.UNDEFINED_TIME);
						leg.setRoute(null);
					}
				}
			}
		}

		new PopulationWriter(scenario.getPopulation(), scenario.
				getNetwork()).
				write(inputPath + "0.plans_small_DRT.xml.gz");
	}
}
