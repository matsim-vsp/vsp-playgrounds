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

package playground.agarwalamit.fundamentalDiagrams.core;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * Created by amit on 03.07.17.
 */

public class FDConfigGroup extends ReflectiveConfigGroup {

    public FDConfigGroup() {
        super(GROUP_NAME);
    }

    public static final String GROUP_NAME = "fundamentalDiagram";

    private static final String TRACK_LINK_LENGTH = "trackLinkLength";
    private double trackLinkLength = 1000.0;
    static final String TRACK_LINK_LENGTH_CMT = "length of one side of the triangular equilateral race track. Default is 1000m.";

    private static final String TRACK_LINK_CAPACITY = "trackLinkCapacity";
    private double trackLinkCapacity = 1600.;
    static final String TRACK_LINK_CAPACITY_CMT = "capacity of the link of the triangular network. Deafult is 1600. PCU/h";

    private static final String TRACK_LINK_SPEED = "trackLinkSpeed";
    private double trackLinkSpeed = 60.0/3.6;
    static final String TRACK_LINK_SPEED_CMT = "maximum speed (in mps) on the link of the triangular network. Default is 60 kph.";

    private static final String TRACK_LINK_LANES = "trackLinkLanes";
    private double trackLinkLanes = 1;
    static final String TRACK_LINK_LANES_CMT = "number of lanes of the link of the triangular network. Default is 1.";

    private static final String TRACK_LINK_DIVISON_FACTOR = "trackLinkDivisionFactor";
    private int trackLinkDivisionFactor = 1;
    static final String TRACK_LINK_DIVISON_FACTOR_CMT = "a factor to cut each link of the triangular network in equal parts. Default is 1.";

    private static final String REDUCE_DATA_POINTS_BY_FACTOR = "reduceDataPointsByFactor";
    private int reduceDataPointsByFactor = 1;
    static final String REDUCE_DATA_POINTS_BY_FACTOR_CMT = "a factor by which the number of data points will be reduced to get quick results. \n" +
            " By default, all possible combinations for given modal share will be executed.";

    private static final String RUNNING_DISTRIBUTION = "isRunningDistribution";
    private boolean isRunningDistribution = false;
    private static final String RUNNING_DISTRIBUTION_CMT = "set to true if all possible combinations for all possible modal share should be executed. Default is false.";

    private static final String MODAL_SHARE_PCU = "modalShareInPCU";
    private Collection<Double> modalShareInPCU = Arrays.asList(1.0);
    private static final String MODAL_SHARE_PCU_CMT = "comma seperated modal share in PCU. By default, equal modal share will be used.";

    @StringGetter(TRACK_LINK_LENGTH)
    public double getTrackLinkLength() {
        return trackLinkLength;
    }

    @StringSetter(TRACK_LINK_LENGTH)
    public void setTrackLinkLength(double trackLinkLength) {
        this.trackLinkLength = trackLinkLength;
    }

    @StringGetter(TRACK_LINK_CAPACITY)
    public double getTrackLinkCapacity() {
        return trackLinkCapacity;
    }

    @StringSetter(TRACK_LINK_CAPACITY)
    public void setTrackLinkCapacity(double trackLinkCapacity) {
        this.trackLinkCapacity = trackLinkCapacity;
    }

    @StringGetter(TRACK_LINK_SPEED)
    public double getTrackLinkSpeed() {
        return trackLinkSpeed;
    }

    @StringSetter(TRACK_LINK_SPEED)
    public void setTrackLinkSpeed(double trackLinkSpeed) {
        this.trackLinkSpeed = trackLinkSpeed;
    }

    @StringGetter(TRACK_LINK_LANES)
    public double getTrackLinkLanes() {
        return trackLinkLanes;
    }

    @StringSetter(TRACK_LINK_LANES)
    public void setTrackLinkLanes(double trackLinkLanes) {
        this.trackLinkLanes = trackLinkLanes;
    }

    @StringGetter(TRACK_LINK_DIVISON_FACTOR)
    public int getTrackLinkDivisionFactor() {
        return trackLinkDivisionFactor;
    }

    @StringSetter(TRACK_LINK_DIVISON_FACTOR)
    public void setTrackLinkDivisionFactor(int trackLinkDivisionFactor) {
        this.trackLinkDivisionFactor = trackLinkDivisionFactor;
    }

    @StringGetter(REDUCE_DATA_POINTS_BY_FACTOR)
    public int getReduceDataPointsByFactor() {
        return reduceDataPointsByFactor;
    }

    @StringSetter(REDUCE_DATA_POINTS_BY_FACTOR)
    public void setReduceDataPointsByFactor(int reduceDataPointsByFactor) {
        this.reduceDataPointsByFactor = reduceDataPointsByFactor;
    }

    @StringGetter(RUNNING_DISTRIBUTION)
    public boolean isRunningDistribution() {
        return isRunningDistribution;
    }

    @StringSetter(RUNNING_DISTRIBUTION)
    public void setRunningDistribution(boolean runningDistribution) {
        isRunningDistribution = runningDistribution;
    }

    @StringGetter(MODAL_SHARE_PCU)
    public String getModalShareInPCUAsString() {
        return this.modalShareInPCU.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    public Collection<Double> getModalShareInPCU() {
        return modalShareInPCU;
    }

    @StringSetter(MODAL_SHARE_PCU)
    public void setModalShareInPCU(String modalShareInPCU) {
        this.modalShareInPCU = Arrays.asList(modalShareInPCU.split(","))
                                     .stream()
                                     .map(Double::valueOf)
                                     .collect(Collectors.toList());
    }

    @Override
    public Map<String, String> getComments() {
        Map<String, String> map = super.getComments();
        map.put(TRACK_LINK_LENGTH, TRACK_LINK_LENGTH_CMT);
        map.put(TRACK_LINK_CAPACITY, TRACK_LINK_CAPACITY_CMT);
        map.put(TRACK_LINK_SPEED, TRACK_LINK_SPEED_CMT);
        map.put(TRACK_LINK_LANES, TRACK_LINK_LANES_CMT);
        map.put(TRACK_LINK_DIVISON_FACTOR, TRACK_LINK_DIVISON_FACTOR_CMT);
        map.put(REDUCE_DATA_POINTS_BY_FACTOR, REDUCE_DATA_POINTS_BY_FACTOR_CMT);
        map.put(RUNNING_DISTRIBUTION, RUNNING_DISTRIBUTION_CMT);
        map.put(MODAL_SHARE_PCU, MODAL_SHARE_PCU_CMT);
        return map;
    }

    //
    private boolean writeDataIfNoStability = false;

    public boolean isWriteDataIfNoStability() {
        return writeDataIfNoStability;
    }

    public void setWriteDataIfNoStability(boolean writeDataIfNoStability) {
        this.writeDataIfNoStability = writeDataIfNoStability;
    }
}