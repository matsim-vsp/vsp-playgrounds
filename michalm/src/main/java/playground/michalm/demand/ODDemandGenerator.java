/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.michalm.demand;

import java.util.function.Function;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.contrib.util.random.RandomUtils;
import org.matsim.contrib.util.random.UniformRandom;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrix;

import playground.michalm.util.matrices.MatrixUtils;

public class ODDemandGenerator<L> {

	public interface PersonCreator<L> {
		Person createPerson(Plan plan, L fromLocation, L toLocation);
	}

	public interface ActivityCreator<L> {
		Activity createActivity(L location, String actType);
	}

	private final UniformRandom uniform = RandomUtils.getGlobalUniform();

	private final Scenario scenario;
	private final ActivityCreator activityCreator;
	private final PersonCreator personCreator;
	private final Function<String, L> locationProvider;
	private final boolean addEmptyRoute;

	public ODDemandGenerator(Scenario scenario, Function<String, L> locationProvider, boolean addEmptyRoute) {
		this(scenario, locationProvider, addEmptyRoute, new DefaultActivityCreator(scenario),
				new DefaultPersonCreator(scenario));
	}

	public ODDemandGenerator(Scenario scenario, Function<String, L> locationProvider, boolean addEmptyRoute,
			ActivityCreator activityCreator, PersonCreator personCreator) {
		this.scenario = scenario;
		this.locationProvider = locationProvider;
		this.addEmptyRoute = addEmptyRoute;
		this.activityCreator = activityCreator;
		this.personCreator = personCreator;
	}

	public void generateSinglePeriod(Matrix matrix, String fromActivityType, String toActivityType, String mode,
			double startTime, double duration, double flowCoeff) {
		PopulationFactory pf = scenario.getPopulation().getFactory();
		Iterable<Entry> entryIter = MatrixUtils.createEntryIterable(matrix);

		for (Entry e : entryIter) {
			L fromLocation = locationProvider.apply(e.getFromLocation());
			L toLocation = locationProvider.apply(e.getToLocation());
			int trips = (int)uniform.floorOrCeil(flowCoeff * e.getValue());

			for (int k = 0; k < trips; k++) {
				Plan plan = pf.createPlan();

				// act0
				Activity startAct = activityCreator.createActivity(fromLocation, fromActivityType);
				startAct.setEndTime((int)uniform.nextDouble(startTime, startTime + duration));

				// act1
				Activity endAct = activityCreator.createActivity(toLocation, toActivityType);

				// leg
				Leg leg = pf.createLeg(mode);
				if (addEmptyRoute) {
					leg.setRoute(RouteUtils.createGenericRouteImpl(startAct.getLinkId(), endAct.getLinkId()));
				}
				leg.setDepartureTime(startAct.getEndTime().seconds());

				plan.addActivity(startAct);
				plan.addLeg(leg);
				plan.addActivity(endAct);

				Person person = personCreator.createPerson(plan, fromLocation, toLocation);
				person.addPlan(plan);
				scenario.getPopulation().addPerson(person);
			}
		}
	}

	public void generateMultiplePeriods(Matrix matrix, String fromActivityType, String toActivityType, String mode,
			double startTime, double duration, double[] flowCoeffs) {
		for (int i = 0; i < flowCoeffs.length; i++) {
			generateSinglePeriod(matrix, fromActivityType, toActivityType, mode, startTime, duration, flowCoeffs[i]);
			startTime += duration;
		}
	}

	public void generateMultiplePeriods(Matrix[] matrices, String fromActivityType, String toActivityType, String mode,
			double startTime, double duration, double flowCoeffs) {
		for (int i = 0; i < matrices.length; i++) {
			generateSinglePeriod(matrices[i], fromActivityType, toActivityType, mode, startTime, duration, flowCoeffs);
			startTime += duration;
		}
	}

	public void write(String plansFile) {
		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(plansFile);
		System.out.println("Generated population written to: " + plansFile);
	}
}
