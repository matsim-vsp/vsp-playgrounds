/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package playground.tschlenther.generalUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import com.vividsolutions.jts.geom.Coordinate;

public class ActivityPlotter {
	
	private Population population;
	private CoordinateReferenceSystem popCrs;

	private static Logger logger = Logger.getLogger(ActivityPlotter.class);

	public ActivityPlotter(Population pop, CoordinateReferenceSystem crs){
		this.population = pop;
		this.popCrs = crs;
	}
	
	public void writeShapeFile(String filename, CoordinateReferenceSystem targetCrs){
		try {
			MathTransform transformation = CRS.findMathTransform(this.popCrs, targetCrs, true);

			PointFeatureFactory factory = new PointFeatureFactory.Builder().
					setCrs(targetCrs).
					setName("activity").
					addAttribute("person_id", String.class).
					addAttribute("act_type", String.class).
					addAttribute("start_time", Double.class).
					addAttribute("end_time", Double.class).
					create();
			
			List<SimpleFeature> features = new ArrayList<SimpleFeature>();
			SimpleFeature f = null;
			int counter = 0;
			int exponent = 1;
			for (Person p : this.population.getPersons().values()){
				counter ++;
				if(counter % Math.pow(2, exponent) == 0) {
					logger.info("person #" + counter);
					exponent++;
				}
				Plan plan = p.getSelectedPlan();
				for (PlanElement pe : plan.getPlanElements()){
					if (pe instanceof Activity){
						Activity activity = (Activity) pe;
						String id = p.getId().toString();
						String type = activity.getType();
						Double startTime = activity.getStartTime();
						Double endTime = activity.getEndTime();

						Coordinate actCoordinate = MGC.coord2Coordinate(activity.getCoord());
						actCoordinate = JTS.transform(actCoordinate, actCoordinate, transformation);
						
						f = factory.createPoint(actCoordinate, new Object[] {id, type, startTime, endTime}, null);
						features.add(f);
					}
				}
			}
			
			ShapeFileWriter.writeGeometries(features, filename);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
	public void writeCSVFile(String filename, CoordinateReferenceSystem targetCrs) {

		try {
			if(!filename.endsWith(".csv")) throw new IllegalArgumentException("wrong file ending. please use .csv");
			
			MathTransform transformation = CRS.findMathTransform(this.popCrs, targetCrs, true);
	
			BufferedWriter writer = IOUtils.getBufferedWriter(filename);
			int counter = 0;
			int exponent = 2;
			
			writer.write("personID;xCoord;yCoord;actType;startTime;endTime");
			
			for (Person p : this.population.getPersons().values()){
				counter ++;
				if(counter % Math.pow(2, exponent) == 0) {
					logger.info("person #" + counter);
					exponent++;
				}
				Plan plan = p.getSelectedPlan();
				for (PlanElement pe : plan.getPlanElements()){
					if (pe instanceof Activity){
						Activity activity = (Activity) pe;
						String id = p.getId().toString();
						String type = activity.getType();
						if(type.contains("_")) {
							type = type.substring(0, type.indexOf("_"));
						}
						
						Double startTime = activity.getStartTime();
						Double endTime = activity.getEndTime();
	
						Coord actCoordinate = activity.getCoord();
//						Coordinate actCoordinate = MGC.coord2Coordinate(activity.getCoord());
//						actCoordinate = JTS.transform(actCoordinate, actCoordinate, transformation);
						
							writer.newLine();
							writer.write(id + ";" + actCoordinate.getX() + ";" + actCoordinate.getY() + ";" + type + ";" + startTime + ";" + endTime);
					}
				}
			}
			writer.flush();
			writer.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static void main(String[] args) {
		
//		String inputPopulation = "C:/Users/Work/VSP/urbanAtlasBerlin/output/.selected_plans_UrbanAtlas.xml.gz";
		String inputPopulation = "C:/Users/Work/VSP/urbanAtlasBerlin/troubleShooting/plans/be_400_c_10pct_person_freight.tempelhofCut_MODIFIED_corine_100.xml.gz";
	
//		String outputFile = "C:/Users/Work/VSP/urbanAtlasBerlin/output/.selected_plans_UrbanAtlas_shortTypes.csv";
		String outputFile =  "C:/Users/Work/VSP/urbanAtlasBerlin/troubleShooting/plans/be_400_c_10pct_person_freight.tempelhofCut_MODIFIED_corine_100.csv";
		
		if(args.length != 0) {
			inputPopulation = args[0];
			outputFile = args[1];
		}
		
		Scenario urbanAtlasScenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader reader = new PopulationReader(urbanAtlasScenario);
		logger.info("reading " + inputPopulation);
		reader.readFile(inputPopulation);

		ActivityPlotter plotter = new ActivityPlotter(urbanAtlasScenario.getPopulation(), MGC.getCRS("EPSG:3035"));
		logger.info("converting activities and writing them to " + outputFile);
		plotter.writeCSVFile(outputFile, MGC.getCRS("EPSG:25832"));
			
		logger.info("----DONE----");
		
		
	}
}
