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
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

/**
 * Created by amit on 31.07.17.
 */

public class CorineLandCoverData {

    public static final Logger LOGGER = Logger.getLogger(CorineLandCoverData.class);
    private final Map<String, Geometry> activityType2LandcoverZone = new HashMap<>();
    private int warnCnt = 0;
    private final LandCoverUtils landCoverUtils = new LandCoverUtils();

    private final boolean simplifyGeometries;

    public CorineLandCoverData(final String corineLandCoverShapeFile, final boolean simplifyGeometries) {
        LOGGER.info("Reading CORINE landcover shape file . . .");
        Collection<SimpleFeature> landCoverFeatures = ShapeFileReader.getAllFeatures(corineLandCoverShapeFile);

        this.simplifyGeometries = simplifyGeometries;
        if (this.simplifyGeometries) LOGGER.warn("Geometries will be simplified such that number of vertices in each geometry is less than 1000. This is likely to speed up the process.");

        Map<String, List<Geometry>> activityTypes2ListOfGeometries = new HashMap<>();

        for (SimpleFeature landCoverZone : landCoverFeatures) {
            int landCoverId = Integer.valueOf( (String) landCoverZone.getAttribute(LandCoverUtils.CORINE_LANDCOVER_TAG_ID));
            List<String> acts = landCoverUtils.getActivityTypesFromZone(landCoverId);

            for (String activityTypeFromLandCover : acts ) {
                List<Geometry> geoms = activityTypes2ListOfGeometries.get(activityTypeFromLandCover);
                if (geoms==null) {
                    geoms = new ArrayList<>();
                }

                Geometry geomToAdd = (Geometry)landCoverZone.getDefaultGeometry();
                if (this.simplifyGeometries) geomToAdd = GeometryUtils.getSimplifiedGeom(geomToAdd);

                geoms.add(  geomToAdd );
                activityTypes2ListOfGeometries.put(activityTypeFromLandCover, geoms);
            }
        }

        // combined geoms of the same activity types
        // Not sure, Out of two options --simplify and merge OR merge and simplify-- I think, the former would be somewhat better. Amit Oct'17
        for (String activityTypeFromLandCover : activityTypes2ListOfGeometries.keySet()) {
            LOGGER.info("Merging the geometries of the activity types "+activityTypeFromLandCover+".");
            Geometry combinedGeom = GeometryUtils.combine(activityTypes2ListOfGeometries.get(activityTypeFromLandCover));
            activityType2LandcoverZone.put(activityTypeFromLandCover, combinedGeom);
        }
    }

    public CorineLandCoverData(final String corineLandCoverShapeFile) {
        this(corineLandCoverShapeFile, false);
    }

    // An example
    public static void main(String[] args) {
        String landcoverFile = "../../repos/shared-svn/projects/nemo_mercator/30_Scenario/cemdap_input/shapeFiles/CORINE_landcover_nrw/corine_nrw_src_clc12.shp";
        String zoneFile = "../../repos/shared-svn/projects/nemo_mercator/30_Scenario/cemdap_input/shapeFiles/sourceShape_NRW/dvg2gem_nw.shp";
        CorineLandCoverData landCoverInformer = new CorineLandCoverData(landcoverFile, true);
//        landCoverInformer.getRandomPoint(...);
    }

    /**
     * @param feature simpleFeature (zone)
     * @param activityType classified as 'home' and 'other' activity types.
     * @return a random point such that it is inside the @param feature as well as inside the zones corresponding to the given activity type.
     */
    public Point getRandomPoint (final SimpleFeature feature, final String activityType) {
        Geometry zoneGeom;
        if ( this.simplifyGeometries ) {
              zoneGeom = GeometryUtils.getSimplifiedGeom( (Geometry) feature.getDefaultGeometry() ) ;
        } else {
            zoneGeom = (Geometry) feature.getDefaultGeometry();
        }

        Geometry landUseGeom ;
        if (activityType.equalsIgnoreCase("home") ) {
            landUseGeom =  this.activityType2LandcoverZone.get(activityType) ;
        } else {
           if (warnCnt < 1) {
               LOGGER.warn("A random point is desired for activity type "+ activityType+ ". However, the CORINE landcover data is categorized only for 'home' and 'other' activity types.");
               LOGGER.warn(Gbl.ONLYONCE);
               warnCnt++;
           }
            landUseGeom =  this.activityType2LandcoverZone.get("other") ;
        }

        return GeometryUtils.getPointInteriorToGeometries( landUseGeom, zoneGeom );
    }

    /**
     * @param feature simpleFeature (zone)
     * @param activityType classified as 'home' and 'other' activity types.
     * @return
     */
    public Coord getRandomCoord (final SimpleFeature feature, final String activityType) {
        Point p = getRandomPoint(feature, activityType);
        return new Coord(p.getX(), p.getY());
    }

    /**
     *
     * @param activityType
     * @param point
     * @return if point falls inside the geom of given activity type
     */
    public boolean isPointInsideLandCover(String activityType, Point point){
        Geometry geometry = this.activityType2LandcoverZone.get(activityType);
        return geometry.contains(point);
    }
}
