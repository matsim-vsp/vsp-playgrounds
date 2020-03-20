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
package scenarios.illustrative.analysis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;

import com.google.inject.Inject;

/**
 * Abstract tool to analyze a MATSim simulation of an arbitrary scenario.
 * 
 * It calculates the total and average travel time on the routes and the number
 * of users on each route. Both once in total and once depending on the
 * departure and arrival times.
 * 
 * Additionally it calculates the number of route starts per second and the
 * number of agents on each route per second.
 * 
 * Last but not least it calculates the total travel time in the network.
 * 
 * Note: To use this analyzer you have to implement the abstract methods. I.e.
 * you have to define the number of different routes of your specific scenario
 * and implement a unique route determination via link enter events.
 * 
 * Note: This class calculates travel times via departure and arrival events.
 * I.e. it only gives reliable results if all agents can departure without delay
 * regarding to the flow capacity of the first link. If they are delayed because
 * of storage capacity the results are still fine.
 * 
 * The results can be plotted by gnuplot scripts (see e.g. runs-svn/braess/analysis).
 * 
 * @author tthunig, tschlenther
 */
public abstract class TtAbstractAnalysisTool implements PersonArrivalEventHandler, PersonDepartureEventHandler, 
	LinkEnterEventHandler, PersonStuckEventHandler, PersonEntersVehicleEventHandler, PersonMoneyEventHandler {

	private static final Logger log = Logger.getLogger(TtAbstractAnalysisTool.class);
	
	private double totalTT;
	private double[] totalRouteTTs;
	private double[] routeUsers_PCU;
	private double stuckedAgents_PCU = 0;
	private double[] totalRouteTolls;

	// collects the departure times per person
	private Map<Id<Person>, Double> personDepartureTimes;
	// collects information about the used route per person
	private Map<Id<Person>, Integer> personRouteChoice;
	// collects tolls paid per person
	private Map<Id<Person>, Double> personTolls;
	
	// sums up the PCUs of route starts per second (gets filled when the agent
	// arrives)
	private Map<Double, double[]> routeStartsPerTimeStep_PCU;
	// sums up the PCUs of agents on each route per second
	private Map<Double, double[]> onRoutePerSecond_PCU;

	private Map<Double, double[]> totalRouteTollsByDepartureTime;	
	private Map<Double, double[]> totalRouteTTsByDepartureTime;
	private Map<Double, int[]> numberOfRouteUsersByDepartureTime; // note: not PCU, but number of!
	
	
	private Map<Id<Vehicle>, Set<Id<Person>>> vehicle2PersonsMap;
	private Map<Id<Person>, Id<Vehicle>> person2VehicleMap;
	
	private int numberOfRoutes;

//	@Inject private ;
	private final double timeStepSize;
	private final Vehicles vehicles;

	@Inject
	public TtAbstractAnalysisTool(Scenario scenario) {
		defineNumberOfRoutes();
		reset(0);
		this.timeStepSize = scenario.getConfig().qsim().getTimeStepSize();
		this.vehicles = scenario.getVehicles();
	}
	
	/**
	 * resets all fields
	 */
	@Override
	public void reset(int iteration) {
		this.totalTT = 0.0;
		this.totalRouteTTs = new double[numberOfRoutes];
		this.routeUsers_PCU = new double[numberOfRoutes];
		this.stuckedAgents_PCU = 0;
		this.totalRouteTolls = new double[numberOfRoutes];
		
		this.personDepartureTimes = new HashMap<>();
		this.personRouteChoice = new HashMap<>();
		this.routeStartsPerTimeStep_PCU = new TreeMap<>();
		this.onRoutePerSecond_PCU = new TreeMap<>();
		this.personTolls = new HashMap<>();

		this.totalRouteTTsByDepartureTime = new TreeMap<>();
		this.numberOfRouteUsersByDepartureTime = new TreeMap<>();
		this.totalRouteTollsByDepartureTime = new TreeMap<>();
		
		this.vehicle2PersonsMap = new HashMap<>();
		this.person2VehicleMap = new HashMap<>();
	}
	
	/**
	 * Defines the field variable numberOfRoutes for the specific scenario by
	 * using the setter method.
	 */
	protected abstract void defineNumberOfRoutes();

	/**
	 * Creates a mapping between vehicles and their occupants
	 */
	@Override
	public void handleEvent(PersonEntersVehicleEvent event){
		if (!vehicle2PersonsMap.containsKey(event.getVehicleId()))
			vehicle2PersonsMap.put(event.getVehicleId(), new HashSet<Id<Person>>());
		vehicle2PersonsMap.get(event.getVehicleId()).add(event.getPersonId());
		person2VehicleMap.put(event.getPersonId(), event.getVehicleId());
	}
	
	/**
	 * Remembers the persons departure times.
	 */
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (this.personDepartureTimes.containsKey(event.getPersonId())) {
			throw new IllegalStateException(
					"A person has departured at least two times without arrival.");
		}

		// remember the persons departure time
		this.personDepartureTimes.put(event.getPersonId(), event.getTime());
	}

	/**
	 * Determines the agents route choice.
	 */
	@Override
	public void handleEvent(LinkEnterEvent event) {
		int route = determineRoute(event);

		// if a route was determined
		if (route != -1) {
			// remember the route choice for all persons inside the vehicle
			for (Id<Person> occupantId : vehicle2PersonsMap.get(event.getVehicleId())){
				if (this.personRouteChoice.containsKey(occupantId))
					throw new IllegalStateException("Person " + occupantId + " was seen at least twice on a route specific link."
						+ " Did it travel more than once without arrival?");

				this.personRouteChoice.put(occupantId, route);
			}
		}
	}

	/**
	 * Determines the vehicles route choice if it is unique.
	 * 
	 * @return the route id (counts from 0 to numberOfRoutes)
	 */
	protected abstract int determineRoute(LinkEnterEvent linkEnterEvent);
	
	
	/**
	 * Calculates the total travel time and the route travel time of the agent.
	 * 
	 * Fills all fields and maps with the person specific route and travel time
	 * informations.
	 */
	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (!this.personDepartureTimes.containsKey(event.getPersonId())) {
			throw new IllegalStateException(
					"Person " + event.getPersonId() + " has arrived without departure.");
		}
		if (!this.personRouteChoice.containsKey(event.getPersonId())) {
			throw new IllegalStateException(
					"Person " + event.getPersonId() + " arrived, but was not seen on any route.");
		}
		Vehicle vehicle = vehicles.getVehicles().get(person2VehicleMap.get(event.getPersonId()));
		double vehPCU = vehicle.getType().getPcuEquivalents();

		// calculate total travel time
		double personArrivalTime = event.getTime();
		double personDepartureTime = this.personDepartureTimes.get(event.getPersonId());
		double personTotalTT = personArrivalTime - personDepartureTime;
		this.totalTT += personTotalTT;
		double personTotalToll = this.personTolls.containsKey(event.getPersonId())? this.personTolls.get(event.getPersonId()) : 0.0;

		// store route specific information
		int personRoute = this.personRouteChoice.get(event.getPersonId());
		this.totalRouteTTs[personRoute] += personTotalTT;
		this.routeUsers_PCU[personRoute]+= vehPCU;
		this.totalRouteTolls[personRoute] += personTotalToll;

		// fill maps for calculating avg tt per route
		if (!this.totalRouteTTsByDepartureTime.containsKey(personDepartureTime)) {
			// this is equivalent to
			// !this.routeUsersPerDepartureTime.containsKey(personDepartureTime)
			// and
			// !this.totalRouteTollsByDepartureTime.containsKey(personDepartureTime)
			this.totalRouteTTsByDepartureTime.put(personDepartureTime, new double[numberOfRoutes]);
			this.numberOfRouteUsersByDepartureTime.put(personDepartureTime, new int[numberOfRoutes]);
			this.totalRouteTollsByDepartureTime.put(personDepartureTime, new double[numberOfRoutes]);
		}
		this.totalRouteTTsByDepartureTime.get(personDepartureTime)[personRoute] += personTotalTT;
		this.numberOfRouteUsersByDepartureTime.get(personDepartureTime)[personRoute]++;
		this.totalRouteTollsByDepartureTime.get(personDepartureTime)[personRoute] += personTotalToll;

		// increase the number of persons on route for each second the
		// person is traveling on it
		for (int i = 0; i < personTotalTT; i++) {
			if (!this.onRoutePerSecond_PCU.containsKey(personDepartureTime + i)) {
				this.onRoutePerSecond_PCU.put(personDepartureTime + i, new double[numberOfRoutes]);
			}
			this.onRoutePerSecond_PCU.get(personDepartureTime + i)[this.personRouteChoice
					.get(event.getPersonId())]+= vehPCU;
		}

		// add one route start for the specific departure time
		if (!this.routeStartsPerTimeStep_PCU.containsKey(personDepartureTime)) {
			this.routeStartsPerTimeStep_PCU.put(personDepartureTime, new double[numberOfRoutes]);
		}
		this.routeStartsPerTimeStep_PCU.get(personDepartureTime)[personRoute]+= vehPCU;

		// remove all trip dependent information of the arrived person
		this.personDepartureTimes.remove(event.getPersonId());
		this.personRouteChoice.remove(event.getPersonId());
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		log.warn("Agent " + event.getPersonId() + " stucked on link " + event.getLinkId());
		if (stuckedAgents_PCU == 0){
			log.warn("This handler counts stucked agents but doesn't consider its travel times or route choice.");
		}
		
		this.personDepartureTimes.remove(event.getPersonId());
		this.personRouteChoice.remove(event.getPersonId());
		
		Vehicle vehicle = vehicles.getVehicles().get(person2VehicleMap.get(event.getPersonId()));
		double vehPCU = vehicle.getType().getPcuEquivalents();

		stuckedAgents_PCU+= vehPCU;
	}
	
	@Override
	public void handleEvent(PersonMoneyEvent event) {
		if (!this.personTolls.containsKey(event.getPersonId())) {
			this.personTolls.put(event.getPersonId(), 0.);
		}

		// assume a negative toll
		if (event.getAmount() > 0){
			log.error("An agent got money instead of paying tolls. The toll analyzer does not support this option.");
		}
		
		// add the current toll to the previous tolls of this person
		this.personTolls.put(event.getPersonId(), this.personTolls.get(event.getPersonId()) - event.getAmount());
	}

	/**
	 * Calculates and returns the average travel times on the single routes in
	 * Braess' example. The first entry corresponds to the upper route, the
	 * second to the middle route and the third to the lower route.
	 * 
	 * @return average travel times
	 */
	public double[] calculateAvgRouteTTs() {
		double[] avgRouteTTs = new double[numberOfRoutes];
		for (int i = 0; i < numberOfRoutes; i++) {
			if (this.routeUsers_PCU[i] == 0)
				avgRouteTTs[i] = Double.NaN;
			else
				avgRouteTTs[i] = this.totalRouteTTs[i] / this.routeUsers_PCU[i];
		}
		return avgRouteTTs;
	}
	
	/**
	 * Calculates and returns the average tolls paid on the single routes in
	 * Braess' example. The first entry corresponds to the upper route, the
	 * second to the middle route and the third to the lower route.
	 * 
	 * @return average tolls
	 */
	public double[] calculateAvgRouteTolls() {
		double[] avgRouteTolls = new double[numberOfRoutes];
		for (int i = 0; i < numberOfRoutes; i++) {
			if (this.routeUsers_PCU[i] == 0)
				avgRouteTolls[i] = Double.NaN;
			else
				avgRouteTolls[i] = this.totalRouteTolls[i] / this.routeUsers_PCU[i];
		}
		return avgRouteTolls;
	}

	public double getTotalTT() {
		return totalTT;
	}

	public double[] getTotalRouteTTs() {
		// return NaN for routes where no agent has traveled
		for (int i = 0; i < numberOfRoutes; i++){
			if (routeUsers_PCU[i] == 0)
				totalRouteTTs[i] = Double.NaN;
		}
		return totalRouteTTs;
	}

	public double[] getRouteUsers_PCU() {
		return routeUsers_PCU;
	}
	
	public double[] getTotalRouteTolls() {
		// return NaN for routes where no agent has traveled
		for (int i = 0; i < numberOfRoutes; i++){
			if (routeUsers_PCU[i] == 0)
				totalRouteTolls[i] = Double.NaN;
		}
		return totalRouteTolls;
	}

	/**
	 * @return a map containing the number of route starts for each time step.
	 * Thereby a route start is the departure event of an agent using this route.
	 */
	public Map<Double, double[]> getRouteDeparturesPerTimeStep_PCU() {
		// determine minimum and maximum departure time
		Tuple<Double, Double> firstLastDepartureTuple = determineMinMaxDoubleInSet(this.routeStartsPerTimeStep_PCU.keySet());
		
		// fill missing time steps between first and last departure with zero starts
		// note: with time steps != 1, matsim departure times need not be integer values
		double time = firstLastDepartureTuple.getFirst();
		
		while (time <= firstLastDepartureTuple.getSecond()) {
			if (!this.routeStartsPerTimeStep_PCU.containsKey(time)) {
				this.routeStartsPerTimeStep_PCU.put(time, new double[numberOfRoutes]);
			}
			time += timeStepSize;
		}

		return routeStartsPerTimeStep_PCU;
	}
	
	/**
	 * @param set
	 * @return minimum and maximum entry of the set of doubles
	 */
	public static Tuple<Double, Double> determineMinMaxDoubleInSet(Set<Double> set) {
		double minEntry = Long.MAX_VALUE;
		double maxEntry = Long.MIN_VALUE;
		for (Double currentEntry : set) {
			if (currentEntry < minEntry)
				minEntry = currentEntry;
			if (currentEntry > maxEntry)
				maxEntry = currentEntry;
		}
		return new Tuple<Double, Double>(minEntry, maxEntry);
	}

	/** 
	 * @return a map containing the number of bygone route starts for each time step.
	 * Thereby a route start is the departure event of an agent using this route.
	 * For each time step all bygone route starts are summed up.
	 */
	public Map<Double, double[]> calculateSummedRouteDeparturesPerTimeStep_PCU(){
		
		// determine minimum and maximum departure time
		Tuple<Double, Double> firstLastDepartureTuple = determineMinMaxDoubleInSet(this.routeStartsPerTimeStep_PCU.keySet());
		
		Map<Double, double[]> summedRouteDeparturesPerTimeStep_PCU = new TreeMap<>();
		
		// create a map entry for each time step between minimum and maximum departure time
		// note: with time step size != 1, matsim departure times need not be integer values
		double time = firstLastDepartureTuple.getFirst();
		double[] summedDepPreviousTimeStep = new double[numberOfRoutes];
		
		while (time <= firstLastDepartureTuple.getSecond()) {
			
			// initialize departure array as {0,...,0}
			summedRouteDeparturesPerTimeStep_PCU.put(time, new double[numberOfRoutes]);
			for (int i = 0; i < numberOfRoutes; i++){
				// add value from the previous second
				summedRouteDeparturesPerTimeStep_PCU.get(time)[i] += summedDepPreviousTimeStep[i];
				// increment for every departure in this second
				if (this.routeStartsPerTimeStep_PCU.containsKey(time)) {
					summedRouteDeparturesPerTimeStep_PCU.get(time)[i] += 
							this.routeStartsPerTimeStep_PCU.get(time)[i];
				}
			}		
			
			summedDepPreviousTimeStep = summedRouteDeparturesPerTimeStep_PCU.get(time);
			time += timeStepSize;
		}
		
		return summedRouteDeparturesPerTimeStep_PCU;
	}

	/**
	 * @return the number of agents on route (between departure and arrival
	 * event) per time step.
	 */
	public Map<Double, double[]> getOnRoutePerSecond_PCU() {
		// already contains entries for all time steps (seconds)
		// between first departure and last arrival
		return onRoutePerSecond_PCU;
	}

	/**
	 * @return the average route travel times by departure time.
	 * 
	 * Thereby the double array in each map entry contains the average
	 * route travel times for all different routes in the network
	 * (always for agents with the specific departure time)
	 */
	public Map<Double, double[]> calculateAvgRouteTTsByDepartureTime() {
		Map<Double, double[]> avgTTsPerRouteByDepartureTime = new TreeMap<>();

		// calculate average route travel times for existing departure times
		for (Double departureTime : this.totalRouteTTsByDepartureTime.keySet()) {
			double[] totalTTsPerRoute = this.totalRouteTTsByDepartureTime
					.get(departureTime);
			int[] usersPerRoute = this.numberOfRouteUsersByDepartureTime
					.get(departureTime);
			double[] avgTTsPerRoute = new double[numberOfRoutes];
			for (int i = 0; i < numberOfRoutes; i++) {
				if (usersPerRoute[i] == 0)
					// no agent is departing for the specific route at this time
					avgTTsPerRoute[i] = Double.NaN;
				else
					avgTTsPerRoute[i] = totalTTsPerRoute[i] / usersPerRoute[i];
			}
			avgTTsPerRouteByDepartureTime.put(departureTime, avgTTsPerRoute);
		}

		// fill missing time steps between first and last departure
		Tuple<Double, Double> firstLastDepartureTuple = determineMinMaxDoubleInSet(this.routeStartsPerTimeStep_PCU.keySet());
		// create a map entry for each time step between minimum and maximum departure time
		// note: with time step size != 1, matsim departure times need not be integer values
		double time = firstLastDepartureTuple.getFirst();
				
		while (time <= firstLastDepartureTuple.getSecond()) {
			if (!avgTTsPerRouteByDepartureTime.containsKey(time)) {
				// add NaN-values as travel times when no agent departures
				double[] nanTTsPerRoute = new double[numberOfRoutes];
				for (int i = 0; i < numberOfRoutes; i++){
					nanTTsPerRoute[i] = Double.NaN;
				}
				avgTTsPerRouteByDepartureTime.put(time, nanTTsPerRoute);
			}
			time += timeStepSize;
		}

		return avgTTsPerRouteByDepartureTime;
	}
	
	/**
	 * @return the average route tolls by departure time.
	 * 
	 * Thereby the double array in each map entry contains the average
	 * route toll for all different routes in the network
	 * (always for agents with the specific departure time)
	 */
	public Map<Double, double[]> calculateAvgRouteTollsByDepartureTime() {
		Map<Double, double[]> avgTollsPerRouteByDepartureTime = new TreeMap<>();

		// calculate average route tolls for existing departure times
		for (Double departureTime : this.totalRouteTollsByDepartureTime.keySet()) {
			double[] totalTollsPerRoute = this.totalRouteTollsByDepartureTime
					.get(departureTime);
			int[] usersPerRoute = this.numberOfRouteUsersByDepartureTime
					.get(departureTime);
			double[] avgTollPerRoute = new double[numberOfRoutes];
			for (int i = 0; i < numberOfRoutes; i++) {
				if (usersPerRoute[i] == 0)
					// no agent is departing for the specific route at this time
					avgTollPerRoute[i] = Double.NaN;
				else
					avgTollPerRoute[i] = totalTollsPerRoute[i] / usersPerRoute[i];
			}
			avgTollsPerRouteByDepartureTime.put(departureTime, avgTollPerRoute);
		}

		// fill missing time steps between first and last departure
		Tuple<Double, Double> firstLastDepartureTuple = determineMinMaxDoubleInSet(this.routeStartsPerTimeStep_PCU.keySet());
		// create a map entry for each time step between minimum and maximum departure time
		// note: with time step size != 1, matsim departure times need not be integer values
		double time = firstLastDepartureTuple.getFirst();
						
		while (time <= firstLastDepartureTuple.getSecond()) {
			if (!avgTollsPerRouteByDepartureTime.containsKey(time)) {
				// add NaN-values as tolls when no agent departures
				double[] nanTollsPerRoute = new double[numberOfRoutes];
				for (int i = 0; i < numberOfRoutes; i++){
					nanTollsPerRoute[i] = Double.NaN;
				}
				avgTollsPerRouteByDepartureTime.put(time, nanTollsPerRoute);
			}
			time += timeStepSize;
		}

		return avgTollsPerRouteByDepartureTime;
	}

	public double getStuckedAgentsInPCU() {
		return stuckedAgents_PCU;
	}

	public void setNumberOfRoutes(int numberOfRoutes) {
		this.numberOfRoutes = numberOfRoutes;
	}

	public int getNumberOfRoutes() {
		return numberOfRoutes;
	}
}
