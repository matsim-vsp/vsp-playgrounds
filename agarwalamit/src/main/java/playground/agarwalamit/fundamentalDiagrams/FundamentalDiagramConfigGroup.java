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

package playground.agarwalamit.fundamentalDiagrams;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.utils.collections.CollectionUtils;

/**
 * Created by amit on 03.07.17.
 */

public class FundamentalDiagramConfigGroup extends ReflectiveConfigGroup {

    public FundamentalDiagramConfigGroup() {
        super(GROUP_NAME);
    }

    public static final String GROUP_NAME = "fundamentalDiagram";

    private static final String TRACK_LINK_LENGTH = "trackLinkLength";
    private double trackLinkLength = 1000.0;

    private static final String TRACK_LINK_CAPACITY = "trackLinkCapacity";
    private double trackLinkCapacity = 1600.;

    private static final String TRACK_LINK_SPEED = "trackLinkSpeed";
    private double trackLinkSpeed = 16.67;

    private static final String TRACK_LINK_LANES = "trackLinkLanes";
    private double trackLinkLanes = 1;

    private static final String TRACK_LINK_ALLOWED_MODS = "trackLinkAllowedModes";
    private Collection<String> trackLinkAllowedModes = Arrays.asList(TransportMode.car);

    private static final String TRACK_LINK_DIVISON_FACTOR = "trackLinkDivisionFactor";
    private int trackLinkDivisionFactor = 1;

    private static final String REDUCE_DATA_POINTS_BY_FACTOR = "reduceDataPointsByFactor";
    private int reduceDataPointsByFactor = 1;

    private static final String WRITING_EVENTS = "isWritingEvents";
    private boolean isWritingEvents = false;

    private static final String RUNNIG_DISTRIBUTION = "isRunningDistribution";
    private boolean isRunningDistribution = false;

    private static final String DYNAMIC_PCU = "isUsingDynamicPCU";
    private boolean isUsingDynamicPCU = false;

    private static final String MODAL_SHARE_PCU = "modalShareInPCU";
    private Collection<Double> modalShareInPCU = Arrays.asList(1.0);

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

    @StringGetter(TRACK_LINK_ALLOWED_MODS)
    public String getTrackLinkAllowedModesAsString() {
        return CollectionUtils.setToString(new HashSet<>(getTrackLinkAllowedModes()));
    }

    public Set<String> getTrackLinkAllowedModes() {
        return new HashSet<>(trackLinkAllowedModes);
    }

    @StringSetter(TRACK_LINK_ALLOWED_MODS)
    public void setTrackLinkAllowedModes(String trackLinkAllowedModes) {
        this.trackLinkAllowedModes = Arrays.asList(trackLinkAllowedModes.split(","));
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

    @StringGetter(WRITING_EVENTS)
    public boolean isWritingEvents() {
        return isWritingEvents;
    }

    @StringSetter(WRITING_EVENTS)
    public void setWritingEvents(boolean writingEvents) {
        isWritingEvents = writingEvents;
    }

    @StringGetter(RUNNIG_DISTRIBUTION)
    public boolean isRunningDistribution() {
        return isRunningDistribution;
    }

    @StringSetter(RUNNIG_DISTRIBUTION)
    public void setRunningDistribution(boolean runningDistribution) {
        isRunningDistribution = runningDistribution;
    }

    @StringGetter(DYNAMIC_PCU)
    public boolean isUsingDynamicPCU() {
        return isUsingDynamicPCU;
    }

    @StringSetter(DYNAMIC_PCU)
    public void setUsingDynamicPCU(boolean usingDynamicPCU) {
        isUsingDynamicPCU = usingDynamicPCU;
    }

    @StringGetter(MODAL_SHARE_PCU)
    public String getModalShareInPCUAsString() {
        return CollectionUtils.setToString(new HashSet<>(getModalShareInPCU().stream().map(String::valueOf).collect(Collectors.toList())));
    }

    public Collection<Double> getModalShareInPCU() {
        return modalShareInPCU;
    }

    @StringSetter(MODAL_SHARE_PCU)
    public void setModalShareInPCU(String modalShareInPCU) {
        this.modalShareInPCU = Arrays.asList(modalShareInPCU.split(",")).stream().map(Double::valueOf).collect(Collectors.toList());
    }
}
