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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import playground.agarwalamit.utils.MapUtils;

/**
 * Created by amit on 15.11.17.
 */

public class OnRoadExposureTable {

    private static final Logger LOG = Logger.getLogger(OnRoadExposureTable.class);
    private Map<Id<Person>, List<OnRoadTripExposureInfo>> personInfo = new HashMap<>();

    /**
     * @param personId
     * @param linkId
     * @param mode
     * @param time
     */
    public void createTripAndAddInfo(Id<Person> personId, Id<Link> linkId, String mode, Double time) {
        List<OnRoadTripExposureInfo> list = this.personInfo.get(personId);
        if (list == null ) {
            list = new ArrayList<>();
        }
        OnRoadTripExposureInfo tripExposureInfo =  new OnRoadTripExposureInfo(personId, mode);
        tripExposureInfo.addInhaledMass(time, linkId, new HashMap<>());
        list.add(tripExposureInfo);
        this.personInfo.put(personId, list);
    }


        /**
         * @param personId
         * @param linkId
         * @param mode
         * @param time
         * @param inhaledMass if already exists in the table, values will be summed
         */
    public void addInfoToTable(Id<Person> personId, Id<Link> linkId, String mode, Double time, Map<String, Double> inhaledMass) {
        List<OnRoadTripExposureInfo> list = this.personInfo.get(personId);
        OnRoadTripExposureInfo info = list.get(list.size()-1);
        if (! info.mode.equals(mode)) throw new RuntimeException("A new mode is found for same trip.");
        info.addInhaledMass(time, linkId, inhaledMass);
    }

    public static class OnRoadTripExposureInfo{
        private Id<Person> personId;
        private String mode;

        private final Map<Double,Map<String,Double>> time2Emissions = new HashMap<>();
        private final Map<Id<Link>,Map<String,Double>> link2Emissions = new HashMap<>();
//        Table<Double,Id<Link>, Map<String,Double>> time2link2emissions = HashBasedTable.create();

        OnRoadTripExposureInfo(Id<Person> person, String mode) {
            this.personId = person;
            this.mode = mode;
        }

        void addInhaledMass(double time, Id<Link> linkId, Map<String, Double> inhaledMass){
            {
                Map<String, Double> soFar = this.time2Emissions.get(time);
                if (soFar==null) this.time2Emissions.put(time, inhaledMass);
                else this.time2Emissions.put(time, MapUtils.mergeMaps(inhaledMass, soFar));
            }

            {
                Map<String, Double> soFar = this.link2Emissions.get(linkId);
                if (soFar==null) this.link2Emissions.put(linkId, inhaledMass);
                else this.link2Emissions.put(linkId, MapUtils.mergeMaps(inhaledMass, soFar));
            }
        }
    }

    public void clear(){
        this.personInfo.clear();
    }

    public Map<String, Double> getTotalInhaledMass(){
        LOG.info("Computing total inhaled mass ...");
        Map<String, Double> out = new HashMap<>();
        for (List<OnRoadTripExposureInfo> infoList : this.personInfo.values()){
            for (OnRoadTripExposureInfo info : infoList) {
                out = MapUtils.mergeMaps(out, MapUtils.valueMapSum(info.link2Emissions));
            }
        }
        return out;
    }

    public Map<Id<Person>, Map<String, Double>> getPersonToInhaledMass(){
        LOG.info("Computing total inhaled mass for each person ...");
        Map<Id<Person>, Map<String, Double>> out = new HashMap<>();
        for (List<OnRoadTripExposureInfo>  infoList : this.personInfo.values()){
            for (OnRoadTripExposureInfo info : infoList) {
                Map<String, Double> tempSum = out.get(info.personId);
                if (tempSum == null) tempSum = new HashMap<>();
                tempSum = MapUtils.mergeMaps(tempSum,
                        MapUtils.valueMapSum(info.link2Emissions));
                out.put(info.personId, tempSum);
            }
        }
        return out;
    }

    public Map<String, Map<String,Double>> getModeToInhaledMass(){
        LOG.info("Computing total inhaled mass for each mode ...");
        Map<String, Map<String, Double>> out = new HashMap<>();
        for (List<OnRoadTripExposureInfo>  infoList : this.personInfo.values()){
            for (OnRoadTripExposureInfo info : infoList) {
                Map<String, Double> tempOut = out.get(info.mode);
                if (tempOut == null) tempOut = new HashMap<>();
                tempOut = MapUtils.mergeMaps(tempOut, MapUtils.valueMapSum(info.link2Emissions));
                out.put(info.mode, tempOut);
            }
        }
        return out;
    }

    public Map<Id<Link>,Map<String,Double>> getLinkToInhaledMass(){
        LOG.info("Computing total inhaled mass for each link ...");
        Map<Id<Link>, Map<String,Double>> outMap = new HashMap<>();
        for (List<OnRoadTripExposureInfo>  infoList : this.personInfo.values()){
            for (OnRoadTripExposureInfo info : infoList) {
                outMap = MapUtils.mergeMultiMaps(outMap, info.link2Emissions);
            }
        }
        return outMap;
    }

    public Map<Double,Map<String,Double>> getTimeToInhaledMass() {
        LOG.info("Computing total inhaled mass in each time bin ...");
        Map<Double, Map<String,Double>> outMap = new HashMap<>();
        for (List<OnRoadTripExposureInfo>  infoList : this.personInfo.values()){
            for (OnRoadTripExposureInfo info : infoList) {
                outMap = MapUtils.mergeMultiMaps(outMap, info.time2Emissions);
            }
        }
        return outMap;
    }
}
