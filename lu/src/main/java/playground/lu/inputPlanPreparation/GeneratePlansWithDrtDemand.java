package playground.lu.inputPlanPreparation;

import java.util.Random;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;

public class GeneratePlansWithDrtDemand {
	private static final Logger log = Logger.getLogger(GeneratePlansWithDrtDemand.class);
	private final static long SEEDS[] = { 1, 2, 3, 4, 5 };
	private final static String INPUT_CONFIG_FILE = "C:/Users/cluac/MATSimScenarios/Berlin/testing.config.xml";
	private final static String OUTPUT_PATH_BEGINNING = "C:/Users/cluac/MATSimScenarios/Berlin/drtPlans/ConvertedPlan_";
	private final static String SHAPEFILE = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-shp/berlin.shp";

	private final static double CAR_TO_DRT = 0.00729;
	private final static double PT_TO_DRT = 0.03323;
	private final static double BIKE_TO_DRT = 0.18336;
	private final static double WALK_TO_DRT = 0.06510;
	private final static double RIDE_TO_DRT = 0.0;

	public static void main(String[] args) {
		Geometry serviceArea = CountingTripsWithinServiceArea.generateGeometryFromShapeFile(SHAPEFILE);
		for (int i = 0; i < SEEDS.length; i++) {
			log.info("Generating converted population file based on ssed " + SEEDS[i]);
			generateConvertedPlan(SEEDS[i], serviceArea);
		}
	}

	private static void generateConvertedPlan(long seed, Geometry serviceArea) {
		int personCounter = 0;
		int drtTripsCounter = 0;
		Random rnd = new Random(seed);
		Config config = ConfigUtils.loadConfig(INPUT_CONFIG_FILE);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Network network = scenario.getNetwork();

		for (Person person : scenario.getPopulation().getPersons().values()) {
			if (!person.getId().toString().startsWith("freight")) {
				boolean carToDrt = false;
				boolean bikeToDrt = false;
				if (rnd.nextDouble() < CAR_TO_DRT) {
					carToDrt = true;
				}

				if (rnd.nextDouble() < BIKE_TO_DRT) {
					bikeToDrt = true;
				}

				for (PlanElement planElement : person.getSelectedPlan().getPlanElements()) {
					if (planElement instanceof Leg) {
						Coord startCoord = network.getLinks().get(((Leg) planElement).getRoute().getStartLinkId())
								.getCoord();
						Coord endCoord = network.getLinks().get(((Leg) planElement).getRoute().getEndLinkId())
								.getCoord();
						Point startPoint = MGC.coord2Point(startCoord);
						Point endPoint = MGC.coord2Point(endCoord);
						if (startPoint.within(serviceArea) && endPoint.within(serviceArea)) {
							switch (((Leg) planElement).getMode()) {
							case "car":
								if (carToDrt) {
									((Leg) planElement).setMode("drt");
									drtTripsCounter += 1;
								}
								break;
							case "pt":
								if (rnd.nextDouble() < PT_TO_DRT) {
									((Leg) planElement).setMode("drt");
									drtTripsCounter += 1;
								}
								break;
							case "bicycle":
								if (bikeToDrt) {
									((Leg) planElement).setMode("drt");
									drtTripsCounter += 1;
								}
								break;

							case "walk":
								if (rnd.nextDouble() < WALK_TO_DRT) {
									((Leg) planElement).setMode("drt");
									drtTripsCounter += 1;
								}
								break;
							case "ride":
								if (rnd.nextDouble() < RIDE_TO_DRT) {
									((Leg) planElement).setMode("drt");
									drtTripsCounter += 1;
								}
								break;
							default:
								log.info("Mode " + ((Leg) planElement).getMode()
										+ " is not included in the normal mode list, no conversion will be performed with this leg");
								break;
							}
						}
					}
				}
			}
			personCounter += 1;
			if (personCounter % 100 == 0) {
				log.info("Conversion in progress: " + personCounter + " persons have been processed");
			}
		}

		log.info("Trip conversion complete. There are " + drtTripsCounter + " drt demands");
		log.info("Writing population file");
		new PopulationWriter(scenario.getPopulation()).write(OUTPUT_PATH_BEGINNING + Long.toString(seed) + ".xml.gz");
		log.info("Writing population complete.");

	}

}
