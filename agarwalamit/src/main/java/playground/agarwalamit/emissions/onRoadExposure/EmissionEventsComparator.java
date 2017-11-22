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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;

/**
 * Created by amit on 22.11.17.
 */

public class EmissionEventsComparator {

    private static final List<String> eventTypes = Arrays.asList(
            ColdEmissionEvent.EVENT_TYPE,
            WarmEmissionEvent.EVENT_TYPE,
            LinkLeaveEvent.EVENT_TYPE,
            LinkEnterEvent.EVENT_TYPE,
            VehicleLeavesTrafficEvent.EVENT_TYPE
    );

    private static final List<String> eventTypes_ZeroActDur = Arrays.asList(
            VehicleLeavesTrafficEvent.EVENT_TYPE,
            ColdEmissionEvent.EVENT_TYPE,
            WarmEmissionEvent.EVENT_TYPE,
            LinkLeaveEvent.EVENT_TYPE,
            LinkEnterEvent.EVENT_TYPE,
            VehicleEntersTrafficEvent.EVENT_TYPE
    );

    public static List<Event> sort(List<Event> inputList){

        boolean zeroActDur = false;
        // a major problem is: if vehicleLeave, arrival, actStart, actEnd, depart, vehicleEnter etc happen in same time step. Check for these and process accordingly
        List<String> tempList = inputList.stream().map(e-> e.getEventType()).collect(Collectors.toList());
        if (tempList.contains(VehicleLeavesTrafficEvent.EVENT_TYPE) && tempList.contains(VehicleEntersTrafficEvent.EVENT_TYPE)) {
            zeroActDur = true;
        }

        List<Event> subList = new ArrayList<>();
        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i< inputList.size(); i++) {
            if (eventTypes.contains(inputList.get(i).getEventType())) {
                subList.add(inputList.get(i));
                positions.add(i);
            }
        }

        if (zeroActDur) {
            Collections.sort(subList, new Comparator<Event>() {
                @Override
                public int compare(Event o1, Event o2) {
                    return Integer.valueOf(eventTypes_ZeroActDur.indexOf(o1.getEventType()))
                                  .compareTo(Integer.valueOf(eventTypes_ZeroActDur.indexOf(o2.getEventType())));
                }
            });
        } else {
            Collections.sort(subList, new Comparator<Event>() {
                @Override
                public int compare(Event o1, Event o2) {
                    return Integer.valueOf(eventTypes.indexOf(o1.getEventType()))
                                  .compareTo(Integer.valueOf(eventTypes.indexOf(o2.getEventType())));
                }
            });
        }

        for (int pos : positions) {
            inputList.remove(pos);
            inputList.add(pos, subList.remove(0));
        }

        return inputList;
    }
}
