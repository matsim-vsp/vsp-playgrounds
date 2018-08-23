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

package playground.ikaddoura.optAV.prepare;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
* @author ikaddoura
*/

public class TagCarAndSAVusers {

	private static final Logger log = Logger.getLogger(TagCarAndSAVusers.class);
	
	private final String inputDirectory = "/Users/ihab/Documents/workspace/runs-svn/b5_optAV_congestion/input/population/";
	private final String outputDirectory = "/Users/ihab/Documents/workspace/runs-svn/b5_optAV_congestion/input/population_test/";
	
	private final String inputPlansFile = "berlin-v5.1-1pct.plans.xml.gz";
	private final String outputPlansFile = "berlin-5.1-1pct_plans_taggedCarUsers.xml.gz";

	private final String outputPersonAttributesFile = "berlin-5.0-1pct_person-attributes_potentialSAVusers.xml.gz";

	private final String areaOfPotentialSAVusersSHPFile = "/Users/ihab/Documents/workspace/shared-svn/projects/audi_av/shp/untersuchungsraumAll.shp";
	private final String crsSHPFile = "EPSG:25833";
	private final String crsPopulation = TransformationFactory.DHDN_GK4;

	// Optional: provide input person attributes file...
	private final String inputPersonAttributesFile = "berlin-v5.0.person-attributes.xml.gz";
	private final String inputPersonAttributesSubpopulationPerson = "person";
	
	// Optional: change trip mode for all potential SAV users
	private final boolean changeCar2TaxiTripsForAllPotentialSAVusers = true;
	private final String outputPlansFileChangeTripMode = "berlin-5.1-1pct_plans_taggedCarUsers_berlin-population-taxi.xml.gz";
	
	// Optional: split trips from/to outside of Berlin into two trips (outside = car; inside = pt)
	private final boolean splitTripsForAllTripsCrossingTheBerlinBorder = true;
	private final String outputPlansFileChangeTripModeSplitTrips = "berlin-5.1-1pct_plans_taggedCarUsers_berlin-population-taxi_splitTrips.xml.gz";
	private final Coord[] prCoordinates = {
			new Coord(4594046.225912675, 5836589.393558677), // S Mühlenbeck-Mönchmühle
			new Coord(4596787.252510464, 5836430.391383448), // Schönerlinde
			new Coord(4602589.292594807, 5836203.410669737), // S Röntgental
			new Coord(4606283.312046832, 5827655.318362243), // S Ahrensfelde
			new Coord(4611006.318080213, 5824035.417242844), // U Hönow
			new Coord(4612016.365326108, 5821570.320163683), // S Birkenstein
			new Coord(4619288.439198637, 5812016.2948688), // S Erkner
			new Coord(4610059.579579681, 5805505.010516966), // S Eichwalde
			new Coord(4603139.379672928, 5807465.218550463), // S Schönefeld
			new Coord(4596075.353004076, 5803913.1885673115), // S Mahlow
			new Coord(4588581.255512315, 5806921.197011784), // S Teltow
			new Coord(4586956.237755206, 5807860.2450342635), // S Teltow-Stadt
			new Coord(4580318.237931468, 5810453.194058485), // S Wannsee
			new Coord(4581359.229151482, 5823085.281743402), // S U Spandau
//			new Coord(4577580.099560545, 5823326.95671485), // S Staaken
//			new Coord(4576685.154127585, 5824640.341833886), // Albrechtshof
			new Coord(4581737.18532087, 5834563.406155014), // S Hennigsdorf
			new Coord(4587412.176538966, 5834115.416970312), // S Frohnau
			};
	private final String parkAndRideActivity = "park-and-ride";
	private final double parkAndRideDuration = 60.;
	
	// ####################################################################

	private Scenario scenario;
	
	private int potentialSAVusers = 0;
	private int noPotentialSAVusers = 0;
	private int carUsers = 0;
	private int noCarUsers = 0;
	
	private final Map<Integer, Geometry> zoneId2geometry = new HashMap<Integer, Geometry>();
	private final CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(crsPopulation, crsSHPFile);
	
	public static void main(String[] args) {			
				
		TagCarAndSAVusers generateAVDemand = new TagCarAndSAVusers();
		generateAVDemand.run();
	}

	private void run() {
				
		Config config = ConfigUtils.createConfig();
		config.plans().setInputFile(inputDirectory + inputPlansFile);
		if (inputPersonAttributesFile != null) config.plans().setInputPersonAttributeFile(inputDirectory + inputPersonAttributesFile);
		scenario = ScenarioUtils.loadScenario(config);
		
		tagCarUsers();
		loadShapeFile();
		tagPotentialSAVusers();
		
		log.info("Potential SAV users: " + potentialSAVusers);
		log.info("No potential SAV users: " + noPotentialSAVusers);
		log.info("Car users: " + carUsers);
		log.info("No car users: " + noCarUsers);
		
		new PopulationWriter(scenario.getPopulation()).write(outputDirectory + outputPlansFile);
		new ObjectAttributesXmlWriter(scenario.getPopulation().getPersonAttributes()).writeFile(outputDirectory + outputPersonAttributesFile);
		
		if (changeCar2TaxiTripsForAllPotentialSAVusers) new PopulationWriter(changeTripModes("person_potentialSAVuser", TransportMode.car, TransportMode.taxi)).write(outputDirectory + outputPlansFileChangeTripMode);;

		if (splitTripsForAllTripsCrossingTheBerlinBorder) new PopulationWriter(splitTrips("person_noPotentialSAVuser")).write(outputDirectory + outputPlansFileChangeTripModeSplitTrips);;

	}

	private void loadShapeFile() {		
		Collection<SimpleFeature> features;
		features = ShapeFileReader.getAllFeatures(areaOfPotentialSAVusersSHPFile);
		int featureCounter = 0;
		for (SimpleFeature feature : features) {
			zoneId2geometry.put(featureCounter, (Geometry) feature.getDefaultGeometry());
			featureCounter++;
		}
	}

	private Population splitTrips(String subpopulation) {
				
		log.info("Splitting border-crossing trips for subpopulation " + subpopulation + " and setting inner-city car trips to car...");
		final String[] attributes = {"CarOwnerInBaseCase"};

		Scenario scenarioOutput = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
		Population populationOutput = scenarioOutput.getPopulation();
		
		for (Person person : scenario.getPopulation().getPersons().values()) {
			
			if (person.getPlans().size() > 1) throw new RuntimeException("More than one plan per person. Aborting...");
			
			PopulationFactory factory = populationOutput.getFactory();
			Person personClone = factory.createPerson(person.getId());
			for (String attribute : attributes) {
				personClone.getAttributes().putAttribute(attribute, person.getAttributes().getAttribute(attribute));
			}
			populationOutput.addPerson(personClone);
		
			if (this.scenario.getPopulation().getPersonAttributes().getAttribute(person.getId().toString(), scenario.getConfig().plans().getSubpopulationAttributeName()).equals(subpopulation)) {
		
				boolean addUnModifiedPlan = true;
				
				Plan plan1 = factory.createPlan();
				Plan plan2 = factory.createPlan();

				// add first activity
				plan1.addActivity((Activity) person.getSelectedPlan().getPlanElements().get(0));
				plan2.addActivity((Activity) person.getSelectedPlan().getPlanElements().get(0));

				StageActivityTypes stageActivities = new StageActivityTypesImpl("pt interaction", "car interaction");
				for (Trip trip : TripStructureUtils.getTrips(person.getSelectedPlan().getPlanElements(), stageActivities )) {
					
					String mainMode = new MainModeIdentifierImpl().identifyMainMode(trip.getTripElements());
								
					if (isActivityInArea(trip.getOriginActivity()) && isActivityInArea(trip.getDestinationActivity())) {
						// change from car to pt
						if (mainMode.equals(TransportMode.car)) {
							
							addUnModifiedPlan = false;

							plan1.addLeg(factory.createLeg(TransportMode.pt));
							plan1.addActivity(trip.getDestinationActivity());
							
							plan2.addLeg(factory.createLeg(TransportMode.pt));
							plan2.addActivity(trip.getDestinationActivity());
						
						} else {
							plan1.addLeg(factory.createLeg(mainMode));
							plan1.addActivity(trip.getDestinationActivity());
						}
						
					} else if (isActivityInArea(trip.getOriginActivity()) && !isActivityInArea(trip.getDestinationActivity())) {
						// from inside Berlin to outside Berlin --> split trip
						addUnModifiedPlan = false;
						
						plan1.addLeg(factory.createLeg(TransportMode.pt));
						Activity prActivity = factory.createActivityFromCoord(parkAndRideActivity, getPlausiblePRCoord(trip.getOriginActivity().getCoord(), trip.getDestinationActivity().getCoord()));
						prActivity.setMaximumDuration(parkAndRideDuration);
						plan1.addActivity(prActivity);
						
						plan1.addLeg(factory.createLeg(mainMode));
						plan1.addActivity(trip.getDestinationActivity());
						
						plan2.addLeg(factory.createLeg(TransportMode.pt));
						plan2.addActivity(trip.getDestinationActivity());
						
					} else if (!isActivityInArea(trip.getOriginActivity()) && isActivityInArea(trip.getDestinationActivity())) {
						// from outside Berlin to inside Berlin --> split trip
						addUnModifiedPlan = false;
						
						plan1.addLeg(factory.createLeg(mainMode));
						Activity prActivity = factory.createActivityFromCoord(parkAndRideActivity, getPlausiblePRCoord(trip.getOriginActivity().getCoord(), trip.getDestinationActivity().getCoord()));
						prActivity.setMaximumDuration(parkAndRideDuration);
						plan1.addActivity(prActivity);
						
						plan1.addLeg(factory.createLeg(TransportMode.pt));
						plan1.addActivity(trip.getDestinationActivity());
						
						plan2.addLeg(factory.createLeg(TransportMode.pt));
						plan2.addActivity(trip.getDestinationActivity());
						
					} else if (!isActivityInArea(trip.getOriginActivity()) && !isActivityInArea(trip.getDestinationActivity())){
						// do not change the mode (car outside the city boundaries is still allowed)
						for (Leg leg : trip.getLegsOnly()) {
							plan1.addLeg(leg);
							plan2.addLeg(leg);
						}
						plan1.addActivity(trip.getDestinationActivity());
						plan2.addActivity(trip.getDestinationActivity());

					} else {
						throw new RuntimeException("Aborting...");
					}
				}
				
				if (addUnModifiedPlan) {
					// add unmodified plan
					personClone.addPlan(person.getSelectedPlan());
				} else {
					// add modified plan
					personClone.addPlan(plan1);
					personClone.addPlan(plan2);
				}
				
			} else {
				// add unmodified plan
				personClone.addPlan(person.getSelectedPlan());
			}
			
		}		
		log.info("Done.");
		return populationOutput;
		
	}

	private Coord getPlausiblePRCoord(Coord coordOrigin, Coord coordDestination) {
		double minDistance = Double.MAX_VALUE;
		Coord minDistanceCoord = null;
		for (Coord coord : prCoordinates) {
			double distance = NetworkUtils.getEuclideanDistance(coordOrigin, coord) + NetworkUtils.getEuclideanDistance(coord, coordDestination);
			if (distance < minDistance) {
				minDistance = distance;
				minDistanceCoord = coord;
			}
		}
		return minDistanceCoord;
	}

	private void tagPotentialSAVusers() {
		log.info("Tagging potential SAV users...");
		
		log.info("Going through the population and analyzing the activity coordinates...");
		double popsize = this.scenario.getPopulation().getPersons().size();

		int counter = 0;
		for (Person person : this.scenario.getPopulation().getPersons().values()) {
			counter++;
			
			if (counter % 10000 == 0) {
				log.info("# " + counter / popsize);
			}
			Plan selectedPlan = person.getSelectedPlan();
			if (selectedPlan == null) {
				throw new RuntimeException("No selected plan. Aborting...");
			}
			
			boolean allActivitiesInArea = true;

			for (PlanElement pE : selectedPlan.getPlanElements()) {
				
				if (allActivitiesInArea) {
					if (pE instanceof Activity) {
						Activity activity = (Activity) pE;
						
						if (activity.getType().toString().contains("interaction")) {
							// skip
						} else {
							boolean activityInArea = isActivityInArea(activity);
							
							if (!activityInArea) {
								allActivitiesInArea = false;
							}	
						}		
					}	
				}
			}
			
			if (inputPersonAttributesFile == null) {
				
				if (allActivitiesInArea) { 
					scenario.getPopulation().getPersonAttributes().putAttribute(person.getId().toString(), scenario.getConfig().plans().getSubpopulationAttributeName(), "potentialSAVuser");
					potentialSAVusers++;
				} else {
					scenario.getPopulation().getPersonAttributes().putAttribute(person.getId().toString(), scenario.getConfig().plans().getSubpopulationAttributeName(), "noPotentialSAVuser");
					noPotentialSAVusers++;
				}
			} else {
				
				String subpopulation = (String) scenario.getPopulation().getPersonAttributes().getAttribute(person.getId().toString(), scenario.getConfig().plans().getSubpopulationAttributeName());
				if (subpopulation.equals(inputPersonAttributesSubpopulationPerson)) {
					if (allActivitiesInArea) { 
						scenario.getPopulation().getPersonAttributes().putAttribute(person.getId().toString(), scenario.getConfig().plans().getSubpopulationAttributeName(), "person_potentialSAVuser");
						potentialSAVusers++;
					} else {
						scenario.getPopulation().getPersonAttributes().putAttribute(person.getId().toString(), scenario.getConfig().plans().getSubpopulationAttributeName(), "person_noPotentialSAVuser");
						noPotentialSAVusers++;
					}
				}
			}
		}
		
		log.info("Tagging potential SAV users... Done.");
		
	}

	private boolean isActivityInArea(Activity activity) {
		boolean activityInArea = false;
		for (Geometry geometry : zoneId2geometry.values()) {
			Point p = MGC.coord2Point(ct.transform(activity.getCoord())); 
			
			if (p.within(geometry)) {
				activityInArea = true;
			}
		}
		return activityInArea;
	}

	private void tagCarUsers() {
		
		log.info("Tagging car users...");
		
		for (Person person : scenario.getPopulation().getPersons().values()) {
			Plan selectedPlan = person.getSelectedPlan();
			if (selectedPlan == null) {
				throw new RuntimeException("No selected plan. Aborting...");
			}
			
			boolean personHasCarTrip = false;
			
			for (PlanElement pE : selectedPlan.getPlanElements()) {
				
				if (pE instanceof Leg) {
					Leg leg = (Leg) pE;
					if (leg.getMode().equals(TransportMode.car)) {
						personHasCarTrip = true;
					}	
				}	
			}
			person.getAttributes().putAttribute("CarOwnerInBaseCase", personHasCarTrip);
			if (personHasCarTrip) {
				carUsers++;
			} else {
				noCarUsers++;
			}
		}		
		log.info("Tagging car users... Done.");
	}

	private Population changeTripModes(String subpopulationName, String fromMode, String toMode) {
		log.info("Changing trip modes for subpopulation " + subpopulationName + ": " + fromMode + " --> " + toMode + "...");
		final String[] attributes = {"CarOwnerInBaseCase"};

		Scenario scenarioOutput = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
		Population populationOutput = scenarioOutput.getPopulation();
		
		for (Person person : scenario.getPopulation().getPersons().values()) {
			
			if (person.getPlans().size() > 1) throw new RuntimeException("More than one plan per person. Aborting...");
			
			PopulationFactory factory = populationOutput.getFactory();
			Person personClone = factory.createPerson(person.getId());
			for (String attribute : attributes) {
				personClone.getAttributes().putAttribute(attribute, person.getAttributes().getAttribute(attribute));
			}
			populationOutput.addPerson(personClone);
		
			if (this.scenario.getPopulation().getPersonAttributes().getAttribute(person.getId().toString(), scenario.getConfig().plans().getSubpopulationAttributeName()).equals(subpopulationName)) {
		
				Plan plan = factory.createPlan();
				
				Leg previousLeg = null;
				boolean previousActivityWasCarInteraction = false;
				
				for (PlanElement pE : person.getSelectedPlan().getPlanElements()) {
					
					if (pE instanceof Activity) {
						Activity act = (Activity) pE;
						if (act.getType().equals("car interaction")) {
							// do not add the activity
							previousActivityWasCarInteraction = true;
						} else {
							// add previous leg and current activity
							if (previousLeg != null) plan.addLeg(previousLeg);
							plan.addActivity(act);
							
							previousActivityWasCarInteraction = false;
						}
						
					} else if (pE instanceof Leg) {
						Leg leg = (Leg) pE;
						if (previousActivityWasCarInteraction == false) {
							previousLeg = leg;
						} else {
							previousLeg = null;
						}
						
						if (leg.getMode().equals(TransportMode.car)) {
							leg.setMode("taxi");
							leg.setRoute(null);
							leg.setTravelTime(Double.MIN_VALUE);
							
							plan.addLeg(leg);
						}						
					}	
				}
				// add modified plan
				personClone.addPlan(plan);
			} else {
				// add unmodified plan
				personClone.addPlan(person.getSelectedPlan());
			}
			
		}		
		log.info("Changing trip modes for subpopulation " + subpopulationName + ": " + fromMode + " --> " + toMode + "... Done.");
		return populationOutput;
	}
	
}

