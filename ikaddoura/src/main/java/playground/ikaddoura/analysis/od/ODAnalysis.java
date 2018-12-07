/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
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

package playground.ikaddoura.analysis.od;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
* @author ikaddoura
*/
public final class ODAnalysis {
	
	private static final Logger log = Logger.getLogger(ODAnalysis.class);
	
	private final String shapeFile = "public-svn/matsim/scenarios/countries/de/berlin/projects/avoev/berlin-sav-v5.2-10pct/input/shp-berlin-hundekopf-areas/berlin_hundekopf.shp";
	private final String runId = "berlin-v5.2-10pct";
	private final String runDirectory = "public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.2-10pct/output-berlin-v5.2-10pct/";
	
	private final StageActivityTypes stageActivities = new StageActivityTypesImpl("pt interaction", "car interaction", "ride interaction", "drt interaction");
	private final String zoneId = "SCHLUESSEL";

	private final String analysisOutputDirectory = "od-analysis/";

	private final AvoevMainModeIdentifierImpl mainModeIdentifier = new AvoevMainModeIdentifierImpl();
//	private final MainModeIdentifierImpl mainModeIdentifier = new MainModeIdentifierImpl();

	private final String rootDirectory;

	private final String mode = TransportMode.drt;
	
	public ODAnalysis(String rootDirectory) {
		this.rootDirectory = rootDirectory;
	}

	public static void main(String[] args) throws IOException {
		
		String rootDirectory = null;
		
		if (args.length == 1) {
			rootDirectory = args[0];
		} else {
			throw new RuntimeException("Please set the root directory. Aborting...");
		}
		
		if (!rootDirectory.endsWith("/")) rootDirectory = rootDirectory + "/";
		
		ODAnalysis reader = new ODAnalysis(rootDirectory);
		reader.run();
	}

	private void run() throws IOException {
		
		File file = new File(rootDirectory + runDirectory + analysisOutputDirectory);
		file.mkdirs();
		
		Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(rootDirectory + shapeFile);

		Map<String, Geometry> zones = new HashMap<>();

		for (SimpleFeature feature : features) {
			String id = (String) feature.getAttribute(zoneId);
			Geometry geometry = (Geometry) feature.getDefaultGeometry();
			zones.put(id, geometry);
		}
	
		Config config = ConfigUtils.createConfig();
		config.plans().setInputFile(rootDirectory + runDirectory + runId + ".output_plans.xml.gz");
		config.plans().setInputCRS("GK4");	
		config.global().setCoordinateSystem("GK4");
				
		int noDepTimeCounter = 0;
		int tripCounter = 0;

		List<ODTrip> odTrips = new ArrayList<>();
		
		for (Person person : ScenarioUtils.loadScenario(config).getPopulation().getPersons().values()) {
			
			for (Trip trip : TripStructureUtils.getTrips(person.getSelectedPlan(), stageActivities)) {
				tripCounter++;

				ODTrip odTrip = new ODTrip();
				odTrip.setPersonId(person.getId());
				odTrip.setOrigin(getDistrictId(zones, trip.getOriginActivity().getCoord()));
				odTrip.setDestination(getDistrictId(zones, trip.getDestinationActivity().getCoord()));
				odTrip.setMode(mainModeIdentifier.identifyMainMode(trip.getTripElements()));
				
				double departureTime = 0.;
				if (trip.getLegsOnly().get(0).getDepartureTime() >= 0.) {
					departureTime = trip.getLegsOnly().get(0).getDepartureTime();	
					
				} else if (trip.getOriginActivity().getEndTime() >= 0.) {
					departureTime = trip.getOriginActivity().getEndTime();
					
				} else {
					noDepTimeCounter++;
				}
				
				odTrip.setDepartureTime(departureTime);
				odTrips.add(odTrip);
			}
		}
		
		log.info("Total number of trips: " + tripCounter );
		if (noDepTimeCounter > 0) log.warn("Trips without departure time: " + noDepTimeCounter );
		// If this becomes a problem we could either analyze events or estimate the departure times based on activity end time and travel times.
			
		Map<Integer, Map<String, ODRelation>> hour2odRelations = new HashMap<>();
		
		// hourly data, qgis time manager plugin can't handle time >= 24 * 3600.
		for (int hour = 1; hour <= 23; hour++) {
			
			Map<String, ODRelation> odRelations = new HashMap<>();

			for (ODTrip trip : odTrips) {
				if (new TripFilter((hour - 1) * 3600., hour * 3600., "", mode ).considerTrip(trip)) {
					
					String od = trip.getOrigin() + "-" + trip.getDestination();

					if (odRelations.get(od) == null) {
						odRelations.put(od, new ODRelation(od, trip.getOrigin(), trip.getDestination()));
					} else {
						int tripsSoFar = odRelations.get(od).getTrips();
						odRelations.get(od).setTrips(tripsSoFar + 1);
					}
				} else {
					// skip trip
				}
			}
			
			writeData(odRelations, zones, rootDirectory + runDirectory + analysisOutputDirectory + "od-analysis-matrix_" + hour + "_" + mode + ".csv");
			
			Map<Integer, Map<String, ODRelation>> hourlyOdRelations = new HashMap<>();
			hourlyOdRelations.put(hour, odRelations);
			printODLines(hourlyOdRelations, zones, rootDirectory + runDirectory + analysisOutputDirectory + "od-analysis-matrix_" + hour + "_" + mode + ".shp");
			
			hour2odRelations.put(hour, odRelations);
		}
		
		printODLines(hour2odRelations, zones, rootDirectory + runDirectory + analysisOutputDirectory + "od-analysis-matrix_" + mode + ".shp");
		
	}

	private void writeData(Map<String, ODRelation> odRelations, Map<String, Geometry> zones,  String fileName) throws IOException {
		BufferedWriter writer = IOUtils.getBufferedWriter(fileName);
		writer.write("from/to;");
		
		List<String> zoneIds = new ArrayList<>();
		zoneIds.addAll(zones.keySet());	
		zoneIds.add("other");
		
		for (String zone : zoneIds) {
			writer.write(zone + ";");
		}
		writer.newLine();
		
		int tripsInMatrixCounter = 0;
		for (String zoneFrom : zoneIds) {
			writer.write(zoneFrom + ";");
			for (String zoneTo : zoneIds) {
				
				int trips = 0;
				if (odRelations.get(zoneFrom + "-" + zoneTo) != null) {
					trips = odRelations.get(zoneFrom + "-" + zoneTo).getTrips();
					tripsInMatrixCounter = tripsInMatrixCounter + trips;
				}
				writer.write(trips + ";");
			}
			writer.newLine();
		}
		
		writer.close();
		
		log.info("Matrix written to file.");
		log.info("Total number of trips in Matrix: " + tripsInMatrixCounter);
	}

	private String getDistrictId(Map<String, Geometry> districts, Coord coord) {
		Point point = MGC.coord2Point(coord);
		for (String nameDistrict : districts.keySet()) {
			Geometry geo = districts.get(nameDistrict);
			if (geo.contains(point)) {
				return nameDistrict;
			}
		}
		return "other";
	}
	
	private void printODLines(Map<Integer, Map<String, ODRelation>> hour2odRelations, Map<String, Geometry> zones, String fileName) throws IOException {
        
		PolylineFeatureFactory factory = new PolylineFeatureFactory.Builder()
        		.setCrs(MGC.getCRS(TransformationFactory.DHDN_GK4))
        		.setName("OD")
        		.addAttribute("OD_ID", String.class)
        		.addAttribute("O", String.class)
        		.addAttribute("D", String.class)
        		.addAttribute("trips", Integer.class)
        		.addAttribute("startTime", String.class)
        		.addAttribute("endTime", String.class)
        		.create();
        		
        		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();

        for (Integer hour : hour2odRelations.keySet() ) {
        	 
        	Map<String, ODRelation> odRelations = hour2odRelations.get(hour);
        	
			for (String od : odRelations.keySet()) {
             	
             	if (odRelations.get(od) != null) {
                 			
             		Coordinate originCoord;
             		if (odRelations.get(od).getOrigin().equals("other")) {
             			originCoord = new Coordinate(4628657,5803010);
                 	} else {
                 		originCoord = zones.get(odRelations.get(od).getOrigin()).getCentroid().getCoordinate();
                 	}
                 	
             		Coordinate destinationCoord;
                 	if (odRelations.get(od).getDestination().equals("other")) {
                 		destinationCoord = new Coordinate(4628657,5803010);
                 		} else {
                    			destinationCoord = zones.get(odRelations.get(od).getDestination()).getCentroid().getCoordinate();
                 		}

                 		SimpleFeature feature = factory.createPolyline(
             						
                        		new Coordinate[] {
             						new Coordinate(originCoord),
             						new Coordinate(destinationCoord) }
             					
             					, new Object[] { od, odRelations.get(od).getOrigin(), odRelations.get(od).getDestination(), odRelations.get(od).getTrips(), Time.writeTime((hour-1) * 3600.), Time.writeTime(hour * 3600.) }
                        		, null
             			);	
             		features.add(feature);                			
                 }                		
             }
        }
		
		if (!features.isEmpty()) {
			ShapeFileWriter.writeGeometries(features, fileName);
		} else {
			log.warn("Shape file was not written out.");
		}
	}

}
