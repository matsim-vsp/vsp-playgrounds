/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.ikaddoura.savPricing.runSetupB.prepare;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.prepare.BerlinShpUtils;

/**
* @author ikaddoura
*/

public class BerlinPlansModificationSplitTrips {
	private static final Logger log = Logger.getLogger(BerlinPlansModificationSplitTrips.class);
	
	private final List<Coord> prCoordinatesS;
	private final BerlinShpUtils shpUtils;

	private final String inputPersonAttributesSubpopulationPerson;
	private final String modeToReplaceCarTripsInBerlin;
	private final String modeToReplaceCarTripsInBrandenburg;
	private final String modeToReplaceCarTripsToFromBerlin;

	private final StageActivityTypes stageActivities;
	private final String parkAndRideActivity;
	private final double parkAndRideDuration;
	private final boolean splitTripsS;
	private final boolean splitTripsTaxi;

	public BerlinPlansModificationSplitTrips(
			String transitStopCoordinatesSFile,
			BerlinShpUtils shpUtils,
			String inputPersonAttributesSubpopulationPerson,
			String modeToReplaceCarTripsInBerlin,
			String modeToReplaceCarTripsInBrandenburg,
			String modeToReplaceCarTripsToFromBerlin,
			StageActivityTypes stageActivities,
			String parkAndRideActivity,
			double parkAndRideDuration,
			boolean splitTripsS,
			boolean splitTripsTaxi) {
		
		this.prCoordinatesS = readCSVFile(transitStopCoordinatesSFile);
		this.shpUtils = shpUtils;
		
		this.inputPersonAttributesSubpopulationPerson = inputPersonAttributesSubpopulationPerson;
		this.modeToReplaceCarTripsInBerlin = modeToReplaceCarTripsInBerlin;
		this.modeToReplaceCarTripsInBrandenburg = modeToReplaceCarTripsInBrandenburg;
		this.modeToReplaceCarTripsToFromBerlin = modeToReplaceCarTripsToFromBerlin;
		this.stageActivities = stageActivities;
		this.parkAndRideActivity = parkAndRideActivity;
		this.parkAndRideDuration = parkAndRideDuration;
		this.splitTripsS = splitTripsS;
		this.splitTripsTaxi = splitTripsTaxi;
	}

	public void run(Scenario scenario) {
		log.info("Number of persons before adjusting plans: " + scenario.getPopulation().getPersons().size());
		splitTrips(scenario);
		
		// Delete all link information and (hopefully) do everything via the coordinates...
		for (Person person : scenario.getPopulation().getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				for (PlanElement pE : plan.getPlanElements()) {
					if (pE instanceof Activity) {
						Activity act = (Activity) pE;
						act.setLinkId(null);
					}
				}
			}
		}
		
		log.info("Number of persons after adjusting plans: " + scenario.getPopulation().getPersons().size());
	}
	
	private void splitTrips(Scenario scenario) {
		
		log.info("Adjusting plans...");
		
		int tripsBBtoBER = 0;
		int tripsBERtoBB = 0;
		int tripsBBtoBB = 0;
		int tripsBERtoBER = 0;
		int tripsCar = 0;
		int tripsTotal = 0;

		int counter = 0;
		for (Person person : scenario.getPopulation().getPersons().values()) {
			if (counter % 10000 == 0)
				log.info("person # " + counter);
			counter++;

			if (person.getPlans().size() > 1)
				throw new RuntimeException("More than one plan per person. Aborting...");

			if (scenario.getPopulation().getPersonAttributes().getAttribute(person.getId().toString(), scenario.getConfig().plans().getSubpopulationAttributeName()).equals(inputPersonAttributesSubpopulationPerson)) {

				PopulationFactory factory = scenario.getPopulation().getFactory();

				Plan directTripPlan = factory.createPlan(); // direct trips
				Plan splitTripPlanPT1 = factory.createPlan(); // with split car trips (car + S)
				Plan splitTripPlanCar = factory.createPlan(); // with split car trips (car + DRT)

				// add first activity
				directTripPlan.addActivity((Activity) person.getSelectedPlan().getPlanElements().get(0));
				splitTripPlanPT1.addActivity((Activity) person.getSelectedPlan().getPlanElements().get(0));
				splitTripPlanCar.addActivity((Activity) person.getSelectedPlan().getPlanElements().get(0));

				for (Trip trip : TripStructureUtils.getTrips(person.getSelectedPlan().getPlanElements(),
						stageActivities)) {

					tripsTotal++;

					String mainMode = new MainModeIdentifierImpl().identifyMainMode(trip.getTripElements());
					
					if (mainMode.equals(TransportMode.car)) {
						
						tripsCar++;
						
						if (shpUtils.isCoordInCarRestrictedArea(trip.getOriginActivity().getCoord())
								&& shpUtils.isCoordInCarRestrictedArea(trip.getDestinationActivity().getCoord())) {
							// car-restricted area --> car-restricted area
							tripsBERtoBER++;

							String berlinTripMode = modeToReplaceCarTripsInBerlin;
							
							directTripPlan.addLeg(factory.createLeg(berlinTripMode));
							directTripPlan.addActivity(trip.getDestinationActivity());

							splitTripPlanPT1.addLeg(factory.createLeg(berlinTripMode));
							splitTripPlanPT1.addActivity(trip.getDestinationActivity());

							splitTripPlanCar.addLeg(factory.createLeg(berlinTripMode));
							splitTripPlanCar.addActivity(trip.getDestinationActivity());

						} else if (shpUtils.isCoordInCarRestrictedArea(trip.getOriginActivity().getCoord())
								&& !shpUtils.isCoordInCarRestrictedArea(trip.getDestinationActivity().getCoord())) {
							// car-restricted area --> brandenburg
							tripsBERtoBB++;

							// only split trips for former car trips
							directTripPlan.addLeg(factory.createLeg(modeToReplaceCarTripsToFromBerlin));
							directTripPlan.addActivity(trip.getDestinationActivity());

							splitTripPlanPT1.addLeg(factory.createLeg(modeToReplaceCarTripsToFromBerlin));
							Activity prActivity1 = factory.createActivityFromCoord(parkAndRideActivity, getNearestCoord(trip.getOriginActivity().getCoord(), prCoordinatesS));
							prActivity1.setMaximumDuration(parkAndRideDuration);
							splitTripPlanPT1.addActivity(prActivity1);
							splitTripPlanPT1.addLeg(factory.createLeg(modeToReplaceCarTripsInBrandenburg));
							splitTripPlanPT1.addActivity(trip.getDestinationActivity());

							splitTripPlanCar.addLeg(factory.createLeg(modeToReplaceCarTripsInBerlin));
							Coord firstCarLink = getFirstCarLinkFromPreviousRoute(trip, scenario.getNetwork());
							if (firstCarLink != null) {
								Activity prActivity3 = factory.createActivityFromCoord(parkAndRideActivity, firstCarLink);
								prActivity3.setMaximumDuration(parkAndRideDuration);
								splitTripPlanCar.addActivity(prActivity3);
								splitTripPlanCar.addLeg(factory.createLeg(modeToReplaceCarTripsInBrandenburg));
							} else {
								throw new RuntimeException("couldn't find car link. Aborting...");
							}
							splitTripPlanCar.addActivity(trip.getDestinationActivity());

						} else if (!shpUtils.isCoordInCarRestrictedArea(trip.getOriginActivity().getCoord())
								&& shpUtils.isCoordInCarRestrictedArea(trip.getDestinationActivity().getCoord())) {
							// brandenburg --> car-restricted area
							tripsBBtoBER++;

							directTripPlan.addLeg(factory.createLeg(modeToReplaceCarTripsToFromBerlin));
							directTripPlan.addActivity(trip.getDestinationActivity());

							splitTripPlanPT1.addLeg(factory.createLeg(modeToReplaceCarTripsInBrandenburg));
							Activity prActivity1 = factory.createActivityFromCoord(parkAndRideActivity, getNearestCoord(trip.getOriginActivity().getCoord(), prCoordinatesS));
							prActivity1.setMaximumDuration(parkAndRideDuration);
							splitTripPlanPT1.addActivity(prActivity1);
							splitTripPlanPT1.addLeg(factory.createLeg(modeToReplaceCarTripsToFromBerlin));
							splitTripPlanPT1.addActivity(trip.getDestinationActivity());

							Coord lastCarLink = getLastCarLinkFromPreviousRoute(trip, scenario.getNetwork());
							if (lastCarLink != null) {
								splitTripPlanCar.addLeg(factory.createLeg(modeToReplaceCarTripsInBrandenburg));
								Activity prActivity3 = factory.createActivityFromCoord(parkAndRideActivity, lastCarLink);
								prActivity3.setMaximumDuration(parkAndRideDuration);
								splitTripPlanCar.addActivity(prActivity3);
							} else {
								throw new RuntimeException("couldn't find car link. Aborting...");
							}
							splitTripPlanCar.addLeg(factory.createLeg(modeToReplaceCarTripsInBerlin));
							splitTripPlanCar.addActivity(trip.getDestinationActivity());
							
						} else if (!shpUtils.isCoordInCarRestrictedArea(trip.getOriginActivity().getCoord())
								&& !shpUtils.isCoordInCarRestrictedArea(trip.getDestinationActivity().getCoord())) {
							// brandenburg --> brandenburg
							tripsBBtoBB++;
							
							directTripPlan.addLeg(factory.createLeg(modeToReplaceCarTripsInBrandenburg));
							directTripPlan.addActivity(trip.getDestinationActivity());

							splitTripPlanPT1.addLeg(factory.createLeg(modeToReplaceCarTripsInBrandenburg));
							splitTripPlanPT1.addActivity(trip.getDestinationActivity());

							splitTripPlanCar.addLeg(factory.createLeg(modeToReplaceCarTripsInBrandenburg));
							splitTripPlanCar.addActivity(trip.getDestinationActivity());

						} else {
							throw new RuntimeException("Aborting...");
						}
					
					} else {
						// mainMode is not car
						directTripPlan.addLeg(factory.createLeg(mainMode));
						directTripPlan.addActivity(trip.getDestinationActivity());

						splitTripPlanPT1.addLeg(factory.createLeg(mainMode));
						splitTripPlanPT1.addActivity(trip.getDestinationActivity());

						splitTripPlanCar.addLeg(factory.createLeg(mainMode));
						splitTripPlanCar.addActivity(trip.getDestinationActivity());
					}
				}

				person.removePlan(person.getSelectedPlan());

				if (splitTripsS)
					person.addPlan(splitTripPlanPT1);
				if (splitTripsTaxi)
					person.addPlan(splitTripPlanCar);
				person.addPlan(directTripPlan);
			}
		}
		log.info("Adjusting plans... Done.");

		log.info("Car trips BB to Car-restricted area: " + tripsBBtoBER);
		log.info("Car trips Car-restricted area to BB: " + tripsBERtoBB);
		log.info("Car trips BB to BB: " + tripsBBtoBB);
		log.info("Car trips Car-restricted area to Car-restricted area: " + tripsBERtoBER);
		log.info("Car trips TOTAL: " + tripsCar);
		log.info("Trips TOTAL: " + tripsTotal);
	}

	private Coord getFirstCarLinkFromPreviousRoute(Trip trip, Network network) {

		NetworkRoute networkRoute = getNetworkRoute(trip, network);
		if (networkRoute != null) {
			// car-trip
			Link firstCarLinkWithAllowedCarMode = null;

			for (Id<Link> linkId : networkRoute.getLinkIds()) {
				if (network.getLinks().get(linkId).getAllowedModes().contains(modeToReplaceCarTripsInBrandenburg)) {
					firstCarLinkWithAllowedCarMode = network.getLinks().get(linkId);
					return firstCarLinkWithAllowedCarMode.getFromNode().getCoord();
				}
			}

			return network.getLinks().get(networkRoute.getStartLinkId()).getFromNode().getCoord();

		} else {
			// non-car trip
			return null;
		}
	}

	private Coord getLastCarLinkFromPreviousRoute(Trip trip, Network network) {

		NetworkRoute networkRoute = getNetworkRoute(trip, network);
		if (networkRoute != null) {
			// car-trip
			Link lastCarLinkWithAllowedCarMode = null;

			for (Id<Link> linkId : networkRoute.getLinkIds()) {
				if (network.getLinks().get(linkId).getAllowedModes().contains(modeToReplaceCarTripsInBrandenburg)) {
					lastCarLinkWithAllowedCarMode = network.getLinks().get(linkId);
				}
			}

			if (lastCarLinkWithAllowedCarMode != null) {
				return lastCarLinkWithAllowedCarMode.getToNode().getCoord();
			} else {
				return network.getLinks().get(networkRoute.getEndLinkId()).getToNode().getCoord();
			}

		} else {
			// non-car trip
			return null;
		}
	}

	private NetworkRoute getNetworkRoute(Trip trip, Network network) {
		NetworkRoute networkRoute = null;
		for (Leg leg : trip.getLegsOnly()) {

			if (leg.getMode().equals(TransportMode.car)) {
				if (networkRoute == null) {
					networkRoute = (NetworkRoute) leg.getRoute();
				} else {
					throw new RuntimeException("More than one car leg during a single trip. Aborting...");
				}
			}
		}
		return networkRoute;
	}

	private Coord getNearestCoord(Coord coordOrigin, List<Coord> prCoordinatesS2) {

		double minDistance = Double.MAX_VALUE;
		Coord minDistanceCoord = null;
		for (Coord coord : prCoordinatesS2) {
			double distance = NetworkUtils.getEuclideanDistance(coordOrigin, coord);
			if (distance < minDistance) {
				minDistance = distance;
				minDistanceCoord = coord;
			}
		}

		return minDistanceCoord;
	}
	
	private List<Coord> readCSVFile(String transitStopCoordinatesFile) {

		List<Coord> coordinates = new ArrayList<>();

		BufferedReader br = IOUtils.getBufferedReader(transitStopCoordinatesFile);
		log.info("Reading coordinates csv file...");
		String line = null;
		try {
			while ((line = br.readLine()) != null) {
				String[] columns = line.split(";");
				Coord coord = new Coord(Double.parseDouble(columns[0]), Double.parseDouble(columns[1]));
				log.info("Adding coordinate link ID " + coord.toString());
				coordinates.add(coord);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		log.info("Reading coordinates csv file... Done.");
		return coordinates;
	}

}

