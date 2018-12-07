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
import java.util.stream.Collectors;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;

/**
 * Created by amit on 22.11.17.
 */

public class EmissionEventsComparator {

    private static final List<String> eventTypes = Arrays.asList(
            ColdEmissionEvent.EVENT_TYPE,
            WarmEmissionEvent.EVENT_TYPE,
            LinkLeaveEvent.EVENT_TYPE
    );

    public static List<Event> sort(List<Event> inputList){
        return inputList
                .stream()
                .collect(Collectors.groupingBy(EmissionEventsComparator::getLink,
                Collectors.toList())) // link2event list
                        .entrySet()
                        .stream()
                        .flatMap(e -> sortForSameLink(e.getValue()).stream()) // sort list
                        .collect(Collectors.toList());
    }

    private static String getLink(Event event){
        if ( event.getAttributes().containsKey("link") ) {
            return event.getAttributes().get("link");
        } else {
           return  event.getAttributes().get("linkId");
        }
    }

    private static List<Event> sortForSameLink(List<Event> inputList){
        inputList.sort(new Comparator<Event>() {
            @Override
            public int compare(Event o1, Event o2) {
                return Integer.compare(eventTypes.indexOf(o1.getEventType()), eventTypes.indexOf(o2.getEventType()));
            }
        });
        return inputList;
    }
}
