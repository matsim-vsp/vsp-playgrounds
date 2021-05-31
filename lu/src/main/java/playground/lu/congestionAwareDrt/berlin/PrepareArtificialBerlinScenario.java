package playground.lu.congestionAwareDrt.berlin;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
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
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.fleet.FleetWriter;
import org.matsim.contrib.dvrp.fleet.ImmutableDvrpVehicleSpecification;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import playground.lu.readShapeFile.ShapeFileReadingUtils;

public class PrepareArtificialBerlinScenario {
	private static final Logger log = Logger.getLogger(PrepareArtificialBerlinScenario.class);

	private static final int NUM_OF_TRIPS = 100000;
	private static final int NUM_OF_VEHICLES = 30000;
	private static final int[] TIME_WINDOW = { 21600, 32400 };
	private final static Random RND = new Random(1234);

	private static final String SHAPEFILE = "/Users/luchengqi/Documents/MATSimScenarios/Berlin/Congestion-Aware-DRT" +
			"/OriginalData/Shapefiles/berlin-hundekopf.shp";
	private static final String NETWORK_FILE = "/Users/luchengqi/Documents/MATSimScenarios/Berlin" +
			"/Congestion-Aware-DRT/berlin-v5.5-network.xml.gz";
	private static final String OUTPUT_PLAN_FILE = "/Users/luchengqi/Documents/MATSimScenarios/Berlin" +
			"/Congestion-Aware-DRT/drt.plans.xml";
	private static final String BASE_CASE_PLAN_FILE = "/Users/luchengqi/Documents/MATSimScenarios/Berlin" +
			"/Congestion-Aware-DRT/base-case.plans.xml";
	private static final String OUTPUT_VEHICLE_FILE = "/Users/luchengqi/Documents/MATSimScenarios/Berlin" +
			"/Congestion-Aware-DRT/vehicles.xml";

	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		config.global().setCoordinateSystem("EPSG:31468");
		config.network().setInputFile(NETWORK_FILE);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Scenario scenario2 = ScenarioUtils.loadScenario(config);
		PopulationFactory populationFactory = scenario.getPopulation().getFactory();
		PopulationFactory populationFactory2 = scenario2.getPopulation().getFactory();
		Network network = scenario.getNetwork();

		// Extracting links within the dog head area
		log.info("Loading shapefile...");
		List<Link> linksInBerlinRing = new ArrayList<>();
		Geometry hundekopf = ShapeFileReadingUtils.getGeometryFromShapeFile(SHAPEFILE);

		log.info("Shapefile successfully loaded");
		log.info("Begin extracting links within the Berlin Ring...");
		for (Link link : network.getLinks().values()) {
			if (ShapeFileReadingUtils.isLinkWithinGeometry(link, hundekopf) && link.getAllowedModes().contains("car")) {
				linksInBerlinRing.add(link);
			}
		}
		int numOfLinks = linksInBerlinRing.size();
		int timeWindowLength = TIME_WINDOW[1] - TIME_WINDOW[0];

		log.info("There are in total " + numOfLinks + " links within the Berlin Ring");
		log.info("Begin generating DRT population...");

		// Create population file
		Population population = scenario.getPopulation();
		Population baseCasePopulation = scenario2.getPopulation();
		int counter = 0;
		while (counter < NUM_OF_TRIPS) {
			Link departureLink = linksInBerlinRing.get(RND.nextInt(numOfLinks));
			Link arrivalLink = linksInBerlinRing.get(RND.nextInt(numOfLinks));
			if (departureLink.getId().toString().equals(arrivalLink.getId().toString())){
				continue;
			}
			double departureTime = TIME_WINDOW[0] + RND.nextInt(timeWindowLength);

			Person person = populationFactory
					.createPerson(Id.create("dummy_person_" + Integer.toString(counter), Person.class));
			Person person2 = populationFactory2
					.createPerson(Id.create("dummy_person_" + Integer.toString(counter), Person.class));

			Plan plan = populationFactory.createPlan();
			Plan plan2 = populationFactory2.createPlan();

			Activity act0 = populationFactory.createActivityFromLinkId("dummy", departureLink.getId());
			act0.setStartTime(0);
			act0.setEndTime(departureTime);
			Leg leg = populationFactory.createLeg("drt");
			Leg leg2 = populationFactory2.createLeg("car");
			Activity act1 = populationFactory.createActivityFromLinkId("dummy", arrivalLink.getId());
			act1.setEndTime(43200);

			plan.addActivity(act0);
			plan.addLeg(leg);
			plan.addActivity(act1);
			person.addPlan(plan);
			population.addPerson(person);

			plan2.addActivity(act0);
			plan2.addLeg(leg2);
			plan2.addActivity(act1);
			person2.addPlan(plan2);
			baseCasePopulation.addPerson(person2);

			counter += 1;
			if (counter % 1000 == 0) {
				log.info("plan generation in progress: " + counter + " trips added");
			}
		}

		log.info("Writing population file...");
		PopulationWriter pw = new PopulationWriter(population);
		pw.write(OUTPUT_PLAN_FILE);

		PopulationWriter pw2 = new PopulationWriter(baseCasePopulation);
		pw2.write(BASE_CASE_PLAN_FILE);

		log.info("Writing vehicle file...");
		// Creating vehicle file
		List<DvrpVehicleSpecification> vehicles = new ArrayList<>();
		for (int i = 0; i < NUM_OF_VEHICLES; i++) {
			Link startLink = linksInBerlinRing.get(RND.nextInt(numOfLinks));
			vehicles.add(ImmutableDvrpVehicleSpecification.newBuilder()
					.id(Id.create("drt_" + Integer.toString(i), DvrpVehicle.class)).startLinkId(startLink.getId())
					.capacity(1).serviceBeginTime(Math.round(1)).serviceEndTime(Math.round(30 * 3600)).build());
		}
		new FleetWriter(vehicles.stream()).write(OUTPUT_VEHICLE_FILE);
	}

}
