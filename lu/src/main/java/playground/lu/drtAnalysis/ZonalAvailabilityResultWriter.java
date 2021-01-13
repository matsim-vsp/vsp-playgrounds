package playground.lu.drtAnalysis;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.apache.log4j.Logger;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.matsim.contrib.drt.analysis.zonal.DrtZone;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;

public class ZonalAvailabilityResultWriter implements IterationEndsListener, ShutdownListener {
	private static final Logger log = Logger.getLogger(ZonalAvailabilityHandler.class);
	private final AvailabilityAnalysisHandler availabilityAnalysisHandler;

	public ZonalAvailabilityResultWriter(AvailabilityAnalysisHandler availabilityAnalysisHandler) {
		this.availabilityAnalysisHandler = availabilityAnalysisHandler;
	}

	// Write a csv file at the end of each iteration
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		String fileName = event.getServices().getControlerIO().getIterationFilename(event.getIteration(),
				"zonal_availability_statistics.csv");
		write(fileName);
	}

	// generate a shp file at the end of the simulation
	@Override
	public void notifyShutdown(ShutdownEvent event) {
		String crs = event.getServices().getConfig().global().getCoordinateSystem();
		Collection<SimpleFeature> features = convertGeometriesToSimpleFeatures(crs);
		String fileName = event.getServices().getControlerIO().getOutputFilename("drt_availability_zonal.shp");
		ShapeFileWriter.writeGeometries(features, fileName);

	}

	private void write(String fileName) {
		String delimiter = ",";
		Map<DrtZone, Double> allDayData = availabilityAnalysisHandler.getAllDayAvailabilityRate();
		Map<DrtZone, Double> peakHourData = availabilityAnalysisHandler.getPeakHourAvailabilityRate();

		try {
			FileWriter csvWriter = new FileWriter(fileName);
			csvWriter.append("Zone");
			csvWriter.append(delimiter);
			csvWriter.append("All_day_availability");
			csvWriter.append(delimiter);
			csvWriter.append("Peak_hour_availability");
			csvWriter.append("\n");

			for (DrtZone zone : allDayData.keySet()) {
				csvWriter.append(zone.getId());
				csvWriter.append(delimiter);
				csvWriter.append(Double.toString(allDayData.get(zone)));
				csvWriter.append(delimiter);
				csvWriter.append(Double.toString(peakHourData.get(zone)));
				csvWriter.append("\n");
			}

			csvWriter.flush();
			csvWriter.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private Collection<SimpleFeature> convertGeometriesToSimpleFeatures(String targetCoordinateSystem) {
		SimpleFeatureTypeBuilder simpleFeatureBuilder = new SimpleFeatureTypeBuilder();
		Map<DrtZone, Double> allDayData = availabilityAnalysisHandler.getAllDayAvailabilityRate();
		Map<DrtZone, Double> peakHourData = availabilityAnalysisHandler.getPeakHourAvailabilityRate();

		try {
			simpleFeatureBuilder.setCRS(MGC.getCRS(targetCoordinateSystem));
		} catch (IllegalArgumentException e) {
			log.warn("Coordinate reference system \"" + targetCoordinateSystem
					+ "\" is unknown. Please set a crs in config global. Will try to create drt_availability_zonal.shp anyway.");
		}

		simpleFeatureBuilder.setName("drtZoneFeature");
		// note: column names may not be longer than 10 characters. Otherwise the name
		// is cut after the 10th character and the avalue is NULL in QGis
		simpleFeatureBuilder.add("the_geom", Polygon.class);
		simpleFeatureBuilder.add("zoneIid", String.class);
		simpleFeatureBuilder.add("centerX", Double.class);
		simpleFeatureBuilder.add("centerY", Double.class);
		simpleFeatureBuilder.add("allDay", Double.class);
		simpleFeatureBuilder.add("peakHour", Double.class);
		SimpleFeatureBuilder builder = new SimpleFeatureBuilder(simpleFeatureBuilder.buildFeatureType());

		Collection<SimpleFeature> features = new ArrayList<>();

		for (DrtZone zone : allDayData.keySet()) {
			Object[] routeFeatureAttributes = new Object[6];
			Geometry geometry = zone.getPreparedGeometry() != null ? zone.getPreparedGeometry().getGeometry() : null;
			routeFeatureAttributes[0] = geometry;
			routeFeatureAttributes[1] = zone.getId();
			routeFeatureAttributes[2] = zone.getCentroid().getX();
			routeFeatureAttributes[3] = zone.getCentroid().getY();
			routeFeatureAttributes[4] = allDayData.get(zone);
			routeFeatureAttributes[5] = peakHourData.get(zone);
			try {
				features.add(builder.buildFeature(zone.getId(), routeFeatureAttributes));
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			}
		}

		return features;
	}

}
