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
package scenarios.illustrative.smith;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.routes.RouteUtils;

/**
 * Class to create a population for Smith' scenario.
 * 
 * You can modify 
 * 	- the number of persons you like to simulate, 
 * 	- whether they should get an initial route and 
 *  - the score of the initial route.
 * 
 * @author tthunig
 */
final class CreateSmithPopulation {
	
	private static final Logger log = Logger
			.getLogger(CreateSmithPopulation.class);

	private Population population;
	
	private int numberOfPersons;
	private Double initPlanScore = null;
	private InitRoutesType initType = InitRoutesType.ALL_RIGHT;
	public enum InitRoutesType {
		NONE,
		ALL_RIGHT,
		OSZILLATING
	}

	public CreateSmithPopulation(Population pop, Network net) {
		this.population = pop;
	}

	/**
	 * Fills a population container with the given number of persons. All persons travel from left to right.
	 * All agents are uniformly distributed between 8am and 9am.
	 */
	public void createPersons(int numberOfPersons, InitRoutesType initRouteType, Double initPlanScore) {
		this.numberOfPersons = numberOfPersons;
		this.initType = initRouteType;
		this.initPlanScore = initPlanScore;
		
		log.info("Create population ...");
		
		createWestEastDemand();
	}
	
	public void writePopFile(String pathToPopFile) {
		PopulationWriter popWriter = new PopulationWriter(population);
		popWriter.write(pathToPopFile);
	}

	private void createWestEastDemand() {
		for (int i = 0; i < this.numberOfPersons; i++) {

			// create a person
			Person person = population.getFactory().createPerson(Id.createPersonId(Integer.toString(i)));

			// create a start activity
			Activity startAct = population.getFactory()
					.createActivityFromLinkId("dummy", Id.createLinkId("0_1"));
			// distribute agents uniformly between 8 and 9 am.
			startAct.setEndTime(8 * 3600 + (double)(i)/numberOfPersons * 3600);

			// create a drain activity
			Activity drainAct = population.getFactory().createActivityFromLinkId(
					"dummy", Id.createLinkId("9_10"));

			if (!initType.equals(InitRoutesType.NONE)) {
				Leg leg = createWestEastLeg(true);
				Plan planNorth = createPlan(startAct, leg, drainAct, initPlanScore);
				leg = createWestEastLeg(false);
				Plan planSouth = createPlan(startAct, leg, drainAct, initPlanScore);
				person.addPlan(planNorth);
				person.addPlan(planSouth);
				switch (initType) {
				case ALL_RIGHT:
					person.setSelectedPlan(planSouth);
					break;
				case OSZILLATING:
					if (i % 2 == 0) {
						person.setSelectedPlan(planNorth);
					} else {
						person.setSelectedPlan(planSouth);
					}
					break;
				default:
					break;
				}
			} else {
				Leg leg = population.getFactory().createLeg(TransportMode.car);
				Plan plan = createPlan(startAct, leg, drainAct, initPlanScore);
				person.addPlan(plan);
			}

			// store information in population
			population.addPerson(person);
		}
	}

	private Leg createWestEastLeg(boolean takeNorthernPath) {
		Leg leg = population.getFactory().createLeg(TransportMode.car);

		List<Id<Link>> path = new ArrayList<>();
		path.add(Id.createLinkId("1_2"));
		if (takeNorthernPath) {
			path.add(Id.createLinkId("2_3"));
			path.add(Id.createLinkId("3_5"));
			path.add(Id.createLinkId("5_7"));
			path.add(Id.createLinkId("7_8"));
		} else {
			path.add(Id.createLinkId("2_4"));
			path.add(Id.createLinkId("4_5"));
			path.add(Id.createLinkId("5_6"));
			path.add(Id.createLinkId("6_8"));
		}
		path.add(Id.createLinkId("8_9"));
		path.add(Id.createLinkId("9_10"));

		Route route = RouteUtils.createLinkNetworkRouteImpl(Id.createLinkId("0_1"), path, Id.createLinkId("9_10"));

		leg.setRoute(route);
		return leg;
	}

	private Plan createPlan(Activity startAct, Leg leg, Activity drainAct,
			Double initPlanScore) {
		
		Plan plan = population.getFactory().createPlan();

		plan.addActivity(startAct);
		plan.addLeg(leg);
		plan.addActivity(drainAct);
		plan.setScore(initPlanScore);
		
		return plan;
	}
}
