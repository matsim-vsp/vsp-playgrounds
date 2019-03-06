/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.michalm.util.gis;

import java.util.Collection;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.google.common.collect.Iterables;

public class PolygonBasedFilter {
	public static boolean isLinkInsidePolygon(Link link, Geometry polygonGeometry, boolean includeBorderLinks) {
		Point fromPoint = MGC.coord2Point(link.getFromNode().getCoord());
		boolean fromPointInside = polygonGeometry.contains(fromPoint);

		if (fromPointInside && includeBorderLinks) {
			return true;// inclusion of only 1 point is enough
		} else if (!fromPointInside && !includeBorderLinks) {
			return false;// both points must be within
		}

		// now the result depends on the inclusion of "toPoint"
		Point toPoint = MGC.coord2Point(link.getToNode().getCoord());
		return polygonGeometry.contains(toPoint);
	}

	public static Iterable<? extends Link> filterLinksInsidePolygon(Iterable<? extends Link> links,
			Geometry polygonGeometry, boolean includeBorderLinks) {
		return Iterables.filter(links, link -> isLinkInsidePolygon(link, polygonGeometry, includeBorderLinks));
	}

	public static Iterable<? extends Link> filterLinksOutsidePolygon(Iterable<? extends Link> links,
			Geometry polygonGeometry, boolean includeBorderLinks) {
		// includeBorderLinks must be negated
		return Iterables.filter(links, link -> !isLinkInsidePolygon(link, polygonGeometry, !includeBorderLinks));
	}

	public static boolean isFeatureInsidePolygon(SimpleFeature feature, final Geometry polygonGeometry) {
		return polygonGeometry.contains((Geometry)feature.getDefaultGeometry());
	}

	public static Iterable<? extends SimpleFeature> filterFeaturesInsidePolygon(
			Iterable<? extends SimpleFeature> features, Geometry polygonGeometry) {
		return Iterables.filter(features, f -> isFeatureInsidePolygon(f, polygonGeometry));
	}

	public static Iterable<? extends SimpleFeature> filterFeaturesOutsidePolygon(
			Iterable<? extends SimpleFeature> features, Geometry polygonGeometry) {
		return Iterables.filter(features, f -> !isFeatureInsidePolygon(f, polygonGeometry));
	}

	public static Geometry readPolygonGeometry(String file) {
		Collection<SimpleFeature> ftColl = ShapeFileReader.getAllFeatures(file);
		if (ftColl.size() != 1) {
			throw new RuntimeException("No. of features: " + ftColl.size() + "; should be 1");
		}
		return (Geometry)ftColl.iterator().next().getDefaultGeometry();
	}
}
