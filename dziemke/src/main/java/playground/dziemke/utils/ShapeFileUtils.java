package playground.dziemke.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.opengis.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.Geometry;

/**
 * @author dziemke
 */
public class ShapeFileUtils {

	public static Map<String, Geometry> getGeometryMap(Collection<SimpleFeature> features, String attributeLabel) {
		Map<String, Geometry> zoneGeometries = new HashMap<>();
		for (SimpleFeature feature : features) {
			zoneGeometries.put((String) feature.getAttribute(attributeLabel), (Geometry) feature.getDefaultGeometry());
		}
		return zoneGeometries;
	}

	public static Geometry getGeometryByValueOfAttribute(Collection<SimpleFeature> features, String attributeLabel, String value) {
		Map<String, Geometry> zoneGeometries = new HashMap<>();
		for (SimpleFeature feature : features) {
			zoneGeometries.put((String) feature.getAttribute(attributeLabel), (Geometry) feature.getDefaultGeometry());
		}
		return zoneGeometries.get(value);
	}
}