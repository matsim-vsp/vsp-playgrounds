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

package playground.agarwalamit.emissions;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.vehicles.Vehicle;

/**
 * The problems are:
 * <ul>
 *     <li>The cold emissions are thrown when vehicle enters the traffic as well as at later time when vehicle completes 2km distance.
 *     With, the latter, the emissions are thrown on the link where the vehicle departed, however the time is current time and not the vehicle enters traffic event time.</li>
 *     <li> Warm emissions events are thrown after a vehicle has left.</li>
 * </ul>
 *
 * The idea to track the emissions is to store all emissions information until:
 * <ul>
 *     <li> warm emissions : clean during next warm emissions event </li>
 *     <li> cold emissions : vehicle has completed 2km distance </li>
 * </ul>
 * Created by amit on 15.11.17.
 */

public class EmissionsTrackerHandler {
    private final double DISTANCE_CAP_FOR_COLD_EMISSIONS = 1000.0; // >1000m is the cap. (see line 107 and 100 in ColdEmissionHandler
        private final Id<Vehicle> vehicleId;

        private LinkedHashMap<Id<Link>, ColdEmissionEvent> coldEmissionEventMap = new LinkedHashMap<>(); // no fixed size
        private LinkedList<WarmEmissionEvent> warmEmissionEventLinkedList = new LinkedList<>(); // FIFO queue; at most 2 events should be sufficient

        private double dist = 0.;

        public EmissionsTrackerHandler(Id<Vehicle> vehicleId){
            this.vehicleId = vehicleId;
        }

        public Map<Id<Link>, ColdEmissionEvent> getColdEmissionEventMap() {
            return coldEmissionEventMap;
        }

        public WarmEmissionEvent getAndAddWarmEmissionEvent(WarmEmissionEvent event) {
            warmEmissionEventLinkedList.addLast(event);
            return warmEmissionEventLinkedList.pollFirst(); // null if list is empty
        }

        public boolean clearColdEmissionMap(double distance){
            dist =+ distance;
            if (dist > DISTANCE_CAP_FOR_COLD_EMISSIONS) { // see line 100 in ColdEmissionHandler
                this.coldEmissionEventMap.clear(); // cold emission info is not required anymore
                return true;
            } else {
                return false;
            }
        }

}
