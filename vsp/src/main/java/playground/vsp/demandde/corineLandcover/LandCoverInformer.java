/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.vsp.demandde.corineLandcover;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import org.apache.log4j.Logger;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

/**
 * Created by amit on 31.07.17.
 */

public class LandCoverInformer {

    public static final Logger LOGGER = Logger.getLogger(LandCoverInformer.class);
    private final Map<String, Geometry> activityType2LandcoverZone = new HashMap<>();

    public LandCoverInformer(final String corineLandCoverShapeFile) {
        LOGGER.info("Reading CORINE landcover shape file . . .");
        Collection<SimpleFeature> landCoverFeatures = ShapeFileReader.getAllFeatures(corineLandCoverShapeFile);

        LOGGER.info("Merging the geometries of the same activity types ...");
        Map<String, List<Geometry>> activityTypes2ListOfGeometries = new HashMap<>();

        for (SimpleFeature landCoverZone : landCoverFeatures) {
            int landCoverId = Integer.valueOf( (String) landCoverZone.getAttribute(LandCoverUtils.CORINE_LANDCOVER_TAG_ID));
            List<String> acts = LandCoverUtils.getActivitiesTypeFromZone(landCoverId);

            for (String activityTypeFromLandCover : acts ) {
                List<Geometry> geoms = activityTypes2ListOfGeometries.get(activityTypeFromLandCover);
                if (geoms==null) geoms = new ArrayList<>();

                geoms.add(  (Geometry)landCoverZone.getDefaultGeometry() );
                activityTypes2ListOfGeometries.put(activityTypeFromLandCover, geoms);
            }
        }

        // combined geoms of the same activity types
        for (String activityTypeFromLandCover : activityTypes2ListOfGeometries.keySet()) {
            activityType2LandcoverZone.put(activityTypeFromLandCover, combine(activityTypes2ListOfGeometries.get(activityTypeFromLandCover)));
        }
    }

    public static void main(String[] args) {
        String landcoverFile = "../../repos/shared-svn/projects/nemo_mercator/30_Scenario/cemdap_input/shapeFiles/CORINE_landcover_nrw/corine_nrw_src_clc12.shp";
        String zoneFile = "../../repos/shared-svn/projects/nemo_mercator/30_Scenario/cemdap_input/shapeFiles/sourceShape_NRW/dvg2gem_nw.shp";
        LandCoverInformer landCoverInformer = new LandCoverInformer(landcoverFile);
    }


    public Point getRandomPoint (final SimpleFeature feature, final String activityType) {
        List<Geometry> geoms = new ArrayList<>();
        geoms.add(  (Geometry) feature.getDefaultGeometry() );
        geoms.add( this.activityType2LandcoverZone.get(activityType) );
        return getRandomPointCommonToAllGeometries( geoms  );
    }

    private Geometry combine(final List<Geometry> geoms){
        Geometry geom = null;
        for(Geometry g : geoms){
            if(geom==null) geom = g;
            else {
                geom.union(g);
            }
        }
        return geom;
    }

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
            x = minX + MatsimRandom.getRandom().nextDouble()*(maxX- minX);
            y = minY +MatsimRandom.getRandom().nextDouble()*(maxY- minY);
            p= MGC.xy2Point(x, y);
        } while ( ! isPointInsideAllGeometries(geometries, p) );
        return p;
    }

    public static boolean isPointInsideAllGeometries(final Collection<Geometry> features, final Point point) {
        if (features.isEmpty()) throw new RuntimeException("Collection of geometries is empty.");
        for(Geometry sf : features){
            if ( ! sf.contains(point) ) {
                return false;
            }
        }
        return true;
    }

}
