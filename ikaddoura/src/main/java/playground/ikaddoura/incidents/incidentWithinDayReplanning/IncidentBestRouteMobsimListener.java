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

package playground.ikaddoura.incidents.incidentWithinDayReplanning;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
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
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;
import org.matsim.withinday.utils.EditRoutes;

import com.google.inject.Inject;

import playground.ikaddoura.incidents.NetworkChangeEventsUtils;

/**
 * @author nagel, ikaddoura
 *
 */
public class IncidentBestRouteMobsimListener implements MobsimBeforeSimStepListener, IterationStartsListener {

	private static final Logger log = Logger.getLogger(IncidentBestRouteMobsimListener.class);
	
	private int withinDayReplanInterval = 3600;
	private boolean onlyReplanDirectlyAffectedAgents = true;

	private Set<Id<Person>> withinDayReplanningAgents = new HashSet<>();
	private EditRoutes editRoutes;
	private int modifiedRoutesCounter;
	private int reRouteSameRouteCounter;
	
	@Inject
	private Scenario scenario;
	
	@Inject
	private LeastCostPathCalculatorFactory pathAlgoFactory;
	
	@Inject
	private TravelTime travelTime;
	
	@Inject
	private Map<String, TravelDisutilityFactory> travelDisutilityFactories;
	
	public IncidentBestRouteMobsimListener() {		
		log.info("****** Within-day replanning interval: " + withinDayReplanInterval);
	}

	@Override
	public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent event) {		
		Collection<MobsimAgent> agentsToReplan = getAgentsToReplan( (Netsim) event.getQueueSimulation() ); 
		
		for (MobsimAgent ma : agentsToReplan) {
			doReplanning(ma, (Netsim) event.getQueueSimulation());
		}		

		if ( agentsToReplan.size() > 0 ) {
			log.info("****** Number of re-routed trips (different transport route) at time " + Time.writeTime(event.getSimulationTime(), Time.TIMEFORMAT_HHMMSS) + ": " + modifiedRoutesCounter);
			log.info("****** Number of re-routed trips (same transport route) at time " + Time.writeTime(event.getSimulationTime(), Time.TIMEFORMAT_HHMMSS) + ": " + reRouteSameRouteCounter);
		}
	}

	private List<MobsimAgent> getAgentsToReplan(Netsim mobsim) {

		List<MobsimAgent> agentsToReplan = new ArrayList<MobsimAgent>();

		final double now = mobsim.getSimTimer().getTimeOfDay();
		
		if ( Math.floor(now) % withinDayReplanInterval == 0 ) {

			modifiedRoutesCounter = 0;
			reRouteSameRouteCounter = 0;
//			log.info("****** Within-day replanning at time " + Time.writeTime(now, Time.TIMEFORMAT_HHMMSS));
			
			for ( Id<Link> linkId : this.scenario.getNetwork().getLinks().keySet() ) {
				NetsimLink link = mobsim.getNetsimNetwork().getNetsimLink( linkId ) ;
				for (MobsimVehicle vehicle : link.getAllNonParkedVehicles()) {
					MobsimDriverAgent agent = vehicle.getDriver();
					
					if (withinDayReplanningAgents.contains(agent.getId())) {
						agentsToReplan.add(agent);
					}
				}
			}			
		}
		
		return agentsToReplan;
	}

	private boolean doReplanning(MobsimAgent agent, Netsim netsim ) {
		double now = netsim.getSimTimer().getTimeOfDay() ;

		Plan plan = WithinDayAgentUtils.getModifiablePlan( agent ) ; 

		if (plan == null) {
			log.info( " we don't have a modifiable plan; returning ... ") ;
			return false;
		}
		if ( !(WithinDayAgentUtils.getCurrentPlanElement(agent) instanceof Leg) ) {
			log.info( "agent not on leg; returning ... ") ;
			return false ;
		}
		if (!((Leg) WithinDayAgentUtils.getCurrentPlanElement(agent)).getMode().equals(TransportMode.car)) {
			log.info( "not a car leg; can only replan car legs; returning ... ") ;
			return false;
		}

		final Integer planElementsIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(agent);
		final Leg leg = (Leg) plan.getPlanElements().get(planElementsIndex);
	
		List<Id<Link>> oldLinkIds = new ArrayList<>( ((NetworkRoute) leg.getRoute()).getLinkIds() ) ; // forces a copy, which I need later
		oldLinkIds.add(0, leg.getRoute().getStartLinkId());
		oldLinkIds.add(oldLinkIds.size(), leg.getRoute().getEndLinkId());
		
		final int currentLinkIndex = WithinDayAgentUtils.getCurrentRouteLinkIdIndex(agent) ;
		
		if (leg.getRoute().getEndLinkId().toString().equals(oldLinkIds.get(currentLinkIndex).toString())) {
			// not routing the agent from the current link to the end link because the agent is already on the end link
		} else {
			
			editRoutes.replanCurrentLegRoute(leg, plan.getPerson(), currentLinkIndex, now ) ;
			
			ArrayList<Id<Link>> currentLinkIds = new ArrayList<>( ((NetworkRoute) leg.getRoute()).getLinkIds() ) ;
			currentLinkIds.add(0, leg.getRoute().getStartLinkId());
			currentLinkIds.add(currentLinkIds.size(), leg.getRoute().getEndLinkId());
			
//			double ttOld = 0.;
//			for (int index = currentLinkIndex; index < oldLinkIds.size(); index++) {
//				ttOld += travelTime.getLinkTravelTime(scenario.getNetwork().getLinks().get(oldLinkIds.get(index)), now, plan.getPerson(), null);
//			}
//			
//			double ttNew = 0.;
//			for (int index = currentLinkIndex; index < currentLinkIds.size(); index++) {
//				ttNew += travelTime.getLinkTravelTime(scenario.getNetwork().getLinks().get(currentLinkIds.get(index)), now, plan.getPerson(), null);
//			}
//			
//			log.warn("person: " + plan.getPerson().getId());
//			log.warn("start Link: " + leg.getRoute().getStartLinkId());
//			log.warn("end Link: " + leg.getRoute().getEndLinkId());
//			
//			log.warn("current Link: " + oldLinkIds.get(currentLinkIndex));
//			
//			log.warn("old route: " + oldLinkIds.toString());
//			log.warn("old route - travel time: " + ttOld);
//
//			log.warn("new route: " + currentLinkIds.toString());
//			log.warn("new route - travel time: " + ttNew);
			
//			if (now == (8. * 3600 + 60.)) {
//				log.warn("travel time on link 7_8 at 03:00:00 " + travelTime.getLinkTravelTime(scenario.getNetwork().getLinks().get(Id.createLinkId("link_7_8")), 3 * 3600., plan.getPerson(), null));
//				log.warn("travel time on link 7_8 (now): " + travelTime.getLinkTravelTime(scenario.getNetwork().getLinks().get(Id.createLinkId("link_7_8")), now, plan.getPerson(), null));
//				System.out.println();
//			}
						
			if ( !Arrays.deepEquals(oldLinkIds.toArray(), currentLinkIds.toArray()) ) {
				modifiedRoutesCounter++;
//				log.warn("Route was modified!");
			} else {
				reRouteSameRouteCounter++;
			}

			WithinDayAgentUtils.resetCaches(agent);
		}

		return true;
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		
		this.editRoutes = new EditRoutes(scenario.getNetwork(), pathAlgoFactory.createPathCalculator(scenario.getNetwork(), this.travelDisutilityFactories.get(TransportMode.car).createTravelDisutility(travelTime), travelTime), scenario.getPopulation().getFactory());
		
		log.info("****** Iteration starts. Computing the agents to be considered for within-day replanning...");
		
		withinDayReplanningAgents.clear();
		
		if (onlyReplanDirectlyAffectedAgents) {
			log.info("****** Only re-routing agents driving along an incident link...");
			Set<Id<Link>> incidentLinkIds = NetworkChangeEventsUtils.getIncidentLinksFromNetworkChangeEventsFile(scenario);
			withinDayReplanningAgents.addAll(NetworkChangeEventsUtils.getPersonIDsOfAgentsDrivingAlongSpecificLinks(scenario, incidentLinkIds));
			
			if (withinDayReplanningAgents.isEmpty()) {
				throw new RuntimeException("****** No agent considered for replanning. Either re-route all agents or use the time variant network. Aborting...");
			} else {
				log.info("****** Re-routing " + withinDayReplanningAgents.size() + " agents every " + this.withinDayReplanInterval + " seconds.");
			}
			
		} else {
			log.info("****** Re-routing all agents every " + this.withinDayReplanInterval + " seconds.");
			withinDayReplanningAgents.addAll(this.scenario.getPopulation().getPersons().keySet());
		}
	}
	
	public int getWithinDayReplanInterval() {
		return withinDayReplanInterval;
	}

	public void setWithinDayReplanInterval(int withinDayReplanInterval) {
		log.info("Setting within-day replanning interval to " +  withinDayReplanInterval);
		this.withinDayReplanInterval = withinDayReplanInterval;
	}

	public boolean isOnlyReplanDirectlyAffectedAgents() {
		return onlyReplanDirectlyAffectedAgents;
	}

	public void setOnlyReplanDirectlyAffectedAgents(boolean onlyReplanDirectlyAffectedAgents) {
		log.info("Setting 'onlyReplanDirectlyAffectedAgents' to " +  onlyReplanDirectlyAffectedAgents);
		this.onlyReplanDirectlyAffectedAgents = onlyReplanDirectlyAffectedAgents;
	}

}

