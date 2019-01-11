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
package scenarios.illustrative.parallel.createInput;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
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
import org.matsim.utils.objectattributes.ObjectAttributes;

/**
 * Class to create a population for the parallel scenario.
 * 
 * You can modify 
 * 	- the number of persons you like to simulate, 
 * 	- whether they should get an initial route and 
 *  - the score of the initial route.
 * 
 * @author gthunig, tthunig
 */
public final class TtCreateParallelPopulation {
	
	private static final Logger log = Logger
			.getLogger(TtCreateParallelPopulation.class);

	private Population population;
	private ObjectAttributes personAtt;

	private DemandType demandType = DemandType.SINGLE_OD;
	public enum DemandType {
		SINGLE_OD,
		CROSSING_ROAD, // S-N traffic without route choice (all east)
		CROSSING_ROADS, // S-N and N-S traffic without route choice (all right)
		SECOND_OD
	}
	
	private int numberOfPersons;
	private Double initPlanScore = null;
	private InitRoutesType initType = InitRoutesType.ALL_RIGHT;
	public enum InitRoutesType {
		NONE,
		ALL_RIGHT,
		ALL_LEFT,
		OSZILLATING,
		ALL_N_W // north-west (oncoming traffic on same route, i.e. just one intersection relevant)
	}

	public TtCreateParallelPopulation(Scenario sc) {
		this.population = sc.getPopulation();
		this.personAtt = sc.getPopulation().getPersonAttributes();
	}

	/**
	 * Fills a population container with the given number of persons per OD
	 * Pair. All persons travel from all cardinal directions to the opposite
	 * cardinal direction.
	 * 
	 * All agents start at 8am.
	 * 
	 * @param numberOfPersons
	 *            the number of persons per OD pair
	 * @param initRoutes
	 *            flag that determines whether agents are initialized with or
	 *            without initial routes. If it is false, all agents are
	 *            initialized with no initial routes. If it is true, they are
	 *            initialized with both routes for their OD Pair, whereby every
	 *            second agent gets the first and every other agent the other
	 *            route as initial selected route.
	 * @param initPlanScore
	 *            initial score for all plans the persons will get. Use null for
	 *            no scores.
	 */
	public void createPersons(int numberOfPersons, InitRoutesType initType, Double initPlanScore) {
		this.numberOfPersons = numberOfPersons;
		this.initType = initType;
		this.initPlanScore = initPlanScore;
		
		log.info("Create population ...");
		
		createWestEastDemand();
		createEastWestDemand();
		
		switch (demandType) {
		case CROSSING_ROAD:
			createCrossingDemandSouthNorth();
			break;
		case CROSSING_ROADS:
			createCrossingDemandSouthNorth();
			createCrossingDemandNorthSouth();
			break;
		case SECOND_OD:
			createNorthSouthDemand();
			createSouthNorthDemand();
			break;
		}
	}

	public void writePopFile(String pathToPopFile) {
		PopulationWriter popWriter = new PopulationWriter(population);
		popWriter.write(pathToPopFile);
	}

	public void writePopFileToDefaultPath() {
		writePopFile("../../runs-svn/parallel/");
	}

	private void createWestEastDemand() {
		for (int i = 0; i < this.numberOfPersons; i++) {

			// create a person
			Person person = population.getFactory().createPerson(
					Id.createPersonId(Integer.toString(i) + "_we"));
//			person.getAttributes().putAttribute("subpopulation", "symTraffic");
			personAtt.putAttribute(person.getId().toString(), "subpopulation", "symTraffic");

			// create a start activity
			Activity startAct = population.getFactory()
					.createActivityFromLinkId("dummy", Id.createLinkId("w_1"));
			// distribute agents uniformly between 8 and 9 am.
			startAct.setEndTime(8 * 3600 + (double)(i)/numberOfPersons * 3600);

			// create a drain activity
			Activity drainAct = population.getFactory().createActivityFromLinkId(
					"dummy", Id.createLinkId("6_e"));

			if (!initType.equals(InitRoutesType.NONE)) {
				Leg leg = createWestEastLeg(true);
				Plan planNorth = createPlan(startAct, leg, drainAct);
				leg = createWestEastLeg(false);
				Plan planSouth = createPlan(startAct, leg, drainAct);
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
				case ALL_LEFT:
				case ALL_N_W:
					person.setSelectedPlan(planNorth);
					break;
				default:
					break;
				}
			} else {
				Leg leg = population.getFactory().createLeg(TransportMode.car);
				Plan plan = createPlan(startAct, leg, drainAct);
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
			path.add(Id.createLinkId("3_4"));
			path.add(Id.createLinkId("4_5"));
		} else {
			path.add(Id.createLinkId("2_7"));
			path.add(Id.createLinkId("7_8"));
			path.add(Id.createLinkId("8_5"));
		}

		path.add(Id.createLinkId("5_6"));

		Route route = RouteUtils.createLinkNetworkRouteImpl(Id.createLinkId("w_1"), path, Id.createLinkId("6_e"));

		leg.setRoute(route);
		return leg;
	}

	private void createEastWestDemand() {
		for (int i = 0; i < this.numberOfPersons; i++) {

			// create a person
			Person person = population.getFactory().createPerson(
					Id.createPersonId(Integer.toString(i) + "_ew"));
//			person.getAttributes().putAttribute("subpopulation", "symTraffic");
			personAtt.putAttribute(person.getId().toString(), "subpopulation", "symTraffic");

			// create a start activity
			Activity startAct = population.getFactory()
					.createActivityFromLinkId("dummy", Id.createLinkId("e_6"));
			// distribute agents uniformly between 8 and 9 am.
			startAct.setEndTime(8 * 3600 + (double)(i)/numberOfPersons * 3600);

			// create a drain activity
			Activity drainAct = population.getFactory().createActivityFromLinkId(
					"dummy", Id.createLinkId("1_w"));

			if (!initType.equals(InitRoutesType.NONE)) {
				Leg leg = createEastWestLeg(true);
				Plan planNorth = createPlan(startAct, leg, drainAct);
				leg = createEastWestLeg(false);
				Plan planSouth = createPlan(startAct, leg, drainAct);
				person.addPlan(planNorth);
				person.addPlan(planSouth);
				switch (initType) {
				case ALL_RIGHT:
				case ALL_N_W:
					person.setSelectedPlan(planNorth);
					break;
				case OSZILLATING:
					if (i % 2 == 0) {
						person.setSelectedPlan(planNorth);
					} else {
						person.setSelectedPlan(planSouth);
					}
					break;
				case ALL_LEFT:
					person.setSelectedPlan(planSouth);
					break;
				default:
					break;
				}
			} else {
				Leg leg = population.getFactory().createLeg(TransportMode.car);
				Plan plan = createPlan(startAct, leg, drainAct);
				person.addPlan(plan);
			}

			// store information in population
			population.addPerson(person);
		}
	}

	private Leg createEastWestLeg(boolean takeNorthernPath) {
		Leg leg = population.getFactory().createLeg(TransportMode.car);

		List<Id<Link>> path = new ArrayList<>();
		path.add(Id.createLinkId("6_5"));
		if (takeNorthernPath) {
			path.add(Id.createLinkId("5_4"));
			path.add(Id.createLinkId("4_3"));
			path.add(Id.createLinkId("3_2"));
		} else {
			path.add(Id.createLinkId("5_8"));
			path.add(Id.createLinkId("8_7"));
			path.add(Id.createLinkId("7_2"));
		}
		path.add(Id.createLinkId("2_1"));

		Route route = RouteUtils.createLinkNetworkRouteImpl(Id.createLinkId("e_6"), path, Id.createLinkId("1_w"));

		leg.setRoute(route);
		return leg;
	}

	private void createNorthSouthDemand() {
		for (int i = 0; i < this.numberOfPersons; i++) {

			// create a person
			Person person = population.getFactory().createPerson(
					Id.createPersonId(Integer.toString(i) + "_ns"));
//			person.getAttributes().putAttribute("subpopulation", "symTraffic");
			personAtt.putAttribute(person.getId().toString(), "subpopulation", "symTraffic");

			// create a start activity
			Activity startAct = population.getFactory()
					.createActivityFromLinkId("dummy", Id.createLinkId("n_9"));
			// distribute agents uniformly between 8 and 9 am.
			startAct.setEndTime(8 * 3600 + (double)(i)/numberOfPersons * 3600);

			// create a drain activity
			Activity drainAct = population.getFactory().createActivityFromLinkId(
					"dummy", Id.createLinkId("12_s"));

			if (!initType.equals(InitRoutesType.NONE)) {
				Leg leg = createNorthSouthLeg(true);
				Plan planWest = createPlan(startAct, leg, drainAct);
				leg = createNorthSouthLeg(false);
				Plan planEast = createPlan(startAct, leg, drainAct);
				person.addPlan(planWest);
				person.addPlan(planEast);
				switch (initType) {
				case ALL_RIGHT:
				case ALL_N_W:
					person.setSelectedPlan(planWest);
					break;
				case OSZILLATING:
					if (i % 2 == 0) {
						person.setSelectedPlan(planWest);
					} else {
						person.setSelectedPlan(planEast);
					}
					break;
				case ALL_LEFT:
					person.setSelectedPlan(planEast);
					break;
				default:
					break;
				}
			} else {
				Leg leg = population.getFactory().createLeg(TransportMode.car);
				Plan plan = createPlan(startAct, leg, drainAct);
				person.addPlan(plan);
			}

			// store information in population
			population.addPerson(person);
		}
	}

	private Leg createNorthSouthLeg(boolean takeWesternPath) {
		Leg leg = population.getFactory().createLeg(TransportMode.car);

		List<Id<Link>> path = new ArrayList<>();
		path.add(Id.createLinkId("9_10"));
		if (takeWesternPath) {
			path.add(Id.createLinkId("10_3"));
			path.add(Id.createLinkId("3_7"));
			path.add(Id.createLinkId("7_11"));
		} else {
			path.add(Id.createLinkId("10_4"));
			path.add(Id.createLinkId("4_8"));
			path.add(Id.createLinkId("8_11"));
		}
		path.add(Id.createLinkId("11_12"));

		Route route = RouteUtils.createLinkNetworkRouteImpl(Id.createLinkId("n_9"), path, Id.createLinkId("12_s"));

		leg.setRoute(route);
		return leg;
	}

	private void createSouthNorthDemand() {
		for (int i = 0; i < this.numberOfPersons; i++) {

			// create a person
			Person person = population.getFactory().createPerson(
					Id.createPersonId(Integer.toString(i) + "_sn"));
//			person.getAttributes().putAttribute("subpopulation", "symTraffic");
			personAtt.putAttribute(person.getId().toString(), "subpopulation", "symTraffic");

			// create a start activity
			Activity startAct = population.getFactory()
					.createActivityFromLinkId("dummy", Id.createLinkId("s_12"));
			// distribute agents uniformly between 8 and 9 am.
			startAct.setEndTime(8 * 3600 + (double)(i)/numberOfPersons * 3600);

			// create a drain activity
			Activity drainAct = population.getFactory().createActivityFromLinkId(
					"dummy", Id.createLinkId("9_n"));

			if (!initType.equals(InitRoutesType.NONE)) {
				Leg leg = createSouthNorthLeg(true);
				Plan planWest = createPlan(startAct, leg, drainAct);
				leg = createSouthNorthLeg(false);
				Plan planEast = createPlan(startAct, leg, drainAct);
				person.addPlan(planWest);
				person.addPlan(planEast);
				switch (initType) {
				case ALL_RIGHT:
					person.setSelectedPlan(planEast);
					break;
				case OSZILLATING:
					if (i % 2 == 0) {
						person.setSelectedPlan(planWest);
					} else {
						person.setSelectedPlan(planEast);
					}
					break;
				case ALL_N_W:
				case ALL_LEFT:
					person.setSelectedPlan(planWest);
					break;
				default:
					break;
				}
			} else {
				Leg leg = population.getFactory().createLeg(TransportMode.car);
				Plan plan = createPlan(startAct, leg, drainAct);
				person.addPlan(plan);
			}

			// store information in population
			population.addPerson(person);
		}
	}

	private Leg createSouthNorthLeg(boolean takeWesternPath) {
		Leg leg = population.getFactory().createLeg(TransportMode.car);

		List<Id<Link>> path = new ArrayList<>();
		path.add(Id.createLinkId("12_11"));
		if (takeWesternPath) {
			path.add(Id.createLinkId("11_7"));
			path.add(Id.createLinkId("7_3"));
			path.add(Id.createLinkId("3_10"));
		} else {
			path.add(Id.createLinkId("11_8"));
			path.add(Id.createLinkId("8_4"));
			path.add(Id.createLinkId("4_10"));
		}
		path.add(Id.createLinkId("10_9"));

		Route route = RouteUtils.createLinkNetworkRouteImpl(Id.createLinkId("s_12"), path, Id.createLinkId("9_n"));

		leg.setRoute(route);
		return leg;
	}

	private void createCrossingDemandSouthNorth() {
		for (int i = 0; i < this.numberOfPersons; i++) {
			// create a person
			Person person = population.getFactory().createPerson(
					Id.createPersonId(Integer.toString(i) + "_sn"));
//			person.getAttributes().putAttribute("subpopulation", "crossingTraffic");
			personAtt.putAttribute(person.getId().toString(), "subpopulation", "crossingTraffic");

			// create a start activity
			Activity startAct = population.getFactory()
					.createActivityFromLinkId("dummy", Id.createLinkId("s_12"));
			// distribute agents uniformly between 8 and 9 am.
			startAct.setEndTime(8 * 3600 + (double)(i)/numberOfPersons * 3600);

			// create a drain activity
			Activity drainAct = population.getFactory().createActivityFromLinkId(
					"dummy", Id.createLinkId("9_n"));

			// initialize eastern route and stick to this during replanning
			Leg leg = createSouthNorthLeg(false);
			Plan planEast = createPlan(startAct, leg, drainAct);
			person.addPlan(planEast);
			person.setSelectedPlan(planEast);

			// store information in population
			population.addPerson(person);
		}
	}
	
	private void createCrossingDemandNorthSouth() {
		for (int i = 0; i < this.numberOfPersons; i++) {
			// create a person
			Person person = population.getFactory().createPerson(
					Id.createPersonId(Integer.toString(i) + "_ns"));
//			person.getAttributes().putAttribute("subpopulation", "crossingTraffic");
			personAtt.putAttribute(person.getId().toString(), "subpopulation", "crossingTraffic");

			// create a start activity
			Activity startAct = population.getFactory()
					.createActivityFromLinkId("dummy", Id.createLinkId("n_9"));
			// distribute agents uniformly between 8 and 9 am.
			startAct.setEndTime(8 * 3600 + (double)(i)/numberOfPersons * 3600);

			// create a drain activity
			Activity drainAct = population.getFactory().createActivityFromLinkId(
					"dummy", Id.createLinkId("12_s"));

			// initialize western route and stick to this during replanning
			Leg leg = createNorthSouthLeg(true);
			Plan planWest = createPlan(startAct, leg, drainAct);
			person.addPlan(planWest);
			person.setSelectedPlan(planWest);

			// store information in population
			population.addPerson(person);
		}
	}

	private Plan createPlan(Activity startAct, Leg leg, Activity drainAct) {
		
		Plan plan = population.getFactory().createPlan();

		plan.addActivity(startAct);
		plan.addLeg(leg);
		plan.addActivity(drainAct);
		plan.setScore(initPlanScore);
		
		return plan;
	}

	public void setDemandType(DemandType demandType) {
		this.demandType = demandType;
	}
}
