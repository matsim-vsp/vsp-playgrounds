package playground.lu.inputPlanPreparation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

public class CountingTripsWithinServiceArea {
	private static final Logger log = Logger.getLogger(CountingTripsWithinServiceArea.class);

	private final static String SHAPEFILE = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.5-10pct/input/berlin-shp/berlin.shp";
	private final static String TRIPS_FILE = "C:\\Users\\cluac\\MATSimScenarios\\Berlin\\Berlin Base Scenario\\Output\\berlin-v5.5-10pct.output_trips.csv";

	public static void main(String[] args) throws IOException {
		log.info("Starting counting trips");
		Geometry serviceArea = generateGeometryFromShapeFile(SHAPEFILE);
		log.info("Start reading trip file");
		
		Map<String, Integer> tripsData = new HashMap<>();
		tripsData.put("car", 0);
		tripsData.put("walk", 0);
		tripsData.put("pt", 0);
		tripsData.put("bicycle", 0);
		tripsData.put("ride", 0);

		String cvsSplitBy = ";";
		String line;

		int counter = 1;

		try (BufferedReader br = new BufferedReader(new FileReader(TRIPS_FILE))) {
			String firstLine = br.readLine(); // the first line is the title, which needs to be skipped
			String[] title = firstLine.split(cvsSplitBy);
			log.info("Checking if the correct data is used: " + title[15] + title[16] + title[19] + title[20]
					+ title[8]);
			while ((line = br.readLine()) != null) {
				String[] trip = line.split(cvsSplitBy);
				Coord startCoord = new Coord(Double.parseDouble(trip[15]), Double.parseDouble(trip[16]));
				Coord endCoord = new Coord(Double.parseDouble(trip[19]), Double.parseDouble(trip[20]));
				String mode = trip[8];
				Point startPoint = MGC.coord2Point(startCoord);
				Point endPoint = MGC.coord2Point(endCoord);
				if (startPoint.within(serviceArea) && endPoint.within(serviceArea)) {
					if (tripsData.keySet().contains(mode)) {
						int newCount = tripsData.get(mode) + 1;
						tripsData.put(mode, newCount);
					} else {
						log.info("A trip with mode of" + mode
								+ "is identified. This mode is not recognized and will not be counted");
					}
				}
				counter += 1;
				if (counter % 5000 == 0) {
					log.info("Processing: " + counter + " trips have been analyzed");
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		log.info("Trips successfully read and analyzed. Writing results...");
		log.info("Car trips: " + tripsData.get("car"));
		log.info("pt trips: " + tripsData.get("pt"));
		log.info("walk trips: " + tripsData.get("walk"));
		log.info("bicycle trips: " + tripsData.get("bicycle"));
		log.info("ride trips: " + tripsData.get("ride"));
		log.info("There are in total " + counter + " trips within the service area");
		FileWriter csvWriter = new FileWriter(
				"C:/Users/cluac/MATSimScenarios/Berlin/Berlin Base Scenario/Output/numTripsWithinServiceArea.csv");
		csvWriter.append("car");
		csvWriter.append(",");
		csvWriter.append("pt");
		csvWriter.append(",");
		csvWriter.append("walk");
		csvWriter.append(",");
		csvWriter.append("bicycle");
		csvWriter.append(",");
		csvWriter.append("ride");
		csvWriter.append(",");
		csvWriter.append("total trips");
		csvWriter.append("\n");

		csvWriter.append(Integer.toString(tripsData.get("car")));
		csvWriter.append(",");
		csvWriter.append(Integer.toString(tripsData.get("pt")));
		csvWriter.append(",");
		csvWriter.append(Integer.toString(tripsData.get("walk")));
		csvWriter.append(",");
		csvWriter.append(Integer.toString(tripsData.get("bicycle")));
		csvWriter.append(",");
		csvWriter.append(Integer.toString(tripsData.get("ride")));
		csvWriter.append(",");
		csvWriter.append(Integer.toString(counter));
		csvWriter.append("\n");

		csvWriter.flush();
		csvWriter.close();

	}

	private static Collection<SimpleFeature> getFeatures(String shapeFile) {
		if (shapeFile != null) {
			Collection<SimpleFeature> features;
			if (shapeFile.startsWith("http")) {
				URL shapeFileAsURL = null;
				try {
					shapeFileAsURL = new URL(shapeFile);
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
				features = ShapeFileReader.getAllFeatures(shapeFileAsURL);
			} else {
				features = ShapeFileReader.getAllFeatures(shapeFile);
			}
			return features;
		} else {
			return null;
		}
	}

	public static Geometry generateGeometryFromShapeFile(String shapefile) {
		log.info("Reading Shapefile for service area");
		Collection<SimpleFeature> features = getFeatures(shapefile);
		if (features.size() < 1) {
			throw new RuntimeException("There is no feature (zone) in the shape file. Aborting...");
		}
		Geometry serviceArea = (Geometry) features.iterator().next().getDefaultGeometry();
		if (features.size() > 1) {
			for (SimpleFeature simpleFeature : features) {
				Geometry subArea = (Geometry) simpleFeature.getDefaultGeometry();
				serviceArea.union(subArea);
			}
		}
		log.info("Service area have been loaded");
		return serviceArea;
	}
}
