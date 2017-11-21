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
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;

/**
 * Assuming that all events are in same time step, else, sooner be returned.
 *
 * Created by amit on 20.11.17.
 */

public class EventsComparatorForEmissions implements Comparator<Event> {

    public enum EventsOrder {
        NATURAL_ORDER, // this will fix the events which are messed up. e.g., cold emission event before vehicle enters traffic event
        EMISSION_EVENTS_BEFORE_LINK_LEAVE_EVENT // to include self exposure
    }

    public EventsComparatorForEmissions(List<String> desiredOrderOfEvents) {
        this.naturalOrderOfEvents = desiredOrderOfEvents;
    }

    public EventsComparatorForEmissions(EventsOrder eventsOrder) {
        switch (eventsOrder) {
            case NATURAL_ORDER:
                this.naturalOrderOfEvents = Arrays.asList(
                        ActivityEndEvent.EVENT_TYPE,
                        PersonDepartureEvent.EVENT_TYPE,
                        PersonEntersVehicleEvent.EVENT_TYPE,
                        VehicleEntersTrafficEvent.EVENT_TYPE,
                        LinkLeaveEvent.EVENT_TYPE,
                        LinkEnterEvent.EVENT_TYPE,
                        VehicleLeavesTrafficEvent.EVENT_TYPE,
                        PersonLeavesVehicleEvent.EVENT_TYPE,
                        PersonArrivalEvent.EVENT_TYPE,
                        ActivityStartEvent.EVENT_TYPE,
                        ColdEmissionEvent.EVENT_TYPE,
                        WarmEmissionEvent.EVENT_TYPE
                );
                break;
            case EMISSION_EVENTS_BEFORE_LINK_LEAVE_EVENT:
                this.naturalOrderOfEvents = Arrays.asList( // using only concerned events
                        VehicleEntersTrafficEvent.EVENT_TYPE,
                        ColdEmissionEvent.EVENT_TYPE,
                        WarmEmissionEvent.EVENT_TYPE,
                        LinkLeaveEvent.EVENT_TYPE,
                        LinkEnterEvent.EVENT_TYPE,
                        VehicleLeavesTrafficEvent.EVENT_TYPE
                );
                break;
            default:
                throw new RuntimeException("not implemented yet.");
        }
    }

    public EventsComparatorForEmissions() {
        this(EventsOrder.NATURAL_ORDER);
    }

    private final List<String> naturalOrderOfEvents;

    @Override
    public int compare(Event event1, Event event2) {
        int compareValue = new Double(event1.getTime()).compareTo(new Double(event2.getTime()));
        if (compareValue==0) {
            // now check if they belongs to same person/vehicle
            if ( ! getPersonIdFromEvent(event1).equals(getPersonIdFromEvent(event2))) return 0; // not same persons then return as they are
            else {
                return Integer.valueOf(naturalOrderOfEvents.indexOf(event1.getEventType()))
                              .compareTo(Integer.valueOf(naturalOrderOfEvents.indexOf(event2.getEventType())));
            }
        } else return compareValue;
    }

    private String getPersonIdFromEvent(Event event) {

        if (event instanceof ActivityEndEvent) {
            return ((ActivityEndEvent) event).getPersonId().toString();
        } else if (event instanceof PersonDepartureEvent) {
            return ((PersonDepartureEvent) event).getPersonId().toString();
        } else if (event instanceof PersonEntersVehicleEvent) {
            return ((PersonEntersVehicleEvent) event).getPersonId().toString();
        } else if (event instanceof VehicleEntersTrafficEvent) {
            return ((VehicleEntersTrafficEvent) event).getPersonId().toString();
        } else if (event instanceof LinkLeaveEvent) {
            return getPersonFromVehicleId(((LinkLeaveEvent) event).getVehicleId().toString());
        } else if (event instanceof LinkEnterEvent) {
            return getPersonFromVehicleId(((LinkEnterEvent) event).getVehicleId().toString());
        } else if (event instanceof VehicleLeavesTrafficEvent) {
            return ((VehicleLeavesTrafficEvent) event).getPersonId().toString();
        } else if (event instanceof PersonLeavesVehicleEvent) {
            return ((PersonLeavesVehicleEvent) event).getPersonId().toString();
        } else if (event instanceof PersonArrivalEvent) {
            return ((PersonArrivalEvent) event).getPersonId().toString();
        } else if (event instanceof ActivityStartEvent) {
            return ((ActivityStartEvent) event).getPersonId().toString();
        } else if (event instanceof ColdEmissionEvent) {
            return getPersonFromVehicleId(((ColdEmissionEvent) event).getVehicleId().toString());
        } else if (event instanceof WarmEmissionEvent) {
            return getPersonFromVehicleId(((WarmEmissionEvent) event).getVehicleId().toString());
        } else {
            throw new RuntimeException("Event "+event.toString()+", is not implemented yet.");
        }
    }

    private String getPersonFromVehicleId(String vehicleId){
        if (! vehicleId.contains("_")) return vehicleId;
        int lastIndexOf = vehicleId.lastIndexOf("_");
        return vehicleId.substring(0,lastIndexOf);
    }
}
