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

package playground.jbischoff.drt.cottbus;/*
 * created by jbischoff, 12.09.2018
 */

import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import java.util.Arrays;
import java.util.List;

public class RunDrtCottbus {
    public static void main(String[] args) {

        String folder = "D:/Bachelorarbeit/scenarios/Cottbus_DRT/";
        String configFile = folder + "drtconfig_stops.xml";
//        List<String> vehicleFiles = Arrays.asList(new String[]{"550"});
        List<String> vehicleFiles = Arrays.asList(new String[]{"200", "250", "300", "400", "500", "550", "600"});


        for (String vehicles : vehicleFiles) {
            final Config config = ConfigUtils.loadConfig(configFile, new DrtConfigGroup(), new DvrpConfigGroup(),
                    new OTFVisConfigGroup());
            config.controler().setLastIteration(1);
            config.controler().setWriteEventsInterval(1);
            config.controler().setWritePlansInterval(1);
            config.controler().setRunId("door2door_" + vehicles);
            config.controler().setOutputDirectory("output/stopbased_rejects_notransitwalks_er_350/" + config.controler().getRunId());
            DrtConfigGroup drt = (DrtConfigGroup) config.getModules().get(DrtConfigGroup.GROUP_NAME);
            drt.setVehiclesFile(folder + "drt_vehicles_" + vehicles + ".xml");
            drt.setMaxWaitTime(600);
            drt.setMaxWaitTime(600);
            drt.setMaxWalkDistance(350);
            drt.setOperationalScheme(DrtConfigGroup.OperationalScheme.stopbased.toString());
            drt.setRequestRejection(true);
            Controler controler = DrtControlerCreator.createControler(config, false);

            controler.run();
        }

    }

}
