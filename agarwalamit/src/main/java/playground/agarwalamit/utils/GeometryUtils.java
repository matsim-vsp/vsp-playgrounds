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
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.simplify.TopologyPreservingSimplifier;
import org.opengis.geometry.BoundingBox;

/**
 * @author amit
 */

public final class GeometryUtils {

	private GeometryUtils(){}
	private static final Random RAND = MatsimRandom.getRandom(); // matsim random will return same coord.
	private static final GeometryFactory GF = new GeometryFactory();

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

	public static boolean isLinkInsideGeometries(final Collection<Geometry> features, final Link link) {
		Coord coord = link.getCoord();
		Geometry linkGeo = GF.createPoint(new Coordinate(coord.getX(), coord.getY()));
		for(Geometry  geo: features){
			if ( geo.contains(linkGeo) ) {
				return true;
			}
		}
		return false;
	}
	
	public static boolean isCoordInsideFeatures(final Collection<Geometry> features, final Coord coord) {
		Geometry point = GF.createPoint(new Coordinate(coord.getX(), coord.getY()));
		for(Geometry  geo: features){
			if ( geo.contains(point) ) {
				return true;
			}
		}
		return false;
	}
	
	public static boolean isLinkInsideFeatures(final Collection<SimpleFeature> features, final Link link) {
		Coord coord = link.getCoord();
		Geometry geo = GF.createPoint(new Coordinate(coord.getX(), coord.getY()));
		for(SimpleFeature sf : features){
			if ( ( getSimplifiedGeom( (Geometry) sf.getDefaultGeometry() ) ).contains(geo) ) {
				return true;
			}
		}
		return false;
	}

	public static boolean isPointInsideFeatures(final Collection<SimpleFeature> features, final Point point) {
		Geometry geo = GF.createPoint( new Coordinate( point.getCoordinate() ) );
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
	 * @return A simplified geometry by increasing tolerance until number of vertices are less than 1000.
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


	public static Geometry getSimplifiedGeom(final Geometry geom, final double distanceTolerance){
		return TopologyPreservingSimplifier.simplify(geom, distanceTolerance);
	}

	public static int getNumberOfVertices(final Geometry geom){
		return geom.getNumPoints();
	}

	public static Point getRandomPointsInsideFeatures (final List<SimpleFeature> features) {
		Tuple<Double,Double> xs = getMaxMinXFromFeatures(features);
		Tuple<Double,Double> ys = getMaxMinYFromFeatures(features);
		Geometry combinedGeometry = getGeometryFromListOfFeatures(features);
		Point p = null;
		double x,y;
		do {
			x = xs.getFirst()+RAND.nextDouble()*(xs.getSecond() - xs.getFirst());
			y = ys.getFirst()+RAND.nextDouble()*(ys.getSecond() - ys.getFirst());
			p= MGC.xy2Point(x, y);
		} while (! (combinedGeometry).contains(p) );
		return p;
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

	public static ReferencedEnvelope getBoundingBox(final String shapeFile){
		ShapeFileReader shapeFileReader = new ShapeFileReader();
		shapeFileReader.readFileAndInitialize(shapeFile);
		return shapeFileReader.getBounds();
	}
}