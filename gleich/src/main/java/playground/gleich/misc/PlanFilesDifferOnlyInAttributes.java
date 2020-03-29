/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package playground.gleich.misc;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

import com.google.common.base.Verify;

public class PlanFilesDifferOnlyInAttributes {
	
	private static final Logger log = Logger.getLogger(PlanFilesDifferOnlyInAttributes.class);

	public static void main(String[] args) {		
		String planFile1 = "/home/gregor/git/matsim/matsim/test/output/org/matsim/integration/replanning/ReRoutingIT/testReRoutingDijkstra/reference_population.xml.gz";
		String planFile2 = "/home/gregor/git/matsim/matsim/test/output/org/matsim/integration/replanning/ReRoutingIT/testReRoutingDijkstra/output_population.xml.gz";
		Scenario scenario1 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Scenario scenario2 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		(new PopulationReader(scenario1)).readFile(planFile1);
		(new PopulationReader(scenario2)).readFile(planFile2);
		Population population1 = scenario1.getPopulation();
		Population population2 = scenario2.getPopulation();
		populationsEqualBesidesAttributes(population1, population2);
	}
	
	private static void populationsEqualBesidesAttributes(Population population1, Population population2) {
		Verify.verify(population1.getPersons().size() == population2.getPersons().size(), "different population sizes");
		
		// clear attributes
		population1.getPersons().values().parallelStream().forEach(p -> p.getAttributes().clear() );
		population2.getPersons().values().parallelStream().forEach(p -> p.getAttributes().clear() );
		
		population1.getPersons().values().stream().forEach(p -> {
			Person p2 = population2.getPersons().get(p.getId());
			log.info("Person id " + p.getId());
			if (!p.equals(p2)) {
				
				Verify.verify(p.getPlans().size() == p2.getPlans().size(), "different number of plans");
				
				if (p.getPlans().size() > 0) {
					for (int i = 0; i < p.getPlans().size() - 1; i++) {
						Plan plan1 = p.getPlans().get(i);
						Plan plan2 = p2.getPlans().get(i);
						if (!planEquals(plan1, plan2)) {
							log.warn("agent with id " + p.getId() + " differs");
							log.warn("Plan " + i);
							log.warn("Plan in Population 1 :" + p.getPlans().get(i).toString() + " : " + p.getPlans().get(i).getPlanElements().toString());
							log.warn("Plan in Population 2 :" + p2.getPlans().get(i).toString() + " : " + p2.getPlans().get(i).getPlanElements().toString());
						}
					}
				}

			}
		});
	}
	
	private static boolean planEquals (Plan plan1, Plan plan2) {
		if (plan1.getScore().equals(plan2.getScore())) {
			for (int i = 0; i < plan1.getPlanElements().size() - 1; i++) {
				PlanElement pe1 = plan1.getPlanElements().get(i);
				PlanElement pe2 = plan2.getPlanElements().get(i);
				if (pe1 instanceof Activity) {
					if (!activityEquals((Activity) pe1, (Activity) pe2)) {
						return false;
					}
				} else if (pe1 instanceof Leg) {
					if (!legEquals((Leg) pe1, (Leg) pe2)) {
						return false;
					}
				}
			}
			return true;
		} else {
			return false;
		}
	}
	
	private static boolean activityEquals (Activity act1, Activity act2) {
		return /* act1.getAttributes().equals(act2.getAttributes()) && */
				act1.getCoord().equals(act2.getCoord()) &&
				act1.getEndTime().equals(act2.getEndTime()) &&
				(act1.getFacilityId() != null ? act1.getFacilityId().equals(act2.getFacilityId()) : act2.getFacilityId() == null) &&
				act1.getLinkId().equals(act2.getLinkId()) &&
				act1.getMaximumDuration().equals(act2.getMaximumDuration()) &&
				act1.getStartTime().equals(act2.getStartTime());
	}
	
	private static boolean legEquals (Leg leg1, Leg leg2) {
		return /* leg1.getAttributes().equals(leg2.getAttributes()) && */
				Double.compare(leg1.getDepartureTime().seconds(), leg2.getDepartureTime().seconds()) == 0 &&
				leg1.getMode().equals(leg2.getMode()) &&
				routeEquals(leg1.getRoute(), leg2.getRoute()) &&
				Double.compare(leg1.getDepartureTime().seconds(), leg2.getDepartureTime().seconds()) == 0;
	}
	
	/*
	 * TODO: Look into sub-classes (ExperimentalTransitRoute, GenericRouteImpl, NetworkRoute ... )
	 */
	private static boolean routeEquals (Route route1, Route route2) {
		return Double.compare(route1.getDistance(), route2.getDistance()) == 0 &&
				route1.getEndLinkId().equals(route2.getEndLinkId()) &&
				route1.getRouteDescription().equals(route2.getRouteDescription()) &&
				route1.getRouteType().equals(route2.getRouteType()) &&
				route1.getStartLinkId().equals(route2.getStartLinkId()) &&
				Double.compare(route1.getTravelTime().seconds(), route2.getTravelTime().seconds()) == 0;
	}

}
