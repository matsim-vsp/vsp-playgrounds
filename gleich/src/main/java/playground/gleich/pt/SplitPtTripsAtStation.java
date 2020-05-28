package playground.gleich.pt;

import java.util.HashSet;
import java.util.ListIterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.population.io.StreamingPopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import playground.gleich.utilsFromOthers.jbischoff.JbUtils;

/**
 * Splits pt legs which depart or end within an area specified by an shp-file
 * into a trip within that area to the next railway station (or any other
 * Coords-Set given) and a teleported pt leg to the origin/destination outside
 * that area.
 * 
 * Assumes that plans are not routed and that there are no pt/walk/etc.
 * interaction activities. Assumes that population, shape file and station
 * coords are in the same Coordinate Reference System.
 * 
 * @author vsp-gleich
 *
 */
public class SplitPtTripsAtStation {

	private static final Logger log = Logger.getLogger(SplitPtTripsAtStation.class);
	private String inputPopulationPath;
	private String studyAreaShpPath;
	private String studyAreaShpKey;
	private String studyAreaShpElement;
	private String outputPopulationPath;
	private Scenario inputScenario;
	private String oldMode;
	private String newMode;
	private String interchangeActivityType;
	private Set<Coord> stationsInArea = new HashSet<>();
	private Geometry geometryStudyArea;

	public static void main(String[] args) {
		SplitPtTripsAtStation tripSplitter;

		if (args.length == 9) {
			String inputPopulationPath = args[0];
			String studyAreaShpPath = args[1];
			String studyAreaShpKey = args[2];
			String studyAreaShpElement = args[3];
			String outputPopulationPath = args[4];
			String oldMode = args[5];
			String newMode = args[6];
			String interchangeActivityType = args[7];
			Set<Coord> stationsInArea = new HashSet<>();
			// TODO: station coords
			tripSplitter = new SplitPtTripsAtStation(inputPopulationPath, studyAreaShpPath, studyAreaShpKey,
					studyAreaShpElement, outputPopulationPath, oldMode, newMode, interchangeActivityType,
					stationsInArea);
		} else {
			log.error("Wrong number of command line arguments. Using defaults");
			String inputPopulationPath = "data/dropboxshare/MATSimData/scenario/NorthSumidaV1/plans_TokyoGenPopV1_10pct_Sumida_acts.xml.gz";
			String studyAreaShpPath = "data/dropboxshare/GISdata/AdminBoundary/NorthSumida.shp";
			String studyAreaShpKey = "Id";
			String studyAreaShpElement = "0";
			String outputPopulationPath = "data/dropboxshare/MATSimData/scenario/NorthSumidaV1/plans_TokyoGenPopV1_10pct_Sumida_acts_tripsCutAtStations.xml.gz";
			String oldMode = "pt";
			String newMode = "walk_Sumida";
			String interchangeActivityType = "railway station";
			Set<Coord> stationsInAreaWGS84 = new HashSet<>();
//			stationsInAreaWGS84.add(CoordUtils.createCoord(139.8091837, 35.7104065)); // TOKYO SKYTREE, no direct city center service
			stationsInAreaWGS84.add(CoordUtils.createCoord(139.8134123, 35.7107458)); // Oshiage ‘SKYTREE’
//			stationsInAreaWGS84.add(CoordUtils.createCoord(139.8275533, 35.7103615)); // Omurai, no direct city center service
//			stationsInAreaWGS84.add(CoordUtils.createCoord(139.8318015, 35.7070671)); // Higashi-azuma, no direct city center service
			stationsInAreaWGS84.add(CoordUtils.createCoord(139.8167501, 35.7183056)); // Hikifune
			stationsInAreaWGS84.add(CoordUtils.createCoord(139.8193204, 35.724279)); // Higashi-mukojima
			stationsInAreaWGS84.add(CoordUtils.createCoord(139.8201376, 35.7333722)); // Kanegafuchi
			stationsInAreaWGS84.add(CoordUtils.createCoord(139.820412, 35.7187273)); // Keisei Hikifune
			stationsInAreaWGS84.add(CoordUtils.createCoord(139.8283287, 35.7271367)); // Yahiro

			// EPSG:6677 is mentioned in other code, but in config only Atlantis
			CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84,
					"EPSG:6677");
			Set<Coord> stationsInArea = new HashSet<>();
			for (Coord coord : stationsInAreaWGS84) {
				stationsInArea.add(ct.transform(coord));
			}

			tripSplitter = new SplitPtTripsAtStation(inputPopulationPath, studyAreaShpPath, studyAreaShpKey,
					studyAreaShpElement, outputPopulationPath, oldMode, newMode, interchangeActivityType,
					stationsInArea);
			tripSplitter.run();
			
			// Run another time to cut car trips
			inputPopulationPath = "data/dropboxshare/MATSimData/scenario/NorthSumidaV1/plans_TokyoGenPopV1_10pct_Sumida_acts_tripsCutAtStations.xml.gz";
			studyAreaShpPath = "data/dropboxshare/GISdata/AdminBoundary/NorthSumida.shp";
			studyAreaShpKey = "Id";
			studyAreaShpElement = "0";
			outputPopulationPath = "data/dropboxshare/MATSimData/scenario/NorthSumidaV1/plans_TokyoGenPopV1_10pct_Sumida_acts_tripsCutAtStationsGarages.xml.gz";
			oldMode = "car";
			newMode = "walk_Sumida";
			interchangeActivityType = "carfree zone garage";
			stationsInAreaWGS84 = new HashSet<>();
			stationsInAreaWGS84.add(CoordUtils.createCoord(139.8057582, 35.7128790)); // near TOKYO SKYTREE, where Mitsume-dori and Mito Kaido intersect
			stationsInAreaWGS84.add(CoordUtils.createCoord(139.8288762, 35.7112076)); // near Omurai where Meiji-dori and Maruhachi-dori branch off
			stationsInAreaWGS84.add(CoordUtils.createCoord(139.8130979, 35.7276032)); // near Higashi-mukojima where Meiji-dori and Bokutei-dori intersect
			stationsInAreaWGS84.add(CoordUtils.createCoord(139.8250638, 35.7286255)); // near Yahiro where Mito Kaido and Kanegafuchi-dori intersect

			// EPSG:6677 is mentioned in other code, but in config only Atlantis
			ct = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84,
					"EPSG:6677");
			stationsInArea = new HashSet<>();
			for (Coord coord : stationsInAreaWGS84) {
				stationsInArea.add(ct.transform(coord));
			}

			tripSplitter = new SplitPtTripsAtStation(inputPopulationPath, studyAreaShpPath, studyAreaShpKey,
					studyAreaShpElement, outputPopulationPath, oldMode, newMode, interchangeActivityType,
					stationsInArea);
		}
		tripSplitter.run();
	}

	public SplitPtTripsAtStation(String inputPopulationPath, String studyAreaShpPath, String studyAreaShpKey,
			String studyAreaShpElement, String outputPopulationPath, String oldMode, String newMode,
			String interchangeActivityType, Set<Coord> stationsInArea) {
		this.inputPopulationPath = inputPopulationPath;
		this.studyAreaShpPath = studyAreaShpPath;
		this.studyAreaShpKey = studyAreaShpKey;
		this.studyAreaShpElement = studyAreaShpElement;
		this.outputPopulationPath = outputPopulationPath;
		this.oldMode = oldMode;
		this.newMode = newMode;
		this.interchangeActivityType = interchangeActivityType;
		this.stationsInArea = stationsInArea;
	}

	public void run() {
		initialize();
		System.out.println("initialize done");
		StreamingPopulationWriter popWriter = new StreamingPopulationWriter();
		popWriter.writeStartPlans(outputPopulationPath);

		StreamingPopulationReader spr = new StreamingPopulationReader(inputScenario);
		spr.addAlgorithm(new PersonAlgorithm() {

			@Override
			public void run(Person person) {
				Plan selectedPlan = person.getSelectedPlan();
				ListIterator<? extends Plan> it = person.getPlans().listIterator();
				while (it.hasNext()) {
					it.next();
					it.remove();
				}
				person.addPlan(selectedPlan);
				if (selectedPlan.getPlanElements().size() < 3) {
					log.warn("Agent " + person.getId().toString() + " has plan of less than 3 elements, skipping.");
					return;
				}

				ListIterator<PlanElement> iterator = selectedPlan.getPlanElements().listIterator();
				Activity lastActivity = (Activity) iterator.next();

				while (iterator.hasNext()) {
					PlanElement pe = iterator.next();
					if (pe instanceof Activity) {
						lastActivity = (Activity) pe;
					}
					if (pe instanceof Leg) {
						Leg leg = (Leg) pe;
						if (leg.getMode().equals(oldMode)) {
							// look at preceding (lastActivity) and following activity location
							Activity followAct = (Activity) iterator.next(); // iterator at following real activity
							if (isActivityInArea(lastActivity)) {
								if (isActivityInArea(followAct)) {
									// leg completely within area -> only change leg mode
									leg.setMode(newMode);
								} else {
									// leg originates within but ends outside area -> split
									Coord nearestStationCoord = findNearestCoord(lastActivity.getCoord());
									// go 2 elements back to activity before this leg
									iterator.previous();
									iterator.previous();
									// add a new leg of newMode (within the study area) after previous real
									// activity, before original leg
									Leg accessLegInArea = PopulationUtils.createLeg(newMode);
									iterator.add(accessLegInArea);
									// add new activity at right position (after new leg, before original leg
									Activity interchangeAct = PopulationUtils.createActivityFromCoord(interchangeActivityType, nearestStationCoord);
									interchangeAct.setMaximumDuration(0);
									iterator.add(interchangeAct);
									// (keep original leg for travel out of study area)
									// move iterator to position of 2nd leg for while loop (loop will continue with next real activity)
									iterator.next();
								}
							} else if (isActivityInArea(followAct)) {
								// leg originates outside area but ends within area -> split
								Coord nearestStationCoord = findNearestCoord(followAct.getCoord());
								// (keep original leg for travel out of study area)
								// go 1 element back to original leg
								iterator.previous();
								// add new activity at right position (after before original leg, before new leg)
								Activity interchangeAct = PopulationUtils.createActivityFromCoord(interchangeActivityType,
										nearestStationCoord);
								interchangeAct.setMaximumDuration(0);
								iterator.add(interchangeAct);
								// add a new leg of newMode (within the study area) after new interchange
								// activity, before following real activity
								Leg egressLegInArea = PopulationUtils.createLeg(newMode);
								iterator.add(egressLegInArea);
							} else {
								// leg originates and ends outside area, keep it as it is
							}
						}
					}
				}
				popWriter.writePerson(person);
			}

		});
		spr.readFile(inputPopulationPath);
		popWriter.writeEndPlans();
		System.out.println("SplitPtTripsAtStation done");
	}

	private void initialize() {
		inputScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		geometryStudyArea = JbUtils.readShapeFileAndExtractGeometry(studyAreaShpPath, studyAreaShpKey)
				.get(studyAreaShpElement);
	}

	private boolean isActivityInArea(Activity act) {
		Coord coord = act.getCoord();
		if (geometryStudyArea.contains(MGC.coord2Point(coord))) {
			return true;
		} else {
			return false;
		}
	}

	private Coord findNearestCoord(Coord destinationCoord) {
		Coord nearestCoord = null;
		double nearestCoordDistance = Double.MAX_VALUE;
		for (Coord coord : stationsInArea) {
			double distance = CoordUtils.calcEuclideanDistance(destinationCoord, coord);
			if (distance < nearestCoordDistance) {
				nearestCoordDistance = distance;
				nearestCoord = coord;
			}
		}
		return nearestCoord;
	}

}
