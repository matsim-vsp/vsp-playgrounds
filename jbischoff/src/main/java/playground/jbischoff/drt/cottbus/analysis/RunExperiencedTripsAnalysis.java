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

package playground.jbischoff.drt.cottbus.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RunExperiencedTripsAnalysis {
    public static void main(String[] args) {
//        String dir = "D:/runs-svn/cottbus/FuehrerBA/cb04";
        String dir = "C:/Users/Joschka/git/vsp-playgrounds/jbischoff/output/stopbased_norejects_notransitwalks_er_350/";
//		List<String> runIds =  Arrays.asList(new String[]{"door2door_550"});
        //List<String> runIds = Arrays.asList(new String[]{"door2door_200", "door2door_250", "door2door_300", "door2door_400", "door2door_500", "door2door_550", "door2door_600"});
        List<String> runIds = Arrays.asList(new String[]{"stopbased_200", "stopbased_250", "stopbased_300", "stopbased_400", "stopbased_500", "stopbased_600"});
//        List<String> runIds = Arrays.asList(new String[]{""});
        System.out.println(runIds);
        for (String runId : runIds) {

            String runDirectory = dir + runId + "/";
            String runPrefix = runDirectory + "/" + runId + ".";
            //String runPrefix = runDirectory + "/" ;

            Set<String> monitoredModes = new HashSet<>();
            monitoredModes.add("pt");
            monitoredModes.add("drt");
            monitoredModes.add("drt_walk");
            monitoredModes.add("transit_walk");


            Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
            new MatsimNetworkReader(scenario.getNetwork()).readFile(runPrefix + "output_network.xml.gz");
            //new TransitScheduleReader(scenario).readFile(runPrefix+"output_transitSchedule.xml.gz");


            // Analysis
            EventsManager events = EventsUtils.createEventsManager();


            Set<Id<Link>> monitoredStartAndEndLinks = new HashSet<>();

            DrtPtTripEventHandler eventHandler = new DrtPtTripEventHandler(scenario.getNetwork(), scenario.getTransitSchedule(),
                    monitoredModes, monitoredStartAndEndLinks);
            events.addHandler(eventHandler);
            new DrtEventsReader(events).readFile(runPrefix + "output_events.xml.gz");
            System.out.println("Start writing trips of " + eventHandler.getPerson2ExperiencedTrips().size() + " agents.");
            ExperiencedTripsWriter tripsWriter = new ExperiencedTripsWriter(runPrefix +
                    "experiencedTrips.csv",
                    eventHandler.getPerson2ExperiencedTrips(), monitoredModes, scenario.getNetwork());
            tripsWriter.writeExperiencedTrips();
            ExperiencedTripsWriter legsWriter = new ExperiencedTripsWriter(runPrefix +
                    "experiencedLegs.csv",
                    eventHandler.getPerson2ExperiencedTrips(), monitoredModes, scenario.getNetwork());
            legsWriter.writeExperiencedLegs();
        }
    }

}
