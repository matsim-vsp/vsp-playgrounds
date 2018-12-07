package playground.mdziakowski.ODMatrixBerlin;

import java.io.BufferedWriter;
import java.io.IOException;
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
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class ShapeReader {

	public static void main(String[] args) throws IOException {

		String shapeFile = "D:\\Arbeit\\Berlin\\Weitere Aufgaben\\Bezirke shp\\dhdn gk4\\Bezirke_GK4.shp";
		String outFile = "D:/Arbeit/Berlin/TripListe.csv";
		String plans1pct = "D:/Arbeit/Berlin/git/scenarios/berlin-v5.2-1pct/output-berlin-v5.2-1pct_2018-09-04/berlin-v5.2-1pct.output_plans.xml.gz";
		
		Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(shapeFile);

		Map<String, Geometry> zones = new HashMap<>();

		for (SimpleFeature feature : features) {
			String id = (String) feature.getAttribute("Name");
			System.out.println(id);
			Geometry geometry = (Geometry) feature.getDefaultGeometry();
			zones.put(id, geometry);
		}
	
		Config config = ConfigUtils.createConfig();
		config.plans().setInputFile(plans1pct);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		Population population = scenario.getPopulation();
	
		BufferedWriter writer = IOUtils.getBufferedWriter(outFile);
		writer.write("person;start;ziel;mode;activity;endtime;starttime;traveltime");
		writer.newLine();
		
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
					String activity = null;
					if (!(act.getType().contains("interaction"))) {
						if (act.getType().contains("home")) {
							activity = "home";
						} else if (act.getType().contains("work")){
							activity = "work";
						} else if (act.getType().contains("leisure")){
							activity = "leisure";
						} else if (act.getType().contains("other")){
							activity = "other";
						} else if (act.getType().contains("shopping")){
							activity = "shopping";
						} else if (act.getType().contains("freight")){
							activity = "freight";
						}
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
							if (mode == null) {
								mode = "walk";
							}
//							System.out.println(person.getId() + ";" + startdistrict + ";" + enddistrict + ";" + mode
//									+ ";" + act.getType() + ";" + clockTime + ";" + movingclockTime + ";" + travelTime);
							writer.write(person.getId() + ";" + startdistrict + ";" + enddistrict + ";" + mode + ";"
									+ activity + ";" + clockTime + ";" + movingclockTime + ";" + travelTime);
							writer.newLine();
							writer.flush();
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
						if (leg.getMode().equals("transit_walk")) {
							mode = "walk";
						}
					}	
					travelTime = travelTime + leg.getRoute().getTravelTime();
					movingclockTime = movingclockTime + leg.getRoute().getTravelTime();
				}
			}
			// break;
		}
		writer.close();
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
