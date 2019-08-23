/* *********************************************************************** *
 * project: org.matsim.*
 * MyWithinDayMobsimListener.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.vsptelematics.bangbang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.interfaces.NetsimLink;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.withinday.utils.EditRoutes;

import com.google.inject.Inject;

import playground.vsptelematics.bangbang.KNAccidentScenario.MyIterationCounter;

/**
 * @author nagel
 *
 */
class WithinDayReRouteMobsimListener implements MobsimBeforeSimStepListener {
	
	static class Config {
	
	}

	private static final Logger log = Logger.getLogger(WithinDayReRouteMobsimListener.class);

	@Inject private Scenario scenario;
	@Inject private LeastCostPathCalculatorFactory pathAlgoFactory;
	@Inject private TravelTime travelTime;
	@Inject private Map<String, TravelDisutilityFactory> travelDisutilityFactories;
	@Inject private MyIterationCounter iterationCounter;
	
	private boolean init = true ;
	
	private EditRoutes editRoutes ;
	
	private double lastReplanningIteration = Double.POSITIVE_INFINITY ;
	void setLastReplanningIteration(double lastReplanningIteration) {
		this.lastReplanningIteration = lastReplanningIteration;
	}

	private double replanningProba = 1.0 ;
	public void setReplanningProba(double replanningProba) {
		this.replanningProba = replanningProba;
	}
	
	@Override
	public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent event) {
		if ( this.iterationCounter.getIteration() > this.lastReplanningIteration ) {
			log.warn("NOT replanning"); 
			return ;
		}
		
		if ( init ){
			init= false ;
			TravelDisutility travelDisutility = travelDisutilityFactories.get(TransportMode.car).createTravelDisutility( travelTime ) ;
			LeastCostPathCalculator pathAlgo = pathAlgoFactory.createPathCalculator(scenario.getNetwork(), travelDisutility, travelTime) ;
			PopulationFactory pf = scenario.getPopulation().getFactory() ;
			this.editRoutes = new EditRoutes( scenario.getNetwork(), pathAlgo, pf ) ;
		}
		
		Collection<MobsimAgent> agentsToReplan = getAgentsToReplan( (Netsim) event.getQueueSimulation(), replanningProba);
		
		for (MobsimAgent ma : agentsToReplan) {
			doReplanning(ma, (Netsim) event.getQueueSimulation());
		}
	}
	
	private static int cnt2 = 0 ;

	static List<MobsimAgent> getAgentsToReplan(Netsim mobsim, double replanningProba) {

		List<MobsimAgent> set = new ArrayList<>();

		final double now = mobsim.getSimTimer().getTimeOfDay();
		if ( now < 8.*3600. || now > 10.*3600. || Math.floor(now) % 10 != 0 ) {
			return set;
		}

		// find agents that are on the "interesting" links:
		for ( Id<Link> linkId : KNAccidentScenario.replanningLinkIds ) {
			NetsimLink link = mobsim.getNetsimNetwork().getNetsimLink( linkId ) ;
			for (MobsimVehicle vehicle : link.getAllNonParkedVehicles()) {
				MobsimDriverAgent agent=vehicle.getDriver();
				if ( KNAccidentScenario.replanningLinkIds.contains( agent.getCurrentLinkId() ) ) {
					//					System.out.println("found agent");
					if ( cnt2==0 ) {
						log.warn("only replanning with proba=" + replanningProba + "!" );
						cnt2++ ;
					}
					if ( MatsimRandom.getRandom().nextDouble() < replanningProba) {
						set.add(agent);
					}
				}
			}
		}

		return set;

	}
	
	private static int cnt = 0 ;

	private boolean doReplanning(MobsimAgent agent, Netsim netsim ) {
		double now = netsim.getSimTimer().getTimeOfDay() ;

		Plan plan = WithinDayAgentUtils.getModifiablePlan( agent ) ; 
		
		if ( !WithinDayAgentUtils.isOnReplannableCarLeg(agent) ) {
			return false ;
		}

		final Integer planElementsIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(agent);

		// ---

		final Leg leg = (Leg) plan.getPlanElements().get(planElementsIndex);
		
		// for vis:
		List<Id<Link>> oldLinkIds = new ArrayList<>( ((NetworkRoute) leg.getRoute()).getLinkIds() ) ; // forces a copy, which I need later

		// "real" action:
		final int currentLinkIndex = WithinDayAgentUtils.getCurrentRouteLinkIdIndex(agent);
		editRoutes.replanCurrentLegRoute(leg, plan.getPerson(), currentLinkIndex, now ) ;
		
		// for vis:
		ArrayList<Id<Link>> currentLinkIds = new ArrayList<>( ((NetworkRoute) leg.getRoute()).getLinkIds() ) ;
		if ( !Arrays.deepEquals(oldLinkIds.toArray(), currentLinkIds.toArray()) ) {
			if ( cnt < 10 ) {
				log.warn("modified route");
				cnt++ ;
			}
			this.scenario.getPopulation().getPersons().get(agent.getId()).getAttributes().putAttribute("marker", true ) ;
		}

		// ---

		// finally reset the cached Values of the PersonAgent - they may have changed!
		WithinDayAgentUtils.resetCaches(agent);

		return true;
	}

}
