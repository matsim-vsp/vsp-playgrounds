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

package playground.jbischoff.pt.scenario;/*
 * created by jbischoff, 24.05.2018
 */

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MergeTransitSchedules {


    public static void main(String[] args) {

        String folder = "D:\\matsim_davis\\Scenario_1\\transit";
        List<String> list = listFilesForFolder(new File(folder));
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

        for (String f : list) {
            Scenario s2 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
            String p = f;
            p = p.replace(folder + "\\GTFSTransitData_", "");
            p = p.replace(".zip_transitSchedule.xml.gz", "");
            final String prefix = p;
            new TransitScheduleReader(s2).readFile(f);
            s2.getTransitSchedule().getFacilities().values().forEach(v ->
            {
                TransitStopFacility facility = s2.getTransitSchedule().getFactory().createTransitStopFacility(Id.create(prefix + "_" + v.getId().toString(), TransitStopFacility.class), v.getCoord(), v.getIsBlockingLane());
                facility.setName(v.getName());
                scenario.getTransitSchedule().addStopFacility(facility);
            });
            s2.getTransitSchedule().getTransitLines().values().forEach(l -> {
                TransitLine nl = scenario.getTransitSchedule().getFactory().createTransitLine(Id.create(prefix + "_" + l.getId().toString(), TransitLine.class));
                nl.setName(l.getName());
                l.getRoutes().values().forEach(r -> nl.addRoute(r));
                nl.getRoutes().values().forEach(tr -> tr.getStops().forEach(transitRouteStop -> transitRouteStop.setStopFacility(scenario.getTransitSchedule().getFacilities().get(Id.create(prefix + "_" + transitRouteStop.getStopFacility().getId().toString(), TransitStopFacility.class)))
                ));
                scenario.getTransitSchedule().addTransitLine(nl);
            });
        }
        new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile(folder + "/mergedSchedule.xml");

    }

    private static List<String> listFilesForFolder(final File folder) {
        List<String> files = new ArrayList<>();
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                listFilesForFolder(fileEntry);
            } else {
                if (!fileEntry.getName().endsWith(".xml.gz"))
                    continue;
                System.out.println(fileEntry.getName());
                files.add(fileEntry.getPath());
            }
        }
        return files;

    }
}
