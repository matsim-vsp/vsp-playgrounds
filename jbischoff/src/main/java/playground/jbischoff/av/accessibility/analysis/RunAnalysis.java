/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.jbischoff.av.accessibility.analysis;/*
 * created by jbischoff, 17.05.2018
 */

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RunAnalysis {

    public static void main(String[] args) {
        new RunAnalysis().run();
    }

    public void run() {
        Network network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile("D:/runs-svn/avsim/av_accessibility/input/berlin_only_net_dominik.xml.gz");
        Map<String, List<Double>[]> aggregatedWaitTimes = new HashMap<>();
        network.getLinks().values().forEach(l -> {
            String zone = (String) l.getAttributes().getAttribute("zoneId");
            if (zone != null) {
                List<Double>[] waitsPerHour = new List[24];
                for (int i = 0; i < waitsPerHour.length; i++) {
                    waitsPerHour[i] = new ArrayList<>();
                }
                aggregatedWaitTimes.put(zone, waitsPerHour);
            }
        });

        for (int i = 0; i < 5; i++) {
            EventsManager events = EventsUtils.createEventsManager();
            events.addHandler(new WaitTimeHandler(network, aggregatedWaitTimes));
            new MatsimEventsReader(events).readFile("D:\\runs-svn\\avsim\\av_accessibility\\output/taxi" + i + "/b5_22.output_events.xml.gz");

        }
        Map<String, int[]> averageHourlyWaitTimes = new HashMap<>();

        for (Map.Entry<String, List<Double>[]> entry : aggregatedWaitTimes.entrySet()) {
            averageHourlyWaitTimes.put(entry.getKey(), new int[24]);
            for (int i = 0; i < entry.getValue().length; i++) {
                DescriptiveStatistics statistics = new DescriptiveStatistics();
                entry.getValue()[i].forEach(aDouble -> statistics.addValue(aDouble));
                averageHourlyWaitTimes.get(entry.getKey())[i] = (int) statistics.getMean();
            }
        }

        final BufferedWriter bw = IOUtils.getBufferedWriter("D:\\runs-svn\\avsim\\av_accessibility\\output/averageTaxiWaitTimes_dominik.csv");
        try {
            bw.write("zoneId");
            for (int i = 0; i < 24; i++) {
                bw.write(";" + i);
            }
            for (Map.Entry<String, int[]> e : averageHourlyWaitTimes.entrySet()) {
                bw.newLine();
                bw.write(e.getKey());
                for (int i = 0; i < 24; i++) {
                    bw.write(";" + e.getValue()[i]);
                }
            }
            bw.flush();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    class WaitTimeHandler implements PersonDepartureEventHandler, PersonEntersVehicleEventHandler {

        private Network network;
        Map<String, List<Double>[]> aggregatedWaitTimes;
        Map<Id<Person>, Double> taxiDepartures = new HashMap<>();
        Map<Id<Person>, Id<Link>> taxiDepartureLocations = new HashMap<>();

        public WaitTimeHandler(Network network, Map<String, List<Double>[]> aggregatedWaitTimes) {
            this.network = network;
            this.aggregatedWaitTimes = aggregatedWaitTimes;
        }

        @Override
        public void handleEvent(PersonDepartureEvent event) {
            if (event.getLegMode().equals(TransportMode.taxi)) {
                taxiDepartures.put(event.getPersonId(), event.getTime());
                taxiDepartureLocations.put(event.getPersonId(), event.getLinkId());
            }
        }

        @Override
        public void handleEvent(PersonEntersVehicleEvent event) {
            if (taxiDepartures.containsKey(event.getPersonId())) {
                double departureTime = taxiDepartures.remove(event.getPersonId());
                double waitTime = event.getTime() - departureTime;

                int hour = (int) (departureTime / 3600);
                if (hour < 24) {
                    Link l = network.getLinks().get(taxiDepartureLocations.remove(event.getPersonId()));
                    if (l != null) {
                        String zone = (String) l.getAttributes().getAttribute("zoneId");
                        if (zone != null) {
                            aggregatedWaitTimes.get(zone)[hour].add(waitTime);
                        }
                    }
                }
            }
        }
    }

}
