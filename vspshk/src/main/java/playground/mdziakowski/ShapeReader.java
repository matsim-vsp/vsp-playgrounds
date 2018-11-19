package playground.mdziakowski;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class ShapeReader {

	public static void main(String[] args) {

		String shapeFile = "D:\\Arbeit\\Berlin\\Weitere Aufgaben\\Bezirke shp\\dhdn gk4\\Bezirke_GK4.shp";

		Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(shapeFile);

		Map<String, Geometry> zones = new HashMap<>();

		for (SimpleFeature feature : features) {
			String id = (String) feature.getAttribute("Name");
			System.out.println(id);
			Geometry geometry = (Geometry) feature.getDefaultGeometry();
			zones.put(id, geometry);
		}

		String plans1pct = "D:/Arbeit/Berlin/git/scenarios/berlin-v5.2-1pct/output-berlin-v5.2-1pct_2018-09-04/berlin-v5.2-1pct.output_plans.xml.gz";

		Config config = ConfigUtils.createConfig();
		config.plans().setInputFile(plans1pct);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		Population population = scenario.getPopulation();

		for (Person person : population.getPersons().values()) {
			Plan selectedPlan = person.getSelectedPlan();

			boolean anfang = false;
			boolean ende = false;

			String startdistrict = null;
			String enddistrict = null;
			String mode = null;

			double clockTime = 0.0;
			double movingclockTime = 0.0;
			double travelTime = 0.0;

			for (PlanElement pe : selectedPlan.getPlanElements()) {

				if (pe instanceof Activity) {
					Activity act = (Activity) pe;
					if (!(act.getType().contains("interaction"))) {
						if (!(anfang || ende)) {
							startdistrict = inDistirct(zones, act.getCoord());
							anfang = true;
							clockTime = act.getEndTime();
							movingclockTime = act.getEndTime();
						} else if (anfang && !(ende)) {
							enddistrict = inDistirct(zones, act.getCoord());
							ende = true;
						}

						if (anfang && ende) {
//							System.out.println(person.getId() + ";" + startdistrict + ";" + enddistrict + ";" + mode
//									+ ";" + act.getType() + ";" + clockTime + ";" + movingclockTime + ";" + travelTime);
							clockTime = movingclockTime;
							startdistrict = enddistrict;
							travelTime = 0.0;
							ende = false;
							if (act.getEndTime() > 0) {
								clockTime = act.getEndTime();
								movingclockTime = act.getEndTime();
							}
							if (act.getMaximumDuration() > 0) {
								clockTime = clockTime + act.getMaximumDuration();
								movingclockTime = movingclockTime + act.getMaximumDuration();
							}
						}
					}
				}

				if (pe instanceof Leg) {
					Leg leg = (Leg) pe;
					if (!(leg.getMode().equals("access_walk") || leg.getMode().equals("egress_walk")
							|| leg.getMode().equals("transit_walk"))) {
						mode = leg.getMode();
					}
					travelTime = travelTime + leg.getRoute().getTravelTime();
					movingclockTime = movingclockTime + leg.getRoute().getTravelTime();
				}
			}
			// break;
		}
		System.out.println("Done");

	}

	private static String inDistirct(Map<String, Geometry> districts, Coord coord) {
		Point point = MGC.coord2Point(coord);
		for (String nameDistrict : districts.keySet()) {
			Geometry geo = districts.get(nameDistrict);
			if (geo.contains(point)) {
				return nameDistrict;
			}
		}
		return "außerhalbBerlin";
	}

}
