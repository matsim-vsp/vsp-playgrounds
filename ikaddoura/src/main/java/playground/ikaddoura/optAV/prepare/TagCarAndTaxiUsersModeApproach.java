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
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
* @author ikaddoura
*/

public class TagCarAndSAVusers2 {

	private static final Logger log = Logger.getLogger(TagCarAndSAVusers2.class);
	
	private final String inputDirectory = "/Users/ihab/Documents/workspace/runs-svn/b5_optAV_congestion/input/population/";
	private final String outputDirectory = "/Users/ihab/Documents/workspace/runs-svn/b5_optAV_congestion/input/population/";
	
	private final String inputPlansFile = "berlin-v5.1-1pct.plans.xml.gz";
	private final String outputPlansFile = "berlin-5.1-1pct_plans_taggedCarUsers_noCarInBerlin_splitTrips.xml.gz";

	private final String inputPersonAttributesFile = "berlin-v5.0.person-attributes.xml.gz";
	private final String inputPersonAttributesSubpopulationPerson = "person";
	
	private final String areaOfPotentialSAVusersSHPFile = "/Users/ihab/Documents/workspace/shared-svn/projects/audi_av/shp/untersuchungsraumAll.shp";
	private final String crsSHPFile = "EPSG:25833";
	private final String crsPopulation = TransformationFactory.DHDN_GK4;
	
	private final String modeToReplaceCarTripsInBerlin = TransportMode.taxi;
	private final String modeToReplaceCarTripsToFromBerlin = TransportMode.pt;
	private final String berlinModeTail = "";
	private final String brandenburgModeTail = "_brandenburg";
	private final String fromToBerlinModeTail = "_from-to-berlin";
	
	private final boolean splitTrips = true; 
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
			new Coord(4581737.18532087, 5834563.406155014), // S Hennigsdorf
			new Coord(4587412.176538966, 5834115.416970312), // S Frohnau
		};
	private final String parkAndRideActivity = "park-and-ride";
	private final double parkAndRideDuration = 60.;

	// ####################################################################

	private Scenario scenario;
	private final StageActivityTypes stageActivities = new StageActivityTypesImpl("pt interaction", "car interaction");
	private int carUsers = 0;
	private int noCarUsers = 0;
	
	private final Map<Integer, Geometry> zoneId2geometry = new HashMap<Integer, Geometry>();
	private final CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(crsPopulation, crsSHPFile);
	
	public static void main(String[] args) {			
				
		TagCarAndSAVusers2 generateAVDemand = new TagCarAndSAVusers2();
		generateAVDemand.run();
	}

	private void run() {
				
		Config config = ConfigUtils.createConfig();
		config.plans().setInputFile(inputDirectory + inputPlansFile);
		config.plans().setInputPersonAttributeFile(inputDirectory + inputPersonAttributesFile);
		scenario = ScenarioUtils.loadScenario(config);
		
		final int agentsBefore = scenario.getPopulation().getPersons().size();
		
		tagCarUsers();
		loadShapeFile();		
		new PopulationWriter(splitTrips()).write(outputDirectory + outputPlansFile);
		
		log.info("Car users: " + carUsers);
		log.info("No car users: " + noCarUsers);
		
		log.info("Agents (before): " + agentsBefore);
		log.info("Agents (after): " + scenario.getPopulation().getPersons().size());
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

	private Population splitTrips() {
				
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
						
			if (this.scenario.getPopulation().getPersonAttributes().getAttribute(person.getId().toString(), scenario.getConfig().plans().getSubpopulationAttributeName()).equals(inputPersonAttributesSubpopulationPerson)) {

				Plan splitTripPlan = factory.createPlan(); // with split activities
				Plan directTripPlan = factory.createPlan(); // without split activities

				// add first activity
				splitTripPlan.addActivity((Activity) person.getSelectedPlan().getPlanElements().get(0));
				directTripPlan.addActivity((Activity) person.getSelectedPlan().getPlanElements().get(0));

				for (Trip trip : TripStructureUtils.getTrips(person.getSelectedPlan().getPlanElements(), stageActivities )) {
					
					String mainMode = new MainModeIdentifierImpl().identifyMainMode(trip.getTripElements());
								
					if (isActivityInArea(trip.getOriginActivity()) && isActivityInArea(trip.getDestinationActivity())) {
						// berlin --> berlin
						if (mainMode.equals(TransportMode.car)) {				
							splitTripPlan.addLeg(factory.createLeg(modeToReplaceCarTripsInBerlin + berlinModeTail ));		
							directTripPlan.addLeg(factory.createLeg(modeToReplaceCarTripsInBerlin + berlinModeTail));				
						} else {
							splitTripPlan.addLeg(factory.createLeg(mainMode + berlinModeTail));
							directTripPlan.addLeg(factory.createLeg(mainMode + berlinModeTail));
						}
						
						splitTripPlan.addActivity(trip.getDestinationActivity());
						directTripPlan.addActivity(trip.getDestinationActivity());
						
					} else if (isActivityInArea(trip.getOriginActivity()) && !isActivityInArea(trip.getDestinationActivity())) {
						// berlin --> brandenburg
						
						// berlin trip
						if (mainMode.equals(TransportMode.car)) {	
							splitTripPlan.addLeg(factory.createLeg(modeToReplaceCarTripsInBerlin + berlinModeTail));
							directTripPlan.addLeg(factory.createLeg(modeToReplaceCarTripsToFromBerlin + fromToBerlinModeTail));
						} else {
							splitTripPlan.addLeg(factory.createLeg(mainMode + berlinModeTail));
							directTripPlan.addLeg(factory.createLeg(mainMode + fromToBerlinModeTail));
						}

						Activity prActivity = factory.createActivityFromCoord(parkAndRideActivity, getPlausiblePRCoord(trip.getOriginActivity().getCoord(), trip.getDestinationActivity().getCoord()));
						prActivity.setMaximumDuration(parkAndRideDuration);
						splitTripPlan.addActivity(prActivity);
						
						// brandenburg trip
						splitTripPlan.addLeg(factory.createLeg(mainMode + brandenburgModeTail));
						
						splitTripPlan.addActivity(trip.getDestinationActivity());		
						directTripPlan.addActivity(trip.getDestinationActivity());
						
					} else if (!isActivityInArea(trip.getOriginActivity()) && isActivityInArea(trip.getDestinationActivity())) {
						// brandenburg --> berlin
						
						// brandenburg trip
						splitTripPlan.addLeg(factory.createLeg(mainMode + brandenburgModeTail));
						
						Activity prActivity = factory.createActivityFromCoord(parkAndRideActivity, getPlausiblePRCoord(trip.getOriginActivity().getCoord(), trip.getDestinationActivity().getCoord()));
						prActivity.setMaximumDuration(parkAndRideDuration);
						splitTripPlan.addActivity(prActivity);
						
						// berlin trip
						if (mainMode.equals(TransportMode.car)) {	
							splitTripPlan.addLeg(factory.createLeg(modeToReplaceCarTripsInBerlin + berlinModeTail));
							directTripPlan.addLeg(factory.createLeg(modeToReplaceCarTripsToFromBerlin +  fromToBerlinModeTail));
						} else {
							splitTripPlan.addLeg(factory.createLeg(mainMode + berlinModeTail));
							directTripPlan.addLeg(factory.createLeg(mainMode + fromToBerlinModeTail));
						}
						
						splitTripPlan.addActivity(trip.getDestinationActivity());			
						directTripPlan.addActivity(trip.getDestinationActivity());
						
					} else if (!isActivityInArea(trip.getOriginActivity()) && !isActivityInArea(trip.getDestinationActivity())){
						// brandenburg --> brandenburg
						splitTripPlan.addLeg(factory.createLeg(mainMode + brandenburgModeTail));
						directTripPlan.addLeg(factory.createLeg(mainMode + brandenburgModeTail));
						
						splitTripPlan.addActivity(trip.getDestinationActivity());
						directTripPlan.addActivity(trip.getDestinationActivity());

					} else {
						throw new RuntimeException("Aborting...");
					}
				}
				if (splitTrips) personClone.addPlan(splitTripPlan);
				personClone.addPlan(directTripPlan);
			} else {
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

}

