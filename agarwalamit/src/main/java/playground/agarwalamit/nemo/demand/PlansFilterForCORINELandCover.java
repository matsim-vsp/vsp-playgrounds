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

package playground.agarwalamit.nemo.demand;

import java.util.Collection;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;
import playground.agarwalamit.utils.LoadMyScenarios;
import playground.vsp.demandde.cemdap.output.Cemdap2MatsimUtils;
import playground.vsp.demandde.corineLandcover.CorineLandCoverData;

/**
 * Created by amit on 24.10.17.
 *
 * - takes matsim plans, CORINE shape file and zonal shape file as input
 * - check for every activity location, if they are inside the correct zone based on activity type
 * - if not, reassign the coordinate.
 */

public class PlansFilterForCORINELandCover {

    private final CorineLandCoverData corineLandCoverData;
    private final Population population;

    private final Collection<SimpleFeature> zoneFeatures;

    private final boolean sameHomeActivity ;
    private String zoneIdTag;

    /**
     * @param matsimPlans
     * @param zoneFile
     * @param CORINELandCoverFile
     * @param simplifyGeoms
     *
     * For this, it is assumed that home activity location is same in all plans of a person. If this is not the case, use other constructor.
     */
    public PlansFilterForCORINELandCover (String matsimPlans, String zoneFile, String CORINELandCoverFile, boolean simplifyGeoms) {
        this(matsimPlans, zoneFile, CORINELandCoverFile, simplifyGeoms, true);
    }

    public PlansFilterForCORINELandCover (String matsimPlans, String zoneFile, String CORINELandCoverFile, boolean simplifyGeoms, boolean sameHomeActivity) {
        this.corineLandCoverData = new CorineLandCoverData(CORINELandCoverFile, simplifyGeoms);
        this.population = LoadMyScenarios.loadScenarioFromPlans(matsimPlans).getPopulation();
        this.zoneFeatures = ShapeFileReader.getAllFeatures(zoneFile);
        this.sameHomeActivity = sameHomeActivity;
    }

    public static void main(String[] args) {

        String corineLandCoverFile = "/Users/amit/Documents/gitlab/nemo/data/cemdap_input/shapeFiles/CORINE_landcover_nrw/corine_nrw_src_clc12.shp";
        String zoneFile = "/Users/amit/Documents/gitlab/nemo/data/cemdap_input/shapeFiles/sourceShape_NRW/modified/dvg2gem_nw_mod.prj.shp";
        int numberOfPlansFile = 100;
        String matsimPlans = "/Users/amit/Documents/gitlab/nemo/data/matsim_initial/" + numberOfPlansFile + "/matsim_initial_plans_1pct.xml.gz";
        boolean simplifyGeom = true;
        boolean sameHomeActivity = true;
        String outPlans = "/Users/amit/Documents/gitlab/nemo/data/matsim_initial/matsim_initial_plans_1pct_filteredForCorineLandCover.xml.gz";

        PlansFilterForCORINELandCover plansFilterForCORINELandCover = new PlansFilterForCORINELandCover(matsimPlans, zoneFile, corineLandCoverFile, simplifyGeom, sameHomeActivity);
        plansFilterForCORINELandCover.process();
        plansFilterForCORINELandCover.writePlans(outPlans);
    }

    public void process() {
        for(Person person : population.getPersons().values()) {

            Coord homeCoord = null;

            for (Plan plan : person.getPlans()){
                for (PlanElement planElement : plan.getPlanElements()){
                    if (planElement instanceof Activity) {
                        Activity activity = (Activity) planElement;
                        String activityType = activity.getType();
                        Coord coord = activity.getCoord();

                        if (coord==null) {
                            reassignCoord(activityType, activityType.split("_")[1]);
                        } else {
                            boolean reassignCoord = false;
                            Point point = MGC.coord2Point(coord);

                            if ( activity.equals("home") ) {
                                if (homeCoord==null || ! sameHomeActivity) {
                                    homeCoord = coord;
                                    reassignCoord = ! corineLandCoverData.isPointInsideLandCover(activityType, point);
                                } else {
                                    if (! homeCoord.toString().equals(coord.toString())) {
                                        throw new RuntimeException("Home location for person "+person.getId().toString()+ " is different in plans." +
                                                " If this is possible, then use another construction and set \'sameHomeActivity\' to false.");
                                    }
                                    // dont have to check for reassignment because home activities in all plans are same for a person
                                }
                            } else {
                                reassignCoord = ! corineLandCoverData.isPointInsideLandCover(activityType, point);
                            }
                            if ( reassignCoord ) {
                                activity.setCoord(reassignCoord(point,activityType));
                            }
                        }
                    }
                }
            }
        }
    }

    public void writePlans(String outFile){
        new PopulationWriter(population).write(outFile);
    }

    /**
     *
     * @param activityType
     * @param zoneId
     * @return a random coord if there was no coordinate assigned to activity already.
     */
    private Coord reassignCoord(String activityType, String zoneId){
        SimpleFeature zone =null;
        for (SimpleFeature feature : this.zoneFeatures) {
            String featureId = (String) feature.getAttribute(zoneIdTag);
            String shapeId = Cemdap2MatsimUtils.removeLeadingZeroFromString(featureId);
            if (shapeId==zoneId) {
                zone = feature;
                break;
            }
        }
        return corineLandCoverData.getRandomCoord(zone, activityType);
    }

    /**
     *
     * @param point
     * @param activityType
     * @return a random coord if there was already a coordinate assigned to activity.
     */
    private Coord reassignCoord(Point point, String activityType){
        SimpleFeature zone = null;
        for (SimpleFeature feature : this.zoneFeatures) {
            if ( ((Geometry)feature).contains(point)) {
                zone = feature;
                break;
            }
        }
        return corineLandCoverData.getRandomCoord(zone, activityType);
    }
}
