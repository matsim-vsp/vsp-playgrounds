package playground.lu.targetLinkSelection;

import static com.google.common.base.Preconditions.checkArgument;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.analysis.zonal.DrtZone;
import org.matsim.contrib.drt.analysis.zonal.DrtZoneTargetLinkSelector;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

public class PredeterminedPointTargetLinkSelector implements DrtZoneTargetLinkSelector {
	private static final Logger log = Logger.getLogger(PredeterminedPointTargetLinkSelector.class);
	private final Map<DrtZone, Link> targetLinks = new HashMap<>();
	private final DrtZonalSystem drtZonalSystem;
	private final String defaultPath = "C:\\Users\\cluac\\MATSimScenarios\\Vulkaneifel\\ZonalSystem\\RequestClusterTargetPoints.shp";

	public PredeterminedPointTargetLinkSelector(DrtZonalSystem drtZonalSystem, String pathToPredeterminedPoints) {
		this.drtZonalSystem = drtZonalSystem;
		if (pathToPredeterminedPoints == null) {
			pathToPredeterminedPoints = defaultPath;
		}
		List<Point> targetPoints = getTargetPoints(pathToPredeterminedPoints);

		for (DrtZone zone : drtZonalSystem.getZones().values()) {
			boolean targetLinkFound = false;
			for (Point targetPoint : targetPoints) {
				if (targetPoint.within(zone.getPreparedGeometry().getGeometry())) {
					Coord targetCoord = MGC.point2Coord(targetPoint);
					Optional<Link> targetLink = zone.getLinks().stream().min(
							Comparator.<Link>comparingDouble(link -> squaredDistance(targetCoord, link.getCoord())));
					targetLinks.put(zone, targetLink.orElseThrow());
					targetLinkFound = true;
				}
			}
			checkArgument(targetLinkFound, "No target point within the zone " + zone.getId()
					+ ". Target link cannot be generated for that zone. Aborting...");
		}
	}

	private double squaredDistance(Coord targetCoord, Coord linkCoord) {
		double deltaX = targetCoord.getX() - linkCoord.getX();
		double deltaY = targetCoord.getY() - linkCoord.getY();
		return deltaX * deltaX + deltaY * deltaY;
	}

	@Override
	public Link selectTargetLink(DrtZone zone) {
		return this.targetLinks.get(zone);
	}

	private List<Point> getTargetPoints(String pathToPredeterminedPoints) {
		List<Point> targetPoints = new ArrayList<>();
		Collection<SimpleFeature> features = getFeatures(pathToPredeterminedPoints);
		if (features.size() != drtZonalSystem.getZones().keySet().size()) {
			log.warn("The number of desingated points (" + features.size() + ") is not equal to the number of zones ("
					+ drtZonalSystem.getZones().keySet().size() + ")");
		}

		for (SimpleFeature simpleFeature : features) {
			Geometry targetPoint = (Geometry) simpleFeature.getDefaultGeometry();
			checkArgument(targetPoint instanceof Point,
					"There is a targetPoint that is not an instance of Point. Aborting...");
			targetPoints.add((Point) targetPoint);
		}
		return targetPoints;
	}

	private Collection<SimpleFeature> getFeatures(String pathToShapeFile) {
		if (pathToShapeFile != null) {
			Collection<SimpleFeature> features;
			if (pathToShapeFile.startsWith("http")) {
				URL shapeFileAsURL = null;
				try {
					shapeFileAsURL = new URL(pathToShapeFile);
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
				features = ShapeFileReader.getAllFeatures(shapeFileAsURL);
			} else {
				features = ShapeFileReader.getAllFeatures(pathToShapeFile);
			}
			return features;
		} else {
			return null;
		}
	}

}
