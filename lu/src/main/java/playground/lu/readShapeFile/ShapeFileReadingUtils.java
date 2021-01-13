package playground.lu.readShapeFile;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

public class ShapeFileReadingUtils {
	public static Collection<SimpleFeature> getFeatures(String pathToShapeFile) {
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

	public static Geometry getGeometryFromShapeFile(String pathToShapeFile) {
		Collection<SimpleFeature> features = getFeatures(pathToShapeFile);
		if (features.size() < 1) {
			throw new RuntimeException("There is no feature in the shape file. Aborting...");
		}
		Geometry geometry = (Geometry) features.iterator().next().getDefaultGeometry();
		if (features.size() > 1) {
			for (SimpleFeature simpleFeature : features) {
				Geometry subArea = (Geometry) simpleFeature.getDefaultGeometry();
				geometry.union(subArea);
			}
		}
		return geometry;
	}

	public static boolean isLinkWithinGeometry(Network network, String linkIdString, Geometry geometry) {
		Link link = network.getLinks().get(Id.create(linkIdString, Link.class));
		return isCoordWithinGeometry(link.getToNode().getCoord(), geometry);
	}

	public static boolean isLinkWithinGeometry(Network network, Id<Link> linkId, Geometry geometry) {
		Link link = network.getLinks().get(linkId);
		return isCoordWithinGeometry(link.getToNode().getCoord(), geometry);
	}

	public static boolean isCoordWithinGeometry(Coord coord, Geometry geometry) {
		Point point = MGC.coord2Point(coord);
		if (point.within(geometry)) {
			return true;
		}
		return false;
	}

}
