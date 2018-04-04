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
package playground.dziemke.input;

import java.util.Arrays;
import java.util.List;
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
	
	private String inputPlansFile;
	private String outputPlansFile;
	private double selectionProbability;
	private boolean onlyTransferSelectedPlan;
	private boolean considerHomeStayingAgents;
	private boolean includeStayHomePlans;
	private boolean onlyConsiderPeopleAlwaysGoingByCar; // TODO A leftover from an early, quite specific case; should be generalized
	private int maxNumberOfAgentsConsidered;
	private boolean removeLinksAndRoutes;
	private CoordinateTransformation ct;
	private List<String> attributesToKeep; // TODO Might be removed once handling of new, integrated attributes becomes more standard
	
	Random random = MatsimRandom.getLocalInstance();
	
	
	public static void main(String[] args) {
		// Check if args has an interpretable length
		if (args.length != 0 && args.length != 9 && args.length != 11) {
			throw new IllegalArgumentException("Arguments array must have a length of 0, 9, or 11!");
		}
		
		// Local use
//		String inputPlansFile = "../../upretoria/data/capetown/scenario_2017/original/population.xml.gz";
//		String outputPlansFile = "../../upretoria/data/capetown/scenario_2017/population_32734.xml.gz";
//		String inputPlansFile = "../../capetown/data/scenario_2017/population_32734.xml.gz";
//		String outputPlansFile = "../../capetown/data/scenario_2017/population_32734_1pct.xml.gz";
		String inputPlansFile = "../../shared-svn/studies/countries/de/open_berlin_scenario/be_x/population_2/plans.xml.gz";
		String outputPlansFile = "../../shared-svn/studies/countries/de/open_berlin_scenario/be_x/population_2/plans_small_sample.xml.gz";
//		double selectionProbability = 1.;
		double selectionProbability = 0.1;
		boolean onlyTransferSelectedPlan = false;
		boolean considerHomeStayingAgents = true;
		boolean includeStayHomePlans = true;
		boolean onlyConsiderPeopleAlwaysGoingByCar = false;
		int maxNumberOfAgentsConsidered = 10000;
		boolean removeLinksAndRoutes = false;
//		String inputCRS = TransformationFactory.HARTEBEESTHOEK94_LO19;
//		String outputCRS = "EPSG:32734";
		String inputCRS = null;
		String outputCRS = null;
		List<String> attributesToKeep = Arrays.asList("age", "employed", "gender", "hasLicense", "householdId", "locationOfSchool", "locationOfWork", "parent", "student");

		
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
			attributesToKeep = Arrays.asList();
		}
		
		// Server use, version with CRS transformation
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
			attributesToKeep = Arrays.asList();
		}
		
		PlanFileModifier planFileModifier = new PlanFileModifier(inputPlansFile, outputPlansFile, selectionProbability, onlyTransferSelectedPlan,
				considerHomeStayingAgents, includeStayHomePlans, onlyConsiderPeopleAlwaysGoingByCar,
				maxNumberOfAgentsConsidered, removeLinksAndRoutes, ct, attributesToKeep);
		
		planFileModifier.modifyPlans();
	}
	
	
	public PlanFileModifier(String inputPlansFile, String outputPlansFile, double selectionProbability, boolean onlyTransferSelectedPlan,
			boolean considerHomeStayingAgents, boolean includeStayHomePlans, boolean onlyConsiderPeopleAlwaysGoingByCar,
			int maxNumberOfAgentsConsidered, boolean removeLinksAndRoutes, CoordinateTransformation ct, List<String> attributesToKeep) {
		this.inputPlansFile = inputPlansFile;
		this.outputPlansFile = outputPlansFile;
		this.selectionProbability = selectionProbability;
		this.onlyTransferSelectedPlan = onlyTransferSelectedPlan;
		this.considerHomeStayingAgents = considerHomeStayingAgents;
		this.includeStayHomePlans = includeStayHomePlans;
		this.onlyConsiderPeopleAlwaysGoingByCar = onlyConsiderPeopleAlwaysGoingByCar;
		this.maxNumberOfAgentsConsidered = maxNumberOfAgentsConsidered;
		this.removeLinksAndRoutes = removeLinksAndRoutes;
		this.onlyTransferSelectedPlan = onlyTransferSelectedPlan;
		this.ct = ct;
		this.attributesToKeep = attributesToKeep;
	}
	
	public void modifyPlans() {		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenario).readFile(inputPlansFile);
		Population population = scenario.getPopulation();

		Scenario scenario2 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population population2 = scenario2.getPopulation();
		
		int agentCounter = 0;
		
		
		for (Person person : population.getPersons().values()) {
			if (agentCounter < maxNumberOfAgentsConsidered) {
				
				Plan selectedPlan = person.getSelectedPlan();
				boolean copyPerson = decideIfPersonIsCopied(person, selectedPlan);
				
				// If selected according to all criteria, create a copy of the person and add it to new population
				if (copyPerson) {
					createPersonAndAddToPopulation(population2, person, selectedPlan);
					agentCounter ++;
				}
			}
		}
						
		// Write population file
		new PopulationWriter(scenario2.getPopulation(), null).write(outputPlansFile);
		LOG.info("Modified plans file contains " + agentCounter + " agents.");
		LOG.info("Modified plans file has been written to " + outputPlansFile);
	}

	private boolean decideIfPersonIsCopied(Person person, Plan selectedPlan) {
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

	private void createPersonAndAddToPopulation(Population population, Person person, Plan selectedPlan) {
		Id<Person> id = person.getId();
		Person person2 = population.getFactory().createPerson(id);
		
		if (onlyTransferSelectedPlan) {
			transformCoordinates(selectedPlan);
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
					transformCoordinates(plan);
					if (removeLinksAndRoutes) {
						removeLinksAndRoutes(plan);
					}
					person2.addPlan(plan);
				}
			}
			
			// Keeping the attributes of a person
			for (String attribute : attributesToKeep) {
				person2.getAttributes().putAttribute(attribute, person.getAttributes().getAttribute(attribute));
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
	
	private void transformCoordinates(Plan plan) {
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity) {
				((Activity) pe).setCoord(ct.transform(((Activity) pe).getCoord()));
			}
		}
	}
}