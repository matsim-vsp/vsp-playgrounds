/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.ikaddoura.analysis.moneyGIS;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.PolygonFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * 
 * Reads in a shapeFile and performs a zone-based analysis:
 * Calculates the number of home activities, work activities, all activities per zone.
 * Calculates the congestion costs of the whole day mapped back to the causing agents' home zones.
 * 
 * The shape file has to contain a grid (e.g. squares, hexagons) which can be created using a QGIS plugin called MMQGIS.
 * 
 * @author ikaddoura
 *
 */
public class TollGISAnalyzer {
	
	private final String homeActivity;
	private final int scalingFactor;
		
	private static final Logger log = Logger.getLogger(TollGISAnalyzer.class);
	private Map<Integer, SimpleFeature> features = new HashMap<>();
	private Map<Integer, Geometry> zoneId2geometry = new HashMap<Integer, Geometry>();
	private String zonesCRS;
	private String fileName;
	private CoordinateTransformation ct;

	public TollGISAnalyzer(
			String shapeFileZones,
			int scalingFactor,
			String homeActivity,
			String zonesCRS,
			String scenarioCRS,
			String fileName) {
		
		this.scalingFactor = scalingFactor;
		this.homeActivity = homeActivity;
		this.fileName = fileName;
		
		this.zonesCRS = zonesCRS;
		this.ct = TransformationFactory.getCoordinateTransformation(scenarioCRS, zonesCRS);

		log.info("Reading zone shapefile...");
		int featureCounter = 0;
		for (SimpleFeature feature : ShapeFileReader.getAllFeatures(shapeFileZones)) {
			features.put(featureCounter, feature);
			this.zoneId2geometry.put(featureCounter, (Geometry) feature.getDefaultGeometry());
			featureCounter++;
		}
		log.info("Reading zone shapefile... Done. Number of zones: " + featureCounter);
	}
	
	public void analyzeZoneTollsUserBenefits(Scenario scenario, String runDirectory, Map<Id<Person>, Double> personId2userBenefits,
			Map<Id<Person>, Double> personId2tollPayments,
			Map<Id<Person>, Double> personId2congestionPayments,
			Map<Id<Person>, Double> personId2noisePayments,
			Map<Id<Person>, Double> personId2airPollutionPayments) {
		String outputPath = runDirectory + "spatial_analysis/tolls_userBenefits_zones/";
		
		File file = new File(outputPath);
		file.mkdirs();
				
		// home activities
		log.info("Analyzing Home activities per zone...");
		Map<Integer,Integer> zoneNr2homeActivities = getZoneNr2activityLocations(homeActivity, scenario.getPopulation(), this.zoneId2geometry, this.scalingFactor);
		log.info("Analyzing Home activities per zone... Done.");
		
		// all activities
		log.info("Analyzing all activities per zone...");
		Map<Integer,Integer> zoneNr2activities = getZoneNr2activityLocations(null, scenario.getPopulation(), this.zoneId2geometry, this.scalingFactor);
		log.info("Analyzing all activities per zone... Done.");
		
		// absolute numbers mapped back to home location
		log.info("Mapping absolute toll payments and user benefits to home location...");
		Map<Integer,Double> zoneNr2tollPayments = getZoneNr2totalAmount(scenario.getPopulation(), personId2tollPayments, this.zoneId2geometry, this.scalingFactor);
		
		Map<Integer,Double> zoneNr2congestionPayments = getZoneNr2totalAmount(scenario.getPopulation(), personId2congestionPayments, this.zoneId2geometry, this.scalingFactor);
		Map<Integer,Double> zoneNr2noisePayments = getZoneNr2totalAmount(scenario.getPopulation(), personId2noisePayments, this.zoneId2geometry, this.scalingFactor);
		Map<Integer,Double> zoneNr2airPollutionPayments = getZoneNr2totalAmount(scenario.getPopulation(), personId2airPollutionPayments, this.zoneId2geometry, this.scalingFactor);

		Map<Integer,Double> zoneNr2userBenefits = getZoneNr2totalAmount(scenario.getPopulation(), personId2userBenefits, this.zoneId2geometry, this.scalingFactor);
		log.info("Mapping absolute toll payments and user benefits to home location... Done.");
		
		log.info("Writing shape file...");
		
		PolygonFeatureFactory featureFactory = new PolygonFeatureFactory.Builder().
				setCrs(MGC.getCRS(zonesCRS)).
				setName("zone").
				addAttribute("ID", Integer.class).
				addAttribute("HomeAct", Integer.class).
				addAttribute("AllAct", Integer.class).
				addAttribute("Tolls", Double.class).
				addAttribute("C", Double.class).
				addAttribute("N", Double.class).
				addAttribute("A", Double.class).
				addAttribute("Scores", Double.class).
				create();
		
		Collection<SimpleFeature> featuresToWriteOut = new ArrayList<SimpleFeature>();

		for (Integer id : features.keySet()) {
			Map<String, Object> attributeValues = new HashMap<>();
			attributeValues.put("ID", id);
			attributeValues.put("HomeAct", zoneNr2homeActivities.get(id));
			attributeValues.put("AllAct", zoneNr2activities.get(id));		
			attributeValues.put("Tolls", zoneNr2tollPayments.get(id));
			attributeValues.put("C", zoneNr2congestionPayments.get(id));
			attributeValues.put("N", zoneNr2noisePayments.get(id));
			attributeValues.put("A", zoneNr2airPollutionPayments.get(id));
			attributeValues.put("Scores", zoneNr2userBenefits.get(id));		

			Geometry geometry = (Geometry) features.get(id).getDefaultGeometry();
			Coordinate[] coordinates = geometry.getCoordinates();
			
			SimpleFeature feature = featureFactory.createPolygon(coordinates, attributeValues, Integer.toString(id));
			featuresToWriteOut.add(feature);
		}
		
		ShapeFileWriter.writeGeometries(featuresToWriteOut, outputPath + scenario.getConfig().controler().getLastIteration() + "." + fileName);

		log.info("Writing shape file... Done.");
		
	}

	private Map<Integer, Double> getZoneNr2totalAmount(Population population, Map<Id<Person>, Double> personId2amountSum, Map<Integer, Geometry> zoneId2geometry, int scalingFactor) {

		Map<Integer, Double> zoneNr2totalAmount = new HashMap<Integer, Double>();	
		
		for (Integer zoneId : zoneId2geometry.keySet()) {
			zoneNr2totalAmount.put(zoneId, 0.);
		}
		
		SortedMap<Id<Person>,Coord> personId2homeCoord = getPersonId2Coordinates(population, homeActivity);
		
		for (Id<Person> personId : personId2amountSum.keySet()) {
			if (personId2homeCoord.containsKey(personId)){
				for (Integer zoneId : zoneId2geometry.keySet()) {
					Geometry geometry = zoneId2geometry.get(zoneId);
					Point p = MGC.coord2Point(ct.transform(personId2homeCoord.get(personId))); 
					
					if (p.within(geometry)){
						if (zoneNr2totalAmount.get(zoneId) == null){
							zoneNr2totalAmount.put(zoneId, personId2amountSum.get(personId) * scalingFactor);
						} else {
							double tollPayments = zoneNr2totalAmount.get(zoneId);
							zoneNr2totalAmount.put(zoneId, tollPayments + (personId2amountSum.get(personId) * scalingFactor) );
						}
					}
				}
			} else {
				// person doesn't have a home activity
			}
		}
		
		return zoneNr2totalAmount;
	}

	private Map<Integer, Integer> getZoneNr2activityLocations(String activity, Population population, Map<Integer, Geometry> zoneNr2zoneGeometry, int scalingFactor) {
		Map<Integer, Integer> zoneNr2activity = new HashMap<Integer, Integer>();	

		for (Integer zoneId : zoneId2geometry.keySet()) {
			zoneNr2activity.put(zoneId, 0);
		}
		
		SortedMap<Id<Person>,Coord> personId2activityCoord = getPersonId2Coordinates(population, activity);
		
		for (Coord coord : personId2activityCoord.values()) {
			for (Integer nr : zoneNr2zoneGeometry.keySet()) {
				Geometry geometry = zoneNr2zoneGeometry.get(nr);
				Point p = MGC.coord2Point(ct.transform(coord)); 
				
				if (p.within(geometry)){
					if (zoneNr2activity.get(nr) == null){
						zoneNr2activity.put(nr, scalingFactor);
					} else {
						int activityCounter = zoneNr2activity.get(nr);
						zoneNr2activity.put(nr, activityCounter + scalingFactor);
					}
				}
			}
		}
		return zoneNr2activity;
	}
	
	private SortedMap<Id<Person>, Coord> getPersonId2Coordinates(Population population, String activity) {
		SortedMap<Id<Person>,Coord> personId2coord = new TreeMap<Id<Person>,Coord>();
		
		for(Person person : population.getPersons().values()){
			
			for (PlanElement pE : person.getSelectedPlan().getPlanElements()){
				
				if (pE instanceof Activity){
					Activity act = (Activity) pE;
					
					if (act.getType().equals(activity) || activity == null) {
						
						Coord coord = act.getCoord();
						personId2coord.put(person.getId(), coord);
					
					} else {
						//  other activity type
					}
				}
			}
		}
		return personId2coord;
	}
}
