package playground.lu.congestionAwareDrt.berlin;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
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
import org.matsim.core.router.FastAStarLandmarksFactory;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

import playground.lu.readShapeFile.ShapeFileReadingUtils;

public class PrepareInnerBerlinPopulation {
	private static final Logger log = Logger.getLogger(PrepareInnerBerlinPopulation.class);

//	private static final String INPUT_PLAN_FILE = "C:\\Users\\cluac\\MATSimScenarios\\Berlin\\output\\default\\testing-plan.xml";
//	private static final String NETWORK_FILE = "C:\\Users\\cluac\\MATSimScenarios\\CongestionAwareDrt\\"
//			+ "Berlin\\network.xml.gz";
//	private static final String SHAPEFILE = "C:\\Users\\cluac\\MATSimScenarios\\CongestionAwareDrt\\"
//			+ "Berlin\\shp-berlin-hundekopf-areas\\berlin-hundekopf.shp";
//	private static final String OUTPUT_PLAN_FILE = "C:\\Users\\cluac\\MATSimScenarios\\CongestionAwareDrt\\"
//			+ "Berlin\\test-plans.xml";

	public static void main(String[] args) {
		String INPUT_PLAN_FILE = args[0];
		String NETWORK_FILE = args[1];
		String SHAPEFILE = args[2];
		String OUTPUT_PLAN_FILE = args[3];
		
		Config config = ConfigUtils.createConfig();
		config.global().setCoordinateSystem("EPSG:31468");
//		config.global().setCoordinateSystem("GK4");
		config.network().setInputFile(NETWORK_FILE);
		config.plans().setInputFile(INPUT_PLAN_FILE);
		config.plansCalcRoute().setRoutingRandomness(0);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Population originalPopulation = scenario.getPopulation();
		Network network = scenario.getNetwork();

		Config config2 = ConfigUtils.createConfig();
		config2.global().setCoordinateSystem("EPSG:31468");
		config2.network().setInputFile(NETWORK_FILE);
		Scenario outputScenario = ScenarioUtils.loadScenario(config2);
		Population outputPopulation = outputScenario.getPopulation();
		PopulationFactory populationFactory = outputScenario.getPopulation().getFactory();

		FreeSpeedTravelTime travelTime = new FreeSpeedTravelTime();
		FastAStarLandmarksFactory fastAStarLandmarksFactory = new FastAStarLandmarksFactory(8);
		RandomizingTimeDistanceTravelDisutilityFactory disutilityFactory = new RandomizingTimeDistanceTravelDisutilityFactory(
				"car", config);
		TravelDisutility travelDisutility = disutilityFactory.createTravelDisutility(travelTime);
		LeastCostPathCalculator router = fastAStarLandmarksFactory.createPathCalculator(network, travelDisutility,
				travelTime);

		// Categorize links into different group (inside, outside, ring)
		log.info("Loading shapefile...");
		List<Id<Link>> insideLinks = new ArrayList<>();
		List<Id<Link>> linksOnRing = new ArrayList<>();
		Geometry hundekopf = ShapeFileReadingUtils.getGeometryFromShapeFile(SHAPEFILE);

		log.info("Shapefile successfully loaded");
		log.info("Begin extracting links within and on the Berlin Ring...");
		for (Link link : network.getLinks().values()) {
			if (link.getAllowedModes().contains("car")) {
				if (ShapeFileReadingUtils.isLinkWithinGeometry(link, hundekopf)) {
					insideLinks.add(link.getId());
				}
				if (ShapeFileReadingUtils.isCoordWithinGeometry(link.getFromNode().getCoord(), hundekopf)
						^ ShapeFileReadingUtils.isCoordWithinGeometry(link.getToNode().getCoord(), hundekopf)) {
					linksOnRing.add(link.getId());
				}
			}
		}

		// Process population file
		log.info("Begin analyzing the population...");
		int populationSize = originalPopulation.getPersons().keySet().size();
		log.info("Population size of original population file is " + populationSize);
		int processed = 0;
		int outputCounter = 0;
		for (Person person : originalPopulation.getPersons().values()) {
			for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
				if (planElement instanceof Leg) {
					Leg leg = (Leg) planElement;
					if (leg.getMode().equals("car")) {
						Id<Link> fromLinkId = leg.getRoute().getStartLinkId();
						Id<Link> toLinkId = leg.getRoute().getEndLinkId();

						boolean fromLinkIsInside = insideLinks.contains(fromLinkId);
						boolean toLinkIsInside = insideLinks.contains(toLinkId);

						// Case 0 Through trips (ignored)
						if (!fromLinkIsInside && !toLinkIsInside) {
							continue; // DO nothing
						}

						Person outputPerson = populationFactory
								.createPerson(Id.create(Integer.toString(outputCounter), Person.class));
						Plan plan = populationFactory.createPlan();

						// Case 1 Both origin and destination links are within the ring
						if (fromLinkIsInside && toLinkIsInside) {
							Activity act0 = populationFactory.createActivityFromLinkId("dummy", fromLinkId);
							act0.setEndTime(leg.getDepartureTime().orElse(0));
							Leg outputLeg = populationFactory.createLeg("drt");
							Activity act1 = populationFactory.createActivityFromLinkId("dummy", toLinkId);
							plan.addActivity(act0);
							plan.addLeg(outputLeg);
							plan.addActivity(act1);
						}

						// Case 2 Incoming trips
						if (!fromLinkIsInside && toLinkIsInside) {
							String[] interesctionInfo = findIntersectionPoint(fromLinkId, toLinkId, router, network,
									linksOnRing);
							Id<Link> interesctionLinkId = Id.create(interesctionInfo[0], Link.class);
							Activity act0 = populationFactory.createActivityFromLinkId("dummy", interesctionLinkId);
							double estimatedTravelTime = Double.parseDouble(interesctionInfo[1]);
							act0.setEndTime(leg.getDepartureTime().orElse(0) + estimatedTravelTime);
							Leg outputLeg = populationFactory.createLeg("drt");
							Activity act1 = populationFactory.createActivityFromLinkId("dummy", toLinkId);
							plan.addActivity(act0);
							plan.addLeg(outputLeg);
							plan.addActivity(act1);
						}
						// Case 3 Outgoing trips
						if (fromLinkIsInside && !toLinkIsInside) {
							Activity act0 = populationFactory.createActivityFromLinkId("dummy", fromLinkId);
							act0.setEndTime(leg.getDepartureTime().orElse(0));
							Leg outputLeg = populationFactory.createLeg("drt");
							String[] interesctionInfo = findIntersectionPoint(fromLinkId, toLinkId, router, network,
									linksOnRing);
							Id<Link> interesctionLinkId = Id.create(interesctionInfo[0], Link.class);
							Activity act1 = populationFactory.createActivityFromLinkId("dummy", interesctionLinkId);
							plan.addActivity(act0);
							plan.addLeg(outputLeg);
							plan.addActivity(act1);
						}

						outputPerson.addPlan(plan);
						outputPopulation.addPerson(outputPerson);
						outputCounter += 1;
					}
				}

			}
			processed += 1;
			if (processed % 500 == 0) {
				log.info("Processing population: " + processed + " completed");
			}
		}

		// write population file
		log.info("Writing population file...");
		PopulationWriter pw = new PopulationWriter(outputPopulation);
		pw.write(OUTPUT_PLAN_FILE);
	}

	private static String[] findIntersectionPoint(Id<Link> fromLinkId, Id<Link> toLinkId,
			LeastCostPathCalculator router, Network network, List<Id<Link>> linksOnRing) {
		Node fromNode = network.getLinks().get(fromLinkId).getToNode();
		Node toNode = network.getLinks().get(toLinkId).getToNode();
		Path path = router.calcLeastCostPath(fromNode, toNode, 0, null, null);
		double travelTime = 0;
		for (Link link : path.links) {
			travelTime += link.getLength() / link.getFreespeed();
			if (linksOnRing.contains(link.getId())) {
				return new String[] { link.getId().toString(), Double.toString(travelTime) };
			}
		}
		return new String[] { linksOnRing.get(0).toString(), "0" };
	}

}
