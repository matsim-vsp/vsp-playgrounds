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
import java.util.HashMap;
import java.util.Map;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
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

public class PlansModifierForCORINELandCover {

    private static final Logger LOG = Logger.getLogger(PlansModifierForCORINELandCover.class);

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
    public PlansModifierForCORINELandCover(String matsimPlans, String zoneFile, String zoneIdTag, String CORINELandCoverFile, boolean simplifyGeoms, boolean combiningGeoms) {
        this(matsimPlans, zoneFile, zoneIdTag, CORINELandCoverFile, simplifyGeoms, combiningGeoms, true);
    }

    public PlansModifierForCORINELandCover(String matsimPlans, String zoneFile, String zoneIdTag, String CORINELandCoverFile, boolean simplifyGeoms, boolean combiningGeoms, boolean sameHomeActivity) {
        this.corineLandCoverData = new CorineLandCoverData(CORINELandCoverFile, simplifyGeoms, combiningGeoms);
        LOG.info("Loading population from plans file "+ matsimPlans);
        this.population = LoadMyScenarios.loadScenarioFromPlans(matsimPlans).getPopulation();
        LOG.info("Processing zone file "+ zoneFile);
        this.zoneFeatures = ShapeFileReader.getAllFeatures(zoneFile);
        this.zoneIdTag = zoneIdTag;
        this.sameHomeActivity = sameHomeActivity;
        if (this.sameHomeActivity) LOG.info("Home activities for a person will be at the same location.");
    }

    public static void main(String[] args) {

        String corineLandCoverFile = "/Users/amit/Documents/gitlab/nemo/data/cemdap_input/shapeFiles/CORINE_landcover_nrw/corine_nrw_src_clc12.shp";
        String zoneFile = "/Users/amit/Documents/gitlab/nemo/data/cemdap_input/shapeFiles/sourceShape_NRW/modified/dvg2gem_nw_mod.shp";
        String zoneIdTag = "KN";
        String matsimPlans = "/Users/amit/Documents/gitlab/nemo/data/input/matsim_initial_plans/plans_1pct_fullChoiceSet.xml.gz";
        boolean simplifyGeom = true;
        boolean combiningGeoms = false;
        boolean sameHomeActivity = true;
        String outPlans = "/Users/amit/Documents/gitlab/nemo/data/input/matsim_initial_plans/plans_1pct_fullChoiceSet_filteredForCorineLandCover.xml.gz";

        if(args.length > 0){
            corineLandCoverFile = args[0];
            zoneFile = args[1];
            zoneIdTag = args[2];
            matsimPlans = args[3];
            simplifyGeom = Boolean.valueOf(args[4]);
            combiningGeoms = Boolean.valueOf(args[5]);
            sameHomeActivity = Boolean.valueOf(args[6]);
            outPlans = args[7];
        }

        PlansModifierForCORINELandCover plansFilterForCORINELandCover = new PlansModifierForCORINELandCover(matsimPlans, zoneFile,zoneIdTag, corineLandCoverFile, simplifyGeom, combiningGeoms, sameHomeActivity);
        plansFilterForCORINELandCover.process();
        plansFilterForCORINELandCover.writePlans(outPlans);
    }

    public void process() {
        LOG.info("Start processing, this may take a while ... ");
        Map<Id<Person>, Coord> person2HomeCoord = new HashMap<>();
        for(Person person : population.getPersons().values()) {
            for (Plan plan : person.getPlans()){
                for (PlanElement planElement : plan.getPlanElements()){
                    if (planElement instanceof Activity) {
                        Activity activity = (Activity) planElement;
                        String activityType = activity.getType().split("_")[0];
                        String zoneId = activity.getType().split("_")[1];
                        Coord coord = activity.getCoord();

                        // during matsim plans generation, for home activities following fake coordinate is assigned.
                        Coord fakeCoord = new Coord(-1,-1);
                        if (coord.equals(fakeCoord)) coord=null;

                        if (activityType.equals("home") && sameHomeActivity) {
                            if (coord==null) {
                                coord = getRandomCoord(activityType, zoneId);
                                activity.setCoord(coord);
                                person2HomeCoord.put(person.getId(),coord);
                            } else {
                                boolean reassignCoord = false;
                                Point point = MGC.coord2Point(coord);

                                if (! person2HomeCoord.containsKey(person.getId())) {
                                    reassignCoord = ! corineLandCoverData.isPointInsideLandCover(activityType, point);
                                } else {
                                    if (! person2HomeCoord.get(person.getId()).toString().equals(coord.toString())) {
                                        throw new RuntimeException("Home location for person "+person.getId().toString()+ " is different in plans." +
                                                " If this is possible, then use another construction and set \'sameHomeActivity\' to false.");
                                    }
                                }
                                if ( reassignCoord ) {
                                    Coord newCoord = reassignCoord(point,activityType);
                                    activity.setCoord(newCoord);
                                    person2HomeCoord.put(person.getId(),newCoord);
                                }
                            }
                        } else {
                            if (coord ==null) {
                                coord = getRandomCoord(activityType, zoneId);
                                activity.setCoord(coord);
                            } else {
                                Point point = MGC.coord2Point(coord);

                                if (! corineLandCoverData.isPointInsideLandCover(activityType, point) ){
                                    activity.setCoord(reassignCoord(point,activityType));
                                }
                            }
                            // get a coord if it is null
                            // assign new coord if it is not null and not in the given feature
                        }
                        activity.setType(activityType);
                    }
                }
            }
        }
        LOG.info("Finished processing.");
    }

    public void writePlans(String outFile){
        LOG.info("Writing resulting plans to "+outFile);
        new PopulationWriter(population).write(outFile);
    }

    /**
     *
     * @param activityType
     * @param zoneId
     * @return a random coord if there was no coordinate assigned to activity already.
     */
    private Coord getRandomCoord(String activityType, String zoneId){
        SimpleFeature zone =null;
        for (SimpleFeature feature : this.zoneFeatures) {
            String featureId = (String) feature.getAttribute(zoneIdTag);
            String shapeId = Cemdap2MatsimUtils.removeLeadingZeroFromString(featureId);
            if (shapeId.equals(zoneId)) {
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
