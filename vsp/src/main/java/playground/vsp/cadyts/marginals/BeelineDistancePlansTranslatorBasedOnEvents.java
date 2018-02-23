/* *********************************************************************** *
 * project: org.matsim.*
 * PlanToPlanStepBasedOnEvents.java
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

package playground.vsp.cadyts.marginals;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import cadyts.demand.PlanBuilder;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.cadyts.general.PlansTranslator;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.network.NetworkUtils;
import playground.vsp.cadyts.marginals.prep.DistanceBin;
import playground.vsp.cadyts.marginals.prep.DistanceDistribution;
import playground.vsp.cadyts.marginals.prep.DistanceDistributionUtils;
import playground.vsp.cadyts.marginals.prep.ModalBinIdentifier;

class BeelineDistancePlansTranslatorBasedOnEvents implements PlansTranslator<ModalBinIdentifier>, PersonDepartureEventHandler,
		PersonArrivalEventHandler {

	private static final Logger log = Logger.getLogger(BeelineDistancePlansTranslatorBasedOnEvents.class);

	private final Scenario scenario;

	private final Map<Id<Person>, Coord> personToOriginCoord = new HashMap<>();

	private int iteration = -1;

	// this is _only_ there for output:
    private final Set<Plan> plansEverSeen = new HashSet<>();

	//<--a problem may appear if using following same strings with multiple Cadyts contexts. Amit Feb'18-->
	private static final String STR_PLANSTEPFACTORY = "planStepFactory"+ModalDistanceCadytsBuilderImpl.MARGINALS;
	private static final String STR_ITERATION = "iteration"+ModalDistanceCadytsBuilderImpl.MARGINALS;

	private final Map<Id<ModalBinIdentifier>, ModalBinIdentifier> modalDistanceBinMap;
	private final DistanceDistribution inputDistanceDistribution;

	@Inject
    BeelineDistancePlansTranslatorBasedOnEvents(final Scenario scenario, DistanceDistribution inputDistanceDistribution) {
		this.scenario = scenario;
		this.inputDistanceDistribution = inputDistanceDistribution;
		this.modalDistanceBinMap = inputDistanceDistribution.getModalBins();
	}

	private long plansFound = 0;
	private long plansNotFound = 0;

	@Override
	public final cadyts.demand.Plan<ModalBinIdentifier> getCadytsPlan(final Plan plan) {
		PlanBuilder<ModalBinIdentifier> planStepFactory = (PlanBuilder<ModalBinIdentifier>) plan.getCustomAttributes().get(STR_PLANSTEPFACTORY);
		if (planStepFactory == null) {
			this.plansNotFound++;
			return null;
		}
		this.plansFound++;
		return planStepFactory.getResult();
	}

	@Override
	public void reset(final int iteration) {
		this.personToOriginCoord.clear();
		this.iteration = iteration;

		log.warn("found " + this.plansFound + " out of " + (this.plansFound + this.plansNotFound) + " ("
				+ (100. * this.plansFound / (this.plansFound + this.plansNotFound)) + "%)");
		log.warn("(above values may both be at zero for a couple of iterations if multiple plans per agent all have no score)");
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		this.personToOriginCoord.put(event.getPersonId(), scenario.getNetwork().getLinks().get(event.getLinkId()).getToNode().getCoord());
	}
	
	@Override
	public void handleEvent(PersonArrivalEvent event) {
		String mode  = event.getLegMode();

		if (this.inputDistanceDistribution.getDistanceRanges(mode).isEmpty()){
			log.warn("The distance range for mode "+mode+" in the input distance distribution is empty. This will be excluded from the calibration.");
			return;
		}

		Coord originCoord = this.personToOriginCoord.get(event.getPersonId());
		Coord destinationCoord = this.scenario.getNetwork().getLinks().get(event.getLinkId()).getToNode().getCoord();

		//TODO check if we should include beeline distance factor which is not available for network mdoes
		PlansCalcRouteConfigGroup.ModeRoutingParams params = this.scenario.getConfig().plansCalcRoute().getModeRoutingParams().get(mode);
		double beelineDistanceFactor = 1.0;
		if (params!=null) beelineDistanceFactor = params.getBeelineDistanceFactor();
		else if (this.inputDistanceDistribution.getModeToBeelineDistanceFactor().containsKey(mode)){
			beelineDistanceFactor = this.inputDistanceDistribution.getModeToBeelineDistanceFactor().get(mode);
		} else{
			log.warn("The beeline distance factor for mode "+mode+" is not given. Using 1.0");
		}
		double beelineDistance = beelineDistanceFactor *
				NetworkUtils.getEuclideanDistance(originCoord, destinationCoord);

		DistanceBin.DistanceRange distanceRange = DistanceDistributionUtils.getDistanceRange(beelineDistance, this.inputDistanceDistribution.getDistanceRanges(mode));

		// if only a subset of links is calibrated but the link is not contained, ignore the event
		Id<ModalBinIdentifier> mlId = DistanceDistributionUtils.getModalBinId(mode,distanceRange);
		if (this.modalDistanceBinMap.get(mlId) == null) return;

		// get the "Person" behind the id:
		Person person = this.scenario.getPopulation().getPersons().get(event.getPersonId());
		
		// get the selected plan:
		Plan selectedPlan = person.getSelectedPlan();
		
		// get the planStepFactory for the plan (or create one):
		PlanBuilder<ModalBinIdentifier> tmpPlanStepFactory = getPlanStepFactoryForPlan(selectedPlan);
		
		if (tmpPlanStepFactory != null) {
						
			//this beeline distance is being checked from start_time (lower_limit) and end_time (upper_limit)
			tmpPlanStepFactory.addTurn( this.modalDistanceBinMap.get(mlId), (int) beelineDistance);
		}
	}

	// ###################################################################################
	// only private functions below here (low level functionality)

	private PlanBuilder<ModalBinIdentifier> getPlanStepFactoryForPlan(final Plan selectedPlan) {
		PlanBuilder<ModalBinIdentifier> planStepFactory = null;

		planStepFactory = (PlanBuilder<ModalBinIdentifier>) selectedPlan.getCustomAttributes().get(STR_PLANSTEPFACTORY);
		Integer factoryIteration = (Integer) selectedPlan.getCustomAttributes().get(STR_ITERATION);
		if (planStepFactory == null || factoryIteration == null || factoryIteration != this.iteration) {
			// attach the iteration number to the plan:
			selectedPlan.getCustomAttributes().put(STR_ITERATION, this.iteration);

			// construct a new PlanBulder and attach it to the plan:
			planStepFactory = new PlanBuilder<>();
			selectedPlan.getCustomAttributes().put(STR_PLANSTEPFACTORY, planStepFactory);

			// memorize the plan as being seen:
			this.plansEverSeen.add(selectedPlan);
		}

		return planStepFactory;
	}

}
