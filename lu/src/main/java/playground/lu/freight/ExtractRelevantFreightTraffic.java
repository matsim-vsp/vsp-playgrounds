package playground.lu.freight;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.router.FastAStarLandmarksFactory;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

import playground.lu.readShapeFile.ShapeFileReadingUtils;

public class ExtractRelevantFreightTraffic {
	// This script will extract the relevant freight trips within the given shape
	// file from the German wide freight traffic.
	private static final Logger log = Logger.getLogger(ExtractRelevantFreightTraffic.class);

	public static final String GERMAN_FREIGHT = "C:\\Users\\cluac\\MATSimScenarios\\Freight-Germany\\testing.freight.plans.xml";
	public static final String SHAPEFILE_PATH = "C:\\Users\\cluac\\MATSimScenarios\\Freight-Germany\\original_data\\TestingShapeFile\\TestingShapeFile.shp";
	public static final String GERMAN_NETWORK = "C:\\Users\\cluac\\MATSimScenarios\\Freight-Germany\\german-primary-road.network.xml.gz";
	public static final String CRS = "EPSG:5677";

	public static void main(String[] args) {
		if (args.length == 0) {
			args = new String[] {
					"C:\\Users\\cluac\\MATSimScenarios\\Freight-Germany\\relevant-testing-freight.plan.xml" };
		}
		String outputPath = args[0];

		// Loading Scenario
		Config config = ConfigUtils.createConfig();
		config.global().setCoordinateSystem(CRS);
		config.network().setInputFile(GERMAN_NETWORK);
		Scenario outputScenario = ScenarioUtils.loadScenario(config);
		config.plans().setInputFile(GERMAN_FREIGHT);
		config.plansCalcRoute().setRoutingRandomness(0);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Network network = scenario.getNetwork();
		Population originalPlans = scenario.getPopulation();
		Population outputPlans = outputScenario.getPopulation();
		PopulationFactory populationFactory = outputPlans.getFactory();

		// Create router
		FreeSpeedTravelTime travelTime = new FreeSpeedTravelTime();
		FastAStarLandmarksFactory fastAStarLandmarksFactory = new FastAStarLandmarksFactory(8);
		RandomizingTimeDistanceTravelDisutilityFactory disutilityFactory = new RandomizingTimeDistanceTravelDisutilityFactory(
				"car", config);
		TravelDisutility travelDisutility = disutilityFactory.createTravelDisutility(travelTime);
		LeastCostPathCalculator router = fastAStarLandmarksFactory.createPathCalculator(network, travelDisutility,
				travelTime);

		// Reading Shape file
		log.info("Loading shapefile...");
		Geometry relevantArea = ShapeFileReadingUtils.getGeometryFromShapeFile(SHAPEFILE_PATH);
		// TODO this function is only available in this playground. Changes will be
		// needed when moving this to other place
		log.info("Shapefile successfully loaded");

		// Identify links on the boundary
		List<Id<Link>> linksOnTheBoundary = new ArrayList<>();
		for (Link link : network.getLinks().values()) {
			Coord fromCoord = link.getFromNode().getCoord();
			Coord toCoord = link.getToNode().getCoord();
			if (ShapeFileReadingUtils.isCoordWithinGeometry(fromCoord, relevantArea)
					^ ShapeFileReadingUtils.isCoordWithinGeometry(toCoord, relevantArea)) {
				linksOnTheBoundary.add(link.getId());
			}
		}

		// Create modified population
		int generated = 0;
		int processed = 0;
		log.info("Start creating the modified plans: there are in total " + originalPlans.getPersons().keySet().size()
				+ " persons to be processed");
		for (Person person : originalPlans.getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			// By default, the plan of each freight person consist of only 3 elements:
			// startAct, leg, endAct
			Activity startActivity = (Activity) plan.getPlanElements().get(0);
			Activity endActivity = (Activity) plan.getPlanElements().get(2);
			Id<Link> startLink = startActivity.getLinkId();
			Id<Link> endLink = endActivity.getLinkId();
			Coord startCoord = startActivity.getCoord();
			Coord endCoord = endActivity.getCoord();
			double departureTime = startActivity.getEndTime().orElse(0);

			boolean originIsInside = ShapeFileReadingUtils.isCoordWithinGeometry(startCoord, relevantArea);
			boolean destinationIsInside = ShapeFileReadingUtils.isCoordWithinGeometry(endCoord, relevantArea);

			Activity act0 = populationFactory.createActivityFromCoord("freight_start", null);
			Leg leg = populationFactory.createLeg("freight");
			Activity act1 = populationFactory.createActivityFromCoord("freight_end", null);

			// Case 1: both origin and destination are within the relevant region
			if (originIsInside && destinationIsInside) {
				act0.setCoord(startCoord);
				act0.setEndTime(departureTime);
				act1.setCoord(endCoord);
			}

			// Case 2: outgoing trips
			if (originIsInside && !destinationIsInside) {
				act0.setCoord(startCoord);
				act0.setEndTime(departureTime);
				Path route = router.calcLeastCostPath(network.getLinks().get(startLink).getToNode(),
						network.getLinks().get(endLink).getToNode(), 0, null, null);
				for (Link link : route.links) {
					if (linksOnTheBoundary.contains(link.getId())) {
						act1.setCoord(link.getCoord());
						break;
					}
				}

			}
			// Case 3: incoming trips
			if (!originIsInside && destinationIsInside) {
				Path route = router.calcLeastCostPath(network.getLinks().get(startLink).getToNode(),
						network.getLinks().get(endLink).getToNode(), 0, null, null);
				double timeSpent = 0;
				for (Link link : route.links) {
					if (linksOnTheBoundary.contains(link.getId())) {
						act0.setCoord(link.getCoord());
						double newEndTime = departureTime + timeSpent;
						act0.setEndTime(newEndTime);
						break;
					}
					timeSpent += Math.floor(link.getLength() / link.getFreespeed()) + 1;
				}
				act1.setCoord(endCoord);
			}

			// case 4: through trips
			if (!originIsInside && !destinationIsInside) {
				boolean tripIsRelevant = false;
				double timeSpent = 0;
				boolean vehicleIsInside = false;
				Path route = router.calcLeastCostPath(network.getLinks().get(startLink).getToNode(),
						network.getLinks().get(endLink).getToNode(), 0, null, null);
				for (Link link : route.links) {
					if (linksOnTheBoundary.contains(link.getId())) {
						tripIsRelevant = true;
						if (!vehicleIsInside) {
							act0.setCoord(link.getCoord());
							double newEndTime = departureTime + timeSpent;
							act0.setEndTime(newEndTime);
							vehicleIsInside = true;
						} else {
							act1.setCoord(link.getCoord());
							break;
						}
					}
					timeSpent += Math.floor(link.getLength() / link.getFreespeed()) + 1;
				}

				if (!tripIsRelevant) {
					continue;
				}
			}

			// Add new freight person to the output plans
			if (act0.getEndTime().orElse(86400) < 86400) {
				Person freightPerson = populationFactory
						.createPerson(Id.create(Integer.toString(generated), Person.class));
				Plan freightPersonPlan = populationFactory.createPlan();
				freightPersonPlan.addActivity(act0);
				freightPersonPlan.addLeg(leg);
				freightPersonPlan.addActivity(act1);
				freightPerson.addPlan(freightPersonPlan);
				outputPlans.addPerson(freightPerson);
				generated += 1;
			}
			processed += 1;
			if (processed % 100 ==0) {
				log.info("Processing: " + processed + " persons have been processed");
			}
		}

		// Write population
		log.info("Writing population file...");
		PopulationWriter pw = new PopulationWriter(outputPlans);
		pw.write(outputPath);
	}
}
