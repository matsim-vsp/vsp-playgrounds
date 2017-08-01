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

package playground.agarwalamit.corineLandcover;

import java.util.*;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import org.apache.log4j.Logger;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;
import playground.agarwalamit.utils.GeometryUtils;

/**
 * Created by amit on 31.07.17.
 */

public class LandCoversMerger {

    public static final Logger LOGGER = Logger.getLogger(LandCoversMerger.class);

    private final Collection<SimpleFeature> landcoverFeatures ;
    private final Map<SimpleFeature, Map<ActivityTypeFromLandCover, Geometry>> zone2act2geo = new HashMap<>();

    public LandCoversMerger(final String corineLandCoverShapeFile) {
        LOGGER.info("Reading CORINE landcover shape file . . .");
        this.landcoverFeatures = ShapeFileReader.getAllFeatures(corineLandCoverShapeFile);
    }

    public static void main(String[] args) {

        String landcoverFile = "../../repos/shared-svn/projects/nemo_mercator/30_Scenario/cemdap_input/shapeFiles/CORINE_landcover_nrw/corine_nrw_src_clc12.shp";
        String zoneFile = "../../repos/shared-svn/projects/nemo_mercator/30_Scenario/cemdap_input/shapeFiles/sourceShape_NRW/dvg2gem_nw.shp";
        new LandCoversMerger(landcoverFile).merge(ShapeFileReader.getAllFeatures(zoneFile));
    }

    public void merge(final  Collection<SimpleFeature>  zoneFeatures) {
        LOGGER.info("Filtering landcover zones for each zone...");
        Map<SimpleFeature, Map<ActivityTypeFromLandCover, List<Geometry>>> zone2activityType2landcoverGeom = new HashMap<>();
        for (SimpleFeature lancoverZone : this.landcoverFeatures) {
            int landcoverId = Integer.valueOf( (String) lancoverZone.getAttribute(LandCoverUtils.CORINE_LANDCOVER_TAG_ID));
            List<ActivityTypeFromLandCover> acts = LandCoverUtils.getActivitiesTypeFromZone(landcoverId);

            for(SimpleFeature zone : zoneFeatures ) {
                if ( ((Geometry) zone.getDefaultGeometry()).contains( (Geometry) lancoverZone.getDefaultGeometry()  ) ) {
                     if ( zone2activityType2landcoverGeom.containsKey(zone) ) {
                         Map<ActivityTypeFromLandCover, List<Geometry>> activityTypeFromLandCoverListMap = zone2activityType2landcoverGeom.get(zone);
                        for(ActivityTypeFromLandCover activityTypeFromLandCover : acts) {
                         if (activityTypeFromLandCoverListMap.containsKey(activityTypeFromLandCover)) {
                             activityTypeFromLandCoverListMap.get(activityTypeFromLandCover).add((Geometry) lancoverZone.getDefaultGeometry());
                         } else {
                             List<Geometry> geometryList = new ArrayList<>();
                             geometryList.add((Geometry) lancoverZone.getDefaultGeometry());
                             activityTypeFromLandCoverListMap.put(activityTypeFromLandCover, geometryList);
                         }
                        }
                     } else {
                         Map<ActivityTypeFromLandCover, List<Geometry>> activityTypeFromLandCoverListMap = new HashMap<>();
                         for(ActivityTypeFromLandCover activityTypeFromLandCover : acts) {
                             List<Geometry> geometryList = new ArrayList<>();
                             geometryList.add((Geometry) lancoverZone.getDefaultGeometry());
                             activityTypeFromLandCoverListMap.put(activityTypeFromLandCover, geometryList);
                         }
                         zone2activityType2landcoverGeom.put(zone, activityTypeFromLandCoverListMap);
                     }
                }
            }
        }
        LOGGER.info("Filtering is finished. Merging the geometries of the same activity types ...");
        for(SimpleFeature zone : zone2activityType2landcoverGeom.keySet()) {
            Map<ActivityTypeFromLandCover, Geometry> act2geom = new HashMap<>();
            for(ActivityTypeFromLandCover activityTypeFromLandCover : zone2activityType2landcoverGeom.get(zone).keySet() ) {
                Geometry geo = GeometryUtils.combine(zone2activityType2landcoverGeom.get(zone).get(activityTypeFromLandCover));
                act2geom.put(activityTypeFromLandCover, geo);
            }
            zone2act2geo.put(zone, act2geom);
        }
    }

    public Geometry getGeometry(final SimpleFeature feature, final ActivityTypeFromLandCover activityTypeFromLandCover) {
        return this.zone2act2geo.get(feature).get(activityTypeFromLandCover);
    }

    public Point getRandomPoint (final SimpleFeature feature, final  ActivityTypeFromLandCover activityTypeFromLandCover) {
        return GeometryUtils.getRandomPointsInsideGeometry(getGeometry(feature, activityTypeFromLandCover));
    }
}
