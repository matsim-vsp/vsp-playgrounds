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
package playground.agarwalamit.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.simplify.TopologyPreservingSimplifier;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.geometry.BoundingBox;

/**
 * @author amit
 */

public final class GeometryUtils {

	private GeometryUtils(){}
	private static final Random RAND = MatsimRandom.getRandom(); // matsim random will return same coord.
	private static final GeometryFactory GF = new GeometryFactory();

	/**
	 * @return a random point inside given feature
	 */
	public static Point getRandomPointsInsideFeature (final SimpleFeature feature) {
		Point p = null;
		BoundingBox bounds = feature.getBounds();
		double x,y;
		do {
			double minX = bounds.getMinX();
			double minY = bounds.getMinY();
			x = minX +RAND.nextDouble()*(bounds.getMaxX()- minX);
			y = minY +RAND.nextDouble()*(bounds.getMaxY()- minY);
			p= MGC.xy2Point(x, y);
		} while ( ! ( (Geometry) feature.getDefaultGeometry() ).contains(p) );
		return p;
	}

	/**
	 * @return a random point inside given geometry
	 */
	public static Point getRandomPointsInsideGeometry (final Geometry geometry) {
		Point p = null;
		Envelope bounds = geometry.getEnvelopeInternal();
		double x,y;
		do {
			double minX = bounds.getMinX();
			double minY = bounds.getMinY();
			x = minX +RAND.nextDouble()*(bounds.getMaxX()- minX);
			y = minY +RAND.nextDouble()*(bounds.getMaxY()- minY);
			p= MGC.xy2Point(x, y);
		} while ( ! (geometry).contains(p) );
		return p;
	}

	/**
	 * @return a random point which is covered by all the geometries
	 */
	public static Point getRandomPointCommonToAllGeometries(final List<Geometry> geometries) {
		Point p = null;
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;

		for(  Geometry geometry : geometries ) {
			Envelope bounds = geometry.getEnvelopeInternal();
			minX = Math.min(minX, bounds.getMinX());
			minY = Math.min(minY, bounds.getMinY());
			maxX = Math.max(maxX, bounds.getMaxX());
			maxY = Math.max(maxY, bounds.getMaxY());
		}

		double x,y;
		do {
			x = minX +RAND.nextDouble()*(maxX- minX);
			y = minY +RAND.nextDouble()*(maxY- minY);
			p= MGC.xy2Point(x, y);
		} while ( ! isPointInsideAllGeometries(geometries, p) );
		return p;
	}

	/**
	 * Create one geometry from given list of features and then find a random point side the geoemtry.
	 */
	public static Point getRandomPointsInsideFeatures (final List<SimpleFeature> features) {
		Tuple<Double,Double> xs = getMaxMinXFromFeatures(features);
		Tuple<Double,Double> ys = getMaxMinYFromFeatures(features);
		Geometry combinedGeometry = getGeometryFromListOfFeatures(features);
		return getRandomPointsInsideGeometry(combinedGeometry);
	}

	/**
	 * @return true if centroid of the link is covered by any of the geometry
	 */
	public static boolean isLinkInsideGeometries(final Collection<Geometry> geometries, final Link link) {
		Coord coord = link.getCoord();
		Point point = GF.createPoint(new Coordinate(coord.getX(), coord.getY()));
		return isPointInsideGeometries(geometries, point);
	}

	/**
	 * @return true if coord is covered by any of the geometry
	 */
	public static boolean isCoordInsideGeometries(final Collection<Geometry> geometries, final Coord coord) {
		Point point = GF.createPoint(new Coordinate(coord.getX(), coord.getY()));
		return isPointInsideGeometries(geometries, point);
	}

	/**
	 * @return true if centroid of the link is covered by any of the geometry
	 */
	public static boolean isLinkInsideFeatures(final Collection<SimpleFeature> features, final Link link) {
		Coord coord = link.getCoord();
		Point geo = GF.createPoint(new Coordinate(coord.getX(), coord.getY()));
		return isPointInsideFeatures(features, geo);
	}

	/**
	 * @return true ONLY if point is covered by ALL geometries
	 */
	public static boolean isPointInsideAllGeometries(final Collection<Geometry> features, final Point point) {
		if (features.isEmpty()) throw new RuntimeException("Collection of geometries is empty.");
		for(Geometry sf : features){
			if ( ! sf.contains(point) ) {
				return false;
			}
		}
		return true;
	}

	/**
	 * @return true if point is covered by ANY of the geometry
	 */
	public static boolean isPointInsideGeometries(final Collection<Geometry> features, final Point point) {
		if (features.isEmpty()) throw new RuntimeException("Collection of geometries is empty.");
		for(Geometry sf : features){
			if ( sf.contains(point) ) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @return true if point is covered by ANY of the geometry
	 */
	public static boolean isPointInsideFeatures(final Collection<SimpleFeature> features, final Point point) {
		Geometry geo = GF.createPoint( new Coordinate( point.getCoordinate() ) );
		if (features.isEmpty()) throw new RuntimeException("Collection of features is empty.");
		for(SimpleFeature sf : features){
			if ( ( getSimplifiedGeom( (Geometry) sf.getDefaultGeometry() ) ).contains(geo) ) {
				return true;
			}
		}
		return false;
	}

	public static Collection<Geometry> getSimplifiedGeometries(final Collection<SimpleFeature> features){
		Collection<Geometry> geoms = new ArrayList<>();
		for(SimpleFeature sf:features){
			geoms.add(getSimplifiedGeom( (Geometry) sf.getDefaultGeometry()));
		}
		return geoms;
	}

	/**
	 * @param geom
	 * @return a simplified geometry by increasing tolerance until number of vertices are less than 1000.
	 */
	public static Geometry getSimplifiedGeom(final Geometry geom){
		Geometry outGeom = geom;
		double distanceTolerance = 1;
		int numberOfVertices = getNumberOfVertices(geom);
		while (numberOfVertices > 1000){
			outGeom = getSimplifiedGeom(outGeom, distanceTolerance);
			numberOfVertices = getNumberOfVertices(outGeom);
			distanceTolerance *= 10;
		}
		return outGeom;
	}

	/**
	 * simplify the geometry based on given tolerance
	 */

	public static Geometry getSimplifiedGeom(final Geometry geom, final double distanceTolerance){
		return TopologyPreservingSimplifier.simplify(geom, distanceTolerance);
	}

	public static int getNumberOfVertices(final Geometry geom){
		return geom.getNumPoints();
	}

	public static Tuple<Double,Double> getMaxMinXFromFeatures (final List<SimpleFeature> features){
		double minX = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;

		for (SimpleFeature f : features){
			BoundingBox bounds = f.getBounds();
			double localMinX = bounds.getMinX();
			double localMaxX = bounds.getMaxX();
			if (minX > localMinX) minX = localMinX;
			if (maxX < localMaxX) maxX = localMaxX;
		}
		return new Tuple<>(minX, maxX);
	}

	public static Tuple<Double,Double> getMaxMinYFromFeatures (final List<SimpleFeature> features){
		double minY = Double.POSITIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;

		for (SimpleFeature f : features){
			BoundingBox bounds = f.getBounds();
			double localMinY = bounds.getMinY();
			double localMaxY = bounds.getMaxY();
			if (minY > localMinY) minY = localMinY;
			if (maxY < localMaxY) maxY = localMaxY;
		}
		return new Tuple<>(minY, maxY);
	}

	public static Geometry getGeometryFromListOfFeatures(final List<SimpleFeature> features) {
		List<Geometry> geoms = new ArrayList<>();
		for(SimpleFeature sf : features){
			geoms.add( (Geometry) sf.getDefaultGeometry() );
		}
		return combine(geoms);
	}

	/**
	 * It perform "union" for each geometry and return one geometry.
	 */
	public static Geometry combine(final List<Geometry> geoms){
		Geometry geom = null;
		for(Geometry g : geoms){
			if(geom==null) geom = g;
			else {
				geom.union(g);
			}
		}
		return geom;
	}

	/**
	 *
	 * @param shapeFile
	 * @return bounding
	 */
	public static ReferencedEnvelope getBoundingBox(final String shapeFile){
		ShapeFileReader shapeFileReader = new ShapeFileReader();
		shapeFileReader.readFileAndInitialize(shapeFile);
		return shapeFileReader.getBounds();
	}
}