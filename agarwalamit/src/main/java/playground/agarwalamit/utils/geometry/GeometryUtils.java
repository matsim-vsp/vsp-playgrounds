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
package playground.agarwalamit.utils.geometry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.shape.random.RandomPointsBuilder;

/**
 * @author amit
 */

public final class GeometryUtils {

	private GeometryUtils(){}
	private static final Random RAND = MatsimRandom.getRandom();
	private static final GeometryFactory GF = new GeometryFactory();

	/**
	 * @return a random point inside given feature
	 */
	public static Point getRandomPointsInsideFeature (final SimpleFeature feature) {
		return getRandomPointsInsideGeometry( (Geometry) feature.getDefaultGeometry() );
	}

	/**
	 * @return a random point inside given geometry
	 */
	public static Point getRandomPointsInsideGeometry (final Geometry geometry) {
		RandomPointsBuilder rnd = new RandomPointsBuilder(GF);
		rnd.setNumPoints(1);
		rnd.setExtent(geometry);
		Coordinate coordinate = rnd.getGeometry().getCoordinates()[0];
		return GF.createPoint(coordinate);
	}

	/**
	 * Create one geometry from given list of features and then find a random point side the geoemtry.
	 */
	public static Point getRandomPointsInsideFeatures (final List<SimpleFeature> features) {
		Geometry combinedGeometry = getGeometryFromListOfFeatures(features);
		return getRandomPointsInsideGeometry(combinedGeometry);
	}

	/**
	 * @return true if centroid of the link is covered by any of the geometry
	 */
	public static boolean isLinkInsideGeometries(final Collection<Geometry> geometries, final Link link) {
		Coord coord = link.getCoord();
		Point point = GF.createPoint(new Coordinate(coord.getX(), coord.getY()));
		return playground.vsp.corineLandcover.GeometryUtils.isPointInsideGeometries(geometries, point);
	}

	/**
	 * @return true if coord is covered by any of the geometry
	 */
	public static boolean isCoordInsideGeometries(final Collection<Geometry> geometries, final Coord coord) {
		Point point = GF.createPoint(new Coordinate(coord.getX(), coord.getY()));
		return playground.vsp.corineLandcover.GeometryUtils.isPointInsideGeometries(geometries, point);
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
	 * @return true if point is covered by ANY of the geometry
	 */
	public static boolean isPointInsideFeatures(final Collection<SimpleFeature> features, final Point point) {
		if (features.isEmpty()) throw new RuntimeException("Collection of features is empty.");
		for(SimpleFeature sf : features){
			if ( (playground.vsp.corineLandcover.GeometryUtils.getSimplifiedGeom( (Geometry) sf.getDefaultGeometry() ) ).contains(point) ) {
				return true;
			}
		}
		return false;
	}

	public static Collection<Geometry> getSimplifiedGeometries(final Collection<SimpleFeature> features){
		Collection<Geometry> geoms = new ArrayList<>();
		for(SimpleFeature sf:features){
			geoms.add(playground.vsp.corineLandcover.GeometryUtils.getSimplifiedGeom( (Geometry) sf.getDefaultGeometry()));
		}
		return geoms;
	}

//	public static Tuple<Double,Double> getMaxMinXFromFeatures (final List<SimpleFeature> features){
//		double minX = Double.POSITIVE_INFINITY;
//		double maxX = Double.NEGATIVE_INFINITY;
//
//		for (SimpleFeature f : features){
//			BoundingBox bounds = f.getBounds();
//			double localMinX = bounds.getMinX();
//			double localMaxX = bounds.getMaxX();
//			if (minX > localMinX) minX = localMinX;
//			if (maxX < localMaxX) maxX = localMaxX;
//		}
//		return new Tuple<>(minX, maxX);
//	}
//
//	public static Tuple<Double,Double> getMaxMinYFromFeatures (final List<SimpleFeature> features){
//		double minY = Double.POSITIVE_INFINITY;
//		double maxY = Double.NEGATIVE_INFINITY;
//
//		for (SimpleFeature f : features){
//			BoundingBox bounds = f.getBounds();
//			double localMinY = bounds.getMinY();
//			double localMaxY = bounds.getMaxY();
//			if (minY > localMinY) minY = localMinY;
//			if (maxY < localMaxY) maxY = localMaxY;
//		}
//		return new Tuple<>(minY, maxY);
//	}

	public static Geometry getGeometryFromListOfFeatures(final Collection<SimpleFeature> features) {
		List<Geometry> geoms = features.stream().map(f -> (Geometry)f.getDefaultGeometry()).collect(Collectors.toList());
		return playground.vsp.corineLandcover.GeometryUtils.combine(geoms);
	}

	/**
	 * @param shapeFile
	 * @return bounding
	 */
	public static ReferencedEnvelope getBoundingBox(final String shapeFile){
		ShapeFileReader shapeFileReader = new ShapeFileReader();
		shapeFileReader.readFileAndInitialize(shapeFile);
		return shapeFileReader.getBounds();
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

	// rest of the methods are moved to playground.vsp.corineLandcover.GeometryUtils. Amit Oct'17
}