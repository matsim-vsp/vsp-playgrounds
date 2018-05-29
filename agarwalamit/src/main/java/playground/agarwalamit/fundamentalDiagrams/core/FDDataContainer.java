/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package playground.agarwalamit.fundamentalDiagrams.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by amit on 20.05.18.
 */

public class FDDataContainer {

    private final Map<String, TravelModesFlowDynamicsUpdator> travelModesFlowData = new HashMap<>();

    public Map<String, TravelModesFlowDynamicsUpdator> getTravelModesFlowData() {
        return travelModesFlowData;
    }

    private TravelModesFlowDynamicsUpdator globalData;

    public TravelModesFlowDynamicsUpdator getGlobalData() {
        return globalData;
    }

    void setGlobalData(TravelModesFlowDynamicsUpdator globalData) {
        this.globalData = globalData;
    }

    private final List<List<Integer>> listOfPointsToRun = new ArrayList<>();

    public List<List<Integer>> getListOfPointsToRun() {
        return listOfPointsToRun;
    }
}
