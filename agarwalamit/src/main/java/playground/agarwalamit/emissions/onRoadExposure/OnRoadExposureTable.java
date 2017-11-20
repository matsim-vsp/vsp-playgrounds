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

package playground.agarwalamit.emissions.onRoadExposure;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import playground.agarwalamit.utils.MapUtils;
import playground.agarwalamit.utils.TableUtils;

/**
 * Created by amit on 15.11.17.
 */

public class OnRoadExposureTable {

    private static final Logger LOG = Logger.getLogger(OnRoadExposureTable.class);
    private Table<Id<Person>, String, OnRoadTripExposureInfo> personInfo = HashBasedTable.create();

    /**
     * @param personId
     * @param linkId
     * @param mode
     * @param time
     * @param inhaledMass if already exists in the table, values will be summed
     */
    public void addInfoToTable(Id<Person> personId, Id<Link> linkId, String mode, Double time, Map<String, Double> inhaledMass) {
        OnRoadTripExposureInfo tripExposureInfo = this.personInfo.get(personId, mode);
        if (tripExposureInfo == null) {
                tripExposureInfo = new OnRoadTripExposureInfo(personId, mode);
        }
        tripExposureInfo.addInhaledMass(time, linkId, inhaledMass);
        this.personInfo.put(personId, mode, tripExposureInfo);
    }

    public class OnRoadTripExposureInfo{
        private Id<Person> personId;
        private String mode;

        Table<Double,Id<Link>, Map<String,Double>> time2link2emissions = HashBasedTable.create();

        OnRoadTripExposureInfo(Id<Person> person, String mode) {
            this.personId = person;
            this.mode = mode;
        }

        void addInhaledMass(double time, Id<Link> linkId, Map<String, Double> inhaledMass){
            Map<String, Double> massSoFar = this.time2link2emissions.get(time, linkId);
            if (massSoFar == null) {
                this.time2link2emissions.put(time, linkId, inhaledMass);
            } else {
                this.time2link2emissions.put(time, linkId, MapUtils.mergeMaps(massSoFar, inhaledMass));
            }
        }
    }

    public void clear(){
        this.personInfo.clear();
    }

    public Map<String, Double> getTotalInhaledMass(){
        LOG.info("Computing total inhaled mass ...");
        Map<String, Double> out = new HashMap<>();
        Set<Id<Person>> personIds = this.personInfo.rowKeySet();
        Set<String> modes = this.personInfo.columnKeySet();
        for (Id<Person> personId : personIds){
            for (String mode : modes) {
                if (! this.personInfo.contains(personId, mode)) continue;
                out = MapUtils.mergeMaps(out,
                        TableUtils.sumValues(this.personInfo.get(personId, mode).time2link2emissions));
            }
        }
        return out;
    }

    public Map<String, Map<String,Double>> getModeToInhaledMass(){
        LOG.info("Computing total inhaled mass for each mode ...");
        Map<String, Map<String, Double>> out = new HashMap<>();
        Set<Id<Person>> personIds = this.personInfo.rowKeySet();
        Set<String> modes = this.personInfo.columnKeySet();
        for (String mode : modes) {
            Map<String, Double> tempOut = new HashMap<>();
            for (Id<Person> personId : personIds){
                if (this.personInfo.get(personId, mode)==null) continue;
                tempOut = MapUtils.mergeMaps(tempOut,
                        TableUtils.sumValues(this.personInfo.get(personId, mode).time2link2emissions));
            }
            out.put(mode, tempOut);
        }
        return out;
    }

    public Map<Id<Link>,Map<String,Double>> getLinkToInhaledMass(){
        LOG.info("Computing total inhaled mass for each link ...");
        Set<Id<Person>> personIds = this.personInfo.rowKeySet();
        Set<String> modes = this.personInfo.columnKeySet();

        Map<Id<Link>, Map<String,Double>> outMap = new HashMap<>();
        for (Id<Person> personId : personIds) {
            for (String mode : modes) {
                if (this.personInfo.get(personId, mode)==null) continue;
                outMap = MapUtils.mergeMultiMaps(outMap,
                        TableUtils.getLinkId2InhaledMass(this.personInfo.get(personId, mode).time2link2emissions));
            }
        }
        return outMap;
    }

    public Map<Double,Map<String,Double>> getTimeToInhaledMass() {
        LOG.info("Computing total inhaled mass in each time bin ...");
        Set<Id<Person>> personIds = this.personInfo.rowKeySet();
        Set<String> modes = this.personInfo.columnKeySet();

        Map<Double, Map<String,Double>> outMap = new HashMap<>();
        for (Id<Person> personId : personIds) {
            for (String mode : modes) {
                if (this.personInfo.get(personId, mode)==null) continue;
                outMap = MapUtils.mergeMultiMaps(outMap,
                        TableUtils.getTimeBin2InhaledMass(this.personInfo.get(personId, mode).time2link2emissions));
            }
        }
        return outMap;
    }
}
