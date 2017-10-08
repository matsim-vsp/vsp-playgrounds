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
package playground.dziemke.utils;

import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

/**
 * @author dziemke
 * 
 * Reads in a plans file and copies persons with their plans into a new plans file according to
 * configurable parameters. Then writes new plans file to a given location.
 */
public class PlanFileModifier {
	private final static Logger LOG = Logger.getLogger(PlanFileModifier.class);
	
	public static void main(String[] args) {
		// Check if args has an interpretable length
		if (args.length != 0 && args.length != 9 && args.length != 11) {
			throw new IllegalArgumentException("Arguments array must have a length of 0, 9, or 11!");
		}
		
		// Local use
		String inputPlansFile = "../../upretoria/data/capetown/scenario_2017/original/population.xml.gz";
		String outputPlansFile = "../../upretoria/data/capetown/scenario_2017/population_32734.xml.gz";
		double selectionProbability = 1.;
		boolean onlyTransferSelectedPlan = false;
		boolean considerHomeStayingAgents = true;
		boolean includeStayHomePlans = true;
		boolean onlyConsiderPeopleAlwaysGoingByCar = false;
		int maxNumberOfAgentsConsidered = 10000000;
		boolean removeLinksAndRoutes = false;
		String inputCRS = TransformationFactory.HARTEBEESTHOEK94_LO19;
		String outputCRS = "EPSG:32734";
		
		CoordinateTransformation ct;
		if (inputCRS == null && outputCRS == null) {
			ct = new IdentityTransformation();
		} else {
			ct = TransformationFactory.getCoordinateTransformation(inputCRS, outputCRS);
		}
		
		// Server use, version without CRS transformation
		if (args.length == 9) {
			inputPlansFile = args[0];
			outputPlansFile = args[1];
			selectionProbability = Double.parseDouble(args[2]);
			onlyTransferSelectedPlan = Boolean.parseBoolean(args[3]);
			considerHomeStayingAgents = Boolean.parseBoolean(args[4]);
			includeStayHomePlans = Boolean.parseBoolean(args[5]);
			onlyConsiderPeopleAlwaysGoingByCar = Boolean.parseBoolean(args[6]);
			maxNumberOfAgentsConsidered = Integer.parseInt(args[7]);
			removeLinksAndRoutes = Boolean.parseBoolean(args[8]);
			inputCRS = null;
			outputCRS = null;
		}
		
		// Server use, newer version with CRS transformation
		if (args.length == 11) {
			inputPlansFile = args[0];
			outputPlansFile = args[1];
			selectionProbability = Double.parseDouble(args[2]);
			onlyTransferSelectedPlan = Boolean.parseBoolean(args[3]);
			considerHomeStayingAgents = Boolean.parseBoolean(args[4]);
			includeStayHomePlans = Boolean.parseBoolean(args[5]);
			onlyConsiderPeopleAlwaysGoingByCar = Boolean.parseBoolean(args[6]);
			maxNumberOfAgentsConsidered = Integer.parseInt(args[7]);
			removeLinksAndRoutes = Boolean.parseBoolean(args[8]);
			inputCRS = args[9];
			outputCRS = args[10];
		}
		
		modifyPlans(inputPlansFile, outputPlansFile, selectionProbability, onlyTransferSelectedPlan,
				considerHomeStayingAgents, includeStayHomePlans, onlyConsiderPeopleAlwaysGoingByCar,
				maxNumberOfAgentsConsidered, removeLinksAndRoutes, ct);
	}
		
	public static void modifyPlans (String inputPlansFile, String outputPlansFile, double selectionProbability, boolean onlyTransferSelectedPlan,
			boolean considerHomeStayingAgents, boolean includeStayHomePlans, boolean onlyConsiderPeopleAlwaysGoingByCar,
			int maxNumberOfAgentsConsidered, boolean removeLinksAndRoutes, CoordinateTransformation ct) {
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenario).readFile(inputPlansFile);
		Population population = scenario.getPopulation();

		Scenario scenario2 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population population2 = scenario2.getPopulation();
		
		int agentCounter = 0;
		
		for (Person person : population.getPersons().values()) {
			if (agentCounter < maxNumberOfAgentsConsidered) {
				Random random = MatsimRandom.getLocalInstance();
				
				Plan selectedPlan = person.getSelectedPlan();
				boolean copyPerson = decideIfPersonWillBeCopied(selectionProbability, considerHomeStayingAgents,
						onlyConsiderPeopleAlwaysGoingByCar, person, random, selectedPlan);
				
				// If selected according to all criteria, create a copy of the person and add it to new population
				if (copyPerson) {
					createPersonAndAddToPopulation(onlyTransferSelectedPlan, includeStayHomePlans,
							removeLinksAndRoutes, population2, person, selectedPlan, ct);
				}
			}
			agentCounter ++;
		}
						
		// Write population file
		new PopulationWriter(scenario2.getPopulation(), null).write(outputPlansFile);
		LOG.info("Modified plans file contains " + agentCounter + " agents.");
		LOG.info("Modified plans file has been written to " + outputPlansFile);
	}

	private static boolean decideIfPersonWillBeCopied(double selectionProbability, boolean considerHomeStayingAgents,
			boolean onlyConsiderPeopleAlwaysGoingByCar, Person person, Random random, Plan selectedPlan) {
		boolean copyPerson = true;
		if (!considerHomeStayingAgents) {
			if (selectedPlan.getPlanElements().size() <= 1) {
				copyPerson = false;
			}
		}
		int numberOfPlans = person.getPlans().size();
		if (onlyConsiderPeopleAlwaysGoingByCar) {
			for (int i=0; i < numberOfPlans; i++) {
				Plan plan = person.getPlans().get(i);						
				int numberOfPlanElements = plan.getPlanElements().size();
				for (int j=0; j < numberOfPlanElements; j++) {
					if (plan.getPlanElements().get(j) instanceof Leg) {
						Leg leg = (Leg) plan.getPlanElements().get(j);
						if (!leg.getMode().equals(TransportMode.car)) {
							copyPerson = false;
						}
					}
				}
			}
		}
		if (random.nextDouble() > selectionProbability) {
			copyPerson = false;
		}
		return copyPerson;
	}

	private static void createPersonAndAddToPopulation(boolean onlyTransferSelectedPlan, boolean includeStayHomePlans,
			boolean removeLinksAndRoutes, Population population, Person person, Plan selectedPlan, CoordinateTransformation ct) {
		Id<Person> id = person.getId();
		Person person2 = population.getFactory().createPerson(id);
		
		if (onlyTransferSelectedPlan) {
			transformCoordinates(selectedPlan, ct);
			if (removeLinksAndRoutes) {
				removeLinksAndRoutes(selectedPlan);
			}
			person2.addPlan(selectedPlan);
			population.addPerson(person2);
		} else {
			for (int i=0; i < person.getPlans().size(); i++) {
				boolean considerPlan = true;
				
				Plan plan = person.getPlans().get(i);
				int numberOfPlanElements = plan.getPlanElements().size();
				
				if (!includeStayHomePlans) {
					if (numberOfPlanElements <= 1) {
						considerPlan = false;
					}
				}
				
				if (considerPlan) {
					transformCoordinates(plan, ct);
					if (removeLinksAndRoutes) {
						removeLinksAndRoutes(plan);
					}
					person2.addPlan(plan);
				}
			}
			population.addPerson(person2);
		}
	}
	
	private static void removeLinksAndRoutes(Plan plan) {
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {
				((Activity) pe).setLinkId(null); // Remove link
			}
			if (pe instanceof Leg) {
				((Leg) pe).setRoute(null); // Remove route
			}
		}
	}
	
	private static void transformCoordinates(Plan plan, CoordinateTransformation ct) {
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {
				((Activity) pe).setCoord(ct.transform(((Activity) pe).getCoord()));
			}
		}
	}
}