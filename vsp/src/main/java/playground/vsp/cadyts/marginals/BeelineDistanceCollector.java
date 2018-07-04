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

import com.google.inject.Inject;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import playground.vsp.cadyts.marginals.prep.DistanceBin;
import playground.vsp.cadyts.marginals.prep.DistanceDistribution;

/**
 * Created by amit on 21.02.18.
 */

public class BeelineDistanceCollector implements ActivityEndEventHandler, ActivityStartEventHandler, PersonDepartureEventHandler, PersonStuckEventHandler {

    private DistanceDistribution outputDistanceDistribution = new DistanceDistribution();

    private final EventsToBeelinDistanceRange eventsToBeelinDistanceRange;

    @Inject
    public BeelineDistanceCollector(EventsToBeelinDistanceRange handler,
                                    DistanceDistribution inputDistanceDistribution,
                                    EventsManager eventsManager){
        this.eventsToBeelinDistanceRange = handler;
        eventsManager.addHandler(this);

        inputDistanceDistribution.getModeToBeelineDistanceFactor()
                                 .forEach(outputDistanceDistribution::setBeelineDistanceFactorForNetworkModes);

        inputDistanceDistribution.getModeToScalingFactor()
                                 .forEach(outputDistanceDistribution::setModeToScalingFactor);
    }

    // following is useful if not using Guice (i.e. simple events analysis)
    public BeelineDistanceCollector(
            Scenario scenario,
            DistanceDistribution inputDistanceDistribution,
            EventsManager eventsManager,
            AgentFilter agentFilter) {
        this(new EventsToBeelinDistanceRange(scenario, inputDistanceDistribution), inputDistanceDistribution, eventsManager);
    }

    @Override
    public void handleEvent(ActivityStartEvent event) {

        DistanceBin.DistanceRange distanceRange = this.eventsToBeelinDistanceRange.handleEvent(event);
        if (distanceRange==null) return; // i.e. this agent is excluded.
        outputDistanceDistribution.addToDistribution(this.eventsToBeelinDistanceRange.getPersonToMode().get(event.getPersonId()), distanceRange, +1);
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
       this.eventsToBeelinDistanceRange.handleEvent(event);
    }

    @Override
    public void handleEvent(ActivityEndEvent event) {
        this.eventsToBeelinDistanceRange.handleEvent(event);
    }

    @Override
    public void reset(int iteration) {
        this.eventsToBeelinDistanceRange.reset();
        this.outputDistanceDistribution = new DistanceDistribution();
    }

    public DistanceDistribution getOutputDistanceDistribution() {
        return outputDistanceDistribution;
    }

    @Override
    public void handleEvent(PersonStuckEvent event) {
        this.eventsToBeelinDistanceRange.handleEvent(event);
    }
}
