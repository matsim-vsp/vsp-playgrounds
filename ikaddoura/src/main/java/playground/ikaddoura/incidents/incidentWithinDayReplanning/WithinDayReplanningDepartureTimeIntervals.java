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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;
import org.matsim.withinday.utils.EditRoutes;

import com.google.inject.Inject;

/**
 * Within-day replan all registered and traveling agents at their departure time
 * and then in regular intervals, e.g. 600 sec after departure, 1200 sec after departure and so on.
 * 
 * @author nagel, ikaddoura
 *
 */
public class WithinDayReplanningDepartureTimeIntervals implements MobsimBeforeSimStepListener, IterationStartsListener, PersonDepartureEventHandler, PersonArrivalEventHandler, LinkEnterEventHandler, PersonEntersVehicleEventHandler, IterationEndsListener {

	private static final Logger log = Logger.getLogger(WithinDayReplanningDepartureTimeIntervals.class);
	
	private final int withinDayReplanInterval;
	private final Set<Id<Person>> withinDayReplanningAgents;
	
	private EditRoutes editRoutes;
	private int modifiedRoutesCounter = 0;
	private int reRouteSameRouteCounter = 0;
	private int replanningCounter = 0;
	
	private Map<Id<Person>, Double> person2nextReplanTime = new HashMap<>();
	private Map<Id<Person>, Id<Link>> person2link = new HashMap<>();
	private Map<Id<Vehicle>, Id<Person>> vehicleId2PersonId = new HashMap<>();

	@Inject
	private Scenario scenario;
	
	@Inject
	private LeastCostPathCalculatorFactory pathAlgoFactory;
	
	@Inject
	private TravelTime travelTime;
	
	@Inject
	private Map<String, TravelDisutilityFactory> travelDisutilityFactories;
	
	public WithinDayReplanningDepartureTimeIntervals(Set<Id<Person>> personIds, int withinDayReplanInterval) {
		
		this.withinDayReplanningAgents = personIds;
		this.withinDayReplanInterval = withinDayReplanInterval;
		
		log.info("****** Considering " + withinDayReplanningAgents.size() + " agents for within-day replanning...");
				
		if (withinDayReplanningAgents.isEmpty()) {
			throw new RuntimeException("****** No agent considered for replanning. Either re-route all agents or use the time variant network. Aborting...");
		} else {
			log.info("****** Re-routing " + withinDayReplanningAgents.size() + " agents every " + this.withinDayReplanInterval + " seconds.");
		}
		
		log.info("****** Within-day replanning interval: " + withinDayReplanInterval);
	}

	@Override
	public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent event) {	
				
		final Netsim mobsim = (Netsim) event.getQueueSimulation();
		final double now = mobsim.getSimTimer().getTimeOfDay();
		
		for (Id<Person> personId : this.person2nextReplanTime.keySet()) {
			if (withinDayReplanningAgents.contains(personId)) {
				if (Math.floor(now) == this.person2nextReplanTime.get(personId)) {
										
					for (MobsimVehicle vehicle : mobsim.getNetsimNetwork().getNetsimLink( this.person2link.get(personId) ).getAllNonParkedVehicles()) {
						if (vehicle.getDriver().getId().toString().equals(personId.toString())) {
							doReplanning(vehicle.getDriver(), mobsim);
							replanningCounter++;
							
							if (replanningCounter % 1000 == 0) {
								log.info("Within-day replanning agent at time " + Time.writeTime(now, Time.TIMEFORMAT_HHMMSS) + ": #" + replanningCounter);
							}
							
							double nextWithinDayReplanningTime = now + withinDayReplanInterval;
							this.person2nextReplanTime.put(personId, nextWithinDayReplanningTime);
						}
					}
				}
			}
		}
	}
	
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		this.person2nextReplanTime.put(event.getPersonId(), event.getTime() + 1);
		this.person2link.put(event.getPersonId(), event.getLinkId());
	}
	

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		this.person2nextReplanTime.remove(event.getPersonId());
		this.person2link.remove(event.getPersonId());
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		this.person2link.put(this.vehicleId2PersonId.get(event.getVehicleId()), event.getLinkId());
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
//			double ttNew = 0.;
//
//			if (log.isDebugEnabled()) {
//				for (int index = currentLinkIndex; index < oldLinkIds.size(); index++) {
//					ttOld += travelTime.getLinkTravelTime(scenario.getNetwork().getLinks().get(oldLinkIds.get(index)), now, plan.getPerson(), null);
//				}
//				
//				for (int index = currentLinkIndex; index < currentLinkIds.size(); index++) {
//					ttNew += travelTime.getLinkTravelTime(scenario.getNetwork().getLinks().get(currentLinkIds.get(index)), now, plan.getPerson(), null);
//				}
//			}
//			
//			log.debug("person: " + plan.getPerson().getId());
//			log.debug("start Link: " + leg.getRoute().getStartLinkId());
//			log.debug("end Link: " + leg.getRoute().getEndLinkId());
//			
//			log.debug("current Link: " + oldLinkIds.get(currentLinkIndex));
//			
//			log.debug("old route: " + oldLinkIds.toString());
//			log.debug("old route - travel time: " + ttOld);
//
//			log.debug("new route: " + currentLinkIds.toString());
//			log.debug("new route - travel time: " + ttNew);
//			
//			if (now == (8. * 3600 + 60.)) {
//				log.debug("travel time on link 7_8 at 03:00:00 " + travelTime.getLinkTravelTime(scenario.getNetwork().getLinks().get(Id.createLinkId("link_7_8")), 3 * 3600., plan.getPerson(), null));
//				log.debug("travel time on link 7_8 (now): " + travelTime.getLinkTravelTime(scenario.getNetwork().getLinks().get(Id.createLinkId("link_7_8")), now, plan.getPerson(), null));
//			}
						
			if ( !Arrays.deepEquals(oldLinkIds.toArray(), currentLinkIds.toArray()) ) {
				modifiedRoutesCounter++;
//				log.debug("Route was modified!");
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
	
		log.info("-------------------------------------------");
		log.info("Agents allowed to do within-day replanning:");
		for (Id<Person> personId : this.withinDayReplanningAgents) {
			log.info(personId);
		}
		log.info("-------------------------------------------");	
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		this.vehicleId2PersonId.put(event.getVehicleId(), event.getPersonId());
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		log.info("****** Iteration ends...");
		log.info("****** Number of re-routed trips (different transport route): " + modifiedRoutesCounter);
		log.info("****** Number of re-routed trips (same transport route): " + reRouteSameRouteCounter);
		
	}

}

