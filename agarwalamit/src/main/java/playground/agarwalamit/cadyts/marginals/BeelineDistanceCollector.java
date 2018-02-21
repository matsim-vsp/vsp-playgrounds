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

package playground.agarwalamit.cadyts.marginals;

import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.network.NetworkUtils;

/**
 * Created by amit on 21.02.18.
 */

public class BeelineDistanceCollector implements PersonDepartureEventHandler, PersonArrivalEventHandler {

    private final Network network;
    private final PlansCalcRouteConfigGroup configGroup;
    private final DistanceDistribution inputDistanceDistribution;

    private final DistanceDistribution outputDistanceDistribution = new DistanceDistribution();

    @Inject
    public BeelineDistanceCollector(
            Network network,
            PlansCalcRouteConfigGroup plansCalcRouteConfigGroup,
            DistanceDistribution inputDistanceDistribution,
            EventsManager eventsManager
    ){
        eventsManager.addHandler(this);
        this.network = network;
        this.configGroup = plansCalcRouteConfigGroup;
        this.inputDistanceDistribution = inputDistanceDistribution;
    }

    private final Map<Id<Person>, Coord> personToOriginCoord = new HashMap<>();

    @Override
    public void handleEvent(PersonArrivalEvent event) {
        String mode  = event.getLegMode();

        Coord originCoord = this.personToOriginCoord.get(event.getPersonId());
        Coord destinationCoord = this.network.getLinks().get(event.getLinkId()).getToNode().getCoord();

        //TODO check if we should include beeline distance factor which is not available for network mdoes
        PlansCalcRouteConfigGroup.ModeRoutingParams params = this.configGroup.getModeRoutingParams().get(mode);
        double beelineDistanceFactor = 1.3;
        if (params!=null) beelineDistanceFactor = params.getBeelineDistanceFactor();
        double beelineDistance = beelineDistanceFactor *
                NetworkUtils.getEuclideanDistance(originCoord, destinationCoord);

        DistanceBin.DistanceRange distanceRange = DistanceDistributionUtils.getDistanceRange(beelineDistance, this.inputDistanceDistribution.getDistanceRanges(mode));
        outputDistanceDistribution.addToDistribution(mode, distanceRange, +1);
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        this.personToOriginCoord.put(event.getPersonId(), network.getLinks().get(event.getLinkId()).getToNode().getCoord());
    }

    @Override
    public void reset(int iteration) {
        this.personToOriginCoord.clear();
    }

    public DistanceDistribution getOutputDistanceDistribution() {
        return outputDistanceDistribution;
    }
}
