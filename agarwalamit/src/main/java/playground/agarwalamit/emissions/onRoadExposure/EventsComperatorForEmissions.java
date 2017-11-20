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

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;

/**
 * Assuming that all events are in same time step, else, sooner be returned.
 *
 * Created by amit on 20.11.17.
 */

public class EventsComperatorForEmissions implements Comparator<Event>{

    private List<String> narturalOrderOfEvents = Arrays.asList( // for a person in same
            ActivityEndEvent.EVENT_TYPE,
            PersonDepartureEvent.EVENT_TYPE,
            PersonEntersVehicleEvent.EVENT_TYPE,
            VehicleEntersTrafficEvent.EVENT_TYPE,
            LinkLeaveEvent.EVENT_TYPE,
            LinkEnterEvent.EVENT_TYPE,
            VehicleLeavesTrafficEvent.EVENT_TYPE,
            PersonLeavesVehicleEvent.EVENT_TYPE,
            PersonArrivalEvent.EVENT_TYPE,
            ActivityStartEvent.EVENT_TYPE
    );


    @Override
    public int compare(Event event1, Event event2) {
        int compareValue = new Double(event1.getTime()).compareTo(new Double(event2.getTime()));
        if (compareValue==0) {
            // now check if they belongs to same person/vehicle
            event1.getAttributes();
            


        } else return compareValue;

        return 0;
    }
}
