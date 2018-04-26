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

package playground.agarwalamit.analysis.speed.linkSpeed;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;

/**
 * Created by amit on 18.01.18.
 */

public class LinkSpeedHandler implements LinkEnterEventHandler, LinkLeaveEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {

    private Map<Id<Link>,Map<Id<Person>,Double>> link2PersonEnterTime = new HashMap<>();
    private SortedMap<Double,Map<Id<Link>,Double>> time2link2SpeedSum = new TreeMap<>();
    private SortedMap<Double,Map<Id<Link>,Integer>> time2link2timeCount = new TreeMap<>();
    private Map<Id<Person>, String> person2Mode = new HashMap<>();

    private final double timeBinSize;
    private final Network network;
    private final List<String> modesUnderConsideration ;

    private final Vehicle2DriverEventHandler delegate = new Vehicle2DriverEventHandler();

    public LinkSpeedHandler(double simulationEndTime, int noOfTimeBins, Network network){
        this(simulationEndTime, noOfTimeBins, network, null);
    }

    public LinkSpeedHandler(double simulationEndTime, int noOfTimeBins, Network network, List<String> modesUnderConsideration){
        this.timeBinSize = simulationEndTime / noOfTimeBins;
        this.network = network;
        this.modesUnderConsideration = modesUnderConsideration;
    }

    @Override
    public void reset(int iteration) {
        this.time2link2SpeedSum.clear();
        this.time2link2timeCount.clear();
        this.link2PersonEnterTime.clear();
        this.delegate.reset(iteration);
        this.person2Mode.clear();
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        Id<Person> personId = this.delegate.getDriverOfVehicle(event.getVehicleId());
        String mode = this.person2Mode.get(personId);
        if ( this.modesUnderConsideration != null && ! this.modesUnderConsideration.contains(mode) ) {
            // modes under consideration for analysis are provided AND _mode_ is not one of them
            return;
        }

        Id<Link> linkId = event.getLinkId();
        double enterTime = event.getTime();

        if(link2PersonEnterTime.containsKey(linkId)){
            Map<Id<Person>,Double> p2et = link2PersonEnterTime.get(linkId);
            p2et.put(personId, enterTime);
        } else {
            Map<Id<Person>,Double> p2et = new HashMap<>();
            p2et.put(personId, enterTime);
            link2PersonEnterTime.put(linkId, p2et);
        }
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {

        Id<Person> personId = this.delegate.getDriverOfVehicle(event.getVehicleId());
        String mode = this.person2Mode.get(personId);
        if ( this.modesUnderConsideration != null && ! this.modesUnderConsideration.contains(mode) ) {
            // modes under consideration for analysis are provided AND _mode_ is not one of them
            return;
        }

        Id<Link> linkId = event.getLinkId();
        // ignore linkLeave on departure link
        if (this.link2PersonEnterTime.get(linkId) == null || this.link2PersonEnterTime.get(linkId).get(personId) == null ) return;

        double enterTime = this.link2PersonEnterTime.get(linkId).remove(personId);
        double speed = network.getLinks().get(linkId).getLength() * 3.6 / (event.getTime() - enterTime);

        double time = event.getTime();
        if (time==0.0) time = this.timeBinSize;
        double endOfTimeInterval = 0.0;
        endOfTimeInterval = Math.ceil(time/timeBinSize)*timeBinSize;
        if(endOfTimeInterval<=0.0)endOfTimeInterval=timeBinSize;

        Map<Id<Link>,Double> link2speedSum = time2link2SpeedSum.get(endOfTimeInterval);
        Map<Id<Link>,Integer> link2count = time2link2timeCount.get(endOfTimeInterval);

        if(link2speedSum == null){
            link2speedSum = new HashMap<>();
            link2speedSum.put(linkId, speed);

            link2count = new HashMap<>();
            link2count.put(linkId, 1);
        } else {
            link2speedSum.put(linkId, link2speedSum.getOrDefault(linkId,0.)+speed);
            link2count.put(linkId, link2count.getOrDefault(linkId,0)+1);
        }
        this.time2link2SpeedSum.put(endOfTimeInterval, link2speedSum);
        this.time2link2timeCount.put(endOfTimeInterval, link2count);
    }

    @Override
    public void handleEvent(VehicleEntersTrafficEvent event) {
        this.delegate.handleEvent(event);
        this.person2Mode.put(event.getPersonId(), event.getNetworkMode());
    }

    @Override
    public void handleEvent(VehicleLeavesTrafficEvent event) {
        this.delegate.handleEvent(event);
        this.person2Mode.remove(event.getPersonId());
    }

    private SortedMap<Double, Map<Id<Link>, Double>> time2link2timeMeanSpeed = new TreeMap<>();
    boolean timeMeanSpeedStored = false;

    public SortedMap<Double, Map<Id<Link>, Double>> getTime2link2AverageTimeMeanSpeed() {
        if (! timeMeanSpeedStored){
            timeMeanSpeedStored=true;
            for(double time : time2link2timeCount.keySet()) {
                Map<Id<Link>,Double> link2timeMeanSpeed = new HashMap<>();
                for (Id<Link> linkId : time2link2timeCount.get(time).keySet()){
                    link2timeMeanSpeed.put(linkId, time2link2SpeedSum.get(time).get(linkId) / time2link2timeCount.get(time).get(linkId));
                }
                time2link2timeMeanSpeed.put(time, link2timeMeanSpeed);
            }
        }
        return time2link2timeMeanSpeed;
    }
}

