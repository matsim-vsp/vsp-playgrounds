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

package playground.vsp.cadyts.marginals;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.facilities.ActivityFacilities;
import playground.vsp.cadyts.marginals.prep.DistanceBin;
import playground.vsp.cadyts.marginals.prep.DistanceDistribution;
import playground.vsp.cadyts.marginals.prep.DistanceDistributionUtils;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by amit on 20.06.18.
 */

class EventsToBeelinDistanceRange {

    private static final Logger LOG = Logger.getLogger(EventsToBeelinDistanceRange.class);

    private final Network network;
    private final ActivityFacilities activityFacilities;

    @com.google.inject.Inject(optional=true)
    private AgentFilter agentFilter;
    private final DistanceDistribution inputDistanceDistribution;
    private final PlansCalcRouteConfigGroup plansCalcRouteConfigGroup;

    private final Map<Id<Person>, Coord> personToOriginCoord = new HashMap<>();
    private final Map<Id<Person>, String> personToMode = new HashMap<>();

    private int stuckEventsCounter = 0;
    private Set<String> warnedAboutExclusion = new HashSet<>();

    @Inject
    EventsToBeelinDistanceRange(Scenario scenario, DistanceDistribution inputDistanceDistribution){
        this.network = scenario.getNetwork();
        this.activityFacilities = scenario.getActivityFacilities();
        this.inputDistanceDistribution = inputDistanceDistribution;
        this.plansCalcRouteConfigGroup = scenario.getConfig().plansCalcRoute();
    }

    public DistanceBin.DistanceRange handleEvent(ActivityStartEvent event) {
        if (agentFilter!=null && !agentFilter.includeAgent(event.getPersonId())) return null;

        String mode  = personToMode.get(event.getPersonId());
        if (this.inputDistanceDistribution.getDistanceRanges(mode).isEmpty()) {

            // warn on the first occurence
            if (!warnedAboutExclusion.contains(mode)) {
                warnedAboutExclusion.add(mode);
                LOG.warn("The distance range for mode " + mode + " in the input distance distribution is empty. This will be excluded from the calibration.");
            }
            // always skip
            return null;
        }

        Coord originCoord = this.personToOriginCoord.get(event.getPersonId());
        Coord destinationCoord;
        if (this.activityFacilities.getFacilities().isEmpty() || event.getFacilityId() == null) {
            destinationCoord = this.network.getLinks().get(event.getLinkId()).getToNode().getCoord();
        } else {
            destinationCoord = this.activityFacilities.getFacilities().get(event.getFacilityId()).getCoord();
        }

        PlansCalcRouteConfigGroup.ModeRoutingParams params = this.plansCalcRouteConfigGroup.getModeRoutingParams().get(mode);

        double beelineDistanceFactor = 1.3;
        if (params!=null) {
            beelineDistanceFactor = params.getBeelineDistanceFactor();
        } else if (this.inputDistanceDistribution.getModeToBeelineDistanceFactor().containsKey(mode)){
            beelineDistanceFactor = this.inputDistanceDistribution.getModeToBeelineDistanceFactor().get(mode);
        } else{
            LOG.warn("The beeline distance factor for mode "+mode+" is not given. Using 1.3");
        }

        double beelineDistance = beelineDistanceFactor *
                NetworkUtils.getEuclideanDistance(originCoord, destinationCoord);
        try {
            return DistanceDistributionUtils.getDistanceRange(beelineDistance, this.inputDistanceDistribution.getDistanceRanges(mode));
        } catch (Exception e) {
            LOG.error("getDistanceRange threw an error");
            LOG.error("desired distance was: " + beelineDistance);
            LOG.error("desired mode was: " + mode);

            Set<DistanceBin.DistanceRange> ranges = this.inputDistanceDistribution.getDistanceRanges(mode);
            LOG.error("the distance ranges for mode " + mode + " says the following on toString() " + ranges.toString());

                LOG.error("the following distance ranges were found for mode: " + mode);
                ranges.forEach(range -> LOG.error(range.toString()));
            throw new RuntimeException(e);
        }
    }

    public void handleEvent(PersonDepartureEvent event) {
        if (agentFilter!=null && !agentFilter.includeAgent(event.getPersonId())) return; // if filtering and agent should not be included

        this.personToMode.put(event.getPersonId(), event.getLegMode());
    }

    public void handleEvent(ActivityEndEvent event) {
        if (agentFilter!=null && !agentFilter.includeAgent(event.getPersonId())) return; // if filtering and agent should not be included

        if(activityFacilities.getFacilities().isEmpty() || event.getFacilityId() == null) {
            this.personToOriginCoord.put(event.getPersonId(), network.getLinks().get(event.getLinkId()).getToNode().getCoord());
        } else {
            this.personToOriginCoord.put(event.getPersonId(), this.activityFacilities.getFacilities().get(event.getFacilityId()).getCoord());
        }
    }

    public void reset(){
        this.personToMode.clear();
        this.personToOriginCoord.clear();
    }

    public void handleEvent(PersonStuckEvent event) {
        LOG.warn("Stuck event# " + ++stuckEventsCounter + " : " + event);
        LOG.warn("This means, all trips are not included in the output distance distribution.");
    }

    Map<Id<Person>, String> getPersonToMode(){
        return this.personToMode;
    }

}
