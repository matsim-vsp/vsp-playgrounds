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

public class BerlinDistricts {

	private static String inDistirct(Map<String, Collection<SimpleFeature>> districts, Coord coord) {
		Point point = MGC.coord2Point(coord);
		for (String nameDistrict : districts.keySet()) {
			for (SimpleFeature sf : districts.get(nameDistrict)) {
				if (((Geometry) sf.getDefaultGeometry()).contains(point)) {
					return nameDistrict;
				}
			}
		}
		return "außerhalbBerlin";
	}

	public static void main(String[] args) throws IOException {

		String outFile = "D:/Arbeit/Berlin/Matrix10.csv";
//		String outFile = "D:/Arbeit/Berlin/Matrix.txt";

		// load districts
		String shapesLocation = "D:/Arbeit/Berlin/Weitere Aufgaben/Bezirke shp/dhdn gk4/";

		String charlottenburgWilmersdorf = shapesLocation
				+ "Charlottenburg-Wilmersdorf/Name_Charlottenburg-Wilmersdorf.shp";
		String friedrichshainKreuzberg = shapesLocation + "Friedrichshain-Kreuzberg/Name_Friedrichshain-Kreuzberg.shp";
		String lichtenberg = shapesLocation + "Lichtenberg/Name_Lichtenberg.shp";
		String marzahnHellersdorf = shapesLocation + "Marzahn-Hellersdorf/Name_Marzahn-Hellersdorf.shp";
		String mitte = shapesLocation + "Mitte/Name_Mitte.shp";
		String neukölln = shapesLocation + "Neukölln/Name_Neukölln.shp";
		String pankow = shapesLocation + "Pankow/Name_Pankow.shp";
		String reinickendorf = shapesLocation + "Reinickendorf/Name_Reinickendorf.shp";
		String spandau = shapesLocation + "Spandau/Name_Spandau.shp";
		String steglitzZehlendorf = shapesLocation + "Steglitz-Zehlendorf/Name_Steglitz-Zehlendorf.shp";
		String tempelhofSchöneberg = shapesLocation + "Tempelhof-Schöneberg/Name_Tempelhof-Schöneberg.shp";
		String treptowKöpenick = shapesLocation + "Treptow-Köpenick/Name_Treptow-Köpenick.shp";

		Map<String, Collection<SimpleFeature>> districts = new HashMap<>();

		districts.put("charlottenburgWilmersdorf", ShapeFileReader.getAllFeatures(charlottenburgWilmersdorf));
		districts.put("friedrichshainKreuzberg", ShapeFileReader.getAllFeatures(friedrichshainKreuzberg));
		districts.put("lichtenberg", ShapeFileReader.getAllFeatures(lichtenberg));
		districts.put("marzahnHellersdorf", ShapeFileReader.getAllFeatures(marzahnHellersdorf));
		districts.put("mitte", ShapeFileReader.getAllFeatures(mitte));
		districts.put("neukölln", ShapeFileReader.getAllFeatures(neukölln));
		districts.put("pankow", ShapeFileReader.getAllFeatures(pankow));
		districts.put("reinickendorf", ShapeFileReader.getAllFeatures(reinickendorf));
		districts.put("spandau", ShapeFileReader.getAllFeatures(spandau));
		districts.put("steglitzZehlendorf", ShapeFileReader.getAllFeatures(steglitzZehlendorf));
		districts.put("tempelhofSchöneberg", ShapeFileReader.getAllFeatures(tempelhofSchöneberg));
		districts.put("treptowKöpenick", ShapeFileReader.getAllFeatures(treptowKöpenick));

		System.out.println(districts);
		
		// load plans

		String plans1pct = "D:\\Arbeit\\Berlin\\svn\\2018-09-04_output-berlin-v5.2-10pct\\berlin-v5.2-10pct.output_plans.xml.gz";
		// String plans10pct =
		// "D:\\Arbeit\\Berlin\\svn\\2018-09-04_output-berlin-v5.2-10pct\\berlin-v5.2-10pct.output_plans.xml.gz";

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
							startdistrict = inDistirct(districts, act.getCoord());
							anfang = true;
							clockTime = act.getEndTime();
							movingclockTime = act.getEndTime();
						} else if (anfang && !(ende)) {
							enddistrict = inDistirct(districts, act.getCoord());
							ende = true;
						}

						if (anfang && ende) {
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
					if (!(leg.getMode().equals("access_walk") || leg.getMode().equals("egress_walk"))) {
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

}
